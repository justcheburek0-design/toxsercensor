package ru.vadim.toxsercensor.filter;

import java.util.HashMap;
import java.util.Map;

/**
 * 1-to-1 character normalizer for catching leet-speak / homoglyph bypass.
 * <p>
 * Maps latin letters that look like cyrillic (and vice versa) so that
 * 「xуйня」 matches root 「хуй」. Only 1-to-1 substitutions — string length
 * is preserved, making position-based masking trivial.
 */
public final class LeetNormalizer {
    private static final Map<Character, Character> MAP = new HashMap<>();

    static {
        // Latin → Cyrillic homoglyphs
        map('a', 'а');   // latin a → cyrillic а
        map('A', 'А');
        map('b', 'в');   // b → в
        map('B', 'В');
        map('c', 'с');   // c → с
        map('C', 'С');
        map('e', 'е');   // e → е
        map('E', 'Е');
        map('H', 'Н');   // H → Н
        map('i', 'і');   // i → і (ukrainian)
        map('I', 'І');
        map('k', 'к');   // k → к
        map('K', 'К');
        map('M', 'М');
        map('m', 'м');
        map('o', 'о');
        map('O', 'О');
        map('p', 'р');   // p → р
        map('P', 'Р');
        map('r', 'г');   // r → г (mirror)
        map('T', 'Т');
        map('x', 'х');   // x → х
        map('X', 'Х');
        map('y', 'у');   // y → у
        map('Y', 'У');

        // Digit → Cyrillic
        map('0', 'о');
        map('1', 'л');   // 1 → л (как "л" в leet)
        map('3', 'з');   // 3 → з
        map('4', 'ч');   // 4 → ч (как "cha")
        map('6', 'б');   // 6 → б
        map('9', 'г');   // 9 → г

        // Latin lookalikes (2-char special cases skipped — 1-to-1 only)
        // 'u' is NOT mapped: "xyu" should still catch "хуй" because x→х, y→у
        // 'u' → 'и' would be wrong, 'u' → 'ю' would be wrong, leave it

        // Cyrillic → Latin (reverse, for completeness — rarely needed)
        // These fire when someone writes pure cyrillic with homoglyph intent
        // Already covered by the latin→cyrillic mapping above
    }

    private static void map(char from, char to) {
        MAP.put(from, to);
    }

    /**
     * Normalize a string: replace homoglyph characters with their standard
     * cyrillic equivalents. Non-mapped characters stay unchanged.
     * String length is preserved (1-to-1 mapping).
     */
    public static String normalize(String input) {
        if (input == null || input.isEmpty()) return input;
        char[] chars = input.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            Character replacement = MAP.get(chars[i]);
            if (replacement != null) {
                chars[i] = replacement;
            }
        }
        return new String(chars);
    }

    private LeetNormalizer() {
    }
}
