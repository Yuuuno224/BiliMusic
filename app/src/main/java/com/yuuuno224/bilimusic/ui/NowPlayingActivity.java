package com.yuuuno224.bilimusic.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.yuuuno224.bilimusic.R;
import com.yuuuno224.bilimusic.store.MusicStore;
import com.yuuuno224.bilimusic.store.Song;
import com.yuuuno224.bilimusic.player.PlayerConnection;
import com.yuuuno224.bilimusic.util.ImageLoader;

/** 沉浸式播放页：大封面 + 进度 + 控制区 */
public class NowPlayingActivity extends AppCompatActivity implements PlayerConnection.Listener {

    private ImageView cover;
    private TextView title;
    private TextView up;
    private TextView curTime;
    private TextView totalTime;
    private SeekBar seekBar;
    private ImageButton playPause;
    private ImageButton modeBtn;
    private ImageButton favBtn;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean seeking;
    private Toast modeToast;

    private final Runnable progressTick = new Runnable() {
        @Override
        public void run() {
            androidx.media3.common.Player p = PlayerConnection.get();
            if (p != null && p.getPlaybackState() != androidx.media3.common.Player.STATE_IDLE) {
                if (!seeking) {
                    long pos = p.getCurrentPosition();
                    long dur = Math.max(p.getDuration(), 1);
                    seekBar.setProgress((int) (pos * 1000 / dur));
                    curTime.setText(Song.durationText(pos / 1000));
                    totalTime.setText(Song.durationText(dur / 1000));
                }
            }
            handler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_now_playing);

        cover = findViewById(R.id.np_cover);
        title = findViewById(R.id.np_title);
        up = findViewById(R.id.np_up);
        curTime = findViewById(R.id.np_cur);
        totalTime = findViewById(R.id.np_total);
        seekBar = findViewById(R.id.np_seek);
        playPause = findViewById(R.id.np_play_pause);
        ImageButton prev = findViewById(R.id.np_prev);
        ImageButton next = findViewById(R.id.np_next);
        modeBtn = findViewById(R.id.np_mode);
        favBtn = findViewById(R.id.np_fav);
        ImageButton close = findViewById(R.id.np_close);

        close.setOnClickListener(v -> finish());
        playPause.setOnClickListener(v -> PlayerConnection.toggle());
        next.setOnClickListener(v -> PlayerConnection.next());
        prev.setOnClickListener(v -> PlayerConnection.prev());
        findViewById(R.id.np_playlist).setOnClickListener(v -> showPlaylist());

        modeBtn.setOnClickListener(v -> {
            androidx.media3.common.Player p = PlayerConnection.get();
            if (p == null) {
                return;
            }
            int cur = p.getRepeatMode();
            int nextMode = cur == androidx.media3.common.Player.REPEAT_MODE_OFF
                    ? androidx.media3.common.Player.REPEAT_MODE_ALL
                    : cur == androidx.media3.common.Player.REPEAT_MODE_ALL
                    ? androidx.media3.common.Player.REPEAT_MODE_ONE
                    : androidx.media3.common.Player.REPEAT_MODE_OFF;
            p.setRepeatMode(nextMode);
            applyRepeatUi();
            String label = nextMode == androidx.media3.common.Player.REPEAT_MODE_ALL
                    ? "列表循环" : nextMode == androidx.media3.common.Player.REPEAT_MODE_ONE
                    ? "单曲循环" : "顺序播放";
            if (modeToast != null) {
                modeToast.cancel();
            }
            modeToast = Toast.makeText(this, label, Toast.LENGTH_SHORT);
            modeToast.show();
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar sb) {
                seeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar sb) {
                seeking = false;
                androidx.media3.common.Player p = PlayerConnection.get();
                if (p != null) {
                    long dur = Math.max(p.getDuration(), 1);
                    p.seekTo(sb.getProgress() * dur / 1000);
                }
            }
        });

        favBtn.setOnClickListener(v -> {
            Song song = PlayerConnection.currentSong();
            if (song != null) {
                MusicStore.toggleFavorite(song);
                refreshFav(song);
                Toast.makeText(this, MusicStore.isFavorite(song.bvid) ? "已收藏" : "已取消收藏",
                        Toast.LENGTH_SHORT).show();
            }
        });

        PlayerConnection.connect(this, c -> {
            runOnUiThread(() -> {
                PlayerConnection.addListener(this);
                onPlayerChanged();
                applyRepeatUi();
            });
        });
        handler.post(progressTick);
    }

    /** 以播放器实际 repeatMode 同步循环按钮 UI */
    private void applyRepeatUi() {
        androidx.media3.common.Player p = PlayerConnection.get();
        int rm = p != null ? p.getRepeatMode() : androidx.media3.common.Player.REPEAT_MODE_OFF;
        if (rm == androidx.media3.common.Player.REPEAT_MODE_ONE) {
            modeBtn.setImageResource(R.drawable.ic_repeat_one);
            modeBtn.setColorFilter(androidx.core.content.ContextCompat.getColor(this, R.color.accent));
        } else if (rm == androidx.media3.common.Player.REPEAT_MODE_ALL) {
            modeBtn.setImageResource(R.drawable.ic_repeat);
            modeBtn.setColorFilter(androidx.core.content.ContextCompat.getColor(this, R.color.accent));
        } else {
            modeBtn.setImageResource(R.drawable.ic_order);
            modeBtn.setColorFilter(
                    androidx.core.content.ContextCompat.getColor(this, R.color.accent));
        }
    }

    @Override
    public void onPlayerChanged() {
        Song song = PlayerConnection.currentSong();
        if (song == null) {
            title.setText("尚未播放");
            up.setText("");
            cover.setImageResource(R.drawable.ic_music_note);
            return;
        }
        title.setText(song.title);
        up.setText(song.up);
        ImageLoader.load(song.coverLarge(), bmp -> {
            if (bmp != null) {
                cover.setImageBitmap(bmp);
            } else {
                cover.setImageResource(R.drawable.ic_music_note);
            }
        });
        playPause.setImageResource(PlayerConnection.isPlaying()
                ? R.drawable.ic_pause : R.drawable.ic_play);
        refreshFav(song);
    }

    private void refreshFav(Song song) {
        boolean fav = MusicStore.isFavorite(song.bvid);
        favBtn.setImageResource(fav ? R.drawable.ic_heart_filled : R.drawable.ic_heart);
        favBtn.setColorFilter(androidx.core.content.ContextCompat.getColor(this,
                fav ? R.color.heart_red : R.color.on_bg_secondary));
    }

    /** 弹出播放队列 BottomSheet */
    private void showPlaylist() {
        PlaylistDialog.show(this);
    }

    @Override
    protected void onDestroy() {
        PlayerConnection.removeListener(this);
        handler.removeCallbacks(progressTick);
        super.onDestroy();
    }
}
