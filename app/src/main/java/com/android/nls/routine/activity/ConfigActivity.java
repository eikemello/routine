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
import com.android.nls.routine.utils.Constants;
import java.util.List;

public class ConfigActivity extends AppCompatActivity {
    private TextView txtDailyWaterGoal;
    private TextView txtDefaultBtn1;
    private TextView txtDefaultBtn2;
    private TextView txtDefaultBtn3;
    private TextView txtMonthlyLimit;
    private TextView txtNoCards;
    private TextView txtNotifyAccess;
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
        BottomNavHelper.setup(this, R.id.nav_config);
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
        txtNotifyAccess = findViewById(R.id.txtNotifyAccess);
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
        btnDailyWater.setOnClickListener(v -> mConfigService.showAlertDialog(Constants.DAILY_WATER, txtDailyWaterGoal));
        btnDefaultValue1.setOnClickListener(v -> mConfigService.showAlertDialog(Constants.BTN_DEFAULT_1, txtDefaultBtn1));
        btnDefaultValue2.setOnClickListener(v -> mConfigService.showAlertDialog(Constants.BTN_DEFAULT_2, txtDefaultBtn2));
        btnDefaultValue3.setOnClickListener(v -> mConfigService.showAlertDialog(Constants.BTN_DEFAULT_3, txtDefaultBtn3));
        btnMonthlyLimit.setOnClickListener(v -> mConfigService.showAlertDialog(Constants.MONTHLY_LIMIT, txtMonthlyLimit));
        btnCardStatementClosing.setOnClickListener(v -> openCardsDialog());
        btnAllowNotifyAccess.setOnClickListener(v -> {
            mConfigService.setNotifyAccess();
            txtNotifyAccess.setText(this.getResources().getString(R.string.not_allowed));
        });
    }

    private void initFields() {
        txtDailyWaterGoal.setText(this.getString(R.string.water_default_value_init, mConfigService.getDailyWaterGoal()));
        txtDefaultBtn1.setText(this.getString(R.string.water_default_value_init, mConfigService.getDefaultBtn1Value()));
        txtDefaultBtn2.setText(this.getString(R.string.water_default_value_init, mConfigService.getDefaultBtn2Value()));
        txtDefaultBtn3.setText(this.getString(R.string.water_default_value_init, mConfigService.getDefaultBtn3Value()));
        txtMonthlyLimit.setText(this.getString(R.string.total_expense_value_init, mConfigService.getMonthlyLimitValue()));
        renderCards();
        txtNotifyAccess.setText(mConfigService.isNotifyAccessEnabled() ? this.getResources().getString(R.string.allowed)
                : this.getResources().getString(R.string.not_allowed));
    }

    private void openCardsDialog() {
        mConfigService.showCardsDialog(this::renderCards);
    }

    /**
     * Renders one row per configured card (bank, last four digits and closing
     * day) below the "Card statement closing" section.
     */
    private void renderCards() {
        cardsListContainer.removeAllViews();

        List<CreditCard> cards = mConfigService.getCards();
        txtNoCards.setVisibility(cards.isEmpty() ? View.VISIBLE : View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (CreditCard card : cards) {
            View row = inflater.inflate(R.layout.item_cards_config_row, cardsListContainer, false);
            TextView txtCardBank = row.findViewById(R.id.txtCardBank);
            TextView txtCardLastFour = row.findViewById(R.id.txtCardLastFour);
            TextView txtCardClosingDay = row.findViewById(R.id.txtCardClosingDay);

            txtCardBank.setText(card.bankName());
            txtCardLastFour.setText(card.lastFour());
            txtCardClosingDay.setText(this.getString(R.string.card_closing_day_value, card.closingDay()));
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
