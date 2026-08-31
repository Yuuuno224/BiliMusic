package com.yuuuno224.bilimusic.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;

import com.yuuuno224.bilimusic.net.BiliHttp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 轻量图片加载器：内存LRU + 磁盘缓存，避免引入 Glide */
public final class ImageLoader {

    public interface Callback {
        void onLoaded(Bitmap bitmap);
    }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final ExecutorService POOL = Executors.newFixedThreadPool(4);
    private static final LruCache<String, Bitmap> MEM = new LruCache<String, Bitmap>(16 * 1024 * 1024) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }
    };

    private static File cacheDir;

    private ImageLoader() {
    }

    public static void init(android.content.Context ctx) {
        cacheDir = new File(ctx.getCacheDir(), "covers");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
    }

    public static void load(String url, Callback cb) {
        if (url == null || url.isEmpty()) {
            cb.onLoaded(null);
            return;
        }
        // 部分接口返回 http:// 或协议相对地址，Android 9+ 禁止明文请求，统一升级 https
        String norm = url;
        if (norm.startsWith("//")) {
            norm = "https:" + norm;
        } else if (norm.startsWith("http://")) {
            norm = "https://" + norm.substring(7);
        }
        final String key = norm;
        Bitmap mem = MEM.get(key);
        if (mem != null) {
            cb.onLoaded(mem);
            return;
        }
        POOL.execute(() -> {
            Bitmap bmp = diskGet(key);
            if (bmp == null) {
                bmp = netGet(key);
                if (bmp != null) {
                    diskPut(key, bmp);
                }
            }
            Bitmap finalBmp = bmp;
            if (finalBmp != null) {
                MEM.put(key, finalBmp);
            }
            MAIN.post(() -> cb.onLoaded(finalBmp));
        });
    }

    private static Bitmap diskGet(String url) {
        if (cacheDir == null) {
            return null;
        }
        File f = new File(cacheDir, hash(url));
        if (!f.exists() || f.length() == 0) {
            return null;
        }
        return BitmapFactory.decodeFile(f.getAbsolutePath());
    }

    private static void diskPut(String url, Bitmap bmp) {
        if (cacheDir == null) {
            return;
        }
        File f = new File(cacheDir, hash(url));
        try (OutputStream os = new FileOutputStream(f)) {
            bmp.compress(Bitmap.CompressFormat.WEBP, 85, os);
        } catch (Exception ignored) {
        }
    }

    private static Bitmap netGet(String url) {
        HttpURLConnection conn = null;
        try {
            conn = BiliHttp.open(url, null);
            try (InputStream in = conn.getInputStream()) {
                byte[] all = new byte[1024 * 512];
                java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                int n;
                while ((n = in.read(all)) > 0) {
                    bos.write(all, 0, n);
                }
                byte[] bytes = bos.toByteArray();
                return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            }
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String hash(String s) {
        try {
            byte[] d = MessageDigest.getInstance("MD5").digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(s.hashCode());
        }
    }
}
