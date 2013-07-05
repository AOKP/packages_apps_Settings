package com.android.settings.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.provider.ContactsContract.PhoneLookup;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.net.Uri;
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
    private static final String SMS_SERVICE_COMMAND =
            "com.android.settings.service.SMS_SERVICE_COMMAND";

    private static final int FULL_DAY = 1440; // 1440 minutes in a day
    private static final int DEFAULT_DISABLED = 0;
    private static final int ALL_NUMBERS = 1;
    private static final int CONTACTS_ONLY = 2;

    public static boolean isContact(Context context, String phoneNumber) {
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

    private static void sendAutoReply(String message, String phoneNumber) {
        SmsManager sms = SmsManager.getDefault();
        try {
            sms.sendTextMessage(phoneNumber, null, message, null, null);
        } catch (IllegalArgumentException e) {
        }
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
        PendingIntent startPending;
        PendingIntent stopPendingBroadcast;
        Intent startSmsService = new Intent(context, AlarmReceiver.class);
        startSmsService.setAction(SMS_SERVICE_COMMAND);
        Intent stopIntent = new Intent(context, AlarmReceiver.class);
        stopIntent.setAction(STOP_SMS_SERVICE);
        startPending = PendingIntent.getBroadcast(
                context, 1, startSmsService, PendingIntent.FLAG_CANCEL_CURRENT);
        stopPendingBroadcast = PendingIntent.getBroadcast(
                context, 2, stopIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarm.cancel(startPending);
        alarm.cancel(stopPendingBroadcast);

        if (quietHoursEnabled == false
                || (autoCall == DEFAULT_DISABLED
                && autoText == DEFAULT_DISABLED)) {
            context.stopService(new Intent(context, AutoSmsService.class));
            return;
        }


        if (quietHoursEnabled
                && (autoCall != DEFAULT_DISABLED
                || autoText != DEFAULT_DISABLED)) {
            if (quietHoursStart == quietHoursEnd) {
                // 24 Hours - Start without stop
                context.startService(new Intent(context, AutoSmsService.class));
                return;
            }
            Calendar calendar = Calendar.getInstance();
            Calendar stopCal = Calendar.getInstance();
            int minutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
            int serviceStart;
            int serviceEnd;
            if (quietHoursEnd < quietHoursStart) {
                // Starts at night, ends in the morning.
                if (minutes > quietHoursStart) {
                    // In QuietHours - quietHoursEnd in new day
                    context.startService(new Intent(context, AutoSmsService.class));
                    serviceEnd = FULL_DAY - minutes + quietHoursEnd;
                    stopCal.add(Calendar.MINUTE, serviceEnd);
                    alarm.set(AlarmManager.RTC_WAKEUP,
                            stopCal.getTimeInMillis(), stopPendingBroadcast);
                } else if (minutes < quietHoursEnd) {
                    // In QuietHours - quietHoursEnd in same day
                    context.startService(new Intent(context, AutoSmsService.class));
                    serviceEnd = quietHoursEnd - minutes;
                    stopCal.add(Calendar.MINUTE, serviceEnd);
                    alarm.set(AlarmManager.RTC_WAKEUP,
                            stopCal.getTimeInMillis(), stopPendingBroadcast);
                } else {
                    // Out of QuietHours
                    // Current time less than quietHoursStart, greater than quietHoursEnd
                    context.stopService(new Intent(context, AutoSmsService.class));
                    serviceStart = quietHoursStart - minutes;
                    serviceEnd = FULL_DAY - minutes + quietHoursEnd;
                    calendar.add(Calendar.MINUTE, serviceStart);
                    stopCal.add(Calendar.MINUTE, serviceEnd);
                    alarm.set(AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(), startPending);
                    alarm.set(AlarmManager.RTC_WAKEUP,
                            stopCal.getTimeInMillis(), stopPendingBroadcast);
                }
            } else {
                // Starts in the morning, ends at night.
                if ((minutes > quietHoursStart) && (minutes < quietHoursEnd)) {
                    // In QuietHours
                    context.startService(new Intent(context, AutoSmsService.class));
                    serviceEnd = quietHoursEnd - minutes;
                    stopCal.add(Calendar.MINUTE, serviceEnd);
                    alarm.set(AlarmManager.RTC_WAKEUP,
                            stopCal.getTimeInMillis(), stopPendingBroadcast);
                } else {
                    // Out of QuietHours
                    context.stopService(new Intent(context, AutoSmsService.class));
                    if (minutes < quietHoursStart) {
                        serviceStart = quietHoursStart - minutes;
                        serviceEnd = quietHoursEnd - minutes;
                        calendar.add(Calendar.MINUTE, serviceStart);
                        stopCal.add(Calendar.MINUTE, serviceEnd);
                        alarm.set(AlarmManager.RTC_WAKEUP,
                                calendar.getTimeInMillis(), startPending);
                        alarm.set(AlarmManager.RTC_WAKEUP,
                                stopCal.getTimeInMillis(), stopPendingBroadcast);
                    } else {
                        // Current Time greater than quietHoursEnd
                        serviceStart = FULL_DAY - minutes + quietHoursStart;
                        serviceEnd = FULL_DAY - minutes + quietHoursEnd;
                        calendar.add(Calendar.MINUTE, serviceStart);
                        stopCal.add(Calendar.MINUTE, serviceEnd);
                        alarm.set(AlarmManager.RTC_WAKEUP,
                                calendar.getTimeInMillis(), startPending);
                        alarm.set(AlarmManager.RTC_WAKEUP,
                                stopCal.getTimeInMillis(), stopPendingBroadcast);
                    }
                }
            }
        }
    }
}
