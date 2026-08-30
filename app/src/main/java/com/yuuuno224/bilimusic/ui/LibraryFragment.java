package com.yuuuno224.bilimusic.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yuuuno224.bilimusic.R;
import com.yuuuno224.bilimusic.store.MusicStore;
import com.yuuuno224.bilimusic.store.Song;
import com.yuuuno224.bilimusic.player.PlayerConnection;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

/** 乐库：我喜欢 / 最近播放 */
public class LibraryFragment extends Fragment {

    private TextView countText;
    private ImageButton clearBtn;
    private RecyclerView list;
    private SongAdapter adapter;
    private final List<Song> shown = new ArrayList<>();
    private int tab = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_library, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        TabLayout tabs = v.findViewById(R.id.library_tabs);
        countText = v.findViewById(R.id.library_count);
        clearBtn = v.findViewById(R.id.library_clear);
        list = v.findViewById(R.id.library_list);

        adapter = new SongAdapter(shown, (all, position) ->
                PlayerConnection.playQueue(all, position));
        list.setLayoutManager(new LinearLayoutManager(getContext()));
        list.setAdapter(adapter);

        tabs.addTab(tabs.newTab().setText("我喜欢"));
        tabs.addTab(tabs.newTab().setText("最近播放"));
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab t) {
                tab = t.getPosition();
                reload();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab t) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab t) {
            }
        });

        clearBtn.setOnClickListener(vg -> {
            if (tab == 1) {
                MusicStore.clearHistory();
                reload();
            }
        });

        reload();
    }

    @Override
    public void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        shown.clear();
        List<Song> data = tab == 0 ? MusicStore.favorites() : MusicStore.history();
        shown.addAll(data);
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        countText.setText(shown.size() + " 首");
        clearBtn.setVisibility(tab == 1 && !shown.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
