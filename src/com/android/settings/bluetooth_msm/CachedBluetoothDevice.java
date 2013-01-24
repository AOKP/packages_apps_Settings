/*
 * Copyright (C) 2008 The Android Open Source Project
 * Copyright (C) 2012, The Linux Foundation. All rights reserved.
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

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.ParcelUuid;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

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

    private final Collection<Callback> mCallbacks = new ArrayList<Callback>();

    // Following constants indicate the user's choices of Phone book access settings
    // User hasn't made any choice or settings app has wiped out the memory
    final static int PHONEBOOK_ACCESS_UNKNOWN = 0;
    // User has accepted the connection and let Settings app remember the decision
    final static int PHONEBOOK_ACCESS_ALLOWED = 1;
    // User has rejected the connection and let Settings app remember the decision
    final static int PHONEBOOK_ACCESS_REJECTED = 2;

    private final static String PHONEBOOK_PREFS_NAME = "bluetooth_phonebook_permission";

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
        } else if (mLocalNapRoleConnected && profile instanceof PanProfile &&
                ((PanProfile) profile).isLocalRoleNap(mDevice) &&
                newProfileState == BluetoothProfile.STATE_DISCONNECTED) {
            Log.d(TAG, "Removing PanProfile from device after NAP disconnect");
            mProfiles.remove(profile);
            mRemovedProfiles.add(profile);
            mLocalNapRoleConnected = false;
        } else if ((profile instanceof SapProfile || profile instanceof DUNProfile) &&
            newProfileState == BluetoothProfile.STATE_DISCONNECTED) {
            mProfiles.remove(profile);
            mRemovedProfiles.add(profile);
            Log.d(TAG, "Removed Profile from the list");
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
    }

    void disconnect(LocalBluetoothProfile profile) {
        if (profile.disconnect(mDevice)) {
            if (Utils.D) {
                Log.d(TAG, "Command sent successfully:DISCONNECT " + describe(profile));
            }
        }
    }

    public void resetAllServerProfiles() {
        for (LocalBluetoothProfile profile : mProfiles) {
            if (profile instanceof SapProfile) {
                ((SapProfile)profile).setConnectionStatus(BluetoothProfile.STATE_DISCONNECTED);
            }
            if (profile instanceof DUNProfile) {
                ((DUNProfile)profile).setConnectionStatus(BluetoothProfile.STATE_DISCONNECTED);
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
            if (!updateProfiles()) {
                // If UUIDs are not available yet, connect will be happen
                // upon arrival of the ACTION_UUID intent.
                if (DEBUG) Log.d(TAG, "No profiles. Maybe we will connect later");
                return;
            }
        }

        // Reset the only-show-one-error-dialog tracking variable
        mIsConnectingErrorPossible = true;

        int preferredProfiles = 0;
        if ((connectAllProfiles == false) &&
            (mLocalAdapter.isHostPatchRequired(mDevice,
             BluetoothAdapter.HOST_PATCH_AVOID_CONNECT_ON_PAIR))) {
             //Depending on class of device restricting the connection only to Carkit.
             BluetoothClass btClass = mDevice.getBluetoothClass();
             if (btClass.getDeviceClass() == BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE) {
                 Log.d(TAG, "No connection expected with current device");
                 return;
             }
        }
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
    }

    private void connectInt(LocalBluetoothProfile profile) {
        if (!ensurePaired()) {
            return;
        } else {
            // connecting is unreliable while scanning, so cancel discovery
            if (mLocalAdapter == null) {
                Log.e(TAG, "Adapter is null");
                return;
            }
            if (mLocalAdapter.isDiscovering()) {
                mLocalAdapter.cancelDiscovery();
            }
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
        disconnect();

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

    // TODO: do any of these need to run async on a background thread?
    private void fillData() {
        fetchName();
        fetchBtClass();
        updateProfiles();
        fetchPhonebookPermissionChoice();

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

        if (mProfileManager == null) {
            Log.e(TAG, "ProfileManager is null");
            return false;
        }
        boolean isSpecialMappingDev = mLocalAdapter.isHostPatchRequired(mDevice,
                                   BluetoothAdapter.HOST_PATCH_DONT_REMOVE_SERVICE);

        if (!isSpecialMappingDev) {
            mProfileManager.updateProfiles(uuids, localUuids, mProfiles, mRemovedProfiles);
        } else {
            mProfileManager.addNewProfiles(uuids, localUuids, mProfiles, mRemovedProfiles);
        }

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

        if (DEBUG) {
            Log.e(TAG, "onUuidChanged: Time since last connect"
                    + (SystemClock.elapsedRealtime() - mConnectAttempted));
        }

        /*
         * If a connect was attempted earlier without any UUID, we will do the
         * connect now.
         */
        if (!mProfiles.isEmpty()
                && (mConnectAttempted + MAX_UUID_DELAY_FOR_AUTO_CONNECT) > SystemClock
                        .elapsedRealtime()) {
            connectWithoutResettingTimer(false);
        }
        dispatchAttributesChanged();
    }

    void onBondingStateChanged(int bondState) {
        if (bondState == BluetoothDevice.BOND_NONE) {
            mProfiles.clear();
            mConnectAfterPairing = false;  // cancel auto-connect
            setPhonebookPermissionChoice(PHONEBOOK_ACCESS_UNKNOWN);
        }

        if (bondState == BluetoothDevice.BOND_BONDED) {
            fetchName();
        }
        refresh();

        if (bondState == BluetoothDevice.BOND_BONDED) {
            if (mDevice.isBluetoothDock()) {
                onBondingDockConnect();
            } else if (mConnectAfterPairing) {
                connect(false);
            }
            mConnectAfterPairing = false;
        }

        if (bondState == BluetoothDevice.BOND_RETRY) {
            Log.i(TAG, "Bond state is Retry, set autoconnect");
            mConnectAfterPairing = true;
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
        SharedPreferences.Editor editor =
            mContext.getSharedPreferences(PHONEBOOK_PREFS_NAME, Context.MODE_PRIVATE).edit();
        if (permissionChoice == PHONEBOOK_ACCESS_UNKNOWN) {
            editor.remove(mDevice.getAddress());
        } else {
            editor.putInt(mDevice.getAddress(), permissionChoice);
        }
        editor.commit();
        mPhonebookPermissionChoice = permissionChoice;
    }

    private void fetchPhonebookPermissionChoice() {
        SharedPreferences preference = mContext.getSharedPreferences(PHONEBOOK_PREFS_NAME,
                                                                     Context.MODE_PRIVATE);
        mPhonebookPermissionChoice = preference.getInt(mDevice.getAddress(),
                                                       PHONEBOOK_ACCESS_UNKNOWN);
    }

}
