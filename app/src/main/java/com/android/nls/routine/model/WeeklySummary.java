package com.android.nls.routine.model;

public record WeeklySummary(
        int waterDaysAchieved,
        int totalDays,
        double totalSpent,
        int correctMeals,
        int warningMeals,
        int wrongMeals) {
}