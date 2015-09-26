package com.android.settings.aicp.utils;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

import com.android.settings.util.CMDProcessor;

public class OnBoot extends BroadcastReceiver {

    Context settingsContext = null;
    private static final String TAG = "DU_onboot";
    Boolean mSetupRunning = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> procInfos = activityManager.getRunningAppProcesses();
        for(int i = 0; i < procInfos.size(); i++)
        {
            if(procInfos.get(i).processName.equals("com.google.android.setupwizard")) {
                mSetupRunning = true;
            }
        }
        if(!mSetupRunning) {
            if(Settings.System.getIntForUser(context.getContentResolver(),
                        Settings.System.SELINUX_SWITCH_ONBOOT, 0, UserHandle.USER_CURRENT) == 1) {
                if(Settings.System.getIntForUser(context.getContentResolver(),
                        Settings.System.SELINUX_SWITCH_STATE, 1, UserHandle.USER_CURRENT) == 1) {
                    CMDProcessor.runSuCommand("setenforce 1");
                } else if (Settings.System.getIntForUser(context.getContentResolver(),
                        Settings.System.SELINUX_SWITCH_STATE, 1, UserHandle.USER_CURRENT) == 0) {
                    CMDProcessor.runSuCommand("setenforce 0");
                }
            }
        }
    }
}
