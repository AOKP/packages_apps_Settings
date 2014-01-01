/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.settings.wfd;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.hardware.display.DisplayManager;
import android.hardware.display.WifiDisplay;
import android.hardware.display.WifiDisplayStatus;
import android.media.MediaRouter;
import android.media.MediaRouter.RouteInfo;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.WpsInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.Html;
import android.util.Slog;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.android.internal.app.MediaRouteDialogPresenter;
import com.android.settings.ProgressCategory;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

/**
 * The Settings screen for WifiDisplay configuration and connection management.
 *
 * The wifi display routes are integrated together with other remote display routes
 * from the media router.  It may happen that wifi display isn't actually available
 * on the system.  In that case, the enable option will not be shown but other
 * remote display routes will continue to be made available.
 */
public final class WifiDisplaySettings extends SettingsPreferenceFragment {
    private static final String TAG = "WifiDisplaySettings";
    private static final boolean DEBUG = false;

    private static final int MENU_ID_ENABLE_WIFI_DISPLAY = Menu.FIRST;

    private static final int CHANGE_SETTINGS = 1 << 0;
    private static final int CHANGE_ROUTES = 1 << 1;
    private static final int CHANGE_WIFI_DISPLAY_STATUS = 1 << 2;
    private static final int CHANGE_ALL = -1;

    private static final int ORDER_CERTIFICATION = 1;
    private static final int ORDER_CONNECTED = 2;
    private static final int ORDER_AVAILABLE = 3;
    private static final int ORDER_UNAVAILABLE = 4;

    private final Handler mHandler;

    private MediaRouter mRouter;
    private DisplayManager mDisplayManager;

    private boolean mStarted;
    private int mPendingChanges;

    private boolean mWifiDisplayOnSetting;
    private WifiDisplayStatus mWifiDisplayStatus;

    private TextView mEmptyView;

    /* certification */
    private boolean mWifiDisplayCertificationOn;
    private WifiP2pManager mWifiP2pManager;
    private Channel mWifiP2pChannel;
    private PreferenceGroup mCertCategory;
    private boolean mListen;
    private boolean mAutoGO;
    private int mWpsConfig = WpsInfo.INVALID;
    private int mListenChannel;
    private int mOperatingChannel;

    public WifiDisplaySettings() {
        mHandler = new Handler();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        final Context context = getActivity();
        mRouter = (MediaRouter)context.getSystemService(Context.MEDIA_ROUTER_SERVICE);
        mDisplayManager = (DisplayManager)context.getSystemService(Context.DISPLAY_SERVICE);
        mWifiP2pManager = (WifiP2pManager)context.getSystemService(Context.WIFI_P2P_SERVICE);
        mWifiP2pChannel = mWifiP2pManager.initialize(context, Looper.getMainLooper(), null);

        addPreferencesFromResource(R.xml.wifi_display_settings);
        setHasOptionsMenu(true);
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_remote_display;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mEmptyView = (TextView) getView().findViewById(android.R.id.empty);
        mEmptyView.setText(R.string.wifi_display_no_devices_found);
        getListView().setEmptyView(mEmptyView);
    }

    @Override
    public void onStart() {
        super.onStart();
        mStarted = true;

        final Context context = getActivity();
        IntentFilter filter = new IntentFilter();
        filter.addAction(DisplayManager.ACTION_WIFI_DISPLAY_STATUS_CHANGED);
        context.registerReceiver(mReceiver, filter);

        getContentResolver().registerContentObserver(Settings.Global.getUriFor(
                Settings.Global.WIFI_DISPLAY_ON), false, mSettingsObserver);
        getContentResolver().registerContentObserver(Settings.Global.getUriFor(
                Settings.Global.WIFI_DISPLAY_CERTIFICATION_ON), false, mSettingsObserver);
        getContentResolver().registerContentObserver(Settings.Global.getUriFor(
                Settings.Global.WIFI_DISPLAY_WPS_CONFIG), false, mSettingsObserver);

        mRouter.addCallback(MediaRouter.ROUTE_TYPE_REMOTE_DISPLAY, mRouterCallback,
                MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);

        update(CHANGE_ALL);
    }

    @Override
    public void onStop() {
        super.onStop();
        mStarted = false;

        final Context context = getActivity();
        context.unregisterReceiver(mReceiver);

        getContentResolver().unregisterContentObserver(mSettingsObserver);

        mRouter.removeCallback(mRouterCallback);

        unscheduleUpdate();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mWifiDisplayStatus != null && mWifiDisplayStatus.getFeatureState()
                != WifiDisplayStatus.FEATURE_STATE_UNAVAILABLE) {
            MenuItem item = menu.add(Menu.NONE, MENU_ID_ENABLE_WIFI_DISPLAY, 0,
                    R.string.wifi_display_enable_menu_item);
            item.setCheckable(true);
            item.setChecked(mWifiDisplayOnSetting);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ID_ENABLE_WIFI_DISPLAY:
                mWifiDisplayOnSetting = !item.isChecked();
                item.setChecked(mWifiDisplayOnSetting);
                Settings.Global.putInt(getContentResolver(),
                        Settings.Global.WIFI_DISPLAY_ON, mWifiDisplayOnSetting ? 1 : 0);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void scheduleUpdate(int changes) {
        if (mStarted) {
            if (mPendingChanges == 0) {
                mHandler.post(mUpdateRunnable);
            }
            mPendingChanges |= changes;
        }
    }

    private void unscheduleUpdate() {
        if (mPendingChanges != 0) {
            mPendingChanges = 0;
            mHandler.removeCallbacks(mUpdateRunnable);
        }
    }

    private void update(int changes) {
        boolean invalidateOptions = false;

        // Update settings.
        if ((changes & CHANGE_SETTINGS) != 0) {
            mWifiDisplayOnSetting = Settings.Global.getInt(getContentResolver(),
                    Settings.Global.WIFI_DISPLAY_ON, 0) != 0;
            mWifiDisplayCertificationOn = Settings.Global.getInt(getContentResolver(),
                    Settings.Global.WIFI_DISPLAY_CERTIFICATION_ON, 0) != 0;
            mWpsConfig = Settings.Global.getInt(getContentResolver(),
                Settings.Global.WIFI_DISPLAY_WPS_CONFIG, WpsInfo.INVALID);

            // The wifi display enabled setting may have changed.
            invalidateOptions = true;
        }

        // Update wifi display state.
        if ((changes & CHANGE_WIFI_DISPLAY_STATUS) != 0) {
            mWifiDisplayStatus = mDisplayManager.getWifiDisplayStatus();

            // The wifi display feature state may have changed.
            invalidateOptions = true;
        }


            // The wifi display enabled setting may have changed.
            invalidateOptions = true;
        }

        // Update wifi display state.
        if ((changes & CHANGE_WIFI_DISPLAY_STATUS) != 0) {
            mWifiDisplayStatus = mDisplayManager.getWifiDisplayStatus();

            // The wifi display feature state may have changed.
            invalidateOptions = true;
        }

        // Rebuild the routes.
        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceScreen.removeAll();

        // Add all known remote display routes.
        final int routeCount = mRouter.getRouteCount();
        for (int i = 0; i < routeCount; i++) {
            MediaRouter.RouteInfo route = mRouter.getRouteAt(i);
            if (route.matchesTypes(MediaRouter.ROUTE_TYPE_REMOTE_DISPLAY)) {
                preferenceScreen.addPreference(createRoutePreference(route));
            }
        }

        // Additional features for wifi display routes.
        if (mWifiDisplayStatus != null
                && mWifiDisplayStatus.getFeatureState() == WifiDisplayStatus.FEATURE_STATE_ON) {
            // Add all unpaired wifi displays.
            for (WifiDisplay display : mWifiDisplayStatus.getDisplays()) {
                if (!display.isRemembered() && display.isAvailable()
                        && !display.equals(mWifiDisplayStatus.getActiveDisplay())) {
                    preferenceScreen.addPreference(new UnpairedWifiDisplayPreference(
                            getActivity(), display));
                }
            }


        // Additional features for wifi display routes.
        if (mWifiDisplayStatus != null
                && mWifiDisplayStatus.getFeatureState() == WifiDisplayStatus.FEATURE_STATE_ON) {
            // Add all unpaired wifi displays.
            for (WifiDisplay display : mWifiDisplayStatus.getDisplays()) {
                if (!display.isRemembered() && display.isAvailable()
                        && !display.equals(mWifiDisplayStatus.getActiveDisplay())) {
                    preferenceScreen.addPreference(new UnpairedWifiDisplayPreference(
                            getActivity(), display));
                }
            }

            // Add the certification menu if enabled in developer options.
            if (mWifiDisplayCertificationOn) {
                buildCertificationMenu(preferenceScreen);
            }
        }

        // Invalidate menu options if needed.
        if (invalidateOptions) {
            getActivity().invalidateOptionsMenu();
        }
    }

    private RoutePreference createRoutePreference(MediaRouter.RouteInfo route) {
        WifiDisplay display = findWifiDisplay(route.getDeviceAddress());
        if (display != null) {
            return new WifiDisplayRoutePreference(getActivity(), route, display);
        } else {
            return new RoutePreference(getActivity(), route);
        }
    }

    private WifiDisplay findWifiDisplay(String deviceAddress) {
        if (mWifiDisplayStatus != null && deviceAddress != null) {
            for (WifiDisplay display : mWifiDisplayStatus.getDisplays()) {
                if (display.getDeviceAddress().equals(deviceAddress)) {
                    return display;
                }
            }
        }
        return null;
    }

    private void buildCertificationMenu(final PreferenceScreen preferenceScreen) {
        if (mCertCategory == null) {
            mCertCategory = new PreferenceCategory(getActivity());
            mCertCategory.setTitle(R.string.wifi_display_certification_heading);
            mCertCategory.setOrder(ORDER_CERTIFICATION);
        } else {
            mCertCategory.removeAll();
        }
        preferenceScreen.addPreference(mCertCategory);

        // display session info if there is an active p2p session
        if (!mWifiDisplayStatus.getSessionInfo().getGroupId().isEmpty()) {
            Preference p = new Preference(getActivity());
            p.setTitle(R.string.wifi_display_session_info);
            p.setSummary(mWifiDisplayStatus.getSessionInfo().toString());
            mCertCategory.addPreference(p);

            // show buttons for Pause/Resume when a WFD session is established
            if (mWifiDisplayStatus.getSessionInfo().getSessionId() != 0) {
                mCertCategory.addPreference(new Preference(getActivity()) {
                    @Override
                    public View getView(View convertView, ViewGroup parent) {
                        final View v;
                        if (convertView == null) {
                            LayoutInflater li = (LayoutInflater) getActivity().
                                    getSystemService(Service.LAYOUT_INFLATER_SERVICE);
                            v = li.inflate(R.layout.two_buttons_panel, null);
                        } else {
                            v = convertView;
                        }

                        Button b = (Button)v.findViewById(R.id.left_button);
                        b.setText(R.string.wifi_display_pause);
                        b.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mDisplayManager.pauseWifiDisplay();
                            }
                        });

                        b = (Button)v.findViewById(R.id.right_button);
                        b.setText(R.string.wifi_display_resume);
                        b.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mDisplayManager.resumeWifiDisplay();
                            }
                        });

                        return v;
                    }
                });
            }
        }

        // switch for Listen Mode
        CheckBoxPreference cbp = new CheckBoxPreference(getActivity()) {
            @Override
            protected void onClick() {
                mListen = !mListen;
                setListenMode(mListen);
                setChecked(mListen);
            }
        };
        cbp.setTitle(R.string.wifi_display_listen_mode);
        cbp.setChecked(mListen);
        mCertCategory.addPreference(cbp);

        // switch for Autonomous GO
        cbp = new CheckBoxPreference(getActivity()) {
            @Override
            protected void onClick() {
                mAutoGO = !mAutoGO;
                if (mAutoGO) {
                    startAutoGO();
                } else {
                    stopAutoGO();
                }
                setChecked(mAutoGO);
            }
        };
        cbp.setTitle(R.string.wifi_display_autonomous_go);
        cbp.setChecked(mAutoGO);
        mCertCategory.addPreference(cbp);

        // Drop down list for choosing WPS method (PBC/KEYPAD/DISPLAY)
        ListPreference lp = new ListPreference(getActivity()) {
            @Override
            protected void onDialogClosed(boolean positiveResult) {
                super.onDialogClosed(positiveResult);
                if (positiveResult) {
                    mWpsConfig = Integer.parseInt(getValue());
                    setSummary("%1$s");
                    getActivity().invalidateOptionsMenu();
                    Settings.Global.putInt(getActivity().getContentResolver(),
                            Settings.Global.WIFI_DISPLAY_WPS_CONFIG, mWpsConfig);
                }
            }
        };
        mWpsConfig = Settings.Global.getInt(getActivity().getContentResolver(),
                Settings.Global.WIFI_DISPLAY_WPS_CONFIG, WpsInfo.INVALID);
        String[] wpsEntries = { "Default", "PBC", "KEYPAD", "DISPLAY" };
        String[] wpsValues = {
            "" + WpsInfo.INVALID,
            "" + WpsInfo.PBC,
            "" + WpsInfo.KEYPAD,
            "" + WpsInfo.DISPLAY };
        lp.setTitle(R.string.wifi_display_wps_config);
        lp.setEntries(wpsEntries);
        lp.setEntryValues(wpsValues);
        lp.setValue("" + mWpsConfig);
        lp.setSummary("%1$s");
        mCertCategory.addPreference(lp);

        // Drop down list for choosing listen channel
        lp = new ListPreference(getActivity()) {
            @Override
            protected void onDialogClosed(boolean positiveResult) {
                super.onDialogClosed(positiveResult);
                if (positiveResult) {
                    mListenChannel = Integer.parseInt(getValue());
                    setSummary("%1$s");
                    getActivity().invalidateOptionsMenu();
                    setWifiP2pChannels(mListenChannel, mOperatingChannel);
                }
            }
        };
        String[] lcEntries = { "Auto", "1", "6", "11" };
        String[] lcValues = { "0", "1", "6", "11" };
        lp.setTitle(R.string.wifi_display_listen_channel);
        lp.setEntries(lcEntries);
        lp.setEntryValues(lcValues);
        lp.setValue("" + mListenChannel);
        lp.setSummary("%1$s");
        mCertCategory.addPreference(lp);

        // Drop down list for choosing operating channel
        lp = new ListPreference(getActivity()) {
            @Override
            protected void onDialogClosed(boolean positiveResult) {
                super.onDialogClosed(positiveResult);
                if (positiveResult) {
                    mOperatingChannel = Integer.parseInt(getValue());
                    setSummary("%1$s");
                    getActivity().invalidateOptionsMenu();
                    setWifiP2pChannels(mListenChannel, mOperatingChannel);
                }
            }
        };
        String[] ocEntries = { "Auto", "1", "6", "11", "36" };
        String[] ocValues = { "0", "1", "6", "11", "36" };
        lp.setTitle(R.string.wifi_display_operating_channel);
        lp.setEntries(ocEntries);
        lp.setEntryValues(ocValues);
        lp.setValue("" + mOperatingChannel);
        lp.setSummary("%1$s");
        mCertCategory.addPreference(lp);
    }

    private void startAutoGO() {
        if (DEBUG) {
            Slog.d(TAG, "Starting Autonomous GO...");
        }
        mWifiP2pManager.createGroup(mWifiP2pChannel, new ActionListener() {
            @Override
            public void onSuccess() {
                if (DEBUG) {
                    Slog.d(TAG, "Successfully started AutoGO.");
                }
            }

            @Override
            public void onFailure(int reason) {
                Slog.e(TAG, "Failed to start AutoGO with reason " + reason + ".");
            }
        });
    }

    private void stopAutoGO() {
        if (DEBUG) {
            Slog.d(TAG, "Stopping Autonomous GO...");
        }
        mWifiP2pManager.removeGroup(mWifiP2pChannel, new ActionListener() {
            @Override
            public void onSuccess() {
                if (DEBUG) {
                    Slog.d(TAG, "Successfully stopped AutoGO.");
                }
            }

            @Override
            public void onFailure(int reason) {
                Slog.e(TAG, "Failed to stop AutoGO with reason " + reason + ".");
            }
        });
    }

    private void setListenMode(final boolean enable) {
        if (DEBUG) {
            Slog.d(TAG, "Setting listen mode to: " + enable);
        }
        mWifiP2pManager.listen(mWifiP2pChannel, enable, new ActionListener() {
            @Override
            public void onSuccess() {
                if (DEBUG) {
                    Slog.d(TAG, "Successfully " + (enable ? "entered" : "exited")
                            +" listen mode.");
                }
            }

            @Override
            public void onFailure(int reason) {
                Slog.e(TAG, "Failed to " + (enable ? "entered" : "exited")
                        +" listen mode with reason " + reason + ".");
            }
        });
    }

    private void setWifiP2pChannels(final int lc, final int oc) {
        if (DEBUG) {
            Slog.d(TAG, "Setting wifi p2p channel: lc=" + lc + ", oc=" + oc);
        }
        mWifiP2pManager.setWifiP2pChannels(mWifiP2pChannel,
                lc, oc, new ActionListener() {
            @Override
            public void onSuccess() {
                if (DEBUG) {
                    Slog.d(TAG, "Successfully set wifi p2p channels.");
                }
            }

            @Override
            public void onFailure(int reason) {
                Slog.e(TAG, "Failed to set wifi p2p channels with reason " + reason + ".");
            }
        });
    }

    private void toggleRoute(MediaRouter.RouteInfo route) {
        if (route.isSelected()) {
            MediaRouteDialogPresenter.showDialogFragment(getActivity(),
                    MediaRouter.ROUTE_TYPE_REMOTE_DISPLAY, null);
        } else {
            route.select();
        }
    }

    private void pairWifiDisplay(WifiDisplay display) {
        if (display.canConnect()) {
            mDisplayManager.connectWifiDisplay(display.getDeviceAddress());
        }
    }

    private void showWifiDisplayOptionsDialog(final WifiDisplay display) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.wifi_display_options, null);
        final EditText nameEditText = (EditText)view.findViewById(R.id.name);
        nameEditText.setText(display.getFriendlyDisplayName());

        DialogInterface.OnClickListener done = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = nameEditText.getText().toString().trim();
                if (name.isEmpty() || name.equals(display.getDeviceName())) {
                    name = null;
                }
                mDisplayManager.renameWifiDisplay(display.getDeviceAddress(), name);
            }
        };
        DialogInterface.OnClickListener forget = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mDisplayManager.forgetWifiDisplay(display.getDeviceAddress());
            }
        };

        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setCancelable(true)
                .setTitle(R.string.wifi_display_options_title)
                .setView(view)
                .setPositiveButton(R.string.wifi_display_options_done, done)
                .setNegativeButton(R.string.wifi_display_options_forget, forget)
                .create();
        dialog.show();
    }

    private final Runnable mUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            final int changes = mPendingChanges;
            mPendingChanges = 0;
            update(changes);
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(DisplayManager.ACTION_WIFI_DISPLAY_STATUS_CHANGED)) {
                scheduleUpdate(CHANGE_WIFI_DISPLAY_STATUS);
            }
        }
    };

    private final ContentObserver mSettingsObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            scheduleUpdate(CHANGE_SETTINGS);
        }
    };

    private final MediaRouter.Callback mRouterCallback = new MediaRouter.SimpleCallback() {
        @Override
        public void onRouteAdded(MediaRouter router, RouteInfo info) {
            scheduleUpdate(CHANGE_ROUTES);
        }

        @Override
        public void onRouteChanged(MediaRouter router, RouteInfo info) {
            scheduleUpdate(CHANGE_ROUTES);
        }

        @Override
        public void onRouteRemoved(MediaRouter router, RouteInfo info) {
            scheduleUpdate(CHANGE_ROUTES);
        }

        @Override

        @Override
        public void onRouteRemoved(MediaRouter router, RouteInfo info) {
            scheduleUpdate(CHANGE_ROUTES);
        }

        @Override
        public void onRouteSelected(MediaRouter router, int type, RouteInfo info) {
            scheduleUpdate(CHANGE_ROUTES);
        }

        @Override
        public void onRouteUnselected(MediaRouter router, int type, RouteInfo info) {
            scheduleUpdate(CHANGE_ROUTES);
        }
    };

    private class RoutePreference extends Preference
            implements Preference.OnPreferenceClickListener {
        private final MediaRouter.RouteInfo mRoute;

        public RoutePreference(Context context, MediaRouter.RouteInfo route) {
            super(context);

            mRoute = route;
            setTitle(route.getName());
            setSummary(route.getDescription());
            setEnabled(route.isEnabled());
            if (route.isSelected()) {
                setOrder(ORDER_CONNECTED);
                if (route.isConnecting()) {
                    setSummary(R.string.wifi_display_status_connecting);
                } else {
                    setSummary(R.string.wifi_display_status_connected);
                }
            } else {
                if (isEnabled()) {
                    setOrder(ORDER_AVAILABLE);
                } else {
                    setOrder(ORDER_UNAVAILABLE);
                    if (route.getStatusCode() == MediaRouter.RouteInfo.STATUS_IN_USE) {
                        setSummary(R.string.wifi_display_status_in_use);
                    } else {
                        setSummary(R.string.wifi_display_status_not_available);
                    }
                }
            }
            setOnPreferenceClickListener(this);
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            toggleRoute(mRoute);
            return true;
        }
    }

    private class WifiDisplayRoutePreference extends RoutePreference
            implements View.OnClickListener {
        private final WifiDisplay mDisplay;

        public WifiDisplayRoutePreference(Context context, MediaRouter.RouteInfo route,
                WifiDisplay display) {
            super(context, route);

            mDisplay = display;
            setWidgetLayoutResource(R.layout.wifi_display_preference);
        }

        @Override
        protected void onBindView(View view) {
            super.onBindView(view);

            ImageView deviceDetails = (ImageView) view.findViewById(R.id.deviceDetails);
            if (deviceDetails != null) {
                deviceDetails.setOnClickListener(this);
                if (!isEnabled()) {
                    TypedValue value = new TypedValue();
                    getContext().getTheme().resolveAttribute(android.R.attr.disabledAlpha,
                            value, true);
                    deviceDetails.setImageAlpha((int)(value.getFloat() * 255));
                    deviceDetails.setEnabled(true); // always allow button to be pressed
                }
            }
        }

        @Override
        public void onClick(View v) {
            showWifiDisplayOptionsDialog(mDisplay);
        }
    }

    private class UnpairedWifiDisplayPreference extends Preference
            implements Preference.OnPreferenceClickListener {
        private final WifiDisplay mDisplay;

        public UnpairedWifiDisplayPreference(Context context, WifiDisplay display) {
            super(context);

            mDisplay = display;
            setTitle(display.getFriendlyDisplayName());
            setSummary(com.android.internal.R.string.wireless_display_route_description);
            setEnabled(display.canConnect());
            if (isEnabled()) {
                setOrder(ORDER_AVAILABLE);
            } else {
                setOrder(ORDER_UNAVAILABLE);
                setSummary(R.string.wifi_display_status_in_use);
            }
            setOnPreferenceClickListener(this);
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            pairWifiDisplay(mDisplay);
            return true;
        }
    }
}
