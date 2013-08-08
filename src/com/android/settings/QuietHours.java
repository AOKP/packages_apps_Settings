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

package com.android.settings;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.InputFilter;
import android.text.TextUtils;
import android.widget.EditText;

import com.android.settings.R;
import com.android.settings.service.MessagingHelper;
import com.android.settings.SettingsPreferenceFragment;

public class QuietHours extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener  {

    private static final String TAG = "QuietHours";

    private static final String KEY_QUIET_HOURS_ENABLED = "quiet_hours_enabled";

    private static final String KEY_QUIET_HOURS_NOTIFICATIONS = "quiet_hours_notifications";
    
    private static final String KEY_QUIET_HOURS_RINGER = "quiet_hours_ringer";

    private static final String KEY_QUIET_HOURS_STILL = "quiet_hours_still";

    private static final String KEY_QUIET_HOURS_DIM = "quiet_hours_dim";

    private static final String KEY_QUIET_HOURS_TIMERANGE = "quiet_hours_timerange";

    private static final String KEY_AUTO_SMS = "auto_sms";

    private static final String KEY_AUTO_SMS_CALL = "auto_sms_call";

    private static final String KEY_AUTO_SMS_MESSAGE = "auto_sms_message";

    private static final String KEY_CALL_BYPASS = "call_bypass";

    private static final String KEY_SMS_BYPASS = "sms_bypass";

    private static final String KEY_REQUIRED_CALLS = "required_calls";

    private static final String KEY_SMS_BYPASS_CODE = "sms_bypass_code";

    private CheckBoxPreference mQuietHoursEnabled;

    private CheckBoxPreference mQuietHoursNotifications;
    
    private CheckBoxPreference mQuietHoursRinger;

    private CheckBoxPreference mQuietHoursStill;

    private CheckBoxPreference mQuietHoursDim;

    private ListPreference mAutoSms;

    private ListPreference mAutoSmsCall;

    private ListPreference mSmsBypass;

    private ListPreference mCallBypass;

    private ListPreference mCallBypassNumber;

    private Preference mSmsBypassCode;

    private Preference mAutoSmsMessage;


    private TimeRangePreference mQuietHoursTimeRange;

    protected Handler mHandler;
    private SettingsObserver mSettingsObserver;
    private Context mContext;

    private int mSmsPref;
    private int mCallPref;

    private SharedPreferences mPrefs;
    private OnSharedPreferenceChangeListener mPreferencesChangeListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getPreferenceManager() != null) {
            addPreferencesFromResource(R.xml.quiet_hours_settings);

            mContext = getActivity().getApplicationContext();
            ContentResolver resolver = mContext.getContentResolver();
            PreferenceScreen prefSet = getPreferenceScreen();
            mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);

            // Load the preferences
            mQuietHoursEnabled = (CheckBoxPreference) prefSet.findPreference(KEY_QUIET_HOURS_ENABLED);
            mQuietHoursTimeRange = (TimeRangePreference) prefSet.findPreference(KEY_QUIET_HOURS_TIMERANGE);
            mQuietHoursNotifications = (CheckBoxPreference) prefSet.findPreference(KEY_QUIET_HOURS_NOTIFICATIONS);
            mQuietHoursRinger = (CheckBoxPreference) prefSet.findPreference(KEY_QUIET_HOURS_RINGER);
            mQuietHoursStill = (CheckBoxPreference) prefSet.findPreference(KEY_QUIET_HOURS_STILL);
            mQuietHoursDim = (CheckBoxPreference) findPreference(KEY_QUIET_HOURS_DIM);
            mAutoSms = (ListPreference) findPreference(KEY_AUTO_SMS);
            mAutoSmsCall = (ListPreference) findPreference(KEY_AUTO_SMS_CALL);
            mAutoSmsMessage = (Preference) findPreference(KEY_AUTO_SMS_MESSAGE);
            mSmsBypass = (ListPreference) findPreference(KEY_SMS_BYPASS);
            mCallBypass = (ListPreference) findPreference(KEY_CALL_BYPASS);
            mCallBypassNumber = (ListPreference) findPreference(KEY_REQUIRED_CALLS);
            mSmsBypassCode = (Preference) findPreference(KEY_SMS_BYPASS_CODE);

            mSettingsObserver = new SettingsObserver(new Handler());

            // Set the preference state and listeners where applicable
            mQuietHoursTimeRange.setTimeRange(Settings.System.getInt(resolver, Settings.System.QUIET_HOURS_START, 0),
                    Settings.System.getInt(resolver, Settings.System.QUIET_HOURS_END, 0));
            mQuietHoursTimeRange.setOnPreferenceChangeListener(this);
            mQuietHoursNotifications.setChecked(Settings.System.getInt(resolver, Settings.System.QUIET_HOURS_NOTIFICATIONS, 0) == 1);
            mQuietHoursRinger.setChecked(Settings.System.getInt(resolver, Settings.System.QUIET_HOURS_RINGER, 0) == 1);
            mQuietHoursStill.setChecked(Settings.System.getInt(resolver, Settings.System.QUIET_HOURS_STILL, 0) == 1);
            mAutoSms.setValue(mPrefs.getString(KEY_AUTO_SMS, "0"));
            mAutoSms.setOnPreferenceChangeListener(this);
            mAutoSmsCall.setValue(mPrefs.getString(KEY_AUTO_SMS_CALL, "0"));
            mAutoSmsCall.setOnPreferenceChangeListener(this);
            mSmsPref = Integer.parseInt(mPrefs.getString(KEY_AUTO_SMS, "0"));
            mCallPref = Integer.parseInt(mPrefs.getString(KEY_AUTO_SMS_CALL, "0"));
            mSmsBypass.setValue(mPrefs.getString(KEY_SMS_BYPASS, "0"));
            mSmsBypass.setOnPreferenceChangeListener(this);
            int smsBypass = Integer.parseInt(mPrefs.getString(KEY_SMS_BYPASS, "0"));
            mSmsBypass.setSummary(mSmsBypass.getEntries()[smsBypass]);
            mCallBypass.setValue(mPrefs.getString(KEY_CALL_BYPASS, "0"));
            mCallBypass.setOnPreferenceChangeListener(this);
            int callBypass = Integer.parseInt(mPrefs.getString(KEY_CALL_BYPASS, "0"));
            mCallBypass.setSummary(mCallBypass.getEntries()[callBypass]);
            mCallBypassNumber.setValue(mPrefs.getString(KEY_REQUIRED_CALLS, "2"));
            mCallBypassNumber.setOnPreferenceChangeListener(this);
            int callBypassNumber = Integer.parseInt(mPrefs.getString(KEY_REQUIRED_CALLS, "2"));
            mCallBypassNumber.setSummary(mCallBypassNumber.getEntries()[callBypassNumber-2]
                    + getResources().getString(R.string.quiet_hours_calls_required_summary));
            mAutoSms.setSummary(mAutoSms.getEntries()[mSmsPref]);
            mAutoSmsCall.setSummary(mAutoSmsCall.getEntries()[mCallPref]);

            mCallBypassNumber.setEnabled(callBypass != 0);
            mSmsBypassCode.setEnabled(smsBypass != 0);
            shouldDisplayTextPref();
            setSmsBypassCodeSummary();

            TelephonyManager telephonyManager =
                    (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_NONE) {
                prefSet.removePreference((PreferenceGroup) findPreference("sms_respond"));
                prefSet.removePreference((PreferenceGroup) findPreference("quiethours_bypass"));
            }
            // Remove the notification light setting if the device does not support it 
            if (mQuietHoursDim != null && getResources().getBoolean(com.android.internal.R.bool.config_intrusiveNotificationLed) == false) {
                getPreferenceScreen().removePreference(mQuietHoursDim);
            } else {
                mQuietHoursDim.setChecked(Settings.System.getInt(resolver, Settings.System.QUIET_HOURS_DIM, 0) == 1);
            }

            mPreferencesChangeListener = new OnSharedPreferenceChangeListener() {
                public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                    if (key.equals(KEY_AUTO_SMS_CALL)
                            || key.equals(KEY_AUTO_SMS)
                            || key.equals(KEY_CALL_BYPASS)
                            || key.equals(KEY_SMS_BYPASS)) {
                        MessagingHelper.scheduleService(mContext);
                    }
                    if (key.equals(KEY_SMS_BYPASS_CODE)) {
                        setSmsBypassCodeSummary();
                    }
                }
            };
            mPrefs.registerOnSharedPreferenceChangeListener(mPreferencesChangeListener);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mPrefs.unregisterOnSharedPreferenceChangeListener(mPreferencesChangeListener);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        ContentResolver resolver = getActivity().getApplicationContext().getContentResolver();

        if (preference == mQuietHoursEnabled) {
            Settings.System.putInt(resolver, Settings.System.QUIET_HOURS_ENABLED,
                    mQuietHoursEnabled.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mQuietHoursNotifications) {
            Settings.System.putInt(resolver, Settings.System.QUIET_HOURS_NOTIFICATIONS,
                    mQuietHoursNotifications.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mQuietHoursRinger) {
            Settings.System.putInt(resolver, Settings.System.QUIET_HOURS_RINGER,
                    mQuietHoursRinger.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mQuietHoursStill) {
            Settings.System.putInt(resolver, Settings.System.QUIET_HOURS_STILL,
                    mQuietHoursStill.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mQuietHoursDim) {
            Settings.System.putInt(resolver, Settings.System.QUIET_HOURS_DIM,
                    mQuietHoursDim.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mAutoSmsMessage) {
            final String defaultText = getResources().getString(R.string.quiet_hours_auto_sms_null);
            final String autoText = mPrefs.getString(KEY_AUTO_SMS_MESSAGE, defaultText);

            AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
            alert.setTitle(R.string.quiet_hours_auto_string_title);
            alert.setMessage(R.string.quiet_hours_auto_string_explain);

            final EditText input = new EditText(getActivity());
            InputFilter[] filter = new InputFilter[1];
            // No multi/split messages for ease of compatibility
            filter[0] = new InputFilter.LengthFilter(160);
            input.append(autoText);
            input.setFilters(filter);
            alert.setView(input);
            alert.setPositiveButton(getResources().getString(R.string.ok),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            String value = input.getText().toString();
                            if (TextUtils.isEmpty(value)) {
                                value = defaultText;
                            }
                            SharedPreferences.Editor editor = mPrefs.edit();
                            editor.putString(KEY_AUTO_SMS_MESSAGE, value).apply();
                        }
                    });
            alert.show();
            return true;
        } else if (preference == mSmsBypassCode) {
            final String defaultCode = getResources().getString(R.string.quiet_hours_sms_code_null);
            final String code = mPrefs.getString(KEY_SMS_BYPASS_CODE, defaultCode);
            AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
            alert.setTitle(R.string.quiet_hours_sms_code_title);
            alert.setMessage(R.string.quiet_hours_sms_code_explain);

            final EditText input = new EditText(getActivity());
            InputFilter[] filter = new InputFilter[1];
            filter[0] = new InputFilter.LengthFilter(20);
            input.append(code);
            input.setFilters(filter);
            alert.setView(input);
            alert.setPositiveButton(getResources().getString(R.string.ok),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            String value = input.getText().toString();
                            if (TextUtils.isEmpty(value)) {
                                value = defaultCode;
                            }
                            SharedPreferences.Editor editor = mPrefs.edit();
                            editor.putString(KEY_SMS_BYPASS_CODE, value).apply();
                        }
                    });
            alert.show();
            setSmsBypassCodeSummary();
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getApplicationContext().getContentResolver();
        if (preference == mQuietHoursTimeRange) {
            Settings.System.putInt(resolver, Settings.System.QUIET_HOURS_START,
                    mQuietHoursTimeRange.getStartTime());
            Settings.System.putInt(resolver, Settings.System.QUIET_HOURS_END,
                    mQuietHoursTimeRange.getEndTime());
            MessagingHelper.scheduleService(mContext);
            return true;
        } else if (preference == mAutoSms) {
            mSmsPref = Integer.parseInt((String) newValue);
            mAutoSms.setSummary(mAutoSms.getEntries()[mSmsPref]);
            shouldDisplayTextPref();
            return true;
        } else if (preference == mAutoSmsCall) {
            mCallPref = Integer.parseInt((String) newValue);
            mAutoSmsCall.setSummary(mAutoSmsCall.getEntries()[mCallPref]);
            shouldDisplayTextPref();
            return true;
        } else if (preference == mSmsBypass) {
            int val = Integer.parseInt((String) newValue);
            mSmsBypass.setSummary(mSmsBypass.getEntries()[val]);
            mSmsBypassCode.setEnabled(val != 0);
            return true;
        } else if (preference == mCallBypass) {
            int val = Integer.parseInt((String) newValue);
            mCallBypass.setSummary(mCallBypass.getEntries()[val]);
            mCallBypassNumber.setEnabled(val != 0);
            return true;
        } else if (preference == mCallBypassNumber) {
            int val = Integer.parseInt((String) newValue);
            mCallBypassNumber.setSummary(mCallBypassNumber.getEntries()[val-2]
                    + getResources().getString(R.string.quiet_hours_calls_required_summary));
            return true;
        }
        return false;
    }

    private void shouldDisplayTextPref() {
        mAutoSmsMessage.setEnabled(mSmsPref != 0 || mCallPref != 0);
    }

    private void setSmsBypassCodeSummary() {
        final String defaultCode = getResources().getString(R.string.quiet_hours_sms_code_null);
        final String code = mPrefs.getString(KEY_SMS_BYPASS_CODE, defaultCode);
        mSmsBypassCode.setSummary(code);
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
            observe();
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.QUIET_HOURS_ENABLED), false,
                    this);
            updateSettings();
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    }

    protected void updateSettings() {
        ContentResolver resolver = mContext.getContentResolver();
        mQuietHoursEnabled.setChecked(Settings.System.getInt(resolver,
                Settings.System.QUIET_HOURS_ENABLED, 0) == 1);
        MessagingHelper.scheduleService(mContext);
    }
}
