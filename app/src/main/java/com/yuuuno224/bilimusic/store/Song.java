package com.yuuuno224.bilimusic.store;

import java.io.Serializable;

/** 统一歌曲模型：来自搜索结果 / 收藏 / 历史 */
public class Song implements Serializable {
    public String bvid;
    public long cid;
    public String title;
    public String up;
    public String cover;
    public long durationSec;
    public long addedAt;

    public Song() {
    }

    public Song(String bvid, String title, String up, String cover, long durationSec) {
        this.bvid = bvid;
        this.title = title;
        this.up = up;
        this.cover = cover;
        this.durationSec = durationSec;
    }

    public String coverUrl() {
        if (cover == null) {
            return "";
        }
        if (cover.startsWith("//")) {
            return "https:" + cover;
        }
        return cover;
    }

    /** 列表封面裁剪为小图，加快加载 */
    public String coverSmall() {
        String url = coverUrl();
        if (url.contains("@")) {
            return url;
        }
        return url + "@320w_200h.webp";
    }

    public String coverLarge() {
        String url = coverUrl();
        if (url.contains("@")) {
            return url;
        }
        return url + "@672w_378h.webp";
    }

    public String durationText() {
        if (durationSec <= 0) {
            return "--:--";
        }
        long m = durationSec / 60;
        long s = durationSec % 60;
        return String.format("%d:%02d", m, s);
    }

    public static String durationText(long sec) {
        long m = sec / 60;
        long s = sec % 60;
        return String.format("%d:%02d", m, s);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Song)) {
            return false;
        }
        return bvid != null && bvid.equals(((Song) o).bvid);
    }

    @Override
    public int hashCode() {
        return bvid == null ? 0 : bvid.hashCode();
    }
}
