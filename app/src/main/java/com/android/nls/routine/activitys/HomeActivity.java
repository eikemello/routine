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
        Log.d(TAG, "onResume - refreshing water sum");
        loadWaterSumFromDatabase();
    }

    private void startUIComponents() {
        //Water UI
        txtDayWater = findViewById(R.id.txtDayWater);
        txtDayWaterDrank = findViewById(R.id.txtDayWaterDrank);
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
            Log.d(TAG, "btnWater 1 clicked");
            homeService.addWater(txtDayWaterDrank, Integer.parseInt(btnAddWater1.getText().toString()));
        });

        btnAddWater2.setOnClickListener(v -> {
            homeService.addWater(txtDayWaterDrank, Integer.parseInt(btnAddWater2.getText().toString()));
        });

        btnAddWater3.setOnClickListener(v -> {
            homeService.addWater(txtDayWaterDrank, Integer.parseInt(btnAddWater3.getText().toString()));
        });

        btnSaveWater.setOnClickListener(v -> {
            String water = etCustomValueWater.getText().toString().trim();
            if(!water.isBlank()) {
                homeService.saveCustomWaterValue(txtDayWaterDrank, Integer.parseInt(water) );
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

    private void loadWaterSumFromDatabase() {
        homeService.loadWaterSum(txtDayWaterDrank);
    }

    @Override
    protected void onDestroy() {
        homeService.closeDb();
        super.onDestroy();
    }
}