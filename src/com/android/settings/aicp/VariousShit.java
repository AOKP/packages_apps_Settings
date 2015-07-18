/*
 * Copyright (C) 2013 The ChameleonOS Open Source Project
 * Copyright (C) 2014 The Android Ice Cold Project
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

package com.android.settings.aicp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.UserHandle;
import android.preference.CheckBoxPreference;
import android.preference.SwitchPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.util.AbstractAsyncSuCMDProcessor;
import com.android.settings.util.CMDProcessor;
import com.android.settings.util.Helpers;
import com.android.settings.Utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.android.internal.util.cm.QSUtils;

/**
 * LAB files borrowed from excellent ChameleonOS for AICP
 */
public class VariousShit extends SettingsPreferenceFragment
        implements OnSharedPreferenceChangeListener,
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "VariousShit";

    private static final String KEY_LOCKCLOCK = "lock_clock";

    private static final int REQUEST_PICK_BOOT_ANIMATION = 201;

    private static final String PREF_CUSTOM_BOOTANIM = "custom_bootanimation";
    private static final String BOOTANIMATION_SYSTEM_PATH = "/system/media/bootanimation.zip";

    private static final String KEY_HIDDEN_SHIT = "hidden_shit";
    private static final String KEY_HIDDEN_SHIT_UNLOCKED = "hidden_shit_unlocked";
    private static final String KEY_HIDDEN_IMG = "hidden_img";
    private static final String KEY_HIDDEN_YOGA = "hidden_anim";

    private static final String TORCH_CATEGORY = "torch_category";
    private static final String DISABLE_TORCH_ON_SCREEN_OFF = "disable_torch_on_screen_off";
    private static final String DISABLE_TORCH_ON_SCREEN_OFF_DELAY = "disable_torch_on_screen_off_delay";

    private static final String SELINUX = "selinux";

    private static final String CARRIERLABEL_ON_LOCKSCREEN="lock_screen_hide_carrier";

    // Package name of the yoga
    public static final String YOGA_PACKAGE_NAME = "com.android.settings";
    // Intent for launching the yoga actvity
    public static Intent INTENT_YOGA = new Intent(Intent.ACTION_MAIN)
            .setClassName(YOGA_PACKAGE_NAME, YOGA_PACKAGE_NAME + ".aicp.HiddenAnimActivity");

    private static final String BACKUP_PATH = new File(Environment
            .getExternalStorageDirectory(), "/AICP_ota").getAbsolutePath();

    // Package name of the cLock app
    public static final String LOCKCLOCK_PACKAGE_NAME = "com.cyanogenmod.lockclock";

    private PreferenceScreen mVariousShitScreen;
    private Preference mCustomBootAnimation;
    private ImageView mView;
    private TextView mError;
    private AlertDialog mCustomBootAnimationDialog;
    private AnimationDrawable mAnimationPart1;
    private AnimationDrawable mAnimationPart2;
    private String mErrormsg;
    private String mBootAnimationPath;
    private SwitchPreference mTorchOff;
    private ListPreference mTorchOffDelay;
    private PreferenceCategory mTorchCategory;
    private Preference mLockClock;
    private SwitchPreference mSelinux;
    private SwitchPreference mCarrierLabelOnLockScreen;

    private Preference mHiddenShit;
    private PreferenceScreen mHiddenImg;
    private CheckBoxPreference mHiddenShitUnlocked;
    long[] mHits = new long[3];

    private final ArrayList<Preference> mAllPrefs = new ArrayList<Preference>();
    private final ArrayList<CheckBoxPreference> mResetCbPrefs
            = new ArrayList<CheckBoxPreference>();

    private Context mContext;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.aicp_various_shit);

        ContentResolver resolver = getActivity().getContentResolver();
        PreferenceScreen prefSet = getPreferenceScreen();
        PackageManager pm = getPackageManager();
        Resources res = getResources();
        mContext = getActivity();
        Activity activity = getActivity();

        mVariousShitScreen = (PreferenceScreen) findPreference("various_shit_screen");

        // cLock app check
        mLockClock = (Preference) findPreference(KEY_LOCKCLOCK);
        if (!Helpers.isPackageInstalled(LOCKCLOCK_PACKAGE_NAME, pm)) {
            prefSet.removePreference(mLockClock);
        }

        // Hidden shit
        mHiddenShit = (Preference) findPreference(KEY_HIDDEN_SHIT);
        mHiddenImg = (PreferenceScreen) findPreference(KEY_HIDDEN_IMG);
        mAllPrefs.add(mHiddenShit);
        mHiddenShitUnlocked =
                findAndInitCheckboxPref(KEY_HIDDEN_SHIT_UNLOCKED);
        mHiddenShitUnlocked.setOnPreferenceChangeListener(this);

        boolean hiddenShitOpened = Settings.System.getInt(
                getActivity().getContentResolver(),
                Settings.System.HIDDEN_SHIT, 0) == 1;
        mHiddenShitUnlocked.setChecked(hiddenShitOpened);

        if (hiddenShitOpened) {
            mVariousShitScreen.removePreference(mHiddenShit);
        } else {
            mVariousShitScreen.removePreference(mHiddenShitUnlocked);
            mVariousShitScreen.removePreference(mHiddenImg);
        }

        // Custom bootanimation
        mCustomBootAnimation = findPreference(PREF_CUSTOM_BOOTANIM);

        resetBootAnimation();

        // Torch crap
        mTorchCategory = (PreferenceCategory) findPreference(TORCH_CATEGORY);
        mTorchOff = (SwitchPreference) findPreference(DISABLE_TORCH_ON_SCREEN_OFF);
        mTorchOffDelay = (ListPreference) findPreference(DISABLE_TORCH_ON_SCREEN_OFF_DELAY);
        int torchOffDelay = Settings.System.getIntForUser(resolver,
                Settings.System.DISABLE_TORCH_ON_SCREEN_OFF_DELAY, 10, UserHandle.USER_CURRENT);
        mTorchOffDelay.setValue(String.valueOf(torchOffDelay));
        mTorchOffDelay.setSummary(mTorchOffDelay.getEntry());
        mTorchOffDelay.setOnPreferenceChangeListener(this);

        if (!QSUtils.deviceSupportsFlashLight(activity)) {
            prefSet.removePreference(mTorchCategory);
        }

        //SELinux
        mSelinux = (SwitchPreference) findPreference(SELINUX);
        mSelinux.setOnPreferenceChangeListener(this);

        if (CMDProcessor.runSuCommand("getenforce").getStdout().contains("Enforcing")) {
            mSelinux.setChecked(true);
            mSelinux.setSummary(R.string.selinux_enforcing_title);
        } else {
            mSelinux.setChecked(false);
            mSelinux.setSummary(R.string.selinux_permissive_title);
        }

        //CarrierLabel on LockScreen
        mCarrierLabelOnLockScreen = (SwitchPreference) findPreference(CARRIERLABEL_ON_LOCKSCREEN);
        if (!Utils.isWifiOnly(getActivity())) {
            mCarrierLabelOnLockScreen.setOnPreferenceChangeListener(this);

            boolean hideCarrierLabelOnLS = Settings.System.getInt(
                    getActivity().getContentResolver(),
                    Settings.System.LOCK_SCREEN_HIDE_CARRIER, 0) == 1;
            mCarrierLabelOnLockScreen.setChecked(hideCarrierLabelOnLS);
        } else {
            prefSet.removePreference(mCarrierLabelOnLockScreen);
        }

    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mHiddenShit) {
            System.arraycopy(mHits, 1, mHits, 0, mHits.length-1);
            mHits[mHits.length-1] = SystemClock.uptimeMillis();
            if ((Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.HIDDEN_SHIT, 0) == 0) &&
                    (mHits[0] >= (SystemClock.uptimeMillis()-500))) {
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.HIDDEN_SHIT, 1);
                Toast.makeText(getActivity(),
                        R.string.hidden_shit_toast,
                        Toast.LENGTH_LONG).show();
                getPreferenceScreen().removePreference(mHiddenShit);
                addPreference(mHiddenShitUnlocked);
                mHiddenShitUnlocked.setChecked(true);
                addPreference(mHiddenImg);
            }
        } else if (preference == mHiddenImg) {
            System.arraycopy(mHits, 1, mHits, 0, mHits.length-1);
            mHits[mHits.length-1] = SystemClock.uptimeMillis();
            if  (mHits[0] >= (SystemClock.uptimeMillis()-500)) {
                startActivity(INTENT_YOGA);
            }
        } else if (preference == mCustomBootAnimation) {
            openBootAnimationDialog();
            return true;
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        final String key = preference.getKey();
        if (preference == mHiddenShitUnlocked) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.HIDDEN_SHIT,
                    (Boolean) objValue ? 1 : 0);
            return true;
        } else if (preference == mTorchOffDelay) {
            int torchOffDelay = Integer.valueOf((String) objValue);
            int index = mTorchOffDelay.findIndexOfValue((String) objValue);
            Settings.System.putIntForUser(getActivity().getContentResolver(),
                    Settings.System.DISABLE_TORCH_ON_SCREEN_OFF_DELAY, torchOffDelay, UserHandle.USER_CURRENT);
            mTorchOffDelay.setSummary(mTorchOffDelay.getEntries()[index]);
            return true;
        } else if (preference == mSelinux) {
            if (objValue.toString().equals("true")) {
                CMDProcessor.runSuCommand("setenforce 1");
                mSelinux.setSummary(R.string.selinux_enforcing_title);
            } else if (objValue.toString().equals("false")) {
                CMDProcessor.runSuCommand("setenforce 0");
                mSelinux.setSummary(R.string.selinux_permissive_title);
            }
            return true;
        } else if (preference == mCarrierLabelOnLockScreen) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.LOCK_SCREEN_HIDE_CARRIER,
                    (Boolean) objValue ? 1 : 0);
            Helpers.restartSystemUI();
            return true;
        }
        return false;
    }

    private void addPreference(Preference preference) {
        getPreferenceScreen().addPreference(preference);
        preference.setOnPreferenceChangeListener(this);
        mAllPrefs.add(preference);
    }

    private CheckBoxPreference findAndInitCheckboxPref(String key) {
        CheckBoxPreference pref = (CheckBoxPreference) findPreference(key);
        if (pref == null) {
            throw new IllegalArgumentException("Cannot find preference with key = " + key);
        }
        mAllPrefs.add(pref);
        mResetCbPrefs.add(pref);
        return pref;
    }

    /**
     * Resets boot animation path. Essentially clears temporary-set boot animation
     * set by the user from the dialog.
     *
     * @return returns true if a boot animation exists (user or system). false otherwise.
     */
    private boolean resetBootAnimation() {
        boolean bootAnimationExists = false;
        if (new File(BOOTANIMATION_SYSTEM_PATH).exists()) {
            mBootAnimationPath = BOOTANIMATION_SYSTEM_PATH;
            bootAnimationExists = true;
        } else {
            mBootAnimationPath = "";
        }
        return bootAnimationExists;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_PICK_BOOT_ANIMATION) {
                if (data == null) {
                    //Nothing returned by user, probably pressed back button in file manager
                    return;
                }
                mBootAnimationPath = data.getData().getPath();
                openBootAnimationDialog();
            }
        }
    }

    private void openBootAnimationDialog() {
        Log.e(TAG, "boot animation path: " + mBootAnimationPath);
        if (mCustomBootAnimationDialog != null) {
            mCustomBootAnimationDialog.cancel();
            mCustomBootAnimationDialog = null;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.bootanimation_preview);
        if (!mBootAnimationPath.isEmpty()
                && (!BOOTANIMATION_SYSTEM_PATH.equalsIgnoreCase(mBootAnimationPath))) {
            builder.setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    installBootAnim(dialog, mBootAnimationPath);
                    resetBootAnimation();
                }
            });
        }
        builder.setNeutralButton(R.string.set_custom_bootanimation,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        PackageManager packageManager = getActivity().getPackageManager();
                        Intent test = new Intent(Intent.ACTION_GET_CONTENT);
                        test.setType("file/*");
                        List<ResolveInfo> list = packageManager.queryIntentActivities(test,
                                PackageManager.GET_ACTIVITIES);
                        if (!list.isEmpty()) {
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
                            intent.setType("file/*");
                            startActivityForResult(intent, REQUEST_PICK_BOOT_ANIMATION);
                        } else {
                            //No app installed to handle the intent - file explorer required
                            Toast.makeText(mContext, R.string.install_file_manager_error,
                                    Toast.LENGTH_SHORT).show();
                        }

                    }
                });
        builder.setNegativeButton(com.android.internal.R.string.cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        resetBootAnimation();
                        dialog.dismiss();
                    }
                });
        LayoutInflater inflater =
                (LayoutInflater) getActivity()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.dialog_bootanimation_preview,
                (ViewGroup) getActivity()
                        .findViewById(R.id.bootanimation_layout_root));
        mError = (TextView) layout.findViewById(R.id.textViewError);
        mView = (ImageView) layout.findViewById(R.id.imageViewPreview);
        mView.setVisibility(View.GONE);
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        mView.setLayoutParams(new LinearLayout.LayoutParams(size.x / 2, size.y / 2));
        mError.setText(R.string.creating_preview);
        builder.setView(layout);
        mCustomBootAnimationDialog = builder.create();
        mCustomBootAnimationDialog.setOwnerActivity(getActivity());
        mCustomBootAnimationDialog.show();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                createPreview(mBootAnimationPath);
            }
        });
        thread.start();
    }

    private void createPreview(String path) {
        File zip = new File(path);
        ZipFile zipfile = null;
        String desc = "";
        InputStream inputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        try {
            zipfile = new ZipFile(zip);
            ZipEntry ze = zipfile.getEntry("desc.txt");
            inputStream = zipfile.getInputStream(ze);
            inputStreamReader = new InputStreamReader(inputStream);
            StringBuilder sb = new StringBuilder(0);
            bufferedReader = new BufferedReader(inputStreamReader);
            String read = bufferedReader.readLine();
            while (read != null) {
                sb.append(read);
                sb.append('\n');
                read = bufferedReader.readLine();
            }
            desc = sb.toString();
        } catch (Exception handleAllException) {
            mErrormsg = getActivity().getString(R.string.error_reading_zip_file);
            errorHandler.sendEmptyMessage(0);
            return;
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (IOException e) {
                // we tried
            }
            try {
                if (inputStreamReader != null) {
                    inputStreamReader.close();
                }
            } catch (IOException e) {
                // we tried
            }
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                // moving on...
            }
        }

        String[] info = desc.replace("\\r", "").split("\\n");
        // ignore first two ints height and width
        int delay = Integer.parseInt(info[0].split(" ")[2]);
        String partName1 = info[1].split(" ")[3];
        String partName2;
        try {
            if (info.length > 2) {
                partName2 = info[2].split(" ")[3];
            } else {
                partName2 = "";
            }
        } catch (Exception e) {
            partName2 = "";
        }

        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inSampleSize = 4;
        mAnimationPart1 = new AnimationDrawable();
        mAnimationPart2 = new AnimationDrawable();
        try {
            for (Enumeration<? extends ZipEntry> enumeration = zipfile.entries();
                 enumeration.hasMoreElements(); ) {
                ZipEntry entry = enumeration.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                String partname = entry.getName().split("/")[0];
                if (partName1.equalsIgnoreCase(partname)) {
                    InputStream partOneInStream = null;
                    try {
                        partOneInStream = zipfile.getInputStream(entry);
                        mAnimationPart1.addFrame(new BitmapDrawable(getResources(),
                                BitmapFactory.decodeStream(partOneInStream,
                                        null, opt)), delay);
                    } finally {
                        if (partOneInStream != null) {
                            partOneInStream.close();
                        }
                    }
                } else if (partName2.equalsIgnoreCase(partname)) {
                    InputStream partTwoInStream = null;
                    try {
                        partTwoInStream = zipfile.getInputStream(entry);
                        mAnimationPart2.addFrame(new BitmapDrawable(getResources(),
                                BitmapFactory.decodeStream(partTwoInStream,
                                        null, opt)), delay);
                    } finally {
                        if (partTwoInStream != null) {
                            partTwoInStream.close();
                        }
                    }
                }
            }
        } catch (IOException e1) {
            mErrormsg = getActivity().getString(R.string.error_creating_preview);
            errorHandler.sendEmptyMessage(0);
            return;
        }

        if (!partName2.isEmpty()) {
            Log.d(TAG, "Multipart Animation");
            mAnimationPart1.setOneShot(false);
            mAnimationPart2.setOneShot(false);
            mAnimationPart1.setOnAnimationFinishedListener(
                    new AnimationDrawable.OnAnimationFinishedListener() {
                        @Override
                        public void onAnimationFinished() {
                            Log.d(TAG, "First part finished");
                            mView.setImageDrawable(mAnimationPart2);
                            mAnimationPart1.stop();
                            mAnimationPart2.start();
                        }
                    });
        } else {
            mAnimationPart1.setOneShot(false);
        }
        finishedHandler.sendEmptyMessage(0);
    }

    private Handler errorHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            mView.setVisibility(View.GONE);
            mError.setText(mErrormsg);
        }
    };

    private Handler finishedHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            mView.setImageDrawable(mAnimationPart1);
            mView.setVisibility(View.VISIBLE);
            mError.setVisibility(View.GONE);
            mAnimationPart1.start();
        }
    };

    private void installBootAnim(DialogInterface dialog, String bootAnimationPath) {
        DateFormat dateFormat = new SimpleDateFormat("ddMMyyyy_HHmmss");
        Date date = new Date();
        String current = (dateFormat.format(date));
        new AbstractAsyncSuCMDProcessor() {
            @Override
            protected void onPostExecute(String result) {
            }
        }.execute("mount -o rw,remount /system",
                "cp -f /system/media/bootanimation.zip " + BACKUP_PATH + "/bootanimation_backup_" + current + ".zip",
                "cp -f " + bootAnimationPath + " /system/media/bootanimation.zip",
                "chmod 644 /system/media/bootanimation.zip",
                "mount -o ro,remount /system");
    }
}

