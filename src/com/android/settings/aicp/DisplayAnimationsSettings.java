/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.IActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.Editable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class DisplayAnimationsSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, OnPreferenceClickListener {
    private static final String TAG = "DisplayAnimationsSettings";

    private static final int DIALOG_DENSITY = 101;

    private static final String KEY_LISTVIEW_ANIMATION = "listview_animation";
    private static final String KEY_LISTVIEW_INTERPOLATOR = "listview_interpolator";
    private static final String KEY_TOAST_ANIMATION = "toast_animation";
    private static final String KEY_LCD_DENSITY = "lcd_density";

    private ListPreference mListViewAnimation;
    private ListPreference mListViewInterpolator;
    private ListPreference mToastAnimation;
    private ListPreference mLcdDensityPreference;

    protected Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContentResolver resolver = getActivity().getContentResolver();

        addPreferencesFromResource(R.xml.aicp_display_animations_settings);

        PreferenceScreen prefSet = getPreferenceScreen();

        mContext = getActivity().getApplicationContext();
        int newDensityValue;

        // ListView Animations
        mListViewAnimation = (ListPreference) prefSet.findPreference(KEY_LISTVIEW_ANIMATION);
        int listviewanimation = Settings.System.getInt(resolver,
                Settings.System.LISTVIEW_ANIMATION, 0);
        mListViewAnimation.setValue(String.valueOf(listviewanimation));
        mListViewAnimation.setSummary(mListViewAnimation.getEntry());
        mListViewAnimation.setOnPreferenceChangeListener(this);

        mListViewInterpolator = (ListPreference) prefSet.findPreference(KEY_LISTVIEW_INTERPOLATOR);
        int listviewinterpolator = Settings.System.getInt(resolver,
                Settings.System.LISTVIEW_INTERPOLATOR, 0);
        mListViewInterpolator.setValue(String.valueOf(listviewinterpolator));
        mListViewInterpolator.setSummary(mListViewInterpolator.getEntry());
        mListViewInterpolator.setOnPreferenceChangeListener(this);
        mListViewInterpolator.setEnabled(listviewanimation > 0);

        // Toast Animations
        mToastAnimation = (ListPreference) prefSet.findPreference(KEY_TOAST_ANIMATION);
        mToastAnimation.setSummary(mToastAnimation.getEntry());
        int CurrentToastAnimation = Settings.System.getInt(resolver, Settings.System.TOAST_ANIMATION, 1);
        mToastAnimation.setValueIndex(CurrentToastAnimation); //set to index of default value
        mToastAnimation.setSummary(mToastAnimation.getEntries()[CurrentToastAnimation]);
        mToastAnimation.setOnPreferenceChangeListener(this);

        // LCD density
        mLcdDensityPreference = (ListPreference) prefSet.findPreference(KEY_LCD_DENSITY);
        int defaultDensity = DisplayMetrics.DENSITY_DEVICE;
        String[] densityEntries = new String[9];
        for (int idx = 0; idx < 8; ++idx) {
            int pct = (75 + idx*5);
            densityEntries[idx] = Integer.toString(defaultDensity * pct / 100);
        }
        densityEntries[8] = getString(R.string.custom_density);
        int currentDensity = DisplayMetrics.DENSITY_CURRENT;
        mLcdDensityPreference.setEntries(densityEntries);
        mLcdDensityPreference.setEntryValues(densityEntries);
        mLcdDensityPreference.setValue(String.valueOf(currentDensity));
        mLcdDensityPreference.setOnPreferenceChangeListener(this);
        updateLcdDensityPreferenceDescription(currentDensity);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if (KEY_LISTVIEW_ANIMATION.equals(key)) {
            int value = Integer.parseInt((String) objValue);
            int index = mListViewAnimation.findIndexOfValue((String) objValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LISTVIEW_ANIMATION,
                    value);
            mListViewAnimation.setSummary(mListViewAnimation.getEntries()[index]);
            mListViewInterpolator.setEnabled(value > 0);
        }
        if (KEY_LISTVIEW_INTERPOLATOR.equals(key)) {
            int value = Integer.parseInt((String) objValue);
            int index = mListViewInterpolator.findIndexOfValue((String) objValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LISTVIEW_INTERPOLATOR,
                    value);
            mListViewInterpolator.setSummary(mListViewInterpolator.getEntries()[index]);
        }
        if (KEY_TOAST_ANIMATION.equals(key)) {
            int index = mToastAnimation.findIndexOfValue((String) objValue);
            Settings.System.putString(getContentResolver(),
                    Settings.System.TOAST_ANIMATION, (String) objValue);
            mToastAnimation.setSummary(mToastAnimation.getEntries()[index]);
            Toast.makeText(getActivity(), "Toast test!!!", Toast.LENGTH_SHORT).show();
        }
        if (KEY_LCD_DENSITY.equals(key)) {
            String strValue = (String) objValue;
            if (strValue.equals(getResources().getString(R.string.custom_density))) {
                showDialog(DIALOG_DENSITY);
            } else {
                int value = Integer.parseInt((String) objValue);
                writeLcdDensityPreference(value);
                updateLcdDensityPreferenceDescription(value);
            }
        }
        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        return false;
    }

    private void updateLcdDensityPreferenceDescription(int currentDensity) {
        ListPreference preference = mLcdDensityPreference;
        String summary;
        if (currentDensity < 10 || currentDensity >= 1000) {
            // Unsupported value
            summary = "";
        }
        else {
            summary = Integer.toString(currentDensity) + " DPI";
        }
        preference.setSummary(summary);
    }

    public void writeLcdDensityPreference(int value) {
        try {
            SystemProperties.set("persist.sys.lcd_density", Integer.toString(value));
        }
        catch (Exception e) {
            Log.w(TAG, "Unable to save LCD density");
        }
        try {
            final IActivityManager am = ActivityManagerNative.asInterface(ServiceManager.checkService("activity"));
            if (am != null) {
                am.restart();
            }
        }
        catch (RemoteException e) {
            Log.e(TAG, "Failed to restart");
        }
    }

    public Dialog onCreateDialog(int dialogId) {
        LayoutInflater factory = LayoutInflater.from(mContext);

        switch (dialogId) {
            case DIALOG_DENSITY:
                final View textEntryView = factory.inflate(
                        R.layout.alert_dialog_text_entry, null);
                return new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.custom_density_dialog_title)
                        .setMessage(getResources().getString(R.string.custom_density_dialog_summary))
                        .setView(textEntryView)
                        .setPositiveButton(getResources().getString(R.string.set_custom_density_set), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                EditText dpi = (EditText) textEntryView.findViewById(R.id.dpi_edit);
                                Editable text = dpi.getText();
                                Log.i(TAG, text.toString());
                                String editText = dpi.getText().toString();

                                try {
                                    SystemProperties.set("persist.sys.lcd_density", editText);
                                }
                                catch (Exception e) {
                                    Log.w(TAG, "Unable to save LCD density");
                                }
                                try {
                                    final IActivityManager am = ActivityManagerNative.asInterface(ServiceManager.checkService("activity"));
                                    if (am != null) {
                                        am.restart();
                                    }
                                }
                                catch (RemoteException e) {
                                    Log.e(TAG, "Failed to restart");
                                }
                            }

                        })
                        .setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                dialog.dismiss();
                            }
                        }).create();
        }
        return null;
    }

}
