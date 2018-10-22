/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.security;

import static com.android.settings.security.SecuritySettings.UNIFY_LOCK_CONFIRM_DEVICE_REQUEST;
import static com.android.settings.security.SecuritySettings.UNIFY_LOCK_CONFIRM_PROFILE_REQUEST;
import static com.android.settings.security.SecuritySettings.UNUNIFY_LOCK_CONFIRM_DEVICE_REQUEST;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.password.ChooseLockGeneric;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.core.AbstractPreferenceController;

public class LockUnificationPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String KEY_UNIFICATION = "unification";

    private static final int MY_USER_ID = UserHandle.myUserId();

    private final UserManager mUm;
    private final LockPatternUtils mLockPatternUtils;
    private final int mProfileChallengeUserId;
    private final SecuritySettings mHost;

    private RestrictedSwitchPreference mUnifyProfile;


    private String mCurrentDevicePassword;
    private String mCurrentProfilePassword;

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mUnifyProfile = (RestrictedSwitchPreference) screen.findPreference(KEY_UNIFICATION);
    }

    public LockUnificationPreferenceController(Context context, SecuritySettings host) {
        super(context);
        mHost = host;
        mUm = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mLockPatternUtils = FeatureFactory.getFactory(context)
                .getSecurityFeatureProvider()
                .getLockPatternUtils(context);
        mProfileChallengeUserId = Utils.getManagedProfileId(mUm, MY_USER_ID);
    }

    @Override
    public boolean isAvailable() {
        final boolean allowSeparateProfileChallenge =
                mProfileChallengeUserId != UserHandle.USER_NULL
                        && mLockPatternUtils.isSeparateProfileChallengeAllowed(
                        mProfileChallengeUserId);
        return allowSeparateProfileChallenge;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_UNIFICATION;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        if (Utils.startQuietModeDialogIfNecessary(mContext, mUm, mProfileChallengeUserId)) {
            return false;
        }
        if ((Boolean) value) {
            final boolean compliantForDevice =
                    (mLockPatternUtils.getKeyguardStoredPasswordQuality(mProfileChallengeUserId)
                            >= DevicePolicyManager.PASSWORD_QUALITY_SOMETHING
                            && mLockPatternUtils.isSeparateProfileChallengeAllowedToUnify(
                            mProfileChallengeUserId));
            UnificationConfirmationDialog dialog =
                    UnificationConfirmationDialog.newInstance(compliantForDevice);
            dialog.show(mHost);
        } else {
            final String title = mContext.getString(R.string.unlock_set_unlock_launch_picker_title);
            final ChooseLockSettingsHelper helper =
                    new ChooseLockSettingsHelper(mHost.getActivity(), mHost);
            if (!helper.launchConfirmationActivity(
                    UNUNIFY_LOCK_CONFIRM_DEVICE_REQUEST,
                    title, true /* returnCredentials */, MY_USER_ID)) {
                ununifyLocks();
            }
        }
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        if (mUnifyProfile != null) {
            final boolean separate =
                    mLockPatternUtils.isSeparateProfileChallengeEnabled(mProfileChallengeUserId);
            mUnifyProfile.setChecked(!separate);
            if (separate) {
                mUnifyProfile.setDisabledByAdmin(RestrictedLockUtils.checkIfRestrictionEnforced(
                        mContext, UserManager.DISALLOW_UNIFIED_PASSWORD,
                        mProfileChallengeUserId));
            }
        }
    }

    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == UNUNIFY_LOCK_CONFIRM_DEVICE_REQUEST
                && resultCode == Activity.RESULT_OK) {
            ununifyLocks();
            return true;
        } else if (requestCode == UNIFY_LOCK_CONFIRM_DEVICE_REQUEST
                && resultCode == Activity.RESULT_OK) {
            mCurrentDevicePassword =
                    data.getStringExtra(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD);
            launchConfirmProfileLockForUnification();
            return true;
        } else if (requestCode == UNIFY_LOCK_CONFIRM_PROFILE_REQUEST
                && resultCode == Activity.RESULT_OK) {
            mCurrentProfilePassword =
                    data.getStringExtra(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD);
            unifyLocks();
            return true;
        }
        return false;
    }

    private void ununifyLocks() {
        final Bundle extras = new Bundle();
        extras.putInt(Intent.EXTRA_USER_ID, mProfileChallengeUserId);
        new SubSettingLauncher(mContext)
                .setDestination(ChooseLockGeneric.ChooseLockGenericFragment.class.getName())
                    .setTitle(R.string.lock_settings_picker_title_profile)
                .setSourceMetricsCategory(mHost.getMetricsCategory())
                .setArguments(extras)
                .launch();
    }

    void launchConfirmDeviceLockForUnification() {
        final String title = mContext.getString(
                R.string.unlock_set_unlock_launch_picker_title);
        final ChooseLockSettingsHelper helper =
                new ChooseLockSettingsHelper(mHost.getActivity(), mHost);
        if (!helper.launchConfirmationActivity(
                UNIFY_LOCK_CONFIRM_DEVICE_REQUEST, title, true, MY_USER_ID)) {
            launchConfirmProfileLockForUnification();
        }
    }

    private void launchConfirmProfileLockForUnification() {
        final String title = mContext.getString(
                R.string.unlock_set_unlock_launch_picker_title_profile);
        final ChooseLockSettingsHelper helper =
                new ChooseLockSettingsHelper(mHost.getActivity(), mHost);
        if (!helper.launchConfirmationActivity(
                UNIFY_LOCK_CONFIRM_PROFILE_REQUEST, title, true, mProfileChallengeUserId)) {
            unifyLocks();
            // TODO: update relevant prefs.
            // createPreferenceHierarchy();
        }
    }

    private void unifyLocks() {
        int profileQuality =
                mLockPatternUtils.getKeyguardStoredPasswordQuality(mProfileChallengeUserId);
        if (profileQuality == DevicePolicyManager.PASSWORD_QUALITY_SOMETHING) {
            byte patternSize = mLockPatternUtils.getLockPatternSize(MY_USER_ID);
            mLockPatternUtils.saveLockPattern(
                    LockPatternUtils.stringToPattern(mCurrentProfilePassword, patternSize),
                    mCurrentDevicePassword, MY_USER_ID);
        } else {
            mLockPatternUtils.saveLockPassword(
                    mCurrentProfilePassword, mCurrentDevicePassword,
                    profileQuality, MY_USER_ID);
        }
        mLockPatternUtils.setSeparateProfileChallengeEnabled(mProfileChallengeUserId, false,
                mCurrentProfilePassword);
        final boolean profilePatternVisibility =
                mLockPatternUtils.isVisiblePatternEnabled(mProfileChallengeUserId);
        mLockPatternUtils.setVisiblePatternEnabled(profilePatternVisibility, MY_USER_ID);
        mCurrentDevicePassword = null;
        mCurrentProfilePassword = null;
    }

    void unifyUncompliantLocks() {
        mLockPatternUtils.setSeparateProfileChallengeEnabled(mProfileChallengeUserId, false,
                mCurrentProfilePassword);
        new SubSettingLauncher(mContext)
                .setDestination(ChooseLockGeneric.ChooseLockGenericFragment.class.getName())
                .setTitle(R.string.lock_settings_picker_title)
                .setSourceMetricsCategory(mHost.getMetricsCategory())
                .launch();
    }

}
