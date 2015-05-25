/*
 * Copyright (C) 2013 SlimRoms Project
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

package com.android.settings.slim;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.TelephonyManager;
import android.text.Spannable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.util.Helpers;
import com.android.settings.widget.SeekBarPreferenceCham;

import com.android.internal.util.slim.DeviceUtils;
import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class StatusBar extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String TAG = "StatusBarSettings";

    private static final String STATUS_BAR_BRIGHTNESS_CONTROL = "status_bar_brightness_control";
    private static final String KEY_STATUS_BAR_CLOCK = "clock_style_pref";
    private static final String KEY_CARRIERLABEL_PREFERENCE = "carrier_options";
    private static final String KEY_STATUS_BAR_NETWORK_ARROWS= "status_bar_show_network_activity";
    private static final String KEY_STATUS_BAR_GREETING = "status_bar_greeting";
    private static final String KEY_STATUS_BAR_GREETING_TIMEOUT = "status_bar_greeting_timeout";
    private static final String KEY_AICP_LOGO_COLOR = "status_bar_aicp_logo_color";
    private static final String SHOW_FOURG = "show_fourg";

    private SwitchPreference mStatusBarBrightnessControl;
    private PreferenceScreen mCarrierLabel;
    private PreferenceScreen mClockStyle;
    private SwitchPreference mNetworkArrows;
    private SwitchPreference mStatusBarGreeting;
    private SeekBarPreferenceCham mStatusBarGreetingTimeout;
    private ColorPickerPreference mAicpLogoColor;
    private SwitchPreference mShowFourG;

    private String mCustomGreetingText = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.status_bar_settings);

        PreferenceScreen prefSet = getPreferenceScreen();
        PackageManager pm = getPackageManager();
        Resources systemUiResources;
        try {
            systemUiResources = pm.getResourcesForApplication("com.android.systemui");
        } catch (Exception e) {
            Log.e(TAG, "can't access systemui resources",e);
            return;
        }

        mStatusBarBrightnessControl =
            (SwitchPreference) prefSet.findPreference(STATUS_BAR_BRIGHTNESS_CONTROL);
        mStatusBarBrightnessControl.setChecked((Settings.System.getInt(getContentResolver(),
                            Settings.System.STATUS_BAR_BRIGHTNESS_CONTROL, 0) == 1));
        mStatusBarBrightnessControl.setOnPreferenceChangeListener(this);

        mClockStyle = (PreferenceScreen) prefSet.findPreference(KEY_STATUS_BAR_CLOCK);

        mCarrierLabel = (PreferenceScreen) prefSet.findPreference(KEY_CARRIERLABEL_PREFERENCE);
        if (Utils.isWifiOnly(getActivity())) {
            prefSet.removePreference(mCarrierLabel);
        }

        // Network arrows
        mNetworkArrows = (SwitchPreference) prefSet.findPreference(KEY_STATUS_BAR_NETWORK_ARROWS);
        mNetworkArrows.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
            Settings.System.STATUS_BAR_SHOW_NETWORK_ACTIVITY, 1) == 1);
        mNetworkArrows.setOnPreferenceChangeListener(this);
        int networkArrows = Settings.System.getInt(getContentResolver(),
                Settings.System.STATUS_BAR_SHOW_NETWORK_ACTIVITY, 1);
        updateNetworkArrowsSummary(networkArrows);

        updateClockStyleDescription();

        // Greeting
        mStatusBarGreeting = (SwitchPreference) prefSet.findPreference(KEY_STATUS_BAR_GREETING);
        mCustomGreetingText = Settings.System.getString(getActivity().getContentResolver(),
                Settings.System.STATUS_BAR_GREETING);
        boolean greeting = mCustomGreetingText != null && !TextUtils.isEmpty(mCustomGreetingText);
        mStatusBarGreeting.setChecked(greeting);

        mStatusBarGreetingTimeout =
                (SeekBarPreferenceCham) prefSet.findPreference(KEY_STATUS_BAR_GREETING_TIMEOUT);
        int statusBarGreetingTimeout = Settings.System.getInt(getContentResolver(),
                Settings.System.STATUS_BAR_GREETING_TIMEOUT, 400);
        mStatusBarGreetingTimeout.setValue(statusBarGreetingTimeout / 1);
        mStatusBarGreetingTimeout.setOnPreferenceChangeListener(this);

        if (TelephonyManager.getDefault().getPhoneCount() <= 1) {
            removePreference(Settings.System.STATUS_BAR_MSIM_SHOW_EMPTY_ICONS);
        }

        // Aicp logo color
        mAicpLogoColor =
            (ColorPickerPreference) prefSet.findPreference(KEY_AICP_LOGO_COLOR);
        mAicpLogoColor.setOnPreferenceChangeListener(this);
        int intColor = Settings.System.getInt(getContentResolver(),
                Settings.System.STATUS_BAR_AICP_LOGO_COLOR, 0xffffffff);
        String hexColor = String.format("#%08x", (0xffffffff & intColor));
            mAicpLogoColor.setSummary(hexColor);
            mAicpLogoColor.setNewPreviewColor(intColor);

        //  4G LTE switch
        mShowFourG = (SwitchPreference) prefSet.findPreference(SHOW_FOURG);
        if (Utils.isWifiOnly(getActivity())) {
            prefSet.removePreference(mShowFourG);
        } else {
            mShowFourG.setChecked((Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.SHOW_FOURG, 0) == 1));
        }
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mStatusBarBrightnessControl) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.STATUS_BAR_BRIGHTNESS_CONTROL,
                    (Boolean) newValue ? 1 : 0);
            return true;
        } else if (preference == mNetworkArrows) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.STATUS_BAR_SHOW_NETWORK_ACTIVITY,
                    (Boolean) newValue ? 1 : 0);
            int networkArrows = Settings.System.getInt(getContentResolver(),
                    Settings.System.STATUS_BAR_SHOW_NETWORK_ACTIVITY, 1);
            updateNetworkArrowsSummary(networkArrows);
            return true;
        } else if (preference == mStatusBarGreetingTimeout) {
            int timeout = (Integer) newValue;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_GREETING_TIMEOUT, timeout * 1);
            return true;
        } else if (preference == mAicpLogoColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.STATUS_BAR_AICP_LOGO_COLOR, intHex);
            return true;  
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
       if (preference == mStatusBarGreeting) {
           boolean enabled = mStatusBarGreeting.isChecked();
           if (enabled) {
                AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

                alert.setTitle(R.string.status_bar_greeting_title);
                alert.setMessage(R.string.status_bar_greeting_dialog);

                // Set an EditText view to get user input
                final EditText input = new EditText(getActivity());
                input.setText(mCustomGreetingText != null ? mCustomGreetingText :
                        getResources().getString(R.string.status_bar_greeting_main));
                alert.setView(input);
                alert.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String value = ((Spannable) input.getText()).toString();
                        Settings.System.putString(getActivity().getContentResolver(),
                                Settings.System.STATUS_BAR_GREETING, value);
                        updateCheckState(value);
                    }
                });
                alert.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Canceled.
                    }
                });

                alert.show();
            } else {
                Settings.System.putString(getActivity().getContentResolver(),
                        Settings.System.STATUS_BAR_GREETING, "");
            }
        } else if  (preference == mShowFourG) {
            boolean enabled = ((SwitchPreference)preference).isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.SHOW_FOURG, enabled ? 1:0);
            Helpers.restartSystemUI();
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void updateCheckState(String value) {
        if (value == null || TextUtils.isEmpty(value)) mStatusBarGreeting.setChecked(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateClockStyleDescription();
    }

    private void updateClockStyleDescription() {
        if (mClockStyle == null) {
            return;
        }
        if (Settings.System.getInt(getContentResolver(),
               Settings.System.STATUS_BAR_CLOCK, 1) == 1) {
            mClockStyle.setSummary(getString(R.string.enabled));
        } else {
            mClockStyle.setSummary(getString(R.string.disabled));
         }
    }

    private void updateNetworkArrowsSummary(int value) {
        String summary = value != 0
                ? getResources().getString(R.string.enabled)
                : getResources().getString(R.string.disabled);
        mNetworkArrows.setSummary(summary);
    }
}
