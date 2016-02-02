/*
 * Copyright (C) 2012 The CyanogenMod Project
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

package com.android.settings.notificationlight;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settings.R;

public class ApplicationLightPreference extends DialogPreference {

    private static String TAG = "AppLightPreference";
    public static final int DEFAULT_TIME = 1000;
    public static final int DEFAULT_COLOR = 0xffffff;

    private ImageView mLightColorView;
    private TextView mOnValueView;
    private TextView mOffValueView;

    private int mColorValue;
    private int mOnValue;
    private int mOffValue;
    private boolean mOnOffChangeable;

    private Resources mResources;

    /**
     * @param context
     * @param attrs
     */
    public ApplicationLightPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mColorValue = DEFAULT_COLOR;
        mOnValue = DEFAULT_TIME;
        mOffValue = DEFAULT_TIME;
        mOnOffChangeable = context.getResources().getBoolean(
                com.android.internal.R.bool.config_ledCanPulse);
        init();
    }

    /**
     * @param context
     * @param color
     * @param onValue
     * @param offValue
     */
    public ApplicationLightPreference(Context context, int color, int onValue, int offValue) {
        super(context, null);
        mColorValue = color;
        mOnValue = onValue;
        mOffValue = offValue;
        mOnOffChangeable = context.getResources().getBoolean(
                com.android.internal.R.bool.config_ledCanPulse);
        init();
    }

    /**
     * @param context
     * @param color
     * @param onValue
     * @param offValue
     */
    public ApplicationLightPreference(Context context, int color, int onValue, int offValue, boolean onOffChangeable) {
        super(context, null);
        mColorValue = color;
        mOnValue = onValue;
        mOffValue = offValue;
        mOnOffChangeable = onOffChangeable;
        init();
    }

    private void init() {
        setLayoutResource(R.layout.preference_application_light);
        mResources = getContext().getResources();
    }

    public void onStart() {
        LightSettingsDialog d = (LightSettingsDialog) getDialog();
        if (d != null) {
            d.onStart();
        }
    }

    public void onStop() {
        LightSettingsDialog d = (LightSettingsDialog) getDialog();
        if (d != null) {
            d.onStop();
        }
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        mLightColorView = (ImageView) view.findViewById(R.id.light_color);
        mOnValueView = (TextView) view.findViewById(R.id.textViewTimeOnValue);
        mOffValueView = (TextView) view.findViewById(R.id.textViewTimeOffValue);

        // Hide the summary text - it takes up too much space on a low res device
        // We use it for storing the package name for the longClickListener
        TextView tView = (TextView) view.findViewById(android.R.id.summary);
        tView.setVisibility(View.GONE);

        if (!mResources.getBoolean(com.android.internal.R.bool.config_multiColorNotificationLed)) {
            mLightColorView.setVisibility(View.GONE);
        }

        updatePreferenceViews();
    }

    private void updatePreferenceViews() {
        final int size = (int) mResources.getDimension(R.dimen.oval_notification_size);

        if (mLightColorView != null) {
            mLightColorView.setEnabled(true);
            // adjust if necessary to prevent material whiteout
            final int imageColor = ((mColorValue & 0xF0F0F0) == 0xF0F0F0) ?
                    (mColorValue - 0x101010) : mColorValue;
            mLightColorView.setImageDrawable(createOvalShape(size,
                    0xFF000000 + imageColor));
        }
        if (mOnValueView != null) {
            mOnValueView.setText(mapLengthValue(mOnValue));
        }
        if (mOffValueView != null) {
            if (mOnValue == 1 || !mOnOffChangeable) {
                mOffValueView.setVisibility(View.GONE);
            } else {
                mOffValueView.setVisibility(View.VISIBLE);
            }
            mOffValueView.setText(mapSpeedValue(mOffValue));
        }
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);

        final LightSettingsDialog d = (LightSettingsDialog) getDialog();
    }

    @Override
    protected Dialog createDialog() {
        final LightSettingsDialog d = new LightSettingsDialog(getContext(),
                0xFF000000 + mColorValue, mOnValue, mOffValue, mOnOffChangeable);
        d.setAlphaSliderVisible(false);

        d.setButton(AlertDialog.BUTTON_POSITIVE, mResources.getString(R.string.dlg_ok),
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mColorValue =  d.getColor() - 0xFF000000; // strip alpha, led does not support it
                mOnValue = d.getPulseSpeedOn();
                mOffValue = d.getPulseSpeedOff();
                updatePreferenceViews();
                callChangeListener(this);
            }
        });
        d.setButton(AlertDialog.BUTTON_NEGATIVE, mResources.getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        return d;
    }

    /**
     * Getters and Setters
     */

    public int getColor() {
        return mColorValue;
    }

    public void setColor(int color) {
        mColorValue = color;
        updatePreferenceViews();
    }

    public void setOnValue(int value) {
        mOnValue = value;
        updatePreferenceViews();
    }

    public int getOnValue() {
        return mOnValue;
    }

    public void setOffValue(int value) {
        mOffValue = value;
        updatePreferenceViews();
    }

    public int getOffValue() {
        return mOffValue;
    }

    public void setAllValues(int color, int onValue, int offValue) {
        mColorValue = color;
        mOnValue = onValue;
        mOffValue = offValue;
        updatePreferenceViews();
    }

    public void setAllValues(int color, int onValue, int offValue, boolean onOffChangeable) {
        mColorValue = color;
        mOnValue = onValue;
        mOffValue = offValue;
        mOnOffChangeable = onOffChangeable;
        updatePreferenceViews();
    }

    public void setOnOffValue(int onValue, int offValue) {
        mOnValue = onValue;
        mOffValue = offValue;
        updatePreferenceViews();
    }

    public void setOnOffChangeable(boolean value) {
        mOnOffChangeable = value;
    }

    /**
     * Utility methods
     */
    private static ShapeDrawable createOvalShape(int size, int color) {
        ShapeDrawable shape = new ShapeDrawable(new OvalShape());
        shape.setIntrinsicHeight(size);
        shape.setIntrinsicWidth(size);
        shape.getPaint().setColor(color);
        return shape;
    }

    private String mapLengthValue(Integer time) {
        if (!mOnOffChangeable) {
            return getContext().getString(R.string.pulse_length_always_on);
        }
        if (time == DEFAULT_TIME) {
            return getContext().getString(R.string.default_time);
        }

        String[] timeNames = mResources.getStringArray(R.array.notification_pulse_length_entries);
        String[] timeValues = mResources.getStringArray(R.array.notification_pulse_length_values);

        for (int i = 0; i < timeValues.length; i++) {
            if (Integer.decode(timeValues[i]).equals(time)) {
                return timeNames[i];
            }
        }

        return getContext().getString(R.string.custom_time);
    }

    private String mapSpeedValue(Integer time) {
        if (time == DEFAULT_TIME) {
            return getContext().getString(R.string.default_time);
        }

        String[] timeNames = mResources.getStringArray(R.array.notification_pulse_speed_entries);
        String[] timeValues = mResources.getStringArray(R.array.notification_pulse_speed_values);

        for (int i = 0; i < timeValues.length; i++) {
            if (Integer.decode(timeValues[i]).equals(time)) {
                return timeNames[i];
            }
        }

        return getContext().getString(R.string.custom_time);
    }
}
