/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.settings.deviceinfo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

/**
 * storage settings.
 */
public class StorageSettings extends SettingsPreferenceFragment {

    private static final String TAG = "StorageSettings";

    private static final String KEY_NO_MEDIA_NOTIFICTION = "no_media_notification";

    private CheckBoxPreference mNoMediaNotification;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        addPreferencesFromResource(R.xml.storage_settings);
        root = getPreferenceScreen();

        mNoMediaNotification = (CheckBoxPreference) root.findPreference(KEY_NO_MEDIA_NOTIFICTION);
        mNoMediaNotification.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.STORAGE_MEDIA_REMOVED_NOTIFICTION, 1) == 1);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mNoMediaNotification) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STORAGE_MEDIA_REMOVED_NOTIFICTION, mNoMediaNotification.isChecked() ? 1:0);
            return true;
        }

        return false;
    }
}
