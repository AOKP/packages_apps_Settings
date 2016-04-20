/*
 * Copyright (C) 2007 The Android Open Source Project
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


import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.RingtonePreference;
import android.util.AttributeSet;

public class DefaultRingtonePreference extends RingtonePreference {
    private static final String TAG = "DefaultRingtonePreference";
    
    public DefaultRingtonePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onPrepareRingtonePickerIntent(Intent ringtonePickerIntent) {
        super.onPrepareRingtonePickerIntent(ringtonePickerIntent);
        
        /*
         * Since this preference is for choosing the default ringtone, it
         * doesn't make sense to show a 'Default' item.
         */
        ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false);
    }

    @Override
    protected void onSaveRingtone(Uri ringtoneUri) {
        if (getRingtoneType() == RingtoneManager.TYPE_RINGTONE) {
            RingtoneManager.setActualRingtoneUriBySubId(getContext(),
                    getSubId(), ringtoneUri);
        } else {
            RingtoneManager.setActualDefaultRingtoneUri(getContext(),
                    getRingtoneType(), ringtoneUri);
        }
    }

    @Override
    protected Uri onRestoreRingtone() {
        if (getRingtoneType() == RingtoneManager.TYPE_RINGTONE) {
            return RingtoneManager.getActualRingtoneUriBySubId(getContext(), getSubId());
        } else {
            return RingtoneManager.getActualDefaultRingtoneUri(getContext(), getRingtoneType());
        }
    }

}
