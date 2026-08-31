package com.yuuuno224.bilimusic.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.yuuuno224.bilimusic.R;
import com.yuuuno224.bilimusic.player.PlayerConnection;
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
    }

    @Override
    protected void onDestroy() {
        PlayerConnection.removeListener(this);
        super.onDestroy();
    }
}
