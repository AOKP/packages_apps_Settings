/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.telephony.MSimTelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Switch;

import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;
import com.android.settings.nfc.NfcEnabler;
import com.android.settings.NsdEnabler;

public class WirelessSettings extends SettingsPreferenceFragment
    implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "WirelessSettings";

    private static final String KEY_TOGGLE_AIRPLANE = "toggle_airplane";
    private static final String KEY_TOGGLE_NFC = "toggle_nfc";
    private static final String KEY_WIMAX_SETTINGS = "wimax_settings";
    private static final String KEY_ANDROID_BEAM_SETTINGS = "android_beam_settings";
    private static final String KEY_VPN_SETTINGS = "vpn_settings";
    private static final String KEY_TETHER_SETTINGS = "tether_settings";
    private static final String KEY_PROXY_SETTINGS = "proxy_settings";
    private static final String KEY_MOBILE_NETWORK_SETTINGS = "mobile_network_settings";
    private static final String KEY_MANAGE_MOBILE_PLAN = "manage_mobile_plan";
    private static final String KEY_TOGGLE_NSD = "toggle_nsd"; //network service discovery
    private static final String KEY_CELL_BROADCAST_SETTINGS = "cell_broadcast_settings";
    private static final String KEY_NFC_POLLING_MODE = "nfc_polling_mode";

    public static final String EXIT_ECM_RESULT = "exit_ecm_result";
    public static final int REQUEST_CODE_EXIT_ECM = 1;

    private AirplaneModeEnabler mAirplaneModeEnabler;
    private CheckBoxPreference mAirplaneModePreference;
    private NfcEnabler mNfcEnabler;
    private NfcAdapter mNfcAdapter;
    private NsdEnabler mNsdEnabler;
    private ListPreference mNfcPollingMode;

    private ConnectivityManager mCm;
    private TelephonyManager mTm;

    private static final int MANAGE_MOBILE_PLAN_DIALOG_ID = 1;
    private static final String SAVED_MANAGE_MOBILE_PLAN_MSG = "mManageMobilePlanMessage";

    private String mManageMobilePlanMessage;
    private static final String CONNECTED_TO_PROVISIONING_NETWORK_ACTION
            = "com.android.server.connectivityservice.CONNECTED_TO_PROVISIONING_NETWORK_ACTION";

    /**
     * Invoked on each preference click in this hierarchy, overrides
     * PreferenceActivity's implementation.  Used to make sure we track the
     * preference click events.
     */
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        log("onPreferenceTreeClick: preference=" + preference);
        if (preference == mAirplaneModePreference && Boolean.parseBoolean(
                SystemProperties.get(TelephonyProperties.PROPERTY_INECM_MODE))) {
            // In ECM mode launch ECM app dialog
            startActivityForResult(
                new Intent(TelephonyIntents.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS, null),
                REQUEST_CODE_EXIT_ECM);
            return true;
        } else if (preference == findPreference(KEY_MANAGE_MOBILE_PLAN)) {
            onManageMobilePlanClick();
        }
        // Let the intents be launched by the Preference manager
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mNfcPollingMode) {
            int newVal = Integer.parseInt((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.NFC_POLLING_MODE, newVal);
            updateNfcPolling();
            return true;
        }
        return false;
    }

    public void onManageMobilePlanClick() {
        log("onManageMobilePlanClick:");
        mManageMobilePlanMessage = null;
        Resources resources = getActivity().getResources();

        NetworkInfo ni = mCm.getProvisioningOrActiveNetworkInfo();
        if (mTm.hasIccCard() && (ni != null)) {
            // Get provisioning URL
            String url = mCm.getMobileProvisioningUrl();
            if (!TextUtils.isEmpty(url)) {
                Intent intent = new Intent(CONNECTED_TO_PROVISIONING_NETWORK_ACTION);
                intent.putExtra("EXTRA_URL", url);
                Context context = getActivity().getBaseContext();
                context.sendBroadcast(intent);
                mManageMobilePlanMessage = null;
            } else {
                // No provisioning URL
                String operatorName = mTm.getSimOperatorName();
                if (TextUtils.isEmpty(operatorName)) {
                    // Use NetworkOperatorName as second choice in case there is no
                    // SPN (Service Provider Name on the SIM). Such as with T-mobile.
                    operatorName = mTm.getNetworkOperatorName();
                    if (TextUtils.isEmpty(operatorName)) {
                        mManageMobilePlanMessage = resources.getString(
                                R.string.mobile_unknown_sim_operator);
                    } else {
                        mManageMobilePlanMessage = resources.getString(
                                R.string.mobile_no_provisioning_url, operatorName);
                    }
                } else {
                    mManageMobilePlanMessage = resources.getString(
                            R.string.mobile_no_provisioning_url, operatorName);
                }
            }
        } else if (mTm.hasIccCard() == false) {
            // No sim card
            mManageMobilePlanMessage = resources.getString(R.string.mobile_insert_sim_card);
        } else {
            // NetworkInfo is null, there is no connection
            mManageMobilePlanMessage = resources.getString(R.string.mobile_connect_to_internet);
        }
        if (!TextUtils.isEmpty(mManageMobilePlanMessage)) {
            log("onManageMobilePlanClick: message=" + mManageMobilePlanMessage);
            showDialog(MANAGE_MOBILE_PLAN_DIALOG_ID);
        }
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        log("onCreateDialog: dialogId=" + dialogId);
        switch (dialogId) {
            case MANAGE_MOBILE_PLAN_DIALOG_ID:
                return new AlertDialog.Builder(getActivity())
                            .setMessage(mManageMobilePlanMessage)
                            .setCancelable(false)
                            .setPositiveButton(com.android.internal.R.string.ok,
                                    new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    log("MANAGE_MOBILE_PLAN_DIALOG.onClickListener id=" + id);
                                    mManageMobilePlanMessage = null;
                                }
                            })
                            .create();
        }
        return super.onCreateDialog(dialogId);
    }

    private void log(String s) {
        Log.d(TAG, s);
    }

    public static boolean isRadioAllowed(Context context, String type) {
        if (!AirplaneModeEnabler.isAirplaneModeOn(context)) {
            return true;
        }
        // Here we use the same logic in onCreate().
        String toggleable = Settings.Global.getString(context.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_TOGGLEABLE_RADIOS);
        return toggleable != null && toggleable.contains(type);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mManageMobilePlanMessage = savedInstanceState.getString(SAVED_MANAGE_MOBILE_PLAN_MSG);
        }
        log("onCreate: mManageMobilePlanMessage=" + mManageMobilePlanMessage);

        mCm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        mTm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        addPreferencesFromResource(R.xml.wireless_settings);

        final boolean isSecondaryUser = UserHandle.myUserId() != UserHandle.USER_OWNER;

        final Activity activity = getActivity();
        mAirplaneModePreference = (CheckBoxPreference) findPreference(KEY_TOGGLE_AIRPLANE);

        if (MSimTelephonyManager.getDefault().isMultiSimEnabled()) {
            findPreference(KEY_MOBILE_NETWORK_SETTINGS).getIntent().setClassName(
                    "com.android.phone", "com.android.phone.MSimMobileNetworkSettings");
        }

        CheckBoxPreference nfc = (CheckBoxPreference) findPreference(KEY_TOGGLE_NFC);
        PreferenceScreen androidBeam = (PreferenceScreen) findPreference(KEY_ANDROID_BEAM_SETTINGS);
        CheckBoxPreference nsd = (CheckBoxPreference) findPreference(KEY_TOGGLE_NSD);

        mNfcPollingMode = (ListPreference) findPreference(KEY_NFC_POLLING_MODE);
        mNfcPollingMode.setOnPreferenceChangeListener(this);
        mNfcPollingMode.setValue((Settings.System.getInt(activity.getContentResolver(),
                Settings.System.NFC_POLLING_MODE, 3)) + "");
        updateNfcPolling();

        mAirplaneModeEnabler = new AirplaneModeEnabler(activity, mAirplaneModePreference);
        mNfcEnabler = new NfcEnabler(activity, nfc, androidBeam, mNfcPollingMode);

        // Remove NSD checkbox by default
        getPreferenceScreen().removePreference(nsd);
        //mNsdEnabler = new NsdEnabler(activity, nsd);

        String toggleable = Settings.Global.getString(activity.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_TOGGLEABLE_RADIOS);

        //enable/disable wimax depending on the value in config.xml
        boolean isWimaxEnabled = !isSecondaryUser && this.getResources().getBoolean(
                com.android.internal.R.bool.config_wimaxEnabled);
        if (!isWimaxEnabled) {
            PreferenceScreen root = getPreferenceScreen();
            Preference ps = (Preference) findPreference(KEY_WIMAX_SETTINGS);
            if (ps != null) root.removePreference(ps);
        } else {
            if (toggleable == null || !toggleable.contains(Settings.Global.RADIO_WIMAX )
                    && isWimaxEnabled) {
                Preference ps = (Preference) findPreference(KEY_WIMAX_SETTINGS);
                ps.setDependency(KEY_TOGGLE_AIRPLANE);
            }
        }
        // Manually set dependencies for Wifi when not toggleable.
        if (toggleable == null || !toggleable.contains(Settings.Global.RADIO_WIFI)) {
            findPreference(KEY_VPN_SETTINGS).setDependency(KEY_TOGGLE_AIRPLANE);
        }
        if (isSecondaryUser) { // Disable VPN
            removePreference(KEY_VPN_SETTINGS);
        }

        // Manually set dependencies for Bluetooth when not toggleable.
        if (toggleable == null || !toggleable.contains(Settings.Global.RADIO_BLUETOOTH)) {
            // No bluetooth-dependent items in the list. Code kept in case one is added later.
        }

        // Manually set dependencies for NFC when not toggleable.
        if (toggleable == null || !toggleable.contains(Settings.Global.RADIO_NFC)) {
            findPreference(KEY_TOGGLE_NFC).setDependency(KEY_TOGGLE_AIRPLANE);
            findPreference(KEY_ANDROID_BEAM_SETTINGS).setDependency(KEY_TOGGLE_AIRPLANE);
        }

        // Remove NFC if its not available
        mNfcAdapter = NfcAdapter.getDefaultAdapter(activity);
        if (mNfcAdapter == null) {
            getPreferenceScreen().removePreference(nfc);
            getPreferenceScreen().removePreference(mNfcPollingMode);
            getPreferenceScreen().removePreference(androidBeam);
            mNfcEnabler = null;
        }

        // Remove Mobile Network Settings and Manage Mobile Plan if it's a wifi-only device.
        if (isSecondaryUser || Utils.isWifiOnly(getActivity())) {
            removePreference(KEY_MOBILE_NETWORK_SETTINGS);
            removePreference(KEY_MANAGE_MOBILE_PLAN);
        }

        // Enable Proxy selector settings if allowed.
        Preference mGlobalProxy = findPreference(KEY_PROXY_SETTINGS);
        DevicePolicyManager mDPM = (DevicePolicyManager)
                activity.getSystemService(Context.DEVICE_POLICY_SERVICE);
        // proxy UI disabled until we have better app support
        getPreferenceScreen().removePreference(mGlobalProxy);
        mGlobalProxy.setEnabled(mDPM.getGlobalProxyAdmin() == null);

        // Disable Tethering if it's not allowed or if it's a wifi-only device
        ConnectivityManager cm =
                (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (isSecondaryUser || !cm.isTetheringSupported()) {
            getPreferenceScreen().removePreference(findPreference(KEY_TETHER_SETTINGS));
        } else {
            Preference p = findPreference(KEY_TETHER_SETTINGS);
            p.setTitle(Utils.getTetheringLabel(cm));
        }

        // Enable link to CMAS app settings depending on the value in config.xml.
        boolean isCellBroadcastAppLinkEnabled = this.getResources().getBoolean(
                com.android.internal.R.bool.config_cellBroadcastAppLinks);
        try {
            if (isCellBroadcastAppLinkEnabled) {
                PackageManager pm = getPackageManager();
                if (pm.getApplicationEnabledSetting("com.android.cellbroadcastreceiver")
                        == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                    isCellBroadcastAppLinkEnabled = false;  // CMAS app disabled
                }
            }
        } catch (IllegalArgumentException ignored) {
            isCellBroadcastAppLinkEnabled = false;  // CMAS app not installed
        }
        if (isSecondaryUser || !isCellBroadcastAppLinkEnabled) {
            PreferenceScreen root = getPreferenceScreen();
            Preference ps = findPreference(KEY_CELL_BROADCAST_SETTINGS);
            if (ps != null) root.removePreference(ps);
        }
    }

    private void updateNfcPolling() {
        int resId;
        String value = Settings.System.getString(getContentResolver(),
                Settings.System.NFC_POLLING_MODE);
        String[] pollingArray = getResources().getStringArray(R.array.nfc_polling_mode_values);

        if (pollingArray[0].equals(value)) {
            resId = R.string.nfc_polling_mode_screen_off;
            mNfcPollingMode.setValueIndex(0);
        } else if (pollingArray[1].equals(value)) {
            resId = R.string.nfc_polling_mode_screen_locked;
            mNfcPollingMode.setValueIndex(1);
        } else {
            resId = R.string.nfc_polling_mode_screen_unlocked;
            mNfcPollingMode.setValueIndex(2);
        }
        mNfcPollingMode.setSummary(getResources().getString(resId));
    }

    @Override
    public void onResume() {
        super.onResume();

        mAirplaneModeEnabler.resume();
        if (mNfcEnabler != null) {
            mNfcEnabler.resume();
        }
        if (mNsdEnabler != null) {
            mNsdEnabler.resume();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (!TextUtils.isEmpty(mManageMobilePlanMessage)) {
            outState.putString(SAVED_MANAGE_MOBILE_PLAN_MSG, mManageMobilePlanMessage);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        mAirplaneModeEnabler.pause();
        if (mNfcEnabler != null) {
            mNfcEnabler.pause();
        }
        if (mNsdEnabler != null) {
            mNsdEnabler.pause();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_EXIT_ECM) {
            Boolean isChoiceYes = data.getBooleanExtra(EXIT_ECM_RESULT, false);
            // Set Airplane mode based on the return value and checkbox state
            mAirplaneModeEnabler.setAirplaneModeInECM(isChoiceYes,
                    mAirplaneModePreference.isChecked());
        }
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_more_networks;
    }
}
