/*
 * Copyright (C) 2015 The Android Open Source Project
 * Copyright (C) 2016 The CyanogenMod Project
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
package com.android.settings.applications;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.text.format.Formatter.BytesResult;
import android.widget.TextView;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.Settings.AppOpsSummaryActivity;
import com.android.settings.Utils;
import com.android.settings.applications.ProcStatsData.MemInfo;

public class ProcessStatsSummary extends ProcessStatsBase implements OnPreferenceClickListener {

    private static final String KEY_STATUS_HEADER = "status_header";

    private static final String KEY_PERFORMANCE = "performance";
    private static final String KEY_TOTAL_MEMORY = "total_memory";
    private static final String KEY_AVERAGY_USED = "average_used";
    private static final String KEY_FREE = "free";
    private static final String KEY_APP_LIST = "apps_list";
    private static final String KEY_APP_STARTUP = "apps_startup";

    private Activity mActivity;

    private LinearColorBar mColors;
    private LayoutPreference mHeader;
    private TextView mMemStatus;

    private Preference mPerformance;
    private Preference mTotalMemory;
    private Preference mAverageUsed;
    private Preference mFree;
    private Preference mAppListPreference;
    private Preference mAppStartupPreference;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mActivity = getActivity();

        addPreferencesFromResource(R.xml.process_stats_summary);
        mHeader = (LayoutPreference) findPreference(KEY_STATUS_HEADER);
        mMemStatus = (TextView) mHeader.findViewById(R.id.memory_state);
        mColors = (LinearColorBar) mHeader.findViewById(R.id.color_bar);

        mPerformance = findPreference(KEY_PERFORMANCE);
        mTotalMemory = findPreference(KEY_TOTAL_MEMORY);
        mAverageUsed = findPreference(KEY_AVERAGY_USED);
        mFree = findPreference(KEY_FREE);
        mAppListPreference = findPreference(KEY_APP_LIST);
        mAppListPreference.setOnPreferenceClickListener(this);
        mAppStartupPreference = findPreference(KEY_APP_STARTUP);
        mAppStartupPreference.setOnPreferenceClickListener(this);
    }

    @Override
    public void refreshUi() {
        Context context = getContext();
        int memColor = context.getColor(R.color.running_processes_apps_ram);
        mColors.setColors(memColor, memColor, context.getColor(R.color.running_processes_free_ram));

        MemInfo memInfo = mStatsManager.getMemInfo();

        double usedRam = memInfo.realUsedRam;
        double totalRam = memInfo.realTotalRam;
        double freeRam = memInfo.realFreeRam;
        BytesResult usedResult = Formatter.formatBytes(context.getResources(), (long) usedRam,
                Formatter.FLAG_SHORTER);
        String totalString = Formatter.formatShortFileSize(context, (long) totalRam);
        String freeString = Formatter.formatShortFileSize(context, (long) freeRam);
        CharSequence memString;
        CharSequence[] memStatesStr = getResources().getTextArray(R.array.ram_states);
        int memState = mStatsManager.getMemState();
        if (memState >= 0 && memState < memStatesStr.length - 1) {
            memString = memStatesStr[memState];
        } else {
            memString = memStatesStr[memStatesStr.length - 1];
        }
        mMemStatus.setText(TextUtils.expandTemplate(getText(R.string.storage_size_large),
                usedResult.value, usedResult.units));
        float usedRatio = (float)(usedRam / (freeRam + usedRam));
        mColors.setRatios(usedRatio, 0, 1 - usedRatio);

        mPerformance.setSummary(memString);
        mTotalMemory.setSummary(totalString);
        mAverageUsed.setSummary(Utils.formatPercentage((long) usedRam, (long) totalRam));
        mFree.setSummary(freeString);
        String durationString = getString(sDurationLabels[mDurationIndex]);
        int numApps = mStatsManager.getEntries().size();
        mAppListPreference.setSummary(getResources().getQuantityString(
                R.plurals.memory_usage_apps_summary, numApps, numApps, durationString));
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.PROCESS_STATS_SUMMARY;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mAppListPreference) {
            Bundle args = new Bundle();
            args.putBoolean(ARG_TRANSFER_STATS, true);
            args.putInt(ARG_DURATION_INDEX, mDurationIndex);
            mStatsManager.xferStats();
            startFragment(this, ProcessStatsUi.class.getName(), R.string.app_list_memory_use, 0,
                    args);
            return true;
        } else if (preference == mAppStartupPreference) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.putExtra("appops_tab", getString(R.string.app_ops_categories_bootup));
            intent.setClass(mActivity, AppOpsSummaryActivity.class);
            mActivity.startActivity(intent);
            return true;
        }
        return false;
    }

}
