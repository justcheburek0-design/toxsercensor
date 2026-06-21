package ru.vadim.toxsercensor.filter;

import ru.vadim.toxsercensor.config.FilterConfig;

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

        String filtered = applyFullMask(message, config.banwords);
        filtered = applyPartialMask(filtered, config.partialWords);
        return filtered;
    }

    private static String applyFullMask(String message, List<String> entries) {
        return applyMask(message, entries, true);
    }

    private static String applyPartialMask(String message, List<String> entries) {
        return applyMask(message, entries, false);
    }

    private static String applyMask(String message, List<String> entries, boolean fullMask) {
        if (entries == null || entries.isEmpty()) {
            return message;
        }

        String lowerMessage = message.toLowerCase(Locale.ROOT);
        char[] output = message.toCharArray();

        entries.stream()
                .filter(entry -> entry != null && !entry.isBlank())
                .map(String::trim)
                .map(entry -> entry.toLowerCase(Locale.ROOT))
                .sorted(Comparator.comparingInt(String::length).reversed())
                .forEach(pattern -> {
                    int fromIndex = 0;
                    while (fromIndex < lowerMessage.length()) {
                        int matchIndex = lowerMessage.indexOf(pattern, fromIndex);
                        if (matchIndex < 0) {
                            break;
                        }
                        maskRange(output, matchIndex, matchIndex + pattern.length(), fullMask);
                        fromIndex = matchIndex + Math.max(pattern.length(), 1);
                    }
                });

        return new String(output);
    }

    private static void maskRange(char[] output, int start, int end, boolean fullMask) {
        if (fullMask || end - start <= 2) {
            for (int i = start; i < end; i++) {
                output[i] = '*';
            }
            return;
        }

        int length = end - start;
        int prefix = length > 4 ? 2 : 1;
        int suffix = length > 4 ? 2 : 1;
        for (int i = start + prefix; i < end - suffix; i++) {
            output[i] = '*';
        }
    }
}

