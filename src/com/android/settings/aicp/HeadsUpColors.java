/*
* Copyright (C) 2014 The CyanogenMod Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.android.settings.aicp;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.SwitchPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class HeadsUpColors extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String HEADS_UP_BG_COLOR = "heads_up_bg_color";
    private static final String HEADS_UP_TEXT_COLOR = "heads_up_text_color";

    private static final int DEFAULT_BG_COLOR = 0xffffffff;
    private static final int DEFAULT_TEXT_COLOR = 0xff000000;

    private static final int MENU_RESET = Menu.FIRST;
    private static final int DLG_RESET = 0;

    private ColorPickerPreference mHeadsUpBgColor;
    private ColorPickerPreference mHeadsUpTextColor;

    private ContentResolver mResolver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        refreshSettings();
    }

    public void refreshSettings() {
        PreferenceScreen prefs = getPreferenceScreen();
        if (prefs != null) {
            prefs.removeAll();
        }

        addPreferencesFromResource(R.xml.heads_up_colors);
        mResolver = getActivity().getContentResolver();

        int intColor = 0xffffffff;
        String hexColor = String.format("#%08x", (0xffffffff & 0xffffffff));

        mHeadsUpBgColor =
                (ColorPickerPreference) findPreference(HEADS_UP_BG_COLOR);
        intColor = Settings.System.getInt(mResolver,
                Settings.System.HEADS_UP_BG_COLOR,
                DEFAULT_BG_COLOR);
        mHeadsUpBgColor.setNewPreviewColor(intColor);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mHeadsUpBgColor.setSummary(hexColor);
        mHeadsUpBgColor.setOnPreferenceChangeListener(this);
        mHeadsUpBgColor.setAlphaSliderEnabled(true);

        mHeadsUpTextColor = (ColorPickerPreference) findPreference(HEADS_UP_TEXT_COLOR);
        intColor = Settings.System.getInt(mResolver,
                Settings.System.HEADS_UP_TEXT_COLOR,
                DEFAULT_TEXT_COLOR);
        mHeadsUpTextColor.setNewPreviewColor(intColor);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mHeadsUpTextColor.setSummary(hexColor);
        mHeadsUpTextColor.setOnPreferenceChangeListener(this);
        mHeadsUpTextColor.setAlphaSliderEnabled(true);

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.reset)
                .setIcon(R.drawable.ic_settings_backup_restore)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                showDialogInner(DLG_RESET);
                return true;
             default:
                return super.onContextItemSelected(item);
        }
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean value;
        String hex;
        int intHex;

        if (preference == mHeadsUpBgColor) {
            hex = ColorPickerPreference.convertToARGB(
                Integer.valueOf(String.valueOf(newValue)));
            intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(mResolver,
                Settings.System.HEADS_UP_BG_COLOR, intHex);
            preference.setSummary(hex);
            return true;
        } else if (preference == mHeadsUpTextColor) {
            hex = ColorPickerPreference.convertToARGB(
                Integer.valueOf(String.valueOf(newValue)));
            intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(mResolver,
                Settings.System.HEADS_UP_TEXT_COLOR, intHex);
            preference.setSummary(hex);
            return true;
        }
        return false;
    }

    private void showDialogInner(int id) {
        DialogFragment newFragment = MyAlertDialogFragment.newInstance(id);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }

    public static class MyAlertDialogFragment extends DialogFragment {

        public static MyAlertDialogFragment newInstance(int id) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            frag.setArguments(args);
            return frag;
        }

        HeadsUpColors getOwner() {
            return (HeadsUpColors) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            switch (id) {
                case DLG_RESET:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.reset)
                    .setMessage(R.string.dlg_reset_values_message)
                    .setNegativeButton(R.string.cancel, null)
                    .setNeutralButton(R.string.dlg_reset_android,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                             Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.HEADS_UP_BG_COLOR,
                                    DEFAULT_BG_COLOR);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.HEADS_UP_TEXT_COLOR,
                                    DEFAULT_TEXT_COLOR);
                            getOwner().refreshSettings();
                        }
                    })
                    .setPositiveButton(R.string.dlg_reset_aicp,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.HEADS_UP_BG_COLOR,
                                    0xff000000);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.HEADS_UP_TEXT_COLOR,
                                    0xffffffff);
                            getOwner().refreshSettings();
                        }
                    })
                    .create();
            }
            throw new IllegalArgumentException("unknown id " + id);
        }

        @Override
        public void onCancel(DialogInterface dialog) {

        }
    }
}
