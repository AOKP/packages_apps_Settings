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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v14.preference.PreferenceDialogFragment;
import android.support.v7.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;

public class CustomDialogPreference extends DialogPreference {

    private CustomPreferenceDialogFragment mFragment;

    public CustomDialogPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public CustomDialogPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CustomDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomDialogPreference(Context context) {
        super(context);
    }

    public boolean isDialogOpen() {
        return getDialog() != null && getDialog().isShowing();
    }

    public Dialog getDialog() {
        return mFragment != null ? mFragment.getDialog() : null;
    }

    protected void onPrepareDialogBuilder(AlertDialog.Builder builder,
            DialogInterface.OnClickListener listener) {
    }

    protected void onDialogClosed(boolean positiveResult) {
    }

    protected void onClick(DialogInterface dialog, int which) {
    }

    protected void onBindDialogView(View view) {
    }

    private void setFragment(CustomPreferenceDialogFragment fragment) {
        mFragment = fragment;
    }

    protected boolean onDismissDialog(final DialogInterface dialog, final int which) {
        return true;
    }

    public static class CustomPreferenceDialogFragment extends PreferenceDialogFragment {

        public static CustomPreferenceDialogFragment newInstance(String key) {
            final CustomPreferenceDialogFragment fragment = new CustomPreferenceDialogFragment();
            final Bundle b = new Bundle(1);
            b.putString(ARG_KEY, key);
            fragment.setArguments(b);
            return fragment;
        }

        private CustomDialogPreference getCustomizablePreference() {
            return (CustomDialogPreference) getPreference();
        }

        private class OnDismissListener implements View.OnClickListener {
            private final int mWhich;
            private final DialogInterface mDialog;

            public OnDismissListener(final DialogInterface dialog, final int which) {
                mWhich = which;
                mDialog = dialog;
            }

            @Override
            public void onClick(final View view) {
                CustomPreferenceDialogFragment.this.onClick(mDialog, mWhich);
                if (getCustomizablePreference().onDismissDialog(mDialog, mWhich)) {
                    mDialog.dismiss();
                }
            }
        }

        @Override
        public void onStart() {
            super.onStart();

            if (getDialog() instanceof AlertDialog) {
                final AlertDialog dialog = (AlertDialog) getDialog();
                if (dialog.getButton(Dialog.BUTTON_NEUTRAL) != null) {
                    dialog.getButton(Dialog.BUTTON_NEUTRAL).setOnClickListener(
                            new OnDismissListener(dialog, Dialog.BUTTON_NEUTRAL));
                }
                if (dialog.getButton(Dialog.BUTTON_POSITIVE) != null) {
                    dialog.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener(
                            new OnDismissListener(dialog, Dialog.BUTTON_POSITIVE));
                }
                if (dialog.getButton(Dialog.BUTTON_NEGATIVE) != null) {
                    dialog.getButton(Dialog.BUTTON_NEGATIVE).setOnClickListener(
                            new OnDismissListener(dialog, Dialog.BUTTON_NEGATIVE));
                }
            }
        }

        @Override
        protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
            super.onPrepareDialogBuilder(builder);
            getCustomizablePreference().setFragment(this);
            getCustomizablePreference().onPrepareDialogBuilder(builder, this);
        }

        @Override
        public void onDialogClosed(boolean positiveResult) {
            getCustomizablePreference().onDialogClosed(positiveResult);
        }

        @Override
        protected void onBindDialogView(View view) {
            super.onBindDialogView(view);
            getCustomizablePreference().onBindDialogView(view);
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            super.onClick(dialog, which);
            getCustomizablePreference().onClick(dialog, which);
        }
    }
}
