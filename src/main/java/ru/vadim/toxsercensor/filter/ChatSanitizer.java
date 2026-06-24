package ru.vadim.toxsercensor.filter;

import ru.vadim.toxsercensor.config.FilterConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class ChatSanitizer {
    private ChatSanitizer() {
    }

    public static String sanitize(String message, FilterConfig config) {
        if (message == null || message.isEmpty() || config == null) {
            return message;
        }

        // Phase 1: roots (prefix match, full mask) — processes first so derivatives are caught
        String filtered = applyRootMask(message, config.roots);
        // Phase 2: banwords (word-boundary exact, full mask)
        filtered = applyFullMask(filtered, config.banwords);
        // Phase 3: partial (word-boundary exact, partial mask)
        filtered = applyPartialMask(filtered, config.partialWords);
        return filtered;
    }

    private static boolean isLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'а' && c <= 'я') || c == 'ё' || c == 'Ё';
    }

    /** Check left word boundary: start-of-string or non-letter. */
    private static boolean isLeftBoundary(String lower, int index) {
        return index == 0 || !isLetter(lower.charAt(index - 1));
    }

    /** Check right word boundary: end-of-string or non-letter. */
    private static boolean isRightBoundary(String lower, int end) {
        return end >= lower.length() || !isLetter(lower.charAt(end));
    }

    /**
     * Roots: prefix matching. The root must start at a left word boundary.
     * It matches ANY word that begins with the root (right boundary not checked).
     * E.g., root "хуй" matches "хуйня", "хуйнуть", "хуйло" but not "мухуй".
     */
    private static String applyRootMask(String message, List<String> roots) {
        if (roots == null || roots.isEmpty()) {
            return message;
        }

        String lowerMessage = message.toLowerCase(Locale.ROOT);
        char[] output = message.toCharArray();
        List<int[]> ranges = new ArrayList<>();

        roots.stream()
                .filter(r -> r != null && !r.isBlank())
                .map(String::trim)
                .map(r -> r.toLowerCase(Locale.ROOT))
                .sorted(Comparator.comparingInt(String::length).reversed())
                .forEach(root -> {
                    int fromIndex = 0;
                    while (fromIndex < lowerMessage.length()) {
                        int matchIndex = lowerMessage.indexOf(root, fromIndex);
                        if (matchIndex < 0) break;

                        int matchEnd = matchIndex + root.length();

                        if (isLeftBoundary(lowerMessage, matchIndex)) {
                            // Find how far the word extends (for full masking of the word)
                            int wordEnd = matchEnd;
                            while (wordEnd < lowerMessage.length() && isLetter(lowerMessage.charAt(wordEnd))) {
                                wordEnd++;
                            }

                            // Check overlap
                            boolean overlaps = false;
                            for (int[] r : ranges) {
                                if (matchIndex < r[1] && wordEnd > r[0]) {
                                    overlaps = true;
                                    break;
                                }
                            }
                            if (!overlaps) {
                                ranges.add(new int[]{matchIndex, wordEnd});
                            }
                        }
                        fromIndex = matchEnd;
                    }
                });

        for (int[] range : ranges) {
            for (int i = range[0]; i < range[1]; i++) {
                output[i] = '*';
            }
        }
        return new String(output);
    }

    private static String applyFullMask(String message, List<String> entries) {
        return applyMask(message, entries, true);
    }

    private static String applyPartialMask(String message, List<String> entries) {
        return applyMask(message, entries, false);
    }

    /**
     * Banwords / partial: word-boundary matching.
     * The pattern must be a complete word (bounded by non-letters on both sides).
     */
    private static String applyMask(String message, List<String> entries, boolean fullMask) {
        if (entries == null || entries.isEmpty()) {
            return message;
        }

        String lowerMessage = message.toLowerCase(Locale.ROOT);
        char[] output = message.toCharArray();
        List<int[]> ranges = new ArrayList<>();

        entries.stream()
                .filter(entry -> entry != null && !entry.isBlank())
                .map(String::trim)
                .map(entry -> entry.toLowerCase(Locale.ROOT))
                .sorted(Comparator.comparingInt(String::length).reversed())
                .forEach(pattern -> {
                    int fromIndex = 0;
                    while (fromIndex < lowerMessage.length()) {
                        int matchIndex = lowerMessage.indexOf(pattern, fromIndex);
                        if (matchIndex < 0) break;

                        int matchEnd = matchIndex + pattern.length();

                        if (isLeftBoundary(lowerMessage, matchIndex)
                                && isRightBoundary(lowerMessage, matchEnd)) {

                            boolean overlaps = false;
                            for (int[] r : ranges) {
                                if (matchIndex < r[1] && matchEnd > r[0]) {
                                    overlaps = true;
                                    break;
                                }
                            }
                            if (!overlaps) {
                                ranges.add(new int[]{matchIndex, matchEnd});
                            }
                        }
                        fromIndex = matchEnd;
                    }
                });

        for (int[] range : ranges) {
            if (fullMask) {
                for (int i = range[0]; i < range[1]; i++) {
                    output[i] = '*';
                }
            } else {
                for (int i = range[0] + 1; i < range[1] - 1 && i < output.length; i++) {
                    output[i] = '*';
                }
            }
        }
        return new String(output);
    }
}
