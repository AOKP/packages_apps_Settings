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

package com.android.settings.deviceinfo.imei;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.TtsSpan;

import com.android.internal.telephony.PhoneConstants;
import com.android.settings.R;

import java.util.List;

public class ImeiInfoDialogController {

    @VisibleForTesting
    static final int ID_PRL_VERSION_VALUE = R.id.prl_version_value;
    private static final int ID_MIN_NUMBER_LABEL = R.id.min_number_label;
    @VisibleForTesting
    static final int ID_MIN_NUMBER_VALUE = R.id.min_number_value;
    @VisibleForTesting
    static final int ID_MEID_NUMBER_VALUE = R.id.meid_number_value;
    @VisibleForTesting
    static final int ID_IMEI_VALUE = R.id.imei_value;
    @VisibleForTesting
    static final int ID_IMEI_SV_VALUE = R.id.imei_sv_value;
    @VisibleForTesting
    static final int ID_CDMA_SETTINGS = R.id.cdma_settings;
    @VisibleForTesting
    static final int ID_GSM_SETTINGS = R.id.gsm_settings;

    private static CharSequence getTextAsDigits(CharSequence text) {
        if (TextUtils.isDigitsOnly(text)) {
            final Spannable spannable = new SpannableStringBuilder(text);
            final TtsSpan span = new TtsSpan.DigitsBuilder(text.toString()).build();
            spannable.setSpan(span, 0, spannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            text = spannable;
        }
        return text;
    }

    private final ImeiInfoDialogFragment mDialog;
    private final TelephonyManager mTelephonyManager;
    private final SubscriptionInfo mSubscriptionInfo;
    private final int mSlotId;

    public ImeiInfoDialogController(@NonNull ImeiInfoDialogFragment dialog, int slotId) {
        mDialog = dialog;
        mSlotId = slotId;
        final Context context = dialog.getContext();
        mTelephonyManager = (TelephonyManager) context.getSystemService(
                Context.TELEPHONY_SERVICE);
        mSubscriptionInfo = getSubscriptionInfo(context, slotId);
    }

    /**
     * Sets IMEI/MEID information based on whether the device is CDMA or GSM.
     */
    public void populateImeiInfo() {
        if (mTelephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA) {
            updateDialogForCdmaPhone();
        } else {
            updateDialogForGsmPhone();
        }
    }

    private void updateDialogForCdmaPhone() {
        final Resources res = mDialog.getContext().getResources();
        mDialog.setText(ID_MEID_NUMBER_VALUE,
                mSubscriptionInfo != null ? getMeid() : "");
        mDialog.setText(ID_MIN_NUMBER_VALUE,
                mSubscriptionInfo != null ? mTelephonyManager.getCdmaMin(
                        mSubscriptionInfo.getSubscriptionId()) : "");

        if (res.getBoolean(R.bool.config_msid_enable)) {
            mDialog.setText(ID_MIN_NUMBER_LABEL,
                    res.getString(R.string.status_msid_number));
        }

        mDialog.setText(ID_PRL_VERSION_VALUE, getCdmaPrlVersion());

        if (mSubscriptionInfo != null && isCdmaLteEnabled()) {
            // Show IMEI for LTE device
            mDialog.setText(ID_IMEI_VALUE,
                    getTextAsDigits(mTelephonyManager.getImei(mSlotId)));
            mDialog.setText(ID_IMEI_SV_VALUE,
                    getTextAsDigits(mTelephonyManager.getDeviceSoftwareVersion(mSlotId)));
        } else {
            // device is not GSM/UMTS, do not display GSM/UMTS features
            mDialog.removeViewFromScreen(ID_GSM_SETTINGS);
        }
    }

    private void updateDialogForGsmPhone() {
        mDialog.setText(ID_IMEI_VALUE,
                mSubscriptionInfo != null ?
                getTextAsDigits(mTelephonyManager.getImei(mSlotId)) : "");
        mDialog.setText(ID_IMEI_SV_VALUE,
                mSubscriptionInfo != null ?
                getTextAsDigits(mTelephonyManager.
                        getDeviceSoftwareVersion(mSlotId)) : "");
        // device is not CDMA, do not display CDMA features
        mDialog.removeViewFromScreen(ID_CDMA_SETTINGS);
    }

    private SubscriptionInfo getSubscriptionInfo(Context context, int slotId) {
        final List<SubscriptionInfo> subscriptionInfoList = SubscriptionManager.from(context)
                .getActiveSubscriptionInfoList();
        if (subscriptionInfoList == null) {
            return null;
        }
        for (SubscriptionInfo info : subscriptionInfoList) {
            if (slotId == info.getSimSlotIndex()) {
                return info;
            }
        }
        return null;
    }

    @VisibleForTesting
    String getCdmaPrlVersion() {
        return mTelephonyManager.getCdmaPrlVersion();
    }

    @VisibleForTesting
    boolean isCdmaLteEnabled() {
        return mTelephonyManager.getLteOnCdmaMode(mSubscriptionInfo.getSubscriptionId())
                == PhoneConstants.LTE_ON_CDMA_TRUE;
    }

    @VisibleForTesting
    String getMeid() {
        return mTelephonyManager.getMeid(mSlotId);
    }
}
