package com.yuuuno224.bilimusic.repo;

import android.os.Handler;
import android.os.Looper;

import com.yuuuno224.bilimusic.net.BiliApi;
import com.yuuuno224.bilimusic.model.FavData;
import com.yuuuno224.bilimusic.model.SearchData;
import com.yuuuno224.bilimusic.model.ViewData;
import com.yuuuno224.bilimusic.model.PlayData;
import com.yuuuno224.bilimusic.store.Song;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 音乐业务编排：搜索 → cid 解析 → 播放地址解析（串行IO，防风控） */
public final class MusicRepository {

    public interface Callback<T> {
        void onResult(T result);

        void onError(String message);
    }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "music-io");
        t.setDaemon(true);
        return t;
    });

    private MusicRepository() {
    }

    public static void search(String keyword, final Callback<List<Song>> cb) {
        IO.execute(() -> {
            try {
                SearchData data = BiliApi.searchVideos(keyword, 1);
                List<Song> songs = new ArrayList<>();
                if (data != null && data.result != null) {
                    for (SearchData.SearchItem item : data.result) {
                        if (item.bvid == null || item.bvid.isEmpty()) {
                            continue;
                        }
                        Song s = new Song(item.bvid, stripHtml(item.title), item.author, item.pic,
                                parseDuration(item.duration));
                        s.cover = item.pic;
                        songs.add(s);
                    }
                }
                MAIN.post(() -> cb.onResult(songs));
            } catch (Exception e) {
                MAIN.post(() -> cb.onError(e.getMessage() == null ? "搜索失败" : e.getMessage()));
            }
        });
    }

    /** 加载用户收藏夹列表（需要登录） */
    public static void favFolders(long mid, final Callback<List<FavData.Folder>> cb) {
        IO.execute(() -> {
            try {
                FavData.FolderList data = BiliApi.favFolders(mid);
                List<FavData.Folder> folders = new ArrayList<>();
                if (data != null && data.list != null) {
                    folders.addAll(data.list);
                }
                MAIN.post(() -> cb.onResult(folders));
            } catch (Exception e) {
                MAIN.post(() -> cb.onError(e.getMessage() == null ? "获取收藏夹失败" : e.getMessage()));
            }
        });
    }

    /** 加载收藏夹内全部视频（自动翻页，上限 10 页） */
    public static void favResources(long mediaId, final Callback<List<Song>> cb) {
        IO.execute(() -> {
            try {
                List<Song> songs = new ArrayList<>();
                for (int page = 1; page <= 10; page++) {
                    FavData.ResourceList data = BiliApi.favResources(mediaId, page);
                    if (data.medias == null || data.medias.isEmpty()) {
                        break;
                    }
                    for (FavData.Resource r : data.medias) {
                        if (r.bvid == null || r.bvid.isEmpty()) {
                            continue;
                        }
                        String up = r.upper != null ? r.upper.name : "";
                        Song s = new Song(r.bvid, stripHtml(r.title), up, r.cover, r.duration);
                        s.cid = r.cid;
                        songs.add(s);
                    }
                    if (!data.has_more) {
                        break;
                    }
                }
                MAIN.post(() -> cb.onResult(songs));
            } catch (Exception e) {
                MAIN.post(() -> cb.onError(e.getMessage() == null ? "获取收藏内容失败" : e.getMessage()));
            }
        });
    }

    /** 解析 cid（搜索结果不携带 cid，需二次请求视频详情） */
    public static void ensureCid(final Song song, final Callback<Song> cb) {
        if (song.cid > 0) {
            cb.onResult(song);
            return;
        }
        IO.execute(() -> {
            try {
                ViewData view = BiliApi.videoDetail(song.bvid);
                song.cid = view.cid;
                if (song.durationSec <= 0) {
                    song.durationSec = view.duration;
                }
                MAIN.post(() -> cb.onResult(song));
            } catch (Exception e) {
                MAIN.post(() -> cb.onError(e.getMessage() == null ? "解析失败" : e.getMessage()));
            }
        });
    }

    /** 解析最佳音频流地址 */
    public static void resolveAudio(final Song song, final Callback<PlayData.AudioStream> cb) {
        ensureCid(song, new Callback<Song>() {
            @Override
            public void onResult(Song s) {
                IO.execute(() -> {
                    try {
                        PlayData.AudioStream audio = BiliApi.bestAudio(s.bvid, s.cid);
                        MAIN.post(() -> cb.onResult(audio));
                    } catch (Exception e) {
                        MAIN.post(() -> cb.onError(e.getMessage() == null ? "获取音频失败" : e.getMessage()));
                    }
                });
            }

            @Override
            public void onError(String message) {
                cb.onError(message);
            }
        });
    }

    private static String stripHtml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("<em class=\"keyword\">", "")
                .replace("</em>", "")
                .replace("&quot;", "\"")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("\n", " ");
    }

    private static long parseDuration(String d) {
        if (d == null || d.isEmpty()) {
            return 0;
        }
        try {
            String[] parts = d.split(":");
            long sec = 0;
            for (String p : parts) {
                sec = sec * 60 + Long.parseLong(p.trim());
            }
            return sec;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
