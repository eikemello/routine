package com.android.nls.routine.service.score;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.nls.routine.R;
import com.android.nls.routine.model.DayDetails;
import com.android.nls.routine.model.ExpenseRecord;
import com.android.nls.routine.model.MealRecord;
import com.android.nls.routine.model.Tracker;
import com.android.nls.routine.model.TrackerRecord;
import com.android.nls.routine.model.TrackerType;
import com.android.nls.routine.model.WaterRecord;
import com.android.nls.routine.repository.ConfigRepository;
import com.android.nls.routine.service.HistoryService;
import com.android.nls.routine.utils.Constants;

import java.util.List;

/**
 * Renders the day details grid with icon cells for each enabled tracker.
 * Handles tracker icons, titles, values, and status-based coloring.
 */
public class DayDetailsRenderer {

    private final Context mContext;
    private final GridLayout mDayDetailsGrid;
    private final HistoryService mHistoryService;
    private final ConfigRepository mConfigRepository;

    public DayDetailsRenderer(Context context,
                              GridLayout dayDetailsGrid,
                              HistoryService historyService,
                              ConfigRepository configRepository) {
        mContext = context;
        mDayDetailsGrid = dayDetailsGrid;
        mHistoryService = historyService;
        mConfigRepository = configRepository;
    }

    /**
     * Renders the day details as a 2-column grid of small squares.
     * Each square shows the tracker icon, a title, and the value.
     * Only enabled trackers are shown.
     */
    public void render(DayDetails details) {
        mDayDetailsGrid.removeAllViews();

        List<Tracker> enabledTrackers = mHistoryService.getEnabledTrackers();
        if (enabledTrackers.isEmpty()) {
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(mContext);
        int column = 0;

        for (Tracker tracker : enabledTrackers) {
            View cell = inflater.inflate(R.layout.item_day_detail, mDayDetailsGrid, false);
            ImageView imgIcon = cell.findViewById(R.id.imgDetailIcon);
            TextView txtTitle = cell.findViewById(R.id.txtDetailTitle);
            TextView txtValue = cell.findViewById(R.id.txtDetailValue);

            imgIcon.setImageResource(getIconForTracker(tracker.type()));
            txtTitle.setText(getTrackerTitle(tracker));
            txtValue.setText(getTrackerValue(tracker.type(), details));
            txtValue.setTextColor(getValueColor(tracker.type(), details));

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(column, 1f);
            params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
            cell.setLayoutParams(params);

            mDayDetailsGrid.addView(cell);

            // Alternate columns: 0, 1, 0, 1...
            column = (column + 1) % 2;
        }
    }

    /**
     * Returns the drawable resource for a tracker type icon.
     */
    private int getIconForTracker(TrackerType type) {
        return switch (type) {
            case WATER -> R.drawable.ic_water;
            case MEALS -> R.drawable.ic_meal;
            case EXPENSES -> R.drawable.ic_credit_card;
            case WORKOUT -> R.drawable.ic_workout;
            case MEDICATION -> R.drawable.ic_medication;
            case SUPPLEMENT -> R.drawable.ic_supplement;
        };
    }

    private String getTrackerTitle(Tracker tracker) {
        if (tracker.name() != null && !tracker.name().isEmpty()
                && !tracker.name().equalsIgnoreCase(tracker.type().name())) {
            return tracker.name();
        }
        return switch (tracker.type()) {
            case WATER -> mContext.getString(R.string.water);
            case MEALS -> mContext.getString(R.string.meal);
            case EXPENSES -> mContext.getString(R.string.expenses);
            case WORKOUT -> mContext.getString(R.string.workout);
            case MEDICATION -> mContext.getString(R.string.medication);
            case SUPPLEMENT -> mContext.getString(R.string.supplement);
        };
    }

    /**
     * Returns the value text to display for a tracker on the given day.
     */
    private String getTrackerValue(TrackerType type, DayDetails details) {
        switch (type) {
            case WATER:
                int waterSum = 0;
                for (WaterRecord record : details.waterRecords()) {
                    waterSum += record.amount();
                }
                return waterSum + " ml";

            case MEALS:
                if (details.mealRecords().isEmpty()) {
                    return mContext.getString(R.string.not_completed);
                }
                // Show the count of logged meals
                return details.mealRecords().size() + "/4";

            case EXPENSES:
                double expenseSum = 0;
                for (ExpenseRecord record : details.expenseRecords()) {
                    expenseSum += record.amount();
                }
                return String.format("$%.2f", expenseSum);

            case WORKOUT:
                return getTrackerCompletionValue(details.workoutRecords());

            case MEDICATION:
                return getTrackerCompletionValue(details.medicationRecords());

            case SUPPLEMENT:
                return getTrackerCompletionValue(details.supplementRecords());

            default:
                return "";
        }
    }

    private String getTrackerCompletionValue(List<TrackerRecord> records) {
        if (records.isEmpty()) {
            return mContext.getString(R.string.not_completed);
        }
        // A tracker is considered completed if any record is completed
        for (TrackerRecord record : records) {
            if (record.completed()) {
                return mContext.getString(R.string.completed);
            }
        }
        return mContext.getString(R.string.not_completed);
    }

    private int getValueColor(TrackerType type, DayDetails details) {
        switch (type) {
            case WATER:
                int waterSum = 0;
                for (WaterRecord record : details.waterRecords()) {
                    waterSum += record.amount();
                }
                double dailyGoal = mConfigRepository.getDailyWaterGoal();
                if (waterSum >= dailyGoal) {
                    return mContext.getColor(R.color.calendar_green);
                } else if (waterSum > 0) {
                    return mContext.getColor(R.color.calendar_yellow);
                }
                return mContext.getColor(R.color.white);

            case MEALS:
                if (details.mealRecords().isEmpty()) {
                    return mContext.getColor(R.color.white);
                }
                boolean hasWarning = false;
                for (MealRecord record : details.mealRecords()) {
                    if (Constants.WARNING_MEAL.equals(record.status())) {
                        hasWarning = true;
                    } else if (Constants.WRONG_MEAL.equals(record.status())) {
                        return mContext.getColor(R.color.calendar_red);
                    }
                }
                if (hasWarning) {
                    return mContext.getColor(R.color.calendar_yellow);
                }
                return mContext.getColor(R.color.calendar_green);

            case EXPENSES:
                return mContext.getColor(R.color.white);

            case WORKOUT:
                return getTrackerCompletionColor(details.workoutRecords());

            case MEDICATION:
                return getTrackerCompletionColor(details.medicationRecords());

            case SUPPLEMENT:
                return getTrackerCompletionColor(details.supplementRecords());

            default:
                return mContext.getColor(R.color.white);
        }
    }

    private int getTrackerCompletionColor(List<TrackerRecord> records) {
        if (records.isEmpty()) {
            return mContext.getColor(R.color.white);
        }
        for (TrackerRecord record : records) {
            if (record.completed()) {
                return mContext.getColor(R.color.calendar_green);
            }
        }
        return mContext.getColor(R.color.white);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * mContext.getResources().getDisplayMetrics().density);
    }
}