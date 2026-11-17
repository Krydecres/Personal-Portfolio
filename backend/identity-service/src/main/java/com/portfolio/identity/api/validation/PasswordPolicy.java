package com.portfolio.identity.api.validation;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Single source of truth for the password rules. The same rules are published at
 * GET /api/auth/providers so the React form can show a live checklist.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    /** BCrypt only uses the first 72 bytes, so longer inputs are rejected instead of silently truncated. */
    public static final int MAX_BYTES = 72;

    private static final Pattern UPPER = Pattern.compile("\\p{Lu}");
    private static final Pattern LOWER = Pattern.compile("\\p{Ll}");
    private static final Pattern DIGIT = Pattern.compile("\\p{Nd}");
    private static final Pattern SPECIAL = Pattern.compile("[^\\p{L}\\p{Nd}\\s]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s");

    public record Rule(String id, String label, Predicate<String> test) {}

    public static final List<Rule> RULES = List.of(
            new Rule("length", "Ít nhất " + MIN_LENGTH + " ký tự",
                    p -> p.codePointCount(0, p.length()) >= MIN_LENGTH),
            new Rule("upper", "Có ít nhất 1 chữ hoa", p -> UPPER.matcher(p).find()),
            new Rule("lower", "Có ít nhất 1 chữ thường", p -> LOWER.matcher(p).find()),
            new Rule("digit", "Có ít nhất 1 chữ số", p -> DIGIT.matcher(p).find()),
            new Rule("special", "Có ít nhất 1 ký tự đặc biệt (!@#$%...)", p -> SPECIAL.matcher(p).find()),
            new Rule("nospace", "Không chứa khoảng trắng", p -> !WHITESPACE.matcher(p).find()),
            new Rule("maxBytes", "Không quá " + MAX_BYTES + " byte",
                    p -> p.getBytes(StandardCharsets.UTF_8).length <= MAX_BYTES)
    );

    private PasswordPolicy() {}

    /** @return labels of the rules the password breaks; empty when the password is acceptable. */
    public static List<String> violations(String password) {
        List<String> failed = new ArrayList<>();
        String p = password == null ? "" : password;
        for (Rule rule : RULES) {
            if (!rule.test().test(p)) {
                failed.add(rule.label());
            }
        }
        return failed;
    }
}
