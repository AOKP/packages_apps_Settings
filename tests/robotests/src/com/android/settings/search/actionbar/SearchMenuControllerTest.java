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

package com.android.settings.search.actionbar;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Bundle;
import android.provider.Settings.Global;
import android.view.Menu;
import android.view.MenuItem;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.core.lifecycle.ObservableFragment;
import com.android.settingslib.core.lifecycle.ObservablePreferenceFragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

@RunWith(SettingsRobolectricTestRunner.class)
public class SearchMenuControllerTest {

    @Mock
    private Menu mMenu;
    private TestPreferenceFragment mPreferenceHost;
    private ObservableFragment mHost;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mHost = spy(new ObservableFragment());
        when(mHost.getContext()).thenReturn(mContext);
        mPreferenceHost = new TestPreferenceFragment();
        Global.putInt(mContext.getContentResolver(), Global.DEVICE_PROVISIONED, 1);

        when(mMenu.add(Menu.NONE, Menu.NONE, 0 /* order */, R.string.search_menu))
                .thenReturn(mock(MenuItem.class));
    }

    @Test
    public void init_prefFragment_shouldAddMenu() {
        SearchMenuController.init(mPreferenceHost);
        mPreferenceHost.getLifecycle().onCreateOptionsMenu(mMenu, null /* inflater */);

        verify(mMenu).add(Menu.NONE, Menu.NONE, 0 /* order */, R.string.search_menu);
    }

    @Test
    public void init_observableFragment_shouldAddMenu() {
        SearchMenuController.init(mHost);
        mHost.getLifecycle().onCreateOptionsMenu(mMenu, null /* inflater */);

        verify(mMenu).add(Menu.NONE, Menu.NONE, 0 /* order */, R.string.search_menu);
    }

    @Test
    public void init_doNotNeedSearchIcon_shouldNotAddMenu() {
        final Bundle args = new Bundle();
        args.putBoolean(SearchMenuController.NEED_SEARCH_ICON_IN_ACTION_BAR, false);
        mHost.setArguments(args);

        SearchMenuController.init(mHost);
        mHost.getLifecycle().onCreateOptionsMenu(mMenu, null /* inflater */);
        verifyZeroInteractions(mMenu);
    }

    @Test
    public void init_deviceNotProvisioned_shouldNotAddMenu() {
        Global.putInt(mContext.getContentResolver(), Global.DEVICE_PROVISIONED, 0);
        SearchMenuController.init(mHost);
        mHost.getLifecycle().onCreateOptionsMenu(mMenu, null /* inflater */);

        verifyZeroInteractions(mMenu);
    }

    private static class TestPreferenceFragment extends ObservablePreferenceFragment {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        }

        @Override
        public Context getContext() {
            return RuntimeEnvironment.application;
        }
    }
}
