package com.android.nls.routine.parser;

import android.app.Notification;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import android.widget.TextView;
import com.android.nls.routine.utils.Common;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class NotificationTextExtractor {
    private static final String TAG = Common.generateTag(NotificationTextExtractor.class);

    private static final Pattern AMOUNT_PATTERN =
            Pattern.compile("R\\$\\s*[0-9]", Pattern.CASE_INSENSITIVE);

    private static final String[] TEXT_EXTRA_KEYS = {
            Notification.EXTRA_TITLE,
            Notification.EXTRA_TITLE_BIG,
            Notification.EXTRA_TEXT,
            Notification.EXTRA_BIG_TEXT,
            Notification.EXTRA_SUB_TEXT,
            Notification.EXTRA_SUMMARY_TEXT,
            Notification.EXTRA_INFO_TEXT,
            Notification.EXTRA_CONVERSATION_TITLE,
    };

    public static String extractText(Context context, StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();
        Bundle extras = notification.extras;

        List<CharSequence> parts = collectStandardTexts(notification, extras);
        String text = joinParts(parts);

        // The body - and the amount - of some notifications (the BTG "Compra
        // autorizada" one, for instance) only exists inside a custom layout,
        // with every text extra left empty. When no amount came out of the
        // standard extras the layout is read as well and the notification is
        // dumped to the log: a text still missing after that can only be
        // reached once the dump shows where it lives.
        if (!hasAmount(text)) {
            dumpNotification(notification, extras);
            if (context != null) {
                parts.addAll(collectLayoutTexts(context, notification));
                text = joinParts(parts);
            }
        }

        return text;
    }

    private static List<CharSequence> collectStandardTexts(Notification notification, Bundle extras) {
        List<CharSequence> parts = new ArrayList<>();
        CharSequence[] textLines = readExtraArray(extras, Notification.EXTRA_TEXT_LINES);

        if (extras != null) {
            for (String key : TEXT_EXTRA_KEYS) {
                parts.add(readExtra(extras, key));
            }

            if (textLines != null) {
                parts.addAll(Arrays.asList(textLines));
            }

            parts.addAll(readMessages(extras));
        }

        // The ticker is the text shown when the notification arrives while
        // another screen is in front; some apps only put the message there.
        parts.add(notification.tickerText);

        Log.d(TAG, "Title   : " + readExtra(extras, Notification.EXTRA_TITLE));
        Log.d(TAG, "Text    : " + readExtra(extras, Notification.EXTRA_TEXT));
        Log.d(TAG, "BigText : " + readExtra(extras, Notification.EXTRA_BIG_TEXT));
        Log.d(TAG, "Lines   : " + Arrays.toString(textLines));

        return parts;
    }

    /**
     * Reads one extra without letting an unreadable value (the app that posted
     * the notification may be gone) break the whole extraction.
     */
    private static CharSequence readExtra(Bundle extras, String key) {
        if (extras == null) {
            return null;
        }

        try {
            return extras.getCharSequence(key);
        } catch (RuntimeException e) {
            Log.w(TAG, "Could not read the extra " + key, e);
            return null;
        }
    }

    private static CharSequence[] readExtraArray(Bundle extras, String key) {
        if (extras == null) {
            return null;
        }

        try {
            return extras.getCharSequenceArray(key);
        } catch (RuntimeException e) {
            Log.w(TAG, "Could not read the extra " + key, e);
            return null;
        }
    }

    /**
     * A conversation notification keeps its lines in the
     * {@code android.messages} extra instead of the text extras; the framework
     * reader accepts both the bundle form stored there and the legacy message
     * form.
     */
    private static List<CharSequence> readMessages(Bundle extras) {
        List<CharSequence> messages = new ArrayList<>();

        try {
            Parcelable[] parcels = extras.getParcelableArray(Notification.EXTRA_MESSAGES);
            if (parcels == null) {
                return messages;
            }

            for (Notification.MessagingStyle.Message message :
                    Notification.MessagingStyle.Message.getMessagesFromBundleArray(parcels)) {
                if (message != null && message.getText() != null) {
                    messages.add(message.getText());
                }
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "Could not read the messages of the notification", e);
        }

        return messages;
    }

    private static List<CharSequence> collectLayoutTexts(Context context, Notification notification) {
        List<CharSequence> texts = new ArrayList<>();
        collectLayoutText(context, notification.contentView, texts);
        collectLayoutText(context, notification.bigContentView, texts);
        collectLayoutText(context, notification.headsUpContentView, texts);

        if (!texts.isEmpty()) {
            Log.d(TAG, "Layouts : " + texts);
        }

        return texts;
    }

    private static void collectLayoutText(Context context, RemoteViews layout, List<CharSequence> texts) {
        if (layout == null) {
            return;
        }

        try {
            collectViewTexts(layout.apply(context, null), texts);
        } catch (RuntimeException e) {
            // The layout belongs to another app: when it cannot be inflated
            // (or the app that posted the notification is gone) the
            // notification is simply parsed with the text of its extras.
            Log.w(TAG, "Could not read a custom layout of the notification", e);
        }
    }

    /** Collects the text every view of the layout shows, children included. */
    private static void collectViewTexts(View view, List<CharSequence> texts) {
        if (view == null) {
            return;
        }

        if (view instanceof TextView textView && textView.getText() != null) {
            texts.add(textView.getText());
        } else if (view.getContentDescription() != null) {
            texts.add(view.getContentDescription());
        }

        if (view instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                collectViewTexts(group.getChildAt(i), texts);
            }
        }
    }

    /**
     * Logs every text the notification carries, so the source of an amount the
     * extractor still could not reach can be identified from the logs.
     */
    private static void dumpNotification(Notification notification, Bundle extras) {
        Log.d(TAG, "No amount in the text of the notification: dumping it");
        Log.d(TAG, "Views : content=" + (notification.contentView != null)
                + " big=" + (notification.bigContentView != null)
                + " headsUp=" + (notification.headsUpContentView != null));

        if (extras == null) {
            Log.d(TAG, "Extras : null");
            return;
        }

        try {
            for (String key : extras.keySet()) {
                Object value;
                try {
                    value = extras.get(key);
                } catch (RuntimeException e) {
                    value = "unreadable (" + e + ")";
                }
                Log.d(TAG, "Extra : " + key + " = " + value);
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "Could not dump the extras of the notification", e);
        }
    }

    /**
     * Joins every text fragment shown by the notification into the block the
     * parsers read. Kept apart from the notification plumbing so the rules can
     * be unit tested.
     */
    static String joinParts(List<CharSequence> parts) {
        StringBuilder sb = new StringBuilder();
        for (CharSequence part : parts) {
            appendIfNew(sb, part);
        }

        return sb.toString();
    }

    static boolean hasAmount(String text) {
        return text != null && AMOUNT_PATTERN.matcher(text).find();
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