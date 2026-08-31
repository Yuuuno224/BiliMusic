package com.yuuuno224.bilimusic.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yuuuno224.bilimusic.R;
import com.yuuuno224.bilimusic.store.MusicStore;
import com.yuuuno224.bilimusic.store.Song;
import com.yuuuno224.bilimusic.player.PlayerConnection;
import com.yuuuno224.bilimusic.repo.MusicRepository;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

/** 搜索页：关键词搜索B站视频 → 以音乐形式播放 */
public class SearchFragment extends Fragment {

    private EditText searchBox;
    private RecyclerView resultList;
    private TextView hint;
    private ChipGroup historyGroup;
    private View progress;
    private SongAdapter adapter;
    private final List<Song> results = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        searchBox = v.findViewById(R.id.search_box);
        resultList = v.findViewById(R.id.result_list);
        hint = v.findViewById(R.id.search_hint);
        historyGroup = v.findViewById(R.id.history_group);
        progress = v.findViewById(R.id.search_progress);
        ImageButton go = v.findViewById(R.id.search_btn);

        adapter = new SongAdapter(results, (all, position) ->
                PlayerConnection.playQueue(requireContext(), all, position));
        resultList.setLayoutManager(new LinearLayoutManager(getContext()));
        resultList.setAdapter(adapter);

        go.setOnClickListener(vg -> doSearch());
        searchBox.setOnEditorActionListener((tv, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                doSearch();
                return true;
            }
            return false;
        });

        renderHistory();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (adapter != null) {
            adapter.release();
        }
    }

    private void renderHistory() {
        historyGroup.removeAllViews();
        List<String> history = MusicStore.searchHistory();
        if (history.isEmpty()) {
            historyGroup.setVisibility(View.GONE);
            return;
        }
        historyGroup.setVisibility(View.VISIBLE);
        LayoutInflater inf = LayoutInflater.from(getContext());
        for (String kw : history) {
            Chip chip = (Chip) inf.inflate(R.layout.item_search_chip, historyGroup, false);
            chip.setText(kw);
            chip.setOnClickListener(v -> {
                searchBox.setText(kw);
                doSearch();
            });
            historyGroup.addView(chip);
        }
    }

    private void doSearch() {
        String kw = searchBox.getText().toString().trim();
        if (kw.isEmpty()) {
            return;
        }
        MusicStore.addSearchHistory(kw);
        renderHistory();
        progress.setVisibility(View.VISIBLE);
        hint.setVisibility(View.GONE);
        MusicRepository.search(kw, new MusicRepository.Callback<List<Song>>() {
            @Override
            public void onResult(List<Song> songs) {
                if (!isAdded()) {
                    return;
                }
                progress.setVisibility(View.GONE);
                results.clear();
                results.addAll(songs);
                adapter.notifyDataSetChanged();
                if (songs.isEmpty()) {
                    hint.setVisibility(View.VISIBLE);
                    hint.setText("没有找到相关内容");
                }
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) {
                    return;
                }
                progress.setVisibility(View.GONE);
                hint.setVisibility(View.VISIBLE);
                hint.setText(message);
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
