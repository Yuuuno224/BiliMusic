package com.bilimusic.app._07_ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bilimusic.app.R;
import com.bilimusic.app._02_model.QrPollData;
import com.bilimusic.app._04_auth.AuthManager;

/** B站账号登录：生成授权链接，用户点击后在B站APP中确认，本页轮询登录状态 */
public class QrLoginActivity extends AppCompatActivity {

    private static final long POLL_INTERVAL = 2000;
    private static final long TIMEOUT = 180_000;

    private TextView status;
    private Button openBtn;
    private Button copyBtn;

    private String authUrl;
    private String qrcodeKey;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long startAt;
    private boolean running;

    private final Runnable pollTask = new Runnable() {
        @Override
        public void run() {
            if (!running) {
                return;
            }
            if (System.currentTimeMillis() - startAt > TIMEOUT) {
                status.setText("授权超时，请重试");
                running = false;
                return;
            }
            AuthManager.pollOnce(qrcodeKey, callback);
            handler.postDelayed(this, POLL_INTERVAL);
        }
    };

    private final AuthManager.LoginCallback callback = new AuthManager.LoginCallback() {
        @Override
        public void onGenerated(String url, String key) {
            authUrl = url;
            qrcodeKey = key;
            startAt = System.currentTimeMillis();
            running = true;
            status.setText("已生成授权链接，请在B站APP中确认");
            handler.post(pollTask);
        }

        @Override
        public void onStatus(int state, String message) {
            if (state == QrPollData.ST_EXPIRED) {
                status.setText(message + "，正在重新生成…");
                running = false;
                AuthManager.generate(this2());
            } else if (state == QrPollData.ST_SCANNED) {
                status.setText(message);
            }
        }

        @Override
        public void onSuccess(com.bilimusic.app._02_model.NavData me) {
            running = false;
            handler.removeCallbacks(pollTask);
            status.setText("登录成功");
            Toast.makeText(QrLoginActivity.this, "欢迎 " + (me != null ? me.uname : ""), Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        }

        @Override
        public void onError(String message) {
            status.setText(message);
        }
    };

    private AuthManager.LoginCallback this2() {
        return callback;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_login);

        status = findViewById(R.id.login_status);
        openBtn = findViewById(R.id.login_open);
        copyBtn = findViewById(R.id.login_copy);

        openBtn.setOnClickListener(v -> {
            if (authUrl != null) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)));
                } catch (Exception e) {
                    Toast.makeText(this, "未找到可打开链接的应用", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "授权链接尚未生成", Toast.LENGTH_SHORT).show();
            }
        });

        copyBtn.setOnClickListener(v -> {
            if (authUrl != null) {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("bili_auth", authUrl));
                Toast.makeText(this, "链接已复制", Toast.LENGTH_SHORT).show();
            }
        });

        status.setText("正在生成授权链接…");
        AuthManager.generate(callback);
    }

    @Override
    protected void onDestroy() {
        running = false;
        handler.removeCallbacks(pollTask);
        super.onDestroy();
    }
}
