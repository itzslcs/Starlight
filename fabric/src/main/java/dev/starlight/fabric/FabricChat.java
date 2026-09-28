package dev.starlight.fabric;

import dev.starlight.core.chat.ChatLine;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Component <-> ChatLine glue for the chat mixin. Game thread only. */
public final class FabricChat {
    public static boolean reentrant;
    private static final ChatLine LINE = new ChatLine();
    /** The 16 legacy colours -> their '§' code. Built from vanilla's own mapping (26.2 dropped ChatFormatting.getByName/getChar). */
    private static final Map<TextColor, Character> LEGACY_CODES = new HashMap<TextColor, Character>();

    static {
        for (ChatFormatting f : ChatFormatting.values()) {
            TextColor c = TextColor.fromLegacyFormat(f);
            if (c != null) LEGACY_CODES.put(c, f.toString().charAt(1)); // toString() is "§" + code
        }
    }

    private FabricChat() {}

    public static ChatLine line(Component message) {
        return LINE.reset(message.getString(), legacy(message));
    }

    /** Prefix (timestamp), mention marker and repeat counter around the original component (styles kept). */
    public static Component decorate(Component message, ChatLine line) {
        MutableComponent out = Component.empty();
        if (line.highlight != 0) out.append(Component.literal("▍").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(line.highlight & 0xFFFFFF))));
        if (line.prefix != null) out.append(Component.literal(line.prefix));
        out.append(message);
        String suffix = line.repeatSuffix();
        if (suffix != null) out.append(Component.literal(suffix));
        return out;
    }

    /** Legacy '§' rendering of a component (colours that match the 16 legacy ones, plus decorations). */
    static String legacy(Component c) {
        final StringBuilder sb = new StringBuilder();
        c.visit((style, text) -> {
            TextColor color = style.getColor();
            Character code = color == null ? null : LEGACY_CODES.get(color);
            if (code != null) sb.append('§').append(code.charValue());
            if (style.isBold()) sb.append("§l");
            if (style.isItalic()) sb.append("§o");
            if (style.isUnderlined()) sb.append("§n");
            if (style.isStrikethrough()) sb.append("§m");
            if (style.isObfuscated()) sb.append("§k");
            sb.append(text);
            if (color != null || style.isBold() || style.isItalic() || style.isUnderlined() || style.isStrikethrough() || style.isObfuscated()) sb.append("§r");
            return Optional.empty();
        }, Style.EMPTY);
        return sb.toString();
    }
}
