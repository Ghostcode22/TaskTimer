package com.erickoeckel.tasktimer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.*;

public class ShopAdapter extends RecyclerView.Adapter<ShopAdapter.VH> {
    public interface OnBuyClick { void onBuy(ShopItem item); }
    public interface OnPreviewClick { void onPreview(ShopItem item); }

    private final OnBuyClick onBuy;
    private final OnPreviewClick onPreview;
    private final List<ShopItem> items = new ArrayList<>();
    private Map<String, Boolean> unlocks = new HashMap<>();
    private int coins = 0;

    public ShopAdapter(@NonNull OnBuyClick onBuy, @NonNull OnPreviewClick onPreview) {
        this.onBuy = onBuy;
        this.onPreview = onPreview;
    }
    public void submit(List<ShopItem> src, Map<String, Boolean> unlocks, int coins){
        items.clear(); items.addAll(src);
        this.unlocks = (unlocks == null) ? new HashMap<>() : unlocks;
        this.coins = coins;
        notifyDataSetChanged();
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_shop_item, parent, false);
        return new VH(row);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        ShopItem it = items.get(pos);
        boolean owned = Boolean.TRUE.equals(unlocks.get(it.slug));

        h.tvTitle.setText(it.title + (owned ? "  ✓" : ""));
        h.tvDesc.setText(it.desc);
        h.tvPrice.setText("Price: " + it.price + " 🪙");

        boolean canBuy = !owned && coins >= it.price;
        h.btnBuy.setText(owned ? "Owned" : "Buy");
        h.btnBuy.setEnabled(canBuy);
        h.btnBuy.setAlpha(canBuy ? 1f : 0.5f);
        h.btnBuy.setOnClickListener(v -> { if (canBuy) onBuy.onBuy(it); });
        h.itemView.setOnClickListener(v -> onPreview.onPreview(it));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvTitle, tvDesc, tvPrice;
        final Button btnBuy;
        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvItemTitle);
            tvDesc  = itemView.findViewById(R.id.tvItemDesc);
            tvPrice = itemView.findViewById(R.id.tvItemPrice);
            btnBuy  = itemView.findViewById(R.id.btnBuy);
        }
    }
}
