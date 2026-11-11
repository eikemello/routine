package com.android.nls.routine.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.util.Log;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsControllerCompat;
import com.android.nls.routine.R;
import com.android.nls.routine.cardhistory.DynamicCardHistoryService;
import com.android.nls.routine.model.CardSpending;
import com.android.nls.routine.model.DayScoreSummary;
import com.android.nls.routine.model.DayStatus;
import com.android.nls.routine.model.ExpenseCardSummary;
import com.android.nls.routine.model.ExpenseRecord;
import com.android.nls.routine.model.Tracker;
import com.android.nls.routine.model.TrackerRecord;
import com.android.nls.routine.model.TrackerType;
import com.android.nls.routine.model.WaterRecord;
import com.android.nls.routine.repository.MealRepository;
import com.android.nls.routine.repository.TrackerRepository;
import com.android.nls.routine.service.CardStatusResolver;
import com.android.nls.routine.service.HomeCardExpenseService;
import com.android.nls.routine.service.HomeCardMealService;
import com.android.nls.routine.service.HomeCardWaterService;
import com.android.nls.routine.service.HistoryService;
import com.android.nls.routine.service.calendar.CalendarCellStyler;
import com.android.nls.routine.service.calendar.WeekPreviewRenderer;
import com.android.nls.routine.utils.BottomNavHelper;
import com.android.nls.routine.utils.Common;
import com.android.nls.routine.utils.Constants;
import com.android.nls.routine.widget.WidgetCombinedProvider;
import com.android.nls.routine.widget.WidgetTrackerProvider;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import java.util.List;

public class HomeActivity extends AppCompatActivity {
    private static final String TAG = Common.generateTag(HomeActivity.class);
    private HomeCardWaterService mHomeCardWaterService;
    private HomeCardExpenseService mHomeCardExpenseService;
    private HomeCardMealService mHomeCardMealService;
    private DynamicCardHistoryService mDynamicCardHistoryService;
    private TrackerRepository mTrackerRepository;
    private MealRepository mMealRepository;
    private HistoryService mHistoryService;
    private WeekPreviewRenderer mWeekPreviewRenderer;

    // Header views
    private TextView txtCurrentGreeting;
    private TextView txtCurrentDate;

    // Dynamic tracker cards container
    private LinearLayout mTrackerCardsContainer;
    // Ring with today's score in the header
    private CircularProgressIndicator mProgressDayScore;
    private TextView mTxtDayScore;

    // Meal card currently on screen, so its slot bars can be repainted when a
    // meal is saved, edited or removed (null while meals are disabled)
    private View mMealCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        // Force status bar icons to be white (light appearance) since the header background
        // is always dark regardless of system dark/light mode
        WindowInsetsControllerCompat windowInsetsController = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

        mHomeCardWaterService = new HomeCardWaterService(this);
        mHomeCardExpenseService = new HomeCardExpenseService(this);
        mHomeCardMealService = new HomeCardMealService(this);
        mDynamicCardHistoryService = new DynamicCardHistoryService(this);
        mTrackerRepository = new TrackerRepository(this);
        mMealRepository = new MealRepository(this);
        mHistoryService = new HistoryService(this);

        startUIComponents();
        BottomNavHelper.setup(this, R.id.nav_home);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initFields();
        renderTrackerProgress();
        renderTrackerCards();

        // Covers what happened while the app was away: water added on the widget
        // itself and the day rolling over at midnight
        WidgetCombinedProvider.refresh(this);

        // The routines widget mirrors the same day from the launcher: it is
        // repainted on the same occasions, so its tiles never show a routine
        // marked here as open or the other way around
        WidgetTrackerProvider.refresh(this);

        // Warning/wrong meals logged on the widget carry no description - there
        // is no dialog there to ask for one -, so the card offers to describe
        // them the moment the screen comes back to the front
        mHomeCardMealService.showMissingObservationPrompt(this::onMealChanged);
    }

    private void startUIComponents() {
        txtCurrentDate = findViewById(R.id.txtCurrentDate);
        txtCurrentGreeting = findViewById(R.id.txtCurrentGreeting);
        mTrackerCardsContainer = findViewById(R.id.trackerCardsContainer);
        mWeekPreviewRenderer = new WeekPreviewRenderer(this, findViewById(R.id.weekPreviewStrip), mHistoryService);

        mProgressDayScore = findViewById(R.id.progressDayScore);
        mTxtDayScore = findViewById(R.id.txtDayScore);
    }

    private void initFields() {
        txtCurrentDate.setText(Common.getWeekDay());
        txtCurrentGreeting.setText(Common.getCurrentGreeting());
    }

    private void renderTrackerCards() {
        mTrackerCardsContainer.removeAllViews();
        mMealCard = null;
        List<Tracker> enabledTrackers = mTrackerRepository.getEnabledTrackers();

        for (Tracker tracker : enabledTrackers) {
            View card = createTrackerCard(tracker);
            if (card != null) {
                mTrackerCardsContainer.addView(card);
            }
        }
    }

    private View createTrackerCard(Tracker tracker) {
        LayoutInflater inflater = LayoutInflater.from(this);

        return switch (tracker.type()) {
            case WATER -> createWaterCard(inflater);
            case EXPENSES -> createExpenseCard(inflater);
            case MEALS -> createMealCard(inflater);
            case WORKOUT -> createWorkoutCard(inflater, tracker);
            case MEDICATION -> createMedicationCard(inflater, tracker);
            case SUPPLEMENT -> createSupplementCard(inflater, tracker);
        };
    }

    private View createWaterCard(LayoutInflater inflater) {
        View card = inflater.inflate(R.layout.card_water, mTrackerCardsContainer, false);

        MaterialButton btnAddWater1 = card.findViewById(R.id.btnAddWater1);
        MaterialButton btnAddWater2 = card.findViewById(R.id.btnAddWater2);
        MaterialButton btnAddWater3 = card.findViewById(R.id.btnAddWater3);

        btnAddWater1.setText(this.getString(R.string.water_default_value_50, mHomeCardWaterService.getDefaultValueBtn1()));
        btnAddWater2.setText(this.getString(R.string.water_default_value_100, mHomeCardWaterService.getDefaultValueBtn2()));
        btnAddWater3.setText(this.getString(R.string.water_default_value_250, mHomeCardWaterService.getDefaultValueBtn3()));

        renderWaterCard(card);

        btnAddWater1.setOnClickListener(v -> addWater(card, btnAddWater1));
        btnAddWater2.setOnClickListener(v -> addWater(card, btnAddWater2));
        btnAddWater3.setOnClickListener(v -> addWater(card, btnAddWater3));

        card.findViewById(R.id.btnWaterHistory).setOnClickListener(v ->
                mHomeCardWaterService.showDailyHistoryDialog(() -> {
                    renderWaterCard(card);
                    renderTrackerProgress();
                    WidgetCombinedProvider.refresh(this);
                }));

        return card;
    }

    private void renderWaterCard(View card) {
        TextView txtDailyWater = card.findViewById(R.id.txtDailyWater);
        TextView txtDailyWaterDrank = card.findViewById(R.id.txtDailyWaterDrank);
        TextView txtLastWaterAddedTime = card.findViewById(R.id.txtLastWaterAddedTime);
        LinearProgressIndicator progressWater = card.findViewById(R.id.progressWater);

        double dailyWaterGoal = mHomeCardWaterService.getDailyWaterGoal();
        int dailyWaterSum = mHomeCardWaterService.getDailyWaterSum();

        txtDailyWater.setText(this.getString(R.string.daily_water_ml, dailyWaterGoal));

        WaterRecord lastWaterRecord = mHomeCardWaterService.getLastWaterAddedRecord();
        if (lastWaterRecord != null) {
            String lastWaterHour = Common.getHourFromTimestamp(String.valueOf(lastWaterRecord.timestamp()));
            txtLastWaterAddedTime.setText(this.getString(R.string.last_added_at, lastWaterHour, String.valueOf(lastWaterRecord.amount())));
        } else {
            txtLastWaterAddedTime.setText(this.getString(R.string.no_water_records_today));
        }

        mHomeCardWaterService.updateWaterProgress(progressWater, dailyWaterSum, dailyWaterGoal);
        mHomeCardWaterService.setDailyWaterDrank(txtDailyWaterDrank, dailyWaterSum, dailyWaterGoal);

        DayStatus status = CardStatusResolver.water(dailyWaterSum, dailyWaterGoal);
        CalendarCellStyler.tintIcon(card.findViewById(R.id.imgViewWater), status);
        CalendarCellStyler.tintProgress(progressWater, status);
    }

    private void addWater(View card, MaterialButton button) {
        mHomeCardWaterService.addWater(button.getText().toString());
        renderWaterCard(card);
        renderTrackerProgress();
        WidgetCombinedProvider.refresh(this);
    }

    private View createMealCard(LayoutInflater inflater) {
        View card = inflater.inflate(R.layout.card_meal, mTrackerCardsContainer, false);

        MaterialButton btnCorrectMeal = card.findViewById(R.id.btnCorrectMeal);
        MaterialButton btnWarningMeal = card.findViewById(R.id.btnWarningMeal);
        MaterialButton btnWrongMeal = card.findViewById(R.id.btnWrongMeal);

        btnCorrectMeal.setOnClickListener(v ->
                mHomeCardMealService.showAlertDialog(Constants.CORRECT_MEAL, this::onMealChanged));

        btnWarningMeal.setOnClickListener(v ->
                mHomeCardMealService.showAlertDialog(Constants.WARNING_MEAL, this::onMealChanged));

        btnWrongMeal.setOnClickListener(v ->
                mHomeCardMealService.showAlertDialog(Constants.WRONG_MEAL, this::onMealChanged));

        card.findViewById(R.id.btnMealHistory).setOnClickListener(v ->
                mHomeCardMealService.showDailyHistoryDialog(this::onMealChanged));

        mMealCard = card;
        renderMealSlots(card);
        return card;
    }

    /**
     * Paints the four slot bars of the meal card (Breakfast, Lunch, Tea and
     * Dinner) with the status of the meal logged today in each slot - empty
     * bars stay neutral -, tints the card icon with the status of the meals so
     * far and writes the "x of 4" summary under the title.
     */
    private void renderMealSlots(View card) {
        int[] slotIds = {R.id.viewMealSlot1, R.id.viewMealSlot2, R.id.viewMealSlot3, R.id.viewMealSlot4};
        String[] statuses = mHomeCardMealService.getLoggedMealStatusesToday();

        int logged = 0;
        for (int i = 0; i < slotIds.length; i++) {
            String status = i < statuses.length ? statuses[i] : null;
            if (status != null) {
                logged++;
            }
            card.findViewById(slotIds[i]).setBackgroundResource(mealSlotBackground(status));
        }

        CalendarCellStyler.tintIcon(card.findViewById(R.id.imgViewMeal), CardStatusResolver.meals(statuses));

        TextView txtSummary = card.findViewById(R.id.txtMealSlotsSummary);
        txtSummary.setText(logged == 0
                ? getString(R.string.how_was_your_meal)
                : getString(R.string.meal_slots_summary, logged, slotIds.length));
    }

    private int mealSlotBackground(String status) {
        if (Constants.CORRECT_MEAL.equals(status)) {
            return R.drawable.progress_segment_done;
        }
        if (Constants.WARNING_MEAL.equals(status)) {
            return R.drawable.progress_segment_warning;
        }
        if (Constants.WRONG_MEAL.equals(status)) {
            return R.drawable.progress_segment_wrong;
        }
        return R.drawable.progress_segment_background;
    }

    /**
     * Repaints the progress strip and the home screen widget after a meal was
     * saved, edited or removed, so the widget's segments and "x/4" count show
     * the same meals as the card behind it.
     */
    private void onMealChanged() {
        renderTrackerProgress();
        if (mMealCard != null) {
            renderMealSlots(mMealCard);
        }
        WidgetCombinedProvider.refresh(this);
    }

    private View createExpenseCard(LayoutInflater inflater) {
        View card = inflater.inflate(R.layout.card_expense, mTrackerCardsContainer, false);

        renderExpenseCard(card);

        card.findViewById(R.id.btnExpenseHistory).setOnClickListener(v ->
                mHomeCardExpenseService.showDailyHistoryDialog(() -> {
                    renderExpenseCard(card);
                    renderTrackerProgress();
                }));

        return card;
    }

    private void renderExpenseCard(View card) {
        double monthlyLimit = mHomeCardExpenseService.getMonthlyLimitValue();
        ExpenseCardSummary expenseSummary = mHomeCardExpenseService.getExpenseCardSummary();
        double totalSpent = expenseSummary.totalSpent();
        ExpenseRecord expenseRecord = mHomeCardExpenseService.getLastExpenseRecord();

        TextView txtTotalSpent = card.findViewById(R.id.txtTotalSpent);
        TextView txtTotalValue = card.findViewById(R.id.txtTotalValue);
        TextView txtLastExpenseRecord = card.findViewById(R.id.txtLastExpenseRecord);
        LinearProgressIndicator progressExpense = card.findViewById(R.id.progressExpense);
        LinearLayout cardProgressContainer = card.findViewById(R.id.cardProgressContainer);

        if (txtTotalSpent == null || txtTotalValue == null || txtLastExpenseRecord == null
                || progressExpense == null || cardProgressContainer == null) {
            Log.e(TAG, "renderExpenseCard: failed to find views on the expense card");
            return;
        }

        txtTotalValue.setText(this.getString(R.string.total_expense_value_init, monthlyLimit));
        txtTotalSpent.setText(this.getString(R.string.total_expense_value_init, totalSpent));

        if (totalSpent > monthlyLimit) {
            txtTotalSpent.setTextColor(this.getColor(R.color.status_bad));
        } else {
            // Reset it, otherwise the total would stay red after a new cycle
            // starts and the spending is back below the limit
            txtTotalSpent.setTextColor(this.getColor(R.color.text_primary));
        }

        if (expenseRecord != null) {
            txtLastExpenseRecord.setText(this.getString(R.string.last_expense, expenseRecord.amount(), expenseRecord.bank()));
        } else {
            txtLastExpenseRecord.setText(this.getString(R.string.no_expenses_yet));
        }

        CalendarCellStyler.tintIcon(card.findViewById(R.id.imgCreditCard),
                CardStatusResolver.spending(totalSpent, monthlyLimit));

        renderCardProgress(LayoutInflater.from(this), cardProgressContainer, progressExpense, expenseSummary, monthlyLimit);
    }

    /**
     * Shows one progress bar per configured card, each one with the amount spent
     * in that card's own statement cycle. When no card is configured, the single
     * progress bar of the whole cycle is shown instead.
     */
    private void renderCardProgress(LayoutInflater inflater,
                                    LinearLayout cardProgressContainer,
                                    LinearProgressIndicator progressExpense,
                                    ExpenseCardSummary expenseSummary,
                                    double monthlyLimit) {
        cardProgressContainer.removeAllViews();

        List<CardSpending> cardSpending = expenseSummary.cardSpendings();
        if (cardSpending.isEmpty()) {
            progressExpense.setVisibility(View.VISIBLE);
            mHomeCardWaterService.updateExpenseProgress(progressExpense, expenseSummary.totalSpent(), monthlyLimit);
            CalendarCellStyler.tintProgress(progressExpense,
                    CardStatusResolver.spending(expenseSummary.totalSpent(), monthlyLimit));
            return;
        }

        progressExpense.setVisibility(View.GONE);

        for (CardSpending spending : cardSpending) {
            View row = inflater.inflate(R.layout.item_card_progress, cardProgressContainer, false);
            TextView txtCardProgressLabel = row.findViewById(R.id.txtCardProgressLabel);
            TextView txtCardProgressValue = row.findViewById(R.id.txtCardProgressValue);
            LinearProgressIndicator progressCard = row.findViewById(R.id.progressCard);

            txtCardProgressLabel.setText(this.getString(R.string.card_progress_label,
                    spending.bankName(), spending.lastFour()));
            txtCardProgressLabel.setTextSize(14);

            txtCardProgressValue.setText(this.getString(R.string.total_expense_value_init, spending.spent()));
            txtCardProgressValue.setTextSize(14);

            progressCard.setProgress(spending.progress());
            CalendarCellStyler.tintProgress(progressCard,
                    CardStatusResolver.spending(spending.spent(), spending.monthlyLimit()));

            cardProgressContainer.addView(row);
        }
    }

    private View createWorkoutCard(LayoutInflater inflater, Tracker tracker) {
        View card = inflater.inflate(R.layout.card_workout, mTrackerCardsContainer, false);

        TextView txtWorkoutStatus = card.findViewById(R.id.txtWorkoutStatus);
        MaterialButton btnCompleteWorkout = card.findViewById(R.id.btnCompleteWorkout);
        ImageView imgWorkout = card.findViewById(R.id.imgViewWorkout);

        TrackerRecord record = mTrackerRepository.getTrackerRecordForDay(TrackerType.WORKOUT, System.currentTimeMillis());
        updateStatusCard(imgWorkout, txtWorkoutStatus, btnCompleteWorkout, record);

        btnCompleteWorkout.setOnClickListener(v -> {
            mTrackerRepository.saveTrackerRecord(TrackerType.WORKOUT, true, null);
            TrackerRecord updatedRecord = mTrackerRepository.getTrackerRecordForDay(TrackerType.WORKOUT, System.currentTimeMillis());
            updateStatusCard(imgWorkout, txtWorkoutStatus, btnCompleteWorkout, updatedRecord);
            renderTrackerProgress();
            WidgetTrackerProvider.refresh(this);
        });

        card.findViewById(R.id.btnWorkoutHistory).setOnClickListener(v ->
                mDynamicCardHistoryService.showDailyHistoryDialog(tracker, () -> {
                    refreshSimpleCard(TrackerType.WORKOUT, imgWorkout, txtWorkoutStatus, btnCompleteWorkout);
                    renderTrackerProgress();
                    WidgetTrackerProvider.refresh(this);
                }));

        return card;
    }

    private View createMedicationCard(LayoutInflater inflater, Tracker tracker) {
        View card = inflater.inflate(R.layout.card_medication, mTrackerCardsContainer, false);

        TextView txtMedicationName = card.findViewById(R.id.txtMedicationName);
        TextView txtMedicationStatus = card.findViewById(R.id.txtMedicationStatus);
        MaterialButton btnMarkMedicationTaken = card.findViewById(R.id.btnMarkMedicationTaken);
        ImageView imgMedication = card.findViewById(R.id.imgViewMedication);

        String name = tracker.name();
        if (name != null && !name.equalsIgnoreCase(TrackerType.MEDICATION.name())) {
            txtMedicationName.setText(name);
        } else {
            txtMedicationName.setText(getString(R.string.medication));
        }

        TrackerRecord record = mTrackerRepository.getTrackerRecordForDay(TrackerType.MEDICATION, System.currentTimeMillis());
        updateStatusCard(imgMedication, txtMedicationStatus, btnMarkMedicationTaken, record);

        btnMarkMedicationTaken.setOnClickListener(v -> {
            mTrackerRepository.saveTrackerRecord(TrackerType.MEDICATION, true, null);
            TrackerRecord updatedRecord = mTrackerRepository.getTrackerRecordForDay(TrackerType.MEDICATION, System.currentTimeMillis());
            updateStatusCard(imgMedication, txtMedicationStatus, btnMarkMedicationTaken, updatedRecord);
            renderTrackerProgress();
            WidgetTrackerProvider.refresh(this);
        });

        card.findViewById(R.id.btnMedicationHistory).setOnClickListener(v ->
                mDynamicCardHistoryService.showDailyHistoryDialog(tracker, () -> {
                    refreshSimpleCard(TrackerType.MEDICATION, imgMedication, txtMedicationStatus, btnMarkMedicationTaken);
                    renderTrackerProgress();
                    WidgetTrackerProvider.refresh(this);
                }));

        return card;
    }

    private View createSupplementCard(LayoutInflater inflater, Tracker tracker) {
        View card = inflater.inflate(R.layout.card_supplement, mTrackerCardsContainer, false);

        TextView txtSupplementName = card.findViewById(R.id.txtSupplementName);
        TextView txtSupplementDescription = card.findViewById(R.id.txtSupplementDescription);
        TextView txtSupplementStatus = card.findViewById(R.id.txtSupplementStatus);
        MaterialButton btnMarkSupplementTaken = card.findViewById(R.id.btnMarkSupplementTaken);
        ImageView imgSupplement = card.findViewById(R.id.imgViewSupplement);

        String name = tracker.name();
        if (name != null && !name.equalsIgnoreCase(TrackerType.SUPPLEMENT.name())) {
            txtSupplementName.setText(name);
        } else {
            txtSupplementName.setText(getString(R.string.supplement));
        }

        if (tracker.description() != null && !tracker.description().isEmpty()) {
            txtSupplementDescription.setText(tracker.description());
            txtSupplementDescription.setVisibility(View.VISIBLE);
        } else {
            txtSupplementDescription.setVisibility(View.GONE);
        }

        TrackerRecord record = mTrackerRepository.getTrackerRecordForDay(TrackerType.SUPPLEMENT, System.currentTimeMillis());
        updateStatusCard(imgSupplement, txtSupplementStatus, btnMarkSupplementTaken, record);

        btnMarkSupplementTaken.setOnClickListener(v -> {
            mTrackerRepository.saveTrackerRecord(TrackerType.SUPPLEMENT, true, null);
            TrackerRecord updatedRecord = mTrackerRepository.getTrackerRecordForDay(TrackerType.SUPPLEMENT, System.currentTimeMillis());
            updateStatusCard(imgSupplement, txtSupplementStatus, btnMarkSupplementTaken, updatedRecord);
            renderTrackerProgress();
            WidgetTrackerProvider.refresh(this);
        });

        card.findViewById(R.id.btnSupplementHistory).setOnClickListener(v ->
                mDynamicCardHistoryService.showDailyHistoryDialog(tracker, () -> {
                    refreshSimpleCard(TrackerType.SUPPLEMENT, imgSupplement, txtSupplementStatus, btnMarkSupplementTaken);
                    renderTrackerProgress();
                    WidgetTrackerProvider.refresh(this);
                }));

        return card;
    }

    /**
     * Updates a status card (workout, medication, supplement) based on the given record.
     * Shows "Completed" in green, with the icon in green too, and hides the action button when the task is done,
     * otherwise shows "Not completed" in the muted text color with the action button visible.
     */
    private void updateStatusCard(ImageView imgIcon, TextView txtStatus, MaterialButton btnAction, TrackerRecord record) {
        CalendarCellStyler.tintIcon(imgIcon, CardStatusResolver.routine(record));
        if (record != null && record.completed()) {
            txtStatus.setText(getString(R.string.completed));
            txtStatus.setTextColor(getColor(R.color.status_good));
            btnAction.setVisibility(View.GONE);
        } else {
            txtStatus.setText(getString(R.string.not_completed));
            txtStatus.setTextColor(getColor(R.color.text_muted));
            btnAction.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Repaints a simple card (workout, medication or supplement) with the record
     * of the day, so the card behind the history panel shows the same status the
     * panel just changed.
     */
    private void refreshSimpleCard(TrackerType type, ImageView imgIcon, TextView txtStatus, MaterialButton btnAction) {
        updateStatusCard(imgIcon, txtStatus, btnAction, mDynamicCardHistoryService.getTodayRecord(type));
    }

    /**
     * Repaints the week strip and the ring of today's score: the same weighted
     * percentage the history calendar computes, drawn as the vivid status color
     * (green / yellow / red) over a track in the deep tone of that same status.
     * Before anything is logged the ring stays empty on the neutral track, with
     * a dash in the middle.
     */
    private void renderTrackerProgress() {
        mWeekPreviewRenderer.render();

        DayScoreSummary score = mHistoryService.getTodayScore();
        if (!score.hasScore()) {
            mProgressDayScore.setTrackColor(getColor(R.color.accent_deep));
            mProgressDayScore.setProgressCompat(0, true);
            mTxtDayScore.setText(R.string.day_score_empty);
            mProgressDayScore.setContentDescription(getString(R.string.day_score_none_description));
            return;
        }

        int rounded = (int) Math.round(score.percentage());
        mProgressDayScore.setTrackColor(getColor(CalendarCellStyler.statusDeepColorRes(score.status())));
        mProgressDayScore.setIndicatorColor(getColor(CalendarCellStyler.statusColorRes(score.status())));
        mProgressDayScore.setProgressCompat(rounded, true);
        mTxtDayScore.setText(getString(R.string.day_score_percent, rounded));
        mProgressDayScore.setContentDescription(getString(R.string.day_score_description, rounded));
    }

    @Override
    protected void onDestroy() {
        mHomeCardWaterService.closeDb();
        mHomeCardExpenseService.closeDb();
        mHomeCardMealService.closeDb();
        mDynamicCardHistoryService.closeDb();
        mTrackerRepository.closeDb();
        mMealRepository.closeDb();
        mHistoryService.closeDb();
        super.onDestroy();
    }
}
