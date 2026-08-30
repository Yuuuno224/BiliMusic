package com.yuuuno224.bilimusic.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
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

    private static final int MODE_ORDER = 0;
    private static final int MODE_REPEAT_ONE = 1;

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
    private int mode = MODE_ORDER;

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

        modeBtn.setOnClickListener(v -> {
            mode = mode == MODE_ORDER ? MODE_REPEAT_ONE : MODE_ORDER;
            androidx.media3.common.Player p = PlayerConnection.get();
            if (p != null) {
                p.setRepeatMode(mode == MODE_REPEAT_ONE
                        ? androidx.media3.common.Player.REPEAT_MODE_ONE
                        : androidx.media3.common.Player.REPEAT_MODE_OFF);
            }
            modeBtn.setImageResource(mode == MODE_REPEAT_ONE
                    ? R.drawable.ic_repeat_one : R.drawable.ic_repeat);
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
            });
        });
        handler.post(progressTick);
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
        favBtn.setImageResource(MusicStore.isFavorite(song.bvid)
                ? R.drawable.ic_heart_filled : R.drawable.ic_heart);
    }

    @Override
    protected void onDestroy() {
        PlayerConnection.removeListener(this);
        handler.removeCallbacks(progressTick);
        super.onDestroy();
    }
}
