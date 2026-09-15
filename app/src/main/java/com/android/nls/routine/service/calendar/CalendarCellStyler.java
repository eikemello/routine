package com.android.nls.routine.service.calendar;

import android.content.Context;
import android.widget.TextView;
import androidx.appcompat.content.res.AppCompatResources;
import com.android.nls.routine.R;
import com.android.nls.routine.model.DayStatus;
import com.android.nls.routine.model.DayStatusInfo;

/**
 * Single source of truth for painting a calendar day cell: maps a day's score
 * status to its background drawable and handles the selected/today outline.
 * Shared by the history calendar (HistoryCalendarRenderer) and the home week
 * preview (WeekPreviewRenderer) so both screens always paint days identically.
 */
public final class CalendarCellStyler {

    private CalendarCellStyler() {
    }

    /**
     * Resolves the background drawable of a day cell from its score status.
     * Days after today keep the neutral background.
     */
    public static int backgroundResourceFor(DayStatus status, DayStatusInfo info, boolean isFutureDay) {
        if (isFutureDay) {
            return R.drawable.calendar_day_background;
        }
        return switch (status) {
            case GREEN -> R.drawable.calendar_day_green;
            case YELLOW -> R.drawable.calendar_day_yellow;
            case RED -> R.drawable.calendar_day_red;
            default -> {
                if (status == DayStatus.NONE && info != null && info.hasData()) {
                    yield R.drawable.calendar_day_has_data;
                }
                yield R.drawable.calendar_day_background;
            }
        };
    }

    /** Draws the neon outline used to mark the selected/today cell. */
    public static void drawOutline(Context context, TextView cell) {
        cell.setForeground(AppCompatResources.getDrawable(context, R.drawable.calendar_day_selected_outline));
    }

    /** Removes the neon outline from a cell. */
    public static void clearOutline(TextView cell) {
        cell.setForeground(null);
    }
}