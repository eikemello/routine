package com.android.nls.routine.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsControllerCompat;
import com.android.nls.routine.R;
import com.android.nls.routine.model.CreditCard;
import com.android.nls.routine.service.ConfigService;
import com.android.nls.routine.utils.BottomNavHelper;
import com.android.nls.routine.utils.Common;
import com.android.nls.routine.utils.Constants;
import com.android.nls.routine.widget.WidgetCombinedProvider;
import com.google.android.material.materialswitch.MaterialSwitch;
import java.util.List;

public class ConfigActivity extends AppCompatActivity {
    private TextView txtDailyWaterGoal;
    private TextView txtDefaultBtn1;
    private TextView txtDefaultBtn2;
    private TextView txtDefaultBtn3;
    private TextView txtMonthlyLimit;
    private TextView txtNoCards;
    private MaterialSwitch switchNotifyAccess;
    private LinearLayout btnDailyWater;
    private LinearLayout btnDefaultValue1;
    private LinearLayout btnDefaultValue2;
    private LinearLayout btnDefaultValue3;
    private LinearLayout btnMonthlyLimit;
    private LinearLayout btnCardStatementClosing;
    private LinearLayout btnAllowNotifyAccess;
    private LinearLayout cardsListContainer;
    private ConfigService mConfigService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_config);
        WindowInsetsControllerCompat windowInsetsController = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

        mConfigService = new ConfigService(this);

        startUIComponents();
        setupButtonListeners();
        BottomNavHelper.setup(this, R.id.nav_menu);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initFields();
    }

    private void startUIComponents() {
        txtDailyWaterGoal = findViewById(R.id.txtDailyWaterGoal);
        txtDefaultBtn1 = findViewById(R.id.txtDefaultBtn1);
        txtDefaultBtn2 = findViewById(R.id.txtDefaultBtn2);
        txtDefaultBtn3 = findViewById(R.id.txtDefaultBtn3);
        txtMonthlyLimit = findViewById(R.id.txtMonthlyLimit);
        txtNoCards = findViewById(R.id.txtNoCards);
        switchNotifyAccess = findViewById(R.id.switchNotifyAccess);
        btnDailyWater = findViewById(R.id.btnDailyWater);
        btnDefaultValue1 = findViewById(R.id.btnDefaultValue1);
        btnDefaultValue2 = findViewById(R.id.btnDefaultValue2);
        btnDefaultValue3 = findViewById(R.id.btnDefaultValue3);
        btnMonthlyLimit = findViewById(R.id.btnMonthlyLimit);
        btnCardStatementClosing = findViewById(R.id.btnCardStatementClosing);
        btnAllowNotifyAccess = findViewById(R.id.btnAllowNotifyAccess);
        cardsListContainer = findViewById(R.id.cardsListContainer);
    }

    private void setupButtonListeners() {
        btnDailyWater.setOnClickListener(v -> showConfigDialog(Constants.DAILY_WATER, txtDailyWaterGoal));
        btnDefaultValue1.setOnClickListener(v -> showConfigDialog(Constants.BTN_DEFAULT_1, txtDefaultBtn1));
        btnDefaultValue2.setOnClickListener(v -> showConfigDialog(Constants.BTN_DEFAULT_2, txtDefaultBtn2));
        btnDefaultValue3.setOnClickListener(v -> showConfigDialog(Constants.BTN_DEFAULT_3, txtDefaultBtn3));
        btnMonthlyLimit.setOnClickListener(v -> mConfigService.showAlertDialog(Constants.MONTHLY_LIMIT, txtMonthlyLimit));
        btnCardStatementClosing.setOnClickListener(v -> openCardsDialog());
        btnAllowNotifyAccess.setOnClickListener(v -> mConfigService.setNotifyAccess());
    }

    /**
     * Water config dialog (goal and button values). The water widget shows the
     * same numbers, so it is repainted once the value is saved.
     */
    private void showConfigDialog(String configKey, TextView valueView) {
        mConfigService.showAlertDialog(configKey, valueView, () -> WidgetCombinedProvider.refresh(this));
    }

    private void initFields() {
        txtDailyWaterGoal.setText(this.getString(R.string.daily_water_ml, mConfigService.getDailyWaterGoal()));
        txtDefaultBtn1.setText(this.getString(R.string.daily_water_ml, mConfigService.getDefaultBtn1Value()));
        txtDefaultBtn2.setText(this.getString(R.string.daily_water_ml, mConfigService.getDefaultBtn2Value()));
        txtDefaultBtn3.setText(this.getString(R.string.daily_water_ml, mConfigService.getDefaultBtn3Value()));
        txtMonthlyLimit.setText(this.getString(R.string.expense_sum, mConfigService.getMonthlyLimitValue()));
        renderCards();
        // The switch only mirrors the system permission: tapping the row opens
        // the system settings, and onResume reads the state back from there
        switchNotifyAccess.setChecked(mConfigService.isNotifyAccessEnabled());
    }

    private void openCardsDialog() {
        mConfigService.showCardsDialog(this::renderCards);
    }

    /**
     * Renders one row per configured card (bank, last four digits and closing
     * day) inside the Expenses group, each one after an inset divider like
     * the other rows of the group.
     */
    private void renderCards() {
        cardsListContainer.removeAllViews();

        List<CreditCard> cards = mConfigService.getCards();
        txtNoCards.setVisibility(cards.isEmpty() ? View.VISIBLE : View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (CreditCard card : cards) {
            View divider = new View(this);
            LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, Common.dpToPx(this, 1));
            dividerParams.setMarginStart(Common.dpToPx(this, 16));
            divider.setLayoutParams(dividerParams);
            divider.setBackgroundColor(getColor(R.color.outline_subtle));
            cardsListContainer.addView(divider);

            View row = inflater.inflate(R.layout.item_cards_config_row, cardsListContainer, false);
            TextView txtCardBank = row.findViewById(R.id.txtCardBank);
            TextView txtCardLastFour = row.findViewById(R.id.txtCardLastFour);
            TextView txtCardClosingDay = row.findViewById(R.id.txtCardClosingDay);

            txtCardBank.setText(card.bankName());
            txtCardLastFour.setText(this.getString(R.string.card_last_four, card.lastFour()));
            txtCardClosingDay.setText(this.getString(R.string.card_closes_on_day, card.closingDay()));
            row.setOnClickListener(v -> openCardsDialog());

            cardsListContainer.addView(row);
        }
    }

    @Override
    protected void onDestroy() {
        mConfigService.closeDb();
        super.onDestroy();
    }
}
