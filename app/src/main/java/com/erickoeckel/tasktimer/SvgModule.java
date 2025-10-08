package com.erickoeckel.tasktimer;

import android.graphics.drawable.PictureDrawable;
import androidx.annotation.NonNull;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.caverock.androidsvg.SVG;

import java.io.InputStream;

@GlideModule
public class SvgModule extends AppGlideModule {
    @Override public void registerComponents(@NonNull android.content.Context context, @NonNull Glide glide, @NonNull Registry registry) {
        registry.register(SVG.class, PictureDrawable.class, new SvgDrawableTranscoder())
                .append(InputStream.class, SVG.class, new SvgDecoder());
    }
}
