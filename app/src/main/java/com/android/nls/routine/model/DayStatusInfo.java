package com.android.nls.routine.model;

/**
 * Holds the computed status and data-presence flag for a single day.
 * Used by batch calendar rendering to avoid per-day database queries.
 */
public record DayStatusInfo(DayStatus status, boolean hasData) {
}