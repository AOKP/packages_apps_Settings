/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.notification;

import android.app.AlertDialog;
import android.app.AutomaticZenRule;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.provider.Settings;
import android.service.notification.ZenModeConfig;
import android.service.notification.ZenModeConfig.ScheduleInfo;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.support.v7.preference.PreferenceScreen;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.TimePicker;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;

public class ZenModeScheduleRuleSettings extends ZenModeRuleSettingsBase {
    private static final String KEY_DAYS = "days";
    private static final String KEY_START_TIME = "start_time";
    private static final String KEY_END_TIME = "end_time";
    private static final String KEY_EXIT_AT_ALARM = "exit_at_alarm";

    public static final String ACTION = Settings.ACTION_ZEN_MODE_SCHEDULE_RULE_SETTINGS;

    // per-instance to ensure we're always using the current locale
    private final SimpleDateFormat mDayFormat = new SimpleDateFormat("EEE");

    private Preference mDays;
    private TimePickerPreference mStart;
    private TimePickerPreference mEnd;
    private SwitchPreference mExitAtAlarm;

    private ScheduleInfo mSchedule;

    @Override
    protected boolean setRule(AutomaticZenRule rule) {
        mSchedule = rule != null ? ZenModeConfig.tryParseScheduleConditionId(rule.getConditionId())
                : null;
        return mSchedule != null;
    }

    @Override
    protected String getZenModeDependency() {
        return mDays.getKey();
    }

    @Override
    protected int getEnabledToastText() {
        return R.string.zen_schedule_rule_enabled_toast;
    }

    @Override
    protected void onCreateInternal() {
        addPreferencesFromResource(R.xml.zen_mode_schedule_rule_settings);
        final PreferenceScreen root = getPreferenceScreen();

        mDays = root.findPreference(KEY_DAYS);
        mDays.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showDaysDialog();
                return true;
            }
        });

        final FragmentManager mgr = getFragmentManager();

        mStart = new TimePickerPreference(getPrefContext(), mgr);
        mStart.setKey(KEY_START_TIME);
        mStart.setTitle(R.string.zen_mode_start_time);
        mStart.setCallback(new TimePickerPreference.Callback() {
            @Override
            public boolean onSetTime(final int hour, final int minute) {
                if (mDisableListeners) return true;
                if (!ZenModeConfig.isValidHour(hour)) return false;
                if (!ZenModeConfig.isValidMinute(minute)) return false;
                if (hour == mSchedule.startHour && minute == mSchedule.startMinute) {
                    return true;
                }
                if (DEBUG) Log.d(TAG, "onPrefChange start h=" + hour + " m=" + minute);
                mSchedule.startHour = hour;
                mSchedule.startMinute = minute;
                updateRule(ZenModeConfig.toScheduleConditionId(mSchedule));
                return true;
            }
        });
        root.addPreference(mStart);
        mStart.setDependency(mDays.getKey());

        mEnd = new TimePickerPreference(getPrefContext(), mgr);
        mEnd.setKey(KEY_END_TIME);
        mEnd.setTitle(R.string.zen_mode_end_time);
        mEnd.setCallback(new TimePickerPreference.Callback() {
            @Override
            public boolean onSetTime(final int hour, final int minute) {
                if (mDisableListeners) return true;
                if (!ZenModeConfig.isValidHour(hour)) return false;
                if (!ZenModeConfig.isValidMinute(minute)) return false;
                if (hour == mSchedule.endHour && minute == mSchedule.endMinute) {
                    return true;
                }
                if (DEBUG) Log.d(TAG, "onPrefChange end h=" + hour + " m=" + minute);
                mSchedule.endHour = hour;
                mSchedule.endMinute = minute;
                updateRule(ZenModeConfig.toScheduleConditionId(mSchedule));
                return true;
            }
        });
        root.addPreference(mEnd);
        mEnd.setDependency(mDays.getKey());

        mExitAtAlarm = (SwitchPreference) root.findPreference(KEY_EXIT_AT_ALARM);
        mExitAtAlarm.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                mSchedule.exitAtAlarm = (Boolean) o;
                updateRule(ZenModeConfig.toScheduleConditionId(mSchedule));
                return true;
            }
        });
    }

    private void updateDays() {
        // Compute an ordered, delimited list of day names based on the persisted user config.
        final int[] days = mSchedule.days;
        if (days != null && days.length > 0) {
            final StringBuilder sb = new StringBuilder();
            final Calendar c = Calendar.getInstance();
            int[] daysOfWeek = ZenModeScheduleDaysSelection.getDaysOfWeekForLocale(c);
            for (int i = 0; i < daysOfWeek.length; i++) {
                final int day = daysOfWeek[i];
                for (int j = 0; j < days.length; j++) {
                    if (day == days[j]) {
                        c.set(Calendar.DAY_OF_WEEK, day);
                        if (sb.length() > 0) {
                            sb.append(mContext.getString(R.string.summary_divider_text));
                        }
                        sb.append(mDayFormat.format(c.getTime()));
                        break;
                    }
                }
            }
            if (sb.length() > 0) {
                mDays.setSummary(sb);
                mDays.notifyDependencyChange(false);
                return;
            }
        }
        mDays.setSummary(R.string.zen_mode_schedule_rule_days_none);
        mDays.notifyDependencyChange(true);
    }

    private void updateEndSummary() {
        final int startMin = 60 * mSchedule.startHour + mSchedule.startMinute;
        final int endMin = 60 * mSchedule.endHour + mSchedule.endMinute;
        final boolean nextDay = startMin >= endMin;
        final int summaryFormat = nextDay ? R.string.zen_mode_end_time_next_day_summary_format : 0;
        mEnd.setSummaryFormat(summaryFormat);
    }

    @Override
    protected void updateControlsInternal() {
        updateDays();
        mStart.setTime(mSchedule.startHour, mSchedule.startMinute);
        mEnd.setTime(mSchedule.endHour, mSchedule.endMinute);
        mExitAtAlarm.setChecked(mSchedule.exitAtAlarm);
        updateEndSummary();
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.NOTIFICATION_ZEN_MODE_SCHEDULE_RULE;
    }

    private void showDaysDialog() {
        new AlertDialog.Builder(mContext)
                .setTitle(R.string.zen_mode_schedule_rule_days)
                .setView(new ZenModeScheduleDaysSelection(mContext, mSchedule.days) {
                      @Override
                      protected void onChanged(final int[] days) {
                          if (mDisableListeners) return;
                          if (Arrays.equals(days, mSchedule.days)) return;
                          if (DEBUG) Log.d(TAG, "days.onChanged days=" + Arrays.asList(days));
                          mSchedule.days = days;
                          updateRule(ZenModeConfig.toScheduleConditionId(mSchedule));
                      }
                })
                .setOnDismissListener(new OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        updateDays();
                    }
                })
                .setPositiveButton(R.string.done_button, null)
                .show();
    }

    private static class TimePickerPreference extends Preference {
        private final Context mContext;

        private int mSummaryFormat;
        private int mHourOfDay;
        private int mMinute;
        private Callback mCallback;

        public TimePickerPreference(Context context, final FragmentManager mgr) {
            super(context);
            mContext = context;
            setPersistent(false);
            setOnPreferenceClickListener(new OnPreferenceClickListener(){
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    final TimePickerFragment frag = new TimePickerFragment();
                    frag.pref = TimePickerPreference.this;
                    frag.show(mgr, TimePickerPreference.class.getName());
                    return true;
                }
            });
        }

        public void setCallback(Callback callback) {
            mCallback = callback;
        }

        public void setSummaryFormat(int resId) {
            mSummaryFormat = resId;
            updateSummary();
        }

        public void setTime(int hourOfDay, int minute) {
            if (mCallback != null && !mCallback.onSetTime(hourOfDay, minute)) return;
            mHourOfDay = hourOfDay;
            mMinute = minute;
            updateSummary();
        }

        private void updateSummary() {
            final Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY, mHourOfDay);
            c.set(Calendar.MINUTE, mMinute);
            String time = DateFormat.getTimeFormat(mContext).format(c.getTime());
            if (mSummaryFormat != 0) {
                time = mContext.getResources().getString(mSummaryFormat, time);
            }
            setSummary(time);
        }

        public static class TimePickerFragment extends DialogFragment implements
                TimePickerDialog.OnTimeSetListener {
            public TimePickerPreference pref;

            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                final boolean usePref = pref != null && pref.mHourOfDay >= 0 && pref.mMinute >= 0;
                final Calendar c = Calendar.getInstance();
                final int hour = usePref ? pref.mHourOfDay : c.get(Calendar.HOUR_OF_DAY);
                final int minute = usePref ? pref.mMinute : c.get(Calendar.MINUTE);
                return new TimePickerDialog(getActivity(), this, hour, minute,
                        DateFormat.is24HourFormat(getActivity()));
            }

            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                if (pref != null) {
                    pref.setTime(hourOfDay, minute);
                }
            }
        }

        public interface Callback {
            boolean onSetTime(int hour, int minute);
        }
    }

}
