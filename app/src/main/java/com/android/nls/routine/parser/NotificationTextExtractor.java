package com.android.nls.routine.parser;

import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import com.android.nls.routine.utils.Common;
import java.util.Arrays;

public class NotificationTextExtractor {
    private static final String TAG = Common.generateTag(NotificationTextExtractor.class);

    /** Extras that carry the text a notification shows (collapsed and expanded). */
    private static final String EXTRA_TITLE = "android.title";
    private static final String EXTRA_TEXT = "android.text";
    private static final String EXTRA_BIG_TEXT = "android.bigText";
    private static final String EXTRA_TEXT_LINES = "android.textLines";

    public static String extractText(StatusBarNotification sbn) {
        // Notification.extras can be null for notifications built without
        // extras; access it defensively to avoid a NullPointerException that
        // would kill the app process.
        Bundle extras = sbn.getNotification().extras;
        CharSequence title = extras != null ? extras.getCharSequence(EXTRA_TITLE) : null;
        CharSequence text = extras != null ? extras.getCharSequence(EXTRA_TEXT) : null;
        // The collapsed text of an expanded notification is often only a
        // summary ("Compra autorizada") and the full message - the one that
        // carries the amount - lives in bigText, or in textLines for the
        // inbox style. Without them the expense of such a notification is
        // never seen by the parsers.
        CharSequence bigText = extras != null ? extras.getCharSequence(EXTRA_BIG_TEXT) : null;
        CharSequence[] textLines = extras != null ? extras.getCharSequenceArray(EXTRA_TEXT_LINES) : null;

        Log.d(TAG, "Title   : " + title);
        Log.d(TAG, "Text    : " + text);
        Log.d(TAG, "BigText : " + bigText);
        Log.d(TAG, "Lines   : " + Arrays.toString(textLines));

        return joinParts(title, text, bigText, textLines);
    }

    /**
     * Joins every text fragment shown by the notification into the block the
     * parsers read. Kept apart from the notification plumbing so the rules can
     * be unit tested.
     */
    static String joinParts(CharSequence title, CharSequence text, CharSequence bigText, CharSequence[] textLines) {
        StringBuilder sb = new StringBuilder();
        appendIfNew(sb, title);
        appendIfNew(sb, text);
        appendIfNew(sb, bigText);

        if (textLines != null) {
            for (CharSequence line : textLines) {
                appendIfNew(sb, line);
            }
        }

        return sb.toString();
    }

    /**
     * Appends a fragment unless it is empty or already present: bigText and
     * textLines usually repeat what the title/text already brought, and
     * duplicating them would only pollute the parsers.
     */
    private static void appendIfNew(StringBuilder sb, CharSequence value) {
        if (value == null) {
            return;
        }

        String part = value.toString().trim();
        if (part.isEmpty() || sb.indexOf(part) >= 0) {
            return;
        }

        if (sb.length() > 0) {
            sb.append("\n");
        }
        sb.append(part);
    }
}