package com.erickoeckel.tasktimer;

import android.net.Uri;

public final class AvatarUrl {
    private static String enc(String s) { return Uri.encode(s == null ? "" : s); }
    private static boolean ne(String s) { return s != null && !s.isEmpty(); }

    public static String build(AvatarConfig c, int size) {
        StringBuilder u = new StringBuilder("https://api.dicebear.com/7.x/avataaars/svg");
        u.append("?size=").append(size);
        u.append("&seed=").append(enc(c.seed));

        if (c.background) {
            u.append("&transparent=false&backgroundType=solid&backgroundColor=")
                    .append(enc(c.backgroundColor));
        } else {
            u.append("&transparent=true");
        }

        if (ne(c.hair))      u.append("&top=").append(enc(c.hair));
        if (ne(c.hairColor)) u.append("&hairColor=").append(enc(c.hairColor));

        if (ne(c.eyes))      u.append("&eyes=").append(enc(c.eyes));

        if (!ne(c.glasses) || "blank".equalsIgnoreCase(c.glasses)) {
            u.append("&accessoriesProbability=0");
        } else {
            u.append("&accessories=").append(enc(c.glasses))
                    .append("&accessoriesProbability=100");
        }

        if (ne(c.skinColor)) u.append("&skinColor=").append(enc(c.skinColor));

        if (ne(c.clothes))       u.append("&clothes=").append(enc(c.clothes));
        if (ne(c.clothesColor))  u.append("&clothesColor=").append(enc(c.clothesColor));

        return u.toString();
    }
}
