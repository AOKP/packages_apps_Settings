/*
 * Copyright (C) 2014 Slimroms
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

import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;

import com.android.internal.widget.LockPatternUtils;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;

import java.util.Locale;

public class QsSettings extends SettingsPreferenceFragment
            implements OnPreferenceChangeListener  {

    public static final String TAG = "QsSettings";

    private static final String PREF_QUICK_PULLDOWN = "quick_pulldown";
    private static final String PREF_SMART_PULLDOWN = "smart_pulldown";
    private static final String PREF_BLOCK_ON_SECURE_KEYGUARD = "block_on_secure_keyguard";

    ListPreference mQuickPulldown;
    ListPreference mSmartPulldown;
    SwitchPreference mBlockOnSecureKeyguard;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.qs_settings);

        PreferenceScreen prefs = getPreferenceScreen();

        mQuickPulldown = (ListPreference) findPreference(PREF_QUICK_PULLDOWN);
        mSmartPulldown = (ListPreference) findPreference(PREF_SMART_PULLDOWN);

        // Quick Pulldown
        mQuickPulldown.setOnPreferenceChangeListener(this);
        int statusQuickPulldown = Settings.System.getInt(getContentResolver(),
                Settings.System.STATUS_BAR_QUICK_QS_PULLDOWN, 1);
        mQuickPulldown.setValue(String.valueOf(statusQuickPulldown));
        updateQuickPulldownSummary(statusQuickPulldown);

        // Smart Pulldown
        mSmartPulldown.setOnPreferenceChangeListener(this);
        int smartPulldown = Settings.System.getInt(getContentResolver(),
                Settings.System.QS_SMART_PULLDOWN, 0);
        mSmartPulldown.setValue(String.valueOf(smartPulldown));
        updateSmartPulldownSummary(smartPulldown);

        final LockPatternUtils lockPatternUtils = new LockPatternUtils(getActivity());
        mBlockOnSecureKeyguard = (SwitchPreference) findPreference(PREF_BLOCK_ON_SECURE_KEYGUARD);
        if (lockPatternUtils.isSecure()) {
            mBlockOnSecureKeyguard.setChecked(Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.STATUS_BAR_LOCKED_ON_SECURE_KEYGUARD, 1) == 1);
            mBlockOnSecureKeyguard.setOnPreferenceChangeListener(this);
        } else {
            prefs.removePreference(mBlockOnSecureKeyguard);
        }

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mQuickPulldown) {
            int statusQuickPulldown = Integer.valueOf((String) newValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.STATUS_BAR_QUICK_QS_PULLDOWN,
                    statusQuickPulldown);
            updateQuickPulldownSummary(statusQuickPulldown);
            return true;
        } else if (preference == mSmartPulldown) {
            int smartPulldown = Integer.valueOf((String) newValue);
            Settings.System.putInt(getContentResolver(), Settings.System.QS_SMART_PULLDOWN,
                    smartPulldown);
            updateSmartPulldownSummary(smartPulldown);
            return true;
        } else if (preference == mBlockOnSecureKeyguard) {
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.STATUS_BAR_LOCKED_ON_SECURE_KEYGUARD,
                    (Boolean) newValue ? 1 : 0);
            return true;
        }
        return false;
    }

    private void updateSmartPulldownSummary(int value) {
        Resources res = getResources();

        if (value == 0) {
            // Smart pulldown deactivated
            mSmartPulldown.setSummary(res.getString(R.string.smart_pulldown_off));
        } else {
            String type = null;
            switch (value) {
                case 1:
                    type = res.getString(R.string.smart_pulldown_dismissable);
                    break;
                case 2:
                    type = res.getString(R.string.smart_pulldown_persistent);
                    break;
                default:
                    type = res.getString(R.string.smart_pulldown_all);
                    break;
            }
            // Remove title capitalized formatting
            type = type.toLowerCase();
            mSmartPulldown.setSummary(res.getString(R.string.smart_pulldown_summary, type));
        }
    }

    private void updateQuickPulldownSummary(int value) {
        Resources res = getResources();

        if (value == 0) {
            // quick pulldown deactivated
            mQuickPulldown.setSummary(res.getString(R.string.quick_pulldown_off));
        } else {
            Locale l = Locale.getDefault();
            boolean isRtl = TextUtils.getLayoutDirectionFromLocale(l) == View.LAYOUT_DIRECTION_RTL;
            String direction = res.getString(value == 2
                    ? (isRtl ? R.string.quick_pulldown_right : R.string.quick_pulldown_left)
                    : (isRtl ? R.string.quick_pulldown_left : R.string.quick_pulldown_right));
            mQuickPulldown.setSummary(res.getString(R.string.summary_quick_pulldown, direction));
        }
    }

}

