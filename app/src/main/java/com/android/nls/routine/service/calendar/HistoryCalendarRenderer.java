package com.android.nls.routine.service.calendar;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.TextView;
import androidx.appcompat.content.res.AppCompatResources;
import com.android.nls.routine.R;
import com.android.nls.routine.model.DayStatus;
import com.android.nls.routine.model.DayStatusInfo;
import com.android.nls.routine.service.HistoryService;
import com.android.nls.routine.utils.Common;
import com.google.android.material.button.MaterialButton;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;

/**
 * Renders the week/month calendar grid and handles cell highlighting,
 * navigation, and timestamp resolution.
 */
public class HistoryCalendarRenderer {

    public interface OnDaySelectedListener {
        void onDaySelected(long timestamp, TextView cell);
    }

    private final Context mContext;
    private final GridLayout mCalendarGrid;
    private final TextView mTxtMonthYear;
    private final MaterialButton mBtnToggleView;
    private final HistoryService mHistoryService;
    private final OnDaySelectedListener mDaySelectedListener;
    private final Calendar mCurrentMonth;
    private final Calendar mCurrentWeek;
    private boolean mIsWeekView = true;
    private Map<Long, DayStatusInfo> mDayStatusCache;

    public HistoryCalendarRenderer(Context context,
                                   GridLayout calendarGrid,
                                   TextView txtMonthYear,
                                   MaterialButton btnToggleView,
                                   HistoryService historyService,
                                   OnDaySelectedListener daySelectedListener) {
        mContext = context;
        mCalendarGrid = calendarGrid;
        mTxtMonthYear = txtMonthYear;
        mBtnToggleView = btnToggleView;
        mHistoryService = historyService;
        mDaySelectedListener = daySelectedListener;
        mCurrentMonth = initializeCurrentMonth();
        mCurrentWeek = initializeCurrentWeek();
    }

    public void renderCalendar() {
        mCalendarGrid.removeAllViews();

        long rangeStart;
        long rangeEnd;
        if (mIsWeekView) {
            rangeStart = Common.getStartOfDayInMillis(mCurrentWeek.getTimeInMillis());
            Calendar weekEndCal = (Calendar) mCurrentWeek.clone();
            weekEndCal.add(Calendar.DAY_OF_MONTH, 6);
            rangeEnd = Common.getEndOfDayInMillis(weekEndCal.getTimeInMillis());
        } else {
            rangeStart = Common.getStartOfDayInMillis(mCurrentMonth.getTimeInMillis());
            Calendar monthEndCal = (Calendar) mCurrentMonth.clone();
            monthEndCal.set(Calendar.DAY_OF_MONTH, monthEndCal.getActualMaximum(Calendar.DAY_OF_MONTH));
            rangeEnd = Common.getEndOfDayInMillis(monthEndCal.getTimeInMillis());
        }
        mDayStatusCache = mHistoryService.getDayStatusesForRange(rangeStart, rangeEnd);

        if (mIsWeekView) {
            renderWeekView();
        } else {
            renderMonthView();
        }
    }

    public void navigatePrevious() {
        if (mIsWeekView) {
            mCurrentWeek.add(Calendar.WEEK_OF_YEAR, -1);
        } else {
            mCurrentMonth.add(Calendar.MONTH, -1);
        }
        renderCalendar();
    }

    public void navigateNext() {
        if (mIsWeekView) {
            mCurrentWeek.add(Calendar.WEEK_OF_YEAR, 1);
        } else {
            mCurrentMonth.add(Calendar.MONTH, 1);
        }
        renderCalendar();
    }

    public void toggleView() {
        mIsWeekView = !mIsWeekView;
        renderCalendar();
    }

    public boolean isWeekView() {
        return mIsWeekView;
    }

    public Calendar getCurrentMonth() {
        return mCurrentMonth;
    }

    public DayStatusInfo getDayStatus(long timestamp) {
        return mDayStatusCache.get(Common.getStartOfDayInMillis(timestamp));
    }

    public void selectToday() {
        Calendar today = Calendar.getInstance();
        long todayTimestamp = today.getTimeInMillis();
        long todayStart = Common.getStartOfDayInMillis(todayTimestamp);
        int todayDay = today.get(Calendar.DAY_OF_MONTH);

        if (mIsWeekView) {
            long weekStart = Common.getStartOfDayInMillis(mCurrentWeek.getTimeInMillis());
            Calendar weekEndCal = (Calendar) mCurrentWeek.clone();
            weekEndCal.add(Calendar.DAY_OF_MONTH, 6);
            long weekEnd = Common.getStartOfDayInMillis(weekEndCal.getTimeInMillis());

            if (todayStart >= weekStart && todayStart <= weekEnd) {
                TextView todayCell = findCellByDayNumber(todayDay);
                if (todayCell != null) {
                    mDaySelectedListener.onDaySelected(todayTimestamp, todayCell);
                    return;
                }
            }
        } else {
            int year = mCurrentMonth.get(Calendar.YEAR);
            int month = mCurrentMonth.get(Calendar.MONTH);

            if (today.get(Calendar.YEAR) == year && today.get(Calendar.MONTH) == month) {
                TextView todayCell = findCellByDayNumber(todayDay);
                if (todayCell != null) {
                    mDaySelectedListener.onDaySelected(todayTimestamp, todayCell);
                    return;
                }
            }
        }

        mDaySelectedListener.onDaySelected(todayTimestamp, null);
    }

    /**
     * Resets all cell backgrounds to their status-based color and
     * highlights the clicked cell with an outline.
     */
    public void highlightDay(long timestamp, TextView clickedCell) {
        long todayStart = Common.getStartOfDayInMillis(System.currentTimeMillis());
        for (int i = 0; i < mCalendarGrid.getChildCount(); i++) {
            View child = mCalendarGrid.getChildAt(i);
            if (child instanceof TextView && child.getVisibility() == View.VISIBLE) {
                child.setSelected(false);
                child.setForeground(null);
                ((TextView) child).setTextColor(mContext.getColor(R.color.white));
                long cellTimestamp = getTimestampFromCell((TextView) child);
                if (cellTimestamp > todayStart) {
                    child.setBackgroundResource(R.drawable.calendar_day_background);
                    continue;
                }
                DayStatusInfo cellInfo = mDayStatusCache.get(Common.getStartOfDayInMillis(cellTimestamp));
                DayStatus cellStatus = cellInfo != null ? cellInfo.status() : DayStatus.NONE;
                child.setBackgroundResource(getCellBackgroundResource(cellStatus, cellInfo));
            }
        }

        if (clickedCell != null) {
            clickedCell.setSelected(true);
            clickedCell.setForeground(AppCompatResources.getDrawable(mContext, R.drawable.calendar_day_selected_outline));
        }
    }

    public long getTimestampFromCell(TextView cell) {
        Object tag = cell.getTag();
        if (tag instanceof Long) {
            return (Long) tag;
        }

        // Fallback: resolve from day number (for cells created before tags were set)
        String text = cell.getText().toString();
        int day = Integer.parseInt(text);

        if (mIsWeekView) {
            Calendar dayCal = (Calendar) mCurrentWeek.clone();
            for (int i = 0; i < 7; i++) {
                if (dayCal.get(Calendar.DAY_OF_MONTH) == day) {
                    return dayCal.getTimeInMillis();
                }
                dayCal.add(Calendar.DAY_OF_MONTH, 1);
            }
            return mCurrentWeek.getTimeInMillis();
        } else {
            int year = mCurrentMonth.get(Calendar.YEAR);
            int month = mCurrentMonth.get(Calendar.MONTH);
            Calendar cal = new GregorianCalendar(year, month, day);
            return cal.getTimeInMillis();
        }
    }

    private TextView findCellByDayNumber(int dayNumber) {
        for (int i = 0; i < mCalendarGrid.getChildCount(); i++) {
            View child = mCalendarGrid.getChildAt(i);
            if (child instanceof TextView tv && child.getVisibility() == View.VISIBLE) {
                if (tv.getText().toString().equals(String.valueOf(dayNumber))) {
                    return tv;
                }
            }
        }
        return null;
    }

    private void renderWeekView() {
        Calendar weekEnd = (Calendar) mCurrentWeek.clone();
        weekEnd.add(Calendar.DAY_OF_MONTH, 6);
        String startDate = Common.getMonthYearFromTimestamp(mCurrentWeek.getTimeInMillis());
        String endDate = Common.getMonthYearFromTimestamp(weekEnd.getTimeInMillis());

        if (startDate.equals(endDate)) {
            mTxtMonthYear.setText(startDate);
        } else {
            mTxtMonthYear.setText(Common.getAbbreviatedMonthFromTimestamp(mCurrentWeek.getTimeInMillis()) +
                    " - " + Common.getAbbreviatedMonthYearFromTimestamp(weekEnd.getTimeInMillis()));
        }

        mBtnToggleView.setText(mContext.getString(R.string.month_view));

        Calendar today = Calendar.getInstance();
        long todayStart = Common.getStartOfDayInMillis(today.getTimeInMillis());

        for (int i = 0; i < 7; i++) {
            Calendar dayCal = (Calendar) mCurrentWeek.clone();
            dayCal.add(Calendar.DAY_OF_MONTH, i);
            long dayTimestamp = dayCal.getTimeInMillis();
            boolean isFutureDay = dayTimestamp > todayStart;

            TextView dayCell = createDayCell(String.valueOf(dayCal.get(Calendar.DAY_OF_MONTH)), dayTimestamp);
            if (isFutureDay) {
                dayCell.setBackgroundResource(R.drawable.calendar_day_background);
            } else {
                DayStatusInfo info = mDayStatusCache.get(Common.getStartOfDayInMillis(dayTimestamp));
                DayStatus dayStatus = info != null ? info.status() : DayStatus.NONE;
                dayCell.setBackgroundResource(getCellBackgroundResource(dayStatus, info));
            }

            dayCell.setClickable(true);
            dayCell.setFocusable(true);

            final long timestamp = dayTimestamp;
            dayCell.setOnClickListener(v -> mDaySelectedListener.onDaySelected(timestamp, dayCell));

            mCalendarGrid.addView(dayCell);
        }
    }

    private void renderMonthView() {
        mTxtMonthYear.setText(Common.getMonthYearFromTimestamp(mCurrentMonth.getTimeInMillis()));
        mBtnToggleView.setText(mContext.getString(R.string.week_view));

        int year = mCurrentMonth.get(Calendar.YEAR);
        int month = mCurrentMonth.get(Calendar.MONTH);
        int firstDayOfWeek = getFirstDayOfWeek(year, month);
        int daysInMonth = mCurrentMonth.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int i = 0; i < firstDayOfWeek; i++) {
            TextView emptyCell = new TextView(mContext);
            emptyCell.setLayoutParams(createCellParams());
            emptyCell.setVisibility(View.INVISIBLE);
            mCalendarGrid.addView(emptyCell);
        }

        Calendar today = Calendar.getInstance();
        long todayStart = Common.getStartOfDayInMillis(today.getTimeInMillis());

        for (int day = 1; day <= daysInMonth; day++) {
            Calendar dayCal = new GregorianCalendar(year, month, day);
            long dayTimestamp = dayCal.getTimeInMillis();
            boolean isFutureDay = dayTimestamp > todayStart;

            TextView dayCell = createDayCell(String.valueOf(day), dayTimestamp);
            if (isFutureDay) {
                dayCell.setBackgroundResource(R.drawable.calendar_day_background);
            } else {
                DayStatusInfo info = mDayStatusCache.get(Common.getStartOfDayInMillis(dayTimestamp));
                DayStatus dayStatus = info != null ? info.status() : DayStatus.NONE;
                dayCell.setBackgroundResource(getCellBackgroundResource(dayStatus, info));
            }

            dayCell.setClickable(true);
            dayCell.setFocusable(true);

            final long timestamp = dayTimestamp;
            dayCell.setOnClickListener(v -> mDaySelectedListener.onDaySelected(timestamp, dayCell));

            mCalendarGrid.addView(dayCell);
        }
    }

    private TextView createDayCell(String dayText, long timestamp) {
        TextView dayCell = new TextView(mContext);
        dayCell.setText(dayText);
        dayCell.setGravity(Gravity.CENTER);
        dayCell.setTextSize(16);
        dayCell.setTextColor(mContext.getColor(R.color.white));
        dayCell.setLayoutParams(createCellParams());
        dayCell.setTag(timestamp);
        return dayCell;
    }

    private GridLayout.LayoutParams createCellParams() {
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = dpToPx(45);
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));
        return params;
    }

    private int getCellBackgroundResource(DayStatus status, DayStatusInfo info) {
        switch (status) {
            case GREEN:
                return R.drawable.calendar_day_green;
            case YELLOW:
                return R.drawable.calendar_day_yellow;
            case RED:
                return R.drawable.calendar_day_red;
            default:
                if (status == DayStatus.NONE && info != null && info.hasData()) {
                    return R.drawable.calendar_day_has_data;
                }
                return R.drawable.calendar_day_background;
        }
    }

    private int getFirstDayOfWeek(int year, int month) {
        Calendar calendar = new GregorianCalendar(year, month, 1);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        // Convert to 0-based with Monday = 0
        return (dayOfWeek + 5) % 7;
    }

    private Calendar initializeCurrentMonth() {
        Calendar calendar = new GregorianCalendar();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    private Calendar initializeCurrentWeek() {
        Calendar calendar = new GregorianCalendar();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // Calculate the Monday of the current week explicitly.
        // Do NOT use set(DAY_OF_WEEK, MONDAY): it depends on the locale's
        // firstDayOfWeek (e.g. SUNDAY in en-US), which can yield the wrong Monday
        // (e.g. on a Sunday it would return the NEXT week's Monday).
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int daysFromMonday = (dayOfWeek + 5) % 7; // Sunday=1 -> 6, Monday=2 -> 0, ..., Saturday=7 -> 5
        calendar.add(Calendar.DAY_OF_MONTH, -daysFromMonday);
        return calendar;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * mContext.getResources().getDisplayMetrics().density);
    }
}