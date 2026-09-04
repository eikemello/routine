package com.android.nls.routine.service.score;

import com.android.nls.routine.model.DayDetails;
import com.android.nls.routine.model.MealRecord;
import com.android.nls.routine.model.Tracker;
import com.android.nls.routine.model.TrackerRecord;
import com.android.nls.routine.model.TrackerType;
import com.android.nls.routine.model.WaterRecord;
import com.android.nls.routine.service.HistoryService;
import com.android.nls.routine.utils.Constants;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Extracts the score computation inputs (water sum, meal counts,
 * tracker completions, enabled trackers) from a DayDetails object.
 */
public class DayScoreData {

    private final int waterSum;
    private final Map<String, int[]> mealCountsByType;
    private final Map<TrackerType, Boolean> trackerCompletions;
    private final Set<TrackerType> enabledTrackers;

    public DayScoreData(DayDetails details, HistoryService historyService) {
        this.waterSum = computeWaterSum(details);
        this.mealCountsByType = buildMealCounts(details);
        this.trackerCompletions = buildTrackerCompletions(details);
        this.enabledTrackers = buildEnabledTrackers(historyService);
    }

    public int getWaterSum() {
        return waterSum;
    }

    public Map<String, int[]> getMealCountsByType() {
        return mealCountsByType;
    }

    public Map<TrackerType, Boolean> getTrackerCompletions() {
        return trackerCompletions;
    }

    public Set<TrackerType> getEnabledTrackers() {
        return enabledTrackers;
    }

    private int computeWaterSum(DayDetails details) {
        int sum = 0;
        for (WaterRecord record : details.waterRecords()) {
            sum += record.amount();
        }
        return sum;
    }

    private Map<String, int[]> buildMealCounts(DayDetails details) {
        Map<String, int[]> mealCountsByType = new HashMap<>();
        for (MealRecord record : details.mealRecords()) {
            int[] counts = mealCountsByType.computeIfAbsent(record.meal(), k -> new int[3]);
            if (Constants.CORRECT_MEAL.equals(record.status())) {
                counts[0]++;
            } else if (Constants.WARNING_MEAL.equals(record.status())) {
                counts[1]++;
            } else if (Constants.WRONG_MEAL.equals(record.status())) {
                counts[2]++;
            }
        }
        return mealCountsByType;
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

    private Set<TrackerType> buildEnabledTrackers(HistoryService historyService) {
        // Enabled trackers excluding EXPENSES
        Set<TrackerType> enabledTrackers = new LinkedHashSet<>();
        for (Tracker tracker : historyService.getEnabledTrackers()) {
            if (tracker.type() != TrackerType.EXPENSES) {
                enabledTrackers.add(tracker.type());
            }
        }
        return enabledTrackers;
    }
}