package com.erickoeckel.tasktimer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class PreviewShopItemDialog extends BottomSheetDialogFragment {
    private static final String ARG_ITEM_ID = "arg_item_slug";
    private static final String ARG_ITEM_TITLE = "arg_item_title";
    private static final String ARG_ITEM_DESC = "arg_item_desc";
    private static final String ARG_ITEM_PRICE = "arg_item_price";
    private static final String ARG_UNLOCKED   = "arg_unlocked";
    private static final String ARG_COINS      = "arg_coins";

    private String itemSlug;
    private String itemTitle;
    private String itemDesc;
    private int    itemPrice;
    private boolean isUnlocked;
    private int    coinsNow;
    private ImageView iv;
    private MaterialButton btnPrimary, btnClose, btnSecondary;
    private AvatarConfig baseCfg;
    private AvatarConfig previewCfg;
    private ShopViewModel shopVm;
    public static PreviewShopItemDialog newInstance(@NonNull ShopItem item, boolean unlocked) {
        Bundle a = new Bundle();
        a.putString(ARG_ITEM_ID, item.slug);
        a.putString(ARG_ITEM_TITLE, item.title);
        a.putBoolean(ARG_UNLOCKED, unlocked);
        PreviewShopItemDialog d = new PreviewShopItemDialog();
        d.setArguments(a);
        return d;
    }

    public static void show(@NonNull androidx.fragment.app.Fragment host,
                            @NonNull ShopItem item,
                            boolean unlocked,
                            int coins) {
        Bundle a = new Bundle();
        a.putString(ARG_ITEM_ID,  item.slug);
        a.putString(ARG_ITEM_TITLE, item.title);
        a.putString(ARG_ITEM_DESC,  item.desc);
        a.putInt(ARG_ITEM_PRICE,    item.price);
        a.putBoolean(ARG_UNLOCKED,  unlocked);
        a.putInt(ARG_COINS,         coins);
        PreviewShopItemDialog d = new PreviewShopItemDialog();
        d.setArguments(a);
        d.show(host.getParentFragmentManager(), "shopPreview");
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        View v = inf.inflate(R.layout.dialog_preview_avatar, c, false);
        iv = v.findViewById(R.id.ivPreview);
        btnPrimary = v.findViewById(R.id.btnPrimary);
        btnClose   = v.findViewById(R.id.btnClose);
        Bundle args = getArguments();
        if (args != null) {
            itemSlug   = args.getString(ARG_ITEM_ID, "");
            itemTitle  = args.getString(ARG_ITEM_TITLE, "");
            itemDesc   = args.getString(ARG_ITEM_DESC, "");
            itemPrice  = args.getInt(ARG_ITEM_PRICE, 0);
            isUnlocked = args.getBoolean(ARG_UNLOCKED, false);
            coinsNow   = args.getInt(ARG_COINS, 0);
        }

        shopVm = new ViewModelProvider(requireActivity()).get(ShopViewModel.class);

        String itemId   = getArguments().getString(ARG_ITEM_ID, "");
        String itemName = getArguments().getString(ARG_ITEM_TITLE, "");
        boolean unlocked = getArguments().getBoolean(ARG_UNLOCKED, false);

        btnPrimary.setText(unlocked ? "Equip" : "Buy & Equip");
        btnClose.setOnClickListener(x -> dismiss());

        String uid = (FirebaseAuth.getInstance().getCurrentUser() != null)
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "local";

        FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .get()
                .addOnSuccessListener(this::onLoaded)
                .addOnFailureListener(e -> {
                    baseCfg = new AvatarConfig();
                    renderPreview(itemId);
                });

        btnPrimary.setOnClickListener(x -> {
            if (previewCfg == null) return;

            if (unlocked) {
                saveAvatar(previewCfg);
            } else {
                ShopItem item = new ShopItem(itemId, itemName, "", 0); // price not needed for VM call
                shopVm.purchase(item)
                        .addOnSuccessListener(ok -> saveAvatar(previewCfg))
                        .addOnFailureListener(e ->
                                android.widget.Toast.makeText(requireContext(),
                                        e.getMessage() == null ? "Purchase failed" : e.getMessage(),
                                        android.widget.Toast.LENGTH_LONG).show());
            }
        });

        refreshButtons();
        return v;
    }

    private void onLoaded(DocumentSnapshot snap) {
        AvatarConfig loaded = AvatarConfig.from(snap);
        baseCfg = (loaded != null) ? loaded : new AvatarConfig();
        String itemId = getArguments().getString(ARG_ITEM_ID, "");
        renderPreview(itemId);
    }

    private void renderPreview(@NonNull String slug) {
        previewCfg = AvatarConfig.copyOf(baseCfg);
        applyShopSlug(previewCfg, slug);
        sanitize(previewCfg);
        if (iv != null) AvatarSvgLoader.load(iv, previewCfg, "SHOP_PREVIEW");
    }

    private void saveAvatar(@NonNull AvatarConfig cfg) {
        if (!isUnlocked) {
            android.widget.Toast.makeText(requireContext(),
                    "This item is locked. Buy it first.", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        AvatarConfig clean = AvatarConfig.copyOf(cfg);
        sanitize(clean);

        String uid = (FirebaseAuth.getInstance().getCurrentUser() != null)
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "local";

        FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .update("avatarConfig", clean.toMap())
                .addOnSuccessListener(aVoid -> dismiss())
                .addOnFailureListener(e ->
                        android.util.Log.e("PreviewShop", "Failed to save avatarConfig", e));
    }

    private static void applyShopSlug(@NonNull AvatarConfig c, @NonNull String slug) {
        if (slug.startsWith("LongHair") || slug.startsWith("ShortHair")
                || "NoHair".equals(slug) || "Hat".equals(slug) || "Hijab".equals(slug) || "Turban".equals(slug)) {
            c.top = slug;
            return;
        }
        if ("Hoodie".equals(slug) || "Overall".equals(slug)
                || "BlazerShirt".equals(slug) || "BlazerSweater".equals(slug)
                || "CollarSweater".equals(slug) || "GraphicShirt".equals(slug)
                || "ShirtCrewNeck".equals(slug) || "ShirtScoopNeck".equals(slug) || "ShirtVNeck".equals(slug)) {
            c.clothing = slug;
            return;
        }
        if (slug.startsWith("Beard") || slug.startsWith("Moustache")) {
            c.facialHair = slug;
            c.facialHairOn = !"Blank".equals(slug);
            return;
        }
        if ("Hearts".equals(slug) || "Dizzy".equals(slug)) {
            c.eyes = slug; return;
        }
        if ("Twinkle".equals(slug) || "Tongue".equals(slug)) {
            c.mouth = slug; return;
        }
    }

    private static boolean isHatTop(@Nullable String top) {
        return "Hat".equals(top) || (top != null && top.startsWith("WinterHat"));
    }
    private static boolean isHeadCover(@Nullable String top) {
        return "Hijab".equals(top) || "Turban".equals(top);
    }
    private static boolean isHairTop(@Nullable String t) {
        return t != null && (t.startsWith("ShortHair") || t.startsWith("LongHair"));
    }
    private static boolean isBlazer(@Nullable String clotheType) {
        return clotheType != null && clotheType.startsWith("Blazer");
    }

    private static void sanitize(@NonNull AvatarConfig c) {
        if (isHatTop(c.top) || isHeadCover(c.top) || "NoHair".equals(c.top)) c.hairColor = null;

        if (!"GraphicShirt".equals(c.clothing)) c.clothingGraphic = null;

        if (isBlazer(c.clothing)) c.clothesColor = null;

        c.accessoriesOn = c.accessories != null && !"Blank".equals(c.accessories);
        c.facialHairOn  = c.facialHair != null && !"Blank".equals(c.facialHair) && c.facialHairOn;

        if (c.skinColor == null) c.skinColor = "Light";
    }

    private void refreshButtons() {
        boolean canAfford = coinsNow >= itemPrice;

        if (btnSecondary != null) {
            btnSecondary.setText("Close");
            btnSecondary.setOnClickListener(v -> dismiss());
            btnSecondary.setEnabled(true);
            btnSecondary.setAlpha(1f);
        }

        if (btnPrimary == null) return;

        if (isUnlocked) {
            btnPrimary.setText("Equip");
            btnPrimary.setEnabled(true);
            btnPrimary.setAlpha(1f);
            btnPrimary.setOnClickListener(v -> {
                saveAvatar(previewCfg);
            });
        } else if (canAfford) {
            btnPrimary.setText("Buy • " + itemPrice + " 🪙");
            btnPrimary.setEnabled(true);
            btnPrimary.setAlpha(1f);
            btnPrimary.setOnClickListener(v -> {
                ShopViewModel vm = new androidx.lifecycle.ViewModelProvider(requireActivity())
                        .get(ShopViewModel.class);

                ShopItem item = new ShopItem(itemSlug,
                        (itemTitle == null ? "" : itemTitle),
                        (itemDesc == null ? "" : itemDesc),
                        itemPrice);

                btnPrimary.setEnabled(false);
                vm.purchase(item)
                        .addOnSuccessListener(x -> {
                            isUnlocked = true;
                            coinsNow -= itemPrice;
                            android.widget.Toast
                                    .makeText(requireContext(), "Purchased " + item.title + "!", android.widget.Toast.LENGTH_SHORT)
                                    .show();
                            refreshButtons();
                        })
                        .addOnFailureListener(e -> {
                            btnPrimary.setEnabled(true);
                            String msg = (e != null && e.getMessage() != null) ? e.getMessage() : "Purchase failed";
                            android.widget.Toast
                                    .makeText(requireContext(), msg, android.widget.Toast.LENGTH_LONG)
                                    .show();
                        });
            });
        } else {
            int shortBy = Math.max(0, itemPrice - coinsNow);
            btnPrimary.setText("Need " + shortBy + " more 🪙");
            btnPrimary.setEnabled(false);
            btnPrimary.setAlpha(0.5f);
            btnPrimary.setOnClickListener(null);
        }
    }

}
