/*
 * Copyright (C) 2008 The Android Open Source Project
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
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources.NotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Telephony;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.telephony.CarrierConfigManager;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.dataconnection.ApnSetting;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.UiccController;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

public class ApnSettings extends RestrictedSettingsFragment implements
        Preference.OnPreferenceChangeListener {
    static final String TAG = "ApnSettings";

    public static final String EXTRA_POSITION = "position";
    public static final String RESTORE_CARRIERS_URI =
        "content://telephony/carriers/restore";
    public static final String PREFERRED_APN_URI =
        "content://telephony/carriers/preferapn";

    public static final String APN_ID = "apn_id";
    public static final String SUB_ID = "sub_id";
    public static final String MVNO_TYPE = "mvno_type";
    public static final String MVNO_MATCH_DATA = "mvno_match_data";

    private static final String APN_NAME_DM = "CMCC DM";

    private static final int ID_INDEX = 0;
    private static final int NAME_INDEX = 1;
    private static final int APN_INDEX = 2;
    private static final int TYPES_INDEX = 3;
    private static final int MVNO_TYPE_INDEX = 4;
    private static final int MVNO_MATCH_DATA_INDEX = 5;
    private static final int RO_INDEX = 6;
    private static final int BEARER_INDEX = 7;
    private static final int BEARER_BITMASK_INDEX = 8;

    private static final int MENU_NEW = Menu.FIRST;
    private static final int MENU_RESTORE = Menu.FIRST + 1;

    private static final int EVENT_RESTORE_DEFAULTAPN_START = 1;
    private static final int EVENT_RESTORE_DEFAULTAPN_COMPLETE = 2;

    private static final int DIALOG_RESTORE_DEFAULTAPN = 1001;

    private static final Uri DEFAULTAPN_URI = Uri.parse(RESTORE_CARRIERS_URI);
    private static final Uri PREFERAPN_URI = Uri.parse(PREFERRED_APN_URI);

    private static boolean mRestoreDefaultApnMode;

    private UserManager mUserManager;
    private RestoreApnUiHandler mRestoreApnUiHandler;
    private RestoreApnProcessHandler mRestoreApnProcessHandler;
    private HandlerThread mRestoreDefaultApnThread;
    private SubscriptionInfo mSubscriptionInfo;
    private UiccController mUiccController;
    private String mMvnoType;
    private String mMvnoMatchData;

    private String mSelectedKey;

    private IntentFilter mMobileStateFilter;

    private boolean mUnavailable;

    private boolean mHideImsApn;
    private boolean mAllowAddingApns;
    private boolean mApnSettingsHidden;

    public ApnSettings() {
        super(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS);
    }

    private HashSet mIccidSet;

    private final BroadcastReceiver mMobileStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(
                    TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED)) {
                PhoneConstants.DataState state = getMobileDataState(intent);
                switch (state) {
                case CONNECTED:
                    if (!mRestoreDefaultApnMode) {
                        fillList();
                    }
                    break;
                }
            }
        }
    };

    private static PhoneConstants.DataState getMobileDataState(Intent intent) {
        String str = intent.getStringExtra(PhoneConstants.STATE_KEY);
        if (str != null) {
            return Enum.valueOf(PhoneConstants.DataState.class, str);
        } else {
            return PhoneConstants.DataState.DISCONNECTED;
        }
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.APN;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        final Activity activity = getActivity();
        final int subId = activity.getIntent().getIntExtra(SUB_ID,
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        fillOperatorIccidset();
        Log.d(TAG, "onCreate: subId = " + subId);


        mMobileStateFilter = new IntentFilter(
                TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);

        setIfOnlyAvailableForAdmins(true);

        mSubscriptionInfo = SubscriptionManager.from(activity).getActiveSubscriptionInfo(subId);
        mUiccController = UiccController.getInstance();

        CarrierConfigManager configManager = (CarrierConfigManager)
                getSystemService(Context.CARRIER_CONFIG_SERVICE);
        PersistableBundle b = configManager.getConfig();
        mHideImsApn = b.getBoolean(CarrierConfigManager.KEY_HIDE_IMS_APN_BOOL);
        mAllowAddingApns = b.getBoolean(CarrierConfigManager.KEY_ALLOW_ADDING_APNS_BOOL);
        mUserManager = UserManager.get(activity);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getEmptyTextView().setText(R.string.apn_settings_not_available);
        mUnavailable = isUiRestricted();
        setHasOptionsMenu(!mUnavailable);
        if (mUnavailable) {
            setPreferenceScreen(new PreferenceScreen(getPrefContext(), null));
            getPreferenceScreen().removeAll();
            return;
        }

        addPreferencesFromResource(R.xml.apn_settings);
    }

    @Override
    public void onStop() {
        super.onStop();
        mApnSettingsHidden = true;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mUnavailable) {
            return;
        }

        getActivity().registerReceiver(mMobileStateReceiver, mMobileStateFilter);

        if (!mRestoreDefaultApnMode) {
            fillList();
        }
        mApnSettingsHidden = false;
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mUnavailable) {
            return;
        }

        getActivity().unregisterReceiver(mMobileStateReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mRestoreDefaultApnThread != null) {
            mRestoreDefaultApnThread.quit();
        }
    }

    @Override
    public EnforcedAdmin getRestrictionEnforcedAdmin() {
        final UserHandle user = UserHandle.of(mUserManager.getUserHandle());
        if (mUserManager.hasUserRestriction(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS, user)
                && !mUserManager.hasBaseUserRestriction(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS,
                        user)) {
            return EnforcedAdmin.MULTIPLE_ENFORCED_ADMIN;
        }
        return null;
    }

    private void fillOperatorIccidset(){
        mIccidSet = new HashSet<String>();
        mIccidSet.add("8991840");
        mIccidSet.add("8991854");
        mIccidSet.add("8991855");
        mIccidSet.add("8991856");
        mIccidSet.add("8991857");
        mIccidSet.add("8991858");
        mIccidSet.add("8991859");
        mIccidSet.add("899186");
        mIccidSet.add("8991870");
        mIccidSet.add("8991871");
        mIccidSet.add("8991872");
        mIccidSet.add("8991873");
        mIccidSet.add("8991874");
    }

    private void fillList() {
        final TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        final String mccmnc = mSubscriptionInfo == null ? ""
                : tm.getSimOperator(mSubscriptionInfo.getSubscriptionId());
        Log.d(TAG, "mccmnc = " + mccmnc);
        StringBuilder where = new StringBuilder("numeric=\"" + mccmnc +
                "\" AND NOT (type='ia' AND (apn=\"\" OR apn IS NULL)) AND user_visible!=0");

        if (SystemProperties.getBoolean("persist.sys.hideapn", true)) {
            Log.d(TAG, "hiden apn feature enable.");
            // remove the filtered items, no need to show in UI

            if(getResources().getBoolean(R.bool.config_hide_ims_apns)){
                mHideImsApn = true;
            }

            // Filer fota and dm for specail carrier
            if (getResources().getBoolean(R.bool.config_hide_dm_enabled)) {
                for (String plmn : getResources().getStringArray(R.array.hidedm_plmn_list)) {
                    if (plmn.equals(mccmnc)) {
                        where.append(" and name <>\"" + APN_NAME_DM + "\"");
                        break;
                    }
                }
            }

            if (getResources().getBoolean(R.bool.config_hidesupl_enable)) {
                boolean needHideSupl = false;
                for (String plmn : getResources().getStringArray(R.array.hidesupl_plmn_list)) {
                    if (plmn.equals(mccmnc)) {
                        needHideSupl = true;
                        break;
                    }
                }

                if (needHideSupl) {
                    where.append(" and type <>\"" + PhoneConstants.APN_TYPE_SUPL + "\"");
                }
            }

            // Hide mms if config is true
            if (getResources().getBoolean(R.bool.config_hide_mms_enable)) {
                  where.append( " and type <>\"" + PhoneConstants.APN_TYPE_MMS + "\"");
            }
        }

        if(getResources().getBoolean(R.bool.config_regional_hide_ims_and_dun_apns)){
            where.append(" AND type <>\"" + PhoneConstants.APN_TYPE_DUN + "\"");
            where.append(" AND type <>\"" + PhoneConstants.APN_TYPE_IMS + "\"");
        }
        if (mHideImsApn) {
            where.append(" AND NOT (type='ims')");
        }

        if (isOperatorIccId()) {
            where.append(" AND type <>\"" + PhoneConstants.APN_TYPE_EMERGENCY + "\"");
            where.append(" AND type <>\"" + PhoneConstants.APN_TYPE_IMS + "\"");
        }
        Log.d(TAG, "where---" + where);

        Cursor cursor = getContentResolver().query(Telephony.Carriers.CONTENT_URI, new String[] {
                "_id", "name", "apn", "type", "mvno_type", "mvno_match_data", "read_only", "bearer",
                "bearer_bitmask"}, where.toString(), null, Telephony.Carriers.DEFAULT_SORT_ORDER);

        if (cursor != null) {
            IccRecords r = null;
            if (mUiccController != null && mSubscriptionInfo != null) {
                r = mUiccController.getIccRecords(SubscriptionManager.getPhoneId(
                        mSubscriptionInfo.getSubscriptionId()), UiccController.APP_FAM_3GPP);
            }
            PreferenceGroup apnList = (PreferenceGroup) findPreference("apn_list");
            apnList.removeAll();

            ArrayList<ApnPreference> mnoApnList = new ArrayList<ApnPreference>();
            ArrayList<ApnPreference> mvnoApnList = new ArrayList<ApnPreference>();
            ArrayList<ApnPreference> mnoMmsApnList = new ArrayList<ApnPreference>();
            ArrayList<ApnPreference> mvnoMmsApnList = new ArrayList<ApnPreference>();

            mSelectedKey = getSelectedApnKey();
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                String name = cursor.getString(NAME_INDEX);
                String apn = cursor.getString(APN_INDEX);
                String key = cursor.getString(ID_INDEX);
                String type = cursor.getString(TYPES_INDEX);
                String mvnoType = cursor.getString(MVNO_TYPE_INDEX);
                String mvnoMatchData = cursor.getString(MVNO_MATCH_DATA_INDEX);
                boolean readOnly = (cursor.getInt(RO_INDEX) == 1);
                String localizedName = getLocalizedName(getActivity(), cursor, NAME_INDEX);
                if (!TextUtils.isEmpty(localizedName)) {
                    name = localizedName;
                }
                int bearer = cursor.getInt(BEARER_INDEX);
                int bearerBitMask = cursor.getInt(BEARER_BITMASK_INDEX);
                int fullBearer = ServiceState.getBitmaskForTech(bearer) | bearerBitMask;
                int subId = mSubscriptionInfo != null ? mSubscriptionInfo.getSubscriptionId()
                        : SubscriptionManager.INVALID_SUBSCRIPTION_ID;
                int radioTech = networkTypeToRilRidioTechnology(TelephonyManager.getDefault()
                        .getDataNetworkType(subId));
                if (!ServiceState.bitmaskHasTech(fullBearer, radioTech)
                        && (bearer != 0 || bearerBitMask != 0)) {
                    // In OOS, show APN with bearer as default
                    if ((radioTech != ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN) || (bearer == 0
                            && radioTech == ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN)) {
                        cursor.moveToNext();
                        continue;
                    }
                }
                ApnPreference pref = new ApnPreference(getPrefContext());

                pref.setApnReadOnly(readOnly);
                pref.setKey(key);
                pref.setTitle(name);
                pref.setSummary(apn);
                pref.setPersistent(false);
                pref.setOnPreferenceChangeListener(this);

                boolean selectable = ((type == null) || !type.equals("mms"));
                pref.setSelectable(selectable);
                if (selectable) {
                    if ((mSelectedKey != null) && mSelectedKey.equals(key)) {
                        pref.setChecked();
                    }
                    addApnToList(pref, mnoApnList, mvnoApnList, r, mvnoType, mvnoMatchData);
                } else {
                    addApnToList(pref, mnoMmsApnList, mvnoMmsApnList, r, mvnoType, mvnoMatchData);
                }
                cursor.moveToNext();
            }
            cursor.close();

            if (!mvnoApnList.isEmpty()) {
                mnoApnList = mvnoApnList;
                mnoMmsApnList = mvnoMmsApnList;

                // Also save the mvno info
            }

            for (Preference preference : mnoApnList) {
                apnList.addPreference(preference);
            }
            for (Preference preference : mnoMmsApnList) {
                apnList.addPreference(preference);
            }
        }
    }

    private boolean isOperatorIccId(){
        final String iccid = mSubscriptionInfo == null ? ""
                : mSubscriptionInfo.getIccId();
        Iterator<String> itr = mIccidSet.iterator();
        while (itr.hasNext()) {
            if (iccid.contains(itr.next())) {
                return true;
            }
        }
        return false;
    }

    private int networkTypeToRilRidioTechnology(int nt) {
        switch(nt) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
                return ServiceState.RIL_RADIO_TECHNOLOGY_GPRS;
            case TelephonyManager.NETWORK_TYPE_EDGE:
                return ServiceState.RIL_RADIO_TECHNOLOGY_EDGE;
            case TelephonyManager.NETWORK_TYPE_UMTS:
                return ServiceState.RIL_RADIO_TECHNOLOGY_UMTS;
            case TelephonyManager.NETWORK_TYPE_HSDPA:
                return ServiceState.RIL_RADIO_TECHNOLOGY_HSDPA;
            case TelephonyManager.NETWORK_TYPE_HSUPA:
                return ServiceState.RIL_RADIO_TECHNOLOGY_HSUPA;
            case TelephonyManager.NETWORK_TYPE_HSPA:
                return ServiceState.RIL_RADIO_TECHNOLOGY_HSPA;
            case TelephonyManager.NETWORK_TYPE_CDMA:
                return ServiceState.RIL_RADIO_TECHNOLOGY_IS95B;
            case TelephonyManager.NETWORK_TYPE_1xRTT:
                return ServiceState.RIL_RADIO_TECHNOLOGY_1xRTT;
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
                return ServiceState.RIL_RADIO_TECHNOLOGY_EVDO_0;
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
                return ServiceState.RIL_RADIO_TECHNOLOGY_EVDO_A;
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
                return ServiceState.RIL_RADIO_TECHNOLOGY_EVDO_B;
            case TelephonyManager.NETWORK_TYPE_EHRPD:
                return ServiceState.RIL_RADIO_TECHNOLOGY_EHRPD;
            case TelephonyManager.NETWORK_TYPE_LTE:
                return ServiceState.RIL_RADIO_TECHNOLOGY_LTE;
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return ServiceState.RIL_RADIO_TECHNOLOGY_HSPAP;
            case TelephonyManager.NETWORK_TYPE_GSM:
                return ServiceState.RIL_RADIO_TECHNOLOGY_GSM;
            case TelephonyManager.NETWORK_TYPE_TD_SCDMA:
                return ServiceState.RIL_RADIO_TECHNOLOGY_TD_SCDMA;
            case TelephonyManager.NETWORK_TYPE_IWLAN:
                return ServiceState.RIL_RADIO_TECHNOLOGY_IWLAN;
            case TelephonyManager.NETWORK_TYPE_LTE_CA:
                return ServiceState.RIL_RADIO_TECHNOLOGY_LTE_CA;
            default:
                return ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN;
        }
    }

    public static String getLocalizedName(Context context, Cursor cursor, int index) {
        // If can find a localized name, replace the APN name with it
        String resName = cursor.getString(index);
        String localizedName = null;
        if (resName != null && !resName.isEmpty()) {
            int resId = context.getResources().getIdentifier(resName, "string",
                    context.getPackageName());
            try {
                localizedName = context.getResources().getString(resId);
                Log.d(TAG, "Replaced apn name with localized name");
            } catch (NotFoundException e) {
                Log.e(TAG, "Got execption while getting the localized apn name.", e);
            }
        }
        return localizedName;
    }

    private void addApnToList(ApnPreference pref, ArrayList<ApnPreference> mnoList,
                              ArrayList<ApnPreference> mvnoList, IccRecords r, String mvnoType,
                              String mvnoMatchData) {
        if (r != null && !TextUtils.isEmpty(mvnoType) && !TextUtils.isEmpty(mvnoMatchData)) {
            if (ApnSetting.mvnoMatches(r, mvnoType, mvnoMatchData)) {
                mvnoList.add(pref);
                // Since adding to mvno list, save mvno info
                mMvnoType = mvnoType;
                mMvnoMatchData = mvnoMatchData;
            }
        } else {
            mnoList.add(pref);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!mUnavailable) {
            if (mAllowAddingApns) {
                menu.add(0, MENU_NEW, 0,
                        getResources().getString(R.string.menu_new))
                        .setIcon(R.drawable.ic_menu_add_white)
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            }
            menu.add(0, MENU_RESTORE, 0,
                    getResources().getString(R.string.menu_restore))
                    .setIcon(android.R.drawable.ic_menu_upload);
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_NEW:
            addNewApn();
            return true;

        case MENU_RESTORE:
            restoreDefaultApn();
            return true;

        case android.R.id.home:
            getActivity().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void addNewApn() {
        Intent intent = new Intent(Intent.ACTION_INSERT, Telephony.Carriers.CONTENT_URI);
        int subId = mSubscriptionInfo != null ? mSubscriptionInfo.getSubscriptionId()
                : SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        intent.putExtra(SUB_ID, subId);
        if (!TextUtils.isEmpty(mMvnoType) && !TextUtils.isEmpty(mMvnoMatchData)) {
            intent.putExtra(MVNO_TYPE, mMvnoType);
            intent.putExtra(MVNO_MATCH_DATA, mMvnoMatchData);
        }
        startActivity(intent);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        int pos = Integer.parseInt(preference.getKey());
        Uri url = ContentUris.withAppendedId(Telephony.Carriers.CONTENT_URI, pos);
        Intent intent = new Intent(Intent.ACTION_EDIT, url);
        if (preference instanceof ApnPreference) {
            intent.putExtra("DISABLE_EDITOR", ((ApnPreference) preference).getApnReadOnly());
        }
        startActivity(intent);
        return true;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.d(TAG, "onPreferenceChange(): Preference - " + preference
                + ", newValue - " + newValue + ", newValue type - "
                + newValue.getClass());
        if (newValue instanceof String) {
            setSelectedApnKey((String) newValue);
        }

        return true;
    }

    private void setSelectedApnKey(String key) {
        mSelectedKey = key;
        ContentResolver resolver = getContentResolver();

        ContentValues values = new ContentValues();
        values.put(APN_ID, mSelectedKey);
        resolver.update(getUri(PREFERAPN_URI), values, null, null);
    }

    private String getSelectedApnKey() {
        String key = null;

        Cursor cursor = getContentResolver().query(getUri(PREFERAPN_URI), new String[] {"_id"},
                null, null, Telephony.Carriers.DEFAULT_SORT_ORDER);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            key = cursor.getString(ID_INDEX);
        }
        cursor.close();
        return key;
    }

    private boolean restoreDefaultApn() {
        showDialog(DIALOG_RESTORE_DEFAULTAPN);
        mRestoreDefaultApnMode = true;

        if (mRestoreApnUiHandler == null) {
            mRestoreApnUiHandler = new RestoreApnUiHandler();
        }

        if (mRestoreApnProcessHandler == null ||
            mRestoreDefaultApnThread == null) {
            mRestoreDefaultApnThread = new HandlerThread(
                    "Restore default APN Handler: Process Thread");
            mRestoreDefaultApnThread.start();
            mRestoreApnProcessHandler = new RestoreApnProcessHandler(
                    mRestoreDefaultApnThread.getLooper(), mRestoreApnUiHandler);
        }

        mRestoreApnProcessHandler
                .sendEmptyMessage(EVENT_RESTORE_DEFAULTAPN_START);
        return true;
    }

    private class RestoreApnUiHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_RESTORE_DEFAULTAPN_COMPLETE:
                    Activity activity = getActivity();
                    if (activity == null) {
                        mRestoreDefaultApnMode = false;
                        return;
                    }
                    fillList();
                    getPreferenceScreen().setEnabled(true);
                    mRestoreDefaultApnMode = false;
                    // if current fragment is not visible, in background or Homekey is pressed,
                    // dismiss the dialog with state loss.
                    removeDialog(DIALOG_RESTORE_DEFAULTAPN, mApnSettingsHidden);
                    Toast.makeText(
                        activity,
                        getResources().getString(
                                R.string.restore_default_apn_completed),
                        Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

    private class RestoreApnProcessHandler extends Handler {
        private Handler mRestoreApnUiHandler;

        public RestoreApnProcessHandler(Looper looper, Handler restoreApnUiHandler) {
            super(looper);
            this.mRestoreApnUiHandler = restoreApnUiHandler;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_RESTORE_DEFAULTAPN_START:
                    ContentResolver resolver = getContentResolver();
                    resolver.delete(getUri(DEFAULTAPN_URI), null, null);
                    mRestoreApnUiHandler
                        .sendEmptyMessage(EVENT_RESTORE_DEFAULTAPN_COMPLETE);
                    break;
            }
        }
    }

    @Override
    public Dialog onCreateDialog(int id) {
        if (id == DIALOG_RESTORE_DEFAULTAPN) {
            ProgressDialog dialog = new ProgressDialog(getActivity()) {
                public boolean onTouchEvent(MotionEvent event) {
                    return true;
                }
            };
            dialog.setMessage(getResources().getString(R.string.restore_default_apn));
            dialog.setCancelable(false);
            return dialog;
        }
        return null;
    }

    private Uri getUri(Uri uri) {
        int subId = SubscriptionManager.getDefaultDataSubscriptionId();
        if (mSubscriptionInfo != null && SubscriptionManager.isValidSubscriptionId(
                mSubscriptionInfo.getSubscriptionId())) {
            subId = mSubscriptionInfo.getSubscriptionId();
        }
        return Uri.withAppendedPath(uri, "/subId/" + subId);
    }
}
