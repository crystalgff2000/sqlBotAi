package com.sqlbot.service.rag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextTokenizer {

    private static final Pattern ENGLISH = Pattern.compile("[a-zA-Z0-9_]{2,}");
    private static final Pattern CHINESE = Pattern.compile("[\\u4e00-\\u9fff]+");

    private TextTokenizer() {}

    public static List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        Set<String> tokens = new HashSet<>();
        String lower = text.toLowerCase(Locale.ROOT);

        Matcher englishMatcher = ENGLISH.matcher(lower);
        while (englishMatcher.find()) {
            tokens.add(englishMatcher.group());
        }

        Matcher chineseMatcher = CHINESE.matcher(text);
        while (chineseMatcher.find()) {
            String segment = chineseMatcher.group();
            if (segment.length() <= 4) {
                tokens.add(segment);
            } else {
                tokens.add(segment);
                for (int i = 0; i <= segment.length() - 2; i++) {
                    tokens.add(segment.substring(i, i + 2));
                }
            }
        }

        List<String> result = new ArrayList<>();
        for (String token : tokens) {
            if (token.length() >= 2) {
                result.add(token);
            }
        }
        return result;
    }
}
