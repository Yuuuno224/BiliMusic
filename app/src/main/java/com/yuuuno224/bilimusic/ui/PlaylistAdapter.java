package com.yuuuno224.bilimusic.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.media3.common.Player;
import androidx.recyclerview.widget.RecyclerView;

import com.yuuuno224.bilimusic.R;
import com.yuuuno224.bilimusic.player.PlaybackService;
import com.yuuuno224.bilimusic.store.Song;

/** 播放队列适配器：点击跳转 / 删除 */
public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.Holder> {

    private final Player player;
    private final Runnable onChange;

    public PlaylistAdapter(Player player, Runnable onChange) {
        this.player = player;
        this.onChange = onChange;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_playlist_song, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        final int idx = position;
        Song s = PlaybackService.songOf(player.getMediaItemAt(idx));
        if (s != null) {
            h.title.setText(s.title);
            h.up.setText(s.up);
        } else {
            h.title.setText("未知曲目");
            h.up.setText("");
        }
        boolean current = idx == player.getCurrentMediaItemIndex();
        h.title.setTextColor(ContextCompat.getColor(h.itemView.getContext(),
                current ? R.color.accent : R.color.on_bg));
        h.itemView.setOnClickListener(v -> {
            player.seekTo(idx, 0);
            player.play();
            notifyDataSetChanged();
        });
        h.delete.setOnClickListener(v -> {
            player.removeMediaItem(idx);
            notifyDataSetChanged();
            if (onChange != null) {
                onChange.run();
            }
        });
    }

    @Override
    public int getItemCount() {
        return player.getMediaItemCount();
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView title;
        TextView up;
        ImageButton delete;

        Holder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.pl_title);
            up = itemView.findViewById(R.id.pl_up);
            delete = itemView.findViewById(R.id.pl_delete);
        }
    }
}
