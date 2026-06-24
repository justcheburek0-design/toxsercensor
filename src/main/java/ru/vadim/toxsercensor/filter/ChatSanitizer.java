package ru.vadim.toxsercensor.filter;

import ru.vadim.toxsercensor.config.FilterConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChatSanitizer {
    private ChatSanitizer() {
    }

    /**
     * Filter a chat message: normalize leet-speak, match roots/banwords/partial/regex,
     * mask the original message at matching positions.
     */
    public static String sanitize(String message, FilterConfig config) {
        if (message == null || message.isEmpty() || config == null) {
            return message;
        }

        // Normalize for matching (leet-speak, homoglyphs)
        String matchMessage = LeetNormalizer.normalize(message);

        String display = message;
        String match = matchMessage;

        // Phase 0: regex roots (before standard roots)
        if (hasEntries(config.regexRoots)) {
            String[] res = applyRegexMask(display, match, config.regexRoots);
            display = res[0];
            match = res[1];
        }

        // Phase 1: standard roots (prefix match, entire-word mask)
        if (hasEntries(config.roots)) {
            String[] res = applyRootMask(display, match, config.roots);
            display = res[0];
            match = res[1];
        }

        // Phase 2: banwords (word-boundary exact, full mask)
        if (hasEntries(config.banwords)) {
            String[] res = applyFullMask(display, match, config.banwords);
            display = res[0];
            match = res[1];
        }

        // Phase 3: partial (word-boundary exact, first+last visible)
        if (hasEntries(config.partialWords)) {
            String[] res = applyPartialMask(display, match, config.partialWords);
            display = res[0];
            // match doesn't need updating after last phase
        }

        return display;
    }

    // -- Helpers --

    private static boolean hasEntries(List<?> list) {
        return list != null && !list.isEmpty();
    }

    // -- Letter boundary detection --

    private static boolean isLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'а' && c <= 'я') || c == 'ё' || c == 'Ё';
    }

    private static boolean isLeftBoundary(String lower, int index) {
        return index == 0 || !isLetter(lower.charAt(index - 1));
    }

    private static boolean isRightBoundary(String lower, int end) {
        return end >= lower.length() || !isLetter(lower.charAt(end));
    }

    // -- Regex roots (Phase 0) --

    private static String[] applyRegexMask(String display, String match, List<String> regexStrings) {
        if (regexStrings == null || regexStrings.isEmpty()) {
            return new String[]{display, match};
        }

        String lowerMatch = match.toLowerCase(Locale.ROOT);
        char[] displayOut = display.toCharArray();
        char[] matchOut = match.toCharArray();
        List<int[]> ranges = new ArrayList<>();

        for (String regexStr : regexStrings) {
            if (regexStr == null || regexStr.isBlank()) continue;
            try {
                Pattern pattern = Pattern.compile(regexStr, Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(lowerMatch);
                while (matcher.find()) {
                    int start = matcher.start();
                    int end = matcher.end();
                    if (start < 0 || end > lowerMatch.length()) continue;

                    // Check overlap
                    boolean overlaps = false;
                    for (int[] r : ranges) {
                        if (start < r[1] && end > r[0]) {
                            overlaps = true;
                            break;
                        }
                    }
                    if (!overlaps) {
                        ranges.add(new int[]{start, end});
                    }
                }
            } catch (Exception e) {
                // Skip invalid regex patterns silently
            }
        }

        for (int[] range : ranges) {
            for (int i = range[0]; i < range[1] && i < displayOut.length; i++) {
                displayOut[i] = '*';
                if (i < matchOut.length) matchOut[i] = '*';
            }
        }
        return new String[]{new String(displayOut), new String(matchOut)};
    }

    // -- Standard roots (Phase 1) --

    private static String[] applyRootMask(String display, String match, List<String> roots) {
        if (roots == null || roots.isEmpty()) {
            return new String[]{display, match};
        }

        String lowerMatch = match.toLowerCase(Locale.ROOT);
        char[] displayOut = display.toCharArray();
        char[] matchOut = match.toCharArray();
        List<int[]> ranges = new ArrayList<>();

        roots.stream()
                .filter(r -> r != null && !r.isBlank())
                .map(String::trim)
                .map(r -> r.toLowerCase(Locale.ROOT))
                .sorted(Comparator.comparingInt(String::length).reversed())
                .forEach(root -> {
                    int fromIndex = 0;
                    while (fromIndex < lowerMatch.length()) {
                        int matchIndex = lowerMatch.indexOf(root, fromIndex);
                        if (matchIndex < 0) return; // no more matches for this root

                        int matchEnd = matchIndex + root.length();

                        if (isLeftBoundary(lowerMatch, matchIndex)) {
                            // Find how far the word extends (for full masking of the word)
                            int wordEnd = matchEnd;
                            while (wordEnd < lowerMatch.length() && isLetter(lowerMatch.charAt(wordEnd))) {
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
            for (int i = range[0]; i < range[1] && i < displayOut.length; i++) {
                displayOut[i] = '*';
                if (i < matchOut.length) matchOut[i] = '*';
            }
        }
        return new String[]{new String(displayOut), new String(matchOut)};
    }

    // -- Banwords (Phase 2) and Partial (Phase 3) --

    private static String[] applyFullMask(String display, String match, List<String> entries) {
        return applyMask(display, match, entries, true);
    }

    private static String[] applyPartialMask(String display, String match, List<String> entries) {
        return applyMask(display, match, entries, false);
    }

    /**
     * Banwords / partial: word-boundary matching.
     * The pattern must be a complete word (bounded by non-letters on both sides).
     */
    private static String[] applyMask(String display, String match, List<String> entries, boolean fullMask) {
        if (entries == null || entries.isEmpty()) {
            return new String[]{display, match};
        }

        String lowerMatch = match.toLowerCase(Locale.ROOT);
        char[] displayOut = display.toCharArray();
        char[] matchOut = match.toCharArray();
        List<int[]> ranges = new ArrayList<>();

        entries.stream()
                .filter(entry -> entry != null && !entry.isBlank())
                .map(String::trim)
                .map(entry -> entry.toLowerCase(Locale.ROOT))
                .sorted(Comparator.comparingInt(String::length).reversed())
                .forEach(pattern -> {
                    int fromIndex = 0;
                    while (fromIndex < lowerMatch.length()) {
                        int matchIndex = lowerMatch.indexOf(pattern, fromIndex);
                        if (matchIndex < 0) return;

                        int matchEnd = matchIndex + pattern.length();

                        if (isLeftBoundary(lowerMatch, matchIndex)
                                && isRightBoundary(lowerMatch, matchEnd)) {

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
            int start = range[0];
            int end = range[1];
            if (fullMask) {
                for (int i = start; i < end && i < displayOut.length; i++) {
                    displayOut[i] = '*';
                    if (i < matchOut.length) matchOut[i] = '*';
                }
            } else {
                for (int i = start + 1; i < end - 1 && i < displayOut.length; i++) {
                    displayOut[i] = '*';
                    if (i < matchOut.length) matchOut[i] = '*';
                }
            }
        }
        return new String[]{new String(displayOut), new String(matchOut)};
    }
}
