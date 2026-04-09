package com.nonxedy.nonchat.util.core.colors;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Color;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.md_5.bungee.api.ChatColor;

/**
 * Simple LRU cache implementation.
 */
class LRUCache<K, V> {

    private final Map<K, V> cache;

    public LRUCache(int maxSize) {
        this.cache = Collections.synchronizedMap(
            new LinkedHashMap<K, V>(maxSize, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Entry<K, V> eldest) {
                    return size() > maxSize;
                }
            }
        );
    }

    public V get(K key) {
        return cache.get(key);
    }

    public void put(K key, V value) {
        cache.put(key, value);
    }
}

/**
 * Utility for parsing legacy colors, hex colors and full MiniMessage into Adventure Components.
 */
public class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private static final Pattern LEGACY_COLOR_PATTERN = Pattern.compile("(?i)&[0-9A-FK-OR]");

    private static final Pattern SECTION_COLOR_PATTERN = Pattern.compile("(?i)§[0-9A-FK-OR]");

    // &x&R&G&B&R&G&B  (Essentials / BungeeCord ampersand hex format)
    private static final Pattern AMPERSAND_HEX_PATTERN = Pattern.compile(
        "(?i)&x&([0-9a-fA-F])&([0-9a-fA-F])&([0-9a-fA-F])&([0-9a-fA-F])&([0-9a-fA-F])&([0-9a-fA-F])"
    );

    // §x§R§G§B§R§G§B  (BungeeCord section hex format, produced by parseColor)
    private static final Pattern BUNGEE_HEX_PATTERN = Pattern.compile(
        "§x§([0-9a-fA-F])§([0-9a-fA-F])§([0-9a-fA-F])§([0-9a-fA-F])§([0-9a-fA-F])§([0-9a-fA-F])"
    );

    /**
     * Detects tags like:
     * <red>, </red>, <click:run_command:/seed>, <hover:show_text:'text'>, <#FFFFFF>, etc.
     */
    private static final Pattern MINIMESSAGE_TAG_PATTERN =
            Pattern.compile("</?[a-zA-Z][a-zA-Z0-9_:-]*(?::[^<>\\r\\n]*)?>|<#[0-9a-fA-F]{6}>");

    private static final Pattern GRADIENT_PATTERN = Pattern.compile("(?i)<gradient:[^>]+>");

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final LRUCache<String, String> COLOR_CACHE = new LRUCache<>(1000);

    private static final Map<String, Component> COMPONENT_CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * Converts legacy & codes and &#RRGGBB into section-coded string.
     * This is only for old string-based paths.
     *
     * Format: &#RRGGBB becomes §x§R§G§B§R§G§B (BungeeCord hex format)
     */
    public static String parseColor(String message) {
        if (message == null) return "";

        String cached = COLOR_CACHE.get(message);
        if (cached != null) return cached;

        //  Convert &x&R&G&B&R&G&B -> §x§R§G§B§R§G§B
        Matcher ampMatcher = AMPERSAND_HEX_PATTERN.matcher(message);
        StringBuffer ampBuffer = new StringBuffer(message.length());
        while (ampMatcher.find()) {
            String bungee = "§x"
                    + "§" + ampMatcher.group(1)
                    + "§" + ampMatcher.group(2)
                    + "§" + ampMatcher.group(3)
                    + "§" + ampMatcher.group(4)
                    + "§" + ampMatcher.group(5)
                    + "§" + ampMatcher.group(6);
            ampMatcher.appendReplacement(ampBuffer, Matcher.quoteReplacement(bungee));
        }
        ampMatcher.appendTail(ampBuffer);
        String preProcessed = ampBuffer.toString();

        //Translate standard &x codes
        String withTranslated = ChatColor.translateAlternateColorCodes('&', preProcessed);

        // Convert &#RRGGBB -> §x§R§G§B§R§G§B
        Matcher hexMatcher = HEX_PATTERN.matcher(withTranslated);
        StringBuilder buffer = new StringBuilder(withTranslated.length() + 32);
        while (hexMatcher.find()) {
            String hex = hexMatcher.group(1);
            String bungeeHex = "§x"
                    + "§" + hex.charAt(0)
                    + "§" + hex.charAt(1)
                    + "§" + hex.charAt(2)
                    + "§" + hex.charAt(3)
                    + "§" + hex.charAt(4)
                    + "§" + hex.charAt(5);
            hexMatcher.appendReplacement(buffer, Matcher.quoteReplacement(bungeeHex));
        }
        hexMatcher.appendTail(buffer);

        String result = buffer.toString();
        COLOR_CACHE.put(message, result);
        return result;
    }

    public static Component parseComponentCached(String message) {
        if (message == null || message.isEmpty()) return Component.empty();
        return COMPONENT_CACHE.computeIfAbsent(message, ColorUtil::parseComponent);
    }

    /**
     * Main parser for user/chat text.
     * Supports:
     * - &a, &l, etc.
     * - §a, §l, etc.
     * - &#RRGGBB
     * - &x&R&G&B&R&G&B
     * - <#RRGGBB>
     * - full MiniMessage tags like <click>, <hover>, <gradient>, etc.
     */
    public static Component parseComponent(String message) {
        if (message == null || message.isEmpty()) return Component.text("");

        try {
            String normalized = normalizeToMiniMessage(message);
            if (containsMiniMessageTags(normalized) || containsLegacyCodes(message)) {
                return MINI_MESSAGE.deserialize(normalized);
            }
            return Component.text(message);
        } catch (Exception e) {
            try {
                String legacyMessage = parseColor(message);
                return LegacyComponentSerializer.legacySection().deserialize(legacyMessage);
            } catch (Exception ignored) {
                return Component.text(stripAllColors(message));
            }
        }
    }

    /**
     * If player has no color permission, strip all formatting and interactive tags.
     */
    public static Component parseComponent(String message, Player player) {
        if (player != null && !player.hasPermission("nonchat.color")) {
            return Component.text(stripAllColors(message));
        }
        return parseComponent(message);
    }

    public static Component parseMiniMessageComponent(String message) {
        return parseComponent(message);
    }

    public static Component parseMiniMessageComponent(String message, Player player) {
        return parseComponent(message, player);
    }

    /**
     * Safe formatting method for chat templates.
     *
     * format example:
     * "<gray>[<gold>%player%</gold>]</gray> %message%"
     *
     * player name is inserted as plain/unparsed text
     * message is inserted as parsed MiniMessage text
     */
    public static Component parseChatFormat(String format, Player sender, String message) {
        String playerName = sender != null ? sender.getName() : "";
        boolean allowFormatting = sender == null || sender.hasPermission("nonchat.color");
        return parseChatFormat(format, playerName, message, allowFormatting);
    }

    public static Component parseChatFormat(String format, String playerName, String message) {
        return parseChatFormat(format, playerName, message, true);
    }

    public static Component parseChatFormat(String format, String playerName, String message, boolean allowFormatting) {
        if (format == null || format.isEmpty()) {
            return allowFormatting
                    ? parseComponent(message)
                    : Component.text(stripAllColors(message));
        }

        String safePlayerName = playerName == null ? "" : playerName;
        String safeMessage    = message    == null ? "" : message;

        try {
            String normalizedFormat = normalizeToMiniMessage(format)
                    .replace("%player%", "<player>")
                    .replace("%message%", "<message>");

            TagResolver resolver = allowFormatting
                    ? TagResolver.resolver(
                            Placeholder.unparsed("player", safePlayerName),
                            Placeholder.parsed("message", normalizeToMiniMessage(safeMessage))
                      )
                    : TagResolver.resolver(
                            Placeholder.unparsed("player", safePlayerName),
                            Placeholder.unparsed("message", stripAllColors(safeMessage))
                      );

            return MINI_MESSAGE.deserialize(normalizedFormat, resolver);
        } catch (Exception e) {
            String fallback = format
                    .replace("%player%", safePlayerName)
                    .replace("%message%", allowFormatting ? safeMessage : stripAllColors(safeMessage));
            return parseComponent(fallback);
        }
    }

    /**
     * For config strings or any server-defined format lines.
     */
    public static Component parseConfigComponent(String message) {
        return parseComponent(message);
    }

    /**
     * For legacy-only string paths.
     */
    public static String processMessageWithPermission(String message, Player player) {
        if (player != null && !player.hasPermission("nonchat.color")) {
            return stripAllColors(message);
        }
        return parseColor(message);
    }

    /**
     * Preferred permission-aware component path.
     */
    public static Component processComponentWithPermission(String message, Player player) {
        return parseComponent(message, player);
    }

    public static boolean containsMiniMessageTags(String message) {
        if (message == null || message.isEmpty()) return false;
        return MINIMESSAGE_TAG_PATTERN.matcher(message).find();
    }

    public static boolean containsGradient(String message) {
        if (message == null || message.isEmpty()) return false;
        return GRADIENT_PATTERN.matcher(message).find();
    }

    public static boolean hasColorCodes(String message) {
        if (message == null || message.isEmpty()) return false;
        return AMPERSAND_HEX_PATTERN.matcher(message).find()
                || HEX_PATTERN.matcher(message).find()
                || LEGACY_COLOR_PATTERN.matcher(message).find()
                || SECTION_COLOR_PATTERN.matcher(message).find()
                || MINIMESSAGE_TAG_PATTERN.matcher(message).find();
    }

    /**
     * Removes all formatting and returns visible plain text only.
     */
    public static String stripAllColors(String message) {
        if (message == null || message.isEmpty()) return "";

        try {
            return PlainTextComponentSerializer.plainText().serialize(parseComponent(message));
        } catch (Exception e) {
            String result = message;
            result = AMPERSAND_HEX_PATTERN.matcher(result).replaceAll("");
            result = HEX_PATTERN.matcher(result).replaceAll("");
            result = LEGACY_COLOR_PATTERN.matcher(result).replaceAll("");
            result = SECTION_COLOR_PATTERN.matcher(result).replaceAll("");
            result = result.replaceAll("<[^>]+>", "");
            return result;
        }
    }

    /**
     * Converts hex string into Bukkit Color.
     */
    public static Color parseHexColor(String hexColor) {
        if (hexColor == null || hexColor.isEmpty()) return Color.BLACK;

        try {
            String hex = hexColor.startsWith("#") ? hexColor.substring(1) : hexColor;

            if (hex.length() == 3) {
                hex = "" + hex.charAt(0) + hex.charAt(0)
                        + hex.charAt(1) + hex.charAt(1)
                        + hex.charAt(2) + hex.charAt(2);
            }

            if (hex.length() != 6) return Color.BLACK;

            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);

            return Color.fromRGB(r, g, b);
        } catch (IllegalArgumentException e) {
            return Color.BLACK;
        }
    }

    /**
     * Converts:
     * &x&R&G&B&R&G&B  -> <reset><#RRGGBB>
     * §x§R§G§B§R§G§B  -> <reset><#RRGGBB>
     * &#RRGGBB        -> <reset><#RRGGBB>
     * &a / §a         -> <reset><green>, etc.
     * while not touching already-existing MiniMessage tags.
     */
    private static String normalizeToMiniMessage(String message) {
        if (message == null || message.isEmpty()) return "";

        String result = message;

        // &x&R&G&B&R&G&B -> <reset><#RRGGBB>  (must run before safelyConvertLegacyColors)
        Matcher ampMatcher = AMPERSAND_HEX_PATTERN.matcher(result);
        StringBuffer ampBuffer = new StringBuffer(result.length());

        while (ampMatcher.find()) {
            String hex = ampMatcher.group(1) + ampMatcher.group(2)
                    + ampMatcher.group(3) + ampMatcher.group(4)
                    + ampMatcher.group(5) + ampMatcher.group(6);
            ampMatcher.appendReplacement(ampBuffer, Matcher.quoteReplacement("<reset><#" + hex + ">"));
        }

        ampMatcher.appendTail(ampBuffer);
        result = ampBuffer.toString();

        // §x§R§G§B§R§G§B -> <reset><#RRGGBB>
        Matcher bungeeMatcher = BUNGEE_HEX_PATTERN.matcher(result);
        StringBuffer bungeeBuffer = new StringBuffer(result.length());

        while (bungeeMatcher.find()) {
            String hex = bungeeMatcher.group(1) + bungeeMatcher.group(2)
                    + bungeeMatcher.group(3) + bungeeMatcher.group(4)
                    + bungeeMatcher.group(5) + bungeeMatcher.group(6);
            bungeeMatcher.appendReplacement(bungeeBuffer, Matcher.quoteReplacement("<reset><#" + hex + ">"));
        }

        bungeeMatcher.appendTail(bungeeBuffer);
        result = bungeeBuffer.toString();

        // &#FFFFFF -> <reset><#FFFFFF>
        Matcher hexMatcher = HEX_PATTERN.matcher(result);
        StringBuffer hexBuffer = new StringBuffer(result.length() + 32);

        while (hexMatcher.find()) {
            hexMatcher.appendReplacement(hexBuffer, "<reset><#" + hexMatcher.group(1) + ">");
        }

        hexMatcher.appendTail(hexBuffer);
        result = hexBuffer.toString();

        // &a / §a -> <reset><green>, etc.
        result = safelyConvertLegacyColors(result);

        return result;
    }

private static String safelyConvertLegacyColors(String message) {
        if (message == null || message.isEmpty()) return message;

        StringBuilder result = new StringBuilder(message.length() + 16);
        int i   = 0;
        int len = message.length();

        while (i < len) {
            char current = message.charAt(i);

            // Do not modify already existing MiniMessage tags.
            if (current == '<') {
                int endTag = message.indexOf('>', i);
                if (endTag != -1) {
                    // Process content inside the tag for color codes
                    String tagContent = message.substring(i + 1, endTag);
                    String processedContent = safelyConvertLegacyColors(tagContent);
                    result.append('<').append(processedContent).append('>');
                    i = endTag + 1;
                    continue;
                }
            }

            // Convert both &x and §x
            if (i < len - 1 && (current == '&' || current == '§')) {
                char code = message.charAt(i + 1);
                String miniMessageTag = convertLegacyCodeToMiniMessage(code);
                if (miniMessageTag != null) {
                    result.append(miniMessageTag);
                    i += 2;
                    continue;
                }
            }

            result.append(current);
            i++;
        }

        return result.toString();
    }

    private static String convertLegacyCodeToMiniMessage(char code) {
        // Color codes reset all active formatting (bold, italic, etc.) before applying
        // the new color, matching classic Minecraft legacy behavior.
        switch (code) {
            case '0': return "<reset><black>";
            case '1': return "<reset><dark_blue>";
            case '2': return "<reset><dark_green>";
            case '3': return "<reset><dark_aqua>";
            case '4': return "<reset><dark_red>";
            case '5': return "<reset><dark_purple>";
            case '6': return "<reset><gold>";
            case '7': return "<reset><gray>";
            case '8': return "<reset><dark_gray>";
            case '9': return "<reset><blue>";
            case 'a': case 'A': return "<reset><green>";
            case 'b': case 'B': return "<reset><aqua>";
            case 'c': case 'C': return "<reset><red>";
            case 'd': case 'D': return "<reset><light_purple>";
            case 'e': case 'E': return "<reset><yellow>";
            case 'f': case 'F': return "<reset><white>";
            case 'k': case 'K': return "<obfuscated>";
            case 'l': case 'L': return "<bold>";
            case 'm': case 'M': return "<strikethrough>";
            case 'n': case 'N': return "<underlined>";
            case 'o': case 'O': return "<italic>";
            case 'r': case 'R': return "<reset>";
            default:            return null;
        }
    }

    private static boolean containsLegacyCodes(String message) {
        if (message == null || message.isEmpty()) return false;
        return AMPERSAND_HEX_PATTERN.matcher(message).find()
                || HEX_PATTERN.matcher(message).find()
                || LEGACY_COLOR_PATTERN.matcher(message).find()
                || SECTION_COLOR_PATTERN.matcher(message).find();
    }
}