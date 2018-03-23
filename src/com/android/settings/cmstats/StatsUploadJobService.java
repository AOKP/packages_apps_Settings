/*
 * Copyright (C) 2015 The CyanogenMod Project
 *           (C) 2017 The LineageOS Project
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

package com.android.settings.cmstats;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.PersistableBundle;
import android.util.ArrayMap;
import android.util.Log;
import com.android.settings.R;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

public class StatsUploadJobService extends JobService {

    private static final String TAG = StatsUploadJobService.class.getSimpleName();
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    public static final String KEY_DEVICE_ID = "deviceId";
    public static final String KEY_DEVICE_NAME = "deviceName";
    public static final String KEY_BUILD_VERSION = "buildVersion";
    public static final String KEY_BUILD_DATE = "buildDate";
    public static final String KEY_RELEASE_TYPE = "releaseType";
    public static final String KEY_COUNTRY_CODE = "countryCode";
    public static final String KEY_CARRIER_NAME = "carrierName";
    public static final String KEY_CARRIER_ID = "carrierId";
    public static final String KEY_TIMESTAMP = "timeStamp";

    private final Map<JobParameters, StatsUploadTask> mCurrentJobs
            = Collections.synchronizedMap(new ArrayMap<JobParameters, StatsUploadTask>());

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        if (DEBUG)
            Log.d(TAG, "onStartJob() called with " + "jobParameters = [" + jobParameters + "]");

        if (!Utilities.isStatsCollectionEnabled(this)) {
            return false;
        }

        final StatsUploadTask uploadTask = new StatsUploadTask(jobParameters);
        mCurrentJobs.put(jobParameters, uploadTask);
        uploadTask.execute((Void) null);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        if (DEBUG)
            Log.d(TAG, "onStopJob() called with " + "jobParameters = [" + jobParameters + "]");

        final StatsUploadTask cancelledJob;
        cancelledJob = mCurrentJobs.remove(jobParameters);

        if (cancelledJob != null) {
            // cancel the ongoing background task
            cancelledJob.cancel(true);
            return true; // reschedule
        }

        return false;
    }

    private class StatsUploadTask extends AsyncTask<Void, Void, Boolean> {

        private JobParameters mJobParams;

        public StatsUploadTask(JobParameters jobParams) {
            this.mJobParams = jobParams;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            PersistableBundle extras = mJobParams.getExtras();

            String deviceId = extras.getString(KEY_DEVICE_ID);
            String deviceName = extras.getString(KEY_DEVICE_NAME);
            String buildVersion = extras.getString(KEY_BUILD_VERSION);
            String buildDate = extras.getString(KEY_BUILD_DATE);
            String releaseType = extras.getString(KEY_RELEASE_TYPE);
            String countryCode = extras.getString(KEY_COUNTRY_CODE);
            String carrierName = extras.getString(KEY_CARRIER_NAME);
            String carrierId = extras.getString(KEY_CARRIER_ID);
            long timeStamp = extras.getLong(KEY_TIMESTAMP);

            boolean success = false;
            int jobType = extras.getInt(KEY_JOB_TYPE, -1);
            if (!isCancelled()) {
                try {
                    success = upload(deviceId, deviceName,
                            buildVersion, buildDate, releaseType,
                            countryCode, carrierName, carrierId);
                } catch (IOException e) {
                    Log.e(TAG, "Could not upload stats checkin to aokp server", e);
                    success = false;
                }
            }
            if (DEBUG)
                Log.d(TAG, "job id " + mJobParams.getJobId() + ", has finished with success="
                        + success);
            return success;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            mCurrentJobs.remove(mJobParams);
            jobFinished(mJobParams, !success);
        }
    }


    private boolean upload(String deviceId, String deviceName,
                           String buildVersion, String buildDate, String releaseType,
                           String countryCode, String carrierName, String carrierId)
            throws IOException {

        final URL url = new URL(getString(R.string.stats_aokp_url));
        Uri uri = new Uri.Builder()
                .appendQueryParameter("device_id", deviceId)
                .appendQueryParameter("device_name", deviceName)
                .appendQueryParameter("build_version", buildVersion)
                .appendQueryParameter("build_date", buildDate)
                .appendQueryParameter("release_type", releaseType)
                .appendQueryParameter("country_code", countryCode)
                .appendQueryParameter("carrier_name", carrierName)
                .appendQueryParameter("carrier_id", carrierId)
                .build();

        HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
        try {
            urlConnection.setConnectTimeout(60000);
            urlConnection.setReadTimeout(60000);
            urlConnection.setInstanceFollowRedirects(true);
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            urlConnection.connect();

            OutputStream os = urlConnection.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            writer.write(uri.getEncodedQuery());
            writer.flush();
            writer.close();
            os.close();

            urlConnection.connect();

            final int responseCode = urlConnection.getResponseCode();
            if (DEBUG) Log.d(TAG, "cm server response code=" + responseCode);
            final boolean success = responseCode == HttpURLConnection.HTTP_OK;
            if (!success) {
                Log.w(TAG, "failed sending, server returned: " + getResponse(urlConnection,
                        !success));
            }
            return success;
        } finally {
            urlConnection.disconnect();
        }

    }

    private String getResponse(HttpURLConnection httpUrlConnection, boolean errorStream)
            throws IOException {
        InputStream responseStream = new BufferedInputStream(errorStream
                ? httpUrlConnection.getErrorStream()
                : httpUrlConnection.getInputStream());

        BufferedReader responseStreamReader = new BufferedReader(
                new InputStreamReader(responseStream));
        String line = "";
        StringBuilder stringBuilder = new StringBuilder();
        while ((line = responseStreamReader.readLine()) != null) {
            stringBuilder.append(line).append("\n");
        }
        responseStreamReader.close();
        responseStream.close();

        return stringBuilder.toString();
    }

}
