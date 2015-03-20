/*
 * Copyright (C) 2015 Dirty Unicorns
 * Copyright (C) 2015 AICP
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

package com.android.settings.aicp.hfm;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import android.app.ListActivity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.android.settings.R;

public class HfmWhitelist extends ListActivity {

    private static final String SET_HFM_WHITELIST = "hfm_whitelist";

    private static final String REMOVE_URL_CMD = "busybox sed -i \"/%s/D\" /etc/hosts.alt";
    private static final String REMOUNT_CMD = "busybox mount -o %s,remount /system";

    private static final int MENU_APPLY = Menu.FIRST;

    private ArrayList<String> mHfmWhitelist = new ArrayList<String>();
    private ArrayAdapter<String> mAdapter;
    private SharedPreferences mSharedPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        Set<String> hfmWhitelistSet = mSharedPreferences.getStringSet(SET_HFM_WHITELIST, null);
        if (hfmWhitelistSet != null) {
            mHfmWhitelist = new ArrayList<String>(hfmWhitelistSet);
        } else {
            mHfmWhitelist = new ArrayList<String>();
        }

        setContentView(R.layout.hfm_whitelist);

        Button hfmButtonAdd = (Button) findViewById(R.id.hfm_whitelist_button_add);

        mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mHfmWhitelist);

        OnClickListener addListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText edit = (EditText) findViewById(R.id.hfm_whitelist_text);
                String text = edit.getText().toString().trim();
                if ( text.length() > 0 && ! mHfmWhitelist.contains(text)) {
                    mHfmWhitelist.add(text);
                }
                edit.setText("");
                mAdapter.notifyDataSetChanged();
            }
        };

        OnItemLongClickListener itemLongClickListener = new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView arg0, View arg1, int arg2, long arg3) {
                mHfmWhitelist.remove(arg2);
                mAdapter.notifyDataSetChanged();
                return false;
            }
        };

        ListView lv = getListView();
        lv.setOnItemLongClickListener(itemLongClickListener);

        hfmButtonAdd.setOnClickListener(addListener);

        setListAdapter(mAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_APPLY, 0, R.string.hfm_whitelist_pref_menu_apply)
                .setIcon(R.drawable.ic_hfm_whitelist_apply)
                .setAlphabeticShortcut('a')
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM |
                MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_APPLY:
                mSharedPreferences.edit()
                    .putStringSet("hfm_whitelist", new HashSet<String>(mHfmWhitelist))
                    .commit();
                applyWhitelist(getBaseContext());
                Toast.makeText(getBaseContext(), "Whitelist applied to hosts file" , Toast.LENGTH_SHORT).show();
                return true;

            default:
                return false;
        }
    }

    private void applyWhitelist(Context context) {
        applyWhitelist(context, mHfmWhitelist);
    }

    public static void applyWhitelist(Context context, ArrayList<String> hfmWhitelist) {
        File altOrigHosts = new File("/etc/hosts.alt_orig");

        String cmd = String.format(REMOUNT_CMD, "rw");

        if (altOrigHosts.exists()) {
            cmd = cmd + " && cp -f /etc/hosts.alt_orig /etc/hosts.alt";
        } else {
            cmd = cmd + " && cp -f /etc/hosts.alt /etc/hosts.alt_orig"
                      + " && chmod 644 /etc/hosts.alt_orig";
        }

        for(String url: hfmWhitelist){
            cmd = cmd + " && " + String.format(REMOVE_URL_CMD, url);
        }

        if (Settings.System.getInt(context.getContentResolver(), Settings.System.HFM_DISABLE_ADS, 0) == 1) {
            cmd = cmd + " && cp -f /etc/hosts.alt /etc/hosts";
        }

        cmd = cmd + " && " + String.format(REMOUNT_CMD, "ro");

        try {
            HfmHelpers.RunAsRoot(cmd);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
