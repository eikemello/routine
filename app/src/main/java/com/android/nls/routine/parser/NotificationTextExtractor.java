package com.android.nls.routine.parser;

import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;

import com.android.nls.routine.utils.Common;

public class NotificationTextExtractor {
    private static final String TAG = Common.generateTag(NotificationTextExtractor.class);

    public static String extractText(StatusBarNotification sbn) {
        CharSequence title = sbn.getNotification().extras.getCharSequence("android.title");
        CharSequence text = sbn.getNotification().extras.getCharSequence("android.text");

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