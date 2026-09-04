package com.android.nls.routine.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsControllerCompat;
import com.android.nls.routine.R;
import com.android.nls.routine.model.DayDetails;
import com.android.nls.routine.model.DayStatus;
import com.android.nls.routine.model.DayStatusInfo;
import com.android.nls.routine.model.WeeklySummary;
import com.android.nls.routine.repository.ConfigRepository;
import com.android.nls.routine.service.HistoryService;
import com.android.nls.routine.service.score.DayScore;
import com.android.nls.routine.utils.BottomNavHelper;
import com.android.nls.routine.utils.Common;
import com.android.nls.routine.service.score.DayDetailsRenderer;
import com.android.nls.routine.service.score.DayScoreData;
import com.android.nls.routine.service.score.HistoryCalendarRenderer;
import com.google.android.material.button.MaterialButton;

public class HistoryActivity extends AppCompatActivity {
    private HistoryService mHistoryService;
    private ConfigRepository mConfigRepository;

    // Weekly summary views
    private TextView txtSummaryTitle;
    private TextView txtWeeklyWater;
    private TextView txtWeeklySpent;
    private TextView txtWeeklyMeals;

    // Calendar views
    private ImageButton btnPrevMonth;
    private ImageButton btnNextMonth;
    private MaterialButton btnToggleView;

    // Day details views
    private TextView txtSelectedDate;
    private TextView txtNoData;
    private MaterialButton txtPrincipalDayScore;
    private TextView txtScoreBreakdown;
    private View viewScoreStatus;

    private HistoryCalendarRenderer mCalendarRenderer;
    private DayDetailsRenderer mDayDetailsRenderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history);
        WindowInsetsControllerCompat windowInsetsController = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

        mHistoryService = new HistoryService(this);
        mConfigRepository = new ConfigRepository(this);

        startUIComponents();
        setupButtonListeners();
        loadSummary();
        mCalendarRenderer.renderCalendar();
        mCalendarRenderer.selectToday();
        BottomNavHelper.setup(this, R.id.nav_history);
    }

    private void startUIComponents() {
        txtSummaryTitle = findViewById(R.id.txtSummaryTitle);
        txtWeeklyWater = findViewById(R.id.txtWeeklyWater);
        txtWeeklySpent = findViewById(R.id.txtWeeklySpent);
        txtWeeklyMeals = findViewById(R.id.txtWeeklyMeals);

        GridLayout calendarGrid = findViewById(R.id.calendarGrid);
        TextView txtMonthYear = findViewById(R.id.txtMonthYear);
        btnPrevMonth = findViewById(R.id.btnPrevMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);
        btnToggleView = findViewById(R.id.btnToggleView);

        txtSelectedDate = findViewById(R.id.txtSelectedDate);
        txtNoData = findViewById(R.id.txtNoData);
        GridLayout dayDetailsGrid = findViewById(R.id.dayDetailsGrid);
        txtPrincipalDayScore = findViewById(R.id.txtPrincipalDayScore);
        txtScoreBreakdown = findViewById(R.id.txtScoreBreakdown);
        viewScoreStatus = findViewById(R.id.viewScoreStatus);

        mCalendarRenderer = new HistoryCalendarRenderer(this, calendarGrid, txtMonthYear, btnToggleView,
                mHistoryService, this::showDayDetails);
        mDayDetailsRenderer = new DayDetailsRenderer(this, dayDetailsGrid, mHistoryService, mConfigRepository);
    }

    private void setupButtonListeners() {
        btnPrevMonth.setOnClickListener(v -> {
            mCalendarRenderer.navigatePrevious();
            loadSummary();
        });

        btnNextMonth.setOnClickListener(v -> {
            mCalendarRenderer.navigateNext();
            loadSummary();
        });

        btnToggleView.setOnClickListener(v -> {
            mCalendarRenderer.toggleView();
            loadSummary();
        });
    }

    private void loadSummary() {
        WeeklySummary summary;
        if (mCalendarRenderer.isWeekView()) {
            txtSummaryTitle.setText(getString(R.string.weekly_summary));
            summary = mHistoryService.getWeeklySummary();
        } else {
            txtSummaryTitle.setText(getString(R.string.monthly_summary));
            summary = mHistoryService.getMonthlySummary(mCalendarRenderer.getCurrentMonth());
        }

        txtWeeklyWater.setText(getString(R.string.weekly_water_achieved, summary.waterDaysAchieved(), summary.totalDays()));
        txtWeeklySpent.setText(getString(R.string.weekly_spent, summary.totalSpent()));
        txtWeeklyMeals.setText(getString(R.string.weekly_meals, summary.correctMeals(), summary.warningMeals(), summary.wrongMeals()));
    }

    private void showDayDetails(long timestamp, TextView clickedCell) {
        mCalendarRenderer.highlightDay(timestamp, clickedCell);

        txtSelectedDate.setText(Common.getDateFromTimestamp(timestamp));

        // Show score breakdown for the selected day with status color indicator
        DayStatusInfo dayInfo = mCalendarRenderer.getDayStatus(timestamp);
        if (dayInfo != null && dayInfo.scoreBreakdown() != null && !dayInfo.scoreBreakdown().isEmpty()) {
            txtScoreBreakdown.setText(dayInfo.scoreBreakdown());
            txtScoreBreakdown.setTextColor(getColor(R.color.white));

            switch (dayInfo.status()) {
                case GREEN:
                    viewScoreStatus.setBackgroundResource(R.drawable.calendar_day_green);
                    viewScoreStatus.setVisibility(View.VISIBLE);
                    break;
                case YELLOW:
                    viewScoreStatus.setBackgroundResource(R.drawable.calendar_day_yellow);
                    viewScoreStatus.setVisibility(View.VISIBLE);
                    break;
                case RED:
                    viewScoreStatus.setBackgroundResource(R.drawable.calendar_day_red);
                    viewScoreStatus.setVisibility(View.VISIBLE);
                    break;
                default:
                    viewScoreStatus.setVisibility(View.GONE);
                    break;
            }
        } else {
            txtScoreBreakdown.setText("");
            viewScoreStatus.setVisibility(View.GONE);
        }

        DayDetails details = mHistoryService.getDayDetails(timestamp);

        boolean hasAnyData = !details.waterRecords().isEmpty()
                || !details.mealRecords().isEmpty()
                || !details.expenseRecords().isEmpty()
                || !details.workoutRecords().isEmpty()
                || !details.medicationRecords().isEmpty()
                || !details.supplementRecords().isEmpty();

        txtNoData.setVisibility(hasAnyData ? View.GONE : View.VISIBLE);

        // Render the day details grid with icon cells for each enabled tracker
        mDayDetailsRenderer.render(details);

        // Update the principal day score button with the weighted percentage and status color
        updatePrincipalDayScore(dayInfo, details);
    }

    private void updatePrincipalDayScore(DayStatusInfo dayInfo, DayDetails details) {
        if (dayInfo == null || dayInfo.status() == DayStatus.NONE) {
            txtPrincipalDayScore.setText("");
            txtPrincipalDayScore.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.calendar_day_background_default)));
            return;
        }

        // Compute the weighted percentage using the same logic as the calendar
        double dailyGoal = mConfigRepository.getDailyWaterGoal();
        DayScoreData scoreData = new DayScoreData(details, mHistoryService);

        double percentage = DayScore.computePercentage(scoreData.getWaterSum(), dailyGoal,
                scoreData.getMealCountsByType(), scoreData.getTrackerCompletions(), scoreData.getEnabledTrackers());
        if (percentage < 0) {
            txtPrincipalDayScore.setText("");
            txtPrincipalDayScore.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.calendar_day_background_default)));
            return;
        }

        int rounded = (int) Math.round(percentage);
        String scoreText;
        int scoreColor = switch (dayInfo.status()) {
            case GREEN -> {
                scoreText = getString(R.string.score_percentage_good, rounded);
                yield R.color.calendar_green;
            }
            case YELLOW -> {
                scoreText = getString(R.string.score_percentage_warning, rounded);
                yield R.color.calendar_yellow;
            }
            case RED -> {
                scoreText = getString(R.string.score_percentage_bad, rounded);
                yield R.color.calendar_red;
            }
            default -> {
                scoreText = "";
                yield R.color.calendar_day_background_default;
            }
        };

        txtPrincipalDayScore.setText(scoreText);
        txtPrincipalDayScore.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(scoreColor)));
    }

    @Override
    protected void onDestroy() {
        mHistoryService.closeDb();
        mConfigRepository.closeDb();
        super.onDestroy();
    }
}