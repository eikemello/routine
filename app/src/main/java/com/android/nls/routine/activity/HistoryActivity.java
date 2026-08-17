package com.android.nls.routine.activity;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.view.WindowInsetsControllerCompat;
import com.android.nls.routine.R;
import com.android.nls.routine.model.DayDetails;
import com.android.nls.routine.model.DayStatus;
import com.android.nls.routine.model.ExpenseRecord;
import com.android.nls.routine.model.MealRecord;
import com.android.nls.routine.model.TrackerRecord;
import com.android.nls.routine.model.WaterRecord;
import com.android.nls.routine.model.WeeklySummary;
import com.android.nls.routine.service.HistoryService;
import com.android.nls.routine.utils.Common;
import com.android.nls.routine.utils.Constants;
import com.google.android.material.button.MaterialButton;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class HistoryActivity extends AppCompatActivity {
    private HistoryService mHistoryService;

    // Weekly summary views
    private TextView txtWeeklyWater;
    private TextView txtWeeklySpent;
    private TextView txtWeeklyMeals;

    // Calendar views
    private TextView txtMonthYear;
    private GridLayout calendarGrid;
    private ImageButton btnPrevMonth;
    private ImageButton btnNextMonth;
    private MaterialButton btnToggleView;

    // Day details views
    private TextView txtSelectedDate;
    private TextView txtNoData;
    private TextView txtWaterSectionTitle;
    private TextView txtMealSectionTitle;
    private TextView txtExpenseSectionTitle;
    private TextView txtWorkoutSectionTitle;
    private TextView txtMedicationSectionTitle;
    private TextView txtSupplementSectionTitle;
    private LinearLayout waterDetailsContainer;
    private LinearLayout mealDetailsContainer;
    private LinearLayout expenseDetailsContainer;
    private LinearLayout workoutDetailsContainer;
    private LinearLayout medicationDetailsContainer;
    private LinearLayout supplementDetailsContainer;

    private Calendar mCurrentMonth;
    private Calendar mCurrentWeek;
    private long mSelectedDayTimestamp;
    private boolean mIsWeekView = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history);
        WindowInsetsControllerCompat windowInsetsController = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

        mHistoryService = new HistoryService(this);
        mCurrentMonth = new GregorianCalendar();
        mCurrentMonth.set(Calendar.DAY_OF_MONTH, 1);
        mCurrentMonth.set(Calendar.HOUR_OF_DAY, 0);
        mCurrentMonth.set(Calendar.MINUTE, 0);
        mCurrentMonth.set(Calendar.SECOND, 0);
        mCurrentMonth.set(Calendar.MILLISECOND, 0);

        mCurrentWeek = new GregorianCalendar();
        mCurrentWeek.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        mCurrentWeek.set(Calendar.HOUR_OF_DAY, 0);
        mCurrentWeek.set(Calendar.MINUTE, 0);
        mCurrentWeek.set(Calendar.SECOND, 0);
        mCurrentWeek.set(Calendar.MILLISECOND, 0);

        startUIComponents();
        setupButtonListeners();
        loadWeeklySummary();
        renderCalendar();
        selectToday();
    }

    private void startUIComponents() {
        txtWeeklyWater = findViewById(R.id.txtWeeklyWater);
        txtWeeklySpent = findViewById(R.id.txtWeeklySpent);
        txtWeeklyMeals = findViewById(R.id.txtWeeklyMeals);

        txtMonthYear = findViewById(R.id.txtMonthYear);
        calendarGrid = findViewById(R.id.calendarGrid);
        btnPrevMonth = findViewById(R.id.btnPrevMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);
        btnToggleView = findViewById(R.id.btnToggleView);

        txtSelectedDate = findViewById(R.id.txtSelectedDate);
        txtNoData = findViewById(R.id.txtNoData);
        txtWaterSectionTitle = findViewById(R.id.txtWaterSectionTitle);
        txtMealSectionTitle = findViewById(R.id.txtMealSectionTitle);
        txtExpenseSectionTitle = findViewById(R.id.txtExpenseSectionTitle);
        txtWorkoutSectionTitle = findViewById(R.id.txtWorkoutSectionTitle);
        txtMedicationSectionTitle = findViewById(R.id.txtMedicationSectionTitle);
        txtSupplementSectionTitle = findViewById(R.id.txtSupplementSectionTitle);
        waterDetailsContainer = findViewById(R.id.waterDetailsContainer);
        mealDetailsContainer = findViewById(R.id.mealDetailsContainer);
        expenseDetailsContainer = findViewById(R.id.expenseDetailsContainer);
        workoutDetailsContainer = findViewById(R.id.workoutDetailsContainer);
        medicationDetailsContainer = findViewById(R.id.medicationDetailsContainer);
        supplementDetailsContainer = findViewById(R.id.supplementDetailsContainer);
    }

    private void setupButtonListeners() {
        btnPrevMonth.setOnClickListener(v -> {
            if (mIsWeekView) {
                mCurrentWeek.add(Calendar.WEEK_OF_YEAR, -1);
            } else {
                mCurrentMonth.add(Calendar.MONTH, -1);
            }
            renderCalendar();
        });

        btnNextMonth.setOnClickListener(v -> {
            if (mIsWeekView) {
                mCurrentWeek.add(Calendar.WEEK_OF_YEAR, 1);
            } else {
                mCurrentMonth.add(Calendar.MONTH, 1);
            }
            renderCalendar();
        });

        btnToggleView.setOnClickListener(v -> {
            mIsWeekView = !mIsWeekView;
            renderCalendar();
        });
    }

    private void loadWeeklySummary() {
        WeeklySummary summary = mHistoryService.getWeeklySummary();

        txtWeeklyWater.setText(getString(R.string.weekly_water_achieved, summary.waterDaysAchieved(), summary.totalDays()));
        txtWeeklySpent.setText(getString(R.string.weekly_spent, summary.totalSpent()));
        txtWeeklyMeals.setText(getString(R.string.weekly_meals, summary.correctMeals(), summary.warningMeals(), summary.wrongMeals()));
    }

    private void renderCalendar() {
        calendarGrid.removeAllViews();

        if (mIsWeekView) {
            renderWeekView();
        } else {
            renderMonthView();
        }
    }

    private void renderWeekView() {
        // Update the title to show the week's date range
        Calendar weekEnd = (Calendar) mCurrentWeek.clone();
        weekEnd.add(Calendar.DAY_OF_MONTH, 6);
        String startDate = Common.getMonthYearFromTimestamp(mCurrentWeek.getTimeInMillis());
        String endDate = Common.getMonthYearFromTimestamp(weekEnd.getTimeInMillis());

        if (startDate.equals(endDate)) {
            txtMonthYear.setText(startDate);
        } else {
            txtMonthYear.setText(startDate + " - " + endDate);
        }

        btnToggleView.setText(getString(R.string.month_view));

        Calendar today = Calendar.getInstance();
        long todayStart = Common.getStartOfDayInMillis(today.getTimeInMillis());

        // Add 7-day cells for the current week
        for (int i = 0; i < 7; i++) {
            Calendar dayCal = (Calendar) mCurrentWeek.clone();
            dayCal.add(Calendar.DAY_OF_MONTH, i);
            long dayTimestamp = dayCal.getTimeInMillis();
            boolean isFutureDay = dayTimestamp > todayStart;

            TextView dayCell = new TextView(this);
            dayCell.setText(String.valueOf(dayCal.get(Calendar.DAY_OF_MONTH)));
            dayCell.setGravity(Gravity.CENTER);
            dayCell.setTextSize(16);
            dayCell.setTextColor(getColor(R.color.white));

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = dpToPx(45);
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));
            dayCell.setLayoutParams(params);

            if (isFutureDay) {
                dayCell.setBackgroundResource(R.drawable.calendar_day_background);
            } else {
                DayStatus dayStatus = mHistoryService.getDayStatus(dayTimestamp);
                switch (dayStatus) {
                    case GREEN:
                        dayCell.setBackgroundResource(R.drawable.calendar_day_green);
                        break;
                    case YELLOW:
                        dayCell.setBackgroundResource(R.drawable.calendar_day_yellow);
                        break;
                    case RED:
                        dayCell.setBackgroundResource(R.drawable.calendar_day_red);
                        break;
                    default:
                        if (dayStatus == DayStatus.NONE && mHistoryService.hasDataOnDay(dayTimestamp)) {
                            dayCell.setBackgroundResource(R.drawable.calendar_day_has_data);
                        } else {
                            dayCell.setBackgroundResource(R.drawable.calendar_day_background);
                        }
                        break;
                }
            }

            dayCell.setClickable(true);
            dayCell.setFocusable(true);

            final long timestamp = dayTimestamp;
            dayCell.setOnClickListener(v -> showDayDetails(timestamp, dayCell));

            calendarGrid.addView(dayCell);
        }
    }

    private void renderMonthView() {
        txtMonthYear.setText(Common.getMonthYearFromTimestamp(mCurrentMonth.getTimeInMillis()));
        btnToggleView.setText(getString(R.string.week_view));

        int year = mCurrentMonth.get(Calendar.YEAR);
        int month = mCurrentMonth.get(Calendar.MONTH);
        int firstDayOfWeek = getFirstDayOfWeek(year, month);
        int daysInMonth = mCurrentMonth.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Add empty cells for days before the first day of the month
        for (int i = 0; i < firstDayOfWeek; i++) {
            TextView emptyCell = new TextView(this);
            GridLayout.LayoutParams emptyParams = new GridLayout.LayoutParams();
            emptyParams.width = 0;
            emptyParams.height = dpToPx(45);
            emptyParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            emptyParams.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            emptyParams.setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));
            emptyCell.setLayoutParams(emptyParams);
            emptyCell.setVisibility(View.INVISIBLE);
            calendarGrid.addView(emptyCell);
        }

        // Add day cells
        Calendar today = Calendar.getInstance();
        long todayStart = Common.getStartOfDayInMillis(today.getTimeInMillis());

        for (int day = 1; day <= daysInMonth; day++) {
            Calendar dayCal = new GregorianCalendar(year, month, day);
            long dayTimestamp = dayCal.getTimeInMillis();
            boolean isFutureDay = dayTimestamp > todayStart;

            TextView dayCell = new TextView(this);
            dayCell.setText(String.valueOf(day));
            dayCell.setGravity(Gravity.CENTER);
            dayCell.setTextSize(16);
            dayCell.setTextColor(getColor(R.color.white));

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = dpToPx(45);
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));
            dayCell.setLayoutParams(params);

            if (isFutureDay) {
                // Future days: no coloring, just default background
                dayCell.setBackgroundResource(R.drawable.calendar_day_background);
            } else {
                DayStatus dayStatus = mHistoryService.getDayStatus(dayTimestamp);
                switch (dayStatus) {
                    case GREEN:
                        dayCell.setBackgroundResource(R.drawable.calendar_day_green);
                        break;
                    case YELLOW:
                        dayCell.setBackgroundResource(R.drawable.calendar_day_yellow);
                        break;
                    case RED:
                        dayCell.setBackgroundResource(R.drawable.calendar_day_red);
                        break;
                    default:
                        if (dayStatus == DayStatus.NONE && mHistoryService.hasDataOnDay(dayTimestamp)) {
                            dayCell.setBackgroundResource(R.drawable.calendar_day_has_data);
                        } else {
                            dayCell.setBackgroundResource(R.drawable.calendar_day_background);
                        }
                        break;
                }
            }

            dayCell.setClickable(true);
            dayCell.setFocusable(true);

            final long timestamp = dayTimestamp;
            dayCell.setOnClickListener(v -> showDayDetails(timestamp, dayCell));

            calendarGrid.addView(dayCell);
        }
    }

    private int getFirstDayOfWeek(int year, int month) {
        Calendar calendar = new GregorianCalendar(year, month, 1);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        // Convert to 0-based with Monday = 0
        // Calendar.DAY_OF_WEEK: Sunday=1, Monday=2, ..., Saturday=7
        // (dayOfWeek + 5) % 7: Monday=0, Tuesday=1, ..., Sunday=6
        return (dayOfWeek + 5) % 7;
    }

    private void selectToday() {
        Calendar today = Calendar.getInstance();
        mSelectedDayTimestamp = today.getTimeInMillis();
        long todayStart = Common.getStartOfDayInMillis(today.getTimeInMillis());

        // Find today's cell in the calendar grid and highlight it
        int todayDay = today.get(Calendar.DAY_OF_MONTH);

        if (mIsWeekView) {

            // Check if today is in the current displayed week
            long weekStart = Common.getStartOfDayInMillis(mCurrentWeek.getTimeInMillis());
            Calendar weekEndCal = (Calendar) mCurrentWeek.clone();
            weekEndCal.add(Calendar.DAY_OF_MONTH, 6);
            long weekEnd = Common.getStartOfDayInMillis(weekEndCal.getTimeInMillis());

            if (todayStart >= weekStart && todayStart <= weekEnd) {
                for (int i = 0; i < calendarGrid.getChildCount(); i++) {
                    View child = calendarGrid.getChildAt(i);
                    if (child instanceof TextView tv && child.getVisibility() == View.VISIBLE) {
                        String text = tv.getText().toString();
                        if (text.equals(String.valueOf(todayDay))) {
                            showDayDetails(mSelectedDayTimestamp, tv);
                            return;
                        }
                    }
                }
            }
        } else {
            int year = mCurrentMonth.get(Calendar.YEAR);
            int month = mCurrentMonth.get(Calendar.MONTH);

            if (today.get(Calendar.YEAR) == year && today.get(Calendar.MONTH) == month) {

                // Today is in the current displayed month
                for (int i = 0; i < calendarGrid.getChildCount(); i++) {
                    View child = calendarGrid.getChildAt(i);
                    if (child instanceof TextView tv && child.getVisibility() == View.VISIBLE) {
                        String text = tv.getText().toString();
                        if (text.equals(String.valueOf(todayDay))) {
                            showDayDetails(mSelectedDayTimestamp, tv);
                            return;
                        }
                    }
                }
            }
        }

        // Today is not in the current displayed view, just show details without highlighting
        showDayDetails(mSelectedDayTimestamp, null);
    }

    private void showDayDetails(long timestamp, TextView clickedCell) {
        mSelectedDayTimestamp = timestamp;

        // Reset all cell backgrounds to their status-based color
        long todayStart = Common.getStartOfDayInMillis(System.currentTimeMillis());
        for (int i = 0; i < calendarGrid.getChildCount(); i++) {
            View child = calendarGrid.getChildAt(i);
            if (child instanceof TextView && child.getVisibility() == View.VISIBLE) {
                child.setSelected(false);
                child.setForeground(null);
                ((TextView) child).setTextColor(getColor(R.color.white));
                long cellTimestamp = getTimestampFromCell((TextView) child);
                if (cellTimestamp > todayStart) {

                    // Future day: default background
                    child.setBackgroundResource(R.drawable.calendar_day_background);
                    continue;
                }
                DayStatus cellStatus = mHistoryService.getDayStatus(cellTimestamp);
                switch (cellStatus) {
                    case GREEN:
                        child.setBackgroundResource(R.drawable.calendar_day_green);
                        break;
                    case YELLOW:
                        child.setBackgroundResource(R.drawable.calendar_day_yellow);
                        break;
                    case RED:
                        child.setBackgroundResource(R.drawable.calendar_day_red);
                        break;
                    default:
                        if (mHistoryService.hasDataOnDay(cellTimestamp)) {
                            child.setBackgroundResource(R.drawable.calendar_day_has_data);
                        } else {
                            child.setBackgroundResource(R.drawable.calendar_day_background);
                        }
                        break;
                }
            }
        }

        // Highlight the clicked cell with an outline, keeping the status color background
        if (clickedCell != null) {
            clickedCell.setSelected(true);
            clickedCell.setForeground(AppCompatResources.getDrawable(this, R.drawable.calendar_day_selected_outline));
        }

        txtSelectedDate.setText(Common.getDateFromTimestamp(timestamp));

        DayDetails details = mHistoryService.getDayDetails(timestamp);

        boolean hasAnyData = !details.waterRecords().isEmpty()
                || !details.mealRecords().isEmpty()
                || !details.expenseRecords().isEmpty()
                || !details.workoutRecords().isEmpty()
                || !details.medicationRecords().isEmpty()
                || !details.supplementRecords().isEmpty();

        txtNoData.setVisibility(hasAnyData ? View.GONE : View.VISIBLE);

        // Water details - show total sum
        waterDetailsContainer.removeAllViews();
        if (!details.waterRecords().isEmpty()) {
            txtWaterSectionTitle.setVisibility(View.VISIBLE);
            int waterSum = 0;
            for (WaterRecord record : details.waterRecords()) {
                waterSum += record.amount();
            }
            TextView tv = createDetailTextView(getString(R.string.water_sum, waterSum));
            waterDetailsContainer.addView(tv);
        } else {
            txtWaterSectionTitle.setVisibility(View.GONE);
        }

        // Meal details
        mealDetailsContainer.removeAllViews();
        if (!details.mealRecords().isEmpty()) {
            txtMealSectionTitle.setVisibility(View.VISIBLE);
            for (MealRecord record : details.mealRecords()) {
                String time = Common.getHourFromTimestamp(String.valueOf(record.timestamp()));
                String status = getMealStatusLabel(record.status());
                String meal = record.meal();
                String obs = record.observation();

                TextView tv;
                if (obs != null && !obs.isEmpty()) {
                    tv = createDetailTextView(getString(R.string.meal_record_with_obs, time, meal, status, obs));
                } else {
                    tv = createDetailTextView(getString(R.string.meal_record, time, meal, status));
                }
                mealDetailsContainer.addView(tv);
            }
        } else {
            txtMealSectionTitle.setVisibility(View.GONE);
        }

        // Expense details - show total sum
        expenseDetailsContainer.removeAllViews();
        if (!details.expenseRecords().isEmpty()) {
            txtExpenseSectionTitle.setVisibility(View.VISIBLE);
            double expenseSum = 0;
            for (ExpenseRecord record : details.expenseRecords()) {
                expenseSum += record.amount();
            }
            TextView tv = createDetailTextView(getString(R.string.expense_sum, expenseSum));
            expenseDetailsContainer.addView(tv);
        } else {
            txtExpenseSectionTitle.setVisibility(View.GONE);
        }

        // Workout details
        workoutDetailsContainer.removeAllViews();
        if (!details.workoutRecords().isEmpty()) {
            txtWorkoutSectionTitle.setVisibility(View.VISIBLE);
            for (TrackerRecord record : details.workoutRecords()) {
                String status = record.completed() ? getString(R.string.completed) : getString(R.string.not_completed);
                TextView tv = createDetailTextView(status);
                workoutDetailsContainer.addView(tv);
            }
        } else {
            txtWorkoutSectionTitle.setVisibility(View.GONE);
        }

        // Medication details
        medicationDetailsContainer.removeAllViews();
        if (!details.medicationRecords().isEmpty()) {
            txtMedicationSectionTitle.setVisibility(View.VISIBLE);
            for (TrackerRecord record : details.medicationRecords()) {
                String status = record.completed() ? getString(R.string.completed) : getString(R.string.not_completed);
                TextView tv = createDetailTextView(status);
                medicationDetailsContainer.addView(tv);
            }
        } else {
            txtMedicationSectionTitle.setVisibility(View.GONE);
        }

        // Supplement details
        supplementDetailsContainer.removeAllViews();
        if (!details.supplementRecords().isEmpty()) {
            txtSupplementSectionTitle.setVisibility(View.VISIBLE);
            for (TrackerRecord record : details.supplementRecords()) {
                String status = record.completed() ? getString(R.string.completed) : getString(R.string.not_completed);
                TextView tv = createDetailTextView(status);
                supplementDetailsContainer.addView(tv);
            }
        } else {
            txtSupplementSectionTitle.setVisibility(View.GONE);
        }
    }

    private long getTimestampFromCell(TextView cell) {
        String text = cell.getText().toString();
        int day = Integer.parseInt(text);

        if (mIsWeekView) {

            // In week view, the cell's day number corresponds to the day of the week
            // We need to find which day in the current week matches this day number
            Calendar dayCal = (Calendar) mCurrentWeek.clone();
            for (int i = 0; i < 7; i++) {
                if (dayCal.get(Calendar.DAY_OF_MONTH) == day) {
                    return dayCal.getTimeInMillis();
                }
                dayCal.add(Calendar.DAY_OF_MONTH, 1);
            }

            // Fallback: use the first day of the week
            return mCurrentWeek.getTimeInMillis();
        } else {
            int year = mCurrentMonth.get(Calendar.YEAR);
            int month = mCurrentMonth.get(Calendar.MONTH);
            Calendar cal = new GregorianCalendar(year, month, day);
            return cal.getTimeInMillis();
        }
    }

    private TextView createDetailTextView(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(getColor(R.color.white));
        tv.setTextSize(14);
        tv.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
        return tv;
    }

    private String getMealStatusLabel(String status) {
        if (Constants.CORRECT_MEAL.equals(status)) {
            return getString(R.string.correct_meal);
        } else if (Constants.WARNING_MEAL.equals(status)) {
            return getString(R.string.warning_meal);
        } else if (Constants.WRONG_MEAL.equals(status)) {
            return getString(R.string.wrong_meal);
        }
        return status;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        mHistoryService.closeDb();
        super.onDestroy();
    }
}