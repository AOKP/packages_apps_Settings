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

    public static final String INTENT_EXTRA_SHORTCUT = "aicp.settings.external.shortcut";

    public static final String EXTRA_SHORTCUT_RECENT_APP_SIDEBAR = "recent_app_sidebar";

    public static final String EXTRA_SHORTCUT_WAKELOCK_BLOCKER = "wakelock_blocker";

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

        // Launch shortcut if available
        String shortcut = getActivity().getIntent().getStringExtra(INTENT_EXTRA_SHORTCUT);
        if (EXTRA_SHORTCUT_RECENT_APP_SIDEBAR.equals(shortcut)) {
            Bundle extras = new Bundle();
            extras.putInt("actionMode", 7);
            extras.putInt("maxAllowedActions", -1);
            extras.putBoolean("useAppPickerOnly", true);
            extras.putString("fragment", "com.android.settings.aicp.RecentAppSidebarFragment");
            startFragment(this, "com.android.settings.aicp.dslv.ActionListViewSettings",
                    R.string.recent_app_sidebar_title, 0, extras);
            getActivity().finish();
        } else if (EXTRA_SHORTCUT_WAKELOCK_BLOCKER.equals(shortcut)) {
            startFragment(this, "com.android.settings.aicp.WakelockBlocker",
                    R.string.wakelock_blocker_title, 0, null);
            getActivity().finish();
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
