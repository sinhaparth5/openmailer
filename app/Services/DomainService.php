<?php

namespace App\Services;

use App\Models\Domain;
use Illuminate\Support\Str;

class DomainService
{
    public function addDomain(int $userId, string $domainName): Domain
    {
        // Generate DKIM keys
        $dkimKeys = $this->generateDKIMKeys();

        // Generate verification token
        $verificationToken = Str::random(32);

        $domain = Domain::create([
            'user_id' => $userId,
            'domain' => $domainName,
            'verification_token' => $verificationToken,
            'dkim_selector' => 'mail',
            'dkim_private_key' => $dkimKeys['private_key'],
            'dkim_public_key' => $dkimKeys['public_key'],
            'status' => 'pending',
        ]);

        // Generate DNS records
        $this->generateDNSRecords($domain);

        return $domain;
    }

    public function verifyDomain(Domain $domain): bool
    {
        // Check TXT records for verification
        $txtRecords = dns_get_record($domain->domain, DNS_TXT);

        foreach ($txtRecords as $record) {
            if (isset($record['txt']) && $record['txt'] === 'openmailer-verification='.$domain->verification_token) {
                $domain->update([
                    'is_verified' => true,
                    'status' => 'verified',
                ]);

                return true;
            }
        }

        // Check DKIM record
        $dkimRecord = dns_get_record($domain->dkim_selector.'._domainkey.'.$domain->domain, DNS_TXT);

        $dkimValid = false;
        if (! empty($dkimRecord)) {
            foreach ($dkimRecord as $record) {
                if (isset($record['txt']) && strpos($record['txt'], $domain->dkim_public_key) !== false) {
                    $dkimValid = true;
                    break;
                }
            }
        }

        // Check SPF record
        $spfValid = false;
        foreach ($txtRecords as $record) {
            if (isset($record['txt']) && strpos($record['txt'], 'v=spf1') !== false) {
                $spfValid = true;
                break;
            }
        }

        if (! $dkimValid || ! $spfValid) {
            $domain->update(['status' => 'failed']);

            return false;
        }

        return false;
    }

    private function generateDKIMKeys(): array
    {
        $config = [
            'private_key_bits' => 2048,
            'private_key_type' => OPENSSL_KEYTYPE_RSA,
        ];

        $privateKey = openssl_pkey_new($config);
        openssl_pkey_export($privateKey, $privateKeyString);

        $publicKeyDetails = openssl_pkey_get_details($privateKey);
        $publicKeyString = $publicKeyDetails['key'];

        // Extract public key in DKIM format
        preg_match('/-----BEGIN PUBLIC KEY-----(.+)-----END PUBLIC KEY-----/s', $publicKeyString, $matches);
        $publicKeyDKIM = trim(str_replace(["\n", "\r"], '', $matches[1] ?? ''));

        return [
            'private' => $privateKeyString,
            'public' => $publicKeyDKIM,
        ];
    }

    private function generateDNSRecords(Domain $domain): void
    {
        // SPF Record
        $serverIP = gethostbyname(gethostname());
        $spfRecord = "v=spf1 ip4:{$serverIP} ~all";

        // DMARC Record
        $dmarcRecord = "v=DMARC1; p=quarantine; rua=mailto:dmarc@{$domain->domain}";

        $domain->update([
            'spf_record' => $spfRecord,
            'dmarc_record' => $dmarcRecord,
        ]);
    }

    public function getDNSInstructions(Domain $domain): array
    {
        return [
            'verification' => [
                'type' => 'TXT',
                'name' => '@',
                'value' => 'openmailer-verification'.$domain->verification_token,
            ],
            'spf' => [
                'type' => 'TXT',
                'name' => '@',
                'value' => $domain->spf_record,
            ],
            'dkim' => [
                'type' => 'TXT',
                'name' => $domain->dkim_selector.'._domainkey',
                'value' => 'v=DKIM1; k=rsa; p='.$domain->dkim_public_key,
            ],
            'dmarc' => [
                'type' => 'TXT',
                'name' => '_dmarc',
                'value' => $domain->dmarc_record,
            ],
        ];
    }
}
