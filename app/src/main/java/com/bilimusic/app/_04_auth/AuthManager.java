package com.bilimusic.app._04_auth;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.bilimusic.app._01_net.BiliApi;
import com.bilimusic.app._01_net.CookieStore;
import com.bilimusic.app._02_model.ApiResp;
import com.bilimusic.app._02_model.NavData;
import com.bilimusic.app._02_model.QrGenData;
import com.bilimusic.app._02_model.QrPollData;
import com.google.gson.Gson;

import java.io.IOException;

/** 登录态管理：生成授权链接 → 轮询 → 保存Cookie → 拉取用户信息 */
public final class AuthManager {

    public interface LoginCallback {
        void onGenerated(String authUrl, String qrcodeKey);

        void onStatus(int state, String message);

        void onSuccess(NavData me);

        void onError(String message);
    }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Gson GSON = new Gson();
    private static NavData cachedMe;

    private AuthManager() {
    }

    public static boolean isLoggedIn() {
        return CookieStore.has("SESSDATA") && CookieStore.has("bili_jct");
    }

    /** 第一步：生成授权链接（手机上点链接会唤起B站APP确认登录） */
    public static void generate(LoginCallback cb) {
        exec(() -> {
            try {
                QrGenData gen = BiliApi.qrGenerate();
                MAIN.post(() -> cb.onGenerated(gen.url, gen.qrcode_key));
            } catch (IOException e) {
                MAIN.post(() -> cb.onError("网络错误: " + e.getMessage()));
            }
        });
    }

    /** 第二步：单次轮询，Activity 每 2 秒调用一次直到成功/过期 */
    public static void pollOnce(String qrcodeKey, LoginCallback cb) {
        exec(() -> {
            try {
                ApiResp<QrPollData> resp = BiliApi.qrPoll(qrcodeKey);
                int state = resp.data != null ? resp.data.code : -1;
                switch (state) {
                    case QrPollData.ST_SUCCESS:
                        CookieStore.persist(appContext);
                        NavData me = fetchMeQuietly();
                        if (me != null) {
                            persistMe(appContext, me);
                            NavData finalMe = me;
                            MAIN.post(() -> cb.onSuccess(finalMe));
                        } else {
                            MAIN.post(cb::onSuccess);
                        }
                        break;
                    case QrPollData.ST_SCANNED:
                        MAIN.post(() -> cb.onStatus(state, "已在B站APP中确认，请稍候…"));
                        break;
                    case QrPollData.ST_EXPIRED:
                        MAIN.post(() -> cb.onStatus(state, "授权已过期"));
                        break;
                    default:
                        MAIN.post(() -> cb.onStatus(state, "等待B站APP确认授权…"));
                        break;
                }
            } catch (IOException e) {
                MAIN.post(() -> cb.onError("网络错误: " + e.getMessage()));
            }
        });
    }

    public static void fetchMe(MeCallback cb) {
        exec(() -> {
            try {
                NavData me = BiliApi.navInfo();
                persistMe(appContext, me);
                MAIN.post(() -> cb.onResult(true, me));
            } catch (IOException e) {
                MAIN.post(() -> cb.onResult(false, null));
            }
        });
    }

    public static NavData cachedMe() {
        return cachedMe;
    }

    public static void logout() {
        exec(() -> {
            try {
                BiliApi.logout();
            } catch (IOException ignored) {
            }
            CookieStore.clearSession();
            CookieStore.persist(appContext);
            cachedMe = null;
            persistMe(appContext, null);
        });
    }

    private static NavData fetchMeQuietly() {
        try {
            return BiliApi.navInfo();
        } catch (IOException e) {
            return null;
        }
    }

    private static void persistMe(Context ctx, NavData me) {
        cachedMe = me;
        ctx.getSharedPreferences("bili_auth", Context.MODE_PRIVATE)
                .edit()
                .putString("me", me == null ? null : GSON.toJson(me))
                .apply();
    }

    public static void loadCachedMe(Context ctx) {
        String json = ctx.getSharedPreferences("bili_auth", Context.MODE_PRIVATE).getString("me", null);
        if (json != null) {
            cachedMe = GSON.fromJson(json, NavData.class);
        }
    }

    public interface MeCallback {
        void onResult(boolean ok, NavData me);
    }

    private static Context appContext;

    public static void init(Context ctx) {
        appContext = ctx.getApplicationContext();
        CookieStore.init(appContext);
        loadCachedMe(appContext);
    }

    private static void exec(Runnable r) {
        new Thread(r, "auth-io").start();
    }
}
