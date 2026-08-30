import com.bilimusic.app._01_net.WbiSigner;
import java.util.Map;
import java.util.TreeMap;

public class _04_WbiVerify {
    public static void main(String[] args) throws Exception {
        String img = "7cd084941338484aae1ad9425b84077c";
        String sub = "4932caff0ff746eab6f01bf08b70ac45";
        String mixin = WbiSigner.mixinKey(img, sub);
        String expected = "ea1db124af3c7062474693fa704f4ff8";
        System.out.println("mixinKey = " + mixin);
        System.out.println("expected = " + expected);
        System.out.println("MATCH = " + expected.equals(mixin));

        Map<String, String> params = new TreeMap<>();
        params.put("keyword", "周杰伦 晴天");
        params.put("search_type", "video");
        params.put("page", "1");
        String q = WbiSigner.sign(params, mixin);
        System.out.println("signed query len=" + q.length());
        System.out.println("contains w_rid=" + q.contains("&w_rid="));
        System.out.println("contains wts=" + q.contains("&wts="));
        System.out.println("encoded space ok=" + q.contains("%20"));
    }
}
