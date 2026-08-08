package com.android.nls.routine.activity;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsControllerCompat;
import com.android.nls.routine.R;
import com.android.nls.routine.service.ConfigService;
import com.android.nls.routine.utils.Constants;

public class ConfigActivity extends AppCompatActivity {
    private TextView txtDailyWaterGoal;
    private TextView txtDefaultBtn1;
    private TextView txtDefaultBtn2;
    private TextView txtDefaultBtn3;
    private TextView txtMonthlyLimit;
    private LinearLayout btnDailyWater;
    private LinearLayout btnDefaultValue1;
    private LinearLayout btnDefaultValue2;
    private LinearLayout btnDefaultValue3;
    private LinearLayout btnMonthlyLimit;
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
        btnDailyWater = findViewById(R.id.btnDailyWater);
        btnDefaultValue1 = findViewById(R.id.btnDefaultValue1);
        btnDefaultValue2 = findViewById(R.id.btnDefaultValue2);
        btnDefaultValue3 = findViewById(R.id.btnDefaultValue3);
        btnMonthlyLimit = findViewById(R.id.btnMonthlyLimit);
    }

    private void setupButtonListeners() {
        btnDailyWater.setOnClickListener(v -> mConfigService.showAlertDialog(Constants.DAILY_WATER, txtDailyWaterGoal));
        btnDefaultValue1.setOnClickListener(v -> mConfigService.showAlertDialog(Constants.BTN_DEFAULT_1, txtDefaultBtn1));
        btnDefaultValue2.setOnClickListener(v -> mConfigService.showAlertDialog(Constants.BTN_DEFAULT_2, txtDefaultBtn2));
        btnDefaultValue3.setOnClickListener(v -> mConfigService.showAlertDialog(Constants.BTN_DEFAULT_3, txtDefaultBtn3));
        btnMonthlyLimit.setOnClickListener(v -> mConfigService.showAlertDialog(Constants.MONTHLY_LIMIT, txtMonthlyLimit));
    }

    private void initFields() {
        txtDailyWaterGoal.setText(this.getString(R.string.water_default_value_init, mConfigService.getDailyWaterGoal()));
        txtDefaultBtn1.setText(this.getString(R.string.water_default_value_init, mConfigService.getDefaultBtn1Value()));
        txtDefaultBtn2.setText(this.getString(R.string.water_default_value_init, mConfigService.getDefaultBtn2Value()));
        txtDefaultBtn3.setText(this.getString(R.string.water_default_value_init, mConfigService.getDefaultBtn3Value()));
        txtMonthlyLimit.setText(this.getString(R.string.monthly_limit_value_init, mConfigService.getMonthlyLimitValue()));
    }

    @Override
    protected void onDestroy() {
        mConfigService.closeDb();
        super.onDestroy();
    }
}
