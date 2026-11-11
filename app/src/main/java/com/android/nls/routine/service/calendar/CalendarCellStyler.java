package com.android.nls.routine.service.calendar;

import android.content.Context;
import android.content.res.ColorStateList;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.ColorRes;
import androidx.appcompat.content.res.AppCompatResources;
import com.android.nls.routine.R;
import com.android.nls.routine.model.DayStatus;
import com.android.nls.routine.model.DayStatusInfo;
import com.google.android.material.progressindicator.LinearProgressIndicator;

/**
 * Single source of truth for painting a day by its score status: the
 * background drawable and number color of a calendar day cell, the
 * selected/today outline and the status color pair (vivid + deep) used by
 * the day ring and the score pill. Shared by the history calendar
 * (HistoryCalendarRenderer), the home week preview (WeekPreviewRenderer),
 * the home ring and the history score so every screen paints days identically.
 * The home cards reuse the same pair on their icon and progress bar.
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

    /**
     * Color of the day number: the vivid status color over the deep status
     * fill of a scored day; the plain text color otherwise, muted for days
     * after today.
     */
    @ColorRes
    public static int textColorFor(DayStatus status, boolean isFutureDay) {
        if (isFutureDay) {
            return R.color.text_muted;
        }
        return switch (status) {
            case GREEN, YELLOW, RED -> statusColorRes(status);
            default -> R.color.text_primary;
        };
    }

    /** Vivid color of a status: what is drawn on top (indicator, text). */
    @ColorRes
    public static int statusColorRes(DayStatus status) {
        return switch (status) {
            case GREEN -> R.color.status_good;
            case YELLOW -> R.color.status_warning;
            case RED -> R.color.status_bad;
            default -> R.color.accent;
        };
    }

    /** Deep tone of a status: the base the vivid color sits on (track, fill). */
    @ColorRes
    public static int statusDeepColorRes(DayStatus status) {
        return switch (status) {
            case GREEN -> R.color.status_good_deep;
            case YELLOW -> R.color.status_warning_deep;
            case RED -> R.color.status_bad_deep;
            default -> R.color.accent_deep;
        };
    }

    /**
     * Paints a home card icon with a status: the deep tone fills its box and
     * the vivid tone tints the glyph. NONE keeps the neutral accent pair.
     */
    public static void tintIcon(ImageView icon, DayStatus status) {
        Context context = icon.getContext();
        icon.setBackgroundTintList(ColorStateList.valueOf(context.getColor(statusDeepColorRes(status))));
        icon.setImageTintList(ColorStateList.valueOf(context.getColor(statusColorRes(status))));
    }

    /** Paints a progress bar with a status: deep track, vivid indicator. */
    public static void tintProgress(LinearProgressIndicator progress, DayStatus status) {
        Context context = progress.getContext();
        progress.setTrackColor(context.getColor(statusDeepColorRes(status)));
        progress.setIndicatorColor(context.getColor(statusColorRes(status)));
    }

    /** Draws the outline used to mark the selected/today cell. */
    public static void drawOutline(Context context, TextView cell) {
        cell.setForeground(AppCompatResources.getDrawable(context, R.drawable.calendar_day_selected_outline));
    }

    /** Removes the outline from a cell. */
    public static void clearOutline(TextView cell) {
        cell.setForeground(null);
    }
}