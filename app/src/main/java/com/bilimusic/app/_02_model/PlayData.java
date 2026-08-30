package com.bilimusic.app._02_model;

import java.util.List;

/** 播放地址（player/playurl fnval=16 DASH） */
public class PlayData {
    public int quality;
    public long timelength;
    public List<Integer> accept_quality;
    public Dash dash;
    public List<DurlItem> durl;

    public static class Dash {
        public long duration;
        public List<AudioStream> audio;
        public Flac flac;
    }

    public static class AudioStream {
        public int id;
        public String baseUrl;
        public String base_url;
        public List<String> backupUrl;
        public List<String> backup_url;
        public long bandwidth;
        public String codecs;

        public String primaryUrl() {
            return baseUrl != null && !baseUrl.isEmpty() ? baseUrl : base_url;
        }

        public List<String> backups() {
            return backupUrl != null && !backupUrl.isEmpty() ? backupUrl : backup_url;
        }
    }

    public static class Flac {
        public AudioStream audio;
    }

    public static class DurlItem {
        public String url;
        public long length;
        public long size;
    }
}
