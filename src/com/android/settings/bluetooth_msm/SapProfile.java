/*
 ** Copyright 2008, The Android Open Source Project
 ** Copyright (c) 2011, The Linux Foundation. All rights reserved.
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at
 **
 **     http://www.apache.org/licenses/LICENSE-2.0
 **
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ** See the License for the specific language governing permissions and
 ** limitations under the License.
 */

package com.android.settings.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSap;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import com.android.settings.R;

import java.util.HashMap;
import java.util.List;

/**
 * SapProfile handles Bluetooth SAP profile.
 */
final class SapProfile implements LocalBluetoothProfile {
    private BluetoothSap mService;
    private int mConnectionStatus = 0;

    // Tethering direction for each device
    private final HashMap<BluetoothDevice, Integer> mDeviceRoleMap =
            new HashMap<BluetoothDevice, Integer>();

    static final String NAME = "SAP";

    // Order of this profile in device profiles list
    private static final int ORDINAL = 5;

    SapProfile() {
        mService = new BluetoothSap();
    }

    public boolean isConnectable() {
        return true;
    }

    public boolean isAutoConnectable() {
        return false;
    }

    public boolean connect(BluetoothDevice device) {
        return false;
    }

    public boolean disconnect(BluetoothDevice device) {
        boolean ret = mService.disconnect();
        return ret;
    }

    public int getConnectionStatus(BluetoothDevice device) {
        return mConnectionStatus;
    }

    public void setConnectionStatus(int status) {
        mConnectionStatus = status;
    }

    public boolean isPreferred(BluetoothDevice device) {
        return true;
    }

    public int getPreferred(BluetoothDevice device) {
        return -1;
    }

    public void setPreferred(BluetoothDevice device, boolean preferred) {
        // ignore: isPreferred is always true for SAP
    }

    public boolean isProfileReady() {
        return true;
    }

    public String toString() {
        return NAME;
    }

    public int getOrdinal() {
        return ORDINAL;
    }

    public int getNameResource(BluetoothDevice device) {
        return R.string.bluetooth_profile_sap;
    }

    public int getSummaryResourceForDevice(BluetoothDevice device) {
        int state = getConnectionStatus(device);
        switch (state) {
            case BluetoothProfile.STATE_DISCONNECTED:
                return R.string.bluetooth_sap_profile_summary_use_for;

            case BluetoothProfile.STATE_CONNECTED:
                    return R.string.bluetooth_sap_profile_summary_connected;

            default:
                return Utils.getConnectionStateSummary(state);
        }
    }

    public int getDrawableResource(BluetoothClass btClass) {
        //TODO change this for SAP
        return R.drawable.ic_bt_network_pan;
    }

}
