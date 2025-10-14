<?php

namespace App\Services;

use App\Models\Campaign;
use App\Models\Contact;
use App\Models\EmailQueue;
use App\Models\SmtpSetting;
use App\Models\UnsubscribeToken;
use Illuminate\Support\Str;
use PHPMailer\PHPMailer\PHPMailer;

class EmailService {
    public function sendEmail(EmailQueue $emailQueue): bool {
        try {
            $campaign = $emailQueue->campaign;
            $contact = $emailQueue->contact;
            $domain = $emailQueue->domain;

            $mail = new PHPMailer(true);

            /** Get SMTP settings */
            $smtpSettings = $this->getSmtpSettings($campaign->user_id, $domain?->id);

            /** SMTP Configuration */
            $mail->isSMTP();
            $mail->Host = $smtpSettings->host;
            $mail->SMTPAuth = true;
            $mail->Username = $smtpSettings->username;
            $mail->Password = $smtpSettings->password;
            $mail->SMTPSecure = $smtpSettings->encryption === 'ssl' ? PHPMailer::ENCRYPTION_SMTPS : PHPMailer::ENCRYPTION_STARTTLS;
            $mail->Port = $smtpSettings->port;

            /** DKIM Signing (if domain is verified) */
            if ($domain && $domain->is_verified && $domain->dkim_private_key) {
                $mail->DKIM_domain = $domain->domain;
                $mail->DKIM_private_string = $domain->dkim_private_string;
                $mail->DKIM_selector = $domain->dkim_selector;
                $mail->DKIM_passphrase = '';
                $mail->DKIM_identity = $campaign->from_email;
            }

            /** Email Headers */
            $mail->setFrom($campaign->from_email, $campaign->from_name);
            $mail->addAddress($contact->email, $contact->first_name . ' ' . $contact->last_name);

            if ($campaign->reply_to) {
                $mail->addReplyTo($campaign->reply_to);
            }

            /** Tracking Headers */
            $mail->addCustomHeader('X-Campaign-Id', $campaign->id);
            $mail->addCustomHeader('X-Contact-ID', $contact->id);
            $mail->addCustomHeader('X-Tracking-ID', $emailQueue->tracking_id);

            /** List-Unsubscribe Header (RFC 2369) */
            $unsubscribeUrl = route('unsubscribe', ['token' => $this->generateUnsubscribeToken($contact, $campaign)]);
            $mail->addCustomHeader('List-Unsubscribe', '<' . $unsubscribeUrl . '>');
            $mail->addCustomHeader('List-Unsubscribe-Post', 'List-Unsubscribe=One-Click');

            /** Email Content */
            $mail->isHTML(true);
            $mail->Subject = $this->personalize($campaign->subject, $contact);
            $mail->Body = $this->personalize($campaign->html_content, $contact);
            $mail->Body = $this->addTrackingPixel($mail->Body, $emailQueue->tracking_id);
            $mail->Body = $this->rewriteLinks($mail->Body, $campaign, $emailQueue->tracking_id);

            $mail->send();

            $emailQueue->update([
                'status' => 'sent',
                'sent_at' => now(),
            ]);

            return true;
        } catch (\Exception $e) {
            $emailQueue->update([
                'status' => 'failed',
                'attempts' => $emailQueue->attempts + 1,
                'error_message' => $e->getMessage(),
            ]);
            return false;
        }
    }

    private function getSmtpSettings($userId, $domainId = null): SmtpSetting {
        if ($domainId) {
            $settings = SmtpSetting::where('user_id', $userId)
                ->where('domain_id', $domainId)
                ->where('is_active', true)
                ->first();

            if ($settings) { return $settings; }
        }

        return SmtpSetting::where('user_id', $userId)
            ->where('is_default', true)
            ->where('is_active', true)
            ->firstOrFail();
    }

    private function personalize(string $content, Contact $contact): string {
        $replacements = [
            '{{first_name}}' => $contact->first_name ?? '',
            '{{last_name}}' => $contact->last_name ?? '',
            '{{email}}' => $contact->email,
            '{{full_name}}' => trim(($contact->first_name ?? '') . ' ' . ($contact->last_name ?? '')),
        ];

        if ($contact->custom_fields) {
            foreach ($contact->custom_fields as $key => $value) {
                $replacements['{{' . $key . '}}'] = $value;
            }
        }

        return str_replace(array_keys($replacements), array_values($replacements), $content);
    }

    private function addTrackingPixel(string $html, string $trackingId): string {
        $trackingUrl = route('track.open', ['id' => $trackingId]);
        $pixel = '<img src="' . $trackingUrl . '" width="1" height="1" alt="" />';

        return str_replace('</body', $pixel . '</body>', $html);
    }

    private function rewriteLinks(string $html, Campaign $campaign, string $trackingId): string {
        preg_match_all('/<a\s+(?:[^>]*?\s+)?href="([^"]*)"/', $html, $matches);
        if (empty($matches[1])) { return $html; }

        foreach ($matches[1] as $url) {
            if (filter_var($url, FILTER_VALIDATE_URL)) {
                $shortCode = $this->getOrCreateShortCode($campaign, $url);
                $trackingUrl = route('track.click', [
                    'code' => $shortCode,
                    'tid' => $trackingId,
                ]);
                $html = str_replace('href="' . $url . '"', 'href="' . $trackingUrl . '"', $html);
            }
        }
        return $html;
    }

    private function getOrCreateShortCode(Campaign $campaign, string $url): string {
        $link = $campaign->emailLinks()->where('original_url', $url)->first();
        if (!$link) {
            $link = $campaign->emailLinks()->create([
                'original_url' => $url,
                'short_code' => Str::random(8),
            ]);
        }

        return $link->short_code;
    }

    private function generateUnsubscribeToken(Contact $contact, Campaign $campaign): string {
        $token = Str::random(64);

        UnsubscribeToken::create([
            'contact_id' => $contact->id,
            'campaign_id' => $campaign->id,
            'token' => $token,
            'expires_at' => now()->addDays(30),
        ]);
        return $token;
    }
}
