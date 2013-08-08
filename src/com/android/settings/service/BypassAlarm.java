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
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.view.Window;
import android.view.WindowManager;

import com.android.settings.R;

public class BypassAlarm extends Activity implements OnDismissListener {

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        final Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);

        Bundle extras = getIntent().getExtras();
        String phoneNumber = extras.getString("number");
        startAlertDialog(phoneNumber);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startService();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // let's just ignore it for now
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopService();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        stopService();
    }

    @Override
    public void onBackPressed() {
        // bugger off, eh?
        return;
    }

    private void startAlertDialog(String phoneNumber) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.quiet_hours_alarm_dialog_title);
        alert.setMessage(phoneNumber + getResources().getString(
                R.string.quiet_hours_alarm_message));
        alert.setPositiveButton(getResources().getString(R.string.quiet_hours_alarm_dismiss),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        finish();
                    }
                });
        alert.setOnDismissListener(this);
        alert.show();
    }

    private void startService() {
        Intent serviceIntent = new Intent(this, AlarmService.class);
        this.startService(serviceIntent);
    }

    private void stopService() {
        Intent serviceIntent = new Intent(this, AlarmService.class);
        this.stopService(serviceIntent);
    }
}
