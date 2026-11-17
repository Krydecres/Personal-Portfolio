package com.portfolio.common.util;

import java.text.Normalizer;
import java.util.Locale;

/** Builds URL-safe slugs; handles Vietnamese diacritics ("Xin chào Đà Nẵng" -> "xin-chao-da-nang"). */
public final class Slugs {

    private Slugs() {}

    public static String slugify(String input) {
        if (input == null) {
            return "";
        }
        String s = input.replace('đ', 'd').replace('Đ', 'D');
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        s = s.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        if (s.length() > 120) {
            s = s.substring(0, 120).replaceAll("-+$", "");
        }
        return s;
    }

    /** Trims and converts blank strings to null so optional fields are stored consistently. */
    public static String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
