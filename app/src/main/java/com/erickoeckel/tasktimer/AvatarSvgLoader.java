package com.erickoeckel.tasktimer;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AvatarSvgLoader {

    private static final String TAG = "Avatar";
    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private AvatarSvgLoader() {}

    public static void load(@NonNull ImageView into, @NonNull AvatarConfig cfg, @NonNull String originTag) {
        final String initialUrlRaw = AvatarUrl.build(cfg);

        final String initialUrl = initialUrlRaw;

        EXEC.execute(() -> {
            PreflightResult pre = preflight(initialUrl, originTag, false);
            String urlToUse = initialUrl;

            if (!pre.ok) {
                String retryUrl = computeFallbackUrl(initialUrl);
                if (retryUrl != null) {
                    retryUrl = normalizeUrlForAvataaarsV9(retryUrl);
                    PreflightResult retry = preflight(retryUrl, originTag, true);
                    if (retry.ok) {
                        urlToUse = retryUrl;
                        Log.i(TAG, "[" + originTag + "] Using generic fallback URL after successful retry.");
                    } else {
                        Log.e(TAG, "[" + originTag + "] Fallback also failed; proceeding with original to let Glide render error.");
                    }
                }
            }

            final String finalUrl = urlToUse;
            MAIN.post(() -> {
                Glide.with(into.getContext())
                        .load(finalUrl)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .signature(new com.bumptech.glide.signature.ObjectKey(
                                finalUrl + "#" + System.nanoTime()))
                        .into(into);
            });
        });
    }

    private static String normalizeUrlForAvataaarsV9(String url) {
        String out = url;

        out = out.replace("https://api.dicebear.com/7.x/avataaars/", "https://api.dicebear.com/9.x/avataaars/");

        final boolean hadTransparentTrue = out.matches(".*([&?])transparent=true(&|$).*");
        out = out.replaceAll("([&?])transparent=(true|false)(&|$)", "$1");

        if (hadTransparentTrue && !out.contains("backgroundColor=")) {
            out = upsertQueryParam(out, "backgroundColor", "transparent");
        }

        String hairVal = extractParamValue(out, "hair");
        if (hairVal != null) {
            if (extractParamValue(out, "top") == null) {
                out = upsertQueryParam(removeQueryParam(out, "hair"), "top", hairVal);
            } else {
                out = removeQueryParam(out, "hair");
            }
        }

        final String topVal = extractParamValue(out, "top");
        final boolean isHat = topVal != null && (topVal.equals("hat") || topVal.startsWith("winterHat"));
        final boolean isNoHair = topVal != null && topVal.equals("noHair");
        final boolean isHairTop = topVal != null && (topVal.startsWith("shortHair") || topVal.startsWith("longHair"));

        if (!isHairTop) {
            out = out.replaceAll("&hairColor=[A-Fa-f0-9]{6}", "");
        }
        if (!isHat) {
            out = out.replaceAll("&hatColor=[A-Fa-f0-9]{6}", "");
        }
        if (isNoHair) {
            out = out.replaceAll("&hairColor=[A-Fa-f0-9]{6}", "");
            out = removeQueryParam(out, "hair"); // just in case
        }

        out = out.replace("?&", "?").replace("&&", "&");
        if (out.endsWith("&") || out.endsWith("?")) out = out.substring(0, out.length() - 1);

        Log.d(TAG, "[BUILD] v9-normalized URL = " + out);
        return out;
    }

    private static String computeFallbackUrl(String url) {
        if (url.contains("&graphicType=")) {
            Log.w(TAG, "[FALLBACK] Removing graphicType …");
            return removeQueryParam(url, "graphicType");
        }
        if (url.contains("&clotheColor=")) {
            Log.w(TAG, "[FALLBACK] Removing clotheColor …");
            return removeQueryParam(url, "clotheColor");
        }
        if (url.contains("&facialHairColor=")) {
            Log.w(TAG, "[FALLBACK] Removing facialHairColor …");
            return removeQueryParam(url, "facialHairColor");
        }
        if (url.contains("&hairColor=")) {
            Log.w(TAG, "[FALLBACK] Removing hairColor …");
            return removeQueryParam(url, "hairColor");
        }
        if (url.contains("&accessoriesType=") && !url.contains("accessoriesType=Blank")) {
            Log.w(TAG, "[FALLBACK] Forcing accessoriesType=Blank …");
            return upsertQueryParam(url, "accessoriesType", "Blank");
        }
        return null;
    }

    private static PreflightResult preflight(String urlStr, String originTag, boolean isRetry) {
        HttpURLConnection conn = null;
        try {
            URL u = new URL(urlStr);
            conn = (HttpURLConnection) u.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(7000);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "image/svg+xml,application/json;q=0.9,*/*;q=0.8");
            conn.setRequestProperty("User-Agent", "TaskTimer/" + (Build.VERSION.RELEASE != null ? Build.VERSION.RELEASE : "android"));

            int status = conn.getResponseCode();
            boolean ok = status >= 200 && status < 300;
            String body = readBody(ok ? conn.getInputStream() : conn.getErrorStream(), 65536);

            if (!ok) {
                Log.e(TAG, "[" + originTag + (isRetry ? " RETRY" : "") + "] Preflight status=" + status + " url=" + urlStr);
                if (body != null && !body.isEmpty()) {
                    Log.e(TAG, "[" + originTag + (isRetry ? " RETRY" : "") + "] Preflight body (first 1k): " +
                            body.substring(0, Math.min(1024, body.length())));
                }
            } else {
                Log.d(TAG, "[" + originTag + (isRetry ? " RETRY" : "") + "] Preflight OK url=" + urlStr);
            }
            return new PreflightResult(ok, status, body);

        } catch (Exception e) {
            Log.e(TAG, "[" + originTag + (isRetry ? " RETRY" : "") + "] Preflight exception for url=" + urlStr, e);
            return new PreflightResult(false, -1, e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static String readBody(InputStream is, int max) {
        if (is == null) return null;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder sb = new StringBuilder();
            int total = 0;
            String line;
            while ((line = br.readLine()) != null && total < max) {
                sb.append(line).append('\n');
                total += line.length();
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static final class PreflightResult {
        final boolean ok;
        final int status;
        final String body;
        PreflightResult(boolean ok, int status, String body) { this.ok = ok; this.status = status; this.body = body; }
    }
    private static String extractParamValue(String url, String key) {
        int i = url.indexOf("&" + key + "=");
        if (i < 0) i = url.indexOf("?" + key + "=");
        if (i < 0) return null;
        int start = i + key.length() + 2;
        int end = url.indexOf("&", start);
        return (end < 0) ? url.substring(start) : url.substring(start, end);
    }

    private static String upsertQueryParam(String url, String key, String value) {
        if (value == null) return url;
        String out = removeQueryParam(url, key);
        char join = out.contains("?") ? '&' : '?';
        out = out + join + key + "=" + value;
        return out.replace("?&", "?").replace("&&", "&");
    }

    private static String removeQueryParam(String url, String key) {
        String out = url.replaceAll("([&?])" + key + "=[^&]*", "$1");
        out = out.replace("?&", "?").replace("&&", "&");
        if (out.endsWith("&") || out.endsWith("?")) out = out.substring(0, out.length() - 1);
        return out;
    }

}
