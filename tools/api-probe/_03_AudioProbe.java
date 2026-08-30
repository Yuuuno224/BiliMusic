import java.io.*;
import java.net.*;
import java.util.stream.Collectors;

/** 验证 DASH audio 流可下载 + 音质 id 枚举 */
public class _03_AudioProbe {
    static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";

    public static void main(String[] args) throws Exception {
        String page = get("https://api.bilibili.com/x/player/playurl?bvid=BV1d4411N7zD&cid=317843818&fnval=16&qn=0&fourk=1");
        int ai = page.indexOf("\"audio\":");
        String audioPart = page.substring(ai, Math.min(ai + 1200, page.length()));
        System.out.println("audio部分: " + audioPart);
        int bi = page.indexOf("\"baseUrl\":\"", ai);
        String url = page.substring(bi + 11, page.indexOf('"', bi + 11)).replace("\\u0026", "&");

        System.out.println("\naudio baseUrl: " + url.substring(0, Math.min(120, url.length())) + "...");
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestProperty("User-Agent", UA);
        c.setRequestProperty("Referer", "https://www.bilibili.com/");
        System.out.println("下载验证: HTTP " + c.getResponseCode() + " type=" + c.getContentType() + " size=" + c.getContentLengthLong() + "B");
        c.disconnect();

        HttpURLConnection c2 = (HttpURLConnection) new URL(url).openConnection();
        c2.setRequestProperty("User-Agent", UA);
        c2.setRequestProperty("Referer", "https://www.bilibili.com/");
        try (InputStream in = c2.getInputStream()) {
            byte[] head = new byte[8];
            int n = in.read(head);
            System.out.print("文件头magic: ");
            for (int i = 0; i < n; i++) System.out.printf("%02X ", head[i]);
            System.out.println(" (00 00 00 18 66 74 79 70 = ftyp/M4A ✓)");
        }
        c2.disconnect();
    }

    static String get(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestProperty("User-Agent", UA);
        c.setRequestProperty("Referer", "https://www.bilibili.com/");
        return new BufferedReader(new InputStreamReader(c.getInputStream(), java.nio.charset.StandardCharsets.UTF_8)).lines().collect(Collectors.joining());
    }
}
