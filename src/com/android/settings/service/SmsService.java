package com.android.settings.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Telephony.Sms.Intents;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;

import java.util.Calendar;

import com.android.settings.R;

public class SmsService extends BroadcastReceiver {

    final static String TAG = "smsAutoReceiver";

    public static final String KEY_AUTO_SMS = "auto_sms";
    public static final String KEY_AUTO_SMS_MESSAGE = "auto_sms_message";
    private static final int DEFAULT_SMS = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        int userAutoSms = returnUserAutoSms(context);
        if (userAutoSms == DEFAULT_SMS) {
            return;
        }
        if (inQuietHours(context)) {
            String message = null;
            String defaultSms = getResources().getString(R.string.quiet_hours_auto_sms_null);
            SharedPreferences prefs =
                    PreferenceManager.getDefaultSharedPreferences(context);
            message = prefs.getString(KEY_AUTO_SMS_MESSAGE, defaultSms);
            SmsMessage[] msgs = Intents.getMessagesFromIntent(intent);
            sendAutoReply(message, msgs);
        }
    }

    public static int returnUserAutoSms(Context context) {
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(context);
        return Integer.parseInt(prefs.getString(KEY_AUTO_SMS, String.valueOf(DEFAULT_SMS)));
    }

    private boolean inQuietHours(Context context) {
        boolean quietHoursEnabled = Settings.System.getInt(context.getContentResolver(),
                Settings.System.QUIET_HOURS_ENABLED, 0) != 0;
        int quietHoursStart = Settings.System.getInt(context.getContentResolver(),
                Settings.System.QUIET_HOURS_START, 0);
        int quietHoursEnd = Settings.System.getInt(context.getContentResolver(),
                Settings.System.QUIET_HOURS_END, 0);

        if (quietHoursEnabled) {
            if (quietHoursStart == quietHoursEnd) {
                return true;
            }
            // Get the date in "quiet hours" format.
            Calendar calendar = Calendar.getInstance();
            int minutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
            if (quietHoursEnd < quietHoursStart) {
                // Starts at night, ends in the morning.
                return (minutes > quietHoursStart) || (minutes < quietHoursEnd);
            } else {
                return (minutes > quietHoursStart) && (minutes < quietHoursEnd);
            }
        }
        return false;
    }

    private void sendAutoReply(String message, SmsMessage[] msgs) {
        SmsMessage msg = msgs[0];
        SmsManager sms = SmsManager.getDefault(); 
        String phoneNumber = msg.getOriginatingAddress();
        try {
            sms.sendTextMessage(phoneNumber, null, message, null, null);
            // TODO: add to sent messages (option?)
        } catch (IllegalArgumentException e) {
        }
    }
}
