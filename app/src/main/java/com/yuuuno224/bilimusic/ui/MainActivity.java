package com.yuuuno224.bilimusic.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.yuuuno224.bilimusic.R;
import com.yuuuno224.bilimusic.player.PlayerConnection;
import com.yuuuno224.bilimusic.store.MusicStore;
import com.yuuuno224.bilimusic.store.Song;
import com.yuuuno224.bilimusic.util.ImageLoader;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/** 主界面：底部三栏（搜索/乐库/我的）+ 迷你播放条 */
public class MainActivity extends AppCompatActivity implements PlayerConnection.Listener {

    private View miniBar;
    private ImageView miniCover;
    private TextView miniTitle;
    private TextView miniUp;
    private ImageButton miniPlay;
    private ImageButton miniFav;
    private Fragment current;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView nav = findViewById(R.id.bottom_nav);
        miniBar = findViewById(R.id.mini_bar);
        miniCover = findViewById(R.id.mini_cover);
        miniTitle = findViewById(R.id.mini_title);
        miniUp = findViewById(R.id.mini_up);
        miniPlay = findViewById(R.id.mini_play);
        miniFav = findViewById(R.id.mini_fav);

        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_search) {
                show(new SearchFragment(), "search");
                return true;
            } else if (id == R.id.nav_library) {
                show(new LibraryFragment(), "library");
                return true;
            } else if (id == R.id.nav_me) {
                show(new MeFragment(), "me");
                return true;
            }
            return false;
        });

        miniBar.setOnClickListener(v -> startActivity(new android.content.Intent(this, NowPlayingActivity.class)));
        miniPlay.setOnClickListener(v -> PlayerConnection.toggle());
        miniFav.setOnClickListener(v -> {
            Song song = PlayerConnection.currentSong();
            if (song != null) {
                MusicStore.toggleFavorite(song);
                refreshMiniFav(song);
                Toast.makeText(this, MusicStore.isFavorite(song.bvid) ? "已收藏" : "已取消收藏",
                        Toast.LENGTH_SHORT).show();
            }
        });
        findViewById(R.id.mini_playlist).setOnClickListener(v -> PlaylistDialog.show(this));

        PlayerConnection.connect(this, c -> runOnUiThread(this::onPlayerChanged));
        PlayerConnection.addListener(this);
    }

    private android.content.Intent newOwningIntent() {
        return new android.content.Intent(this, NowPlayingActivity.class);
    }

    private void show(Fragment f, String tag) {
        current = f;
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, f, tag)
                .commit();
    }

    @Override
    public void onPlayerChanged() {
        Song song = PlayerConnection.currentSong();
        if (song == null) {
            miniBar.setVisibility(View.GONE);
            return;
        }
        miniBar.setVisibility(View.VISIBLE);
        miniTitle.setText(song.title);
        miniUp.setText(song.up);
        ImageLoader.load(song.coverSmall(), bmp -> {
            if (bmp != null) {
                miniCover.setImageBitmap(bmp);
            } else {
                miniCover.setImageResource(R.drawable.ic_music_note);
            }
        });
        miniPlay.setImageResource(PlayerConnection.isPlaying()
                ? R.drawable.ic_pause : R.drawable.ic_play);
        refreshMiniFav(song);
    }

    private void refreshMiniFav(Song song) {
        boolean fav = MusicStore.isFavorite(song.bvid);
        miniFav.setImageResource(fav ? R.drawable.ic_heart_filled : R.drawable.ic_heart);
        miniFav.setColorFilter(ContextCompat.getColor(this,
                fav ? R.color.heart_red : R.color.on_bg));
    }

    @Override
    protected void onDestroy() {
        PlayerConnection.removeListener(this);
        super.onDestroy();
    }
}
