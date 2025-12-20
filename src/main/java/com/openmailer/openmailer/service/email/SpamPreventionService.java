package com.openmailer.openmailer.service.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for analyzing email content and preventing spam.
 * Checks for spam trigger words, excessive capitalization, link density, etc.
 */
@Service
public class SpamPreventionService {

    private static final Logger log = LoggerFactory.getLogger(SpamPreventionService.class);

    // Common spam trigger words (case-insensitive)
    private static final Set<String> SPAM_TRIGGER_WORDS = Set.of(
            "free", "winner", "congratulations", "urgent", "limited time",
            "act now", "click here", "buy now", "order now", "special promotion",
            "lowest price", "cheap", "discount", "guarantee", "risk-free",
            "money back", "no obligation", "cash", "prize", "claim",
            "exclusive deal", "incredible deal", "once in a lifetime",
            "earn money", "make money", "work from home", "be your own boss",
            "weight loss", "lose weight", "miracle", "amazing",
            "dear friend", "hello friend", "you have been selected"
    );

    // Spam phrases (case-insensitive)
    private static final Set<String> SPAM_PHRASES = Set.of(
            "click here now",
            "act immediately",
            "limited time offer",
            "don't delete",
            "this is not spam",
            "100% free",
            "no credit card",
            "no strings attached",
            "billion dollars",
            "million dollars"
    );

    /**
     * Analyzes email content and returns a spam score with recommendations.
     *
     * @param subject the email subject line
     * @param htmlBody the HTML body content
     * @param textBody the plain text body content
     * @return spam analysis result
     */
    public SpamAnalysisResult analyzeContent(String subject, String htmlBody, String textBody) {
        log.debug("Analyzing email content for spam indicators");

        SpamAnalysisResult result = new SpamAnalysisResult();
        List<String> warnings = new ArrayList<>();
        int score = 0;

        // Analyze subject line
        if (subject != null && !subject.isEmpty()) {
            score += analyzeSubjectLine(subject, warnings);
        }

        // Analyze body content
        String content = htmlBody != null ? htmlBody : (textBody != null ? textBody : "");
        if (!content.isEmpty()) {
            score += analyzeBodyContent(content, warnings);
            score += analyzeCapitalization(content, warnings);
            score += analyzeLinkDensity(content, warnings);
            score += analyzeSpecialCharacters(content, warnings);
        }

        result.setSpamScore(score);
        result.setWarnings(warnings);
        result.setRecommendations(generateRecommendations(warnings));

        // Determine risk level
        if (score >= 15) {
            result.setRiskLevel("HIGH");
        } else if (score >= 8) {
            result.setRiskLevel("MEDIUM");
        } else {
            result.setRiskLevel("LOW");
        }

        log.info("Spam analysis complete. Score: {}, Risk: {}, Warnings: {}",
                score, result.getRiskLevel(), warnings.size());

        return result;
    }

    /**
     * Analyzes the subject line for spam indicators.
     */
    private int analyzeSubjectLine(String subject, List<String> warnings) {
        int score = 0;
        String lowerSubject = subject.toLowerCase();

        // Check for all caps subject
        if (subject.equals(subject.toUpperCase()) && subject.length() > 5) {
            warnings.add("Subject line is in ALL CAPS");
            score += 3;
        }

        // Check for excessive punctuation
        long exclamationCount = subject.chars().filter(ch -> ch == '!').count();
        if (exclamationCount > 1) {
            warnings.add("Excessive exclamation marks in subject (" + exclamationCount + ")");
            score += 2;
        }

        // Check for spam trigger words in subject
        for (String trigger : SPAM_TRIGGER_WORDS) {
            if (lowerSubject.contains(trigger)) {
                warnings.add("Spam trigger word in subject: '" + trigger + "'");
                score += 2;
            }
        }

        // Check for "RE:" or "FW:" when not actually a reply
        if (lowerSubject.startsWith("re:") || lowerSubject.startsWith("fw:")) {
            warnings.add("Subject contains 'RE:' or 'FW:' which may appear deceptive");
            score += 1;
        }

        return score;
    }

    /**
     * Analyzes body content for spam trigger words and phrases.
     */
    private int analyzeBodyContent(String content, List<String> warnings) {
        int score = 0;
        String lowerContent = content.toLowerCase();

        // Check for spam trigger words
        int triggerWordCount = 0;
        for (String trigger : SPAM_TRIGGER_WORDS) {
            if (lowerContent.contains(trigger)) {
                triggerWordCount++;
            }
        }

        if (triggerWordCount > 5) {
            warnings.add("High concentration of spam trigger words (" + triggerWordCount + " found)");
            score += 5;
        } else if (triggerWordCount > 2) {
            warnings.add("Multiple spam trigger words found (" + triggerWordCount + ")");
            score += 2;
        }

        // Check for spam phrases
        for (String phrase : SPAM_PHRASES) {
            if (lowerContent.contains(phrase)) {
                warnings.add("Spam phrase detected: '" + phrase + "'");
                score += 3;
            }
        }

        return score;
    }

    /**
     * Analyzes capitalization patterns.
     */
    private int analyzeCapitalization(String content, List<String> warnings) {
        int score = 0;

        // Remove HTML tags for accurate analysis
        String plainText = content.replaceAll("<[^>]*>", "");

        if (plainText.length() > 50) {
            long upperCount = plainText.chars().filter(Character::isUpperCase).count();
            long letterCount = plainText.chars().filter(Character::isLetter).count();

            if (letterCount > 0) {
                double upperRatio = (double) upperCount / letterCount;

                if (upperRatio > 0.5) {
                    warnings.add("Excessive capitalization (over 50% uppercase)");
                    score += 4;
                } else if (upperRatio > 0.3) {
                    warnings.add("High capitalization ratio (over 30% uppercase)");
                    score += 2;
                }
            }
        }

        return score;
    }

    /**
     * Analyzes link density in the content.
     */
    private int analyzeLinkDensity(String content, List<String> warnings) {
        int score = 0;

        // Count links
        Pattern linkPattern = Pattern.compile("<a[^>]+href=[\"']([^\"']+)[\"'][^>]*>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = linkPattern.matcher(content);
        int linkCount = 0;

        while (matcher.find()) {
            linkCount++;
        }

        // Remove HTML tags for word count
        String plainText = content.replaceAll("<[^>]*>", "");
        int wordCount = plainText.split("\\s+").length;

        if (wordCount > 0 && linkCount > 0) {
            double linkRatio = (double) linkCount / wordCount;

            if (linkRatio > 0.1) {
                warnings.add("Very high link density (" + linkCount + " links in " + wordCount + " words)");
                score += 3;
            } else if (linkRatio > 0.05) {
                warnings.add("High link density (" + linkCount + " links)");
                score += 1;
            }
        }

        // Check for shortened URLs (common in spam)
        if (content.contains("bit.ly") || content.contains("tinyurl.com") ||
            content.contains("t.co") || content.contains("goo.gl")) {
            warnings.add("Contains shortened URLs which may be flagged");
            score += 2;
        }

        return score;
    }

    /**
     * Analyzes special characters and symbols.
     */
    private int analyzeSpecialCharacters(String content, List<String> warnings) {
        int score = 0;

        // Count excessive punctuation
        long exclamationCount = content.chars().filter(ch -> ch == '!').count();
        if (exclamationCount > 5) {
            warnings.add("Excessive exclamation marks (" + exclamationCount + ")");
            score += 2;
        }

        // Check for dollar signs (common in spam)
        long dollarCount = content.chars().filter(ch -> ch == '$').count();
        if (dollarCount > 3) {
            warnings.add("Multiple dollar signs detected (" + dollarCount + ")");
            score += 1;
        }

        return score;
    }

    /**
     * Generates recommendations based on warnings.
     */
    private List<String> generateRecommendations(List<String> warnings) {
        List<String> recommendations = new ArrayList<>();

        for (String warning : warnings) {
            if (warning.contains("ALL CAPS")) {
                recommendations.add("Use mixed case in subject line");
            } else if (warning.contains("exclamation")) {
                recommendations.add("Reduce exclamation marks to 1 or remove entirely");
            } else if (warning.contains("trigger word")) {
                recommendations.add("Rephrase to avoid spam trigger words");
            } else if (warning.contains("capitalization")) {
                recommendations.add("Use normal capitalization (avoid excessive caps)");
            } else if (warning.contains("link density")) {
                recommendations.add("Reduce number of links or add more text content");
            } else if (warning.contains("shortened URL")) {
                recommendations.add("Use full URLs instead of link shorteners");
            }
        }

        // Remove duplicates
        return new ArrayList<>(new LinkedHashSet<>(recommendations));
    }

    /**
     * Quick check if content is likely spam (simple threshold).
     *
     * @param subject the email subject
     * @param body the email body
     * @return true if likely spam
     */
    public boolean isLikelySpam(String subject, String body) {
        SpamAnalysisResult result = analyzeContent(subject, body, null);
        return result.getSpamScore() >= 15;
    }

    /**
     * Result object for spam analysis.
     */
    public static class SpamAnalysisResult {
        private int spamScore;
        private String riskLevel; // LOW, MEDIUM, HIGH
        private List<String> warnings = new ArrayList<>();
        private List<String> recommendations = new ArrayList<>();

        public int getSpamScore() {
            return spamScore;
        }

        public void setSpamScore(int spamScore) {
            this.spamScore = spamScore;
        }

        public String getRiskLevel() {
            return riskLevel;
        }

        public void setRiskLevel(String riskLevel) {
            this.riskLevel = riskLevel;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public void setWarnings(List<String> warnings) {
            this.warnings = warnings;
        }

        public List<String> getRecommendations() {
            return recommendations;
        }

        public void setRecommendations(List<String> recommendations) {
            this.recommendations = recommendations;
        }
    }
}
