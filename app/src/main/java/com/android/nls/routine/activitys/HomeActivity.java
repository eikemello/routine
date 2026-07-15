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
import com.google.android.material.button.MaterialButton;

public class HomeActivity extends AppCompatActivity {
    private static final String TAG = Common.generateTag(HomeActivity.class);

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
    private ImageButton btnCorrectMeal;
    private ImageButton btnWarningMeal;
    private ImageButton btnWrongMeal;

    private HomeService homeService;

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
        btnCorrectMeal = findViewById(R.id.btnCorrectMeal);
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
            homeService.saveCustomWaterValue(txtDayWaterDrank, Integer.parseInt(etCustomValueWater.getText().toString()));
        });
    }

    private void setupMealButtonListeners() {
        btnCorrectMeal.setOnClickListener(v -> {

        });

        btnWarningMeal.setOnClickListener(v -> {

        });

        btnWrongMeal.setOnClickListener(v -> {

        });
    }


    @Override
    protected void onDestroy() {
        homeService.closeDb();
        super.onDestroy();
    }
}