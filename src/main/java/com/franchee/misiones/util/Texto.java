package com.franchee.misiones.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Locale;

public final class Texto {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private Texto() {
    }

    /** Para nombres y lore de items: sin cursiva por defecto. */
    public static Component color(String texto) {
        return LEGACY.deserialize(texto == null ? "" : texto).decoration(TextDecoration.ITALIC, false);
    }

    /** Para chat y titulos de menu. */
    public static Component chat(String texto) {
        return LEGACY.deserialize(texto == null ? "" : texto);
    }

    /** OAK_LOG -> "Oak Log" */
    public static String bonito(String nombreEnum) {
        String[] partes = nombreEnum.toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : partes) {
            if (p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }
}
