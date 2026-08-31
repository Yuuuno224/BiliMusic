package com.yuuuno224.bilimusic.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.yuuuno224.bilimusic.R;
import com.yuuuno224.bilimusic.model.NavData;
import com.yuuuno224.bilimusic.auth.AuthManager;
import com.yuuuno224.bilimusic.store.MusicStore;
import com.yuuuno224.bilimusic.util.ImageLoader;

/** 我的：B站账号登录 / 统计 / 快捷入口 / 退出 */
public class MeFragment extends Fragment {

    private ImageView avatar;
    private TextView name;
    private TextView level;
    private TextView statFav;
    private TextView statHist;
    private Button loginBtn;
    private Button logoutBtn;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_me, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        avatar = v.findViewById(R.id.me_avatar);
        name = v.findViewById(R.id.me_name);
        level = v.findViewById(R.id.me_level);
        statFav = v.findViewById(R.id.me_stat_fav);
        statHist = v.findViewById(R.id.me_stat_hist);
        loginBtn = v.findViewById(R.id.me_login);
        logoutBtn = v.findViewById(R.id.me_logout);

        loginBtn.setOnClickListener(vg ->
                startActivity(new Intent(getContext(), QrLoginActivity.class)));
        logoutBtn.setOnClickListener(vg -> {
            AuthManager.logout();
            refresh();
            Toast.makeText(getContext(), "已退出登录", Toast.LENGTH_SHORT).show();
        });

        v.findViewById(R.id.me_entry_fav).setOnClickListener(vg -> openLibrary(0));
        v.findViewById(R.id.me_entry_hist).setOnClickListener(vg -> openLibrary(1));
        v.findViewById(R.id.me_entry_folder).setOnClickListener(vg -> {
            if (!AuthManager.isLoggedIn()) {
                Toast.makeText(getContext(), "请先登录B站账号", Toast.LENGTH_SHORT).show();
                return;
            }
            openLibrary(2);
        });
        v.findViewById(R.id.me_entry_about).setOnClickListener(vg -> showAbout());
        v.findViewById(R.id.me_entry_github).setOnClickListener(vg ->
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/Yuuuno224/BiliMusic"))));
    }

    private void openLibrary(int tab) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openLibraryTab(tab);
        }
    }

    private void showAbout() {
        new AlertDialog.Builder(requireContext())
                .setTitle("关于 BiliMusic")
                .setMessage(getString(R.string.disclaimer)
                        + "\n\n音源：B站公开视频\n播放：Media3 ExoPlayer\n开源：GitHub @Yuuuno224")
                .setPositiveButton("确定", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        NavData me = AuthManager.cachedMe();
        if (AuthManager.isLoggedIn() && me != null && me.isLogin) {
            loginBtn.setVisibility(View.GONE);
            logoutBtn.setVisibility(View.VISIBLE);
            name.setText(me.uname);
            level.setText("LV" + me.level() + " · 已登录B站账号");
            if (me.face != null && !me.face.isEmpty()) {
                ImageLoader.load(me.face, bmp -> {
                    if (bmp != null) {
                        avatar.setImageBitmap(bmp);
                    }
                });
            }
        } else {
            loginBtn.setVisibility(View.VISIBLE);
            logoutBtn.setVisibility(View.GONE);
            name.setText("未登录");
            level.setText("登录后可获得更高音质");
            avatar.setImageResource(R.drawable.ic_person);
        }
        statFav.setText(String.valueOf(MusicStore.favorites().size()));
        statHist.setText(String.valueOf(MusicStore.history().size()));
    }
}
