package com.bilimusic.app;

import android.app.Application;

import com.bilimusic.app._01_net.CookieStore;
import com.bilimusic.app._03_store.MusicStore;
import com.bilimusic.app._04_auth.AuthManager;
import com.bilimusic.app._08_util.ImageLoader;

public class BiliMusicApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AuthManager.init(this);
        CookieStore.init(this);
        MusicStore.init(this);
        ImageLoader.init(this);
    }
}
