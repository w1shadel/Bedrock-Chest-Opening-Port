package com.maxwell.bedrockchestopeningport.client.DataUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public final class LegacyStringParser {
    private LegacyStringParser() {
    }

    public static Component parse(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        MutableComponent first = null;
        MutableComponent result = null;
        StringBuilder currentText = new StringBuilder();
        Style currentStyle = Style.EMPTY;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                if (!currentText.isEmpty()) {
                    MutableComponent segment =
                            Component.literal(currentText.toString()).withStyle(currentStyle);
                    currentText.setLength(0);
                    if (first == null) {
                        first = segment;
                    } else {
                        if (result == null) {
                            result = Component.empty().append(first);
                        }
                        result.append(segment);
                    }
                }
                char code = text.charAt(i + 1);
                ChatFormatting formatting = ChatFormatting.getByCode(code);
                if (formatting == ChatFormatting.RESET) {
                    currentStyle = Style.EMPTY;
                } else if (formatting != null) {
                    if (TextColor.fromLegacyFormat(formatting) != null) {
                        currentStyle = currentStyle
                                .withColor(formatting)
                                .withBold(currentStyle.isBold())
                                .withItalic(currentStyle.isItalic())
                                .withUnderlined(currentStyle.isUnderlined())
                                .withStrikethrough(currentStyle.isStrikethrough())
                                .withObfuscated(currentStyle.isObfuscated());
                    } else {
                        currentStyle = currentStyle.applyFormat(formatting);
                    }
                }
                i++;
            } else {
                currentText.append(c);
            }
        }
        if (!currentText.isEmpty()) {
            MutableComponent segment =
                    Component.literal(currentText.toString()).withStyle(currentStyle);
            if (first == null) {
                first = segment;
            } else {
                if (result == null) {
                    result = Component.empty().append(first);
                }
                result.append(segment);
            }
        }
        if (result != null) {
            return result;
        }
        return first != null ? first : Component.empty();
    }
}