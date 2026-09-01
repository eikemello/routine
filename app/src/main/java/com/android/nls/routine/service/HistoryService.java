package com.android.nls.routine.service;

import android.content.Context;
import com.android.nls.routine.model.DayDetails;
import com.android.nls.routine.model.DayStatus;
import com.android.nls.routine.model.DayStatusInfo;
import com.android.nls.routine.model.ExpenseRecord;
import com.android.nls.routine.model.MealRecord;
import com.android.nls.routine.model.Tracker;
import com.android.nls.routine.model.TrackerRecord;
import com.android.nls.routine.model.TrackerType;
import com.android.nls.routine.model.WaterRecord;
import com.android.nls.routine.model.WeeklySummary;
import com.android.nls.routine.repository.ConfigRepository;
import com.android.nls.routine.repository.ExpenseRepository;
import com.android.nls.routine.repository.MealRepository;
import com.android.nls.routine.repository.TrackerRepository;
import com.android.nls.routine.repository.WaterRepository;
import com.android.nls.routine.service.score.DayScore;
import com.android.nls.routine.utils.Common;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HistoryService {
    private final WaterRepository mWaterRepository;
    private final MealRepository mMealRepository;
    private final ExpenseRepository mExpenseRepository;
    private final TrackerRepository mTrackerRepository;
    private final ConfigRepository mConfigRepository;

    public HistoryService(Context context) {
        mWaterRepository = new WaterRepository(context);
        mMealRepository = new MealRepository(context);
        mExpenseRepository = new ExpenseRepository(context);
        mTrackerRepository = new TrackerRepository(context);
        mConfigRepository = new ConfigRepository(context);
    }

    public WeeklySummary getWeeklySummary() {
        long startOfWeek = Common.getStartOfWeekInMillis();
        long endOfWeek = Common.getEndOfWeekInMillis();

        int waterDaysAchieved = countWaterDaysAchieved(startOfWeek, endOfWeek);
        int totalDays = getDaysElapsedInWeek();
        double totalSpent = mExpenseRepository.getTotalSpent(startOfWeek, endOfWeek);
        int[] mealCounts = mMealRepository.getMealCounts(startOfWeek, endOfWeek);

        return new WeeklySummary(
                waterDaysAchieved,
                totalDays,
                totalSpent,
                mealCounts[0],
                mealCounts[1],
                mealCounts[2]
        );
    }

    public WeeklySummary getMonthlySummary(Calendar month) {
        long startOfMonth = Common.getStartOfDayInMillis(month.getTimeInMillis());
        Calendar endCal = (Calendar) month.clone();
        endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH));
        long endOfMonth = Common.getEndOfDayInMillis(endCal.getTimeInMillis());

        int waterDaysAchieved = countWaterDaysAchieved(startOfMonth, endOfMonth);
        int totalDays = getDaysElapsedInMonth(month);
        double totalSpent = mExpenseRepository.getTotalSpent(startOfMonth, endOfMonth);
        int[] mealCounts = mMealRepository.getMealCounts(startOfMonth, endOfMonth);

        return new WeeklySummary(
                waterDaysAchieved,
                totalDays,
                totalSpent,
                mealCounts[0],
                mealCounts[1],
                mealCounts[2]
        );
    }

    public DayDetails getDayDetails(long timestamp) {
        long startOfDay = Common.getStartOfDayInMillis(timestamp);
        long endOfDay = Common.getEndOfDayInMillis(timestamp);

        List<WaterRecord> waterRecords = mWaterRepository.getWaterRecords(startOfDay, endOfDay);
        List<MealRecord> mealRecords = mMealRepository.getMealRecords(startOfDay, endOfDay);
        List<ExpenseRecord> expenseRecords = mExpenseRepository.getExpenseRecords(startOfDay, endOfDay);
        List<TrackerRecord> workoutRecords = mTrackerRepository.getTrackerRecords(TrackerType.WORKOUT, startOfDay, endOfDay);
        List<TrackerRecord> medicationRecords = mTrackerRepository.getTrackerRecords(TrackerType.MEDICATION, startOfDay, endOfDay);
        List<TrackerRecord> supplementRecords = mTrackerRepository.getTrackerRecords(TrackerType.SUPPLEMENT, startOfDay, endOfDay);

        return new DayDetails(waterRecords, mealRecords, expenseRecords, workoutRecords, medicationRecords, supplementRecords);
    }

    /**
     * Computes the DayStatus and hasData flag for every day in the given range
     * using a small fixed number of queries (one per table),
     *
     * @return a map keyed by start-of-day timestamp
     */
    public Map<Long, DayStatusInfo> getDayStatusesForRange(long start, long end) {
        double dailyGoal = mConfigRepository.getDailyWaterGoal();

        // Enabled tracker cards (excluding EXPENSES, which is not part of the score)
        Set<TrackerType> enabledTrackers = new LinkedHashSet<>();
        for (Tracker tracker : mTrackerRepository.getEnabledTrackers()) {
            if (tracker.type() != TrackerType.EXPENSES) {
                enabledTrackers.add(tracker.type());
            }
        }

        Map<Long, Integer> dailyWaterSums = mWaterRepository.getDailyWaterSums(start, end);
        Map<Long, Map<String, int[]>> dailyMealCountsByType = mMealRepository.getDailyMealCountsByType(start, end);
        Map<Long, Map<TrackerType, Boolean>> dailyTrackerCompletions = mTrackerRepository.getDailyTrackerCompletions(start, end);
        Set<Long> daysWithWater = mWaterRepository.getDaysWithWaterData(start, end);
        Set<Long> daysWithMeals = mMealRepository.getDaysWithMealData(start, end);
        Set<Long> daysWithExpenses = mExpenseRepository.getDaysWithExpenseData(start, end);
        Set<Long> daysWithTrackers = mTrackerRepository.getDaysWithTrackerData(start, end);

        Map<Long, DayStatusInfo> result = new HashMap<>();
        Calendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(start);

        while (calendar.getTimeInMillis() <= end) {
            long dayStart = calendar.getTimeInMillis();

            int waterSum = dailyWaterSums.getOrDefault(dayStart, 0);
            Map<String, int[]> mealCountsByType = dailyMealCountsByType.get(dayStart);
            Map<TrackerType, Boolean> trackerCompletions = dailyTrackerCompletions.get(dayStart);

            boolean hasData = daysWithWater.contains(dayStart)
                    || daysWithMeals.contains(dayStart)
                    || daysWithExpenses.contains(dayStart)
                    || daysWithTrackers.contains(dayStart);

            DayStatus status = DayScore.compute(waterSum, dailyGoal, mealCountsByType, trackerCompletions, enabledTrackers);
            result.put(dayStart, new DayStatusInfo(status, hasData));

            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        return result;
    }

    private int countWaterDaysAchieved(long startOfWeek, long endOfWeek) {
        double dailyGoal = mConfigRepository.getDailyWaterGoal();
        Map<Long, Integer> dailySums = mWaterRepository.getDailyWaterSums(startOfWeek, endOfWeek);

        int daysAchieved = 0;
        Calendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(startOfWeek);

        while (calendar.getTimeInMillis() <= endOfWeek) {
            long dayStart = calendar.getTimeInMillis();
            Integer daySum = dailySums.get(dayStart);

            if (daySum != null && daySum >= dailyGoal) {
                daysAchieved++;
            }
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        return daysAchieved;
    }

    private int getDaysElapsedInWeek() {
        long startOfWeek = Common.getStartOfWeekInMillis();
        Calendar startCal = new GregorianCalendar();
        startCal.setTimeInMillis(startOfWeek);

        long now = System.currentTimeMillis();
        Calendar nowCal = new GregorianCalendar();
        nowCal.setTimeInMillis(now);

        int days = 0;

        while (startCal.getTimeInMillis() <= nowCal.getTimeInMillis()) {
            days++;
            startCal.add(Calendar.DAY_OF_MONTH, 1);
        }

        return days;
    }

    private int getDaysElapsedInMonth(Calendar month) {
        Calendar startCal = (Calendar) month.clone();
        startCal.set(Calendar.DAY_OF_MONTH, 1);
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);

        long now = System.currentTimeMillis();
        Calendar nowCal = new GregorianCalendar();
        nowCal.setTimeInMillis(now);

        // If the displayed month is in the future, return 0 days elapsed
        if (startCal.getTimeInMillis() > nowCal.getTimeInMillis()) {
            return 0;
        }

        int days = 0;
        while (startCal.getTimeInMillis() <= nowCal.getTimeInMillis()
                && startCal.get(Calendar.MONTH) == month.get(Calendar.MONTH)
                && startCal.get(Calendar.YEAR) == month.get(Calendar.YEAR)) {
            days++;
            startCal.add(Calendar.DAY_OF_MONTH, 1);
        }

        return days;
    }

    public void closeDb() {
        mWaterRepository.closeDb();
        mMealRepository.closeDb();
        mExpenseRepository.closeDb();
        mTrackerRepository.closeDb();
        mConfigRepository.closeDb();
    }
}