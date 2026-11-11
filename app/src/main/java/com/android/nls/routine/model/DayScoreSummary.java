package com.android.nls.routine.model;

/**
 * The score of one day as the home screen ring shows it: the weighted
 * percentage (0-100, or -1 when there is nothing to score yet) and the
 * status color it falls into, both computed by DayScore.
 */
public record DayScoreSummary(double percentage, DayStatus status) {

    public boolean hasScore() {
        return percentage >= 0;
    }
}
