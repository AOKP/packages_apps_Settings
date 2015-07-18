/*
 * Copyright (C) 2010 The Android Open Source Project
 * Copyright (C) 2014 The CyanogenMod Project
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

import com.android.internal.view.RotationPolicy;
import com.android.settings.notification.DropDownPreference;
import com.android.settings.notification.DropDownPreference.Callback;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import static android.hardware.CmHardwareManager.FEATURE_TAP_TO_WAKE;
import static android.provider.Settings.Secure.WAKE_GESTURE_ENABLED;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;

import android.app.Activity;
import android.app.ActivityManagerNative;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.hardware.CmHardwareManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.slim.DisplayRotation;

import java.util.ArrayList;
import java.util.List;

import org.cyanogenmod.hardware.CalibrationControl;
import org.cyanogenmod.hardware.SweepToSleep;
import org.cyanogenmod.hardware.SweepToWake;
import com.android.settings.Utils;

public class DisplaySettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, OnPreferenceClickListener, Indexable {
    private static final String TAG = "DisplaySettings";

    /** If there is no setting in the provider, use this. */
    private static final int FALLBACK_SCREEN_TIMEOUT_VALUE = 30000;

    private static final String KEY_DISPLAY_ROTATION = "display_rotation";
    private static final String KEY_SCREEN_TIMEOUT = "screen_timeout";
    private static final String KEY_FONT_SIZE = "font_size";
    private static final String KEY_SCREEN_SAVER = "screensaver";
    private static final String KEY_LIFT_TO_WAKE = "lift_to_wake";
    private static final String KEY_AUTO_BRIGHTNESS = "auto_brightness";
    private static final String KEY_AUTO_ROTATE = "auto_rotate";
    private static final String KEY_WAKEUP_WHEN_PLUGGED_UNPLUGGED = "wakeup_when_plugged_unplugged";
    private static final String KEY_WAKEUP_CATEGORY = "category_wakeup_options";

    private static final String CATEGORY_ADVANCED = "advanced_display_prefs";
    private static final String KEY_SWEEP_TO_SLEEP = "sweep_sleep_gesture";
    private static final String KEY_SWEEP_TO_WAKE = "sweep_wake_gesture";
    private static final String KEY_TAP_TO_WAKE = "double_tap_wake_gesture";
    private static final String KEY_WAKE_GESTURES = "wake_gestures";
    private static final String KEY_SCREEN_OFF_GESTURE_SETTINGS = "screen_off_gesture_settings";
    private static final String KEY_CALIBRATION_CONTROL = "calibration_control";

    private static final String KEY_DOZE_FRAGMENT = "doze_fragment";

    private static final int DLG_GLOBAL_CHANGE_WARNING = 1;

    private static final String ROTATION_ANGLE_0 = "0";
    private static final String ROTATION_ANGLE_90 = "90";
    private static final String ROTATION_ANGLE_180 = "180";
    private static final String ROTATION_ANGLE_270 = "270";

    private PreferenceScreen mDisplaySettings;
    private PreferenceScreen mDisplayRotationPreference;

    private FontDialogPreference mFontSizePref;

    private SwitchPreference mWakeUpWhenPluggedOrUnplugged;
    private PreferenceCategory mWakeUpOptions;

    private final Configuration mCurConfig = new Configuration();

    private ListPreference mScreenTimeoutPreference;
    private Preference mScreenSaverPreference;
    private SwitchPreference mLiftToWakePreference;
    private SwitchPreference mAutoBrightnessPreference;
    private SwitchPreference mCalibrationControl;
    private SwitchPreference mSweepToSleep;
    private SwitchPreference mSweepToWake;
    private SwitchPreference mTapToWake;
    private PreferenceScreen mWakeGestures;
    private PreferenceScreen mDozeFragement;
    private SwitchPreference mProximityWake;

    private CmHardwareManager mCmHardwareManager;

    private ContentObserver mAccelerometerRotationObserver =
            new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            updateDisplayRotationPreferenceDescription();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Activity activity = getActivity();
        final ContentResolver resolver = activity.getContentResolver();
        mCmHardwareManager = (CmHardwareManager) activity.getSystemService(Context.CMHW_SERVICE);

        addPreferencesFromResource(R.xml.display_settings);

        PreferenceScreen prefSet = getPreferenceScreen();

        mDisplaySettings = (PreferenceScreen) findPreference("display_settings");

        mScreenSaverPreference = findPreference(KEY_SCREEN_SAVER);
        if (mScreenSaverPreference != null
                && getResources().getBoolean(
                        com.android.internal.R.bool.config_dreamsSupported) == false) {
            getPreferenceScreen().removePreference(mScreenSaverPreference);
        }

        mScreenTimeoutPreference = (ListPreference) findPreference(KEY_SCREEN_TIMEOUT);
        final long currentTimeout = Settings.System.getLong(resolver, SCREEN_OFF_TIMEOUT,
                FALLBACK_SCREEN_TIMEOUT_VALUE);
        mScreenTimeoutPreference.setValue(String.valueOf(currentTimeout));
        mScreenTimeoutPreference.setOnPreferenceChangeListener(this);
        disableUnusableTimeouts(mScreenTimeoutPreference);
        updateTimeoutPreferenceDescription(currentTimeout);

        mFontSizePref = (FontDialogPreference) findPreference(KEY_FONT_SIZE);
        mFontSizePref.setOnPreferenceChangeListener(this);
        mFontSizePref.setOnPreferenceClickListener(this);

        if (isAutomaticBrightnessAvailable(getResources())) {
            mAutoBrightnessPreference = (SwitchPreference) findPreference(KEY_AUTO_BRIGHTNESS);
            mAutoBrightnessPreference.setOnPreferenceChangeListener(this);
        } else {
            removePreference(KEY_AUTO_BRIGHTNESS);
        }

        // Proximity wake up
        mProximityWake = (SwitchPreference) findPreference("proximity_on_wake");
        boolean proximityCheckOnWait = getResources().getBoolean(
                com.android.internal.R.bool.config_proximityCheckOnWake);
        if (!proximityCheckOnWait) {
            mDisplaySettings.removePreference(mProximityWake);
            Settings.System.putInt(getContentResolver(), Settings.System.PROXIMITY_ON_WAKE, 0);
        }

        if (isLiftToWakeAvailable(activity)) {
            mLiftToWakePreference = (SwitchPreference) findPreference(KEY_LIFT_TO_WAKE);
            mLiftToWakePreference.setOnPreferenceChangeListener(this);
        } else {
            removePreference(KEY_LIFT_TO_WAKE);
         }

        PreferenceCategory advancedPrefs = (PreferenceCategory) findPreference(CATEGORY_ADVANCED);

        mDozeFragement = (PreferenceScreen) findPreference(KEY_DOZE_FRAGMENT);
        if (!Utils.isDozeAvailable(activity)) {
            getPreferenceScreen().removePreference(mDozeFragement);
        }

        if (RotationPolicy.isRotationSupported(activity)) {
            mDisplayRotationPreference = (PreferenceScreen) findPreference(KEY_DISPLAY_ROTATION);
        } else {
            removePreference(KEY_DISPLAY_ROTATION);
        }

        mWakeUpOptions = (PreferenceCategory) prefSet.findPreference(KEY_WAKEUP_CATEGORY);
        mWakeUpWhenPluggedOrUnplugged =
            (SwitchPreference) findPreference(KEY_WAKEUP_WHEN_PLUGGED_UNPLUGGED);

        // hide option if device is already set to never wake up
        if(!getResources().getBoolean(
                com.android.internal.R.bool.config_unplugTurnsOnScreen)) {
                mWakeUpOptions.removePreference(mWakeUpWhenPluggedOrUnplugged);
                prefSet.removePreference(mWakeUpOptions);
        } else {
            mWakeUpWhenPluggedOrUnplugged.setChecked(Settings.System.getInt(resolver,
                        Settings.System.WAKEUP_WHEN_PLUGGED_UNPLUGGED, 1) == 1);
            mWakeUpWhenPluggedOrUnplugged.setOnPreferenceChangeListener(this);
        }

        mTapToWake = (SwitchPreference) findPreference(KEY_TAP_TO_WAKE);

        if (advancedPrefs != null && mTapToWake != null
                && !mCmHardwareManager.isSupported(FEATURE_TAP_TO_WAKE)) {
            advancedPrefs.removePreference(mTapToWake);
            mTapToWake = null;
        }

        mWakeGestures = (PreferenceScreen) findPreference(KEY_WAKE_GESTURES);

        mSweepToWake = (SwitchPreference) findPreference(KEY_SWEEP_TO_WAKE);
        if (!isSweepToWakeSupported()) {
            advancedPrefs.removePreference(mSweepToWake);
            advancedPrefs.removePreference(mWakeGestures);
            mSweepToWake = null;
        } else if (isSweepToWakeSupported() && !areWakeGesturesAvailable(getResources())) {
            advancedPrefs.removePreference(mWakeGestures);
        } else if (isSweepToWakeSupported() && areWakeGesturesAvailable(getResources())) {
            advancedPrefs.removePreference(mSweepToWake);
            mSweepToWake = null;
        }

        mSweepToSleep = (SwitchPreference) findPreference(KEY_SWEEP_TO_SLEEP);
        if (!isSweepToSleepSupported()) {
            advancedPrefs.removePreference(mSweepToSleep);
            advancedPrefs.removePreference(mWakeGestures);
            mSweepToSleep = null;
        } else if (isSweepToSleepSupported() && !areWakeGesturesAvailable(getResources())) {
            advancedPrefs.removePreference(mWakeGestures);
        } else if (isSweepToSleepSupported() && areWakeGesturesAvailable(getResources())) {
            advancedPrefs.removePreference(mSweepToSleep);
            mSweepToSleep = null;
        }

        mCalibrationControl = (SwitchPreference) findPreference(KEY_CALIBRATION_CONTROL);
        if (!isCalibrationControlSupported() || !getResources().getBoolean(
                R.bool.config_hasCalibrationControl)) {
            advancedPrefs.removePreference(mCalibrationControl);
            mCalibrationControl = null;
        }

        Utils.updatePreferenceToSpecificActivityFromMetaDataOrRemove(getActivity(),
                getPreferenceScreen(), KEY_SCREEN_OFF_GESTURE_SETTINGS);

        if (!mCmHardwareManager.isSupported(FEATURE_TAP_TO_WAKE) && !isSweepToWakeSupported()
                && !isSweepToSleepSupported() && !isCalibrationControlSupported() ) {
                prefSet.removePreference(advancedPrefs);
        }

    }

    private static boolean allowAllRotations(Context context) {
        return Resources.getSystem().getBoolean(
                com.android.internal.R.bool.config_allowAllRotations);
    }

    private static boolean isLiftToWakeAvailable(Context context) {
        SensorManager sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        return sensors != null && sensors.getDefaultSensor(Sensor.TYPE_WAKE_GESTURE) != null;
    }

    private static boolean isAutomaticBrightnessAvailable(Resources res) {
        return res.getBoolean(com.android.internal.R.bool.config_automatic_brightness_available);
    }

    private void updateTimeoutPreferenceDescription(long currentTimeout) {
        ListPreference preference = mScreenTimeoutPreference;
        String summary;
        if (currentTimeout < 0) {
            // Unsupported value
            summary = "";
        } else {
            final CharSequence[] entries = preference.getEntries();
            final CharSequence[] values = preference.getEntryValues();
            if (entries == null || entries.length == 0) {
                summary = "";
            } else {
                int best = 0;
                for (int i = 0; i < values.length; i++) {
                    long timeout = Long.parseLong(values[i].toString());
                    if (currentTimeout >= timeout) {
                        best = i;
                    }
                }
                summary = preference.getContext().getString(R.string.screen_timeout_summary,
                        entries[best]);
            }
        }
        preference.setSummary(summary);
    }

    private void updateDisplayRotationPreferenceDescription() {
        if (mDisplayRotationPreference == null) {
            return;
        }
        PreferenceScreen preference = mDisplayRotationPreference;
        StringBuilder summary = new StringBuilder();
        Boolean rotationEnabled = Settings.System.getInt(getContentResolver(),
                Settings.System.ACCELEROMETER_ROTATION, 0) != 0;
        int mode = Settings.System.getInt(getContentResolver(),
                Settings.System.ACCELEROMETER_ROTATION_ANGLES,
                DisplayRotation.ROTATION_0_MODE|DisplayRotation.ROTATION_90_MODE
                |DisplayRotation.ROTATION_270_MODE);

        if (!rotationEnabled) {
            summary.append(getString(R.string.display_rotation_disabled));
        } else {
            ArrayList<String> rotationList = new ArrayList<String>();
            String delim = "";
            if ((mode & DisplayRotation.ROTATION_0_MODE) != 0) {
                rotationList.add(ROTATION_ANGLE_0);
            }
            if ((mode & DisplayRotation.ROTATION_90_MODE) != 0) {
                rotationList.add(ROTATION_ANGLE_90);
            }
            if ((mode & DisplayRotation.ROTATION_180_MODE) != 0) {
                rotationList.add(ROTATION_ANGLE_180);
            }
            if ((mode & DisplayRotation.ROTATION_270_MODE) != 0) {
                rotationList.add(ROTATION_ANGLE_270);
            }
            for (int i = 0; i < rotationList.size(); i++) {
                summary.append(delim).append(rotationList.get(i));
                if ((rotationList.size() - i) > 2) {
                    delim = ", ";
                } else {
                    delim = " & ";
                }
            }
            summary.append(" " + getString(R.string.display_rotation_unit));
        }
        preference.setSummary(summary);
    }

    private void disableUnusableTimeouts(ListPreference screenTimeoutPreference) {
        final DevicePolicyManager dpm =
                (DevicePolicyManager) getActivity().getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        final long maxTimeout = dpm != null ? dpm.getMaximumTimeToLock(null) : 0;
        if (maxTimeout == 0) {
            return; // policy not enforced
        }
        final CharSequence[] entries = screenTimeoutPreference.getEntries();
        final CharSequence[] values = screenTimeoutPreference.getEntryValues();
        ArrayList<CharSequence> revisedEntries = new ArrayList<CharSequence>();
        ArrayList<CharSequence> revisedValues = new ArrayList<CharSequence>();
        for (int i = 0; i < values.length; i++) {
            long timeout = Long.parseLong(values[i].toString());
            if (timeout <= maxTimeout) {
                revisedEntries.add(entries[i]);
                revisedValues.add(values[i]);
            }
        }
        if (revisedEntries.size() != entries.length || revisedValues.size() != values.length) {
            final int userPreference = Integer.parseInt(screenTimeoutPreference.getValue());
            screenTimeoutPreference.setEntries(
                    revisedEntries.toArray(new CharSequence[revisedEntries.size()]));
            screenTimeoutPreference.setEntryValues(
                    revisedValues.toArray(new CharSequence[revisedValues.size()]));
            if (userPreference <= maxTimeout) {
                screenTimeoutPreference.setValue(String.valueOf(userPreference));
            } else if (revisedValues.size() > 0
                    && Long.parseLong(revisedValues.get(revisedValues.size() - 1).toString())
                    == maxTimeout) {
                // If the last one happens to be the same as the max timeout, select that
                screenTimeoutPreference.setValue(String.valueOf(maxTimeout));
            } else {
                // There will be no highlighted selection since nothing in the list matches
                // maxTimeout. The user can still select anything less than maxTimeout.
                // TODO: maybe append maxTimeout to the list and mark selected.
            }
        }
        screenTimeoutPreference.setEnabled(revisedEntries.size() > 0);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mTapToWake != null) {
            mTapToWake.setChecked(mCmHardwareManager.get(FEATURE_TAP_TO_WAKE));
        }

        if (mSweepToWake != null) {
            mSweepToWake.setChecked(SweepToWake.isEnabled());
        }

        if (mSweepToSleep != null) {
            mSweepToSleep.setChecked(SweepToSleep.isEnabled());
        }

        if (mCalibrationControl != null) {
            mCalibrationControl.setChecked(CalibrationControl.isEnabled());
        }

        updateState();
        getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION), true,
                mAccelerometerRotationObserver);
        updateDisplayRotationPreferenceDescription();
    }

    @Override
    public void onPause() {
        super.onPause();
        getContentResolver().unregisterContentObserver(mAccelerometerRotationObserver);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        if (dialogId == DLG_GLOBAL_CHANGE_WARNING) {
            return Utils.buildGlobalChangeWarningDialog(getActivity(),
                    R.string.global_font_change_title,
                    new Runnable() {
                        public void run() {
                            mFontSizePref.click();
                        }
                    });
        }
        return null;
    }

    private void updateState() {
        readFontSizePreference(mFontSizePref);
        updateScreenSaverSummary();

        // Update auto brightness if it is available.
        if (mAutoBrightnessPreference != null) {
            int brightnessMode = Settings.System.getInt(getContentResolver(),
                    SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_MANUAL);
            mAutoBrightnessPreference.setChecked(brightnessMode != SCREEN_BRIGHTNESS_MODE_MANUAL);
        }

        // Update lift-to-wake if it is available.
        if (mLiftToWakePreference != null) {
            int value = Settings.Secure.getInt(getContentResolver(), WAKE_GESTURE_ENABLED, 0);
            mLiftToWakePreference.setChecked(value != 0);
        }
    }

    private void updateScreenSaverSummary() {
        if (mScreenSaverPreference != null) {
            mScreenSaverPreference.setSummary(
                    DreamSettings.getSummaryTextWithDreamName(getActivity()));
        }
    }

    private static boolean isSweepToWakeSupported() {
        try {
            return SweepToWake.isSupported();
        } catch (NoClassDefFoundError e) {
            // Hardware abstraction framework not installed
            return false;
        }
    }

    private static boolean isSweepToSleepSupported() {
        try {
            return SweepToSleep.isSupported();
        } catch (NoClassDefFoundError e) {
            // Hardware abstraction framework not installed
            return false;
        }
    }

    private static boolean areWakeGesturesAvailable(Resources res) {
        return res.getBoolean(R.bool.config_wake_gestures_available);
    }

    private static boolean isCalibrationControlSupported() {
        try {
            return CalibrationControl.isSupported();
        } catch (NoClassDefFoundError e) {
            // Hardware abstraction framework not installed
            return false;
        }
    }

    /**
     * Reads the current font size and sets the value in the summary text
     */
    public void readFontSizePreference(Preference pref) {
        try {
            mCurConfig.updateFrom(ActivityManagerNative.getDefault().getConfiguration());
        } catch (RemoteException e) {
            Log.w(TAG, "Unable to retrieve font size");
        }

        // report the current size in the summary text
        final Resources res = getResources();
        String fontDesc = FontDialogPreference.getFontSizeDescription(res, mCurConfig.fontScale);
        pref.setSummary(getString(R.string.summary_font_size, fontDesc));
    }

    public void writeFontSizePreference(Object objValue) {
        try {
            mCurConfig.fontScale = Float.parseFloat(objValue.toString());
            ActivityManagerNative.getDefault().updatePersistentConfiguration(mCurConfig);
        } catch (RemoteException e) {
            Log.w(TAG, "Unable to save font size");
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mTapToWake) {
            return mCmHardwareManager.set(FEATURE_TAP_TO_WAKE, mTapToWake.isChecked());
        } else if (preference == mSweepToWake) {
            return SweepToWake.setEnabled(mSweepToWake.isChecked());
        } else if (preference == mSweepToSleep) {
            return SweepToSleep.setEnabled(mSweepToSleep.isChecked());
        } else if (preference == mCalibrationControl) {
            return CalibrationControl.setEnabled(mCalibrationControl.isChecked());
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if (KEY_SCREEN_TIMEOUT.equals(key)) {
            try {
                int value = Integer.parseInt((String) objValue);
                Settings.System.putInt(getContentResolver(), SCREEN_OFF_TIMEOUT, value);
                updateTimeoutPreferenceDescription(value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist screen timeout setting", e);
            }
        }
        if (KEY_FONT_SIZE.equals(key)) {
            writeFontSizePreference(objValue);
        }
        if (preference == mAutoBrightnessPreference) {
            boolean auto = (Boolean) objValue;
            Settings.System.putInt(getContentResolver(), SCREEN_BRIGHTNESS_MODE,
                    auto ? SCREEN_BRIGHTNESS_MODE_AUTOMATIC : SCREEN_BRIGHTNESS_MODE_MANUAL);
        }
        if (preference == mLiftToWakePreference) {
            boolean value = (Boolean) objValue;
            Settings.Secure.putInt(getContentResolver(), WAKE_GESTURE_ENABLED, value ? 1 : 0);
        }
        if (KEY_WAKEUP_WHEN_PLUGGED_UNPLUGGED.equals(key)) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.WAKEUP_WHEN_PLUGGED_UNPLUGGED,
                    (Boolean) objValue ? 1 : 0);
        }

        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mFontSizePref) {
            if (Utils.hasMultipleUsers(getActivity())) {
                showDialog(DLG_GLOBAL_CHANGE_WARNING);
                return true;
            } else {
                mFontSizePref.click();
            }
        }
        return false;
    }


    /**
     * Restore the properties associated with this preference on boot
     *
     * @param ctx A valid context
     */
    public static void restore(Context ctx) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        CmHardwareManager cmHardwareManager =
            (CmHardwareManager) ctx.getSystemService(Context.CMHW_SERVICE);
        if (cmHardwareManager.isSupported(FEATURE_TAP_TO_WAKE)) {
            final boolean enabled = prefs.getBoolean(KEY_TAP_TO_WAKE,
                cmHardwareManager.get(FEATURE_TAP_TO_WAKE));

            if (!cmHardwareManager.set(FEATURE_TAP_TO_WAKE, enabled)) {
                Log.e(TAG, "Failed to restore tap-to-wake settings.");
            } else {
                Log.d(TAG, "Tap-to-wake settings restored.");
            }
        }

        if (isSweepToWakeSupported()) {
            final boolean enabled = prefs.getBoolean(KEY_SWEEP_TO_WAKE,
                SweepToWake.isEnabled());

            if (!SweepToWake.setEnabled(enabled)) {
                Log.e(TAG, "Failed to restore sweep-to-wake settings.");
            } else {
                Log.d(TAG, "Sweep-to-wake settings restored.");
            }
        }

        if (isSweepToSleepSupported()) {
            final boolean enabled = prefs.getBoolean(KEY_SWEEP_TO_SLEEP,
                SweepToSleep.isEnabled());

            if (!SweepToSleep.setEnabled(enabled)) {
                Log.e(TAG, "Failed to restore sweep-to-sleep settings.");
            } else {
                Log.d(TAG, "Sweep-to-sleep settings restored.");
            }
        }

        if (isCalibrationControlSupported()) {
            final boolean enabled = prefs.getBoolean(KEY_CALIBRATION_CONTROL,
                CalibrationControl.isEnabled());

            if (!CalibrationControl.setEnabled(enabled)) {
                Log.e(TAG, "Failed to restore calibration control settings.");
            } else {
                Log.d(TAG, "Calibration control settings restored.");
            }
        }
    }


    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {

                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.display_settings;
                    result.add(sir);

                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    CmHardwareManager cmHardwareManager =
                        (CmHardwareManager) context.getSystemService(Context.CMHW_SERVICE);
                    ArrayList<String> result = new ArrayList<String>();
                    if (!context.getResources().getBoolean(
                            com.android.internal.R.bool.config_dreamsSupported)) {
                        result.add(KEY_SCREEN_SAVER);
                    }
                    if (!cmHardwareManager.isSupported(FEATURE_TAP_TO_WAKE)) {
                        result.add(KEY_TAP_TO_WAKE);
                    }
                    if (!isAutomaticBrightnessAvailable(context.getResources())) {
                        result.add(KEY_AUTO_BRIGHTNESS);
                    }
                    if (!isLiftToWakeAvailable(context)) {
                        result.add(KEY_LIFT_TO_WAKE);
                    }
                    if (!Utils.isDozeAvailable(context)) {
                        result.add(KEY_DOZE_FRAGMENT);
                    }
                    if (!RotationPolicy.isRotationLockToggleVisible(context)) {
                        result.add(KEY_AUTO_ROTATE);
                    }
                    return result;
                }
            };
}
