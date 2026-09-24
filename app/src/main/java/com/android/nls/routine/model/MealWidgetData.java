package com.android.nls.routine.model;

/**
 * Data transfer object containing processed information for the meal widget.
 */
public record MealWidgetData(
    int[] segmentDrawables,
    String countText,
    int countColor
) {}
