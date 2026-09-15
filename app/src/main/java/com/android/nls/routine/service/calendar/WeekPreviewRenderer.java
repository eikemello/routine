package com.android.nls.routine.service.calendar;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.nls.routine.R;
import com.android.nls.routine.model.DayStatus;
import com.android.nls.routine.model.DayStatusInfo;
import com.android.nls.routine.service.HistoryService;
import com.android.nls.routine.utils.Common;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;

/**
 * Renders the read-only week preview shown on the home screen below the date:
 * seven day-number squares painted with the same score colors used by the
 * history calendar. Days are purely informative (no click handling).
 */
public class WeekPreviewRenderer {

    private static final int CELL_HEIGHT_DP = 40;
    private static final int CELL_MARGIN_DP = 2;

    private final Context mContext;
    private final LinearLayout mContainer;
    private final HistoryService mHistoryService;

    public WeekPreviewRenderer(Context context, LinearLayout container, HistoryService historyService) {
        mContext = context;
        mContainer = container;
        mHistoryService = historyService;
    }

    /**
     * Paints the squares of the current week (Monday through Sunday) with each
     * day's score color. Days after today keep the neutral background.
     */
    public void render() {
        mContainer.removeAllViews();

        long todayStart = Common.getStartOfDayInMillis();
        long weekStart = Common.getStartOfWeekInMillis();
        long weekEnd = Common.getEndOfWeekInMillis();
        Map<Long, DayStatusInfo> dayStatuses = mHistoryService.getDayStatusesForRange(weekStart, weekEnd);

        Calendar day = new GregorianCalendar();
        day.setTimeInMillis(weekStart);
        while (day.getTimeInMillis() <= weekEnd) {
            mContainer.addView(createDayCell(day, dayStatuses, todayStart));
            day.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private TextView createDayCell(Calendar day, Map<Long, DayStatusInfo> dayStatuses, long todayStart) {
        long dayStart = Common.getStartOfDayInMillis(day.getTimeInMillis());
        DayStatusInfo info = dayStatuses.get(dayStart);
        DayStatus status = info != null ? info.status() : DayStatus.NONE;

        TextView cell = new TextView(mContext);
        cell.setText(String.valueOf(day.get(Calendar.DAY_OF_MONTH)));
        cell.setGravity(Gravity.CENTER);
        cell.setTextSize(12);
        cell.setTypeface(Typeface.DEFAULT_BOLD);
        cell.setTextColor(mContext.getColor(R.color.white));
        // Spoken label for accessibility (the visible text is just the number).
        cell.setContentDescription(Common.getDateFromTimestamp(day.getTimeInMillis()));
        cell.setLayoutParams(createCellParams());
        cell.setBackgroundResource(CalendarCellStyler.backgroundResourceFor(status, info, dayStart > todayStart));

        if (dayStart == todayStart) {
            // Today keeps the same outline the history calendar uses for the selected day.
            CalendarCellStyler.drawOutline(mContext, cell);
        }
        return cell;
    }

    private LinearLayout.LayoutParams createCellParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, Common.dpToPx(mContext, CELL_HEIGHT_DP), 1f);
        params.setMargins(Common.dpToPx(mContext, CELL_MARGIN_DP), 0, Common.dpToPx(mContext, CELL_MARGIN_DP), 0);
        return params;
    }
}