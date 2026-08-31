package com.yuuuno224.bilimusic.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yuuuno224.bilimusic.R;
import com.yuuuno224.bilimusic.auth.AuthManager;
import com.yuuuno224.bilimusic.model.FavData;
import com.yuuuno224.bilimusic.model.NavData;
import com.yuuuno224.bilimusic.repo.MusicRepository;
import com.yuuuno224.bilimusic.store.MusicStore;
import com.yuuuno224.bilimusic.store.Song;
import com.yuuuno224.bilimusic.player.PlayerConnection;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

/** 乐库：我喜欢 / 最近播放 / B站收藏夹 */
public class LibraryFragment extends Fragment {

    private TextView countText;
    private ImageButton clearBtn;
    private RecyclerView list;
    private SongAdapter adapter;
    private FavFolderAdapter favAdapter;
    private final List<Song> shown = new ArrayList<>();
    private final List<FavData.Folder> folders = new ArrayList<>();
    private int tab = 0;
    private boolean folderMode = true;
    private FavData.Folder currentFolder;

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
                PlayerConnection.playQueue(requireContext(), all, position));
        favAdapter = new FavFolderAdapter(folders, this::openFolder);
        list.setLayoutManager(new LinearLayoutManager(getContext()));
        list.setAdapter(adapter);

        tabs.addTab(tabs.newTab().setText("我喜欢"));
        tabs.addTab(tabs.newTab().setText("最近播放"));
        tabs.addTab(tabs.newTab().setText("收藏夹"));
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab t) {
                tab = t.getPosition();
                folderMode = true;
                currentFolder = null;
                reload();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab t) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab t) {
                if (t.getPosition() == 2 && !folderMode) {
                    backToFolders();
                }
            }
        });

        clearBtn.setOnClickListener(vg -> {
            if (tab == 2 && !folderMode) {
                backToFolders();
            } else if (tab == 1) {
                MusicStore.clearHistory();
                reload();
            }
        });

        reload();
    }

    @Override
    public void onResume() {
        super.onResume();
        // 收藏夹曲目浏览中不重载，避免打断导航
        if (tab == 2 && !folderMode) {
            return;
        }
        reload();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (adapter != null) {
            adapter.release();
        }
    }

    private void reload() {
        if (tab == 2) {
            loadFolders();
            return;
        }
        list.setAdapter(adapter);
        shown.clear();
        List<Song> data = tab == 0 ? MusicStore.favorites() : MusicStore.history();
        shown.addAll(data);
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        countText.setText(shown.size() + " 首");
        clearBtn.setImageResource(R.drawable.ic_close);
        clearBtn.setVisibility(tab == 1 && !shown.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void loadFolders() {
        list.setAdapter(favAdapter);
        clearBtn.setImageResource(R.drawable.ic_close);
        clearBtn.setVisibility(View.GONE);
        if (!AuthManager.isLoggedIn()) {
            folders.clear();
            favAdapter.notifyDataSetChanged();
            countText.setText("未登录，请先在【我的】页登录");
            return;
        }
        countText.setText("加载中…");
        NavData me = AuthManager.cachedMe();
        long mid = me != null ? me.mid : 0;
        MusicRepository.favFolders(mid, new MusicRepository.Callback<List<FavData.Folder>>() {
            @Override
            public void onResult(List<FavData.Folder> result) {
                if (!isAdded() || tab != 2 || !folderMode) {
                    return;
                }
                folders.clear();
                folders.addAll(result);
                favAdapter.notifyDataSetChanged();
                countText.setText(folders.size() + " 个收藏夹");
            }

            @Override
            public void onError(String message) {
                if (!isAdded() || tab != 2 || !folderMode) {
                    return;
                }
                folders.clear();
                favAdapter.notifyDataSetChanged();
                countText.setText(message);
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openFolder(FavData.Folder folder) {
        currentFolder = folder;
        folderMode = false;
        list.setAdapter(adapter);
        clearBtn.setImageResource(R.drawable.ic_arrow_back);
        clearBtn.setVisibility(View.VISIBLE);
        countText.setText("加载中…");
        MusicRepository.favResources(folder.id,
                new MusicRepository.Callback<List<Song>>() {
                    @Override
                    public void onResult(List<Song> result) {
                        if (!isAdded() || tab != 2 || folderMode) {
                            return;
                        }
                        shown.clear();
                        shown.addAll(result);
                        adapter.notifyDataSetChanged();
                        countText.setText(currentFolder.title + " · " + shown.size() + " 首");
                    }

                    @Override
                    public void onError(String message) {
                        if (!isAdded() || tab != 2 || folderMode) {
                            return;
                        }
                        shown.clear();
                        adapter.notifyDataSetChanged();
                        countText.setText(message);
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void backToFolders() {
        folderMode = true;
        currentFolder = null;
        shown.clear();
        adapter.notifyDataSetChanged();
        loadFolders();
    }
}
