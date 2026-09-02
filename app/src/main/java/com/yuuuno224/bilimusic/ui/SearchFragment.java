package com.yuuuno224.bilimusic.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.yuuuno224.bilimusic.R;
import com.yuuuno224.bilimusic.store.MusicStore;
import com.yuuuno224.bilimusic.store.Song;
import com.yuuuno224.bilimusic.player.PlayerConnection;
import com.yuuuno224.bilimusic.repo.MusicRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** 搜索页：首页推荐（热门搜索 + 音乐排行榜 + 最近播放）→ 关键词搜索B站视频 */
public class SearchFragment extends Fragment {

    private static final String[] FALLBACK_HOT = {
            "音乐", "BGM", "纯音乐", "翻唱", "ACG",
            "Lo-fi", "钢琴曲", "古风", "电子音", "爵士"
    };
    private static final int RECENT_LIMIT = 5;
    private static final int RANK_LIMIT = 10;

    private EditText searchBox;
    private View searchHome;
    private View historyScroll;
    private ChipGroup historyGroup;
    private ChipGroup hotGroup;
    private View rankSection;
    private RecyclerView rankList;
    private View recentSection;
    private RecyclerView recentList;
    private RecyclerView resultList;
    private TextView hint;
    private View progress;
    private SongAdapter adapter;
    private SongAdapter rankAdapter;
    private SongAdapter recentAdapter;
    private final List<Song> results = new ArrayList<>();
    private final List<Song> rank = new ArrayList<>();
    private final List<Song> recent = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        searchBox = v.findViewById(R.id.search_box);
        searchHome = v.findViewById(R.id.search_home);
        historyScroll = v.findViewById(R.id.history_scroll);
        historyGroup = v.findViewById(R.id.history_group);
        hotGroup = v.findViewById(R.id.hot_group);
        rankSection = v.findViewById(R.id.rank_section);
        rankList = v.findViewById(R.id.rank_list);
        recentSection = v.findViewById(R.id.recent_section);
        recentList = v.findViewById(R.id.recent_list);
        resultList = v.findViewById(R.id.result_list);
        hint = v.findViewById(R.id.search_hint);
        progress = v.findViewById(R.id.search_progress);
        ImageButton go = v.findViewById(R.id.search_btn);

        adapter = new SongAdapter(results, (all, position) ->
                PlayerConnection.playQueue(requireContext(), all, position));
        resultList.setLayoutManager(new LinearLayoutManager(getContext()));
        resultList.setAdapter(adapter);

        rankAdapter = new SongAdapter(rank, (all, position) ->
                PlayerConnection.playQueue(requireContext(), all, position));
        rankList.setLayoutManager(new LinearLayoutManager(getContext()));
        rankList.setAdapter(rankAdapter);
        rankList.setNestedScrollingEnabled(false);

        recentAdapter = new SongAdapter(recent, (all, position) ->
                PlayerConnection.playQueue(requireContext(), all, position));
        recentList.setLayoutManager(new LinearLayoutManager(getContext()));
        recentList.setAdapter(recentAdapter);
        recentList.setNestedScrollingEnabled(false);

        go.setOnClickListener(vg -> doSearch());
        searchBox.setOnEditorActionListener((tv, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                doSearch();
                return true;
            }
            return false;
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        showHome();
        renderHistory();
        renderHot();
        renderRank();
        renderRecent();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (adapter != null) {
            adapter.release();
        }
        if (rankAdapter != null) {
            rankAdapter.release();
        }
        if (recentAdapter != null) {
            recentAdapter.release();
        }
    }

    private void showHome() {
        searchHome.setVisibility(View.VISIBLE);
        resultList.setVisibility(View.GONE);
        hint.setVisibility(View.GONE);
        progress.setVisibility(View.GONE);
    }

    private void showResults() {
        searchHome.setVisibility(View.GONE);
        resultList.setVisibility(View.VISIBLE);
    }

    private void fillChips(ChipGroup group, List<String> words) {
        group.removeAllViews();
        LayoutInflater inf = LayoutInflater.from(getContext());
        for (String kw : words) {
            Chip chip = (Chip) inf.inflate(R.layout.item_search_chip, group, false);
            chip.setText(kw);
            chip.setOnClickListener(v -> {
                searchBox.setText(kw);
                doSearch();
            });
            group.addView(chip);
        }
    }

    private void renderHistory() {
        List<String> history = MusicStore.searchHistory();
        if (history.isEmpty()) {
            historyScroll.setVisibility(View.GONE);
            return;
        }
        historyScroll.setVisibility(View.VISIBLE);
        fillChips(historyGroup, history);
    }

    private void renderHot() {
        MusicRepository.hotWords(new MusicRepository.Callback<List<String>>() {
            @Override
            public void onResult(List<String> words) {
                if (!isAdded() || words.isEmpty()) {
                    fillChips(hotGroup, Arrays.asList(FALLBACK_HOT));
                    return;
                }
                fillChips(hotGroup, words);
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) {
                    return;
                }
                fillChips(hotGroup, Arrays.asList(FALLBACK_HOT));
            }
        });
    }

    private void renderRank() {
        MusicRepository.musicRanking(new MusicRepository.Callback<List<Song>>() {
            @Override
            public void onResult(List<Song> songs) {
                if (!isAdded()) {
                    return;
                }
                rank.clear();
                int n = Math.min(songs.size(), RANK_LIMIT);
                for (int i = 0; i < n; i++) {
                    rank.add(songs.get(i));
                }
                if (rank.isEmpty()) {
                    rankSection.setVisibility(View.GONE);
                    return;
                }
                rankSection.setVisibility(View.VISIBLE);
                rankAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) {
                    return;
                }
                rankSection.setVisibility(View.GONE);
            }
        });
    }

    private void renderRecent() {
        recent.clear();
        List<Song> all = MusicStore.history();
        int n = Math.min(all.size(), RECENT_LIMIT);
        for (int i = 0; i < n; i++) {
            recent.add(all.get(i));
        }
        if (recent.isEmpty()) {
            recentSection.setVisibility(View.GONE);
            return;
        }
        recentSection.setVisibility(View.VISIBLE);
        recentAdapter.notifyDataSetChanged();
    }

    private void doSearch() {
        String kw = searchBox.getText().toString().trim();
        if (kw.isEmpty()) {
            showHome();
            return;
        }
        MusicStore.addSearchHistory(kw);
        renderHistory();
        showResults();
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
