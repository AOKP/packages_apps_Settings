/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemProperties;
import android.provider.SearchIndexableResource;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LegalSettings extends SettingsPreferenceFragment implements Indexable {

    private static final String LOG_TAG = "LegalSettings";
    private static final String KEY_TERMS = "terms";
    private static final String KEY_LICENSE = "license";
    private static final String KEY_COPYRIGHT = "copyright";
    private static final String KEY_WEBVIEW_LICENSE = "webview_license";
    private static final String PROPERTY_CMLICENSE_URL = "ro.cmlegal.url";
    private static final String KEY_CM_LICENSE = "cmlicense";

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.about_legal);

        final Activity act = getActivity();
        // These are contained in the "container" preference group
        PreferenceGroup parentPreference = getPreferenceScreen();
        Utils.updatePreferenceToSpecificActivityOrRemove(act, parentPreference, KEY_TERMS,
                Utils.UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY);
        Utils.updatePreferenceToSpecificActivityOrRemove(act, parentPreference, KEY_LICENSE,
                Utils.UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY);
        Utils.updatePreferenceToSpecificActivityOrRemove(act, parentPreference, KEY_COPYRIGHT,
                Utils.UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY);
        Utils.updatePreferenceToSpecificActivityOrRemove(act, parentPreference, KEY_WEBVIEW_LICENSE,
                Utils.UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference.getKey().equals(KEY_CM_LICENSE)) {
            String userCMLicenseUrl = SystemProperties.get(PROPERTY_CMLICENSE_URL);
            final Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setData(Uri.parse(userCMLicenseUrl));
            try {
                startActivity(intent);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Unable to start activity " + intent.toString());
            }
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.ABOUT_LEGAL_SETTINGS;
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {

            @Override
            public List<SearchIndexableResource> getXmlResourcesToIndex(
                    Context context, boolean enabled) {
                final SearchIndexableResource sir = new SearchIndexableResource(context);
                sir.xmlResId = R.xml.about_legal;
                return Arrays.asList(sir);
            }

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                final List<String> keys = new ArrayList<String>();
                if (!checkIntentAction(context, "android.settings.TERMS")) {
                    keys.add(KEY_TERMS);
                }
                if (!checkIntentAction(context, "android.settings.LICENSE")) {
                    keys.add(KEY_LICENSE);
                }
                if (!checkIntentAction(context, "android.settings.COPYRIGHT")) {
                    keys.add(KEY_COPYRIGHT);
                }
                if (!checkIntentAction(context, "android.settings.WEBVIEW_LICENSE")) {
                    keys.add(KEY_WEBVIEW_LICENSE);
                }
                return keys;
            }

            private boolean checkIntentAction(Context context, String action) {
                final Intent intent = new Intent(action);

                // Find the activity that is in the system image
                final PackageManager pm = context.getPackageManager();
                final List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
                final int listSize = list.size();

                for (int i = 0; i < listSize; i++) {
                    ResolveInfo resolveInfo = list.get(i);
                    if ((resolveInfo.activityInfo.applicationInfo.flags &
                            ApplicationInfo.FLAG_SYSTEM) != 0) {
                        return true;
                    }
                }

                return false;
            }
    };

}
