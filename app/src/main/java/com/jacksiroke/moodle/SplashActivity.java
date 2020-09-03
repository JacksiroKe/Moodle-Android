package com.jacksiroke.moodle;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import helper.UserAccount;

public class SplashActivity extends AppCompatActivity {
    private UserAccount userAccount;
    private long ms=0, splashTime=2500;
    private boolean splashActive = true, paused=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Thread mythread = new Thread() {
            public void run() {
                try {
                    while (splashActive && ms < splashTime) {
                        if (!paused)
                            ms = ms + 100;
                        sleep(100);
                    }
                } catch (Exception e) {
                } finally {
                    nextAction();
                }
            }
        };
        mythread.start();
    }

    void nextAction()
    {
        userAccount = new UserAccount(this);
        if (userAccount.isLoggedIn())
        {
            Intent intent = new Intent(this, MainActivity.class);
            if (getIntent().getParcelableExtra("path") != null) {
                intent.putExtra("path", getIntent().getParcelableExtra("path").toString());
            }
            startActivity(intent);
            finish();
        }
        else
        {
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            finish();
        }
    }

}