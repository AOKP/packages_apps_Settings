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

package com.android.settings.cmstats;

import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class PreviewData extends SettingsPreferenceFragment {
    private static final String DEVICE_ID = "preview_device_id";
    private static final String DEVICE_NAME = "preview_device_name";
    private static final String BUILD_VERSION = "preview_build_version";
    private static final String BUILD_DATE = "preview_build_date";
    private static final String RELEASE_TYPE = "preview_release_type";
    private static final String COUNTRY_CODE = "preview_country_code";
    private static final String CARRIER_NAME = "preview_carrier_name";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preview_data);

        final PreferenceScreen prefSet = getPreferenceScreen();
        final Context context = getActivity();

        prefSet.findPreference(DEVICE_ID).setSummary(Utilities.getDeviceID(context));
        prefSet.findPreference(DEVICE_NAME).setSummary(Utilities.getDeviceName());
        prefSet.findPreference(BUILD_VERSION).setSummary(Utilities.getBuildVersion());
        prefSet.findPreference(BUILD_DATE).setSummary(Utilities.getBuildDate());
        prefSet.findPreference(RELEASE_TYPE).setSummary(Utilities.getReleaseType());
        prefSet.findPreference(COUNTRY_CODE).setSummary(Utilities.getCountryCode(context));
        prefSet.findPreference(CARRIER_NAME).setSummary(Utilities.getCarrierName(context));
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.PREVIEW_DATA;
    }
}
