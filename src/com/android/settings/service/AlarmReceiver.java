package com.android.settings.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {

    private final static String TAG = "AlarmReceiver";

    private static final String SMS_SERVICE_COMMAND =
            "com.android.settings.service.SMS_SERVICE_COMMAND";

    @Override
    public void onReceive(Context context, Intent intent) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wl.acquire();
        final String action = intent.getAction();

        if (SMS_SERVICE_COMMAND.equals(action)) {
            context.startService(new Intent(context, AutoSmsService.class));
        } else {
            context.stopService(new Intent(context, AutoSmsService.class));
            MessagingHelper.scheduleService(context);
        }
        wl.release();
    }

}
