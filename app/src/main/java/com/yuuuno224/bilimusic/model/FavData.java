package com.yuuuno224.bilimusic.model;

import java.util.List;

/** 收藏夹接口数据模型 */
public class FavData {

    /** 收藏夹列表响应 data（list-all） */
    public static class FolderList {
        public List<Folder> list;
    }

    /** 收藏夹 */
    public static class Folder {
        public long id;
        public String title;
        public long media_count;
        public String cover;
    }

    /** 收藏夹内容响应 data（resource/list） */
    public static class ResourceList {
        public List<Resource> medias;
        public boolean has_more;
    }

    /** 收藏夹内容条目（type=2 视频） */
    public static class Resource {
        public long id;
        public String bvid;
        public String title;
        public String cover;
        public long duration;
        public long cid;
        public Upper upper;
    }

    public static class Upper {
        public String name;
    }
}
