package com.yuuuno224.bilimusic.model;

import java.util.List;

/** 推荐数据：热门搜索词 + 音乐区排行榜（共用响应外壳） */
public class RecommendData {

    public Trending trending;
    public List<RankItem> list;

    public static class Trending {
        public List<HotItem> list;
    }

    public static class HotItem {
        public String keyword;
        public String show_name;
    }

    public static class RankItem {
        public String bvid;
        public String title;
        public String pic;
        public String author;
        public String duration;
    }

    public static class Owner {
        public String name;
    }
}
