package com.yuuuno224.bilimusic.model;

/** 二维码轮询 data 部分：data.code 表示登录状态 86101=未扫码 86090=已扫码未确认 86038=已过期 0=成功 */
public class QrPollData {
    public String url;
    public String refresh_token;
    public long timestamp;
    public int code;

    public static final int ST_WAITING = 86101;
    public static final int ST_SCANNED = 86090;
    public static final int ST_EXPIRED = 86038;
    public static final int ST_SUCCESS = 0;
}
