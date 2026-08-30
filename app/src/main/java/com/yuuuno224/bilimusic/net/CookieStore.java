package com.yuuuno224.bilimusic.net;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/** Cookie 持久化：登录成功后写入，所有请求统一携带 */
public final class CookieStore {

    private static final String PREF = "bili_cookies";
    private static final String KEY = "cookies_json";

    private static final Gson GSON = new Gson();
    private static Map<String, String> cookies = new HashMap<>();

    private CookieStore() {
    }

    public static void init(Context ctx) {
        SharedPreferences sp = ctx.getApplicationContext()
                .getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String json = sp.getString(KEY, null);
        if (json != null) {
            Type t = new TypeToken<Map<String, String>>() { }.getType();
            Map<String, String> loaded = GSON.fromJson(json, t);
            if (loaded != null) {
                cookies = loaded;
            }
        }
    }

    public static synchronized void putAll(Map<String, String> incoming) {
        if (incoming == null) {
            return;
        }
        cookies.putAll(incoming);
    }

    public static synchronized void put(String name, String value) {
        cookies.put(name, value);
    }

    public static synchronized String get(String name) {
        return cookies.get(name);
    }

    public static synchronized boolean has(String name) {
        return cookies.containsKey(name) && cookies.get(name) != null && !cookies.get(name).isEmpty();
    }

    public static synchronized void clearSession() {
        cookies.remove("SESSDATA");
        cookies.remove("bili_jct");
        cookies.remove("DedeUserID");
        cookies.remove("DedeUserID__ckMd5");
        cookies.remove("sid");
    }

    /** 生成请求 Cookie 头 */
    public static synchronized String cookieHeader() {
        if (cookies.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : cookies.entrySet()) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(e.getKey()).append('=').append(e.getValue());
        }
        return sb.toString();
    }

    public static synchronized void persist(Context ctx) {
        SharedPreferences sp = ctx.getApplicationContext()
                .getSharedPreferences(PREF, Context.MODE_PRIVATE);
        sp.edit().putString(KEY, GSON.toJson(cookies)).apply();
    }

    /** 从 Set-Cookie 头解析 "k=v; Path=/..." 形式 */
    public static Map<String, String> parseSetCookie(Iterable<String> setCookies) {
        Map<String, String> out = new HashMap<>();
        if (setCookies == null) {
            return out;
        }
        for (String raw : setCookies) {
            String first = raw.split(";")[0];
            int eq = first.indexOf('=');
            if (eq > 0) {
                out.put(first.substring(0, eq).trim(), first.substring(eq + 1).trim());
            }
        }
        return out;
    }
}
