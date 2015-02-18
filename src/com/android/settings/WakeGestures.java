/*
 * Copyright (C) 2015 AICP
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

package com.android.settings;

import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.util.FileUtils;

public class WakeGestures extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, OnPreferenceClickListener {

    private static final String TAG = "WakeGestures";

    private static String SWEEP2WAKE_PATH = "/sys/android_touch/sweep2wake";
    private static String SWEEP2SLEEP_PATH = "/sys/android_touch/sweep2sleep";

    private static final String KEY_SWEEP_TO_WAKE_SWITCH = "sweep_to_wake_switch";
    private static final String KEY_SWEEP_TO_SLEEP_SWITCH = "sweep_to_sleep_switch";

    private static final String KEY_SWEEP_TO_WAKE_RIGHT = "sweep_to_wake_right";
    private static final String KEY_SWEEP_TO_WAKE_LEFT = "sweep_to_wake_left";
    private static final String KEY_SWEEP_TO_WAKE_UP = "sweep_to_wake_up";
    private static final String KEY_SWEEP_TO_WAKE_DOWN = "sweep_to_wake_down";
    private static final String KEY_SWEEP_TO_SLEEP_RIGHT = "sweep_to_sleep_right";
    private static final String KEY_SWEEP_TO_SLEEP_LEFT = "sweep_to_sleep_left";

    private static CheckBoxPreference mSweepToWakeRight;
    private static CheckBoxPreference mSweepToWakeLeft;
    private static CheckBoxPreference mSweepToWakeUp;
    private static CheckBoxPreference mSweepToWakeDown;
    private static CheckBoxPreference mSweepToSleepRight;
    private static CheckBoxPreference mSweepToSleepLeft;
    private static SwitchPreference mSweepToWakeSwitch;
    private static SwitchPreference mSweepToSleepSwitch;

    protected Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContentResolver resolver = getActivity().getContentResolver();

        addPreferencesFromResource(R.xml.wake_gestures);

        PreferenceScreen prefSet = getPreferenceScreen();

        mContext = getActivity().getApplicationContext();

        mSweepToWakeSwitch =
            (SwitchPreference) prefSet.findPreference(KEY_SWEEP_TO_WAKE_SWITCH);

        mSweepToSleepSwitch =
            (SwitchPreference) prefSet.findPreference(KEY_SWEEP_TO_SLEEP_SWITCH);

        mSweepToWakeRight =
            (CheckBoxPreference) prefSet.findPreference(KEY_SWEEP_TO_WAKE_RIGHT);
        mSweepToWakeRight.setOnPreferenceChangeListener(this);

        mSweepToWakeLeft =
            (CheckBoxPreference) prefSet.findPreference(KEY_SWEEP_TO_WAKE_LEFT);
        mSweepToWakeLeft.setOnPreferenceChangeListener(this);

        mSweepToWakeUp =
            (CheckBoxPreference) prefSet.findPreference(KEY_SWEEP_TO_WAKE_UP);
        mSweepToWakeUp.setOnPreferenceChangeListener(this);

        mSweepToWakeDown =
            (CheckBoxPreference) prefSet.findPreference(KEY_SWEEP_TO_WAKE_DOWN);
        mSweepToWakeDown.setOnPreferenceChangeListener(this);

        mSweepToSleepRight =
            (CheckBoxPreference) prefSet.findPreference(KEY_SWEEP_TO_SLEEP_RIGHT);
        mSweepToSleepRight.setOnPreferenceChangeListener(this);

        mSweepToSleepLeft =
            (CheckBoxPreference) prefSet.findPreference(KEY_SWEEP_TO_SLEEP_LEFT);
        mSweepToSleepLeft.setOnPreferenceChangeListener(this);

        writeToWakeFile();
        writeToSleepFile();

    }

    @Override
    public void onResume() {
        super.onResume();
        writeToWakeFile();
        writeToSleepFile();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final String key = preference.getKey();
        if (preference == mSweepToWakeRight) {
            boolean value = (Boolean) newValue;
            writeToWakeFile();
        } else if (preference == mSweepToWakeLeft) {
            boolean value = (Boolean) newValue;
            writeToWakeFile();
        } else if (preference == mSweepToWakeUp) {
            boolean value = (Boolean) newValue;
            writeToWakeFile();
        } else if (preference == mSweepToWakeDown) {
            boolean value = (Boolean) newValue;
            writeToWakeFile();
        } else if (preference == mSweepToSleepRight) {
            boolean value = (Boolean) newValue;
            writeToSleepFile();
        } else if (preference == mSweepToSleepLeft) {
            boolean value = (Boolean) newValue;
            writeToSleepFile();
        } else if (preference == mSweepToWakeSwitch) {
            boolean value = (Boolean) newValue;
            writeToWakeFile();
        } else if (preference == mSweepToSleepSwitch) {
            boolean value = (Boolean) newValue;
            writeToSleepFile();
        }
        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        return false;
    }

    private void writeToWakeFile() {
        FileUtils.writeLine(SWEEP2WAKE_PATH, "0");
        if (mSweepToWakeSwitch.isChecked()) {
            if (mSweepToWakeRight.isChecked() && !mSweepToWakeLeft.isChecked() &&
                    !mSweepToWakeUp.isChecked() && !mSweepToWakeDown.isChecked()) {
                FileUtils.writeLine(SWEEP2WAKE_PATH, "1");
            } else if (!mSweepToWakeRight.isChecked() && mSweepToWakeLeft.isChecked() &&
                    !mSweepToWakeUp.isChecked() && !mSweepToWakeDown.isChecked()) {
                FileUtils.writeLine(SWEEP2WAKE_PATH, "2");
            } else if (!mSweepToWakeRight.isChecked() && !mSweepToWakeLeft.isChecked() &&
                    mSweepToWakeUp.isChecked() && !mSweepToWakeDown.isChecked()) {
                FileUtils.writeLine(SWEEP2WAKE_PATH, "4");
            } else if (!mSweepToWakeRight.isChecked() && !mSweepToWakeLeft.isChecked() &&
                    !mSweepToWakeUp.isChecked() && mSweepToWakeDown.isChecked()) {
                FileUtils.writeLine(SWEEP2WAKE_PATH, "8");
            } else if (mSweepToWakeRight.isChecked() && mSweepToWakeLeft.isChecked() &&
                    !mSweepToWakeUp.isChecked() && !mSweepToWakeDown.isChecked()) {
                FileUtils.writeLine(SWEEP2WAKE_PATH, "3");
            } else if (mSweepToWakeRight.isChecked() && !mSweepToWakeLeft.isChecked() &&
                    mSweepToWakeUp.isChecked() && !mSweepToWakeDown.isChecked()) {
                FileUtils.writeLine(SWEEP2WAKE_PATH, "5");
            } else if (mSweepToWakeRight.isChecked() && !mSweepToWakeLeft.isChecked() &&
                    !mSweepToWakeUp.isChecked() && mSweepToWakeDown.isChecked()) {
                FileUtils.writeLine(SWEEP2WAKE_PATH, "9");
            } else if (!mSweepToWakeRight.isChecked() && mSweepToWakeLeft.isChecked() &&
                    mSweepToWakeUp.isChecked() && !mSweepToWakeDown.isChecked()) {
                FileUtils.writeLine(SWEEP2WAKE_PATH, "6");
            } else if (mSweepToWakeRight.isChecked() && mSweepToWakeLeft.isChecked() &&
                    mSweepToWakeUp.isChecked() && !mSweepToWakeDown.isChecked()) {
                FileUtils.writeLine(SWEEP2WAKE_PATH, "7");
            } else if (!mSweepToWakeRight.isChecked() && mSweepToWakeLeft.isChecked() &&
                    !mSweepToWakeUp.isChecked() && mSweepToWakeDown.isChecked()) {
                FileUtils.writeLine(SWEEP2WAKE_PATH, "10");
            } else if (mSweepToWakeRight.isChecked() && mSweepToWakeLeft.isChecked() &&
                    !mSweepToWakeUp.isChecked() && mSweepToWakeDown.isChecked()) {
                FileUtils.writeLine(SWEEP2WAKE_PATH, "11");
            } else if (!mSweepToWakeRight.isChecked() && !mSweepToWakeLeft.isChecked() &&
                    mSweepToWakeUp.isChecked() && mSweepToWakeDown.isChecked()) {
                FileUtils.writeLine(SWEEP2WAKE_PATH, "12");
            } else if (mSweepToWakeRight.isChecked() && !mSweepToWakeLeft.isChecked() &&
                    mSweepToWakeUp.isChecked() && mSweepToWakeDown.isChecked()) {
                FileUtils.writeLine(SWEEP2WAKE_PATH, "13");
            } else if (!mSweepToWakeRight.isChecked() && mSweepToWakeLeft.isChecked() &&
                    mSweepToWakeUp.isChecked() && mSweepToWakeDown.isChecked()) {
                FileUtils.writeLine(SWEEP2WAKE_PATH, "14");
            } else if (mSweepToWakeRight.isChecked() && mSweepToWakeLeft.isChecked() &&
                    mSweepToWakeUp.isChecked() && mSweepToWakeDown.isChecked()) {
                FileUtils.writeLine(SWEEP2WAKE_PATH, "15");
            }
        } else if (!mSweepToWakeSwitch.isChecked()) {
            FileUtils.writeLine(SWEEP2WAKE_PATH, "0");
        }
    }

    private void writeToSleepFile() {
        FileUtils.writeLine(SWEEP2SLEEP_PATH, "0");
        if (mSweepToSleepSwitch.isChecked()) {
            if (mSweepToSleepRight.isChecked() && !mSweepToSleepLeft.isChecked()) {
                FileUtils.writeLine(SWEEP2SLEEP_PATH, "1");
            } else if (!mSweepToSleepRight.isChecked() && mSweepToSleepLeft.isChecked()) {
                FileUtils.writeLine(SWEEP2SLEEP_PATH, "2");
            } else if (mSweepToSleepRight.isChecked() && mSweepToSleepLeft.isChecked()) {
                FileUtils.writeLine(SWEEP2SLEEP_PATH, "3");
            }
        } else if (!mSweepToSleepSwitch.isChecked()) {
            FileUtils.writeLine(SWEEP2SLEEP_PATH, "0");
        }
    }

}
