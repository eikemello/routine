package com.android.nls.routine.activitys;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsControllerCompat;
import com.android.nls.routine.R;
import com.android.nls.routine.services.HomeService;
import com.android.nls.routine.utils.Common;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;

public class HomeActivity extends AppCompatActivity {
    private static final String TAG = Common.generateTag(HomeActivity.class);
    private HomeService mHomeService;

    // Water section views
    private TextView txtDailyWater;
    private TextView txtDailyWaterDrank;
    private TextView txtCurrentGreeting;
    private TextView txtCurrentDate;
    private TextView txtLastWaterAddedTime;
    private TextInputLayout txtInputError;
    private EditText etCustomValueWater;
    private ImageButton btnUserConfig;
    private ImageButton btnAddWater;
    private ImageButton btnMinusWater;
    private MaterialButton btnAddWater1;
    private MaterialButton btnAddWater2;
    private MaterialButton btnAddWater3;

    // Meal section views
    private ImageButton btnRightMeal;
    private ImageButton btnWarningMeal;
    private ImageButton btnWrongMeal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        // Force status bar icons to be white (light appearance) since the header background
        // is always dark regardless of system dark/light mode
        WindowInsetsControllerCompat windowInsetsController = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

        mHomeService = new HomeService(this);

        startUIComponents();
        setupWaterButtonListeners();
        setupMealButtonListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDailyWaterSum();
        loadLastWaterAddedTime();
        initFields();
    }

    private void startUIComponents() {
        //Water UI
        txtDailyWater = findViewById(R.id.txtDailyWater);
        txtDailyWaterDrank = findViewById(R.id.txtDailyWaterDrank);
        txtDailyWaterDrank.setText(this.getString(R.string.water_default_value_init, String.valueOf(0)));
        txtLastWaterAddedTime = findViewById(R.id.txtLastWaterAddedTime);
        txtLastWaterAddedTime.setText(this.getString(R.string.last_added_at, ""));
        txtCurrentDate = findViewById(R.id.txtCurrentDate);
        txtCurrentGreeting = findViewById(R.id.txtCurrentGreeting);
        etCustomValueWater = findViewById(R.id.etCustomValueWater);
        btnUserConfig = findViewById(R.id.btnUserConfig);
        btnAddWater = findViewById(R.id.btnAddWater);
        btnMinusWater = findViewById(R.id.btnMinusWater);
        btnAddWater1 = findViewById(R.id.btnAddWater1);
        btnAddWater2 = findViewById(R.id.btnAddWater2);
        btnAddWater3 = findViewById(R.id.btnAddWater3);
        txtInputError = findViewById(R.id.txtInputError);

        //Meal UI
        btnRightMeal = findViewById(R.id.btnRightMeal);
        btnWarningMeal = findViewById(R.id.btnWarningMeal);
        btnWrongMeal = findViewById(R.id.btnWrongMeal);
    }

    private void setupWaterButtonListeners() {
        btnAddWater1.setOnClickListener(v -> mHomeService.addWater(txtDailyWaterDrank, txtLastWaterAddedTime, Integer.parseInt(btnAddWater1.getText().toString())));

        btnAddWater2.setOnClickListener(v -> mHomeService.addWater(txtDailyWaterDrank, txtLastWaterAddedTime, Integer.parseInt(btnAddWater2.getText().toString())));

        btnAddWater3.setOnClickListener(v -> mHomeService.addWater(txtDailyWaterDrank, txtLastWaterAddedTime, Integer.parseInt(btnAddWater3.getText().toString())));

        btnAddWater.setOnClickListener(v -> {
            String water = etCustomValueWater.getText().toString().trim();
            if (!water.isBlank()) {
                txtInputError.setError(null);
                mHomeService.saveCustomWaterValue(txtDailyWaterDrank, txtLastWaterAddedTime, Integer.parseInt(water));
                etCustomValueWater.getText().clear();
            } else {
                txtInputError.setError("Please enter valid number!");
            }
        });

        btnUserConfig.setOnClickListener(this::showStyledPopupMenu);

        btnMinusWater.setOnClickListener(v -> {
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

    private void initFields(){
        txtCurrentDate.setText(Common.getWeekDay());
        txtCurrentGreeting.setText(Common.getCurrentGreeting());
    }

    private void loadDailyWaterSum() {
        int totalSum = mHomeService.getDailyWaterSum();
        txtDailyWaterDrank.setText(this.getString(R.string.water_default_value_init, String.valueOf(totalSum)));
        Log.d(TAG, "Daily water sum loaded: " + totalSum + "ml");
    }

    private void loadLastWaterAddedTime() {
        String lastWaterAddedTime = mHomeService.getLastWaterAddedTime();
        txtLastWaterAddedTime.setText(this.getString(R.string.last_added_at, lastWaterAddedTime));
    }

    @Override
    protected void onDestroy() {
        mHomeService.closeDb();
        super.onDestroy();
    }
}