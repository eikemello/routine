package com.android.nls.routine.listener;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ExpenseDeduplicatorTest {

    private static final long T0 = 1_700_000_000_000L;
    private static final String KEY = "0|br.com.btg|1|null|10123";

    @Test
    public void firstExpense_isNotDuplicate() {
        ExpenseDeduplicator dedup = new ExpenseDeduplicator();
        assertFalse(dedup.isDuplicate(KEY, "BTG", 62.78, T0));
    }

    @Test
    public void updateOfSameNotification_isDuplicate() {
        ExpenseDeduplicator dedup = new ExpenseDeduplicator();
        dedup.isDuplicate(KEY, "BTG", 62.78, T0);
        assertTrue(dedup.isDuplicate(KEY, "BTG", 62.78, T0 + 5 * 60 * 1000L));
    }

    @Test
    public void secondNotificationOfSamePurchase_isDuplicate() {
        ExpenseDeduplicator dedup = new ExpenseDeduplicator();
        dedup.isDuplicate(KEY, "BTG", 62.78, T0);
        assertTrue(dedup.isDuplicate("other-key", "BTG", 62.78, T0 + 2_000L));
    }

    @Test
    public void sameAmountAtSameBank_afterPurchaseWindow_isNew() {
        ExpenseDeduplicator dedup = new ExpenseDeduplicator();
        dedup.isDuplicate(KEY, "BTG", 5.00, T0);
        assertFalse(dedup.isDuplicate("other-key", "BTG", 5.00,
                T0 + ExpenseDeduplicator.SAME_PURCHASE_WINDOW_MILLIS + 1));
    }

    @Test
    public void reusedNotificationKey_withDifferentAmount_isNew() {
        ExpenseDeduplicator dedup = new ExpenseDeduplicator();
        dedup.isDuplicate(KEY, "BTG", 62.78, T0);
        assertFalse(dedup.isDuplicate(KEY, "BTG", 15.90, T0 + 1_000L));
    }

    @Test
    public void reusedNotificationKey_afterKeyWindow_isNew() {
        ExpenseDeduplicator dedup = new ExpenseDeduplicator();
        dedup.isDuplicate(KEY, "BTG", 5.00, T0);
        assertFalse(dedup.isDuplicate(KEY, "BTG", 5.00,
                T0 + ExpenseDeduplicator.SAME_KEY_WINDOW_MILLIS + 1));
    }

    @Test
    public void sameAmountAtOtherBank_isNew() {
        ExpenseDeduplicator dedup = new ExpenseDeduplicator();
        dedup.isDuplicate(KEY, "BTG", 62.78, T0);
        assertFalse(dedup.isDuplicate("other-key", "Itaú", 62.78, T0 + 1_000L));
    }

    @Test
    public void amountsAreComparedInCents() {
        ExpenseDeduplicator dedup = new ExpenseDeduplicator();
        dedup.isDuplicate(KEY, "BTG", 0.1 + 0.2, T0);
        assertTrue(dedup.isDuplicate("other-key", "BTG", 0.3, T0 + 1_000L));
    }
}
