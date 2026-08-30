package com.bilimusic.app._07_ui;

import android.content.Intent;
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
import androidx.fragment.app.Fragment;

import com.bilimusic.app.R;
import com.bilimusic.app._02_model.NavData;
import com.bilimusic.app._04_auth.AuthManager;
import com.bilimusic.app._08_util.ImageLoader;

/** 我的：B站账号登录 / 用户信息 / 退出 */
public class MeFragment extends Fragment {

    private ImageView avatar;
    private TextView name;
    private TextView level;
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
        loginBtn = v.findViewById(R.id.me_login);
        logoutBtn = v.findViewById(R.id.me_logout);

        loginBtn.setOnClickListener(vg ->
                startActivity(new Intent(getContext(), QrLoginActivity.class)));
        logoutBtn.setOnClickListener(vg -> {
            AuthManager.logout();
            refresh();
            Toast.makeText(getContext(), "已退出登录", Toast.LENGTH_SHORT).show();
        });
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
    }
}
