/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.notification;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.util.SparseArray;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;

import java.util.ArrayList;
import java.util.List;

public class ZenModeSettings extends ZenModeSettingsBase implements Indexable {
    private static final String KEY_PRIORITY_SETTINGS = "priority_settings";
    private static final String KEY_AUTOMATION_SETTINGS = "automation_settings";
    private static final String KEY_ZEN_ACCESS = "manage_zen_access";

    private Preference mPrioritySettings;
    private Preference mZenAccess;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.zen_mode_settings);
        final PreferenceScreen root = getPreferenceScreen();

        mPrioritySettings = root.findPreference(KEY_PRIORITY_SETTINGS);
        if (!isScheduleSupported(mContext)) {
            removePreference(KEY_AUTOMATION_SETTINGS);
        }

        mZenAccess = findPreference(KEY_ZEN_ACCESS);
        refreshZenAccess();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateControls();
        refreshZenAccess();
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.NOTIFICATION_ZEN_MODE;
    }

    @Override
    protected void onZenModeChanged() {
        updateControls();
    }

    @Override
    protected void onZenModeConfigChanged() {
        updateControls();
    }

    private void updateControls() {
        updatePrioritySettingsSummary();
    }

    private void updatePrioritySettingsSummary() {
        final ArrayList<String> items = new ArrayList<>();
        items.add(getString(R.string.zen_mode_alarms));
        if (mConfig.allowReminders) {
            items.add(getString(R.string.zen_mode_summary_reminders));
        }
        if (mConfig.allowEvents) {
            items.add(getString(R.string.zen_mode_summary_events));
        }
        if (mConfig.allowCalls || mConfig.allowRepeatCallers) {
            items.add(getString(R.string.zen_mode_summary_selected_callers));
        }
        if (mConfig.allowMessages) {
            items.add(getString(R.string.zen_mode_summary_selected_messages));
        }
        mPrioritySettings.setSummary(Utils.join(getResources(), items));
    }

    private static SparseArray<String> allKeyTitles(Context context) {
        final SparseArray<String> rt = new SparseArray<String>();
        rt.put(R.string.zen_mode_priority_settings_title, KEY_PRIORITY_SETTINGS);
        rt.put(R.string.zen_mode_automation_settings_title, KEY_AUTOMATION_SETTINGS);
        return rt;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_uri_interruptions;
    }

    // Enable indexing of searchable data
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {

            @Override
            public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
                final SparseArray<String> keyTitles = allKeyTitles(context);
                final int N = keyTitles.size();
                final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>(N);
                final Resources res = context.getResources();
                for (int i = 0; i < N; i++) {
                    final SearchIndexableRaw data = new SearchIndexableRaw(context);
                    data.key = keyTitles.valueAt(i);
                    data.title = res.getString(keyTitles.keyAt(i));
                    data.screenTitle = res.getString(R.string.zen_mode_settings_title);
                    result.add(data);
                }
                return result;
            }

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                final ArrayList<String> rt = new ArrayList<String>();
                if (!isScheduleSupported(context)) {
                    rt.add(KEY_AUTOMATION_SETTINGS);
                }
                return rt;
            }
        };

    // === Zen access ===

    private void refreshZenAccess() {
        // noop for now
    }
}
