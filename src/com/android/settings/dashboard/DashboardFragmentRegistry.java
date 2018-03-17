/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.dashboard;

import android.util.ArrayMap;

import com.android.settings.DisplaySettings;
import com.android.settings.SecuritySettings;
import com.android.settings.accounts.AccountDetailDashboardFragment;
import com.android.settings.accounts.UserAndAccountDashboardFragment;
import com.android.settings.applications.AppAndNotificationDashboardFragment;
import com.android.settings.applications.DefaultAppSettings;
import com.android.settings.connecteddevice.ConnectedDeviceDashboardFragment;
import com.android.settings.development.DevelopmentSettings;
import com.android.settings.deviceinfo.StorageDashboardFragment;
import com.android.settings.fuelgauge.PowerUsageSummary;
import com.android.settings.language.LanguageAndInputSettings;
import com.android.settings.network.NetworkDashboardFragment;
import com.android.settings.notification.ConfigureNotificationSettings;
import com.android.settings.notification.SoundSettings;
import com.android.settings.security.LockscreenDashboardFragment;
import com.android.settings.system.SystemDashboardFragment;
import com.android.settingslib.drawer.CategoryKey;

import com.aokp.romcontrol.MainActivity;

import com.android.settings.du.FlingSettings;
import com.android.settings.du.NavbarSettings;
import com.android.settings.du.PulseSettings;
import com.android.settings.du.SmartbarSettings;

import java.util.Map;

/**
 * A registry to keep track of which page hosts which category.
 */
public class DashboardFragmentRegistry {

    /**
     * Map from parent fragment to category key. The parent fragment hosts child with
     * category_key.
     */
    public static final Map<String, String> PARENT_TO_CATEGORY_KEY_MAP;

    /**
     * Map from category_key to parent. This is a helper to look up which fragment hosts the
     * category_key.
     */
    public static final Map<String, String> CATEGORY_KEY_TO_PARENT_MAP;

    static {
        PARENT_TO_CATEGORY_KEY_MAP = new ArrayMap<>();
        PARENT_TO_CATEGORY_KEY_MAP.put(
                NetworkDashboardFragment.class.getName(), CategoryKey.CATEGORY_NETWORK);
        PARENT_TO_CATEGORY_KEY_MAP.put(ConnectedDeviceDashboardFragment.class.getName(),
                CategoryKey.CATEGORY_DEVICE);
        PARENT_TO_CATEGORY_KEY_MAP.put(AppAndNotificationDashboardFragment.class.getName(),
                CategoryKey.CATEGORY_APPS);
        PARENT_TO_CATEGORY_KEY_MAP.put(PowerUsageSummary.class.getName(),
                CategoryKey.CATEGORY_BATTERY);
        PARENT_TO_CATEGORY_KEY_MAP.put(DefaultAppSettings.class.getName(),
                CategoryKey.CATEGORY_APPS_DEFAULT);
        PARENT_TO_CATEGORY_KEY_MAP.put(DisplaySettings.class.getName(),
                CategoryKey.CATEGORY_DISPLAY);
        PARENT_TO_CATEGORY_KEY_MAP.put(SoundSettings.class.getName(),
                CategoryKey.CATEGORY_SOUND);
        PARENT_TO_CATEGORY_KEY_MAP.put(StorageDashboardFragment.class.getName(),
                CategoryKey.CATEGORY_STORAGE);
        PARENT_TO_CATEGORY_KEY_MAP.put(SecuritySettings.class.getName(),
                CategoryKey.CATEGORY_SECURITY);
        PARENT_TO_CATEGORY_KEY_MAP.put(AccountDetailDashboardFragment.class.getName(),
                CategoryKey.CATEGORY_ACCOUNT_DETAIL);
        PARENT_TO_CATEGORY_KEY_MAP.put(UserAndAccountDashboardFragment.class.getName(),
                CategoryKey.CATEGORY_ACCOUNT);
        PARENT_TO_CATEGORY_KEY_MAP.put(
                SystemDashboardFragment.class.getName(), CategoryKey.CATEGORY_SYSTEM);
        PARENT_TO_CATEGORY_KEY_MAP.put(LanguageAndInputSettings.class.getName(),
                CategoryKey.CATEGORY_SYSTEM_LANGUAGE);
        PARENT_TO_CATEGORY_KEY_MAP.put(DevelopmentSettings.class.getName(),
                CategoryKey.CATEGORY_SYSTEM_DEVELOPMENT);
        PARENT_TO_CATEGORY_KEY_MAP.put(ConfigureNotificationSettings.class.getName(),
                CategoryKey.CATEGORY_NOTIFICATIONS);
        PARENT_TO_CATEGORY_KEY_MAP.put(LockscreenDashboardFragment.class.getName(),
                CategoryKey.CATEGORY_SECURITY_LOCKSCREEN);
        PARENT_TO_CATEGORY_KEY_MAP.put(MainActivity.class.getName(),
                CategoryKey.CATEGORY_SYSTEM_DEVELOPMENT);
        PARENT_TO_CATEGORY_KEY_MAP.put(NavbarSettings.class.getName(),
                CategoryKey.CATEGORY_SYSTEM_DEVELOPMENT);

        CATEGORY_KEY_TO_PARENT_MAP = new ArrayMap<>(PARENT_TO_CATEGORY_KEY_MAP.size());

        for (Map.Entry<String, String> parentToKey : PARENT_TO_CATEGORY_KEY_MAP.entrySet()) {
            CATEGORY_KEY_TO_PARENT_MAP.put(parentToKey.getValue(), parentToKey.getKey());
        }
    }
}
