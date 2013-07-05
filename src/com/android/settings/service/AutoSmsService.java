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
                int userAutoSms = MessagingHelper.returnUserAutoCall(AutoSmsService.this);
                if (userAutoSms != SMS_DISABLED
                        && MessagingHelper.inQuietHours(AutoSmsService.this)) {
                    MessagingHelper.checkSmsQualifiers(AutoSmsService.this, incomingNumber, userAutoSms);
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
        filter.addAction(Intents.SMS_RECEIVED_ACTION);
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
