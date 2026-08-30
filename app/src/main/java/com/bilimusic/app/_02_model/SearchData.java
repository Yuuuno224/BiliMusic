package com.bilimusic.app._02_model;

import java.util.List;

/** web 接口搜索结果（search_type=video） */
public class SearchData {
    public String seid;
    public List<SearchItem> result;

    public static class SearchItem {
        public String bvid;
        public String title;
        public String description;
        public String author;
        public long mid;
        public String duration;
        public String pic;
        public long play;
        public long danmaku;
        public long favorites;
        public long pubdate;
    }
}
