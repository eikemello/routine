package com.android.nls.routine.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsControllerCompat;

import com.android.nls.routine.R;
import com.android.nls.routine.model.Tracker;
import com.android.nls.routine.model.TrackerRecord;
import com.android.nls.routine.model.TrackerType;
import com.android.nls.routine.service.HomeCardExpenseService;
import com.android.nls.routine.service.HomeCardMealService;
import com.android.nls.routine.service.HomeCardWaterService;
import com.android.nls.routine.repository.TrackerRepository;
import com.android.nls.routine.utils.BottomNavHelper;
import com.android.nls.routine.utils.Common;
import com.android.nls.routine.utils.Constants;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.List;

public class HomeActivity extends AppCompatActivity {
    private HomeCardWaterService mHomeCardWaterService;
    private HomeCardExpenseService mHomeCardExpenseService;
    private HomeCardMealService mHomeCardMealService;
    private TrackerRepository mTrackerRepository;

    // Header views
    private TextView txtCurrentGreeting;
    private TextView txtCurrentDate;

    // Dynamic tracker cards container
    private LinearLayout mTrackerCardsContainer;

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
        mTrackerRepository = new TrackerRepository(this);

        startUIComponents();
        BottomNavHelper.setup(this, R.id.nav_home);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initFields();
        renderTrackerCards();
    }

    private void startUIComponents() {
        txtCurrentDate = findViewById(R.id.txtCurrentDate);
        txtCurrentGreeting = findViewById(R.id.txtCurrentGreeting);
        mTrackerCardsContainer = findViewById(R.id.trackerCardsContainer);
    }

    private void initFields() {
        txtCurrentDate.setText(Common.getWeekDay());
        txtCurrentGreeting.setText(Common.getCurrentGreeting());
    }

    private void renderTrackerCards() {
        mTrackerCardsContainer.removeAllViews();
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

        TextView txtDailyWater = card.findViewById(R.id.txtDailyWater);
        TextView txtDailyWaterDrank = card.findViewById(R.id.txtDailyWaterDrank);
        TextView txtLastWaterAddedTime = card.findViewById(R.id.txtLastWaterAddedTime);
        LinearProgressIndicator progressWater = card.findViewById(R.id.progressWater);
        MaterialButton btnAddWater1 = card.findViewById(R.id.btnAddWater1);
        MaterialButton btnAddWater2 = card.findViewById(R.id.btnAddWater2);
        MaterialButton btnAddWater3 = card.findViewById(R.id.btnAddWater3);

        String dailyWaterGoal = mHomeCardWaterService.getDailyWaterGoal();
        int dailyWaterSum = mHomeCardWaterService.getDailyWaterSum();

        txtDailyWater.setText(this.getString(R.string.daily_water_ml, dailyWaterGoal));
        txtLastWaterAddedTime.setText(this.getString(R.string.last_added_at, mHomeCardWaterService.getLastWaterAddedTime()));
        btnAddWater1.setText(this.getString(R.string.water_default_value_50, mHomeCardWaterService.getDefaultValueBtn1()));
        btnAddWater2.setText(this.getString(R.string.water_default_value_100, mHomeCardWaterService.getDefaultValueBtn2()));
        btnAddWater3.setText(this.getString(R.string.water_default_value_250, mHomeCardWaterService.getDefaultValueBtn3()));
        mHomeCardWaterService.updateWaterProgress(progressWater, dailyWaterSum, dailyWaterGoal);
        mHomeCardWaterService.setDailyWaterDrank(txtDailyWaterDrank, dailyWaterSum, dailyWaterGoal);

        btnAddWater1.setOnClickListener(v -> mHomeCardWaterService.addWater(txtDailyWaterDrank, txtLastWaterAddedTime, progressWater, btnAddWater1.getText().toString()));
        btnAddWater2.setOnClickListener(v -> mHomeCardWaterService.addWater(txtDailyWaterDrank, txtLastWaterAddedTime, progressWater, btnAddWater2.getText().toString()));
        btnAddWater3.setOnClickListener(v -> mHomeCardWaterService.addWater(txtDailyWaterDrank, txtLastWaterAddedTime, progressWater, btnAddWater3.getText().toString()));

        return card;
    }

    private View createMealCard(LayoutInflater inflater) {
        View card = inflater.inflate(R.layout.card_meal, mTrackerCardsContainer, false);

        MaterialButton btnCorrectMeal = card.findViewById(R.id.btnCorrectMeal);
        MaterialButton btnWarningMeal = card.findViewById(R.id.btnWarningMeal);
        MaterialButton btnWrongMeal = card.findViewById(R.id.btnWrongMeal);

        btnCorrectMeal.setOnClickListener(v -> mHomeCardMealService.showAlertDialog(Constants.CORRECT_MEAL));
        btnWarningMeal.setOnClickListener(v -> mHomeCardMealService.showAlertDialog(Constants.WARNING_MEAL));
        btnWrongMeal.setOnClickListener(v -> mHomeCardMealService.showAlertDialog(Constants.WRONG_MEAL));

        return card;
    }

    private View createExpenseCard(LayoutInflater inflater) {
        View card = inflater.inflate(R.layout.card_expense, mTrackerCardsContainer, false);
        String monthlyLimit = mHomeCardExpenseService.getMonthlyLimitValue();
        String totalSpent = mHomeCardExpenseService.getTotalSpent();

        RadioButton rbNotifyPermissions = card.findViewById(R.id.rbNotifyPermission);
        TextView txtTotalSpent = card.findViewById(R.id.txtTotalSpent);
        TextView txtTotalValue = card.findViewById(R.id.txtTotalValue);
        LinearProgressIndicator progressExpense = card.findViewById(R.id.progressExpense);

        txtTotalValue.setText(this.getString(R.string.total_expense_value_init, monthlyLimit));
        txtTotalSpent.setText(this.getString(R.string.total_expense_value_init, totalSpent));

        if (Integer.parseInt(totalSpent) > Integer.parseInt(monthlyLimit)) {
            txtTotalSpent.setTextColor(this.getColor(R.color.red));
        }

        mHomeCardWaterService.updateExpenseProgress(progressExpense, Integer.parseInt(totalSpent), monthlyLimit);

        if (mHomeCardExpenseService.isNotifyAccessEnabled()) {
            rbNotifyPermissions.setChecked(true);
        } else {
            mHomeCardExpenseService.setNotifyAccess();
            rbNotifyPermissions.setChecked(false);
        }

        rbNotifyPermissions.setOnClickListener(view -> {
            mHomeCardExpenseService.setNotifyAccess();
            rbNotifyPermissions.setChecked(false);
        });

        return card;
    }

    private View createWorkoutCard(LayoutInflater inflater, Tracker tracker) {
        View card = inflater.inflate(R.layout.card_workout, mTrackerCardsContainer, false);

        TextView txtWorkoutStatus = card.findViewById(R.id.txtWorkoutStatus);
        MaterialButton btnCompleteWorkout = card.findViewById(R.id.btnCompleteWorkout);

        TrackerRecord record = mTrackerRepository.getTrackerRecordForDay(TrackerType.WORKOUT, System.currentTimeMillis());
        updateStatusCard(txtWorkoutStatus, btnCompleteWorkout, record);

        btnCompleteWorkout.setOnClickListener(v -> {
            mTrackerRepository.saveTrackerRecord(TrackerType.WORKOUT, true, null);
            TrackerRecord updatedRecord = mTrackerRepository.getTrackerRecordForDay(TrackerType.WORKOUT, System.currentTimeMillis());
            updateStatusCard(txtWorkoutStatus, btnCompleteWorkout, updatedRecord);
        });

        return card;
    }

    private View createMedicationCard(LayoutInflater inflater, Tracker tracker) {
        View card = inflater.inflate(R.layout.card_medication, mTrackerCardsContainer, false);

        TextView txtMedicationName = card.findViewById(R.id.txtMedicationName);
        TextView txtMedicationStatus = card.findViewById(R.id.txtMedicationStatus);
        MaterialButton btnMarkMedicationTaken = card.findViewById(R.id.btnMarkMedicationTaken);

        String name = tracker.name();
        if (name != null && !name.equalsIgnoreCase(TrackerType.MEDICATION.name())) {
            txtMedicationName.setText(name);
        } else {
            txtMedicationName.setText(getString(R.string.medication));
        }

        TrackerRecord record = mTrackerRepository.getTrackerRecordForDay(TrackerType.MEDICATION, System.currentTimeMillis());
        updateStatusCard(txtMedicationStatus, btnMarkMedicationTaken, record);

        btnMarkMedicationTaken.setOnClickListener(v -> {
            mTrackerRepository.saveTrackerRecord(TrackerType.MEDICATION, true, null);
            TrackerRecord updatedRecord = mTrackerRepository.getTrackerRecordForDay(TrackerType.MEDICATION, System.currentTimeMillis());
            updateStatusCard(txtMedicationStatus, btnMarkMedicationTaken, updatedRecord);
        });

        return card;
    }

    private View createSupplementCard(LayoutInflater inflater, Tracker tracker) {
        View card = inflater.inflate(R.layout.card_supplement, mTrackerCardsContainer, false);

        TextView txtSupplementName = card.findViewById(R.id.txtSupplementName);
        TextView txtSupplementDescription = card.findViewById(R.id.txtSupplementDescription);
        TextView txtSupplementStatus = card.findViewById(R.id.txtSupplementStatus);
        MaterialButton btnMarkSupplementTaken = card.findViewById(R.id.btnMarkSupplementTaken);

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
        updateStatusCard(txtSupplementStatus, btnMarkSupplementTaken, record);

        btnMarkSupplementTaken.setOnClickListener(v -> {
            mTrackerRepository.saveTrackerRecord(TrackerType.SUPPLEMENT, true, null);
            TrackerRecord updatedRecord = mTrackerRepository.getTrackerRecordForDay(TrackerType.SUPPLEMENT, System.currentTimeMillis());
            updateStatusCard(txtSupplementStatus, btnMarkSupplementTaken, updatedRecord);
        });

        return card;
    }

    /**
     * Updates a status card (workout, medication, supplement) based on the given record.
     * Shows "Completed" in green and hides the action button when the task is done,
     * otherwise shows "Not completed" in white with the action button visible.
     */
    private void updateStatusCard(TextView txtStatus, MaterialButton btnAction, TrackerRecord record) {
        if (record != null && record.completed()) {
            txtStatus.setText(getString(R.string.completed));
            txtStatus.setTextColor(getColor(R.color.green));
            btnAction.setVisibility(View.GONE);
        } else {
            txtStatus.setText(getString(R.string.not_completed));
            txtStatus.setTextColor(getColor(R.color.white));
            btnAction.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        mHomeCardWaterService.closeDb();
        mHomeCardExpenseService.closeDb();
        mHomeCardMealService.closeDb();
        mTrackerRepository.closeDb();
        super.onDestroy();
    }
}