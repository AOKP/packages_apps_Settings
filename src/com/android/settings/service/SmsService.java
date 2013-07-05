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
import android.provider.Telephony.Sms;
import android.provider.Telephony.Sms.Inbox;
import android.provider.Telephony.Sms.Intents;
import android.provider.Telephony.Sms.Outbox; //yeah yeah
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;

import java.util.Calendar;

import com.android.settings.R;

public class SmsService extends Service {

    final static String TAG = "smsAutoReceiver";
    public final static boolean DEBUG = false;

    public static final String KEY_AUTO_SMS = "auto_sms";
    public static final String KEY_AUTO_SMS_MESSAGE = "auto_sms_message";
    private static final int DEFAULT_SMS = 0;

    static boolean mRegistered = false;
    boolean mShouldSwitchBack = false;
    boolean mRunningOwnRingerModeChange = false;

    private BroadcastReceiver smsReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            int userAutoSms = returnUserAutoSms(context);
            String message = null;
            String defaultSms = getResources()
                    .getString(R.string.quiet_hours_auto_sms_null);

            SharedPreferences prefs =
                    PreferenceManager.getDefaultSharedPreferences(context);
            message = prefs.getString(KEY_AUTO_SMS_MESSAGE, defaultSms);
            int userAutoSms = returnUserAutoSms(context);
            SmsMessage[] msgs = Intents.getMessagesFromIntent(intent);
            if (userAutoSms == DEFAULT_SMS) {
                return;
            }
            if (inQuietHours()) {
                sendAutoReply(message, msgs);
            }
        }
    };

    @Override
    public void onDestroy() {
        unregisterReceiver(smsReceiver);
        mRegistered = false;
        super.onDestroy();
    }

    public static int returnUserAutoSms(Context context) {
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(context);
        return Integer.parseInt(prefs.getString(KEY_AUTO_SMS, String.valueOf(DEFAULT_SMS)));
    }

    private boolean inQuietHours() {
        boolean quietHoursEnabled = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.QUIET_HOURS_ENABLED, 0) != 0;
        int quietHoursStart = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.QUIET_HOURS_START, 0);
        int quietHoursEnd = Settings.System.getInt(mContext.getContentResolver(),
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

    public static boolean isStarted() {
        return mRegistered;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!mRegistered) {
            IntentFilter inf = new IntentFilter();
            inf.addAction("android.provider.Telephony.SMS_RECEIVED");
            registerReceiver(smsReceiver, inf);
            mRegistered = true;
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private static void log(String s) {
        if (DEBUG) {
            Log.e(TAG, s);
        }
    }
}
