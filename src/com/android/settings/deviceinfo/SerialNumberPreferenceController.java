/*
 * Copyright (C) 2016 The Android Open Source Project
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
import android.os.Build;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

import lineageos.hardware.LineageHardwareManager;

public class SerialNumberPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin {

    private static final String KEY_SERIAL_NUMBER = "serial_number";

    private final String mSerialNumber;

    public SerialNumberPreferenceController(Context context) {
        this(context, Build.getSerial());
    }

    @VisibleForTesting
    SerialNumberPreferenceController(Context context, String serialNumber) {
        super(context);
        LineageHardwareManager hardware = LineageHardwareManager.getInstance(context);
        if (hardware.isSupported(LineageHardwareManager.FEATURE_SERIAL_NUMBER)) {
            mSerialNumber = hardware.getSerialNumber();
        } else {
            mSerialNumber = serialNumber;
        }
    }

    @Override
    public boolean isAvailable() {
        return !TextUtils.isEmpty(mSerialNumber);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final Preference pref = screen.findPreference(KEY_SERIAL_NUMBER);
        if (pref != null) {
            pref.setSummary(mSerialNumber);
        }
    }

    @Override
    public String getPreferenceKey() {
        return KEY_SERIAL_NUMBER;
    }
}
