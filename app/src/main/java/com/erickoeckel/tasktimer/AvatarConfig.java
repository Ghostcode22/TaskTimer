package com.erickoeckel.tasktimer;

import androidx.annotation.Nullable;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.HashMap;
import java.util.Map;

public class AvatarConfig {
    public String seed = "tasktimer";

    public String top = "Hat";
    public String hairColor = "Brown";

    public String eyes = "Default";
    public String eyebrows = "Default";
    public String mouth = "Default";

    public String accessories = "Blank";
    public boolean accessoriesOn = false;

    public String facialHair = "Blank";
    public String facialHairColor = "Brown";
    public boolean facialHairOn = false;

    public String skinColor  = "Brown";
    public boolean background = false;

    public String clothing = "GraphicShirt";
    public String clothesColor = "Black";
    public String clothingGraphic = "Skull";

    @SuppressWarnings("unchecked")
    public static AvatarConfig from(@Nullable DocumentSnapshot snap) {
        AvatarConfig c = new AvatarConfig();
        if (snap != null && snap.exists()) {
            Object raw = snap.get("avatarConfig");
            if (raw instanceof Map) {
                Map<String, Object> m = (Map<String, Object>) raw;
                c.seed = (String) m.getOrDefault("seed", c.seed);

                c.top = (String) m.getOrDefault("top", c.top);
                c.hairColor = (String) m.getOrDefault("hairColor", c.hairColor);

                c.eyes = (String) m.getOrDefault("eyes", c.eyes);
                c.eyebrows = (String) m.getOrDefault("eyebrows", c.eyebrows);
                c.mouth = (String) m.getOrDefault("mouth", c.mouth);

                c.accessories = (String) m.getOrDefault("accessories", c.accessories);
                Object accOn = m.get("accessoriesOn");
                if (accOn instanceof Boolean) c.accessoriesOn = (Boolean) accOn;

                c.facialHair = (String) m.getOrDefault("facialHair", c.facialHair);
                c.facialHairColor = (String) m.getOrDefault("facialHairColor", c.facialHairColor);
                Object fhOn = m.get("facialHairOn");
                if (fhOn instanceof Boolean) c.facialHairOn = (Boolean) fhOn;

                c.skinColor = (String) m.getOrDefault("skinColor", c.skinColor);

                Object bg = m.get("background");
                if (bg instanceof Boolean) c.background = (Boolean) bg;

                c.clothing = (String) m.getOrDefault("clothing", c.clothing);
                c.clothesColor = (String) m.getOrDefault("clothesColor", c.clothesColor);
                c.clothingGraphic = (String) m.getOrDefault("clothingGraphic", c.clothingGraphic);
            }
        }
        return c;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("seed", seed);
        m.put("top", top);
        m.put("hairColor", hairColor);

        m.put("eyes", eyes);
        m.put("eyebrows", eyebrows);
        m.put("mouth", mouth);

        m.put("accessories", accessories);
        m.put("accessoriesOn", accessoriesOn);

        m.put("facialHair", facialHair);
        m.put("facialHairColor", facialHairColor);
        m.put("facialHairOn", facialHairOn);

        m.put("skinColor", skinColor);

        m.put("background", background);

        m.put("clothing", clothing);
        m.put("clothesColor", clothesColor);
        m.put("clothingGraphic", clothingGraphic);

        return m;
    }

    public static AvatarConfig copyOf(@Nullable AvatarConfig src) {
        AvatarConfig c = new AvatarConfig();
        if (src == null) return c;

        c.seed = src.seed;

        c.top = src.top;
        c.hairColor = src.hairColor;

        c.eyes = src.eyes;
        c.eyebrows = src.eyebrows;
        c.mouth = src.mouth;

        c.accessories = src.accessories;
        c.accessoriesOn = src.accessoriesOn;

        c.facialHair = src.facialHair;
        c.facialHairColor = src.facialHairColor;
        c.facialHairOn = src.facialHairOn;

        c.skinColor = src.skinColor;

        c.background = src.background;

        c.clothing = src.clothing;
        c.clothesColor = src.clothesColor;
        c.clothingGraphic = src.clothingGraphic;

        return c;
    }

}
