package com.bilimusic.app._01_net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/** 轻量 HTTP 封装：统一 UA/Referer/Cookie，基于 HttpURLConnection */
public final class BiliHttp {

    public static final String UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/126.0.0.0 Safari/537.36";
    public static final String REFERER = "https://www.bilibili.com/";

    /** 一次请求的完整结果（含响应头，供登录轮询读取 Set-Cookie） */
    public static class Resp {
        public final String body;
        public final Map<String, List<String>> headers;

        public Resp(String body, Map<String, List<String>> headers) {
            this.body = body;
            this.headers = headers;
        }
    }

    private BiliHttp() {
    }

    /** GET 请求并返回响应体文本 */
    public static String get(String url, Map<String, String> extraHeaders) throws IOException {
        return getResp(url, extraHeaders).body;
    }

    /** GET 请求，返回响应体 + 响应头 */
    public static Resp getResp(String url, Map<String, String> extraHeaders) throws IOException {
        HttpURLConnection conn = open(url, extraHeaders);
        try {
            return new Resp(readAll(conn.getInputStream()), conn.getHeaderFields());
        } finally {
            conn.disconnect();
        }
    }

    /** GET 请求，错误时抛出含状态码的 IOException */
    public static String getStrict(String url, Map<String, String> extraHeaders) throws IOException {
        HttpURLConnection conn = open(url, extraHeaders);
        try {
            int code = conn.getResponseCode();
            if (code >= 400) {
                throw new IOException("HTTP " + code + " for " + url);
            }
            return readAll(conn.getInputStream());
        } finally {
            conn.disconnect();
        }
    }

    public static HttpURLConnection open(String url, Map<String, String> extraHeaders)
            throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("User-Agent", UA);
        conn.setRequestProperty("Referer", REFERER);
        String cookie = CookieStore.cookieHeader();
        if (cookie != null && !cookie.isEmpty()) {
            conn.setRequestProperty("Cookie", cookie);
        }
        if (extraHeaders != null) {
            for (Map.Entry<String, String> e : extraHeaders.entrySet()) {
                conn.setRequestProperty(e.getKey(), e.getValue());
            }
        }
        return conn;
    }

    private static String readAll(InputStream in) throws IOException {
        if (in == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(1 << 16);
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            char[] buf = new char[8192];
            int n;
            while ((n = r.read(buf)) > 0) {
                sb.append(buf, 0, n);
            }
        }
        return sb.toString();
    }
}
