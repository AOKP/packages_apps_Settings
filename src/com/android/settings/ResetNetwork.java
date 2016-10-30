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
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.internal.telephony.PhoneConstants;
import com.android.settingslib.RestrictedLockUtils;

import java.util.ArrayList;
import java.util.List;

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

/**
 * Confirm and execute a reset of the device's network settings to a clean "just out of the box"
 * state.  Multiple confirmations are required: first, a general "are you sure you want to do this?"
 * prompt, followed by a keyguard pattern trace if the user has defined one, followed by a final
 * strongly-worded "THIS WILL RESET EVERYTHING" prompt.  If at any time the phone is allowed to go
 * to sleep, is locked, et cetera, then the confirmation sequence is abandoned.
 *
 * This is the initial screen.
 */
public class ResetNetwork extends OptionsMenuFragment {
    private static final String TAG = "ResetNetwork";

    // Arbitrary to avoid conficts
    private static final int KEYGUARD_REQUEST = 55;

    private List<SubscriptionInfo> mSubscriptions;

    private View mContentView;
    private Spinner mSubscriptionSpinner;
    private Button mInitiateButton;

    /**
     * Keyguard validation is run using the standard {@link ConfirmLockPattern}
     * component as a subactivity
     * @param request the request code to be returned once confirmation finishes
     * @return true if confirmation launched
     */
    private boolean runKeyguardConfirmation(int request) {
        Resources res = getActivity().getResources();
        return new ChooseLockSettingsHelper(getActivity(), this).launchConfirmationActivity(
                request, res.getText(R.string.reset_network_title));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != KEYGUARD_REQUEST) {
            return;
        }

        // If the user entered a valid keyguard trace, present the final
        // confirmation prompt; otherwise, go back to the initial state.
        if (resultCode == Activity.RESULT_OK) {
            showFinalConfirmation();
        } else {
            establishInitialState();
        }
    }

    private void showFinalConfirmation() {
        Bundle args = new Bundle();
        if (mSubscriptions != null && mSubscriptions.size() > 0) {
            int selectedIndex = mSubscriptionSpinner.getSelectedItemPosition();
            SubscriptionInfo subscription = mSubscriptions.get(selectedIndex);
            args.putInt(PhoneConstants.SUBSCRIPTION_KEY, subscription.getSubscriptionId());
        }
        ((SettingsActivity) getActivity()).startPreferencePanel(ResetNetworkConfirm.class.getName(),
                args, R.string.reset_network_confirm_title, null, null, 0);
    }

    /**
     * If the user clicks to begin the reset sequence, we next require a
     * keyguard confirmation if the user has currently enabled one.  If there
     * is no keyguard available, we simply go to the final confirmation prompt.
     */
    private final Button.OnClickListener mInitiateListener = new Button.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (!runKeyguardConfirmation(KEYGUARD_REQUEST)) {
                showFinalConfirmation();
            }
        }
    };

    /**
     * In its initial state, the activity presents a button for the user to
     * click in order to initiate a confirmation sequence.  This method is
     * called from various other points in the code to reset the activity to
     * this base state.
     *
     * <p>Reinflating views from resources is expensive and prevents us from
     * caching widget pointers, so we use a single-inflate pattern:  we lazy-
     * inflate each view, caching all of the widget pointers we'll need at the
     * time, then simply reuse the inflated views directly whenever we need
     * to change contents.
     */
    private void establishInitialState() {
        mSubscriptionSpinner = (Spinner) mContentView.findViewById(R.id.reset_network_subscription);

        mSubscriptions = SubscriptionManager.from(getActivity()).getActiveSubscriptionInfoList();
        if (mSubscriptions != null && mSubscriptions.size() > 0) {
            // Get the default subscription in the order of data, voice, sms, first up.
            int defaultSubscription = SubscriptionManager.getDefaultDataSubscriptionId();
            if (!SubscriptionManager.isUsableSubIdValue(defaultSubscription)) {
                defaultSubscription = SubscriptionManager.getDefaultVoiceSubscriptionId();
            }
            if (!SubscriptionManager.isUsableSubIdValue(defaultSubscription)) {
                defaultSubscription = SubscriptionManager.getDefaultSmsSubscriptionId();
            }
            if (!SubscriptionManager.isUsableSubIdValue(defaultSubscription)) {
                defaultSubscription = SubscriptionManager.getDefaultSubscriptionId();
            }

            int selectedIndex = 0;
            int size = mSubscriptions.size();
            List<String> subscriptionNames = new ArrayList<>();
            for (SubscriptionInfo record : mSubscriptions) {
                if (record.getSubscriptionId() == defaultSubscription) {
                    // Set the first selected value to the default
                    selectedIndex = subscriptionNames.size();
                }
                String name = record.getDisplayName().toString();
                if (TextUtils.isEmpty(name)) {
                    name = record.getNumber();
                }
                if (TextUtils.isEmpty(name)) {
                    name = record.getCarrierName().toString();
                }
                if (TextUtils.isEmpty(name)) {
                    name = String.format("MCC:%s MNC:%s Slot:%s Id:%s", record.getMcc(),
                            record.getMnc(), record.getSimSlotIndex(), record.getSubscriptionId());
                }
                subscriptionNames.add(name);
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                    android.R.layout.simple_spinner_item, subscriptionNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mSubscriptionSpinner.setAdapter(adapter);
            mSubscriptionSpinner.setSelection(selectedIndex);
            if (mSubscriptions.size() > 1) {
                mSubscriptionSpinner.setVisibility(View.VISIBLE);
            } else {
                mSubscriptionSpinner.setVisibility(View.INVISIBLE);
            }
        } else {
            mSubscriptionSpinner.setVisibility(View.INVISIBLE);
        }
        mInitiateButton = (Button) mContentView.findViewById(R.id.initiate_reset_network);
        mInitiateButton.setOnClickListener(mInitiateListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final UserManager um = UserManager.get(getActivity());
        final EnforcedAdmin admin = RestrictedLockUtils.checkIfRestrictionEnforced(
                getActivity(), UserManager.DISALLOW_NETWORK_RESET, UserHandle.myUserId());
        if (!um.isAdminUser() || RestrictedLockUtils.hasBaseUserRestriction(getActivity(),
                UserManager.DISALLOW_NETWORK_RESET, UserHandle.myUserId())) {
            return inflater.inflate(R.layout.network_reset_disallowed_screen, null);
        } else if (admin != null) {
            View view = inflater.inflate(R.layout.admin_support_details_empty_view, null);
            ShowAdminSupportDetailsDialog.setAdminSupportDetails(getActivity(), view, admin, false);
            view.setVisibility(View.VISIBLE);
            return view;
        }

        mContentView = inflater.inflate(R.layout.reset_network, null);

        establishInitialState();
        return mContentView;
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.RESET_NETWORK;
    }
}
