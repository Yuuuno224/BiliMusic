package com.bilimusic.app._05_player;

import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionCommand;
import androidx.media3.session.SessionToken;

import com.bilimusic.app._03_store.Song;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** UI 侧播放控制：单例 MediaController 封装 */
public final class PlayerConnection {

    public interface Listener {
        void onPlayerChanged();
    }

    private static MediaController controller;
    private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<>();
    private static final Player.Listener NOTIFY = new Player.Listener() {
        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            notifyAll();
        }

        @Override
        public void onMediaItemTransition(@androidx.annotation.Nullable MediaItem item, int reason) {
            notifyAll();
        }

        @Override
        public void onPlaybackStateChanged(int state) {
            notifyAll();
        }
    };

    private PlayerConnection() {
    }

    /** 异步建立连接 */
    public static void connect(Context ctx, java.util.function.Consumer<MediaController> onReady) {
        if (controller != null) {
            onReady.accept(controller);
            return;
        }
        SessionToken token = new SessionToken(ctx, new ComponentName(ctx, PlaybackService.class));
        com.google.common.util.concurrent.ListenableFuture<MediaController> future =
                new MediaController.Builder(ctx, token).buildAsync();
        future.addListener(() -> {
            try {
                MediaController c = future.get();
                if (controller != null && controller != c) {
                    c.release();
                    return;
                }
                controller = c;
                controller.addListener(NOTIFY);
                onReady.accept(c);
                notifyAll();
            } catch (Exception ignored) {
            }
        }, Runnable::run);
    }

    public static MediaController get() {
        return controller;
    }

    public static void addListener(Listener l) {
        LISTENERS.add(l);
    }

    public static void removeListener(Listener l) {
        LISTENERS.remove(l);
    }

    private static void notifyAll() {
        for (Listener l : LISTENERS) {
            l.onPlayerChanged();
        }
    }

    // ---------- 播放控制 ----------

    public static void playQueue(List<Song> songs, int index) {
        if (controller == null) {
            return;
        }
        controller.sendCustomCommand(
                new SessionCommand(PlaybackService.CMD_PLAY_QUEUE, Bundle.EMPTY),
                PlaybackService.queueBundle(songs, index));
    }

    public static void playNext(Song song) {
        sendSongs(PlaybackService.CMD_PLAY_NEXT, song);
    }

    public static void addToQueue(Song song) {
        sendSongs(PlaybackService.CMD_ADD_QUEUE, song);
    }

    private static void sendSongs(String action, Song song) {
        if (controller == null) {
            return;
        }
        controller.sendCustomCommand(
                new SessionCommand(action, Bundle.EMPTY),
                PlaybackService.queueBundle(java.util.Collections.singletonList(song), -1));
    }

    public static void toggle() {
        Player p = controller;
        if (p == null) {
            return;
        }
        if (p.isPlaying()) {
            p.pause();
        } else {
            p.play();
        }
    }

    public static void next() {
        if (controller != null) {
            controller.seekToNextMediaItem();
        }
    }

    public static void prev() {
        if (controller != null) {
            controller.seekToPreviousMediaItem();
        }
    }

    public static Song currentSong() {
        MediaController p = controller;
        if (p == null) {
            return null;
        }
        return PlaybackService.songOf(p.getCurrentMediaItem());
    }

    public static boolean isPlaying() {
        return controller != null && controller.isPlaying();
    }

    public static void release(@NonNull Context ctx) {
        if (controller != null) {
            controller.release();
            controller = null;
        }
    }
}
