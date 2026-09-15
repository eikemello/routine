package com.android.nls.routine.parser;

import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;
import com.android.nls.routine.utils.Common;

public class NotificationTextExtractor {
    private static final String TAG = Common.generateTag(NotificationTextExtractor.class);

    public static String extractText(StatusBarNotification sbn) {
        // Notification.extras can be null for notifications built without
        // extras; access it defensively to avoid a NullPointerException that
        // would kill the app process.
        Bundle extras = sbn.getNotification().extras;
        CharSequence title = extras != null ? extras.getCharSequence("android.title") : null;
        CharSequence text = extras != null ? extras.getCharSequence("android.text") : null;

        Log.d(TAG, "Title: " + title);
        Log.d(TAG, "Text : " + text);

        StringBuilder sb = new StringBuilder();
        if (!TextUtils.isEmpty(title)) {
            sb.append(title);
        }
        if (!TextUtils.isEmpty(text)) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(text);
        }
        return sb.toString();
    }
}