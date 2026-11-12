package com.android.nls.routine.listener;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * Remembers the expenses recently saved from notifications so the same
 * purchase is not stored twice. {@code onNotificationPosted} fires for every
 * enqueue: the first post, every update of the same notification (same key)
 * and, for some banks, a second notification carrying the same purchase.
 * <ul>
 *     <li>Same notification key and same amount within {@link #SAME_KEY_WINDOW_MILLIS}:
 *     an update or re-post of the notification already saved.</li>
 *     <li>Same bank and same amount within {@link #SAME_PURCHASE_WINDOW_MILLIS}:
 *     a second notification announcing the same purchase.</li>
 * </ul>
 * The amount is part of the key rule because some apps reuse one notification
 * id for every purchase. Pure Java so the rules can be unit tested.
 */
public class ExpenseDeduplicator {
    static final long SAME_KEY_WINDOW_MILLIS = 10 * 60 * 1000L;
    static final long SAME_PURCHASE_WINDOW_MILLIS = 60 * 1000L;

    private final Deque<Entry> mRecent = new ArrayDeque<>();

    /**
     * Returns {@code true} when the expense duplicates one already accepted;
     * otherwise remembers it and returns {@code false}, so the caller saves it.
     */
    public synchronized boolean isDuplicate(String key, String bank, double amount, long timestamp) {
        prune(timestamp);

        long cents = Math.round(amount * 100);
        for (Entry entry : mRecent) {
            if (entry.cents != cents) {
                continue;
            }

            long elapsed = Math.abs(timestamp - entry.timestamp);
            if (key != null && key.equals(entry.key) && elapsed <= SAME_KEY_WINDOW_MILLIS) {
                return true;
            }
            if (Objects.equals(bank, entry.bank) && elapsed <= SAME_PURCHASE_WINDOW_MILLIS) {
                return true;
            }
        }

        mRecent.addLast(new Entry(key, bank, cents, timestamp));
        return false;
    }

    private void prune(long now) {
        mRecent.removeIf(entry -> now - entry.timestamp > SAME_KEY_WINDOW_MILLIS);
    }

    private record Entry(String key, String bank, long cents, long timestamp) {
    }
}
