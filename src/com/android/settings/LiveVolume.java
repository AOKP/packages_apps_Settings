/*
 * Copyright (C) 2007 The Android Open Source Project
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
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.os.Vibrator;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.VolumePanel;

public class LiveVolume extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "LiveVolume";

    private static final String KEY_VOLUME_STEPS_ALARM = "volume_steps_alarm";
    private static final String KEY_VOLUME_STEPS_DTMF = "volume_steps_dtmf";
    private static final String KEY_VOLUME_STEPS_MUSIC = "volume_steps_music";
    private static final String KEY_VOLUME_STEPS_NOTIFICATION = "volume_steps_notification";
    private static final String KEY_VOLUME_STEPS_RING = "volume_steps_ring";
    private static final String KEY_VOLUME_STEPS_SYSTEM = "volume_steps_system";
    private static final String KEY_VOLUME_STEPS_VOICE_CALL = "volume_steps_voice_call";

    private ListPreference mVolumeStepsAlarm;
    private ListPreference mVolumeStepsDTMF;
    private ListPreference mVolumeStepsMusic;
    private ListPreference mVolumeStepsNotification;
    private ListPreference mVolumeStepsRing;
    private ListPreference mVolumeStepsSystem;
    private ListPreference mVolumeStepsVoiceCall;

    private AudioManager mAudioManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContentResolver resolver = getContentResolver();
        int activePhoneType = TelephonyManager.getDefault().getCurrentPhoneType();

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        addPreferencesFromResource(R.xml.live_volume_settings);


        boolean isPhone = activePhoneType != TelephonyManager.PHONE_TYPE_NONE;
        PreferenceCategory audioCat = (PreferenceCategory) getPreferenceScreen().findPreference("category_volume");

        mVolumeStepsAlarm = (ListPreference) findPreference(KEY_VOLUME_STEPS_ALARM);
        updateVolumeSteps(mVolumeStepsAlarm.getKey(),mAudioManager.getStreamMaxVolume(mAudioManager.STREAM_ALARM));
        mVolumeStepsAlarm.setOnPreferenceChangeListener(this);

        mVolumeStepsDTMF = (ListPreference) findPreference(KEY_VOLUME_STEPS_DTMF);
        if (isPhone) {
            updateVolumeSteps(mVolumeStepsDTMF.getKey(),mAudioManager.getStreamMaxVolume(mAudioManager.STREAM_DTMF));
            mVolumeStepsDTMF .setOnPreferenceChangeListener(this);
        } else {
            audioCat.removePreference(mVolumeStepsDTMF);
        }

        mVolumeStepsMusic = (ListPreference) findPreference(KEY_VOLUME_STEPS_MUSIC);
        updateVolumeSteps(mVolumeStepsMusic.getKey(),mAudioManager.getStreamMaxVolume(mAudioManager.STREAM_MUSIC));
        mVolumeStepsMusic .setOnPreferenceChangeListener(this);

        mVolumeStepsNotification = (ListPreference) findPreference(KEY_VOLUME_STEPS_NOTIFICATION);
        updateVolumeSteps(mVolumeStepsNotification.getKey(),mAudioManager.getStreamMaxVolume(mAudioManager.STREAM_NOTIFICATION));
        mVolumeStepsNotification .setOnPreferenceChangeListener(this);

        mVolumeStepsRing = (ListPreference) findPreference(KEY_VOLUME_STEPS_RING);
        if (isPhone) {
            updateVolumeSteps(mVolumeStepsRing.getKey(),mAudioManager.getStreamMaxVolume(mAudioManager.STREAM_RING));
            mVolumeStepsRing .setOnPreferenceChangeListener(this);
        } else {
            audioCat.removePreference(mVolumeStepsRing);
        }

        mVolumeStepsSystem = (ListPreference) findPreference(KEY_VOLUME_STEPS_SYSTEM);
        updateVolumeSteps(mVolumeStepsSystem.getKey(),mAudioManager.getStreamMaxVolume(mAudioManager.STREAM_SYSTEM));
        mVolumeStepsSystem .setOnPreferenceChangeListener(this);

        mVolumeStepsVoiceCall = (ListPreference) findPreference(KEY_VOLUME_STEPS_VOICE_CALL);
        if (isPhone) {
            updateVolumeSteps(mVolumeStepsVoiceCall.getKey(),mAudioManager.getStreamMaxVolume(mAudioManager.STREAM_VOICE_CALL));
            mVolumeStepsVoiceCall .setOnPreferenceChangeListener(this);
        } else {
            audioCat.removePreference(mVolumeStepsVoiceCall);
        }

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }



    private void updateVolumeSteps(int streamType, int steps) {
        //Change the setting live
        mAudioManager.setStreamMaxVolume(streamType, steps);
    }

    private void updateVolumeSteps(String settingsKey, int steps){

        int streamType = -1;
        if (settingsKey.equals(KEY_VOLUME_STEPS_ALARM))
            streamType = mAudioManager.STREAM_ALARM;

        else if (settingsKey.equals(KEY_VOLUME_STEPS_DTMF))
            streamType = mAudioManager.STREAM_DTMF;

        else if (settingsKey.equals(KEY_VOLUME_STEPS_MUSIC))
            streamType = mAudioManager.STREAM_MUSIC;

        else if (settingsKey.equals(KEY_VOLUME_STEPS_NOTIFICATION))
            streamType = mAudioManager.STREAM_NOTIFICATION;

        else if (settingsKey.equals(KEY_VOLUME_STEPS_RING))
            streamType = mAudioManager.STREAM_RING;

        else if (settingsKey.equals(KEY_VOLUME_STEPS_SYSTEM))
            streamType = mAudioManager.STREAM_SYSTEM;

        else if (settingsKey.equals(KEY_VOLUME_STEPS_VOICE_CALL))
            streamType = mAudioManager.STREAM_VOICE_CALL;

        //Save the setting for next boot
        Settings.System.putInt(getContentResolver(),
                settingsKey, steps);
        ((ListPreference)findPreference(settingsKey)).setSummary(String.valueOf(steps));

        updateVolumeSteps(streamType, steps);
        Log.i(TAG, "Volume steps:" + settingsKey + "" +String.valueOf(steps));

        }


    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mVolumeStepsAlarm) {
            updateVolumeSteps(preference.getKey(),Integer.parseInt(objValue.toString()));
        } else if (preference == mVolumeStepsDTMF) {
            updateVolumeSteps(preference.getKey(),Integer.parseInt(objValue.toString()));
        } else if (preference == mVolumeStepsMusic) {
            updateVolumeSteps(preference.getKey(),Integer.parseInt(objValue.toString()));
        } else if (preference == mVolumeStepsNotification) {
            updateVolumeSteps(preference.getKey(),Integer.parseInt(objValue.toString()));
        } else if (preference == mVolumeStepsRing) {
            updateVolumeSteps(preference.getKey(),Integer.parseInt(objValue.toString()));
        } else if (preference == mVolumeStepsSystem) {
            updateVolumeSteps(preference.getKey(),Integer.parseInt(objValue.toString()));
        } else if (preference == mVolumeStepsVoiceCall) {
            updateVolumeSteps(preference.getKey(),Integer.parseInt(objValue.toString()));
        } 
        return true;
    }
}

