package com.yuuuno224.bilimusic.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yuuuno224.bilimusic.R;
import com.yuuuno224.bilimusic.model.FavData;
import com.yuuuno224.bilimusic.util.ImageLoader;

import java.util.List;

/** 收藏夹列表适配器 */
public class FavFolderAdapter extends RecyclerView.Adapter<FavFolderAdapter.Holder> {

    public interface OnFolderClick {
        void onClick(FavData.Folder folder);
    }

    private final List<FavData.Folder> folders;
    private final OnFolderClick click;

    public FavFolderAdapter(List<FavData.Folder> folders, OnFolderClick click) {
        this.folders = folders;
        this.click = click;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_fav_folder, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        FavData.Folder f = folders.get(position);
        h.title.setText(f.title);
        h.count.setText(f.media_count + " 个内容");
        ImageLoader.load(f.cover, bmp -> {
            if (bmp != null) {
                h.cover.setImageBitmap(bmp);
            } else {
                h.cover.setImageResource(R.drawable.ic_music_note);
            }
        });
        h.itemView.setOnClickListener(v -> {
            if (click != null) {
                click.onClick(f);
            }
        });
    }

    @Override
    public int getItemCount() {
        return folders.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        ImageView cover;
        TextView title;
        TextView count;

        Holder(@NonNull View itemView) {
            super(itemView);
            cover = itemView.findViewById(R.id.folder_cover);
            title = itemView.findViewById(R.id.folder_title);
            count = itemView.findViewById(R.id.folder_count);
        }
    }
}
