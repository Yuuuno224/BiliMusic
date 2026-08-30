package com.yuuuno224.bilimusic.player;

import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionCommand;
import androidx.media3.session.SessionToken;

import com.yuuuno224.bilimusic.store.Song;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** UI 侧播放控制：单例 MediaController 封装 */
public final class PlayerConnection {

    public interface Listener {
        void onPlayerChanged();
    }

    private static MediaController controller;
    private static boolean connecting;
    private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<>();
    private static final List<java.util.function.Consumer<MediaController>> PENDING_CALLBACKS =
            new CopyOnWriteArrayList<>();
    private static final Player.Listener NOTIFY = new Player.Listener() {
        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            notifyListeners();
        }

        @Override
        public void onMediaItemTransition(@androidx.annotation.Nullable MediaItem item, int reason) {
            notifyListeners();
        }

        @Override
        public void onPlaybackStateChanged(int state) {
            notifyListeners();
        }
    };

    private PlayerConnection() {
    }

    /** 异步建立连接；并发调用时只发起一次建连，所有回调在就绪后统一触发 */
    public static void connect(Context ctx, java.util.function.Consumer<MediaController> onReady) {
        if (controller != null) {
            onReady.accept(controller);
            return;
        }
        PENDING_CALLBACKS.add(onReady);
        if (connecting) {
            return;
        }
        connecting = true;
        SessionToken token = new SessionToken(ctx, new ComponentName(ctx, PlaybackService.class));
        com.google.common.util.concurrent.ListenableFuture<MediaController> future =
                new MediaController.Builder(ctx, token).buildAsync();
        future.addListener(() -> {
            try {
                MediaController c = future.get();
                if (controller == null) {
                    controller = c;
                    controller.addListener(NOTIFY);
                } else if (controller != c) {
                    c.release();
                }
                connecting = false;
                List<java.util.function.Consumer<MediaController>> cbs =
                        new ArrayList<>(PENDING_CALLBACKS);
                PENDING_CALLBACKS.clear();
                for (java.util.function.Consumer<MediaController> cb : cbs) {
                    cb.accept(controller);
                }
                notifyListeners();
            } catch (Exception ignored) {
                connecting = false;
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

    private static void notifyListeners() {
        for (Listener l : LISTENERS) {
            l.onPlayerChanged();
        }
    }

    // ---------- 播放控制 ----------

    /** controller 未就绪时先建连，就绪后再执行 */
    private static void ensureConnected(Context ctx, Runnable action) {
        if (controller != null) {
            action.run();
            return;
        }
        connect(ctx, c -> action.run());
    }

    public static void playQueue(Context ctx, List<Song> songs, int index) {
        ensureConnected(ctx, () -> controller.sendCustomCommand(
                new SessionCommand(PlaybackService.CMD_PLAY_QUEUE, Bundle.EMPTY),
                PlaybackService.queueBundle(songs, index)));
    }

    public static void playNext(Context ctx, Song song) {
        sendSongs(ctx, PlaybackService.CMD_PLAY_NEXT, song);
    }

    public static void addToQueue(Context ctx, Song song) {
        sendSongs(ctx, PlaybackService.CMD_ADD_QUEUE, song);
    }

    private static void sendSongs(Context ctx, String action, Song song) {
        ensureConnected(ctx, () -> controller.sendCustomCommand(
                new SessionCommand(action, Bundle.EMPTY),
                PlaybackService.queueBundle(java.util.Collections.singletonList(song), -1)));
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
