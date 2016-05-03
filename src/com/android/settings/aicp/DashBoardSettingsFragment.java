/*
 * Copyright (C) Copyright (C) 2016 AICP
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

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.dashboard.DashboardContainerView;
import com.android.settings.widget.SeekBarPreferenceCham;

import com.android.internal.logging.MetricsLogger;

import android.provider.SearchIndexableResource;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import java.util.List;
import java.util.ArrayList;

public class DashBoardSettingsFragment extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String DASHBOARD_COLUMNS = "dashboard_columns";
    private static final String DASHBOARD_SWITCHES = "dashboard_switches";
    private static final String DASHBOARD_FONT_STYLE = "dashboard_font_style";
    private static final String SETTINGS_TITLE_TEXT_SIZE  = "settings_title_text_size";
    private static final String SETTINGS_CATEGORY_TEXT_SIZE  = "settings_category_text_size";

    private ListPreference mDashboardColumns;
    private ListPreference mDashboardSwitches;
    private ListPreference mDashFontStyle;
    private SeekBarPreferenceCham mDashTitleTextSize;
    private SeekBarPreferenceCham mDashCategoryTextSize;

    private static final int MENU_RESET = Menu.FIRST;
    private static final int DLG_RESET = 0;

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

        addPreferencesFromResource(R.xml.dashboard_settings);
        mResolver = getActivity().getContentResolver();

        mDashboardColumns = (ListPreference) findPreference(DASHBOARD_COLUMNS);
        mDashboardColumns.setValue(String.valueOf(Settings.System.getInt(
                getContentResolver(), Settings.System.DASHBOARD_COLUMNS, DashboardContainerView.mDashboardValue)));
        mDashboardColumns.setSummary(mDashboardColumns.getEntry());
        mDashboardColumns.setOnPreferenceChangeListener(this);

        mDashboardSwitches = (ListPreference) findPreference(DASHBOARD_SWITCHES);
        mDashboardSwitches.setValue(String.valueOf(Settings.System.getInt(
                getContentResolver(), Settings.System.DASHBOARD_SWITCHES, 0)));
        mDashboardSwitches.setSummary(mDashboardSwitches.getEntry());
        mDashboardSwitches.setOnPreferenceChangeListener(this);

        mDashTitleTextSize =
                (SeekBarPreferenceCham) findPreference(SETTINGS_TITLE_TEXT_SIZE);
        mDashTitleTextSize.setValue(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.SETTINGS_TITLE_TEXT_SIZE, 15));
        mDashTitleTextSize.setOnPreferenceChangeListener(this);

        mDashCategoryTextSize =
                (SeekBarPreferenceCham) findPreference(SETTINGS_CATEGORY_TEXT_SIZE);
        mDashCategoryTextSize.setValue(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.SETTINGS_CATEGORY_TEXT_SIZE, 16));
        mDashCategoryTextSize.setOnPreferenceChangeListener(this);

        mDashFontStyle = (ListPreference) findPreference(DASHBOARD_FONT_STYLE);
        mDashFontStyle.setOnPreferenceChangeListener(this);
        mDashFontStyle.setValue(Integer.toString(Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.DASHBOARD_FONT_STYLE, 0)));
        mDashFontStyle.setSummary(mDashFontStyle.getEntry());

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.reset)
                .setIcon(R.drawable.ic_settings_reset)
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
        int intHex;
        String hex;

        if (preference == mDashboardSwitches) {
            Settings.System.putInt(getContentResolver(), Settings.System.DASHBOARD_SWITCHES,
                    Integer.valueOf((String) newValue));
            mDashboardSwitches.setValue(String.valueOf(newValue));
            mDashboardSwitches.setSummary(mDashboardSwitches.getEntry());
            return true;
        } else if (preference == mDashboardColumns) {
            Settings.System.putInt(getContentResolver(), Settings.System.DASHBOARD_COLUMNS,
                    Integer.valueOf((String) newValue));
            mDashboardColumns.setValue(String.valueOf(newValue));
            mDashboardColumns.setSummary(mDashboardColumns.getEntry());
            return true;
        } else if (preference == mDashFontStyle) {
            int val = Integer.parseInt((String) newValue);
            int index = mDashFontStyle.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.DASHBOARD_FONT_STYLE, val);
            mDashFontStyle.setSummary(mDashFontStyle.getEntries()[index]);
            return true;
        } else if (preference == mDashTitleTextSize) {
            int width = ((Integer)newValue).intValue();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.SETTINGS_TITLE_TEXT_SIZE, width);
            return true;
        } else if (preference == mDashCategoryTextSize) {
            int width = ((Integer)newValue).intValue();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.SETTINGS_CATEGORY_TEXT_SIZE, width);
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

        DashBoardSettingsFragment getOwner() {
            return (DashBoardSettingsFragment) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            switch (id) {
                case DLG_RESET:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.reset)
                    .setMessage(R.string.reset_message)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.reset,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.DASHBOARD_SWITCHES,
                                    0);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.DASHBOARD_COLUMNS,
                                    1);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.DASHBOARD_FONT_STYLE,
                                    0);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.SETTINGS_TITLE_TEXT_SIZE,
                                    15);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.SETTINGS_CATEGORY_TEXT_SIZE,
                                    16);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.DASHBOARD_CUSTOMIZATIONS,
                                    0);
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

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.APPLICATION;
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                                                                         boolean enabled) {
                 ArrayList<SearchIndexableResource> result =
                         new ArrayList<SearchIndexableResource>();

                 SearchIndexableResource sir = new SearchIndexableResource(context);
                 sir.xmlResId = R.xml.dashboard_settings;
                 result.add(sir);

                return result;
            }

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                final List<String> keys = new ArrayList<String>();
                return keys;
            }
        };
}
