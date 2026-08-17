package com.android.nls.routine.model;

import java.util.List;

public record DayDetails(
        List<WaterRecord> waterRecords,
        List<MealRecord> mealRecords,
        List<ExpenseRecord> expenseRecords,
        List<TrackerRecord> workoutRecords,
        List<TrackerRecord> medicationRecords,
        List<TrackerRecord> supplementRecords) {
}