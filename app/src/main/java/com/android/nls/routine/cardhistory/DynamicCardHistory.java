package com.android.nls.routine.cardhistory;

import android.content.Context;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import com.android.nls.routine.R;
import com.android.nls.routine.model.Tracker;
import com.android.nls.routine.model.TrackerRecord;
import com.android.nls.routine.utils.Common;
import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic card history of the simple cards - workout, medication and supplement.
 * They are called dynamic because this single implementation answers for all
 * three of them: the card hands over its own tracker (type, name and
 * description) and that is what makes the difference, instead of one history
 * class per card. A simple card has a single value per day - it was completed or
 * it was not - so the panel holds at most one row, the moment it was marked, and
 * offers only removing it: there is no amount, status or observation to edit.
 */
public class DynamicCardHistory implements CardHistory {

    private final Context mContext;
    private final Tracker mTracker;
    private final DynamicCardHistoryService mDynamicService;

    public DynamicCardHistory(Context context, Tracker tracker, DynamicCardHistoryService dynamicService) {
        mContext = context;
        mTracker = tracker;
        mDynamicService = dynamicService;
    }

    /**
     * Today's record of the card - the day was marked or it was not - as the
     * single row of the panel, with the name and the description configured on
     * the card beside the status. The given refresh handle repaints the list
     * after the record is removed.
     */
    @Override
    public CardHistory.Panel getHistoryPanel(Runnable refresh) {
        List<CardHistory.Row> rows = new ArrayList<>();
        TrackerRecord record = mDynamicService.getTodayRecord(mTracker.type());

        if (record != null) {
            rows.add(new CardHistory.Row(
                    Common.getHourFromTimestamp(String.valueOf(record.timestamp())),
                    statusLabel(record),
                    trackerName(),
                    trackerDescription(),
                    // Editing is not offered: the card is a single completion
                    // flag, and removing the row already returns it to its "not
                    // completed" state, where it can be marked again
                    null,
                    () -> confirmDeleteRecord(record, refresh)));
        }

        // The row already is the whole day, so there is no total to add up
        return new CardHistory.Panel(rows, null);
    }

    @Override
    public int getHistoryTitleRes() {
        return resources().titleRes();
    }

    @Override
    public int getHistoryEmptyTextRes() {
        return resources().emptyTextRes();
    }

    @Override
    public int getHistoryIconRes() {
        return resources().iconRes();
    }

    @Override
    public int getHistoryIconTintRes() {
        return resources().iconTintRes();
    }

    @Override
    public int getHistoryValueColorRes() {
        return R.color.green_dark;
    }

    /**
     * Asks before removing the mark of the day, then refreshes the panel and the
     * card behind it.
     */
    private void confirmDeleteRecord(TrackerRecord record, Runnable refresh) {
        CardRecordDialog.confirmRemove(
                mContext,
                R.string.simple_card_record_delete_title,
                mContext.getString(R.string.simple_card_record_delete_message, displayName(),
                        Common.getHourFromTimestamp(String.valueOf(record.timestamp()))),
                () -> {
                    mDynamicService.removeRecord(record.id());
                    refresh.run();
                });
    }

    /** The value of the row: the day was marked or it was not. */
    private String statusLabel(TrackerRecord record) {
        return mContext.getString(record.completed() ? R.string.completed : R.string.not_completed);
    }

    /** The name configured on the card, or the default title of the card. */
    private String displayName() {
        String name = trackerName();
        return name != null ? name : mContext.getString(resources().nameRes());
    }

    /**
     * The name typed on the tracker configuration ("Creatine", "Vitamin D"), or
     * null when the user kept the default one - the same rule the cards apply.
     */
    private String trackerName() {
        String name = mTracker.name();
        if (name != null && !name.isBlank() && !name.equalsIgnoreCase(mTracker.type().name())) {
            return name;
        }
        return null;
    }

    /** The description typed on the tracker configuration, or null when empty. */
    private String trackerDescription() {
        String description = mTracker.description();
        if (description != null && !description.isBlank()) {
            return description;
        }
        return null;
    }

    /**
     * Everything that changes from one simple card to the other: the panel
     * texts, the icon and the default title of the card. Only the simple cards
     * reach this panel, so the last case is just a safe fallback.
     */
    private SimpleCardResources resources() {
        return switch (mTracker.type()) {
            case WORKOUT -> new SimpleCardResources(R.string.workout_history, R.string.no_workout_today,
                    R.drawable.ic_workout, R.color.neon_purple_40, R.string.workout);
            case MEDICATION -> new SimpleCardResources(R.string.medication_history, R.string.no_medication_today,
                    R.drawable.ic_medication, R.color.neon_pink_40, R.string.medication);
            case SUPPLEMENT -> new SimpleCardResources(R.string.supplement_history, R.string.no_supplement_today,
                    R.drawable.ic_supplement, R.color.neon_cyan_40, R.string.supplement);
            default -> new SimpleCardResources(R.string.simple_card_history, R.string.no_simple_card_today,
                    R.drawable.ic_workout, R.color.neon_purple_40, R.string.workout);
        };
    }

    /** Resources that differ per simple card: workout, medication, supplement. */
    private record SimpleCardResources(@StringRes int titleRes,
                                       @StringRes int emptyTextRes,
                                       @DrawableRes int iconRes,
                                       @ColorRes int iconTintRes,
                                       @StringRes int nameRes) {
    }
}
