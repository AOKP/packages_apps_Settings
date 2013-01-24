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

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothUuid;
import android.content.Context;
import android.os.ParcelUuid;

import com.android.settings.R;

import java.util.List;

/**
 * A2dpProfile handles Bluetooth A2DP.
 * TODO: add null checks around calls to mService object.
 */
final class A2dpProfile implements LocalBluetoothProfile {
    private BluetoothA2dp mService;

    static final ParcelUuid[] SINK_UUIDS = {
        BluetoothUuid.AudioSink,
        BluetoothUuid.AdvAudioDist,
    };

    static final String NAME = "A2DP";

    // Order of this profile in device profiles list
    private static final int ORDINAL = 1;

    // These callbacks run on the main thread.
    private final class A2dpServiceListener
            implements BluetoothProfile.ServiceListener {

        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            mService = (BluetoothA2dp) proxy;
        }

        public void onServiceDisconnected(int profile) {
            mService = null;
        }
    }

    A2dpProfile(Context context) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        adapter.getProfileProxy(context, new A2dpServiceListener(),
                BluetoothProfile.A2DP);
    }

    public boolean isConnectable() {
        return true;
    }

    public boolean isAutoConnectable() {
        return true;
    }

    private List<BluetoothDevice> getConnectedDevices() {
        return mService.getDevicesMatchingConnectionStates(
              new int[] {BluetoothProfile.STATE_CONNECTED,
                         BluetoothProfile.STATE_CONNECTING,
                         BluetoothProfile.STATE_DISCONNECTING});
    }

    public boolean connect(BluetoothDevice device) {
        if (mService.getConnectionState(device) == BluetoothProfile.STATE_CONNECTED)
            return true;

        List<BluetoothDevice> sinks = getConnectedDevices();
        if (sinks != null) {
            for (BluetoothDevice sink : sinks) {
                mService.disconnect(sink);
            }
        }
        return mService.connect(device);
    }

    public boolean disconnect(BluetoothDevice device) {
        return mService.disconnect(device);
    }

    public int getConnectionStatus(BluetoothDevice device) {
        return mService.getConnectionState(device);
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

    boolean isA2dpPlaying() {
        List<BluetoothDevice> sinks = mService.getConnectedDevices();
        if (!sinks.isEmpty()) {
            if (mService.isA2dpPlaying(sinks.get(0))) {
                return true;
            }
        }
        return false;
    }

    public boolean isProfileReady() {
        return mService != null;
    }

    public String toString() {
        return NAME;
    }

    public int getOrdinal() {
        return ORDINAL;
    }

    public int getNameResource(BluetoothDevice device) {
        return R.string.bluetooth_profile_a2dp;
    }

    public int getSummaryResourceForDevice(BluetoothDevice device) {
        int state = mService.getConnectionState(device);
        switch (state) {
            case BluetoothProfile.STATE_DISCONNECTED:
                return R.string.bluetooth_a2dp_profile_summary_use_for;

            case BluetoothProfile.STATE_CONNECTED:
                return R.string.bluetooth_a2dp_profile_summary_connected;

            default:
                return Utils.getConnectionStateSummary(state);
        }
    }

    public int getDrawableResource(BluetoothClass btClass) {
        return R.drawable.ic_bt_headphones_a2dp;
    }
}
