package com.yuuuno224.bilimusic.net;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

final class UrlCodec {

    private UrlCodec() {
    }

    /** 与 JS encodeURIComponent 行为一致：空格编码为 %20 */
    static String encode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8")
                    .replace("+", "%20")
                    .replace("%7E", "~");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
