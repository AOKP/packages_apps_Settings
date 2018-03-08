/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.display;

import android.content.Intent;
import android.content.Context;
import android.content.ComponentName;

import android.support.v7.preference.Preference;

import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.R;

import static com.android.internal.logging.nano.MetricsProto.MetricsEvent.ACTION_AMBIENT_DISPLAY;

public class AmbientDisplayCustomPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {

    private Context mContext;
    private String KEY_AMBIENT_CUSTOM = "ambient_display_custom";

    private final MetricsFeatureProvider mMetricsFeatureProvider;

    public AmbientDisplayCustomPreferenceController(Context context) {
        super(context);
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
        mContext = context;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_AMBIENT_CUSTOM;
    }

    @Override
    public boolean isAvailable() {
        return !mContext.getResources().getString(R.string.config_customDozePackage).equals("");
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (KEY_AMBIENT_CUSTOM.equals(preference.getKey())) {
            mMetricsFeatureProvider.action(mContext, ACTION_AMBIENT_DISPLAY);
            try {
                String[] customDozePackage = mContext.getResources().getString(R.string.config_customDozePackage).split("/");
                String activityName = customDozePackage[0];
                String className = customDozePackage[1];
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(activityName, className));
                mContext.startActivity(intent);
            } catch (Exception e){
            }
        }
        return false;
    }
}
