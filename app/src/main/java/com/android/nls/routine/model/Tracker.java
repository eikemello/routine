package com.android.nls.routine.model;

public record Tracker(long id, TrackerType type, String name, String icon, boolean enabled,
                      String description) {

}