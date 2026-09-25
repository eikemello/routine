package com.android.nls.routine.model;

/**
 * Data transfer object containing processed information for the water widget.
 */
public record WaterWidgetData(
    String totalText,
    int totalColor,
    String goalText,
    int progressPercentage,
    boolean goalReached,
    String completedText,
    String[] buttonLabels
) {}
