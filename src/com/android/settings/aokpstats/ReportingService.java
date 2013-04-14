/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.aokpstats;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Tracker;

import com.android.settings.R;
import com.android.settings.Settings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ReportingService extends Service {
    /* package */ static final String TAG = "AOKPStats";

    private StatsUploadTask mTask;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand (Intent intent, int flags, int startId) {
        Log.d(TAG, "User has opted in -- reporting.");

        if (mTask == null || mTask.getStatus() == AsyncTask.Status.FINISHED) {
            mTask = new StatsUploadTask();
            mTask.execute();
        }

        return Service.START_REDELIVER_INTENT;
    }

    private class StatsUploadTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            String deviceId = Utilities.getUniqueID(getApplicationContext());
            String deviceName = Utilities.getDevice();
            String deviceVersion = Utilities.getModVersion();
            String deviceCountry = Utilities.getCountryCode(getApplicationContext());
            String deviceCarrier = Utilities.getCarrier(getApplicationContext());
            String deviceCarrierId = Utilities.getCarrierId(getApplicationContext());

            Log.d(TAG, "SERVICE: Device ID=" + deviceId);
            Log.d(TAG, "SERVICE: Device Name=" + deviceName);
            Log.d(TAG, "SERVICE: Device Version=" + deviceVersion);
            Log.d(TAG, "SERVICE: Country=" + deviceCountry);
            Log.d(TAG, "SERVICE: Carrier=" + deviceCarrier);
            Log.d(TAG, "SERVICE: Carrier ID=" + deviceCarrierId);

            // report to google analytics
            GoogleAnalytics ga = GoogleAnalytics.getInstance(ReportingService.this);
            //ga.setDebug(true);
            Tracker tracker = ga.getTracker(getString(R.string.ga_trackingId));
            tracker.setAppName("AOKP");
            tracker.setAppVersion(deviceVersion);
            tracker.setCustomDimension(1, deviceId);
            tracker.setCustomDimension(2, deviceName);
            tracker.setCustomMetric(1, 1L);
            tracker.sendEvent("checkin", deviceName, deviceVersion, null);
            tracker.close();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            final Context context = ReportingService.this;
            final SharedPreferences prefs = AnonymousStats.getPreferences(context);
            prefs.edit().putLong(AnonymousStats.ANONYMOUS_LAST_CHECKED,
                    System.currentTimeMillis()).apply();
            ReportingServiceManager.setAlarm(context, 0);
            stopSelf();
        }
    }
}

