package com.erickoeckel.tasktimer;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class AvatarEditorFragment extends Fragment {

    private static final String TAG = "Avatar";

    // ===== avataaars.io ENUMS (PascalCase, exact values expected by the API) =====

    // topType (hair + headwear)
    private static final String[] TOPS = {
            "NoHair",
            "Eyepatch",
            "Hat",
            "Hijab",
            "Turban",
            "LongHairBigHair",
            "LongHairBob",
            "LongHairBun",
            "LongHairCurly",
            "LongHairCurvy",
            "LongHairDreads",
            "LongHairFrida",
            "LongHairFro",
            "LongHairFroBand",
            "LongHairMiaWallace",
            "LongHairNotTooLong",
            "LongHairShavedSides",
            "LongHairStraight",
            "LongHairStraight2",
            "LongHairStraightStrand",
            "ShortHairDreads01",
            "ShortHairDreads02",
            "ShortHairFrizzle",
            "ShortHairShaggyMullet",
            "ShortHairShortCurly",
            "ShortHairShortFlat",
            "ShortHairShortRound",
            "ShortHairShortWaved",
            "ShortHairSides",
            "ShortHairTheCaesar",
            "ShortHairTheCaesarSidePart"
    };

    // accessoriesType
    private static final String[] ACCESSORIES = {
            "Blank",
            "Kurt",
            "Prescription01",
            "Prescription02",
            "Round",
            "Sunglasses",
            "Wayfarers"
    };

    // facialHairType
    private static final String[] FACIAL_HAIR = {
            "Blank",
            "BeardLight",
            "BeardMajestic",
            "BeardMedium",
            "MoustacheFancy",
            "MoustacheMagnum"
    };

    // clotheType
    private static final String[] CLOTHES = {
            "BlazerShirt",
            "BlazerSweater",
            "CollarSweater",
            "GraphicShirt",
            "Hoodie",
            "Overall",
            "ShirtCrewNeck",
            "ShirtScoopNeck",
            "ShirtVNeck"
    };

    // graphicType (only when clotheType = GraphicShirt)
    private static final String[] GRAPHICS = {
            "Bat","Bear","Cumbia","Deer","Diamond","Hola","Pizza","Resist","Selena","Skull","SkullOutline"
    };

    // eyeType
    private static final String[] EYES = {
            "Close","Cry","Default","Dizzy","EyeRoll","Happy","Hearts","Side","Squint","Surprised","Wink","WinkWacky"
    };

    // eyebrowType
    private static final String[] EYEBROWS = {
            "Angry","AngryNatural","Default","DefaultNatural","FlatNatural","RaisedExcited",
            "RaisedExcitedNatural","SadConcerned","SadConcernedNatural","UnibrowNatural","UpDown","UpDownNatural"
    };

    // mouthType
    private static final String[] MOUTHS = {
            "Concerned","Default","Disbelief","Eating","Grimace","Sad","ScreamOpen","Serious","Smile","Tongue","Twinkle","Vomit"
    };

    // ===== Named color palettes (no hex!) =====
    private static final String[] HAIR_COLORS = {
            "Auburn","Black","Blonde","BlondeGolden","Brown","BrownDark","PastelPink","Platinum","Red","SilverGray"
    };

    private static final String[] FACIAL_HAIR_COLORS = {
            "Auburn","Black","Blonde","BlondeGolden","Brown","BrownDark","PastelPink","Platinum","Red","SilverGray"
    };

    private static final String[] CLOTHES_COLORS = {
            "Black","Blue01","Blue02","Blue03","Gray01","Gray02","Heather",
            "PastelBlue","PastelGreen","PastelOrange","PastelRed","PastelYellow","Pink","Red","White"
    };

    private static final String[] SKINS = {
            "Tanned","Yellow","Pale","Light","Brown","DarkBrown","Black"
    };

    // Indices for cycling
    private int iTop=0, iAcc=0, iEyes=0, iBrows=0, iMouth=0, iFacial=0, iClothing=0, iGraphic=0;
    private int iHairColor=0, iClothesColor=0, iFacialHairColor=0, iSkinColor=0;

    private AvatarConfig cfg = new AvatarConfig();
    private ImageView ivPreview;

    // Tabs & section containers from your XML
    private TabLayout tabs;
    private View groupHair, groupClothes, groupAccessories, groupFace;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_avatar_editor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);

        MaterialToolbar toolbar = root.findViewById(R.id.toolbarAvatar);
        if (toolbar != null) toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        tabs = root.findViewById(R.id.tabs);
        if (tabs != null) {
            if (tabs.getTabCount() != 4) { // we show 4 sections
                tabs.removeAllTabs();
                tabs.addTab(tabs.newTab().setText("Hair"));
                tabs.addTab(tabs.newTab().setText("Clothes"));
                tabs.addTab(tabs.newTab().setText("Accessories"));
                tabs.addTab(tabs.newTab().setText("Face"));
            }
            tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override public void onTabSelected(TabLayout.Tab tab)   { showSection(tab.getPosition()); }
                @Override public void onTabUnselected(TabLayout.Tab tab) { }
                @Override public void onTabReselected(TabLayout.Tab tab) { showSection(tab.getPosition()); }
            });
        }

        ivPreview = root.findViewById(R.id.ivPreview);

        // Bind section containers from XML
        groupHair        = root.findViewById(R.id.groupHair);
        groupClothes     = root.findViewById(R.id.groupClothes);
        groupAccessories = root.findViewById(R.id.groupAccessories);
        groupFace        = root.findViewById(R.id.groupFace);

        // ---- Buttons (IDs from your layout) ----
        // Hair / Hat (topType + hairColor)
        MaterialButton btnNextTopType        = root.findViewById(R.id.btnNextTopType);
        MaterialButton btnHairColor          = root.findViewById(R.id.btnHairColor);

        // Accessories (accessoriesType only — no color on avataaars.io)
        MaterialButton btnNextAccessoriesType = root.findViewById(R.id.btnNextAccessoriesType);

        // Clothes (clotheType, clotheColor, graphicType)
        MaterialButton btnNextClotheType     = root.findViewById(R.id.btnNextClotheType);
        MaterialButton btnClotheColor        = root.findViewById(R.id.btnClotheColor);
        MaterialButton btnNextGraphicType    = root.findViewById(R.id.btnNextGraphicType);

        // Face (eyeType, eyebrowType, mouthType, facialHairType, facialHairColor)
        MaterialButton btnNextEyeType        = root.findViewById(R.id.btnNextEyeType);
        MaterialButton btnNextEyebrowType    = root.findViewById(R.id.btnNextEyebrowType);
        MaterialButton btnNextMouthType      = root.findViewById(R.id.btnNextMouthType);
        MaterialButton btnNextFacialHairType = root.findViewById(R.id.btnNextFacialHairType);
        MaterialButton btnFacialHairColor    = root.findViewById(R.id.btnFacialHairColor);

        // Avatar style + skin
        MaterialButton btnAvatarStyle        = root.findViewById(R.id.btnAvatarStyle);
        MaterialButton btnSkinColor          = root.findViewById(R.id.btnSkinColor);
        MaterialButton btnSave               = root.findViewById(R.id.btnSave);

        // Initial enable/disable of color buttons
        updateColorButtonStates(btnHairColor, btnFacialHairColor, btnClotheColor, btnSkinColor);

        // ---------------- Click handlers ----------------

        // topType (hair/hat/hijab/turban/noHair/etc.)
        if (btnNextTopType != null) btnNextTopType.setOnClickListener(v -> {
            iTop = (iTop + 1) % TOPS.length;
            cfg.top = TOPS[iTop];
            applyHairHatExclusivity();
            refresh();
            updateColorButtonStates(btnHairColor, btnFacialHairColor, btnClotheColor, btnSkinColor);
        });

        // hairColor only when topType is a hair style (not NoHair/hat/coverings)
        if (btnHairColor != null) btnHairColor.setOnClickListener(v -> {
            if (!isHairTop(cfg.top) || "NoHair".equals(cfg.top)) return;
            iHairColor = (iHairColor + 1) % HAIR_COLORS.length;
            cfg.hairColor = HAIR_COLORS[iHairColor];
            refresh();
        });

        // Accessories → accessoriesType
        if (btnNextAccessoriesType != null) btnNextAccessoriesType.setOnClickListener(v -> {
            iAcc = (iAcc + 1) % ACCESSORIES.length;
            cfg.accessories = ACCESSORIES[iAcc];
            cfg.accessoriesOn = !"Blank".equals(cfg.accessories);
            refresh();
        });

        // clotheType
        if (btnNextClotheType != null) btnNextClotheType.setOnClickListener(v -> {
            iClothing = (iClothing + 1) % CLOTHES.length;
            cfg.clothing = CLOTHES[iClothing];

            if (!"GraphicShirt".equals(cfg.clothing)) {
                cfg.clothingGraphic = null;
            } else if (cfg.clothingGraphic == null) {
                cfg.clothingGraphic = "Bat";
            }
            // Blazers cannot take color
            if (isBlazer(cfg.clothing)) {
                cfg.clothesColor = null;
            } else if (cfg.clothesColor == null) {
                iClothesColor = 0;
                cfg.clothesColor = CLOTHES_COLORS[iClothesColor];
            }
            refresh();
            updateColorButtonStates(btnHairColor, btnFacialHairColor, btnClotheColor, btnSkinColor);
        });

        // clotheColor
        if (btnClotheColor != null) btnClotheColor.setOnClickListener(v -> {
            if (isBlazer(cfg.clothing)) return;
            iClothesColor = (iClothesColor + 1) % CLOTHES_COLORS.length;
            cfg.clothesColor = CLOTHES_COLORS[iClothesColor];
            refresh();
        });

        // graphicType (only for GraphicShirt)
        if (btnNextGraphicType != null) btnNextGraphicType.setOnClickListener(v -> {
            if (!"GraphicShirt".equals(cfg.clothing)) return;
            iGraphic = (iGraphic + 1) % GRAPHICS.length;
            cfg.clothingGraphic = GRAPHICS[iGraphic];
            refresh();
        });

        // eyeType
        if (btnNextEyeType != null) btnNextEyeType.setOnClickListener(v -> {
            iEyes = (iEyes + 1) % EYES.length;
            cfg.eyes = EYES[iEyes];
            refresh();
        });

        // eyebrowType
        if (btnNextEyebrowType != null) btnNextEyebrowType.setOnClickListener(v -> {
            iBrows = (iBrows + 1) % EYEBROWS.length;
            cfg.eyebrows = EYEBROWS[iBrows];
            refresh();
        });

        // mouthType
        if (btnNextMouthType != null) btnNextMouthType.setOnClickListener(v -> {
            iMouth = (iMouth + 1) % MOUTHS.length;
            cfg.mouth = MOUTHS[iMouth];
            refresh();
        });

        // facialHairType + facialHairColor
        if (btnNextFacialHairType != null) btnNextFacialHairType.setOnClickListener(v -> {
            iFacial = (iFacial + 1) % FACIAL_HAIR.length;
            cfg.facialHair = FACIAL_HAIR[iFacial];
            cfg.facialHairOn = !"Blank".equals(cfg.facialHair);
            refresh();
            updateColorButtonStates(btnHairColor, btnFacialHairColor, btnClotheColor, btnSkinColor);
        });

        if (btnFacialHairColor != null) btnFacialHairColor.setOnClickListener(v -> {
            if (!cfg.facialHairOn || "Blank".equals(cfg.facialHair)) return;
            iFacialHairColor = (iFacialHairColor + 1) % FACIAL_HAIR_COLORS.length;
            cfg.facialHairColor = FACIAL_HAIR_COLORS[iFacialHairColor];
            refresh();
        });

        // avatarStyle (toggle Circle ↔ Transparent) using your existing background flag
        if (btnAvatarStyle != null) btnAvatarStyle.setOnClickListener(v -> {
            cfg.background = !cfg.background;
            refresh();
        });

        // skinColor
        if (btnSkinColor != null) btnSkinColor.setOnClickListener(v -> {
            iSkinColor = (iSkinColor + 1) % SKINS.length;
            cfg.skinColor = SKINS[iSkinColor];
            refresh();
        });

        // Save
        if (btnSave != null) btnSave.setOnClickListener(v -> {
            Log.d(TAG, "SAVE avatarConfig: " + cfg.toMap());
            saveAvatarToFirestore(cfg);
        });

        // Load saved config, then open Hair tab by default
        loadAvatarFromFirestore();
        if (tabs != null) tabs.selectTab(tabs.getTabAt(0));
        showSection(0);
    }

    // ------------------------- Tabs / Sections -------------------------

    private void showSection(int index) {
        // 0=Hair, 1=Clothes, 2=Accessories, 3=Face
        setVisible(groupHair,        index == 0);
        setVisible(groupClothes,     index == 1);
        setVisible(groupAccessories, index == 2);
        setVisible(groupFace,        index == 3);
    }

    private void setVisible(View v, boolean visible) {
        if (v != null) v.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    // ------------------------- Firestore load/save -------------------------

    private void loadAvatarFromFirestore() {
        String uid = (FirebaseAuth.getInstance().getCurrentUser() != null)
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "local";

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(this::applyLoadedSnapshot)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load avatarConfig", e);
                    applyHairHatExclusivity();
                    refresh();
                });
    }

    private void applyLoadedSnapshot(DocumentSnapshot snap) {
        AvatarConfig loaded = AvatarConfig.from(snap);
        if (loaded == null) loaded = new AvatarConfig();
        cfg = loaded;
        applyHairHatExclusivity();
        syncIndicesFromCfg();
        refresh();
    }

    private void saveAvatarToFirestore(AvatarConfig raw) {
        // Store EXACT avataaars values (no hex, do not lower-case)
        AvatarConfig clean = new AvatarConfig();
        clean.seed             = safe(raw.seed, "tasktimer");

        // Keep enums as-is (PascalCase for avataaars)
        clean.top              = raw.top;
        clean.accessories      = raw.accessories;
        clean.clothing         = raw.clothing;
        clean.eyes             = raw.eyes;
        clean.eyebrows         = raw.eyebrows;
        clean.mouth            = raw.mouth;
        clean.facialHair       = raw.facialHair;

        // Named colors only:
        clean.hairColor        = raw.hairColor;
        clean.facialHairColor  = raw.facialHairColor;
        clean.clothesColor     = isBlazer(raw.clothing) ? null : raw.clothesColor; // no color for blazers
        clean.skinColor        = raw.skinColor;

        // graphic only for GraphicShirt
        clean.clothingGraphic  = "GraphicShirt".equals(raw.clothing) ? raw.clothingGraphic : null;

        // avatarStyle via background boolean
        clean.background       = raw.background;

        // flags
        clean.accessoriesOn    = !"Blank".equals(raw.accessories);
        clean.facialHairOn     = raw.facialHairOn && !"Blank".equals(raw.facialHair);

        // exclusivity
        applyHairHatExclusivityFor(clean);

        String uid = (FirebaseAuth.getInstance().getCurrentUser() != null)
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "local";

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .update("avatarConfig", clean.toMap())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Saved avatarConfig to Firestore"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save avatarConfig", e));
    }

    // ------------------------- Rendering & helpers -------------------------

    private void refresh() {
        sanitizeForAvataaars(cfg);
        if (ivPreview != null) {
            AvatarSvgLoader.load(ivPreview, cfg, "EDITOR");
        }
    }

    private void sanitizeForAvataaars(AvatarConfig c) {
        applyHairHatExclusivity();

        if (c.accessories == null) {
            c.accessories = "Blank";
            c.accessoriesOn = false;
        } else {
            c.accessoriesOn = !"Blank".equals(c.accessories);
        }

        if (!"GraphicShirt".equals(c.clothing)) c.clothingGraphic = null;
        if (c.skinColor == null) c.skinColor = "Light"; // safe default
    }

    private void applyHairHatExclusivity() {
        if (isHatTop(cfg.top) || isHeadCover(cfg.top)) {
            // hair hidden; keep value but API ignores it (we can null it to be explicit)
            cfg.hairColor = null;
        } else if ("NoHair".equals(cfg.top)) {
            cfg.hairColor = null;
        }
    }

    private void applyHairHatExclusivityFor(AvatarConfig c) {
        if (isHatTop(c.top) || isHeadCover(c.top) || "NoHair".equals(c.top)) {
            c.hairColor = null;
        }
    }

    // clothing helpers
    private static boolean isBlazer(String clotheType) {
        return clotheType != null && clotheType.startsWith("Blazer");
    }

    private static boolean isNoHair(String top) { return "NoHair".equals(top); }

    private static boolean isHeadCover(String top) {
        return "Hijab".equals(top) || "Turban".equals(top);
    }

    private static boolean isHairTop(String topType) {
        if (topType == null) return false;
        return topType.startsWith("ShortHair") || topType.startsWith("LongHair");
    }

    private static boolean isHatTop(String topType) {
        return "Hat".equals(topType) || (topType != null && topType.startsWith("WinterHat"));
    }

    // UI util
    private static void setEnabledWithAlpha(View v, boolean enabled) {
        if (v == null) return;
        v.setEnabled(enabled);
        v.setAlpha(enabled ? 1f : 0.35f);
    }

    /** Call after any change to top / facial hair / clothing (and once on init). */
    private void updateColorButtonStates(
            MaterialButton btnHairColor,
            MaterialButton btnFacialHairColor,
            MaterialButton btnClothesColor,
            MaterialButton btnSkinColor
    ) {
        // Only these 4 color pickers are shown
        if (btnHairColor != null)       btnHairColor.setVisibility(View.VISIBLE);
        if (btnFacialHairColor != null) btnFacialHairColor.setVisibility(View.VISIBLE);
        if (btnClothesColor != null)    btnClothesColor.setVisibility(View.VISIBLE);
        if (btnSkinColor != null)       btnSkinColor.setVisibility(View.VISIBLE);

        final boolean hairColorAllowed       = isHairTop(cfg.top);                          // not for Hat/Hijab/Turban/NoHair
        final boolean facialHairColorAllowed = cfg.facialHairOn && cfg.facialHair != null && !"Blank".equals(cfg.facialHair);
        final boolean clothesColorAllowed    = !isBlazer(cfg.clothing);                     // Blazers: no color
        final boolean skinColorAllowed       = true;

        setEnabledWithAlpha(btnHairColor,       hairColorAllowed);
        setEnabledWithAlpha(btnFacialHairColor, facialHairColorAllowed);
        setEnabledWithAlpha(btnClothesColor,    clothesColorAllowed);
        setEnabledWithAlpha(btnSkinColor,       skinColorAllowed);

        // Clear disabled values so URL stays clean
        if (!hairColorAllowed)       cfg.hairColor       = null;
        if (!facialHairColorAllowed) cfg.facialHairColor = null;
        if (!clothesColorAllowed)    cfg.clothesColor    = null;
    }

    private void syncIndicesFromCfg() {
        iTop       = idxOf(TOPS,         cfg.top, 0);
        iAcc       = idxOf(ACCESSORIES,  cfg.accessories == null ? "Blank" : cfg.accessories, 0);
        iEyes      = idxOf(EYES,         cfg.eyes, 0);
        iBrows     = idxOf(EYEBROWS,     cfg.eyebrows, 0);
        iMouth     = idxOf(MOUTHS,       cfg.mouth, 0);
        iFacial    = idxOf(FACIAL_HAIR,  cfg.facialHairOn ? cfg.facialHair : "Blank", 0);
        iClothing  = idxOf(CLOTHES,      cfg.clothing, 0);
        iGraphic   = idxOf(GRAPHICS,     cfg.clothingGraphic == null ? "Bat" : cfg.clothingGraphic, 0);

        iHairColor        = idxOf(HAIR_COLORS,        cfg.hairColor, 0);
        iFacialHairColor  = idxOf(FACIAL_HAIR_COLORS, cfg.facialHairColor, 0);
        iClothesColor     = idxOf(CLOTHES_COLORS,     cfg.clothesColor, 0);
        iSkinColor        = idxOf(SKINS,              cfg.skinColor, 0);
    }

    private static int idxOf(String[] arr, String v, int dflt) {
        if (v == null) return dflt;
        for (int i = 0; i < arr.length; i++) if (v.equals(arr[i])) return i;
        return dflt;
    }

    private static String safe(String s, String d) { return (s==null||s.isEmpty()) ? d : s; }
}
