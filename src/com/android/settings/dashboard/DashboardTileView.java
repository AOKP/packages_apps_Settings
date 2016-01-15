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

package com.android.settings.dashboard;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.android.settings.ProfileSelectDialog;
import com.android.settings.R;
import com.android.settings.Utils;

import com.android.internal.util.aicp.FontHelper;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class DashboardTileView extends FrameLayout implements View.OnClickListener {

    private static final int DEFAULT_COL_SPAN = 1;

    private ImageView mImageView;
    private TextView mTitleTextView;
    private TextView mStatusTextView;
    private View mDivider;
    private Switch mSwitch;
    private GenericSwitchToggle mSwitchToggle;

    private int mColSpan = DEFAULT_COL_SPAN;

    private DashboardTile mTile;

    public DashboardTileView(Context context) {
        this(context, null);
    }

    public DashboardTileView(Context context, AttributeSet attrs) {
        super(context, attrs);

        final View view = LayoutInflater.from(context).inflate(R.layout.dashboard_tile, this);

        mImageView = (ImageView) view.findViewById(R.id.icon);

        mTitleTextView = (TextView) view.findViewById(R.id.title);
        if (Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.DASHBOARD_TILEVIEW_DOUBLE_LINES, 0) == 1) {
        mTitleTextView.setSingleLine(false);
        } else {
        mTitleTextView.setSingleLine(true);
        }

        mStatusTextView = (TextView) view.findViewById(R.id.status);

        mDivider = view.findViewById(R.id.tile_divider);
        if (Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.DASHBOARD_TILEVIEW_DIVIDERS, 0) == 1) {
        mDivider.setVisibility(View.GONE);
        } else {
        mDivider.setVisibility(View.VISIBLE);
        }
        mSwitch = (Switch) view.findViewById(R.id.dashboard_switch);

        setOnClickListener(this);
        setBackgroundResource(R.drawable.dashboard_tile_background);
        setFocusable(true);
        updateDashFont();
    }

    public TextView getTitleTextView() {
        updateDashFont();
        return mTitleTextView;
    }

    public TextView getStatusTextView() {
        updateDashFont();
        return mStatusTextView;
    }

    public ImageView getImageView() {
        return mImageView;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mSwitchToggle != null) {
            mSwitchToggle.resume(getContext());
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mSwitchToggle != null) {
            mSwitchToggle.pause();
        }
    }

    public void setTile(DashboardTile tile) {
        mTile = tile;

        if (mTile.switchControl != null) {
            try {
                Class<?> clazz = getClass().getClassLoader().loadClass(mTile.switchControl);
                Constructor<?> constructor = clazz.getConstructor(Context.class, Switch.class);
                GenericSwitchToggle sw = (GenericSwitchToggle) constructor.newInstance(
                        getContext(), mSwitch);
                mSwitchToggle = sw;
                mSwitchToggle.resume(getContext());
            } catch (ClassNotFoundException
                    | NoSuchMethodException
                    | InvocationTargetException
                    | InstantiationException
                    | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public void setDividerVisibility(boolean visible) {
        mDivider.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    void setColumnSpan(int span) {
        mColSpan = span;
    }

    int getColumnSpan() {
        return mColSpan;
    }

    @Override
    public void onClick(View v) {
        if (mTile.fragment != null) {
            Utils.startWithFragment(getContext(), mTile.fragment, mTile.fragmentArguments, null, 0,
                    mTile.titleRes, mTile.getTitle(getResources()));
        } else if (mTile.intent != null) {
            int numUserHandles = mTile.userHandle.size();
            if (numUserHandles > 1) {
                ProfileSelectDialog.show(((Activity) getContext()).getFragmentManager(), mTile);
            } else if (numUserHandles == 1) {
                getContext().startActivityAsUser(mTile.intent, mTile.userHandle.get(0));
            } else {
                getContext().startActivity(mTile.intent);
            }
        }
    }

    public Switch getSwitchView() {
        return mSwitch;
    }

    private void updateDashFont() {
        final int mDashFontStyle = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.DASHBOARD_FONT_STYLE, FontHelper.FONT_NORMAL);

        getFontStyle(mDashFontStyle);
    }

    public void getFontStyle(int font) {
        switch (font) {
            case FontHelper.FONT_NORMAL:
            default:
                mTitleTextView.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
                mStatusTextView.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
                break;
            case FontHelper.FONT_ITALIC:
                mTitleTextView.setTypeface(Typeface.create("sans-serif", Typeface.ITALIC));
                mStatusTextView.setTypeface(Typeface.create("sans-serif", Typeface.ITALIC));
                break;
            case FontHelper.FONT_BOLD:
                mTitleTextView.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
                mStatusTextView.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
                break;
            case FontHelper.FONT_BOLD_ITALIC:
                mTitleTextView.setTypeface(Typeface.create("sans-serif", Typeface.BOLD_ITALIC));
                mStatusTextView.setTypeface(Typeface.create("sans-serif", Typeface.BOLD_ITALIC));
                break;
            case FontHelper.FONT_LIGHT:
                mTitleTextView.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
                mStatusTextView.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
                break;
            case FontHelper.FONT_LIGHT_ITALIC:
                mTitleTextView.setTypeface(Typeface.create("sans-serif-light", Typeface.ITALIC));
                mStatusTextView.setTypeface(Typeface.create("sans-serif-light", Typeface.ITALIC));
                break;
            case FontHelper.FONT_THIN:
                mTitleTextView.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
                mStatusTextView.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
                break;
            case FontHelper.FONT_THIN_ITALIC:
                mTitleTextView.setTypeface(Typeface.create("sans-serif-thin", Typeface.ITALIC));
                mStatusTextView.setTypeface(Typeface.create("sans-serif-thin", Typeface.ITALIC));
                break;
            case FontHelper.FONT_CONDENSED:
                mTitleTextView.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
                mStatusTextView.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
                break;
            case FontHelper.FONT_CONDENSED_ITALIC:
                mTitleTextView.setTypeface(Typeface.create("sans-serif-condensed", Typeface.ITALIC));
                mStatusTextView.setTypeface(Typeface.create("sans-serif-condensed", Typeface.ITALIC));
                break;
            case FontHelper.FONT_CONDENSED_LIGHT:
                mTitleTextView.setTypeface(Typeface.create("sans-serif-condensed-light", Typeface.NORMAL));
                mStatusTextView.setTypeface(Typeface.create("sans-serif-condensed-light", Typeface.NORMAL));
                break;
            case FontHelper.FONT_CONDENSED_LIGHT_ITALIC:
                mTitleTextView.setTypeface(Typeface.create("sans-serif-condensed-light", Typeface.ITALIC));
                mStatusTextView.setTypeface(Typeface.create("sans-serif-condensed-light", Typeface.ITALIC));
                break;
            case FontHelper.FONT_CONDENSED_BOLD:
                mTitleTextView.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
                mStatusTextView.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
                break;
            case FontHelper.FONT_CONDENSED_BOLD_ITALIC:
                mTitleTextView.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD_ITALIC));
                mStatusTextView.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD_ITALIC));
                break;
            case FontHelper.FONT_MEDIUM:
                mTitleTextView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                mStatusTextView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                break;
            case FontHelper.FONT_MEDIUM_ITALIC:
                mTitleTextView.setTypeface(Typeface.create("sans-serif-medium", Typeface.ITALIC));
                mStatusTextView.setTypeface(Typeface.create("sans-serif-medium", Typeface.ITALIC));
                break;
            case FontHelper.FONT_BLACK:
                mTitleTextView.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));
                mStatusTextView.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));
                break;
            case FontHelper.FONT_BLACK_ITALIC:
                mTitleTextView.setTypeface(Typeface.create("sans-serif-black", Typeface.ITALIC));
                mStatusTextView.setTypeface(Typeface.create("sans-serif-black", Typeface.ITALIC));
                break;
        }
    }
}
