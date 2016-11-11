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

package com.android.settings.nfc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreference;

/**
 * NfcEnabler is a helper to manage the Nfc on/off checkbox preference. It is
 * turns on/off Nfc and ensures the summary of the preference reflects the
 * current state.
 */
public class NfcEnabler implements Preference.OnPreferenceChangeListener {
    private final Context mContext;
    private final SwitchPreference mSwitch;
    private final RestrictedPreference mAndroidBeam;
    private final PreferenceScreen mNfcPayment;
    private final NfcAdapter mNfcAdapter;
    private final IntentFilter mIntentFilter;
    private boolean mBeamDisallowedBySystem;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (NfcAdapter.ACTION_ADAPTER_STATE_CHANGED.equals(action)) {
                handleNfcStateChanged(intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE,
                        NfcAdapter.STATE_OFF));
            }
        }
    };

    public NfcEnabler(Context context, SwitchPreference switchPreference,
            RestrictedPreference androidBeam, PreferenceScreen nfcPayment) {
        mContext = context;
        mSwitch = switchPreference;
        mAndroidBeam = androidBeam;
        mNfcPayment = nfcPayment;
        mNfcAdapter = NfcAdapter.getDefaultAdapter(context);
        mBeamDisallowedBySystem = RestrictedLockUtils.hasBaseUserRestriction(context,
                UserManager.DISALLOW_OUTGOING_BEAM, UserHandle.myUserId());

        if (mNfcAdapter == null) {
            // NFC is not supported
            mSwitch.setEnabled(false);
            mAndroidBeam.setEnabled(false);
            mNfcPayment.setEnabled(false);
            mIntentFilter = null;
            return;
        }
        if (mBeamDisallowedBySystem) {
            mAndroidBeam.setEnabled(false);
        }
        mIntentFilter = new IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
    }

    public void resume() {
        if (mNfcAdapter == null) {
            return;
        }
        handleNfcStateChanged(mNfcAdapter.getAdapterState());
        mContext.registerReceiver(mReceiver, mIntentFilter);
        mSwitch.setOnPreferenceChangeListener(this);
    }

    public void pause() {
        if (mNfcAdapter == null) {
            return;
        }
        mContext.unregisterReceiver(mReceiver);
        mSwitch.setOnPreferenceChangeListener(null);
    }

    public boolean onPreferenceChange(Preference preference, Object value) {
        // Turn NFC on/off

        final boolean desiredState = (Boolean) value;
        mSwitch.setEnabled(false);

        if (desiredState) {
            mNfcAdapter.enable();
        } else {
            mNfcAdapter.disable();
        }

        return false;
    }

    private void handleNfcStateChanged(int newState) {
        switch (newState) {
        case NfcAdapter.STATE_OFF:
            mSwitch.setChecked(false);
            mSwitch.setEnabled(true);
            mAndroidBeam.setEnabled(false);
            mAndroidBeam.setSummary(R.string.android_beam_disabled_summary);
            mNfcPayment.setEnabled(false);
            break;
        case NfcAdapter.STATE_ON:
            mSwitch.setChecked(true);
            mSwitch.setEnabled(true);
            if (mBeamDisallowedBySystem) {
                mAndroidBeam.setDisabledByAdmin(null);
                mAndroidBeam.setEnabled(false);
            } else {
                mAndroidBeam.checkRestrictionAndSetDisabled(UserManager.DISALLOW_OUTGOING_BEAM);
            }
            if (mNfcAdapter.isNdefPushEnabled() && mAndroidBeam.isEnabled()) {
                mAndroidBeam.setSummary(R.string.android_beam_on_summary);
            } else {
                mAndroidBeam.setSummary(R.string.android_beam_off_summary);
            }
            mNfcPayment.setEnabled(true);
            break;
        case NfcAdapter.STATE_TURNING_ON:
            mSwitch.setChecked(true);
            mSwitch.setEnabled(false);
            mAndroidBeam.setEnabled(false);
            mNfcPayment.setEnabled(false);
            break;
        case NfcAdapter.STATE_TURNING_OFF:
            mSwitch.setChecked(false);
            mSwitch.setEnabled(false);
            mAndroidBeam.setEnabled(false);
            mNfcPayment.setEnabled(false);
            break;
        }
    }
}
