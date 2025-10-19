package com.erickoeckel.tasktimer;

import java.util.Objects;

public class ShopItem {
    public final String slug;
    public final String title;
    public final String desc;
    public final int price;

    public ShopItem(String slug, String title, String desc, int price) {
        this.slug = slug; this.title = title; this.desc = desc; this.price = price;
    }

    @Override public boolean equals(Object o){
        if (this == o) return true;
        if (!(o instanceof ShopItem)) return false;
        ShopItem that = (ShopItem)o;
        return Objects.equals(slug, that.slug);
    }
    @Override public int hashCode(){ return Objects.hash(slug); }
}
