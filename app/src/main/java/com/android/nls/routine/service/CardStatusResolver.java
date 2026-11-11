package com.android.nls.routine.service;

import com.android.nls.routine.model.DayStatus;
import com.android.nls.routine.model.TrackerRecord;
import com.android.nls.routine.utils.Constants;

/**
 * Decides the status a home card shows on its icon and progress bar, using
 * the same {@link DayStatus} pair of colors as the day score: NONE while
 * nothing was logged (neutral), YELLOW while in progress or close to the
 * limit, GREEN once done, RED when it went wrong.
 */
public final class CardStatusResolver {

    static final double GOOD_THRESHOLD = 0.85;
    static final double WARNING_THRESHOLD = 0.55;
    static final double SPENDING_WARNING_RATIO = 0.80;

    private CardStatusResolver() {
    }

    public static DayStatus water(double consumed, double dailyGoal) {
        if (dailyGoal <= 0 || consumed <= 0) {
            return DayStatus.NONE;
        }
        return consumed >= dailyGoal ? DayStatus.GREEN : DayStatus.YELLOW;
    }

    /**
     * Status of the meal card from the statuses of the regular slots logged
     * today (null for an empty slot): in progress until the four are logged,
     * then scored like the day (correct 1.0, warning 0.5, wrong 0.0).
     */
    public static DayStatus meals(String[] slotStatuses) {
        int logged = 0;
        double score = 0;
        for (String status : slotStatuses) {
            if (status == null) {
                continue;
            }
            logged++;
            if (Constants.CORRECT_MEAL.equals(status)) {
                score += 1.0;
            } else if (Constants.WARNING_MEAL.equals(status)) {
                score += 0.5;
            }
        }

        if (logged == 0) {
            return DayStatus.NONE;
        }
        if (logged < slotStatuses.length) {
            return DayStatus.YELLOW;
        }

        double ratio = score / slotStatuses.length;
        if (ratio >= GOOD_THRESHOLD) {
            return DayStatus.GREEN;
        }
        return ratio >= WARNING_THRESHOLD ? DayStatus.YELLOW : DayStatus.RED;
    }

    /** Green inside the budget, yellow from 80% of the limit, red over it. */
    public static DayStatus spending(double spent, double limit) {
        if (limit <= 0 || spent <= 0) {
            return DayStatus.NONE;
        }
        if (spent > limit) {
            return DayStatus.RED;
        }
        return spent >= limit * SPENDING_WARNING_RATIO ? DayStatus.YELLOW : DayStatus.GREEN;
    }

    public static DayStatus routine(TrackerRecord record) {
        return record != null && record.completed() ? DayStatus.GREEN : DayStatus.NONE;
    }
}
