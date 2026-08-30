package com.bilimusic.app._03_store;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** 本地数据：收藏歌单 / 播放历史 / 搜索历史（JSON 文件存储） */
public final class MusicStore {

    private static final Gson GSON = new Gson();
    private static final int MAX_HISTORY = 200;
    private static final int MAX_SEARCH_HISTORY = 10;

    private static List<Song> favorites = new ArrayList<>();
    private static List<Song> history = new ArrayList<>();
    private static Deque<String> searchHistory = new ArrayDeque<>();

    private static File file;
    private static boolean loaded;

    private MusicStore() {
    }

    public static void init(Context ctx) {
        file = new File(ctx.getFilesDir(), "music_store.json");
        load();
    }

    private static synchronized void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        if (file == null || !file.exists()) {
            return;
        }
        try (Reader r = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            StoreData data = GSON.fromJson(r, StoreData.class);
            if (data != null) {
                favorites = data.favorites != null ? data.favorites : new ArrayList<>();
                history = data.history != null ? data.history : new ArrayList<>();
                searchHistory = new ArrayDeque<>(data.searchHistory != null ? data.searchHistory : new ArrayList<>());
            }
        } catch (Exception ignored) {
        }
    }

    private static synchronized void save() {
        try (Writer w = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            GSON.toJson(new StoreData(favorites, history, new ArrayList<>(searchHistory)), w);
        } catch (IOException ignored) {
        }
    }

    // ---------- 收藏 ----------

    public static synchronized boolean isFavorite(String bvid) {
        for (Song s : favorites) {
            if (s.bvid.equals(bvid)) {
                return true;
            }
        }
        return false;
    }

    public static synchronized void toggleFavorite(Song song) {
        if (isFavorite(song.bvid)) {
            favorites.remove(song);
        } else {
            favorites.remove(song);
            song.addedAt = System.currentTimeMillis();
            favorites.add(0, song);
        }
        save();
    }

    public static synchronized List<Song> favorites() {
        return new ArrayList<>(favorites);
    }

    // ---------- 播放历史 ----------

    public static synchronized void addHistory(Song song) {
        history.remove(song);
        song.addedAt = System.currentTimeMillis();
        history.add(0, song);
        while (history.size() > MAX_HISTORY) {
            history.remove(history.size() - 1);
        }
        save();
    }

    public static synchronized List<Song> history() {
        return new ArrayList<>(history);
    }

    public static synchronized void clearHistory() {
        history.clear();
        save();
    }

    // ---------- 搜索历史 ----------

    public static synchronized void addSearchHistory(String keyword) {
        searchHistory.remove(keyword);
        searchHistory.addFirst(keyword);
        while (searchHistory.size() > MAX_SEARCH_HISTORY) {
            searchHistory.removeLast();
        }
        save();
    }

    public static synchronized List<String> searchHistory() {
        return new ArrayList<>(searchHistory);
    }

    public static synchronized void clearSearchHistory() {
        searchHistory.clear();
        save();
    }

    private static class StoreData {
        List<Song> favorites;
        List<Song> history;
        List<String> searchHistory;

        StoreData(List<Song> f, List<Song> h, List<String> s) {
            favorites = f;
            history = h;
            searchHistory = s;
        }
    }
}
