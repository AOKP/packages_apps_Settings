/*
 * Copyright (C) 2013 SlimRoms
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

package com.android.settings.slim.themes;

import android.content.Context;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.android.settings.R;

public class ThemeEnabler implements CompoundButton.OnCheckedChangeListener {
    private final Context mContext;
    private Switch mSwitch;
    private boolean mStateMachineEvent;
    private boolean mEnabled;

    private boolean mAttached;
    private SettingsObserver mSettingsObserver;

    private final class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            mContext.getContentResolver().registerContentObserver(
                    Settings.AOKP.getUriFor(
                    Settings.AOKP.UI_THEME_AUTO_MODE),
                    false, this);
        }

        @Override
        public void onChange(boolean selfChange) {
            setSwitchState();
        }
    }

    public ThemeEnabler(Context context, Switch switch_) {
        mContext = context;
        mSwitch = switch_;
        mSettingsObserver = new SettingsObserver(new Handler());
    }

    public void resume() {
        mSwitch.setOnCheckedChangeListener(this);
        if (!mAttached) {
            mAttached = true;
            mSettingsObserver.observe();
        }
        setSwitchState();
    }

    public void pause() {
        mSwitch.setOnCheckedChangeListener(null);
        if (mAttached) {
            mAttached = false;
            mContext.getContentResolver().unregisterContentObserver(mSettingsObserver);
        }
    }

    public void setSwitch(Switch switch_) {
        if (mSwitch == switch_) return;
        mSwitch.setOnCheckedChangeListener(null);
        mSwitch = switch_;
        mSwitch.setOnCheckedChangeListener(this);
        setSwitchState();
    }

    public void setSwitchState() {
        mEnabled = Settings.AOKP.getIntForUser(mContext.getContentResolver(),
                Settings.AOKP.UI_THEME_AUTO_MODE, 0,
                UserHandle.USER_CURRENT) != 1;

        boolean state = mContext.getResources().getConfiguration().uiThemeMode
                    == Configuration.UI_THEME_MODE_HOLO_DARK;
        mStateMachineEvent = true;
        mSwitch.setChecked(state);
        mStateMachineEvent = false;
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (mStateMachineEvent) {
            return;
        }
        if (!mEnabled) {
            Toast.makeText(mContext, R.string.theme_auto_switch_mode_error,
                    Toast.LENGTH_SHORT).show();
            setSwitchState();
            return;
        }
        // Handle a switch change
        // we currently switch between holodark and hololight till either
        // theme engine is ready or lightheme is ready. Currently due of
        // missing light themeing hololight = system base theme
        Settings.AOKP.putIntForUser(mContext.getContentResolver(),
                Settings.AOKP.UI_THEME_MODE, isChecked
                    ? Configuration.UI_THEME_MODE_HOLO_DARK
                    : Configuration.UI_THEME_MODE_HOLO_LIGHT,
                UserHandle.USER_CURRENT);
    }

}
