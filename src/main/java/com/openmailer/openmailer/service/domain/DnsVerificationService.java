package com.openmailer.openmailer.service.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * Service for verifying DNS records for email domains.
 * Checks SPF, DKIM, and DMARC records.
 */
@Service
public class DnsVerificationService {

    private static final Logger log = LoggerFactory.getLogger(DnsVerificationService.class);

    /**
     * Verifies all DNS records for a domain (SPF, DKIM, DMARC).
     *
     * @param domainName the domain name to verify
     * @param dkimSelector the DKIM selector (e.g., "openmailer")
     * @param expectedDkimPublicKey the expected DKIM public key
     * @return DnsVerificationResult with verification status for each record type
     */
    public DnsVerificationResult verifyDomain(String domainName, String dkimSelector, String expectedDkimPublicKey) {
        log.info("Starting DNS verification for domain: {}", domainName);

        DnsVerificationResult result = new DnsVerificationResult();
        result.setDomainName(domainName);

        // Verify SPF record
        result.setSpfVerified(verifySpfRecord(domainName));

        // Verify DKIM record
        result.setDkimVerified(verifyDkimRecord(domainName, dkimSelector, expectedDkimPublicKey));

        // Verify DMARC record
        result.setDmarcVerified(verifyDmarcRecord(domainName));

        boolean allVerified = result.isSpfVerified() && result.isDkimVerified() && result.isDmarcVerified();
        result.setAllVerified(allVerified);

        log.info("DNS verification completed for {}: SPF={}, DKIM={}, DMARC={}",
                domainName, result.isSpfVerified(), result.isDkimVerified(), result.isDmarcVerified());

        return result;
    }

    /**
     * Verifies SPF record for the domain.
     * Checks if a valid SPF record exists in DNS.
     *
     * @param domainName the domain name
     * @return true if SPF record is found and valid
     */
    public boolean verifySpfRecord(String domainName) {
        try {
            List<String> txtRecords = getTxtRecords(domainName);

            // Check if any TXT record contains SPF information
            for (String record : txtRecords) {
                if (record.startsWith("v=spf1")) {
                    log.debug("Found SPF record for {}: {}", domainName, record);
                    // Additional validation could check if record includes openmailer.com
                    return record.contains("include:openmailer.com") ||
                           record.contains("include:_spf.openmailer.com");
                }
            }

            log.warn("No valid SPF record found for domain: {}", domainName);
            return false;

        } catch (Exception e) {
            log.error("Failed to verify SPF record for {}: {}", domainName, e.getMessage());
            return false;
        }
    }

    /**
     * Verifies DKIM record for the domain.
     * Checks if the DKIM public key in DNS matches the expected key.
     *
     * @param domainName the domain name
     * @param selector the DKIM selector
     * @param expectedPublicKey the expected DKIM public key
     * @return true if DKIM record matches
     */
    public boolean verifyDkimRecord(String domainName, String selector, String expectedPublicKey) {
        try {
            String dkimDomain = selector + "._domainkey." + domainName;
            List<String> txtRecords = getTxtRecords(dkimDomain);

            for (String record : txtRecords) {
                if (record.startsWith("v=DKIM1")) {
                    log.debug("Found DKIM record for {}: {}", dkimDomain, record);

                    // Extract public key from DKIM record
                    String publicKeyFromDns = extractDkimPublicKey(record);

                    if (publicKeyFromDns != null && publicKeyFromDns.equals(expectedPublicKey)) {
                        log.info("DKIM record verified successfully for {}", domainName);
                        return true;
                    } else {
                        log.warn("DKIM public key mismatch for {}", domainName);
                        return false;
                    }
                }
            }

            log.warn("No DKIM record found for {}", dkimDomain);
            return false;

        } catch (Exception e) {
            log.error("Failed to verify DKIM record for {}: {}", domainName, e.getMessage());
            return false;
        }
    }

    /**
     * Verifies DMARC record for the domain.
     * Checks if a valid DMARC policy exists.
     *
     * @param domainName the domain name
     * @return true if DMARC record is found and valid
     */
    public boolean verifyDmarcRecord(String domainName) {
        try {
            String dmarcDomain = "_dmarc." + domainName;
            List<String> txtRecords = getTxtRecords(dmarcDomain);

            for (String record : txtRecords) {
                if (record.startsWith("v=DMARC1")) {
                    log.debug("Found DMARC record for {}: {}", dmarcDomain, record);
                    // Basic validation - could add more checks for policy values
                    return record.contains("p=") && (
                           record.contains("p=none") ||
                           record.contains("p=quarantine") ||
                           record.contains("p=reject")
                    );
                }
            }

            log.warn("No DMARC record found for {}", dmarcDomain);
            return false;

        } catch (Exception e) {
            log.error("Failed to verify DMARC record for {}: {}", domainName, e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves TXT records for a domain using DNS lookup.
     *
     * @param domain the domain name
     * @return list of TXT records
     * @throws NamingException if DNS lookup fails
     */
    private List<String> getTxtRecords(String domain) throws NamingException {
        List<String> txtRecords = new ArrayList<>();

        Hashtable<String, String> env = new Hashtable<>();
        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");

        DirContext ctx = new InitialDirContext(env);
        Attributes attrs = ctx.getAttributes(domain, new String[]{"TXT"});
        Attribute txtAttr = attrs.get("TXT");

        if (txtAttr != null) {
            for (int i = 0; i < txtAttr.size(); i++) {
                String record = txtAttr.get(i).toString();
                // Remove quotes if present
                record = record.replaceAll("^\"|\"$", "");
                txtRecords.add(record);
            }
        }

        ctx.close();
        return txtRecords;
    }

    /**
     * Extracts the public key portion from a DKIM DNS record.
     *
     * @param dkimRecord the full DKIM DNS record
     * @return the public key, or null if not found
     */
    private String extractDkimPublicKey(String dkimRecord) {
        // DKIM records have format: v=DKIM1; k=rsa; p=<public_key>
        String[] parts = dkimRecord.split(";");
        for (String part : parts) {
            part = part.trim();
            if (part.startsWith("p=")) {
                return part.substring(2).trim();
            }
        }
        return null;
    }

    /**
     * Result object for DNS verification.
     */
    public static class DnsVerificationResult {
        private String domainName;
        private boolean spfVerified;
        private boolean dkimVerified;
        private boolean dmarcVerified;
        private boolean allVerified;

        public String getDomainName() {
            return domainName;
        }

        public void setDomainName(String domainName) {
            this.domainName = domainName;
        }

        public boolean isSpfVerified() {
            return spfVerified;
        }

        public void setSpfVerified(boolean spfVerified) {
            this.spfVerified = spfVerified;
        }

        public boolean isDkimVerified() {
            return dkimVerified;
        }

        public void setDkimVerified(boolean dkimVerified) {
            this.dkimVerified = dkimVerified;
        }

        public boolean isDmarcVerified() {
            return dmarcVerified;
        }

        public void setDmarcVerified(boolean dmarcVerified) {
            this.dmarcVerified = dmarcVerified;
        }

        public boolean isAllVerified() {
            return allVerified;
        }

        public void setAllVerified(boolean allVerified) {
            this.allVerified = allVerified;
        }
    }
}
