package com.android.settings.aicp;

import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;
import com.android.settings.Utils;

public class BreathingNotifications extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, Indexable {

    private static final String SMS_BREATH = "sms_breath";
    private static final String MISSED_CALL_BREATH = "missed_call_breath";
    private static final String VOICEMAIL_BREATH = "voicemail_breath";

    private SwitchPreference mSMSBreath;
    private SwitchPreference mMissedCallBreath;
    private SwitchPreference mVoicemailBreath;

    private Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.breathing_notifications);

        PreferenceScreen prefSet = getPreferenceScreen();
        ContentResolver resolver = getActivity().getContentResolver();
        mContext = getActivity();

        mSMSBreath = (SwitchPreference) findPreference(SMS_BREATH);
        mMissedCallBreath = (SwitchPreference) findPreference(MISSED_CALL_BREATH);
        mVoicemailBreath = (SwitchPreference) findPreference(VOICEMAIL_BREATH);

        Context context = getActivity();

        mSMSBreath.setChecked(Settings.System.getInt(resolver,
                Settings.System.KEY_SMS_BREATH, 0) == 1);
        mSMSBreath.setOnPreferenceChangeListener(this);

        if (Utils.isVoiceCapable(mContext)) {
            mMissedCallBreath.setChecked(Settings.System.getInt(resolver,
                    Settings.System.KEY_MISSED_CALL_BREATH, 0) == 1);
            mMissedCallBreath.setOnPreferenceChangeListener(this);

            mVoicemailBreath.setChecked(Settings.System.getInt(resolver,
                    Settings.System.KEY_VOICEMAIL_BREATH, 0) == 1);
            mVoicemailBreath.setOnPreferenceChangeListener(this);
        } else {
            prefSet.removePreference(mMissedCallBreath);
            prefSet.removePreference(mVoicemailBreath);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        return true;
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mSMSBreath) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(resolver, Settings.System.KEY_SMS_BREATH, value ? 1 : 0);
        } else if (preference == mMissedCallBreath) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(resolver, Settings.System.KEY_MISSED_CALL_BREATH, value ? 1 : 0);
        } else if (preference == mVoicemailBreath) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(resolver, Settings.System.KEY_VOICEMAIL_BREATH, value ? 1 : 0);
        } else {
            return false;
        }

        return true;
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.breathing_notifications;
                    result.add(sir);

                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    ArrayList<String> result = new ArrayList<String>();
                    if (Utils.isVoiceCapable(context)) {
                        result.add(MISSED_CALL_BREATH);
                        result.add(VOICEMAIL_BREATH);
                    }
                    return result;
                }
            };
}
