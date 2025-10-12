package com.erickoeckel.tasktimer;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

final class DiceBearUtils {
    private DiceBearUtils(){}

    private static final Map<String, String> NAMED_TO_HEX = new HashMap<String, String>() {{
        put("black", "000000");
        put("white", "FFFFFF");
        put("gray",  "808080");
        put("grey",  "808080");
        put("brown", "795548");
        put("browndark", "4E342E");
        put("auburn", "9C3D2B");
        put("blonde", "F7E27E");
        put("blondegolden", "F4D03F");
        put("pastelpink", "F8BBD0");
        put("c0c0c0", "C0C0C0");
        put("999999", "999999");
        put("c00000", "C00000");
        put("8d5524", "8D5524");
        put("0b5394", "0B5394");
        put("2f2f2f", "2F2F2F");
        put("3d3d3d", "3D3D3D");
        put("f2dbb1", "F2DBB1");
        put("8e7cc3", "8E7CC3");
    }};


    private static final Map<String, String> GRAPHIC_CANON = new HashMap<String, String>() {{
        // keep values exactly as DiceBear expects
        put("bat", "bat");
        put("skull", "skull");
        put("skulloutline", "skullOutline"); // note camelCase in the VALUE
        put("diamond", "diamond");
        put("pizza", "pizza");
        put("resist", "resist");
        put("vue", "vue");
        put("gatsby", "gatsby");
        put("deer", "deer");
        put("cumbia", "cumbia");
        put("hola", "hola"); // default that shows when API falls back
    }};

    /** Case-insensitive mapping to the exact clothingGraphic slug DiceBear accepts. Returns null if unknown. */
    static String normalizeClothingGraphic(String input) {
        if (input == null) return null;
        String k = input.trim().toLowerCase(Locale.ROOT);
        return GRAPHIC_CANON.get(k); // returns canonical (e.g., skullOutline) or null
    }


    /** Returns "transparent" or a 6-digit HEX (uppercase) without '#', or null if invalid. */
    static String normalizeColor(String input) {
        if (input == null) return null;
        String v = input.trim();
        if (v.isEmpty()) return null;

        if ("transparent".equalsIgnoreCase(v)) return "transparent";

        if (v.startsWith("#")) v = v.substring(1);

        String key = v.toLowerCase(Locale.ROOT);
        if (NAMED_TO_HEX.containsKey(key)) v = NAMED_TO_HEX.get(key).toUpperCase(Locale.ROOT);

        if (v.matches("^[A-Fa-f0-9]{6}$")) return v.toUpperCase(Locale.ROOT);
        return null;
    }

    /** DO NOT change case for enums; DiceBear slugs are case-sensitive (camelCase). */
    static String normalizeEnum(String input) {
        return input == null ? null : input.trim();
    }

    static String enc(String v) { return v == null ? null : v.replace(" ", "%20"); }
}
