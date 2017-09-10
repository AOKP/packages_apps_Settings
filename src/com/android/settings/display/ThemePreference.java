/*
 * Copyright (C) 2017 The Paranoid Android Project
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

package com.android.settings.display;

import android.content.Context;
import android.support.v4.content.res.TypedArrayUtils;
import android.support.v7.preference.PreferenceGroup;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.android.settings.R;

/**
 * Preference for changing the current theme colors.
 */
public class ThemePreference extends PreferenceGroup {

    public ThemePreference(Context context, AttributeSet attrs) {
        super(context, attrs, TypedArrayUtils.getAttr(context,
                android.support.v7.preference.R.attr.preferenceScreenStyle,
                android.R.attr.preferenceScreenStyle));

        if (TextUtils.isEmpty(getFragment())) {
            setFragment("com.android.settings.display.ThemeSettings");
        }

        if (TextUtils.isEmpty(getSummary())) {
            String summary = context.getString(R.string.theme_summary);
            setSummary(summary);
        }
    }

    @Override
    protected boolean isOnSameScreenAsChildren() {
        return false;
    }
}
