package com.android.settings.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (MessagingHelper.returnUserAutoCall(context) != 0
                || MessagingHelper.returnUserAutoText(context) != 0) {
            MessagingHelper.scheduleService(context);
        }
    }
}
