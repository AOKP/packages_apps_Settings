package com.android.settings.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.android.settings.R;

public class CallService extends BroadcastReceiver {

    final static String TAG = "callAutoReceiver";

    public static final String KEY_AUTO_SMS_CALL = "auto_sms_call";
    public static final String KEY_AUTO_SMS_MESSAGE = "auto_sms_message";
    private static final int DEFAULT_SMS = 0;
    private static final int ALL_NUMBERS = 1;
    private static final int CONTACTS_ONLY = 2;
    private boolean mReceived = false;

    static PhoneStateListener mPhoneStateListener;

    @Override
    public void onReceive(final Context context, Intent intent) {
        TelephonyManager telephony = (TelephonyManager) context.
                getSystemService(Context.TELEPHONY_SERVICE);
        if (mPhoneStateListener == null) {
            mPhoneStateListener = new PhoneStateListener() {
                @Override
                public void onCallStateChanged(int state, String incomingNumber) {
                    if (state == TelephonyManager.CALL_STATE_RINGING && !mReceived) {
                        mReceived = true;
                    }
                    if (state == TelephonyManager.CALL_STATE_IDLE && mReceived) {
                        mReceived = false;
                        getSmsQualifiers(context, incomingNumber);
                    }
                    super.onCallStateChanged(state, incomingNumber);
                }
            };
            telephony.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    private void getSmsQualifiers(Context context, String incomingNumber) {
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
            switch (userAutoSms) {
                case ALL_NUMBERS:
                    MessagingHelper.sendAutoReply(message, incomingNumber);
                    break;
                case CONTACTS_ONLY:
                    if (MessagingHelper.isContact(context, incomingNumber)) {
                        MessagingHelper.sendAutoReply(message, incomingNumber);
                    }
                    break;
            }
        }
    }

    public static int returnUserAutoSms(Context context) {
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(context);
        return Integer.parseInt(prefs.getString(KEY_AUTO_SMS_CALL, String.valueOf(DEFAULT_SMS)));
    }
}
