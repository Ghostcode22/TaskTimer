package com.erickoeckel.tasktimer;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Arrays;
import java.util.List;

public class ShopFragment extends Fragment {

    private TextView tvCoins;
    private RecyclerView rv;
    private ShopAdapter adapter;
    private ShopViewModel vm;

    private static final List<ShopItem> CATALOG = Arrays.asList(
            new ShopItem(Unlocks.HAIR_BIG,     "Long Big Hair",      "Adds topType: LongHairBigHair.", 120),
            new ShopItem(Unlocks.HAIR_FROBAND, "Fro Band Hair",      "Adds topType: LongHairFroBand.", 120),

            new ShopItem(Unlocks.CLOTHES_HOODIE,  "Hoodie",          "Adds clotheType: Hoodie.",       90),
            new ShopItem(Unlocks.CLOTHES_OVERALL, "Overalls",        "Adds clotheType: Overall.",      90),

            new ShopItem(Unlocks.FACIAL_MAJESTIC, "Beard Majestic",  "Adds facialHairType: BeardMajestic.", 80),
            new ShopItem(Unlocks.FACIAL_FANCY,    "Fancy Moustache", "Adds facialHairType: MoustacheFancy.", 80),

            new ShopItem(Unlocks.EYES_HEARTS, "Hearts Eyes",         "Adds eyeType: Hearts.",          70),
            new ShopItem(Unlocks.EYES_DIZZY,  "Dizzy Eyes",          "Adds eyeType: Dizzy.",           70),

            new ShopItem(Unlocks.MOUTH_TWINKLE, "Twinkle Mouth",     "Adds mouthType: Twinkle.",       70),
            new ShopItem(Unlocks.MOUTH_TONGUE,  "Tongue Mouth",      "Adds mouthType: Tongue.",        70)
    );

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup parent, @Nullable Bundle b) {
        View v = inf.inflate(R.layout.fragment_shop, parent, false);
        tvCoins = v.findViewById(R.id.tvCoins);
        rv = v.findViewById(R.id.rvShop);

        vm = new ViewModelProvider(this).get(ShopViewModel.class);

        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));

        adapter = new ShopAdapter(item ->
                vm.purchase(item)
                        .addOnSuccessListener(x ->
                                Toast.makeText(requireContext(), "Purchased " + item.title + "!", Toast.LENGTH_SHORT).show()
                        )
                        .addOnFailureListener(e ->
                                Toast.makeText(requireContext(),
                                        TextUtils.isEmpty(e.getMessage()) ? "Purchase failed" : e.getMessage(),
                                        Toast.LENGTH_LONG).show()
                        )
        );
        rv.setAdapter(adapter);

        vm.coins().observe(getViewLifecycleOwner(), coins -> {
            tvCoins.setText((coins == null ? 0 : coins) + " 🪙");
            adapter.submit(CATALOG, vm.unlocks().getValue(), coins == null ? 0 : coins);
        });
        vm.unlocks().observe(getViewLifecycleOwner(), unlocks ->
                adapter.submit(CATALOG, unlocks, vm.coins().getValue() == null ? 0 : vm.coins().getValue())
        );

        vm.start();
        return v;
    }
}
