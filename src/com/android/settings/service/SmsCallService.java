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
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.R;
import java.util.Calendar;

public class SmsCallService extends Service {

    private final static String TAG = "SmsCallService";

    public final static String WAKE_TAG = "SmsCallServiceWakeLock";

    private static final int RESPONSE_DISABLED = 0;

    private static TelephonyManager mTelephony;

    public static PowerManager.WakeLock mWakeLock;

    private boolean mIncomingCall = false;

    public boolean mKeepCounting = false;

    private int mBypassCallCount;

    private int mMinutes;

    private int mDay;

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (state == TelephonyManager.CALL_STATE_RINGING) {
                mIncomingCall = true;
            }
            if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                // Don't message if call was answered
                mBypassCallCount = 1;
                mIncomingCall = false;
            }
            if (state == TelephonyManager.CALL_STATE_IDLE && mIncomingCall) {
                // Call Received and now inactive
                mIncomingCall = false;
                int bypassPreference = MessagingHelper.returnUserCallBypass(SmsCallService.this);
                int userAutoSms = MessagingHelper.returnUserAutoCall(SmsCallService.this);
                if ((bypassPreference != RESPONSE_DISABLED
                        || userAutoSms != RESPONSE_DISABLED)
                        && MessagingHelper.inQuietHours(SmsCallService.this)) {
// WTF Broken           boolean isContact = MessagingHelper.isContact(SmsCallService.this, incomingNumber); ***************
                    boolean isContact = true; //temp ***************
                    Log.e(TAG, "This shouldn't be empty (but is): " + incomingNumber); //temp ***************
                    if (!mKeepCounting) {
                        mKeepCounting = true;
                        mBypassCallCount = 0;
                        mDay = MessagingHelper.returnDayOfMonth();
                        mMinutes = MessagingHelper.returnTimeInMinutes();
                    }

                    boolean timeConstraintMet = MessagingHelper.returnTimeConstraintMet(
                            SmsCallService.this, mMinutes, mDay);
                    if (timeConstraintMet) {
                        switch (bypassPreference) {
                            case MessagingHelper.ALL_NUMBERS:
                                mBypassCallCount++;
                                break;
                            case MessagingHelper.CONTACTS_ONLY:
                                if (isContact) {
                                    mBypassCallCount++;
                                }
                                break;
                        }
                    } else {
                        mBypassCallCount = 1;
                        mDay = MessagingHelper.returnDayOfMonth();
                        mMinutes = MessagingHelper.returnTimeInMinutes();
                    }
                    if (mBypassCallCount
                            == MessagingHelper.returnUserCallBypassCount(SmsCallService.this)
                            && timeConstraintMet) {
                        mKeepCounting = false;
                        // StartThatBitch
                        startAlarm(SmsCallService.this, incomingNumber);
                    } else {
                        // Let's not auto-respond if an alarm fired
                        if (userAutoSms != RESPONSE_DISABLED) {
                            MessagingHelper.checkSmsQualifiers(
                                    SmsCallService.this, incomingNumber, userAutoSms, isContact);
                        }
                    }
                }
            }
            super.onCallStateChanged(state, incomingNumber);
        }
    };

    private BroadcastReceiver smsReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            SmsMessage[] msgs = Intents.getMessagesFromIntent(intent);
            SmsMessage msg = msgs[0];
            String incomingNumber = msg.getOriginatingAddress();
            boolean nawDawg = false;
            int userAutoSms = MessagingHelper.returnUserAutoText(context);
            int bypassCodePref = MessagingHelper.returnUserTextBypass(context);
            boolean isContact = MessagingHelper.isContact(context, incomingNumber);

            if ((bypassCodePref != RESPONSE_DISABLED
                    || userAutoSms != RESPONSE_DISABLED)
                    && MessagingHelper.inQuietHours(context)) {
                String bypassCode = MessagingHelper.returnUserTextBypassCode(context);
                String messageBody = msg.getMessageBody();
                if (messageBody.contains(bypassCode)) {
                    switch (bypassCodePref) {
                        case MessagingHelper.ALL_NUMBERS:
                            nawDawg = true;
                            startAlarm(SmsCallService.this, incomingNumber);
                            // StartThatBitch
                            break;
                        case MessagingHelper.CONTACTS_ONLY:
                            if (isContact) {
                                nawDawg = true;
                                startAlarm(SmsCallService.this, incomingNumber);
                                // StartThatBitch
                            }
                            break;
                    }
                }
                if (userAutoSms != RESPONSE_DISABLED && nawDawg == false) {
                    MessagingHelper.checkSmsQualifiers(context, incomingNumber, userAutoSms, isContact);
                }
            }
        }
    };

    @Override
    public void onCreate() {
        mTelephony = (TelephonyManager)
                this.getSystemService(Context.TELEPHONY_SERVICE);
        mTelephony.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intents.SMS_RECEIVED_ACTION);
        registerReceiver(smsReceiver, filter);
    }

    @Override
    public void onDestroy() {
        if (mTelephony != null) {
            mTelephony.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
        mPhoneStateListener = null;
        unregisterReceiver(smsReceiver);
        killWakeLock();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        killWakeLock();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void killWakeLock() {
        if (mWakeLock != null) {
            if (mWakeLock.isHeld()) {
                mWakeLock.release();
            }
        }
    }

    private void startAlarm(Context context, String phoneNumber) {
        if (TextUtils.isEmpty(phoneNumber)) {
            phoneNumber = getResources().getString(R.string.quiet_hours_number_null);
        }
        Intent alarmDialog = new Intent();
        alarmDialog.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        alarmDialog.setClass(context, com.android.settings.service.BypassAlarm.class);
        alarmDialog.putExtra("number", phoneNumber);
        startActivity(alarmDialog);
    }
}
