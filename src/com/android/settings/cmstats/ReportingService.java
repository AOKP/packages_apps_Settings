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

import android.app.IntentService;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.util.Log;
import cyanogenmod.providers.CMSettings;

import java.util.List;

public class ReportingService extends IntentService {
    /* package */ static final String TAG = "CMStats";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    public ReportingService() {
        super(ReportingService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        JobScheduler js = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);

        String deviceId = Utilities.getDeviceID(getApplicationContext());
        String deviceName = Utilities.getDeviceName();
        String buildVersion = Utilities.getBuildVersion();
        String buildDate = Utilities.getBuildDate();
        String releaseType = Utilities.getReleaseType();
        String countryCode = Utilities.getCountryCode(getApplicationContext());
        String carrierName = Utilities.getCarrierName(getApplicationContext());
        String carrierId = Utilities.getCarrierId(getApplicationContext());

        final int cmOrgJobId = AnonymousStats.getNextJobId(getApplicationContext());

        if (DEBUG) Log.d(TAG, "scheduling job id: " + cmOrgJobId);

        PersistableBundle bundle = new PersistableBundle();
        bundle.putString(StatsUploadJobService.KEY_DEVICE_ID, deviceId);
        bundle.putString(StatsUploadJobService.KEY_DEVICE_NAME, deviceName);
        bundle.putString(StatsUploadJobService.KEY_BUILD_VERSION, buildVersion);
        bundle.putString(StatsUploadJobService.KEY_BUILD_DATE, buildDate);
        bundle.putString(StatsUploadJobService.KEY_RELEASE_TYPE, releaseType);
        bundle.putString(StatsUploadJobService.KEY_COUNTRY_CODE, countryCode);
        bundle.putString(StatsUploadJobService.KEY_CARRIER_NAME, carrierName);
        bundle.putString(StatsUploadJobService.KEY_CARRIER_ID, carrierId);
        bundle.putLong(StatsUploadJobService.KEY_TIMESTAMP, System.currentTimeMillis());

        // schedule cmorg stats upload
        js.schedule(new JobInfo.Builder(cmOrgJobId, new ComponentName(getPackageName(),
                StatsUploadJobService.class.getName()))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setMinimumLatency(1000)
                .setExtras(bundle)
                .setPersisted(true)
                .build());

        // reschedule
        AnonymousStats.updateLastSynced(this);
        ReportingServiceManager.setAlarm(this);
    }
}
