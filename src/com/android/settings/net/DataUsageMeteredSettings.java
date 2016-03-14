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

package com.android.settings.net;

import static android.net.NetworkPolicy.LIMIT_DISABLED;
import static android.net.wifi.WifiInfo.removeDoubleQuotes;
import static com.android.settings.DataUsageSummary.hasReadyMobileRadio;
import static com.android.settings.DataUsageSummary.hasWifiRadio;

import android.content.Context;
import android.content.res.Resources;
import android.net.NetworkPolicy;
import android.net.NetworkPolicyManager;
import android.net.NetworkTemplate;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.SwitchPreference;
import android.telephony.TelephonyManager;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settingslib.NetworkPolicyEditor;

import java.util.ArrayList;
import java.util.List;

/**
 * Panel to configure {@link NetworkPolicy#metered} for networks.
 */
public class DataUsageMeteredSettings extends SettingsPreferenceFragment implements Indexable {

    private static final boolean SHOW_MOBILE_CATEGORY = false;

    private NetworkPolicyManager mPolicyManager;
    private WifiManager mWifiManager;

    private NetworkPolicyEditor mPolicyEditor;

    private PreferenceCategory mMobileCategory;
    private PreferenceCategory mWifiCategory;
    private Preference mWifiDisabled;

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.NET_DATA_USAGE_METERED;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        final Context context = getActivity();

        mPolicyManager = NetworkPolicyManager.from(context);
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        mPolicyEditor = new NetworkPolicyEditor(mPolicyManager);
        mPolicyEditor.read();

        addPreferencesFromResource(R.xml.data_usage_metered_prefs);
        mMobileCategory = (PreferenceCategory) findPreference("mobile");
        mWifiCategory = (PreferenceCategory) findPreference("wifi");
        mWifiDisabled = findPreference("wifi_disabled");

        updateNetworks(context);
    }

    private void updateNetworks(Context context) {
        if (SHOW_MOBILE_CATEGORY && hasReadyMobileRadio(context)) {
            mMobileCategory.removeAll();
            mMobileCategory.addPreference(buildMobilePref(context));
        } else {
            getPreferenceScreen().removePreference(mMobileCategory);
        }

        mWifiCategory.removeAll();
        if (hasWifiRadio(context) && mWifiManager.isWifiEnabled()) {
            for (WifiConfiguration config : mWifiManager.getConfiguredNetworks()) {
                if (config.SSID != null) {
                    mWifiCategory.addPreference(buildWifiPref(context, config));
                }
            }
        } else {
            mWifiCategory.addPreference(mWifiDisabled);
        }
    }

    private Preference buildMobilePref(Context context) {
        final TelephonyManager tele = TelephonyManager.from(context);
        final NetworkTemplate template = NetworkTemplate.buildTemplateMobileAll(
                tele.getSubscriberId());
        final MeteredPreference pref = new MeteredPreference(context, template);
        pref.setTitle(tele.getNetworkOperatorName());
        return pref;
    }

    private Preference buildWifiPref(Context context, WifiConfiguration config) {
        final String networkId = config.SSID;
        final NetworkTemplate template = NetworkTemplate.buildTemplateWifi(networkId);
        final MeteredPreference pref = new MeteredPreference(context, template);
        pref.setTitle(removeDoubleQuotes(networkId));
        return pref;
    }

    private class MeteredPreference extends SwitchPreference {
        private final NetworkTemplate mTemplate;
        private boolean mBinding;

        public MeteredPreference(Context context, NetworkTemplate template) {
            super(context);
            mTemplate = template;

            setPersistent(false);

            mBinding = true;
            final NetworkPolicy policy = mPolicyEditor.getPolicyMaybeUnquoted(template);
            if (policy != null) {
                if (policy.limitBytes != LIMIT_DISABLED) {
                    setChecked(true);
                    setEnabled(false);
                } else {
                    setChecked(policy.metered);
                }
            } else {
                setChecked(false);
            }
            mBinding = false;
        }

        @Override
        protected void notifyChanged() {
            super.notifyChanged();
            if (!mBinding) {
                mPolicyEditor.setPolicyMetered(mTemplate, isChecked());
            }
        }
    }

    /**
     * For search
     */
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
                final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();
                final Resources res = context.getResources();

                // Add fragment title
                SearchIndexableRaw data = new SearchIndexableRaw(context);
                data.title = res.getString(R.string.data_usage_menu_metered);
                data.screenTitle = res.getString(R.string.data_usage_menu_metered);
                result.add(data);

                // Body
                data = new SearchIndexableRaw(context);
                data.title = res.getString(R.string.data_usage_metered_body);
                data.screenTitle = res.getString(R.string.data_usage_menu_metered);
                result.add(data);

                if (SHOW_MOBILE_CATEGORY && hasReadyMobileRadio(context)) {
                    // Mobile networks category
                    data = new SearchIndexableRaw(context);
                    data.title = res.getString(R.string.data_usage_metered_mobile);
                    data.screenTitle = res.getString(R.string.data_usage_menu_metered);
                    result.add(data);

                    final TelephonyManager tele = TelephonyManager.from(context);

                    data = new SearchIndexableRaw(context);
                    data.title = tele.getNetworkOperatorName();
                    data.screenTitle = res.getString(R.string.data_usage_menu_metered);
                    result.add(data);
                }

                // Wi-Fi networks category
                data = new SearchIndexableRaw(context);
                data.title = res.getString(R.string.data_usage_metered_wifi);
                data.screenTitle = res.getString(R.string.data_usage_menu_metered);
                result.add(data);

                final WifiManager wifiManager =
                        (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                if (hasWifiRadio(context) && wifiManager.isWifiEnabled()) {
                    for (WifiConfiguration config : wifiManager.getConfiguredNetworks()) {
                        if (config.SSID != null) {
                            final String networkId = config.SSID;

                            data = new SearchIndexableRaw(context);
                            data.title = removeDoubleQuotes(networkId);
                            data.screenTitle = res.getString(R.string.data_usage_menu_metered);
                            result.add(data);
                        }
                    }
                } else {
                    data = new SearchIndexableRaw(context);
                    data.title = res.getString(R.string.data_usage_metered_wifi_disabled);
                    data.screenTitle = res.getString(R.string.data_usage_menu_metered);
                    result.add(data);
                }

                return result;
            }

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                final ArrayList<String> result = new ArrayList<String>();
                if (!SHOW_MOBILE_CATEGORY || !hasReadyMobileRadio(context)) {
                    result.add("mobile");
                }

                return result;
            }
        };

}
