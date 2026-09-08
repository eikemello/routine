package com.android.nls.routine.service.calendar;

import com.android.nls.routine.model.DayDetails;
import com.android.nls.routine.model.MealRecord;
import com.android.nls.routine.model.TrackerRecord;
import com.android.nls.routine.model.TrackerType;
import com.android.nls.routine.model.WaterRecord;
import java.util.HashMap;
import java.util.Map;

/**
 * Extracts the score computation inputs (water sum, meal statuses,
 * tracker completions) from a DayDetails object.
 * <p>
 * This is the single source of truth for computed day values,
 * shared by both the day details grid and the score calculation.
 */
public class DayScoreData {

    private final int waterSum;
    private final Map<String, String> mealStatusesByType;
    private final Map<TrackerType, Boolean> trackerCompletions;

    public DayScoreData(DayDetails details) {
        this.waterSum = computeWaterSum(details);
        this.mealStatusesByType = buildMealStatuses(details);
        this.trackerCompletions = buildTrackerCompletions(details);
    }

    public int getWaterSum() {
        return waterSum;
    }

    public Map<String, String> getMealStatusesByType() {
        return mealStatusesByType;
    }

    public Map<TrackerType, Boolean> getTrackerCompletions() {
        return trackerCompletions;
    }

    private int computeWaterSum(DayDetails details) {
        int sum = 0;
        for (WaterRecord record : details.waterRecords()) {
            sum += record.amount();
        }
        return sum;
    }

    private Map<String, String> buildMealStatuses(DayDetails details) {
        Map<String, String> mealStatusesByType = new HashMap<>();
        for (MealRecord record : details.mealRecords()) {
            mealStatusesByType.put(record.meal(), record.status());
        }
        return mealStatusesByType;
    }

    private Map<TrackerType, Boolean> buildTrackerCompletions(DayDetails details) {
        Map<TrackerType, Boolean> trackerCompletions = new HashMap<>();
        for (TrackerRecord record : details.workoutRecords()) {
            trackerCompletions.put(TrackerType.WORKOUT, record.completed());
        }
        for (TrackerRecord record : details.medicationRecords()) {
            trackerCompletions.put(TrackerType.MEDICATION, record.completed());
        }
        for (TrackerRecord record : details.supplementRecords()) {
            trackerCompletions.put(TrackerType.SUPPLEMENT, record.completed());
        }
        return trackerCompletions;
    }
}