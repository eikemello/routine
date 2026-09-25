package com.android.nls.routine.model;

/**
 * Data transfer object containing processed information for the meal widget.
 */
public record MealWidgetData(
    String currentMealName,
    boolean currentMealLogged,
    String countText,
    int countColor
) {}
