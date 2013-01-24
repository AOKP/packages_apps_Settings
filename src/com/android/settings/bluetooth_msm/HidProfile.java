/*
 * Copyright (C) 2011 The Android Open Source Project
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
import android.bluetooth.BluetoothInputDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

import com.android.settings.R;

import java.util.List;

/**
 * HidProfile handles Bluetooth HID profile.
 */
final class HidProfile implements LocalBluetoothProfile {
    private BluetoothInputDevice mService;
    private boolean mProfileReady;

    static final String NAME = "HID";

    // Order of this profile in device profiles list
    private static final int ORDINAL = 3;

    // These callbacks run on the main thread.
    private final class InputDeviceServiceListener
            implements BluetoothProfile.ServiceListener {

        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            mService = (BluetoothInputDevice) proxy;
            mProfileReady = true;
        }

        public void onServiceDisconnected(int profile) {
            mProfileReady = false;
            mService = null;
        }
    }

    HidProfile(Context context, LocalBluetoothAdapter adapter) {
        adapter.getProfileProxy(context, new InputDeviceServiceListener(),
                BluetoothProfile.INPUT_DEVICE);
    }

    public boolean isConnectable() {
        return true;
    }

    public boolean isAutoConnectable() {
        return true;
    }

    public boolean connect(BluetoothDevice device) {
        return mService.connect(device);
    }

    public boolean disconnect(BluetoothDevice device) {
        return mService.disconnect(device);
    }

    public int getConnectionStatus(BluetoothDevice device) {
        List<BluetoothDevice> deviceList = mService.getConnectedDevices();

        return !deviceList.isEmpty() && deviceList.get(0).equals(device)
                ? mService.getConnectionState(device)
                : BluetoothProfile.STATE_DISCONNECTED;
    }

    public boolean isPreferred(BluetoothDevice device) {
        return mService.getPriority(device) > BluetoothProfile.PRIORITY_OFF;
    }

    public int getPreferred(BluetoothDevice device) {
        return mService.getPriority(device);
    }

    public void setPreferred(BluetoothDevice device, boolean preferred) {
        if (preferred) {
            if (mService.getPriority(device) < BluetoothProfile.PRIORITY_ON) {
                mService.setPriority(device, BluetoothProfile.PRIORITY_ON);
            }
        } else {
            mService.setPriority(device, BluetoothProfile.PRIORITY_OFF);
        }
    }

    public boolean isProfileReady() {
        return mProfileReady;
    }

    public String toString() {
        return NAME;
    }

    public int getOrdinal() {
        return ORDINAL;
    }

    public int getNameResource(BluetoothDevice device) {
        // TODO: distinguish between keyboard and mouse?
        return R.string.bluetooth_profile_hid;
    }

    public int getSummaryResourceForDevice(BluetoothDevice device) {
        int state = mService.getConnectionState(device);
        switch (state) {
            case BluetoothProfile.STATE_DISCONNECTED:
                return R.string.bluetooth_hid_profile_summary_use_for;

            case BluetoothProfile.STATE_CONNECTED:
                return R.string.bluetooth_hid_profile_summary_connected;

            default:
                return Utils.getConnectionStateSummary(state);
        }
    }

    public int getDrawableResource(BluetoothClass btClass) {
        if (btClass == null) {
            return R.drawable.ic_bt_keyboard_hid;
        }
        return getHidClassDrawable(btClass);
    }

    static int getHidClassDrawable(BluetoothClass btClass) {
        switch (btClass.getDeviceClass()) {
            case BluetoothClass.Device.PERIPHERAL_KEYBOARD:
            case BluetoothClass.Device.PERIPHERAL_KEYBOARD_POINTING:
                return R.drawable.ic_bt_keyboard_hid;
            case BluetoothClass.Device.PERIPHERAL_POINTING:
                return R.drawable.ic_bt_pointing_hid;
            default:
                return R.drawable.ic_bt_misc_hid;
        }
    }
}
