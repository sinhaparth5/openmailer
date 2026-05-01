package com.openmailer.openmailer.service.contact;

import com.openmailer.openmailer.model.Contact;
import com.openmailer.openmailer.model.Segment;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class SegmentEvaluationService {

    public List<Contact> filterContacts(List<Contact> contacts, Segment segment) {
        if (segment == null || segment.getConditions() == null || segment.getConditions().isEmpty()) {
            return contacts;
        }
        return contacts.stream()
            .filter(contact -> matches(contact, segment.getConditions()))
            .toList();
    }

    public boolean matches(Contact contact, Map<String, Object> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }

        if (isRule(conditions)) {
            return matchesRule(contact, conditions);
        }

        if (conditions.containsKey("rules")) {
            String operator = stringValue(conditions.get("operator"));
            return matchesGroup(contact, operator, toRuleMaps(conditions.get("rules")));
        }

        if (conditions.containsKey("and")) {
            return matchesGroup(contact, "AND", toRuleMaps(conditions.get("and")));
        }
        if (conditions.containsKey("or")) {
            return matchesGroup(contact, "OR", toRuleMaps(conditions.get("or")));
        }
        if (conditions.containsKey("not")) {
            Object notRule = conditions.get("not");
            if (notRule instanceof Map<?, ?> map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> cast = (Map<String, Object>) map;
                return !matches(contact, cast);
            }
        }

        // Fallback: treat each top-level key/value as equality checks joined by AND.
        for (Map.Entry<String, Object> entry : conditions.entrySet()) {
            if (!matchesRule(contact, Map.of(
                "field", entry.getKey(),
                "operator", "equals",
                "value", entry.getValue()
            ))) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesGroup(Contact contact, String operator, List<Map<String, Object>> rules) {
        String normalizedOperator = operator == null ? "AND" : operator.trim().toUpperCase(Locale.ROOT);
        if (rules.isEmpty()) {
            return true;
        }
        if ("OR".equals(normalizedOperator)) {
            return rules.stream().anyMatch(rule -> matches(contact, rule));
        }
        return rules.stream().allMatch(rule -> matches(contact, rule));
    }

    private boolean matchesRule(Contact contact, Map<String, Object> rule) {
        String field = stringValue(rule.get("field"));
        String operator = normalizeOperator(stringValue(rule.get("operator")));
        Object expected = rule.get("value");
        Object actual = resolveField(contact, field);

        return switch (operator) {
            case "equals" -> compareEquals(actual, expected);
            case "not_equals" -> !compareEquals(actual, expected);
            case "contains" -> contains(actual, expected);
            case "not_contains" -> !contains(actual, expected);
            case "starts_with" -> startsWith(actual, expected);
            case "ends_with" -> endsWith(actual, expected);
            case "in" -> inList(actual, expected);
            case "not_in" -> !inList(actual, expected);
            case "greater_than" -> compareOrdered(actual, expected) > 0;
            case "greater_than_or_equal", "gte" -> compareOrdered(actual, expected) >= 0;
            case "less_than" -> compareOrdered(actual, expected) < 0;
            case "less_than_or_equal", "lte" -> compareOrdered(actual, expected) <= 0;
            case "before" -> compareOrdered(actual, expected) < 0;
            case "after" -> compareOrdered(actual, expected) > 0;
            case "is_true" -> Boolean.TRUE.equals(asBoolean(actual));
            case "is_false" -> Boolean.FALSE.equals(asBoolean(actual));
            case "is_null" -> actual == null;
            case "not_null" -> actual != null;
            default -> compareEquals(actual, expected);
        };
    }

    private Object resolveField(Contact contact, String field) {
        if (field == null || field.isBlank()) {
            return null;
        }

        String normalized = field.trim()
            .replace("custom_fields.", "customFields.")
            .replace("first_name", "firstName")
            .replace("last_name", "lastName")
            .replace("email_verified", "emailVerified")
            .replace("gdpr_consent", "gdprConsent")
            .replace("created_at", "createdAt")
            .replace("updated_at", "updatedAt")
            .replace("subscribed_at", "subscribedAt")
            .replace("confirmed_at", "confirmedAt")
            .replace("unsubscribed_at", "unsubscribedAt")
            .replace("bounce_count", "bounceCount")
            .replace("complaint_count", "complaintCount")
            .replace("bounce_type", "bounceType");

        return switch (normalized) {
            case "email" -> contact.getEmail();
            case "firstName" -> contact.getFirstName();
            case "lastName" -> contact.getLastName();
            case "status" -> contact.getStatus();
            case "emailVerified" -> contact.getEmailVerified();
            case "source" -> contact.getSource();
            case "notes" -> contact.getNotes();
            case "gdprConsent" -> contact.getGdprConsent();
            case "createdAt" -> contact.getCreatedAt();
            case "updatedAt" -> contact.getUpdatedAt();
            case "subscribedAt" -> contact.getSubscribedAt();
            case "confirmedAt" -> contact.getConfirmedAt();
            case "unsubscribedAt" -> contact.getUnsubscribedAt();
            case "bounceCount" -> contact.getBounceCount();
            case "complaintCount" -> contact.getComplaintCount();
            case "bounceType" -> contact.getBounceType();
            case "tags" -> contact.getTags();
            default -> {
                if (normalized.startsWith("customFields.")) {
                    String key = normalized.substring("customFields.".length());
                    yield contact.getCustomFields() != null ? contact.getCustomFields().get(key) : null;
                }
                yield null;
            }
        };
    }

    private boolean compareEquals(Object actual, Object expected) {
        if (actual == null) {
            return expected == null;
        }
        if (actual instanceof Collection<?> collection) {
            return collection.stream().anyMatch(item -> compareEquals(item, expected));
        }
        if (actual instanceof Object[] array) {
            for (Object item : array) {
                if (compareEquals(item, expected)) {
                    return true;
                }
            }
            return false;
        }
        if (actual instanceof Boolean || expected instanceof Boolean) {
            return Objects.equals(asBoolean(actual), asBoolean(expected));
        }
        BigDecimal actualNumber = asNumber(actual);
        BigDecimal expectedNumber = asNumber(expected);
        if (actualNumber != null && expectedNumber != null) {
            return actualNumber.compareTo(expectedNumber) == 0;
        }
        LocalDateTime actualDate = asDateTime(actual);
        LocalDateTime expectedDate = asDateTime(expected);
        if (actualDate != null && expectedDate != null) {
            return actualDate.isEqual(expectedDate);
        }
        return String.valueOf(actual).equalsIgnoreCase(String.valueOf(expected));
    }

    private boolean contains(Object actual, Object expected) {
        if (actual == null || expected == null) {
            return false;
        }
        if (actual instanceof Collection<?> collection) {
            return collection.stream().anyMatch(item -> compareEquals(item, expected));
        }
        if (actual instanceof Object[] array) {
            for (Object item : array) {
                if (compareEquals(item, expected)) {
                    return true;
                }
            }
            return false;
        }
        return String.valueOf(actual).toLowerCase(Locale.ROOT)
            .contains(String.valueOf(expected).toLowerCase(Locale.ROOT));
    }

    private boolean startsWith(Object actual, Object expected) {
        return actual != null
            && expected != null
            && String.valueOf(actual).toLowerCase(Locale.ROOT)
                .startsWith(String.valueOf(expected).toLowerCase(Locale.ROOT));
    }

    private boolean endsWith(Object actual, Object expected) {
        return actual != null
            && expected != null
            && String.valueOf(actual).toLowerCase(Locale.ROOT)
                .endsWith(String.valueOf(expected).toLowerCase(Locale.ROOT));
    }

    private boolean inList(Object actual, Object expected) {
        if (expected instanceof Collection<?> collection) {
            return collection.stream().anyMatch(item -> compareEquals(actual, item));
        }
        if (expected instanceof Object[] array) {
            for (Object item : array) {
                if (compareEquals(actual, item)) {
                    return true;
                }
            }
        }
        return compareEquals(actual, expected);
    }

    private int compareOrdered(Object actual, Object expected) {
        BigDecimal actualNumber = asNumber(actual);
        BigDecimal expectedNumber = asNumber(expected);
        if (actualNumber != null && expectedNumber != null) {
            return actualNumber.compareTo(expectedNumber);
        }
        LocalDateTime actualDate = asDateTime(actual);
        LocalDateTime expectedDate = asDateTime(expected);
        if (actualDate != null && expectedDate != null) {
            return actualDate.compareTo(expectedDate);
        }
        String actualString = actual == null ? "" : String.valueOf(actual).toLowerCase(Locale.ROOT);
        String expectedString = expected == null ? "" : String.valueOf(expected).toLowerCase(Locale.ROOT);
        return actualString.compareTo(expectedString);
    }

    private BigDecimal asNumber(Object value) {
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        if (value instanceof String string && !string.isBlank()) {
            try {
                return new BigDecimal(string.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Boolean asBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String string && !string.isBlank()) {
            return Boolean.parseBoolean(string.trim());
        }
        return null;
    }

    private LocalDateTime asDateTime(Object value) {
        if (value instanceof LocalDateTime dateTime) {
            return dateTime;
        }
        if (value instanceof String string && !string.isBlank()) {
            try {
                return LocalDateTime.parse(string.trim());
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
        return null;
    }

    private boolean isRule(Map<String, Object> conditions) {
        return conditions.containsKey("field");
    }

    private List<Map<String, Object>> toRuleMaps(Object rawRules) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (rawRules instanceof Collection<?> collection) {
            for (Object rawRule : collection) {
                if (rawRule instanceof Map<?, ?> map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> cast = (Map<String, Object>) map;
                    result.add(cast);
                }
            }
        }
        return result;
    }

    private String normalizeOperator(String operator) {
        if (operator == null || operator.isBlank()) {
            return "equals";
        }
        return operator.trim()
            .toLowerCase(Locale.ROOT)
            .replace(" ", "_")
            .replace("-", "_");
    }

    private String stringValue(Object value) {
        return value != null ? String.valueOf(value) : null;
    }
}
