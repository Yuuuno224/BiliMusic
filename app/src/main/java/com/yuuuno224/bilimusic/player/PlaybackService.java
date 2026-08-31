package com.yuuuno224.bilimusic.player;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;
import androidx.media3.session.SessionCommand;
import androidx.media3.session.SessionCommands;
import androidx.media3.session.SessionResult;

import com.yuuuno224.bilimusic.store.MusicStore;
import com.yuuuno224.bilimusic.store.Song;
import com.yuuuno224.bilimusic.ui.NowPlayingActivity;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 播放服务：持有 ExoPlayer 与 MediaSession。
 * 队列由服务端管理：收到 PLAY_QUEUE 后解析当前曲播放，其余曲目后台解析后逐个插入播放器队列，
 * 保证通知栏上一首/下一首可用，并支持自动连播。
 */
public class PlaybackService extends MediaSessionService {

    public static final String CMD_PLAY_QUEUE = "bilimusic.PLAY_QUEUE";
    public static final String CMD_PLAY_NEXT = "bilimusic.PLAY_NEXT";
    public static final String CMD_ADD_QUEUE = "bilimusic.ADD_QUEUE";
    public static final String KEY_SONGS = "songs";
    public static final String KEY_INDEX = "index";

    private static final Gson GSON = new Gson();
    private static final Type SONG_LIST = new TypeToken<List<Song>>() { }.getType();

    private MediaSession session;
    private ExoPlayer player;

    @Override
    public void onCreate() {
        super.onCreate();

        Map<String, String> headers = new HashMap<>();
        headers.put("Referer", "https://www.bilibili.com/");
        DefaultHttpDataSource.Factory httpFactory =
                new DefaultHttpDataSource.Factory()
                        .setUserAgent(com.yuuuno224.bilimusic.net.BiliHttp.UA)
                        .setDefaultRequestProperties(headers)
                        .setAllowCrossProtocolRedirects(true)
                        .setConnectTimeoutMs(10000)
                        .setReadTimeoutMs(15000);

        player = new ExoPlayer.Builder(this)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(httpFactory))
                .setAudioAttributes(
                        new AudioAttributes.Builder()
                                .setUsage(C.USAGE_MEDIA)
                                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                                .build(),
                        true)
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .build();

        player.addListener(new Player.Listener() {
            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO
                        || reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK
                        || reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
                    Song song = songOf(mediaItem);
                    if (song != null) {
                        MusicStore.addHistory(song);
                    }
                }
            }
        });

        PendingIntent pi = PendingIntent.getActivity(this, 0,
                new Intent(this, NowPlayingActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        session = new MediaSession.Builder(this, player)
                .setSessionActivity(pi)
                .setCallback(new MediaSession.Callback() {
                    @Override
                    public MediaSession.ConnectionResult onConnect(
                            MediaSession session, MediaSession.ControllerInfo controller) {
                        SessionCommands cmds = new SessionCommands.Builder()
                                .add(new SessionCommand(CMD_PLAY_QUEUE, Bundle.EMPTY))
                                .add(new SessionCommand(CMD_PLAY_NEXT, Bundle.EMPTY))
                                .add(new SessionCommand(CMD_ADD_QUEUE, Bundle.EMPTY))
                                .build();
                        return MediaSession.ConnectionResult.accept(
                                cmds, MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS);
                    }

                    @Override
                    public ListenableFuture<SessionResult> onCustomCommand(
                            MediaSession mediaSession, MediaSession.ControllerInfo controller,
                            SessionCommand customCommand, Bundle args) {
                        switch (customCommand.customAction) {
                            case CMD_PLAY_QUEUE:
                                handlePlayQueue(args);
                                break;
                            case CMD_PLAY_NEXT:
                                handlePlayNext(args, true);
                                break;
                            case CMD_ADD_QUEUE:
                                handlePlayNext(args, false);
                                break;
                            default:
                                break;
                        }
                        return Futures.immediateFuture(new SessionResult(SessionResult.RESULT_SUCCESS));
                    }
                })
                .build();
    }

    private void handlePlayQueue(Bundle args) {
        List<Song> songs = GSON.fromJson(args.getString(KEY_SONGS), SONG_LIST);
        int index = args.getInt(KEY_INDEX, 0);
        if (songs == null || songs.isEmpty()) {
            return;
        }
        resolveQueueAndLoad(songs, index);
    }

    private void handlePlayNext(Bundle args, boolean asNext) {
        List<Song> songs = GSON.fromJson(args.getString(KEY_SONGS), SONG_LIST);
        if (songs == null || songs.isEmpty() || player == null) {
            return;
        }
        for (Song song : songs) {
            resolveAndAppend(song, asNext);
        }
    }

    /**
     * 播放指定队列：并行解析全部曲目（限流），按原顺序载入并从 startIndex 开始播放；
     * 解析失败的曲目从队列中剔除。
     */
    private void resolveQueueAndLoad(List<Song> songs, int startIndex) {
        if (player == null) {
            return;
        }
        player.stop();
        player.clearMediaItems();

        final List<Song> list = songs;
        RESOLVER.execute(() -> {
            MediaItem[] slots = new MediaItem[list.size()];
            java.util.concurrent.CountDownLatch latch =
                    new java.util.concurrent.CountDownLatch(list.size());
            for (int i = 0; i < list.size(); i++) {
                final int idx = i;
                final Song song = list.get(i);
                RESOLVER.execute(() -> {
                    try {
                        slots[idx] = MediaItemBuilder.build(PlaybackService.this, song);
                    } catch (Exception ignored) {
                    } finally {
                        latch.countDown();
                    }
                });
            }
            try {
                latch.await(20, java.util.concurrent.TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            List<MediaItem> items = new ArrayList<>();
            int mappedStart = 0;
            for (int i = 0; i < slots.length; i++) {
                if (slots[i] != null) {
                    if (i < startIndex) {
                        mappedStart++;
                    }
                    items.add(slots[i]);
                }
            }
            if (items.isEmpty()) {
                return;
            }
            final List<MediaItem> finalItems = items;
            final int finalStart = Math.min(mappedStart, items.size() - 1);
            runOnMain(() -> {
                if (player == null) {
                    return;
                }
                player.setMediaItems(finalItems, finalStart, 0);
                player.prepare();
                player.play();
            });
        });
    }

    /** 解析单首并追加到队列（下一首播放 / 加入队列）；空闲时自动开始播放 */
    private void resolveAndAppend(Song song, boolean asNext) {
        RESOLVER.execute(() -> {
            try {
                MediaItem item = MediaItemBuilder.build(PlaybackService.this, song);
                runOnMain(() -> {
                    if (player == null) {
                        return;
                    }
                    boolean idle = player.getPlaybackState() == Player.STATE_IDLE
                            || player.getCurrentMediaItemIndex() < 0;
                    if (asNext && player.getCurrentMediaItemIndex() >= 0) {
                        player.addMediaItem(player.getCurrentMediaItemIndex() + 1, item);
                    } else {
                        player.addMediaItem(item);
                    }
                    if (idle) {
                        player.prepare();
                        player.play();
                    }
                });
            } catch (Exception ignored) {
                // 解析失败直接丢弃
            }
        });
    }

    private static final java.util.concurrent.ExecutorService RESOLVER =
            java.util.concurrent.Executors.newFixedThreadPool(6, r -> {
                Thread t = new Thread(r, "bili-resolve");
                t.setDaemon(true);
                return t;
            });

    private static void runOnMain(Runnable r) {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(r);
    }

    static Song songOf(MediaItem item) {
        if (item == null || item.mediaMetadata == null || item.mediaMetadata.extras == null) {
            return null;
        }
        return GSON.fromJson(item.mediaMetadata.extras.getString("song"), Song.class);
    }

    @Nullable
    @Override
    public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        return session;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Player p = session != null ? session.getPlayer() : null;
        if (p == null || !p.getPlayWhenReady() || p.getMediaItemCount() == 0) {
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        if (session != null) {
            session.release();
            session = null;
        }
        if (player != null) {
            player.release();
            player = null;
        }
        super.onDestroy();
    }

    /** MediaItem 构建：解析音频地址并附元数据 */
    static final class MediaItemBuilder {
        private MediaItemBuilder() {
        }

        static MediaItem build(android.content.Context ctx, Song song) throws Exception {
            PlayDataAudio audio = resolve(ctx, song);
            MediaMetadata metadata = new MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.up)
                    .setArtworkUri(android.net.Uri.parse(song.coverLarge()))
                    .setExtras(extras(song))
                    .build();
            return new MediaItem.Builder()
                    .setMediaId(song.bvid)
                    .setUri(audio.uri)
                    .setMimeType(audio.mimeType)
                    .setMediaMetadata(metadata)
                    .build();
        }

        private static Bundle extras(Song song) {
            Bundle b = new Bundle();
            b.putString("song", GSON.toJson(song));
            return b;
        }

        /** 在无 Media3 依赖的环境下也可以解析（返回 uri+mimeType） */
        static class PlayDataAudio {
            String uri;
            String mimeType = "audio/mp4";
        }

        private static PlayDataAudio resolve(android.content.Context ctx, Song song) throws Exception {
            if (song.cid <= 0) {
                com.yuuuno224.bilimusic.model.ViewData view = com.yuuuno224.bilimusic.net.BiliApi.videoDetail(song.bvid);
                song.cid = view.cid;
                if (song.durationSec <= 0) {
                    song.durationSec = view.duration;
                }
            }
            com.yuuuno224.bilimusic.model.PlayData.AudioStream stream =
                    com.yuuuno224.bilimusic.net.BiliApi.bestAudio(song.bvid, song.cid);
            PlayDataAudio out = new PlayDataAudio();
            out.uri = stream.primaryUrl();
            // Hi-Res/FLAC 为 fLaC 容器时标记
            if (stream.codecs != null && stream.codecs.toLowerCase().contains("flac")) {
                out.mimeType = "audio/flac";
            }
            return out;
        }
    }

    /** 供 UI 静态构建命令 Bundle */
    public static Bundle queueBundle(List<Song> songs, int index) {
        Bundle b = new Bundle();
        b.putString(KEY_SONGS, GSON.toJson(songs == null ? Collections.emptyList() : songs));
        b.putInt(KEY_INDEX, index);
        return b;
    }
}
