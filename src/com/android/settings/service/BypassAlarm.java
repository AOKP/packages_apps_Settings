/*
 * Copyright (C) 2013 Android Open Kang Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.service;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;

import java.util.Calendar;

public class BypassAlarm extends Activity implements OnDismissListener {
    // all your magic are belonging to us
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        // setup
    }

    @Override
    protected void onResume() {
        super.onResume();
        // start sounding alarm
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // dismiss via pause recreate with new constructors
        setIntent(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // some things
    }

    @Override
    public void onPause() {
        super.onPause();
        // maybe things
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        // things
    }


    @Override
    public void onBackPressed() {
        // no dismiss
        return;
    }
}
