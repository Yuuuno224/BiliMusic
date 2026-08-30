import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * B站接口探测脚本：验证 扫码登录生成/wbi签名/搜索/视频详情/DASH音频流 链路是否可用。
 * 仅用于开发期验证，运行: java _01_BiliApiProbe.java
 */
public class _01_BiliApiProbe {

    static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";
    static String buvid3 = "";
    static String imgKey = "", subKey = "";

    static final int[] MIXIN_TAB_V1 = {
            46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35, 27, 43, 5, 49,
            33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13, 37, 48, 7, 16, 24, 55, 40, 61,
            26, 17, 0, 1, 60, 51, 30, 4, 22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11, 36,
            20, 34, 44, 52};
    static final int[] MIXIN_TAB_V2 = {
            70, 66, 29, 63, 9, 34, 78, 39, 5, 48, 20, 68, 8, 26, 23, 74, 3, 56, 24, 45,
            35, 12, 51, 67, 55, 47, 72, 2, 18, 42, 60, 31, 6, 52, 61, 62, 73, 33, 4, 22,
            13, 65, 1, 75, 44, 30, 27, 40, 64, 76, 53, 36, 25, 57, 49, 69, 50, 14, 15, 10,
            54, 58, 59, 28};

    public static void main(String[] args) throws Exception {
        System.out.println("== STEP1: finger/spi 获取 buvid3 ==");
        String spi = get("https://api.bilibili.com/x/frontend/finger/spi");
        System.out.println(spi.substring(0, Math.min(300, spi.length())));
        buvid3 = extractJson(spi, "b_3");
        System.out.println("buvid3 = " + buvid3);

        System.out.println("\n== STEP2: nav 获取 wbi keys ==");
        String nav = get("https://api.bilibili.com/x/web-interface/nav");
        String imgUrl = extractJson(nav, "img_url");
        String subUrl = extractJson(nav, "sub_url");
        imgKey = imgUrl.substring(imgUrl.lastIndexOf('/') + 1, imgUrl.lastIndexOf('.'));
        subKey = subUrl.substring(subUrl.lastIndexOf('/') + 1, subUrl.lastIndexOf('.'));
        System.out.println("imgKey=" + imgKey + " len=" + imgKey.length());
        System.out.println("subKey=" + subKey + " len=" + subKey.length());
        boolean login = nav.contains("\"isLogin\":true");
        System.out.println("isLogin=" + login);

        System.out.println("\n== STEP3: wbi 签名 + 搜索验证 ==");
        String mixinKey = mixinKey();
        System.out.println("mixinKey=" + mixinKey);
        Map<String, String> params = new TreeMap<>();
        params.put("keyword", "周杰伦 晴天");
        params.put("search_type", "video");
        String signed = signParams(params, mixinKey);
        String search = get("https://api.bilibili.com/x/web-interface/search/type?" + signed);
        String code = extractJson(search, "code");
        System.out.println("search code=" + code + " numResults=" + search.split("\"bvid\"").length);
        String bvid = extractFirst(search, "\"bvid\":\"", "\"");
        System.out.println("first bvid=" + bvid);

        System.out.println("\n== STEP4: view 获取 cid ==");
        String view = get("https://api.bilibili.com/x/web-interface/view?bvid=" + bvid);
        System.out.println("view code=" + extractJson(view, "code") + " title=" + extractFirst(view, "\"title\":\"", "\""));
        String cid = extractFirst(view, "\"cid\":", ",");
        System.out.println("cid=" + cid);

        System.out.println("\n== STEP5: playurl DASH 音频流(未登录) ==");
        Map<String, String> pp = new TreeMap<>();
        pp.put("bvid", bvid);
        pp.put("cid", cid);
        pp.put("fnval", "16");
        pp.put("fourk", "1");
        pp.put("platform", "html5");
        pp.put("qn", "0");
        String play = get("https://api.bilibili.com/x/player/wbi/playurl?" + signParams(pp, mixinKey));
        System.out.println("playurl code=" + extractJson(play, "code") + " quality=" + extractJson(play, "quality"));
        int audioCount = play.split("\"audio\":").length - 1;
        System.out.println("dash.audio 节点数=" + audioCount);
        String audioUrl = extractFirst(play, "\"id\":30216,", "\"baseUrl\":\"");
        if (audioUrl.isEmpty()) audioUrl = extractFirst(play, "\"audio\":[{", "\"baseUrl\":\"");
        System.out.println("audio baseUrl=" + audioUrl);

        System.out.println("\n== STEP6: 音频流下载验证(带Referer) ==");
        if (!audioUrl.isEmpty()) {
            HttpURLConnection c = openConn(audioUrl, true);
            System.out.println("HTTP " + c.getResponseCode() + " content-type=" + c.getContentType() + " content-length=" + c.getContentLengthLong());
            c.disconnect();
        }

        System.out.println("\n== STEP7: 扫码登录二维码生成 ==");
        String qr = get("https://passport.bilibili.com/x/passport-login/web/qrcode/generate");
        System.out.println(qr.substring(0, Math.min(400, qr.length())));
        System.out.println("\nALL DONE");
    }

    static String mixinKey() {
        String joined = imgKey + subKey;
        int[] tab = (joined.length() > 64) ? MIXIN_TAB_V2 : MIXIN_TAB_V1;
        StringBuilder sb = new StringBuilder();
        for (int i : tab) {
            if (i < joined.length()) sb.append(joined.charAt(i));
            if (sb.length() == 32) break;
        }
        return sb.toString();
    }

    static String signParams(Map<String, String> params, String mixinKey) throws Exception {
        params.put("wts", String.valueOf(System.currentTimeMillis() / 1000));
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : new TreeMap<>(params).entrySet()) {
            String v = e.getValue().replaceAll("[!'()*]", "");
            if (sb.length() > 0) sb.append('&');
            sb.append(URLEncoder.encode(e.getKey(), "UTF-8"))
              .append('=')
              .append(URLEncoder.encode(v, "UTF-8"));
        }
        String query = sb.toString();
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] d = md.digest((query + mixinKey).getBytes(StandardCharsets.UTF_8));
        StringBuilder wRid = new StringBuilder();
        for (byte b : d) wRid.append(String.format("%02x", b));
        return query + "&w_rid=" + wRid;
    }

    static String get(String url) throws Exception {
        HttpURLConnection c = openConn(url, false);
        try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
            return r.lines().collect(Collectors.joining());
        } finally {
            c.disconnect();
        }
    }

    static HttpURLConnection openConn(String url, boolean withReferer) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestProperty("User-Agent", UA);
        if (buvid3 != null && !buvid3.isEmpty()) c.setRequestProperty("Cookie", "buvid3=" + buvid3);
        if (withReferer) c.setRequestProperty("Referer", "https://www.bilibili.com/");
        c.setConnectTimeout(10000);
        c.setReadTimeout(15000);
        return c;
    }

    static String extractJson(String s, String key) {
        String p = "\"" + key + "\":\"";
        int i = s.indexOf(p);
        if (i < 0) return "";
        i += p.length();
        int j = s.indexOf('"', i);
        return j < 0 ? "" : s.substring(i, j);
    }

    static String extractFirst(String s, String pre, String suf) {
        int i = s.indexOf(pre);
        if (i < 0) return "";
        i += pre.length();
        int j = s.indexOf(suf, i);
        return j < 0 ? "" : s.substring(i, j);
    }
}
