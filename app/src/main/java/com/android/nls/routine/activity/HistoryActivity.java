package com.android.nls.routine.activity;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.GridLayout;
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
import com.android.nls.routine.service.calendar.DayScore;
import com.android.nls.routine.service.calendar.DayDetailsRenderer;
import com.android.nls.routine.service.calendar.DayScoreData;
import com.android.nls.routine.service.calendar.HistoryCalendarRenderer;
import com.android.nls.routine.service.calendar.HistoryContext;
import com.android.nls.routine.utils.BottomNavHelper;
import com.android.nls.routine.utils.Common;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class HistoryActivity extends AppCompatActivity {
    private HistoryContext mHistoryContext;

    private MaterialCardView cardWeeklySummary;
    private MaterialCardView cardMonthlySummary;

    private TextView txtSummaryTitle;
    private TextView txtWeeklyWater;
    private TextView txtWeeklySpent;
    private TextView txtWeeklyMeals;

    private TextView txtMonthlyTitle;
    private TextView txtMonthlyWater;
    private TextView txtMonthlySpent;
    private TextView txtMonthlyMeals;

    private MaterialButton btnPrevMonth;
    private MaterialButton btnNextMonth;
    private MaterialButton btnToggleView;

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

        mHistoryContext = new HistoryContext(new HistoryService(this), new ConfigRepository(this));

        startUIComponents();
        setupButtonListeners();
        loadSummary();
        mCalendarRenderer.renderCalendar();
        mCalendarRenderer.selectToday();
        BottomNavHelper.setup(this, R.id.nav_history);
    }

    private void startUIComponents() {
        cardWeeklySummary = findViewById(R.id.cardWeeklySummary);
        cardMonthlySummary = findViewById(R.id.cardMonthlySummary);

        txtSummaryTitle = findViewById(R.id.txtSummaryTitle);
        txtWeeklyWater = findViewById(R.id.txtWeeklyWater);
        txtWeeklySpent = findViewById(R.id.txtWeeklySpent);
        txtWeeklyMeals = findViewById(R.id.txtWeeklyMeals);

        txtMonthlyTitle = findViewById(R.id.txtMonthlyTitle);
        txtMonthlyWater = findViewById(R.id.txtMonthlyWater);
        txtMonthlySpent = findViewById(R.id.txtMonthlySpent);
        txtMonthlyMeals = findViewById(R.id.txtMonthlyMeals);

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
                mHistoryContext.getHistoryService(), this::showDayDetails);
        mDayDetailsRenderer = new DayDetailsRenderer(this, dayDetailsGrid, mHistoryContext);
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
            cardWeeklySummary.setVisibility(View.VISIBLE);
            cardMonthlySummary.setVisibility(View.GONE);

            txtSummaryTitle.setText(getString(R.string.weekly_summary));
            summary = mHistoryContext.getHistoryService().getWeeklySummary();

            txtWeeklyWater.setText(getString(R.string.weekly_water_achieved, summary.waterDaysAchieved(), summary.totalDays()));
            txtWeeklySpent.setText(getString(R.string.weekly_spent, summary.totalSpent()));
            txtWeeklyMeals.setText(getString(R.string.weekly_meals, summary.correctMeals(), summary.warningMeals(), summary.wrongMeals()));
        } else {
            cardWeeklySummary.setVisibility(View.GONE);
            cardMonthlySummary.setVisibility(View.VISIBLE);

            txtMonthlyTitle.setText(getString(R.string.monthly_summary));
            summary = mHistoryContext.getHistoryService().getMonthlySummary(mCalendarRenderer.getCurrentMonth());

            txtMonthlyWater.setText(getString(R.string.weekly_water_achieved, summary.waterDaysAchieved(), summary.totalDays()));
            txtMonthlySpent.setText(getString(R.string.weekly_spent, summary.totalSpent()));
            txtMonthlyMeals.setText(getString(R.string.weekly_meals, summary.correctMeals(), summary.warningMeals(), summary.wrongMeals()));
        }
    }

    private void showDayDetails(long timestamp, TextView clickedCell) {
        mCalendarRenderer.highlightDay(timestamp, clickedCell);

        txtSelectedDate.setText(Common.getDateFromTimestamp(timestamp));

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

        DayDetails details = mHistoryContext.getHistoryService().getDayDetails(timestamp);

        boolean hasAnyData = !details.waterRecords().isEmpty()
                || !details.mealRecords().isEmpty()
                || !details.expenseRecords().isEmpty()
                || !details.workoutRecords().isEmpty()
                || !details.medicationRecords().isEmpty()
                || !details.supplementRecords().isEmpty();

        txtNoData.setVisibility(hasAnyData ? View.GONE : View.VISIBLE);

        DayScoreData scoreData = new DayScoreData(details);
        mDayDetailsRenderer.render(details, scoreData);
        updatePrincipalDayScore(dayInfo, scoreData);
    }

    private void updatePrincipalDayScore(DayStatusInfo dayInfo, DayScoreData scoreData) {
        if (dayInfo == null || dayInfo.status() == DayStatus.NONE) {
            txtPrincipalDayScore.setText("");
            txtPrincipalDayScore.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.calendar_day_background_default)));
            return;
        }

        double percentage = DayScore.computePercentage(scoreData.getWaterSum(), mHistoryContext.getDailyWaterGoal(),
                scoreData.getMealStatusesByType(), scoreData.getTrackerCompletions(), mHistoryContext.getScoreTrackerTypes());
        if (percentage < 0) {
            txtPrincipalDayScore.setText("");
            txtPrincipalDayScore.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.calendar_day_background_default)));
            return;
        }

        int rounded = (int) Math.round(percentage);
        String scoreText;
        int scoreColor = switch (dayInfo.status()) {
            case GREEN -> {
                scoreText = getString(R.string.score_percentage_good, rounded);
                yield R.color.green_dark;
            }
            case YELLOW -> {
                scoreText = getString(R.string.score_percentage_warning, rounded);
                yield R.color.yellow_dark;
            }
            case RED -> {
                scoreText = getString(R.string.score_percentage_bad, rounded);
                yield R.color.red_dark;
            }
            default -> {
                scoreText = "";
                yield R.color.calendar_day_background_default;
            }
        };

        txtPrincipalDayScore.setText(scoreText);
        txtPrincipalDayScore.setBackgroundTintList(ColorStateList.valueOf(getColor(scoreColor)));
        txtPrincipalDayScore.setTextSize(14);
    }

    @Override
    protected void onDestroy() {
        mHistoryContext.closeDb();
        super.onDestroy();
    }
}