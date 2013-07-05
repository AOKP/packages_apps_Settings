package com.android.settings.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Sms.Inbox;
import android.provider.Telephony.Sms.Intents;
import android.provider.Telephony.Sms.Outbox; //yeah yeah
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;

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
            String defaultSms = getResources()
                    .getString(R.string.quiet_hours_auto_sms_null);
            SharedPreferences prefs =
                    PreferenceManager.getDefaultSharedPreferences(context);
            String message = null;
            message = prefs.getString(KEY_AUTO_SMS_MESSAGE, defaultSms);
            int userAutoSms = returnUserAutoSms(context);
            SmsMessage[] msgs = Intents.getMessagesFromIntent(intent);
            if (userAutoSms == DEFAULT_SMS) {
                return;
            } else {
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
        int smsSetting = 0;
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(context);
        smsSetting = prefs.getInt(KEY_AUTO_SMS, DEFAULT_SMS);
        return smsSetting;
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
