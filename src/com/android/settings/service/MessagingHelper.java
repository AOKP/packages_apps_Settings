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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.provider.ContactsContract.PhoneLookup;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.SmsManager;

import java.util.Calendar;

import com.android.settings.R;

public class MessagingHelper {

    private final static String TAG = "MessagingHelper";

    private static final String KEY_AUTO_SMS = "auto_sms";
    private static final String KEY_AUTO_SMS_CALL = "auto_sms_call";
    private static final String KEY_AUTO_SMS_MESSAGE = "auto_sms_message";
    private static final String STOP_SMS_SERVICE =
            "com.android.settings.service.STOP_SMS_SERVICE";
    public static final String SMS_SERVICE_COMMAND =
            "com.android.settings.service.SMS_SERVICE_COMMAND";

    private static final int FULL_DAY = 1440; // 1440 minutes in a day
    private static final int DEFAULT_DISABLED = 0;
    private static final int ALL_NUMBERS = 1;
    private static final int CONTACTS_ONLY = 2;

    public static boolean inQuietHours(Context context) {
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

    public static int returnUserAutoCall(Context context) {
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(context);
        return Integer.parseInt(prefs.getString(
                KEY_AUTO_SMS_CALL, String.valueOf(DEFAULT_DISABLED)));
    }

    public static int returnUserAutoText(Context context) {
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(context);
        return Integer.parseInt(prefs.getString(
                KEY_AUTO_SMS, String.valueOf(DEFAULT_DISABLED)));
    }

    public static void checkSmsQualifiers(Context context, String incomingNumber, int userAutoSms) {
        String message = null;
        String defaultSms = context.getResources().getString(
                R.string.quiet_hours_auto_sms_null);
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(context);
        message = prefs.getString(KEY_AUTO_SMS_MESSAGE, defaultSms);
        switch (userAutoSms) {
            case ALL_NUMBERS:
                sendAutoReply(message, incomingNumber);
                break;
            case CONTACTS_ONLY:
                if (isContact(context, incomingNumber)) {
                    sendAutoReply(message, incomingNumber);
                }
                break;
        }
    }

    private static boolean isContact(Context context, String phoneNumber) {
        Uri lookupUri = Uri.withAppendedPath(
                PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        String[] numberProject = {
                PhoneLookup._ID,
                PhoneLookup.NUMBER,
                PhoneLookup.DISPLAY_NAME };
        Cursor c = context.getContentResolver().query(
                lookupUri, numberProject, null, null, null);
        try {
            if (c.moveToFirst()) {
                return true;
            }
        } finally {
            if (c != null) {
               c.close();
            }
        }
        return false;
    }

    private static void sendAutoReply(String message, String phoneNumber) {
        SmsManager sms = SmsManager.getDefault();
        try {
            sms.sendTextMessage(phoneNumber, null, message, null, null);
        } catch (IllegalArgumentException e) {
        }
    }

    private static PendingIntent makeServiceIntent(Context context,
            String action, int requestCode) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(action);
        return PendingIntent.getBroadcast(
                context, requestCode, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    /*
     * Called when:
     * QuietHours Toggled
     * QuietHours TimeChanged
     * AutoSMS Preferences Changed
     * At Boot
     * Time manually adjusted or Timezone Changed
     * AutoSMS service Stopped - Schedule again for next day
     */
    public static void scheduleService(Context context) {
        boolean quietHoursEnabled = Settings.System.getInt(context.getContentResolver(),
                Settings.System.QUIET_HOURS_ENABLED, 0) != 0;
        int quietHoursStart = Settings.System.getInt(context.getContentResolver(),
                Settings.System.QUIET_HOURS_START, 0);
        int quietHoursEnd = Settings.System.getInt(context.getContentResolver(),
                Settings.System.QUIET_HOURS_END, 0);
        int autoCall = returnUserAutoCall(context);
        int autoText = returnUserAutoText(context);

        Intent serviceTriggerIntent = new Intent(context, AutoSmsService.class);
        PendingIntent startIntent = makeServiceIntent(context, SMS_SERVICE_COMMAND, 1);
        PendingIntent stopIntent = makeServiceIntent(context, STOP_SMS_SERVICE, 2);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        am.cancel(startIntent);
        am.cancel(stopIntent);

        if (!quietHoursEnabled ||
                (autoCall == DEFAULT_DISABLED && autoText == DEFAULT_DISABLED)) {
            context.stopService(serviceTriggerIntent);
            return;
        }

        if (quietHoursStart == quietHoursEnd) {
            // 24 hours, start without stop
            context.startService(serviceTriggerIntent);
            return;
        }

        Calendar calendar = Calendar.getInstance();
        int currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);

        boolean inQuietHours = false;
        // time from now on (in minutes) when the service start/stop should be scheduled
        int serviceStartMinutes = -1, serviceStopMinutes = -1;

        if (quietHoursEnd < quietHoursStart) {
            // Starts at night, ends in the morning.
            if (currentMinutes >= quietHoursStart) {
                // In QuietHours - quietHoursEnd in new day
                inQuietHours = true;
                serviceStopMinutes = FULL_DAY - currentMinutes + quietHoursEnd;
            } else if (currentMinutes <= quietHoursEnd) {
                // In QuietHours - quietHoursEnd in same day
                inQuietHours = true;
                serviceStopMinutes = quietHoursEnd - currentMinutes;
            } else {
                // Out of QuietHours
                // Current time less than quietHoursStart, greater than quietHoursEnd
                inQuietHours = false;
                serviceStartMinutes = quietHoursStart - currentMinutes;
                serviceStopMinutes = FULL_DAY - currentMinutes + quietHoursEnd;
            }
        } else {
            // Starts in the morning, ends at night.
            if (currentMinutes >= quietHoursStart && currentMinutes <= quietHoursEnd) {
                // In QuietHours
                inQuietHours = true;
                serviceStopMinutes = quietHoursEnd - currentMinutes;
            } else {
                // Out of QuietHours
                inQuietHours = false;
                if (currentMinutes <= quietHoursStart) {
                    serviceStartMinutes = quietHoursStart - currentMinutes;
                    serviceStopMinutes = quietHoursEnd - currentMinutes;
                } else {
                    // Current Time greater than quietHoursEnd
                    serviceStartMinutes = FULL_DAY - currentMinutes + quietHoursStart;
                    serviceStopMinutes = FULL_DAY - currentMinutes + quietHoursEnd;
                }
            }
        }

        if (inQuietHours) {
            context.startService(serviceTriggerIntent);
        } else {
            context.stopService(serviceTriggerIntent);
        }

        if (serviceStartMinutes >= 0) {
            // Start service a minute early
            serviceStartMinutes--;
            calendar.add(Calendar.MINUTE, serviceStartMinutes);
            am.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), startIntent);
            calendar.add(Calendar.MINUTE, -serviceStartMinutes);
        }

        if (serviceStopMinutes >= 0) {
            // Stop service a minute late
            serviceStopMinutes++;
            calendar.add(Calendar.MINUTE, serviceStopMinutes);
            am.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), stopIntent);
            calendar.add(Calendar.MINUTE, -serviceStopMinutes);
        }
    }
}
