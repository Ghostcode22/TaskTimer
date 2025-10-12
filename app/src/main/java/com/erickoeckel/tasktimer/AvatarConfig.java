package com.erickoeckel.tasktimer;

import androidx.annotation.Nullable;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.HashMap;
import java.util.Map;

public class AvatarConfig {
    public String seed = "tasktimer";

    public String top = "shortHairShortRound";
    public String hairColor = "2f2f2f";
    public String hatColor  = "3d3d3d";

    public String eyes = "default";
    public String eyebrows = "default";
    public String mouth = "default";

    public String accessories = "blank";
    public String accessoriesColor = "000000";
    public boolean accessoriesOn = false;

    public String facialHair = "moustacheFancy";
    public String facialHairColor = "2f2f2f";
    public boolean facialHairOn = false;

    public String skinColor  = "f2dbb1";
    public boolean background = false;
    public String backgroundColor = "0b5394";

    public String clothing = "shirtCrewNeck";
    public String clothesColor = "8e7cc3";
    public String clothingGraphic = "skull";

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
                c.hatColor = (String) m.getOrDefault("hatColor", c.hatColor);

                c.eyes = (String) m.getOrDefault("eyes", c.eyes);
                c.eyebrows = (String) m.getOrDefault("eyebrows", c.eyebrows);
                c.mouth = (String) m.getOrDefault("mouth", c.mouth);

                c.accessories = (String) m.getOrDefault("accessories", c.accessories);
                c.accessoriesColor = (String) m.getOrDefault("accessoriesColor", c.accessoriesColor);
                Object accOn = m.get("accessoriesOn");
                if (accOn instanceof Boolean) c.accessoriesOn = (Boolean) accOn;

                c.facialHair = (String) m.getOrDefault("facialHair", c.facialHair);
                c.facialHairColor = (String) m.getOrDefault("facialHairColor", c.facialHairColor);
                Object fhOn = m.get("facialHairOn");
                if (fhOn instanceof Boolean) c.facialHairOn = (Boolean) fhOn;

                c.skinColor = (String) m.getOrDefault("skinColor", c.skinColor);

                Object bg = m.get("background");
                if (bg instanceof Boolean) c.background = (Boolean) bg;
                c.backgroundColor = (String) m.getOrDefault("backgroundColor", c.backgroundColor);

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
        m.put("hatColor", hatColor);

        m.put("eyes", eyes);
        m.put("eyebrows", eyebrows);
        m.put("mouth", mouth);

        m.put("accessories", accessories);
        m.put("accessoriesColor", accessoriesColor);
        m.put("accessoriesOn", accessoriesOn);

        m.put("facialHair", facialHair);
        m.put("facialHairColor", facialHairColor);
        m.put("facialHairOn", facialHairOn);

        m.put("skinColor", skinColor);

        m.put("background", background);
        m.put("backgroundColor", backgroundColor);

        m.put("clothing", clothing);
        m.put("clothesColor", clothesColor);
        m.put("clothingGraphic", clothingGraphic);

        return m;
    }
}
