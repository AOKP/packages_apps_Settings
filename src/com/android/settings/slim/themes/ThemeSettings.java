/*
 * Copyright (C) 2013 Slimroms
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

package com.android.settings.slim.themes;

import android.app.ActionBar;
import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.ListPreference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Switch;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class ThemeSettings extends SettingsPreferenceFragment {

    private static final String THEME_AUTO_MODE =
        "pref_theme_auto_mode";

    private ListPreference mThemeAutoMode;
    private ThemeEnabler mThemeEnabler;

    private int mCurrentState = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.theme_settings);

        PreferenceScreen prefSet = getPreferenceScreen();

        mThemeAutoMode = (ListPreference) prefSet.findPreference(THEME_AUTO_MODE);
        mThemeAutoMode.setValue(String.valueOf(
                Settings.AOKP.getIntForUser(getContentResolver(),
                Settings.AOKP.UI_THEME_AUTO_MODE, 0,
                UserHandle.USER_CURRENT)));
        mThemeAutoMode.setSummary(mThemeAutoMode.getEntry());

        mThemeAutoMode.setOnPreferenceChangeListener(
            new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference,
                        Object newValue) {
                String val = (String) newValue;
                Settings.AOKP.putInt(getContentResolver(),
                    Settings.AOKP.UI_THEME_AUTO_MODE,
                    Integer.valueOf(val));
                int index = mThemeAutoMode.findIndexOfValue(val);
                mThemeAutoMode.setSummary(
                    mThemeAutoMode.getEntries()[index]);
                return true;
            }
        });

        final Activity activity = getActivity();
        final Switch actionBarSwitch = new Switch(activity);

        if (activity instanceof PreferenceActivity) {
            PreferenceActivity preferenceActivity = (PreferenceActivity) activity;
            if (preferenceActivity.onIsHidingHeaders() || !preferenceActivity.onIsMultiPane()) {
                final int padding = activity.getResources().getDimensionPixelSize(
                        R.dimen.action_bar_switch_padding);
                actionBarSwitch.setPaddingRelative(0, 0, padding, 0);
                activity.getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                        ActionBar.DISPLAY_SHOW_CUSTOM);
                activity.getActionBar().setCustomView(actionBarSwitch, new ActionBar.LayoutParams(
                        ActionBar.LayoutParams.WRAP_CONTENT,
                        ActionBar.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER_VERTICAL | Gravity.END));
            }
        }
        mThemeEnabler = new ThemeEnabler(activity, actionBarSwitch);

    }

    @Override
    public void onResume() {
        super.onResume();
        if (mThemeEnabler != null) {
            mThemeEnabler.resume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mThemeEnabler != null) {
            mThemeEnabler.pause();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.uiThemeMode != mCurrentState && mThemeEnabler != null) {
            mCurrentState = newConfig.uiThemeMode;
            mThemeEnabler.setSwitchState();
        }
    }

}
