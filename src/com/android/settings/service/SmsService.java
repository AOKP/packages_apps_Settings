package com.android.settings.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.Telephony.Sms.Intents;
import android.telephony.SmsMessage;

import com.android.settings.R;

public class SmsService extends BroadcastReceiver {

    final static String TAG = "smsAutoReceiver";

    public static final String KEY_AUTO_SMS = "auto_sms";
    public static final String KEY_AUTO_SMS_MESSAGE = "auto_sms_message";
    private static final int DEFAULT_SMS = 0;
    private static final int ALL_NUMBERS = 1;
    private static final int CONTACTS_ONLY = 2;

    @Override
    public void onReceive(Context context, Intent intent) {
        int userAutoSms = returnUserAutoSms(context);
        if (userAutoSms == DEFAULT_SMS) {
            return;
        }
        if (MessagingHelper.inQuietHours(context)) {
            String message = null;
            String defaultSms = context.getResources().getString(R.string.quiet_hours_auto_sms_null);
            SharedPreferences prefs =
                    PreferenceManager.getDefaultSharedPreferences(context);
            message = prefs.getString(KEY_AUTO_SMS_MESSAGE, defaultSms);
            SmsMessage[] msgs = Intents.getMessagesFromIntent(intent);
            SmsMessage msg = msgs[0];
            String phoneNumber = msg.getOriginatingAddress();
            switch (userAutoSms) {
                case ALL_NUMBERS:
                    MessagingHelper.sendAutoReply(message, phoneNumber);
                    break;
                case CONTACTS_ONLY:
                    if (MessagingHelper.isContact(context, phoneNumber)) {
                        MessagingHelper.sendAutoReply(message, phoneNumber);
                    }
                    break;
            }
        }
    }

    public static int returnUserAutoSms(Context context) {
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(context);
        return Integer.parseInt(prefs.getString(KEY_AUTO_SMS, String.valueOf(DEFAULT_SMS)));
    }
}
