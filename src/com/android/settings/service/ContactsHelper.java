package com.android.settings.service;

import android.provider.ContactsContract.PhoneLookup;
import android.content.Context;
import android.database.Cursor;

public class ContactsHelper {

    public static boolean isContact(Context context, String phoneNumber) {
        Uri lookupUri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        String[] numberProject = {
                PhoneLookup._ID,
                PhoneLookup.NUMBER,
                PhoneLookup.DISPLAY_NAME };
        Cursor c = context.getContentResolver().query(lookupUri, numberProject, null, null, null);
        try {
            if (c.moveToFirst()) {
                return true;
            }
        } finally {
            if (c != null) {
               c.close();
            }
        }
        return false;
    }
}
