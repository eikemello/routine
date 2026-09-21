package com.android.nls.routine.cardhistory;

import android.content.Context;
import com.android.nls.routine.model.Tracker;
import com.android.nls.routine.model.TrackerRecord;
import com.android.nls.routine.model.TrackerType;
import com.android.nls.routine.repository.TrackerRepository;

/**
 * Dynamic card history of the simple cards - workout, medication and supplement.
 * They are called dynamic because this single service backs all three of them
 * instead of one service per card: the cards are the same status plus a button
 * and their histories are the same single row, so the tracker handled by a card
 * - which carries its type, name and description - is the parameter that makes
 * the difference. It owns the repository, and so the database reference, for as
 * long as the home screen lives, while the panel itself is built per open by
 * {@link DynamicCardHistory}.
 */
public class DynamicCardHistoryService {

    private final Context mContext;
    private final TrackerRepository mTrackerRepository;

    public DynamicCardHistoryService(Context context) {
        mContext = context;
        mTrackerRepository = new TrackerRepository(context);
    }

    /**
     * Today's record of a simple tracker - workout, medication or supplement -
     * or null when it was not marked.
     */
    public TrackerRecord getTodayRecord(TrackerType type) {
        return mTrackerRepository.getTrackerRecordForDay(type, System.currentTimeMillis());
    }

    /**
     * Removes the mark of a day, so the card goes back to "not completed", where
     * it can be marked again.
     */
    public void removeRecord(long recordId) {
        mTrackerRepository.deleteTrackerRecord(recordId);
    }

    /**
     * Opens the today history panel of one simple card. The panel is rendered by
     * CardHistoryDialog and the tracker is what tells it which card - workout,
     * medication or supplement - is being listed.
     */
    public void showDailyHistoryDialog(Tracker tracker, Runnable onChanged) {
        new CardHistoryDialog(mContext, new DynamicCardHistory(mContext, tracker, this)).show(onChanged);
    }

    public void closeDb() {
        mTrackerRepository.closeDb();
    }
}
