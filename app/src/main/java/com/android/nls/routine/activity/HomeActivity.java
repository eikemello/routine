package com.android.nls.routine.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsControllerCompat;
import com.android.nls.routine.R;
import com.android.nls.routine.service.ConfigService;
import com.android.nls.routine.service.HomeCardExpenseService;
import com.android.nls.routine.service.HomeCardWaterService;
import com.android.nls.routine.utils.Common;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;

public class HomeActivity extends AppCompatActivity {
    private HomeCardWaterService mHomeCardWaterService;
    private HomeCardExpenseService mHomeCardExpenseService;
    private ConfigService mConfigService;

    // Water section views
    private TextView txtDailyWater;
    private TextView txtDailyWaterDrank;
    private TextView txtWaterPercentage;
    private TextView txtCurrentGreeting;
    private TextView txtCurrentDate;
    private TextView txtLastWaterAddedTime;
    private ImageButton btnUserConfig;
    private MaterialButton btnAddWater1;
    private MaterialButton btnAddWater2;
    private MaterialButton btnAddWater3;
    private CircularProgressIndicator progressWater;

    // Meal section views
    private ImageButton btnRightMeal;
    private ImageButton btnWarningMeal;
    private ImageButton btnWrongMeal;

    // Expense section views
    private RadioButton rbNotifyPermissions;
    private TextView txtTotalSpent;
    private TextView txtTotalValue;

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
        mConfigService = new ConfigService(this);

        startUIComponents();
        setupWaterButtonListeners();
        setupMealButtonListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initFields();

        if (mHomeCardExpenseService.isNotifyAccessEnabled()) {
            rbNotifyPermissions.setChecked(true);
        } else {
            mHomeCardExpenseService.setNotifyAccess();
            rbNotifyPermissions.setChecked(false);
        }
    }

    private void startUIComponents() {
        //Water UI
        txtDailyWater = findViewById(R.id.txtDailyWater);
        txtDailyWaterDrank = findViewById(R.id.txtDailyWaterDrank);
        txtDailyWaterDrank.setText(this.getString(R.string.water_default_value_init, String.valueOf(0)));
        txtWaterPercentage = findViewById(R.id.txtWaterPercentage);
        progressWater = findViewById(R.id.progressWater);
        txtLastWaterAddedTime = findViewById(R.id.txtLastWaterAddedTime);
        txtLastWaterAddedTime.setText(this.getString(R.string.last_added_at, ""));
        txtCurrentDate = findViewById(R.id.txtCurrentDate);
        txtCurrentGreeting = findViewById(R.id.txtCurrentGreeting);
        btnUserConfig = findViewById(R.id.btnUserConfig);
        btnAddWater1 = findViewById(R.id.btnAddWater1);
        btnAddWater2 = findViewById(R.id.btnAddWater2);
        btnAddWater3 = findViewById(R.id.btnAddWater3);

        //Meal UI
        btnRightMeal = findViewById(R.id.btnRightMeal);
        btnWarningMeal = findViewById(R.id.btnWarningMeal);
        btnWrongMeal = findViewById(R.id.btnWrongMeal);

        //Expense UI
        rbNotifyPermissions = findViewById(R.id.rbNotifyPermission);
        txtTotalSpent = findViewById(R.id.txtTotalSpent);
        txtTotalValue = findViewById(R.id.txtTotalValue);
    }

    private void setupWaterButtonListeners() {
        btnAddWater1.setOnClickListener(v -> mHomeCardWaterService.addWater(txtDailyWaterDrank, txtLastWaterAddedTime, progressWater, txtWaterPercentage, btnAddWater1.getText().toString()));

        btnAddWater2.setOnClickListener(v -> mHomeCardWaterService.addWater(txtDailyWaterDrank, txtLastWaterAddedTime, progressWater, txtWaterPercentage, btnAddWater2.getText().toString()));

        btnAddWater3.setOnClickListener(v -> mHomeCardWaterService.addWater(txtDailyWaterDrank, txtLastWaterAddedTime, progressWater, txtWaterPercentage, btnAddWater3.getText().toString()));

        btnUserConfig.setOnClickListener(this::showStyledPopupMenu);

        rbNotifyPermissions.setOnClickListener(view -> {
            mHomeCardExpenseService.setNotifyAccess();
            rbNotifyPermissions.setChecked(false);
        });
    }

    private void showStyledPopupMenu(View anchor) {
        PopupMenu popup = new PopupMenu(HomeActivity.this, anchor);
        popup.getMenuInflater().inflate(R.menu.menu_user_config, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_user_config) {
                Intent intent = new Intent(HomeActivity.this, ConfigActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void setupMealButtonListeners() {
        btnRightMeal.setOnClickListener(v -> {

        });

        btnWarningMeal.setOnClickListener(v -> {

        });

        btnWrongMeal.setOnClickListener(v -> {

        });
    }

    private void initFields() {
        String dailyWaterGoal = mHomeCardWaterService.getDailyWaterGoal();
        int dailyWaterSum = mHomeCardWaterService.getDailyWaterSum();

        txtCurrentDate.setText(Common.getWeekDay());
        txtCurrentGreeting.setText(Common.getCurrentGreeting());
        txtDailyWater.setText(this.getString(R.string.daily_water_ml, dailyWaterGoal));
        txtLastWaterAddedTime.setText(this.getString(R.string.last_added_at, mHomeCardWaterService.getLastWaterAddedTime()));
        txtTotalValue.setText(this.getString(R.string.monthly_limit_value_init, mConfigService.getMonthlyLimitValue()));
        txtTotalSpent.setText(this.getString(R.string.monthly_limit_value_init, String.valueOf(mHomeCardExpenseService.getTotalSpent())));
        btnAddWater1.setText(this.getString(R.string.water_default_value_50, mHomeCardWaterService.getDefaultValueBtn1()));
        btnAddWater2.setText(this.getString(R.string.water_default_value_100, mHomeCardWaterService.getDefaultValueBtn2()));
        btnAddWater3.setText(this.getString(R.string.water_default_value_250, mHomeCardWaterService.getDefaultValueBtn3()));
        mHomeCardWaterService.updateWaterProgress(progressWater, txtWaterPercentage, dailyWaterSum, dailyWaterGoal);
        mHomeCardWaterService.setDailyWaterDrank(txtDailyWaterDrank, dailyWaterSum, dailyWaterGoal);
    }

    @Override
    protected void onDestroy() {
        mHomeCardWaterService.closeDb();
        mHomeCardExpenseService.closeDb();
        super.onDestroy();
    }
}