package com.yuuuno224.bilimusic.net;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;

/**
 * WBI 参数签名：对 query 参数做混淆 MD5（w_rid）。
 * 索引表存在 V1(64位key) / V2(84位key) 两个历史版本，运行时按 key 长度自适应。
 */
public final class WbiSigner {

    private static final int[] TAB_V1 = {
            46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35, 27, 43, 5, 49,
            33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13, 37, 48, 7, 16, 24, 55, 40, 61,
            26, 17, 0, 1, 60, 51, 30, 4, 22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11, 36,
            20, 34, 44, 52};

    private static final int[] TAB_V2 = {
            70, 66, 29, 63, 9, 34, 78, 39, 5, 48, 20, 68, 8, 26, 23, 74, 3, 56, 24, 45,
            35, 12, 51, 67, 55, 47, 72, 2, 18, 42, 60, 31, 6, 52, 61, 62, 73, 33, 4, 22,
            13, 65, 1, 75, 44, 30, 27, 40, 64, 76, 53, 36, 25, 57, 49, 69, 50, 14, 15, 10,
            54, 58, 59, 28};

    private WbiSigner() {
    }

    /** imgKey+subKey 混淆出 32 位 mixinKey */
    public static String mixinKey(String imgKey, String subKey) {
        String joined = imgKey + subKey;
        int[] tab = joined.length() > 64 ? TAB_V2 : TAB_V1;
        StringBuilder sb = new StringBuilder(32);
        for (int idx : tab) {
            if (idx >= joined.length()) {
                break;
            }
            sb.append(joined.charAt(idx));
            if (sb.length() == 32) {
                break;
            }
        }
        return sb.toString();
    }

    /**
     * 生成带 wts/w_rid 的签名 query。
     * 流程：注入 wts 时间戳 → 按 key 字典序排列 → value 过滤 !'()* → urlEncode 拼接 →
     * 末尾接 mixinKey 计算 MD5 → 返回 "k=v&...&wts=..&w_rid=md5"
     */
    public static String sign(Map<String, String> params, String mixinKey) {
        try {
            Map<String, String> sorted = new TreeMap<>(params);
            sorted.put("wts", String.valueOf(System.currentTimeMillis() / 1000L));
            StringBuilder qs = new StringBuilder();
            for (Map.Entry<String, String> e : sorted.entrySet()) {
                String value = e.getValue().replaceAll("[!'()*]", "");
                if (qs.length() > 0) {
                    qs.append('&');
                }
                qs.append(UrlCodec.encode(e.getKey()))
                        .append('=')
                        .append(UrlCodec.encode(value));
            }
            String query = qs.toString();
            String wRid = md5Hex(query + mixinKey);
            return query + "&w_rid=" + wRid;
        } catch (Exception e) {
            throw new IllegalStateException("wbi sign failed", e);
        }
    }

    public static String md5Hex(String s) throws Exception {
        byte[] digest = MessageDigest.getInstance("MD5").digest(s.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(32);
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
