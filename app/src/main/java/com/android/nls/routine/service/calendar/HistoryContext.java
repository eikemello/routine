package com.android.nls.routine.service.calendar;

import com.android.nls.routine.model.Tracker;
import com.android.nls.routine.model.TrackerType;
import com.android.nls.routine.repository.ConfigRepository;
import com.android.nls.routine.service.HistoryService;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Holds the shared data for the History screen, loaded once
 * per Activity lifetime and passed to renderers and helpers.
 * <p>
 * Centralizes service access and cached values so that individual
 * classes do not each re-query the database.
 */
public class HistoryContext {

    private final HistoryService mHistoryService;
    private final ConfigRepository mConfigRepository;
    private final List<Tracker> mEnabledTrackers;
    private final double mDailyWaterGoal;

    /**
     * Loads the cached values (enabled trackers, daily water goal) from the repositories.
     */
    public HistoryContext(HistoryService historyService, ConfigRepository configRepository) {
        mHistoryService = historyService;
        mConfigRepository = configRepository;
        mEnabledTrackers = historyService.getEnabledTrackers();
        mDailyWaterGoal = configRepository.getDailyWaterGoal();
    }

    public HistoryService getHistoryService() {
        return mHistoryService;
    }

    public List<Tracker> getEnabledTrackers() {
        return mEnabledTrackers;
    }

    public double getDailyWaterGoal() {
        return mDailyWaterGoal;
    }

    /**
     * Returns the enabled tracker types excluding EXPENSES,
     * which is not part of the score calculation.
     */
    public Set<TrackerType> getScoreTrackerTypes() {
        Set<TrackerType> types = new LinkedHashSet<>();
        for (Tracker tracker : mEnabledTrackers) {
            if (tracker.type() != TrackerType.EXPENSES) {
                types.add(tracker.type());
            }
        }
        return types;
    }

    public void closeDb() {
        mHistoryService.closeDb();
        mConfigRepository.closeDb();
    }
}