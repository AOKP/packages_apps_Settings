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

package com.android.settings.bluetooth;

import android.bluetooth.BluetoothUuid;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.ParcelUuid;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.bluetooth.BluetoothAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * CachedBluetoothDevice represents a remote Bluetooth device. It contains
 * attributes of the device (such as the address, name, RSSI, etc.) and
 * functionality that can be performed on the device (connect, pair, disconnect,
 * etc.).
 */
final class CachedBluetoothDevice implements Comparable<CachedBluetoothDevice> {
    private static final String TAG = "CachedBluetoothDevice";
    private static final boolean DEBUG = Utils.V;

    private final Context mContext;
    private final LocalBluetoothAdapter mLocalAdapter;
    private final LocalBluetoothProfileManager mProfileManager;
    private final BluetoothDevice mDevice;
    private String mName;
    private short mRssi;
    private BluetoothClass mBtClass;
    private HashMap<LocalBluetoothProfile, Integer> mProfileConnectionState;

    private final List<LocalBluetoothProfile> mProfiles =
            new ArrayList<LocalBluetoothProfile>();

    // List of profiles that were previously in mProfiles, but have been removed
    private final List<LocalBluetoothProfile> mRemovedProfiles =
            new ArrayList<LocalBluetoothProfile>();

    // Device supports PANU but not NAP: remove PanProfile after device disconnects from NAP
    private boolean mLocalNapRoleConnected;

    private boolean mVisible;

    private boolean mDeviceRemove;

    private int mPhonebookPermissionChoice;

    private int mMessagePermissionChoice;

    private int mPhonebookRejectedTimes;

    private int mMessageRejectedTimes;

    private final Collection<Callback> mCallbacks = new ArrayList<Callback>();

    // Following constants indicate the user's choices of Phone book/message access settings
    // User hasn't made any choice or settings app has wiped out the memory
    final static int ACCESS_UNKNOWN = 0;
    // User has accepted the connection and let Settings app remember the decision
    final static int ACCESS_ALLOWED = 1;
    // User has rejected the connection and let Settings app remember the decision
    final static int ACCESS_REJECTED = 2;

    // how many times did User reject the connection to make the rejected persist.
    final static int PERSIST_REJECTED_TIMES_LIMIT = 2;

    private final static String PHONEBOOK_PREFS_NAME = "bluetooth_phonebook_permission";
    private final static String MESSAGE_PREFS_NAME = "bluetooth_message_permission";
    private final static String PHONEBOOK_REJECT_TIMES = "bluetooth_phonebook_reject";
    private final static String MESSAGE_REJECT_TIMES = "bluetooth_message_reject";

    /**
     * When we connect to multiple profiles, we only want to display a single
     * error even if they all fail. This tracks that state.
     */
    private boolean mIsConnectingErrorPossible;

    /**
     * Last time a bt profile auto-connect was attempted.
     * If an ACTION_UUID intent comes in within
     * MAX_UUID_DELAY_FOR_AUTO_CONNECT milliseconds, we will try auto-connect
     * again with the new UUIDs
     */
    private long mConnectAttempted;

    // See mConnectAttempted
    private static final long MAX_UUID_DELAY_FOR_AUTO_CONNECT = 5000;

    private static final long MAX_HOGP_DELAY_FOR_AUTO_CONNECT = 30000;

    /** Auto-connect after pairing only if locally initiated. */
    private boolean mConnectAfterPairing;

    /**
     * Describes the current device and profile for logging.
     *
     * @param profile Profile to describe
     * @return Description of the device and profile
     */
    private String describe(LocalBluetoothProfile profile) {
        StringBuilder sb = new StringBuilder();
        sb.append("Address:").append(mDevice);
        if (profile != null) {
            sb.append(" Profile:").append(profile);
        }

        return sb.toString();
    }

    void onProfileStateChanged(LocalBluetoothProfile profile, int newProfileState) {
        if (Utils.D) {
            Log.d(TAG, "onProfileStateChanged: profile " + profile +
                    " newProfileState " + newProfileState);
        }
        if (mLocalAdapter.getBluetoothState() == BluetoothAdapter.STATE_TURNING_OFF)
        {
            if (Utils.D) Log.d(TAG, " BT Turninig Off...Profile conn state change ignored...");
            return;
        }
        mProfileConnectionState.put(profile, newProfileState);
        if (newProfileState == BluetoothProfile.STATE_CONNECTED) {
            if (!mProfiles.contains(profile)) {
                mRemovedProfiles.remove(profile);
                mProfiles.add(profile);
                if (profile instanceof PanProfile &&
                        ((PanProfile) profile).isLocalRoleNap(mDevice)) {
                    // Device doesn't support NAP, so remove PanProfile on disconnect
                    mLocalNapRoleConnected = true;
                }
            }
            if (profile instanceof MapProfile) {
                profile.setPreferred(mDevice, true);
            }
        } else if (profile instanceof MapProfile &&
                newProfileState == BluetoothProfile.STATE_DISCONNECTED) {
            if (mProfiles.contains(profile)) {
                mRemovedProfiles.add(profile);
                mProfiles.remove(profile);
            }
            profile.setPreferred(mDevice, false);
        } else if (mLocalNapRoleConnected && profile instanceof PanProfile &&
                ((PanProfile) profile).isLocalRoleNap(mDevice) &&
                newProfileState == BluetoothProfile.STATE_DISCONNECTED) {
            Log.d(TAG, "Removing PanProfile from device after NAP disconnect");
            mProfiles.remove(profile);
            mRemovedProfiles.add(profile);
            mLocalNapRoleConnected = false;
        }
    }

    CachedBluetoothDevice(Context context,
                          LocalBluetoothAdapter adapter,
                          LocalBluetoothProfileManager profileManager,
                          BluetoothDevice device) {
        mContext = context;
        mLocalAdapter = adapter;
        mProfileManager = profileManager;
        mDevice = device;
        mProfileConnectionState = new HashMap<LocalBluetoothProfile, Integer>();
        fillData();
    }

    void disconnect() {
        for (LocalBluetoothProfile profile : mProfiles) {
            disconnect(profile);
        }
        // Disconnect  PBAP server in case its connected
        // This is to ensure all the profiles are disconnected as some CK/Hs do not
        // disconnect  PBAP connection when HF connection is brought down
        PbapServerProfile PbapProfile = mProfileManager.getPbapProfile();
        if (PbapProfile.getConnectionStatus(mDevice) == BluetoothProfile.STATE_CONNECTED)
        {
            PbapProfile.disconnect(mDevice);
        }
    }

    void disconnect(LocalBluetoothProfile profile) {
        if (profile.disconnect(mDevice)) {
            if (Utils.D) {
                Log.d(TAG, "Command sent successfully:DISCONNECT " + describe(profile));
            }
        }
    }

    void connect(boolean connectAllProfiles) {
        if (!ensurePaired()) {
            return;
        }

        mConnectAttempted = SystemClock.elapsedRealtime();
        connectWithoutResettingTimer(connectAllProfiles);
    }

    void onBondingDockConnect() {
        // Attempt to connect if UUIDs are available. Otherwise,
        // we will connect when the ACTION_UUID intent arrives.
        connect(false);
    }

    private void connectWithoutResettingTimer(boolean connectAllProfiles) {
        // Try to initialize the profiles if they were not.
        if (mProfiles.isEmpty()) {
            // if mProfiles is empty, then do not invoke updateProfiles. This causes a race
            // condition with carkits during pairing, wherein RemoteDevice.UUIDs have been updated
            // from bluetooth stack but ACTION.uuid is not sent yet.
            // Eventually ACTION.uuid will be received which shall trigger the connection of the
            // various profiles
            // If UUIDs are not available yet, connect will be happen
            // upon arrival of the ACTION_UUID intent.
            Log.d(TAG, "No profiles. Maybe we will connect later");
            return;
        }

        // Reset the only-show-one-error-dialog tracking variable
        mIsConnectingErrorPossible = true;

        int preferredProfiles = 0;
        for (LocalBluetoothProfile profile : mProfiles) {
            if (connectAllProfiles ? profile.isConnectable() : profile.isAutoConnectable()) {
                if (profile.isPreferred(mDevice)) {
                    ++preferredProfiles;
                    connectInt(profile);
                }
            }
        }
        if (DEBUG) Log.d(TAG, "Preferred profiles = " + preferredProfiles);

        if (preferredProfiles == 0) {
            connectAutoConnectableProfiles();
        }
    }

    private void connectAutoConnectableProfiles() {
        if (!ensurePaired()) {
            return;
        }
        // Reset the only-show-one-error-dialog tracking variable
        mIsConnectingErrorPossible = true;

        for (LocalBluetoothProfile profile : mProfiles) {
            if (profile.isAutoConnectable()) {
                profile.setPreferred(mDevice, true);
                connectInt(profile);
            }
        }
    }

    /**
     * Connect this device to the specified profile.
     *
     * @param profile the profile to use with the remote device
     */
    void connectProfile(LocalBluetoothProfile profile) {
        mConnectAttempted = SystemClock.elapsedRealtime();
        // Reset the only-show-one-error-dialog tracking variable
        mIsConnectingErrorPossible = true;
        connectInt(profile);
        // Refresh the UI based on profile.connect() call
        refresh();
    }

    synchronized void connectInt(LocalBluetoothProfile profile) {
        if (!ensurePaired()) {
            return;
        }
        if (profile.connect(mDevice)) {
            if (Utils.D) {
                Log.d(TAG, "Command sent successfully:CONNECT " + describe(profile));
            }
            return;
        }
        Log.i(TAG, "Failed to connect " + profile.toString() + " to " + mName);
    }

    private boolean ensurePaired() {
        if (getBondState() == BluetoothDevice.BOND_NONE) {
            startPairing();
            return false;
        } else {
            return true;
        }
    }

    boolean startPairing() {
        // Pairing is unreliable while scanning, so cancel discovery
        if (mLocalAdapter.isDiscovering()) {
            mLocalAdapter.cancelDiscovery();
        }

        if (!mDevice.createBond()) {
            return false;
        }

        mConnectAfterPairing = true;  // auto-connect after pairing
        return true;
    }

    /**
     * Return true if user initiated pairing on this device. The message text is
     * slightly different for local vs. remote initiated pairing dialogs.
     */
    boolean isUserInitiatedPairing() {
        return mConnectAfterPairing;
    }

    void unpair() {
        int state = getBondState();

        if (state == BluetoothDevice.BOND_BONDING) {
            mDevice.cancelBondProcess();
        }

        if (state != BluetoothDevice.BOND_NONE) {
            final BluetoothDevice dev = mDevice;
            if (dev != null) {
                final boolean successful = dev.removeBond();
                if (successful) {
                    if (Utils.D) {
                        Log.d(TAG, "Command sent successfully:REMOVE_BOND " + describe(null));
                    }
                    setRemovable(true);
                } else if (Utils.V) {
                    Log.v(TAG, "Framework rejected command immediately:REMOVE_BOND " +
                            describe(null));
                }
            }
        }
    }

    int getProfileConnectionState(LocalBluetoothProfile profile) {
        if (mProfileConnectionState == null ||
                mProfileConnectionState.get(profile) == null) {
            // If cache is empty make the binder call to get the state
            int state = profile.getConnectionStatus(mDevice);
            mProfileConnectionState.put(profile, state);
        }
        return mProfileConnectionState.get(profile);
    }

    public void clearProfileConnectionState ()
    {
        if (Utils.D) {
            Log.d(TAG," Clearing all connection state for dev:" + mDevice.getName());
        }
        for (LocalBluetoothProfile profile :getProfiles()) {
            mProfileConnectionState.put(profile, BluetoothProfile.STATE_DISCONNECTED);
        }
    }

    // TODO: do any of these need to run async on a background thread?
    private void fillData() {
        fetchName();
        fetchBtClass();
        updateProfiles();
        fetchPhonebookPermissionChoice();
        fetchMessagePermissionChoice();
        fetchPhonebookRejectTimes();
        fetchMessageRejectTimes();

        mVisible = false;
        dispatchAttributesChanged();
    }

    BluetoothDevice getDevice() {
        return mDevice;
    }

    String getName() {
        return mName;
    }

    void setName(String name) {
        if (!mName.equals(name)) {
            if (TextUtils.isEmpty(name)) {
                // TODO: use friendly name for unknown device (bug 1181856)
                mName = mDevice.getAddress();
            } else {
                mName = name;
            }
            dispatchAttributesChanged();
        }
    }
    void setAliasName(String name) {
        if (!mName.equals(name)) {
            if (!TextUtils.isEmpty(name)) {
                mName = name;
                mDevice.setAlias(name);
            }
            dispatchAttributesChanged();
        }
    }

    void refreshName() {
        fetchName();
        dispatchAttributesChanged();
    }

    private void fetchName() {
        mName = mDevice.getAliasName();

        if (TextUtils.isEmpty(mName)) {
            mName = mDevice.getAddress();
            if (DEBUG) Log.d(TAG, "Device has no name (yet), use address: " + mName);
        }
    }

    void refresh() {
        dispatchAttributesChanged();
    }

    boolean isVisible() {
        return mVisible;
    }

    boolean isRemovable () {
        return mDeviceRemove;
   }


    void setVisible(boolean visible) {
        if (mVisible != visible) {
            mVisible = visible;
            dispatchAttributesChanged();
        }
    }

    void setRemovable(boolean removable) {
        mDeviceRemove = removable;
    }


    int getBondState() {
        return mDevice.getBondState();
    }

    void setRssi(short rssi) {
        if (mRssi != rssi) {
            mRssi = rssi;
            dispatchAttributesChanged();
        }
    }

    /**
     * Checks whether we are connected to this device (any profile counts).
     *
     * @return Whether it is connected.
     */
    boolean isConnected() {
        for (LocalBluetoothProfile profile : mProfiles) {
            int status = getProfileConnectionState(profile);
            if (status == BluetoothProfile.STATE_CONNECTED) {
                return true;
            }
        }

        return false;
    }

    boolean isConnectedProfile(LocalBluetoothProfile profile) {
        int status = getProfileConnectionState(profile);
        return status == BluetoothProfile.STATE_CONNECTED;

    }

    boolean isBusy() {
        for (LocalBluetoothProfile profile : mProfiles) {
            int status = getProfileConnectionState(profile);
            if (status == BluetoothProfile.STATE_CONNECTING
                    || status == BluetoothProfile.STATE_DISCONNECTING) {
                return true;
            }
        }
        return getBondState() == BluetoothDevice.BOND_BONDING;
    }

    /**
     * Fetches a new value for the cached BT class.
     */
    private void fetchBtClass() {
        mBtClass = mDevice.getBluetoothClass();
    }

    private boolean updateProfiles() {
        ParcelUuid[] uuids = mDevice.getUuids();
        if (uuids == null) return false;

        ParcelUuid[] localUuids = mLocalAdapter.getUuids();
        if (localUuids == null) return false;

        mProfileManager.updateProfiles(uuids, localUuids, mProfiles, mRemovedProfiles,
                                       mLocalNapRoleConnected, mDevice);

        if (DEBUG) {
            Log.e(TAG, "updating profiles for " + mDevice.getAliasName());
            BluetoothClass bluetoothClass = mDevice.getBluetoothClass();

            if (bluetoothClass != null) Log.v(TAG, "Class: " + bluetoothClass.toString());
            Log.v(TAG, "UUID:");
            for (ParcelUuid uuid : uuids) {
                Log.v(TAG, "  " + uuid);
            }
        }
        return true;
    }

    /**
     * Refreshes the UI for the BT class, including fetching the latest value
     * for the class.
     */
    void refreshBtClass() {
        fetchBtClass();
        dispatchAttributesChanged();
    }

    /**
     * Refreshes the UI when framework alerts us of a UUID change.
     */
    void onUuidChanged() {
        updateProfiles();
        ParcelUuid[] uuids = mDevice.getUuids();
        long timeout = MAX_UUID_DELAY_FOR_AUTO_CONNECT;
        Log.d(TAG, "onUuidChanged: Time since last connect"
                    + (SystemClock.elapsedRealtime() - mConnectAttempted));

        /*
         * If a connect was attempted earlier without any UUID, we will do the
         * connect now.
         */
        if(BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.Hogp))
        {
            timeout = MAX_HOGP_DELAY_FOR_AUTO_CONNECT;
        }
        Log.d(TAG, "onUuidChanged timeout value="+timeout);
        if (!mProfiles.isEmpty()
                && (mConnectAttempted + timeout) > SystemClock
                        .elapsedRealtime()) {
            connectWithoutResettingTimer(false);
        }
        dispatchAttributesChanged();
    }

    void onBondingStateChanged(int bondState) {

        if(DEBUG) Log.d(TAG, "onBondingStateChanged" + bondState);

        switch (bondState) {
            case BluetoothDevice.BOND_NONE:
                mProfiles.clear();
                mConnectAfterPairing = false;  // cancel auto-connect
                // fall through

            case BluetoothDevice.BOND_BONDING:
                //Sometimes Remote device is unpaired by itself & try to connect again.
                //so permission should be reset for that particular device.
                setPhonebookPermissionChoice(ACCESS_UNKNOWN);
                setMessagePermissionChoice(ACCESS_UNKNOWN);
                mPhonebookRejectedTimes = 0;
                savePhonebookRejectTimes();
                mMessageRejectedTimes = 0;
                saveMessageRejectTimes();

                refresh();
                break;

            case BluetoothDevice.BOND_BONDED:
                if (mDevice.isBluetoothDock()) {
                    onBondingDockConnect();
                } else if (mConnectAfterPairing) {
                    connect(false);
                }
                mConnectAfterPairing = false;
                break;

            default:
                Log.e(TAG, "Incorrect Bond State received");
        }
    }

    void setBtClass(BluetoothClass btClass) {
        if (btClass != null && mBtClass != btClass) {
            mBtClass = btClass;
            dispatchAttributesChanged();
        }
    }

    BluetoothClass getBtClass() {
        return mBtClass;
    }

    List<LocalBluetoothProfile> getProfiles() {
        return Collections.unmodifiableList(mProfiles);
    }

    List<LocalBluetoothProfile> getConnectableProfiles() {
        List<LocalBluetoothProfile> connectableProfiles =
                new ArrayList<LocalBluetoothProfile>();
        for (LocalBluetoothProfile profile : mProfiles) {
            if (profile.isConnectable()) {
                connectableProfiles.add(profile);
            }
        }
        return connectableProfiles;
    }

    List<LocalBluetoothProfile> getRemovedProfiles() {
        return mRemovedProfiles;
    }

    void registerCallback(Callback callback) {
        synchronized (mCallbacks) {
            mCallbacks.add(callback);
        }
    }

    void unregisterCallback(Callback callback) {
        synchronized (mCallbacks) {
            mCallbacks.remove(callback);
        }
    }

    private void dispatchAttributesChanged() {
        synchronized (mCallbacks) {
            for (Callback callback : mCallbacks) {
                callback.onDeviceAttributesChanged();
            }
        }
    }

    @Override
    public String toString() {
        return mDevice.toString();
    }

    @Override
    public boolean equals(Object o) {
        if ((o == null) || !(o instanceof CachedBluetoothDevice)) {
            return false;
        }
        return mDevice.equals(((CachedBluetoothDevice) o).mDevice);
    }

    @Override
    public int hashCode() {
        return mDevice.getAddress().hashCode();
    }

    // This comparison uses non-final fields so the sort order may change
    // when device attributes change (such as bonding state). Settings
    // will completely refresh the device list when this happens.
    public int compareTo(CachedBluetoothDevice another) {
        // Connected above not connected
        int comparison = (another.isConnected() ? 1 : 0) - (isConnected() ? 1 : 0);
        if (comparison != 0) return comparison;

        // Paired above not paired
        comparison = (another.getBondState() == BluetoothDevice.BOND_BONDED ? 1 : 0) -
            (getBondState() == BluetoothDevice.BOND_BONDED ? 1 : 0);
        if (comparison != 0) return comparison;

        // Visible above not visible
        comparison = (another.mVisible ? 1 : 0) - (mVisible ? 1 : 0);
        if (comparison != 0) return comparison;

        // Stronger signal above weaker signal
        comparison = another.mRssi - mRssi;
        if (comparison != 0) return comparison;

        // Fallback on name
        return mName.compareTo(another.mName);
    }

    public interface Callback {
        void onDeviceAttributesChanged();
    }

    int getPhonebookPermissionChoice() {
        return mPhonebookPermissionChoice;
    }

    void setPhonebookPermissionChoice(int permissionChoice) {
        // if user reject it, only save it when reject exceed limit.
        if (permissionChoice == ACCESS_REJECTED) {
            mPhonebookRejectedTimes++;
            savePhonebookRejectTimes();
            if (mPhonebookRejectedTimes < PERSIST_REJECTED_TIMES_LIMIT) {
                return;
            }
        }

        mPhonebookPermissionChoice = permissionChoice;

        SharedPreferences.Editor editor =
            mContext.getSharedPreferences(PHONEBOOK_PREFS_NAME, Context.MODE_PRIVATE).edit();
        if (permissionChoice == ACCESS_UNKNOWN) {
            editor.remove(mDevice.getAddress());
        } else {
            editor.putInt(mDevice.getAddress(), permissionChoice);
        }
        editor.commit();
    }

    private void fetchPhonebookPermissionChoice() {
        SharedPreferences preference = mContext.getSharedPreferences(PHONEBOOK_PREFS_NAME,
                                                                     Context.MODE_PRIVATE);
        mPhonebookPermissionChoice = preference.getInt(mDevice.getAddress(),
                                                       ACCESS_UNKNOWN);
    }

    private void fetchPhonebookRejectTimes() {
        SharedPreferences preference = mContext.getSharedPreferences(PHONEBOOK_REJECT_TIMES,
                                                                     Context.MODE_PRIVATE);
        mPhonebookRejectedTimes = preference.getInt(mDevice.getAddress(), 0);
    }

    private void savePhonebookRejectTimes() {
        SharedPreferences.Editor editor =
            mContext.getSharedPreferences(PHONEBOOK_REJECT_TIMES,
                                          Context.MODE_PRIVATE).edit();
        if (mPhonebookRejectedTimes == 0) {
            editor.remove(mDevice.getAddress());
        } else {
            editor.putInt(mDevice.getAddress(), mPhonebookRejectedTimes);
        }
        editor.commit();
    }

    int getMessagePermissionChoice() {
        return mMessagePermissionChoice;
    }

    void setMessagePermissionChoice(int permissionChoice) {
        // if user reject it, only save it when reject exceed limit.
        if (permissionChoice == ACCESS_REJECTED) {
            mMessageRejectedTimes++;
            saveMessageRejectTimes();
            if (mMessageRejectedTimes < PERSIST_REJECTED_TIMES_LIMIT) {
                return;
            }
        }

        mMessagePermissionChoice = permissionChoice;

        SharedPreferences.Editor editor =
            mContext.getSharedPreferences(MESSAGE_PREFS_NAME, Context.MODE_PRIVATE).edit();
        if (permissionChoice == ACCESS_UNKNOWN) {
            editor.remove(mDevice.getAddress());
        } else {
            editor.putInt(mDevice.getAddress(), permissionChoice);
        }
        editor.commit();
    }

    private void fetchMessagePermissionChoice() {
        SharedPreferences preference = mContext.getSharedPreferences(MESSAGE_PREFS_NAME,
                                                                     Context.MODE_PRIVATE);
        mMessagePermissionChoice = preference.getInt(mDevice.getAddress(),
                                                       ACCESS_UNKNOWN);
    }

    private void fetchMessageRejectTimes() {
        SharedPreferences preference = mContext.getSharedPreferences(MESSAGE_REJECT_TIMES,
                                                                     Context.MODE_PRIVATE);
        mMessageRejectedTimes = preference.getInt(mDevice.getAddress(), 0);
    }

    private void saveMessageRejectTimes() {
        SharedPreferences.Editor editor =
            mContext.getSharedPreferences(MESSAGE_REJECT_TIMES, Context.MODE_PRIVATE).edit();
        if (mMessageRejectedTimes == 0) {
            editor.remove(mDevice.getAddress());
        } else {
            editor.putInt(mDevice.getAddress(), mMessageRejectedTimes);
        }
        editor.commit();
    }

}
