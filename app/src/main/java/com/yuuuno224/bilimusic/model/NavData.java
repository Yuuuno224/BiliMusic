package com.yuuuno224.bilimusic.model;

public class NavData {
    public boolean isLogin;
    public long mid;
    public String uname;
    public String face;
    public LevelInfo level_info;

    public int level() {
        return level_info != null ? level_info.current_level : 0;
    }

    public static class LevelInfo {
        public int current_level;
    }
}
