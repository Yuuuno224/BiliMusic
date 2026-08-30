package com.bilimusic.app._01_net;

import com.bilimusic.app._02_model.ApiResp;
import com.bilimusic.app._02_model.FingerData;
import com.bilimusic.app._02_model.NavData;
import com.bilimusic.app._02_model.PlayData;
import com.bilimusic.app._02_model.QrGenData;
import com.bilimusic.app._02_model.QrPollData;
import com.bilimusic.app._02_model.SearchData;
import com.bilimusic.app._02_model.ViewData;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** B站接口集合：全部为同步阻塞方法，调用方须在子线程执行 */
public final class BiliApi {

    public static final class BiliException extends IOException {
        public BiliException(String msg) {
            super(msg);
        }
    }

    private static final Gson GSON = new Gson();

    private static String imgKey;
    private static String subKey;
    private static String mixinKey;
    private static long wbiFetchedAt;

    private BiliApi() {
    }

    private static <T> ApiResp<T> parse(String body, TypeToken<ApiResp<T>> type) throws IOException {
        ApiResp<T> resp = GSON.fromJson(body, type.getType());
        if (resp == null) {
            throw new BiliException("空响应");
        }
        return resp;
    }

    /** 首次使用时获取 buvid3 风控 cookie */
    public static synchronized void ensureBuvid() throws IOException {
        if (CookieStore.has("buvid3")) {
            return;
        }
        ApiResp<FingerData> resp = parse(
                BiliHttp.get("https://api.bilibili.com/x/frontend/finger/spi", null),
                new TypeToken<ApiResp<FingerData>>() { });
        if (resp.ok() && resp.data != null && resp.data.b_3 != null) {
            CookieStore.put("buvid3", resp.data.b_3);
            CookieStore.put("buvid4", resp.data.b_4);
        }
    }

    /** 从 nav 拉取并缓存 wbi 密钥（2小时刷新） */
    private static synchronized void ensureWbiKeys() throws IOException {
        if (mixinKey != null && System.currentTimeMillis() - wbiFetchedAt < 2 * 3600_000L) {
            return;
        }
        String body = BiliHttp.get("https://api.bilibili.com/x/web-interface/nav", null);
        ApiResp<NavRaw> resp = parse(body, new TypeToken<ApiResp<NavRaw>>() { });
        if (resp.data == null || resp.data.wbi_img == null) {
            throw new BiliException("nav wbi_img 缺失");
        }
        String imgUrl = resp.data.wbi_img.img_url;
        String subUrl = resp.data.wbi_img.sub_url;
        imgKey = tail(imgUrl);
        subKey = tail(subUrl);
        mixinKey = WbiSigner.mixinKey(imgKey, subKey);
        wbiFetchedAt = System.currentTimeMillis();
    }

    private static String tail(String url) {
        if (url == null) {
            return "";
        }
        int slash = url.lastIndexOf('/');
        int dot = url.lastIndexOf('.');
        if (slash < 0 || dot <= slash) {
            return url;
        }
        return url.substring(slash + 1, dot);
    }

    /** 关键词搜索视频（wbi 签名） */
    public static SearchData searchVideos(String keyword, int page) throws IOException {
        ensureBuvid();
        ensureWbiKeys();
        Map<String, String> params = new TreeMap<>();
        params.put("keyword", keyword);
        params.put("page", String.valueOf(page));
        params.put("page_size", "20");
        params.put("platform", "pc");
        params.put("search_type", "video");
        String url = "https://api.bilibili.com/x/web-interface/search/type?" + WbiSigner.sign(params, mixinKey);
        ApiResp<SearchData> resp = parse(BiliHttp.get(url, null),
                new TypeToken<ApiResp<SearchData>>() { });
        if (!resp.ok()) {
            throw new BiliException("搜索失败: " + resp.message);
        }
        return resp.data;
    }

    /** 视频详情：取 cid / 分P */
    public static ViewData videoDetail(String bvid) throws IOException {
        ApiResp<ViewData> resp = parse(
                BiliHttp.get("https://api.bilibili.com/x/web-interface/view?bvid=" + bvid, null),
                new TypeToken<ApiResp<ViewData>>() { });
        if (!resp.ok() || resp.data == null) {
            throw new BiliException("获取视频信息失败: " + resp.message);
        }
        return resp.data;
    }

    /** 取指定视频的最优音频流（登录后可拿更高码率） */
    public static PlayData.AudioStream bestAudio(String bvid, long cid) throws IOException {
        ensureBuvid();
        Map<String, String> params = new TreeMap<>();
        params.put("bvid", bvid);
        params.put("cid", String.valueOf(cid));
        params.put("fnval", "16");
        params.put("fourk", "1");
        params.put("platform", "pc");
        params.put("qn", "0");
        StringBuilder url = new StringBuilder("https://api.bilibili.com/x/player/playurl?");
        boolean first = true;
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (!first) {
                url.append('&');
            }
            url.append(e.getKey()).append('=').append(e.getValue());
            first = false;
        }
        ApiResp<PlayData> resp = parse(BiliHttp.get(url.toString(), null),
                new TypeToken<ApiResp<PlayData>>() { });
        if (!resp.ok() || resp.data == null) {
            throw new BiliException("获取播放地址失败: " + resp.message);
        }
        return pickBestAudio(resp.data);
    }

    static PlayData.AudioStream pickBestAudio(PlayData play) {
        PlayData.AudioStream best = null;
        if (play.dash != null && play.dash.audio != null) {
            for (PlayData.AudioStream a : play.dash.audio) {
                if (best == null || a.id > best.id) {
                    best = a;
                }
            }
        }
        if (best == null && play.dash != null && play.dash.flac != null) {
            best = play.dash.flac.audio;
        }
        if (best == null && play.durl != null && !play.durl.isEmpty()) {
            PlayData.AudioStream fallback = new PlayData.AudioStream();
            fallback.id = 0;
            fallback.baseUrl = play.durl.get(0).url;
            best = fallback;
        }
        if (best == null) {
            throw new IllegalStateException("无可用音频流");
        }
        return best;
    }

    /** 生成扫码/授权登录二维码 */
    public static QrGenData qrGenerate() throws IOException {
        ApiResp<QrGenData> resp = parse(
                BiliHttp.get("https://passport.bilibili.com/x/passport-login/web/qrcode/generate", null),
                new TypeToken<ApiResp<QrGenData>>() { });
        if (!resp.ok() || resp.data == null) {
            throw new BiliException("二维码生成失败: " + resp.message);
        }
        return resp.data;
    }

    /** 轮询二维码状态；成功时响应头携带登录 Cookie */
    public static ApiResp<QrPollData> qrPoll(String qrcodeKey) throws IOException {
        String url = "https://passport.bilibili.com/x/passport-login/web/qrcode/poll?qrcode_key=" + qrcodeKey;
        BiliHttp.Resp resp = BiliHttp.getResp(url, null);
        ApiResp<QrPollData> out = parse(resp.body, new TypeToken<ApiResp<QrPollData>>() { });
        for (String name : resp.headers.keySet()) {
            if ("set-cookie".equalsIgnoreCase(name)) {
                Map<String, String> cookies = CookieStore.parseSetCookie(resp.headers.get(name));
                CookieStore.putAll(cookies);
                break;
            }
        }
        return out;
    }

    /** 当前登录用户信息 */
    public static NavData navInfo() throws IOException {
        ApiResp<NavData> resp = parse(
                BiliHttp.get("https://api.bilibili.com/x/web-interface/nav", null),
                new TypeToken<ApiResp<NavData>>() { });
        if (!resp.ok() || resp.data == null) {
            throw new BiliException("获取用户信息失败: " + resp.message);
        }
        return resp.data;
    }

    /** 注销登录 */
    public static void logout() throws IOException {
        String csrf = CookieStore.get("bili_jct");
        if (csrf == null || csrf.isEmpty()) {
            return;
        }
        HttpURLConnection conn = BiliHttp.open(
                "https://passport.bilibili.com/x/passport-login/web/cookie/revoke", null);
        try {
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            byte[] body = ("csrf=" + csrf).getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body);
            }
            conn.getInputStream().close();
        } finally {
            conn.disconnect();
        }
    }

    /** nav 原始结构（含 wbi_img） */
    private static class NavRaw {
        boolean isLogin;
        long mid;
        String uname;
        String face;
        WbiImg wbi_img;
    }

    private static class WbiImg {
        String img_url;
        String sub_url;
    }

    /** 供测试/调试枚举响应头 */
    public static Map<String, List<String>> debugHeaders(String url) throws IOException {
        return BiliHttp.getResp(url, new HashMap<>()).headers;
    }
}
