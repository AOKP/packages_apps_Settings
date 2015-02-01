package com.android.settings.aicp;

import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.os.Bundle;

import com.android.settings.aicp.views.GifWebView;

public class HiddenAnimActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        InputStream stream = null;
        try {
            stream = getAssets().open("yoga.gif");
        } catch (IOException e) {
            e.printStackTrace();
        }

        GifWebView view = new GifWebView(this, "file:///android_asset/yoga.gif");

        setContentView(view);
    }
}
