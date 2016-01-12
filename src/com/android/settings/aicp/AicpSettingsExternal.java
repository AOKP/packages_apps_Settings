/*
 * Copyright (C) 2015 The Android Ice Cold Project
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

package com.android.settings.aicp;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import org.cyanogenmod.internal.logging.CMMetricsLogger;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class AicpSettingsExternal extends SettingsPreferenceFragment {

    private static final String TAG = "AicpLabs";

    private static final String PREF_RECENT_APP_SIDEBAR = "recent_app_sidebar_content";

    private PreferenceScreen mRecentAppSidebar;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.aicp_lab_prefs);

        PreferenceScreen prefSet = getPreferenceScreen();
        PackageManager pm = getPackageManager();
        ContentResolver resolver = getActivity().getContentResolver();

        mRecentAppSidebar = (PreferenceScreen) prefSet.findPreference(PREF_RECENT_APP_SIDEBAR);

        if (Settings.System.getInt(resolver,
                    Settings.System.USE_SLIM_RECENTS, 0) == 0) {
            prefSet.removePreference(mRecentAppSidebar);
        }

    }

    @Override
    protected int getMetricsCategory() {
        return CMMetricsLogger.AICPEXTRAS;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

}
