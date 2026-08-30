package com.bilimusic.app._02_model;

import java.util.List;

/** 视频详情（web-interface/view） */
public class ViewData {
    public String bvid;
    public long aid;
    public long cid;
    public String title;
    public String pic;
    public long duration;
    public Owner owner;
    public List<Page> pages;

    public static class Owner {
        public long mid;
        public String name;
        public String face;
    }

    public static class Page {
        public long cid;
        public int page;
        public String part;
        public int duration;
    }
}
