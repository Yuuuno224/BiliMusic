package com.yuuuno224.bilimusic.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yuuuno224.bilimusic.R;
import com.yuuuno224.bilimusic.store.MusicStore;
import com.yuuuno224.bilimusic.store.Song;
import com.yuuuno224.bilimusic.player.PlayerConnection;
import com.yuuuno224.bilimusic.util.ImageLoader;

import java.util.List;

/** 歌曲列表适配器：点击播放 / 收藏 / 下一首播放 */
public class SongAdapter extends RecyclerView.Adapter<SongAdapter.Holder> {

    public interface OnSongClick {
        void onClick(List<Song> all, int position);
    }

    private final List<Song> songs;
    private final OnSongClick click;

    public SongAdapter(List<Song> songs, OnSongClick click) {
        this.songs = songs;
        this.click = click;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_song, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Song s = songs.get(position);
        h.title.setText(s.title);
        h.subtitle.setText(s.up + " · " + s.durationText());
        ImageLoader.load(s.coverSmall(), bmp -> {
            if (bmp != null) {
                h.cover.setImageBitmap(bmp);
            } else {
                h.cover.setImageResource(R.drawable.ic_music_note);
            }
        });
        boolean fav = MusicStore.isFavorite(s.bvid);
        applyFav(h.fav, fav);

        h.itemView.setOnClickListener(v -> {
            int pos = h.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && click != null) {
                click.onClick(songs, pos);
            }
        });
        h.fav.setOnClickListener(v -> {
            MusicStore.toggleFavorite(s);
            applyFav(h.fav, MusicStore.isFavorite(s.bvid));
        });
        h.more.setOnClickListener(v -> {
            PlayerConnection.playNext(v.getContext(), s);
            Toast.makeText(v.getContext(), "已加入下一首播放", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    private static void applyFav(ImageButton btn, boolean fav) {
        btn.setImageResource(fav ? R.drawable.ic_heart_filled : R.drawable.ic_heart);
        btn.setColorFilter(androidx.core.content.ContextCompat.getColor(btn.getContext(),
                fav ? R.color.heart_red : R.color.on_bg_secondary));
    }

    static class Holder extends RecyclerView.ViewHolder {
        ImageView cover;
        TextView title;
        TextView subtitle;
        ImageButton fav;
        ImageButton more;

        Holder(@NonNull View itemView) {
            super(itemView);
            cover = itemView.findViewById(R.id.item_cover);
            title = itemView.findViewById(R.id.item_title);
            subtitle = itemView.findViewById(R.id.item_subtitle);
            fav = itemView.findViewById(R.id.item_fav);
            more = itemView.findViewById(R.id.item_next);
        }
    }
}
