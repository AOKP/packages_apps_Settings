/*
 * Copyright (C) 2008 The Android Open Source Project
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

import dalvik.system.DexClassLoader;

import java.util.concurrent.Semaphore;

import com.android.internal.widget.AuthentecLoader;
import com.android.internal.widget.LockPatternUtils;

import android.app.Activity;
import android.view.View;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import android.content.Context;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;


public class ChooseLockFinger extends PreferenceActivity {

    private static final String KEY_TOGGLE_UNLOCK_FINGER = "toggle_unlock_finger";
    private static final String KEY_START_ENROLLMENT_WIZARD = "start_enrollment_wizard";

    // result of an operation from a remote intent
    private int miResult;
    private LockPatternUtils mLockPatternUtils;
    private ChooseLockFinger mChooseLockFinger;

    private CheckBoxPreference mToggleUnlockFinger;
    private Preference mStartEnrollmentWizard;
    private boolean mbFingerSetting = false;
    private String msTempPasscode = null;

    private Class<?> AM_STATUS = null;
    private Class<?> TSM = null;
    private AuthentecLoader loader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Always initialize the pointer to NULL at the begining.
        msTempPasscode = null;

        // use the intent's bundle and parameters in it to determine how it was started.
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            msTempPasscode = bundle.getString("temp-passcode");
        }

        // Don't show the tutorial if the user has seen it before.
        mLockPatternUtils = new LockPatternUtils(this);
        mChooseLockFinger = this;

        // Load TSM and AM_STATUS
        loader = AuthentecLoader.getInstance(null);
        AM_STATUS = loader.getAMStatus();
        TSM = loader.getTSM();

    if (loader == null)
        Log.e("ChooseLockFinger", "Authentec Loader - Null");
    else
        Log.w("ChooseLockFinger", "Authentec Loader: " + loader.toString() );

    if (TSM == null )
        Log.e ("ChooseLockFinger", "TSM - Null");
    else
        Log.w ("ChooseLockFinger", "TSM: " + TSM.toString() );

    if (AM_STATUS == null )
        Log.e ("ChooseLockFinger", "AM_STATUS - Nulll");
    else
        Log.w ("ChooseLockFinger", "AM_STATUS: " + AM_STATUS.toString() );


        if (mLockPatternUtils.savedFingerExists()) {
            // Previous enrolled fingers exist.
            mbFingerSetting = true;

            addPreferencesFromResource(R.xml.finger_prefs);
            mToggleUnlockFinger = (CheckBoxPreference) findPreference(KEY_TOGGLE_UNLOCK_FINGER);
            if (mLockPatternUtils.isLockFingerEnabled()) {
                mToggleUnlockFinger.setChecked(true);
            } else {
                mToggleUnlockFinger.setChecked(false);
            }
            mStartEnrollmentWizard = (Preference) findPreference(KEY_START_ENROLLMENT_WIZARD);
            mStartEnrollmentWizard.setTitle(R.string.lockfinger_change_finger_unlock_title);
        } else {
            mbFingerSetting = false;
            startEnrollmentWizard();
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mToggleUnlockFinger) {
            if (mToggleUnlockFinger.isChecked()) {
                startVerification();
            } else {
                // Turn off the unlock finger mode.
                mLockPatternUtils.setLockFingerEnabled(false);
                mToggleUnlockFinger.setChecked(false);
                // Destroy the activity.
                mChooseLockFinger.finish();
            }
        } else if (preference == mStartEnrollmentWizard) {
            startEnrollmentWizard();
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    // The toast() function is provided to allow non-UI thread code to
    // conveniently raise a toast...
    private void toast(final String s)
    {
        runOnUiThread(new Runnable() {
            public void run()
            {
                Toast.makeText(mChooseLockFinger, s, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startEnrollmentWizard()
    {
        // launch a thread for the enrollment wizard
        new Thread(new Runnable() {
            public void run() {
                try {
                    Class<?> partTypes[] = new Class[1];
                    Object argList[] = new Object[1];

                    partTypes[0] = Context.class;
                    argList[0] = ChooseLockFinger.this;
                    Object TSMi = TSM.getMethod("LAP", partTypes).invoke(null, argList);

                    if (msTempPasscode != null) {
                        partTypes[0] = String.class;
                        argList[0] = msTempPasscode;
                        argList[0] = (String) TSM.getMethod("Hexify", partTypes).invoke(null, argList);

                        TSM.getMethod("usingPasscode", partTypes).invoke(TSMi, argList);

                        TSM.getMethod("enroll").invoke(TSMi);
                        //miResult = TSM.LAP(ChooseLockFinger.this).usingPasscode(TSM.Hexify(msTempPasscode)).enroll().exec();
                    } else {
                        partTypes[0] = String.class;
                        argList[0] = "_classicEnroll";
                        TSM.getMethod("addFunction", partTypes).invoke(TSMi, argList);
                        //miResult = TSM.LAP(ChooseLockFinger.this).addFunction("_classicEnroll").exec();
                    }

                    miResult = (Integer) TSM.getMethod("exec").invoke(TSMi);
                    TSMi = null;

                    // process the returned result
                    if (miResult == AM_STATUS.getDeclaredField("eAM_STATUS_OK").getInt(AM_STATUS)) {
                        toast(getString(R.string.lockfinger_enrollment_succeeded_toast));
                        runOnUiThread(new Runnable() {
                            public void run() {
                                mLockPatternUtils.setLockFingerEnabled(true);
                                if (mbFingerSetting) {
                                    mToggleUnlockFinger.setEnabled(true);
                                    mToggleUnlockFinger.setChecked(true);
                                    mStartEnrollmentWizard.setTitle(R.string.lockfinger_change_finger_unlock_title);
                                }
                            }
                        });
                    } else if (miResult == AM_STATUS.getDeclaredField("eAM_STATUS_LIBRARY_NOT_AVAILABLE").getInt(AM_STATUS)) {
                        toast(getString(R.string.lockfinger_tsm_library_not_available_toast));
                    } else if (miResult == AM_STATUS.getDeclaredField("eAM_STATUS_USER_CANCELED").getInt(AM_STATUS)) {
                        toast(getString(R.string.lockfinger_enrollment_canceled_toast));
                    } else if (miResult == AM_STATUS.getDeclaredField("eAM_STATUS_TIMEOUT").getInt(AM_STATUS)) {
                        toast(getString(R.string.lockfinger_enrollment_timeout_toast));
                    } else if (miResult == AM_STATUS.getDeclaredField("eAM_STATUS_UNKNOWN_ERROR").getInt(AM_STATUS)) {
                        toast(getString(R.string.lockfinger_enrollment_unknown_error_toast));
                    } else if (miResult == AM_STATUS.getDeclaredField("eAM_STATUS_DATABASE_FULL").getInt(AM_STATUS)) {
                        toast(getString(R.string.lockfinger_enrollment_database_full));
                    } else {
                        toast(getString(R.string.lockfinger_enrollment_failure_default_toast, miResult));
                    }
                } catch (Exception e) {
                    Log.e ("ChooseLockFinger", "Exception occured: " + e.toString() );
                    e.printStackTrace();
                    }

                // Destroy the activity.
                mChooseLockFinger.finish();
            }
        }).start();
    }

    private void startVerification()
    {
        // launch a thread for the verification
        new Thread(new Runnable() {
            public void run() {
                try {
                    //miResult = TSM.LAP(ChooseLockFinger.this).verify().viaGfxScreen("lap-verify").exec();
                    Class<?> partTypes[] = new Class[1];
                    Object argList[] = new Object[1];

                    partTypes[0] = Context.class;
                    argList[0] = ChooseLockFinger.this;
                    Object TSMi = TSM.getMethod("LAP", partTypes).invoke(null, argList);

                    TSM.getMethod("verify").invoke(TSMi);

                    partTypes[0] = String.class;
                    argList[0] = "lap-verify";
                    TSM.getMethod("viaGfxScreen", partTypes).invoke(TSMi, argList);

                    miResult = (Integer) TSM.getMethod("exec").invoke(TSMi);
                    TSMi = null;

                    // process the returned result
                    if (miResult == AM_STATUS.getDeclaredField("eAM_STATUS_OK").getInt(AM_STATUS)) {
                        // Turn on the fingerprint unlock mode with the previously enrolled finger(s).
                        runOnUiThread(new Runnable() {
                            public void run() {
                                mLockPatternUtils.setLockFingerEnabled(true);
                                mToggleUnlockFinger.setChecked(true);
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                mToggleUnlockFinger.setChecked(false);
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e ("ChooseLockFinger", "Exception occured: " + e.toString());
                    e.printStackTrace();
                }

                // Destroy the activity.
                mChooseLockFinger.finish();
             }
        }).start();
    }
}
