package com.yuuuno224.bilimusic;

import android.app.Application;

import com.yuuuno224.bilimusic.net.CookieStore;
import com.yuuuno224.bilimusic.store.MusicStore;
import com.yuuuno224.bilimusic.auth.AuthManager;
import com.yuuuno224.bilimusic.util.ImageLoader;

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
