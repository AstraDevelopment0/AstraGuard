package dev.astra.guard.utils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class MiniMessageLegacyParser {

    private static final Map<String, String> COLOR_CODES = new HashMap<>();

    static {
        COLOR_CODES.put("black", "§0");
        COLOR_CODES.put("dark_blue", "§1");
        COLOR_CODES.put("darkgreen", "§2");
        COLOR_CODES.put("dark_green", "§2");
        COLOR_CODES.put("dark_aqua", "§3");
        COLOR_CODES.put("dark_red", "§4");
        COLOR_CODES.put("dark_purple", "§5");
        COLOR_CODES.put("gold", "§6");
        COLOR_CODES.put("gray", "§7");
        COLOR_CODES.put("dark_gray", "§8");
        COLOR_CODES.put("blue", "§9");
        COLOR_CODES.put("green", "§a");
        COLOR_CODES.put("aqua", "§b");
        COLOR_CODES.put("red", "§c");
        COLOR_CODES.put("light_purple", "§d");
        COLOR_CODES.put("yellow", "§e");
        COLOR_CODES.put("white", "§f");
        COLOR_CODES.put("bold", "§l");
        COLOR_CODES.put("italic", "§o");
        COLOR_CODES.put("underline", "§n");
        COLOR_CODES.put("strikethrough", "§m");
        COLOR_CODES.put("reset", "§r");
    }

    private MiniMessageLegacyParser() {}

    public static String parse(String input) {
        if (input == null || input.isEmpty()) return "";

        StringBuilder result = new StringBuilder(input.length() + 16);
        int len = input.length();

        for (int i = 0; i < len; i++) {
            char c = input.charAt(i);

            if (c == '<') {
                int close = input.indexOf('>', i);
                if (close > i + 1) {
                    String tag = input.substring(i + 1, close).toLowerCase(Locale.ROOT);

                    String code = COLOR_CODES.get(tag);
                    if (code != null) {
                        result.append(code);
                        i = close;
                        continue;
                    }

                    if (tag.startsWith("&#") && tag.length() == 8 &&
                            tag.substring(2).matches("[0-9a-fA-F]{6}")) {
                        result.append('§').append('x');
                        for (char hexChar : tag.substring(2).toCharArray()) {
                            result.append('§').append(Character.toLowerCase(hexChar));
                        }
                        i = close;
                        continue;
                    }
                }
            }

            if (c == '&' && i + 1 < len && isColorCodeChar(input.charAt(i + 1))) {
                result.append('§').append(Character.toLowerCase(input.charAt(i + 1)));
                i++;
                continue;
            }

            result.append(c);
        }

        if (!result.toString().endsWith("§r")) {
            result.append("§r");
        }
        return result.toString();
    }

    private static boolean isColorCodeChar(char c) {
        return (c >= '0' && c <= '9') ||
                (c >= 'a' && c <= 'f') ||
                (c >= 'k' && c <= 'o') ||
                (c >= 'A' && c <= 'F') ||
                (c >= 'K' && c <= 'O') ||
                c == 'r' || c == 'R';
    }
}
