package com.android.settings.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Telephony.Sms.Intents;
import android.telephony.PhoneStateListener;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;

public class AutoSmsService extends Service {

    private final static String TAG = "AutoSmsService";

    private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";

    private static final int SMS_DISABLED = 0;

    private boolean mIncomingCall = false;

    private static TelephonyManager mTelephony;

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (state == TelephonyManager.CALL_STATE_RINGING) {
                mIncomingCall = true;
            }
            if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                // Don't message if call was answered
                mIncomingCall = false;
            }
            if (state == TelephonyManager.CALL_STATE_IDLE && mIncomingCall) {
                // Call Received and now inactive
                mIncomingCall = false;
                int userAutoSms = MessagingHelper.returnUserAutoCall(getApplicationContext());
                if (userAutoSms != SMS_DISABLED
                        && MessagingHelper.inQuietHours(getApplicationContext())) {
                    MessagingHelper.checkSmsQualifiers(getApplicationContext(), incomingNumber, userAutoSms);
                }
            }
            super.onCallStateChanged(state, incomingNumber);
        }
    };

    private BroadcastReceiver smsReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            int userAutoSms = MessagingHelper.returnUserAutoText(context);
            if (userAutoSms != SMS_DISABLED && MessagingHelper.inQuietHours(context)) {
                SmsMessage[] msgs = Intents.getMessagesFromIntent(intent);
                SmsMessage msg = msgs[0];
                String incomingNumber = msg.getOriginatingAddress();
                MessagingHelper.checkSmsQualifiers(context, incomingNumber, userAutoSms);
            }
        }
    };

    @Override
    public void onCreate() {
        PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wl.acquire();
        mTelephony = (TelephonyManager)
                this.getSystemService(Context.TELEPHONY_SERVICE);
        mTelephony.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(SMS_RECEIVED);
        registerReceiver(smsReceiver, filter);
        wl.release();
    }

    @Override
    public void onDestroy() {
        if (mTelephony != null) {
            mTelephony.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
        mPhoneStateListener = null;
        unregisterReceiver(smsReceiver);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
