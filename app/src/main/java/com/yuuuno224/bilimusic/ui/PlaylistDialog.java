package com.yuuuno224.bilimusic.ui;

import android.content.Context;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.yuuuno224.bilimusic.R;
import com.yuuuno224.bilimusic.player.PlayerConnection;

/** 播放队列 BottomSheet 弹窗（迷你条与播放页复用） */
public final class PlaylistDialog {

    public static void show(@NonNull Context ctx) {
        androidx.media3.common.Player p = PlayerConnection.get();
        if (p == null || p.getMediaItemCount() == 0) {
            Toast.makeText(ctx, "播放队列为空", Toast.LENGTH_SHORT).show();
            return;
        }
        BottomSheetDialog dialog = new BottomSheetDialog(ctx);
        View view = dialog.getLayoutInflater().inflate(R.layout.dialog_playlist, null);
        TextView count = view.findViewById(R.id.playlist_count);
        RecyclerView rv = view.findViewById(R.id.playlist_list);
        rv.setLayoutManager(new LinearLayoutManager(ctx));
        count.setText(p.getMediaItemCount() + " 首");
        PlaylistAdapter adapter = new PlaylistAdapter(p, () ->
                count.setText(p.getMediaItemCount() + " 首"));
        rv.setAdapter(adapter);
        dialog.setContentView(view);
        dialog.setOnDismissListener(d -> {
            if (ctx instanceof AppCompatActivity) {
                PlayerConnection.Listener l = findListener((AppCompatActivity) ctx);
                if (l != null) {
                    l.onPlayerChanged();
                }
            }
        });
        dialog.show();
    }

    private static PlayerConnection.Listener findListener(AppCompatActivity a) {
        if (a instanceof PlayerConnection.Listener) {
            return (PlayerConnection.Listener) a;
        }
        return null;
    }
}
