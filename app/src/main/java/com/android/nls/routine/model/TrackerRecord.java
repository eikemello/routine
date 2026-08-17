package com.android.nls.routine.model;

public record TrackerRecord(long id, TrackerType type, boolean completed, String note,
                            long timestamp) {

}