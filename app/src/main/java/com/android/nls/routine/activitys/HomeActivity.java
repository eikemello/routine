package com.android.nls.routine.activitys;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.nls.routine.R;
import com.android.nls.routine.services.HomeService;
import com.android.nls.routine.utils.Common;
import com.android.nls.routine.utils.Constants;
import com.google.android.material.button.MaterialButton;

public class HomeActivity extends AppCompatActivity {
    private static final String TAG = Common.generateTag(HomeActivity.class);
    private HomeService homeService;

    // Water section views
    private TextView txtDayWater;//TODO: Turn this value editable (2500ml, 3000ml....)
    private TextView txtDayWaterDrank;
    private TextView txtLastWaterAddedTime;
    private EditText etCustomValueWater;
    private ImageButton btnSaveWater;
    private MaterialButton btnAddWater1;
    private MaterialButton btnAddWater2;
    private MaterialButton btnAddWater3;

    // Meal section views
    private TextView txtCurrentMeal;//TODO: Get current meal checking hour, but turn editable too.
    private ImageButton btnRightMeal;
    private ImageButton btnWarningMeal;
    private ImageButton btnWrongMeal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "Loading home screen");

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainScrollView), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        homeService = new HomeService(this);

        startUIComponents();
        setupWaterButtonListeners();
        setupMealButtonListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDailyWaterSum();
        loadLastWaterAddedTime();
    }

    private void startUIComponents() {
        //Water UI
        txtDayWater = findViewById(R.id.txtDayWater);
        txtDayWaterDrank = findViewById(R.id.txtDayWaterDrank);
        txtDayWaterDrank.setText(this.getString(R.string.water_default_value_init, String.valueOf(0)));
        txtLastWaterAddedTime = findViewById(R.id.txtLastWaterAddedTime);
        txtLastWaterAddedTime.setText(this.getString(R.string.last_water_added_time, ""));
        etCustomValueWater = findViewById(R.id.etCustomValueWater);
        btnSaveWater = findViewById(R.id.btnSaveWater);
        btnAddWater1 = findViewById(R.id.btnAddWater1);
        btnAddWater2 = findViewById(R.id.btnAddWater2);
        btnAddWater3 = findViewById(R.id.btnAddWater3);

        //Meal UI
        txtCurrentMeal = findViewById(R.id.txtCurrentMeal);
        btnRightMeal = findViewById(R.id.btnRightMeal);
        btnWarningMeal = findViewById(R.id.btnWarningMeal);
        btnWrongMeal = findViewById(R.id.btnWrongMeal);
    }

    private void setupWaterButtonListeners() {
        btnAddWater1.setOnClickListener(v -> {
            homeService.addWater(txtDayWaterDrank, txtLastWaterAddedTime, Integer.parseInt(btnAddWater1.getText().toString()));
        });

        btnAddWater2.setOnClickListener(v -> {
            homeService.addWater(txtDayWaterDrank, txtLastWaterAddedTime, Integer.parseInt(btnAddWater2.getText().toString()));
        });

        btnAddWater3.setOnClickListener(v -> {
            homeService.addWater(txtDayWaterDrank, txtLastWaterAddedTime, Integer.parseInt(btnAddWater3.getText().toString()));
        });

        btnSaveWater.setOnClickListener(v -> {
            String water = etCustomValueWater.getText().toString().trim();
            if (!water.isBlank()) {
                homeService.saveCustomWaterValue(txtDayWaterDrank, txtLastWaterAddedTime, Integer.parseInt(water));
                etCustomValueWater.getText().clear();
            } else {
                Common.generateToastMessageShortInvalidNumber(this, Constants.WATER_INVALID_NUMBER);
            }
        });
    }

    private void setupMealButtonListeners() {
        btnRightMeal.setOnClickListener(v -> {

        });

        btnWarningMeal.setOnClickListener(v -> {

        });

        btnWrongMeal.setOnClickListener(v -> {

        });
    }

    private void loadDailyWaterSum() {
        int totalSum = homeService.getDailyWaterSum();
        txtDayWaterDrank.setText(this.getString(R.string.water_default_value_init, String.valueOf(totalSum)));
        Log.d(TAG, "Daily water sum loaded: " + totalSum + "ml");
    }

    private void loadLastWaterAddedTime() {
        String lastWaterAddedTime = homeService.getLastWaterAddedTime();
        txtLastWaterAddedTime.setText(this.getString(R.string.last_water_added_time, lastWaterAddedTime));
        Log.d(TAG, "Last water drank loaded: " + lastWaterAddedTime + "ml");
    }

    @Override
    protected void onDestroy() {
        homeService.closeDb();
        super.onDestroy();
    }
}