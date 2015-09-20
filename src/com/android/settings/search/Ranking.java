/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.search;

import com.android.settings.ChooseLockGeneric;
import com.android.settings.DataUsageSummary;
import com.android.settings.DateTimeSettings;
import com.android.settings.DevelopmentSettings;
import com.android.settings.DeviceInfoSettings;
import com.android.settings.DisplaySettings;
import com.android.settings.HomeSettings;
import com.android.settings.ScreenPinningSettings;
import com.android.settings.PrivacySettings;
import com.android.settings.SecuritySettings;
import com.android.settings.WirelessSettings;
import com.android.settings.accessibility.AccessibilitySettings;
import com.android.settings.aicp.tabs.AddOns;
import com.android.settings.aicp.tabs.BuiltIn;
import com.android.settings.aicp.tabs.Ui;
import com.android.settings.aicp.AmbientSettings;
import com.android.settings.aicp.AnimationControls;
import com.android.settings.aicp.AppCircleBar;
import com.android.settings.aicp.AppSidebar;
import com.android.settings.aicp.BatterySettings;
import com.android.settings.aicp.BreathingNotifications;
import com.android.settings.aicp.CarrierLabel;
import com.android.settings.aicp.DisplayAnimationsSettings;
import com.android.settings.aicp.HeadsUpSettings;
import com.android.settings.aicp.LockScreenColorSettings;
import com.android.settings.aicp.LockScreenWeatherSettings;
import com.android.settings.aicp.NavBarDimensions;
import com.android.settings.aicp.NetworkTrafficFragment;
import com.android.settings.aicp.OmniSwitch;
import com.android.settings.aicp.OverscrollEffects;
import com.android.settings.aicp.PieButtonStyleSettings;
import com.android.settings.aicp.PieControl;
import com.android.settings.aicp.PieStyleSettings;
import com.android.settings.aicp.PieTriggerSettings;
import com.android.settings.aicp.QSColors;
import com.android.settings.aicp.RecentsPanelSettings;
import com.android.settings.aicp.StatusBarWeather;
import com.android.settings.aicp.VariousShit;
import com.android.settings.aicp.VolumeSteps;
import com.android.settings.aicp.gestureanywhere.GestureAnywhereSettings;
import com.android.settings.bluetooth.BluetoothSettings;
import com.android.settings.deviceinfo.Memory;
import com.android.settings.deviceinfo.UsbSettings;
import com.android.settings.fuelgauge.BatterySaverSettings;
import com.android.settings.fuelgauge.PowerUsageSummary;
import com.android.settings.inputmethod.InputMethodAndLanguageSettings;
import com.android.settings.location.LocationSettings;
import com.android.settings.net.DataUsageMeteredSettings;
import com.android.settings.notification.NotificationSettings;
import com.android.settings.notification.OtherSoundSettings;
import com.android.settings.notification.ZenModeSettings;
import com.android.settings.print.PrintSettingsFragment;
import com.android.settings.sim.SimSettings;
import com.android.settings.slim.SoundSettings;
import com.android.settings.users.UserSettings;
import com.android.settings.voice.VoiceInputSettings;
import com.android.settings.wifi.AdvancedWifiSettings;
import com.android.settings.wifi.SavedAccessPointsWifiSettings;
import com.android.settings.wifi.WifiSettings;

import java.util.HashMap;

/**
 * Utility class for dealing with Search Ranking.
 */
public final class Ranking {

    public static final int RANK_WIFI = 1;
    public static final int RANK_BT = 2;
    public static final int RANK_SIM = 3;
    public static final int RANK_DATA_USAGE = 4;
    public static final int RANK_WIRELESS = 5;
    public static final int RANK_AICP_BUILTIN = 6;
    public static final int RANK_AICP_ADDONS = 7;
    public static final int RANK_AICP_UI = 8;
    public static final int RANK_VARIOUSSHIT = 9;
    public static final int RANK_BATTERYSETTINGS = 10;
    public static final int RANK_HEADSUP = 11;
    public static final int RANK_AMBIENT = 12;
    public static final int RANK_OMNISWITCH = 13;
    public static final int RANK_RECENTSPANEL = 14;
    public static final int RANK_BREATHINGNOTIF = 15;
    public static final int RANK_CARRIERLABEL = 16;
    public static final int RANK_GESTUREANYWHERE = 17;
    public static final int RANK_LSCOLOR = 18;
    public static final int RANK_QSCOLOR = 19;
    public static final int RANK_LSWEATHER = 20;
    public static final int RANK_STATUSBARWEATHER = 21;
    public static final int RANK_APPCIRCLEBAR = 22;
    public static final int RANK_APPSIDEBAR = 23;
    public static final int RANK_PIESTYLE = 24;
    public static final int RANK_PIECONTROL = 25;
    public static final int RANK_PIEBUTTON = 26;
    public static final int RANK_PIETRIGGER = 27;
    public static final int RANK_NAVBARDIM = 28;
    public static final int RANK_NETTRAFFIC = 29;
    public static final int RANK_VOLSTEPS = 30;
    public static final int RANK_ADVANCEDSOUNDS = 31;
    public static final int RANK_ANIMATIONCONTROLS = 32;
    public static final int RANK_DISPLAYANIMS = 33;
    public static final int RANK_OVERSCROLL = 34;
    public static final int RANK_HOME = 35;
    public static final int RANK_DISPLAY = 36;
    public static final int RANK_NOTIFICATIONS = 37;
    public static final int RANK_STORAGE = 38;
    public static final int RANK_POWER_USAGE = 39;
    public static final int RANK_USERS = 40;
    public static final int RANK_LOCATION = 41;
    public static final int RANK_SECURITY = 42;
    public static final int RANK_IME = 43;
    public static final int RANK_PRIVACY = 44;
    public static final int RANK_DATE_TIME = 45;
    public static final int RANK_ACCESSIBILITY = 46;
    public static final int RANK_PRINTING = 47;
    public static final int RANK_DEVELOPEMENT = 48;
    public static final int RANK_DEVICE_INFO = 49;

    public static final int RANK_UNDEFINED = -1;
    public static final int RANK_OTHERS = 1024;
    public static final int BASE_RANK_DEFAULT = 2048;

    public static int sCurrentBaseRank = BASE_RANK_DEFAULT;

    private static HashMap<String, Integer> sRankMap = new HashMap<String, Integer>();
    private static HashMap<String, Integer> sBaseRankMap = new HashMap<String, Integer>();

    static {
        // Wi-Fi
        sRankMap.put(WifiSettings.class.getName(), RANK_WIFI);
        sRankMap.put(AdvancedWifiSettings.class.getName(), RANK_WIFI);
        sRankMap.put(SavedAccessPointsWifiSettings.class.getName(), RANK_WIFI);

        // BT
        sRankMap.put(BluetoothSettings.class.getName(), RANK_BT);

        // SIM Cards
        sRankMap.put(SimSettings.class.getName(), RANK_SIM);

        // DataUsage
        sRankMap.put(DataUsageSummary.class.getName(), RANK_DATA_USAGE);
        sRankMap.put(DataUsageMeteredSettings.class.getName(), RANK_DATA_USAGE);

        // Other wireless settinfs
        sRankMap.put(WirelessSettings.class.getName(), RANK_WIRELESS);

        // AICP Extras
        sRankMap.put(AddOns.class.getName(), RANK_AICP_ADDONS);

        // AICP Extras
        sRankMap.put(BuiltIn.class.getName(), RANK_AICP_BUILTIN);

        // AICP Extras
        sRankMap.put(Ui.class.getName(), RANK_AICP_UI);

        // AICP Animation Controls
        sRankMap.put(AnimationControls.class.getName(), RANK_ANIMATIONCONTROLS);

        // AICP App CircleBar
        sRankMap.put(AppCircleBar.class.getName(), RANK_APPCIRCLEBAR);

        // AICP App CircleBar
        sRankMap.put(AppSidebar.class.getName(), RANK_APPSIDEBAR);

        // AICP Battery Settings
        sRankMap.put(BatterySettings.class.getName(), RANK_BATTERYSETTINGS);

        // AICP Breathing Notifications
        sRankMap.put(BreathingNotifications.class.getName(), RANK_BREATHINGNOTIF);

        // AICP Carrier Label
        sRankMap.put(CarrierLabel.class.getName(), RANK_CARRIERLABEL);

        // AICP Display Animations Settings
        sRankMap.put(DisplayAnimationsSettings.class.getName(), RANK_DISPLAYANIMS);

        // AICP HeadsUP
        sRankMap.put(HeadsUpSettings.class.getName(), RANK_HEADSUP);

        // AICP Ambient Settings
        sRankMap.put(AmbientSettings.class.getName(), RANK_AMBIENT);

        // AICP Lockscreen Color
        sRankMap.put(LockScreenColorSettings.class.getName(), RANK_LSCOLOR);

        // AICP Lockscreen Weather
        sRankMap.put(LockScreenWeatherSettings.class.getName(), RANK_LSWEATHER);

        // AICP NavigationBar Dimensions
        sRankMap.put(NavBarDimensions.class.getName(), RANK_NAVBARDIM);

        // AICP Network Traffic
        sRankMap.put(NetworkTrafficFragment.class.getName(), RANK_NETTRAFFIC);

        // AICP OmniSwitch
        sRankMap.put(OmniSwitch.class.getName(), RANK_OMNISWITCH);

        // AICP Overscroll Effects
        sRankMap.put(OverscrollEffects.class.getName(), RANK_OVERSCROLL);

        // AICP PIE Button Style
        sRankMap.put(PieButtonStyleSettings.class.getName(), RANK_PIEBUTTON);

        // AICP PIE Control
        sRankMap.put(PieControl.class.getName(), RANK_PIECONTROL);

        // AICP PIE Style
        sRankMap.put(PieStyleSettings.class.getName(), RANK_PIESTYLE);

        // AICP PIE Trigger
        sRankMap.put(PieStyleSettings.class.getName(), RANK_PIETRIGGER);

        // AICP QS Colors
        sRankMap.put(QSColors.class.getName(), RANK_QSCOLOR);

        // AICP Recents Panel
        sRankMap.put(RecentsPanelSettings.class.getName(), RANK_RECENTSPANEL);

        // AICP Statusbar Weather
        sRankMap.put(StatusBarWeather.class.getName(), RANK_STATUSBARWEATHER);

        // AICP Various Shit
        sRankMap.put(VariousShit.class.getName(), RANK_VARIOUSSHIT);

        // AICP Volume Steps
        sRankMap.put(VolumeSteps.class.getName(), RANK_VOLSTEPS);

        // AICP GestureAnywhere
        sRankMap.put(GestureAnywhereSettings.class.getName(), RANK_GESTUREANYWHERE);

        // AICP Advanced Sounds
        sRankMap.put(SoundSettings.class.getName(), RANK_ADVANCEDSOUNDS);

        // Home
        sRankMap.put(HomeSettings.class.getName(), RANK_HOME);

        // Display
        sRankMap.put(DisplaySettings.class.getName(), RANK_DISPLAY);

        // Notifications
        sRankMap.put(NotificationSettings.class.getName(), RANK_NOTIFICATIONS);
        sRankMap.put(OtherSoundSettings.class.getName(), RANK_NOTIFICATIONS);
        sRankMap.put(ZenModeSettings.class.getName(), RANK_NOTIFICATIONS);

        // Storage
        sRankMap.put(Memory.class.getName(), RANK_STORAGE);
        sRankMap.put(UsbSettings.class.getName(), RANK_STORAGE);

        // Battery
        sRankMap.put(PowerUsageSummary.class.getName(), RANK_POWER_USAGE);
        sRankMap.put(BatterySaverSettings.class.getName(), RANK_POWER_USAGE);

        // Users
        sRankMap.put(UserSettings.class.getName(), RANK_USERS);

        // Location
        sRankMap.put(LocationSettings.class.getName(), RANK_LOCATION);

        // Security
        sRankMap.put(SecuritySettings.class.getName(), RANK_SECURITY);
        sRankMap.put(ChooseLockGeneric.ChooseLockGenericFragment.class.getName(), RANK_SECURITY);
        sRankMap.put(ScreenPinningSettings.class.getName(), RANK_SECURITY);

        // IMEs
        sRankMap.put(InputMethodAndLanguageSettings.class.getName(), RANK_IME);
        sRankMap.put(VoiceInputSettings.class.getName(), RANK_IME);

        // Privacy
        sRankMap.put(PrivacySettings.class.getName(), RANK_PRIVACY);
        sRankMap.put(com.android.settings.cyanogenmod.PrivacySettings.class.getName(), RANK_PRIVACY);

        // Date / Time
        sRankMap.put(DateTimeSettings.class.getName(), RANK_DATE_TIME);

        // Accessibility
        sRankMap.put(AccessibilitySettings.class.getName(), RANK_ACCESSIBILITY);

        // Print
        sRankMap.put(PrintSettingsFragment.class.getName(), RANK_PRINTING);

        // Development
        sRankMap.put(DevelopmentSettings.class.getName(), RANK_DEVELOPEMENT);

        // Device infos
        sRankMap.put(DeviceInfoSettings.class.getName(), RANK_DEVICE_INFO);

        sBaseRankMap.put("com.android.settings", 0);
    }

    public static int getRankForClassName(String className) {
        Integer rank = sRankMap.get(className);
        return (rank != null) ? (int) rank: RANK_OTHERS;
    }

    public static int getBaseRankForAuthority(String authority) {
        synchronized (sBaseRankMap) {
            Integer base = sBaseRankMap.get(authority);
            if (base != null) {
                return base;
            }
            sCurrentBaseRank++;
            sBaseRankMap.put(authority, sCurrentBaseRank);
            return sCurrentBaseRank;
        }
    }
}
