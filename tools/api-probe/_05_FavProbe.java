import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 收藏夹接口探测：
 * 1) x/v3/fav/folder/created/list（分页版）是否带 cover 字段
 * 2) x/v3/fav/resource/list 不带 wbi vs 带 wbi 的返回 code 对比
 * 运行: java _05_FavProbe.java
 */
public class _05_FavProbe {

    static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";
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
        System.out.println("== STEP1: nav 获取 wbi keys ==");
        String nav = get("https://api.bilibili.com/x/web-interface/nav");
        String imgUrl = extractJson(nav, "img_url");
        String subUrl = extractJson(nav, "sub_url");
        imgKey = imgUrl.substring(imgUrl.lastIndexOf('/') + 1, imgUrl.lastIndexOf('.'));
        subKey = subUrl.substring(subUrl.lastIndexOf('/') + 1, subUrl.lastIndexOf('.'));
        String mixinKey = mixinKey();
        System.out.println("mixinKey=" + mixinKey);

        String[] mids = {"2", "946974", "8047632", "220893216", "9824966"};
        String folderId = null;

        for (String mid : mids) {
            System.out.println("\n== STEP2: folder/created/list (分页版, mid=" + mid + ") 不带 wbi ==");
            String url = "https://api.bilibili.com/x/v3/fav/folder/created/list?up_mid=" + mid + "&pn=1&ps=5";
            String body = get(url);
            System.out.println("code=" + extractNum(body, "\"code\":") + " listCount=" + body.split("\"id\":").length);
            String cover = extractJson(body, "cover");
            System.out.println("first cover=" + (cover.isEmpty() ? "(无字段或为空)" : cover));
            if (cover.isEmpty()) {
                System.out.println("body snippet: " + body.substring(0, Math.min(500, body.length())));
            }
            if (folderId == null) {
                folderId = extractFolderId(body);
                if (folderId != null) {
                    System.out.println("取 folder id=" + folderId + " (mid=" + mid + ")");
                }
            }
        }

        if (folderId == null) {
            System.out.println("\n没有找到公开收藏夹，无法继续测 resource/list");
            return;
        }

        System.out.println("\n== STEP3: resource/list 不带 wbi ==");
        String u1 = "https://api.bilibili.com/x/v3/fav/resource/list?media_id=" + folderId
                + "&pn=1&ps=20&order=mtime&type=2&platform=web";
        String b1 = get(u1);
        System.out.println("code=" + extractNum(b1, "\"code\":") + " message=" + extractJson(b1, "message"));
        System.out.println("medias=" + (b1.split("\"bvid\"").length - 1) + " bvidCount");

        System.out.println("\n== STEP4: resource/list 带 wbi ==");
        Map<String, String> p2 = new TreeMap<>();
        p2.put("media_id", folderId);
        p2.put("pn", "1");
        p2.put("ps", "20");
        p2.put("order", "mtime");
        p2.put("type", "2");
        p2.put("platform", "web");
        String u2 = "https://api.bilibili.com/x/v3/fav/resource/list?" + signParams(p2, mixinKey);
        String b2 = get(u2);
        System.out.println("code=" + extractNum(b2, "\"code\":") + " message=" + extractJson(b2, "message"));
        System.out.println("medias=" + (b2.split("\"bvid\"").length - 1) + " bvidCount");
        String bvid = extractFirst(b2, "\"bvid\":\"", "\"");
        String title = extractFirst(b2, "\"title\":\"", "\"");
        String rcover = extractJson(b2, "cover");
        String dur = extractFirst(b2, "\"duration\":", ",");
        System.out.println("first: bvid=" + bvid + " title=" + title + " cover=" + rcover + " duration=" + dur);
        System.out.println("\nALL DONE");
    }

    /** 从 folder/list 响应中提取第一个收藏夹 id */
    static String extractFolderId(String body) {
        int i = body.indexOf("\"list\":[");
        if (i < 0) return null;
        String seg = body.substring(i);
        String p = "\"id\":";
        int j = seg.indexOf(p);
        if (j < 0) return null;
        j += p.length();
        int k = j;
        while (k < seg.length() && Character.isDigit(seg.charAt(k))) k++;
        return seg.substring(j, k);
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
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestProperty("User-Agent", UA);
        c.setRequestProperty("Referer", "https://www.bilibili.com/");
        c.setConnectTimeout(10000);
        c.setReadTimeout(15000);
        try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
            return r.lines().collect(Collectors.joining());
        } finally {
            c.disconnect();
        }
    }

    static String extractJson(String s, String key) {
        String p = "\"" + key + "\":\"";
        int i = s.indexOf(p);
        if (i < 0) return "";
        i += p.length();
        int j = s.indexOf('"', i);
        return j < 0 ? "" : s.substring(i, j);
    }

    static String extractNum(String s, String key) {
        int i = s.indexOf(key);
        if (i < 0) return "?";
        i += key.length();
        int j = i;
        while (j < s.length() && (Character.isDigit(s.charAt(j)) || s.charAt(j) == '-')) j++;
        return s.substring(i, j);
    }

    static String extractFirst(String s, String pre, String suf) {
        int i = s.indexOf(pre);
        if (i < 0) return "";
        i += pre.length();
        int j = s.indexOf(suf, i);
        return j < 0 ? "" : s.substring(i, j);
    }
}
