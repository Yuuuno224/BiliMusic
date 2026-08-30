import java.io.*;
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;

/** playurl 接口细节探测：比较不同参数组合 */
public class _02_PlayUrlProbe {
    static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";
    static String buvid3 = "";

    public static void main(String[] args) throws Exception {
        String spi = get("https://api.bilibili.com/x/frontend/finger/spi");
        buvid3 = between(spi, "\"b_3\":\"", "\"");

        String bvid = "BV1d4411N7zD";
        String cid = "317843818";

        String[][] cases = {
            {"x/player/wbi/playurl fnval16", "https://api.bilibili.com/x/player/wbi/playurl?bvid=%s&cid=%s&fnval=16&qn=0&fourk=1"},
            {"x/player/playurl fnval16", "https://api.bilibili.com/x/player/playurl?bvid=%s&cid=%s&fnval=16&qn=0&fourk=1"},
            {"x/player/playurl fnval404", "https://api.bilibili.com/x/player/playurl?bvid=%s&cid=%s&fnval=4048&qn=0"},
            {"x/v2 dmview", "https://api.bilibili.com/x/player/playurl?bvid=%s&cid=%s&qn=0&type=&otype=json&fnver=0&fnval=16&platform=html5&high_quality=1"},
        };
        for (String[] c : cases) {
            String url = String.format(c[1], bvid, cid);
            System.out.println("\n---- " + c[0] + " ----");
            String resp = get(url);
            System.out.println(resp.length() > 600 ? resp.substring(0, 600) : resp);
        }
    }

    static String get(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestProperty("User-Agent", UA);
        c.setRequestProperty("Referer", "https://www.bilibili.com/");
        c.setRequestProperty("Cookie", "buvid3=" + buvid3);
        c.setConnectTimeout(10000);
        int code = c.getResponseCode();
        InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
        String body = "";
        if (in != null) body = new BufferedReader(new InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).lines().collect(Collectors.joining());
        return "[HTTP " + code + "] " + body;
    }

    static String between(String s, String a, String b) {
        int i = s.indexOf(a);
        if (i < 0) return "";
        i += a.length();
        int j = s.indexOf(b, i);
        return j < 0 ? "" : s.substring(i, j);
    }
}
