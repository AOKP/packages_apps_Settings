package com.android.settings.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;

public class SmsService extends Service {

    Context mContext = this;

    final static String TAG = "smsAutoReceiver";
    public final static boolean DEBUG = false;

    public static final String KEY_AUTO_SMS = "auto_sms";
    public static final String KEY_AUTO_SMS_MESSAGE = "auto_sms_message";
    public static final String DEFAULT_SMS_MESSAGE = getResources()
            .getString(R.string.quiet_hours_auto_sms_null);
    private static final int DEFAULT_SMS = 0;

    static boolean mRegistered = false;
    boolean mShouldSwitchBack = false;
    boolean mRunningOwnRingerModeChange = false;

    private BroadcastReceiver smsReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (Intent.SMS_RECEIVED_ACTION.equals(action)) {
                int userAutoSms = returnUserAutoSms(context);
                String message = null;
                SharedPreferences prefs =
                        PreferenceManager.getDefaultSharedPreferences(c);
                message = prefs.getString(KEY_AUTO_SMS_MESSAGE, DEFAULT_SMS_MESSAGE);

                if (userAutoSms == DEFAULT_SMS) {
                    return;
                } else {
                    sendAutoReply(message);
                }

            }
        }
    };

    @Override
    public void onDestroy() {
        unregisterReceiver(smsReceiver);
        mRegistered = false;
        super.onDestroy();
    }

    public static int returnUserAutoSms(Context c) {
        int smsSetting;
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(c);
        smsSetting = prefs.getInt(KEY_AUTO_SMS, DEFAULT_SMS);
        return smsSetting;
    }

    private void sendAutoReply(String message) {
        SmsManager sms = SmsManager.getDefault();
        String phoneNumber = sms.getOriginatingAddress();

        try {
            sms.sendTextMessage(phoneNumber, null, message, null, null);
            // TODO: add to sent messages (option?)
        } catch (IllegalArgumentException e) {
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!mRegistered) {
            IntentFilter inf = new IntentFilter();
            inf.addAction(Intent.SMS_RECEIVED_ACTION);
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
