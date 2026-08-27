package com.android.nls.routine.service;

import android.content.Context;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import androidx.appcompat.app.AlertDialog;
import com.android.nls.routine.R;
import com.android.nls.routine.repository.MealRepository;
import com.android.nls.routine.utils.Common;
import com.android.nls.routine.utils.Constants;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.Calendar;
import java.util.function.Consumer;

public class HomeCardMealService {
    private static final String TAG = Common.generateTag(HomeCardMealService.class);
    private final Context mContext;
    private final MealRepository mMealRepository;
    private String mSelectedMeal;

    public HomeCardMealService(Context context) {
        mContext = context;
        mMealRepository = new MealRepository(mContext);
    }

    public void showAlertDialog(String buttonClicked) {
        String title;
        Consumer<String> saveAction;
        View view = LayoutInflater.from(mContext).inflate(R.layout.dialog_meal, new FrameLayout(mContext), false);
        enableCurrentRbMeal(view);

        TextInputEditText etValue = view.findViewById(R.id.etMealObservation);

        switch (buttonClicked) {
            case Constants.CORRECT_MEAL:
                title = mContext.getString(R.string.correct_meal);
                saveAction = value -> saveCorrectMeal(mSelectedMeal, value);
                break;
            case Constants.WARNING_MEAL:
                title = mContext.getString(R.string.warning_meal);
                saveAction = value -> saveWarningMeal(mSelectedMeal, value);
                break;
            case Constants.WRONG_MEAL:
                title = mContext.getString(R.string.wrong_meal);
                saveAction = value -> saveWrongMeal(mSelectedMeal, value);
                break;
            default:
                return;
        }
        showSaveDialog(title, view, etValue, saveAction);
    }


    private void showSaveDialog(String title, View view, TextInputEditText etValue, Consumer<String> saveAction) {
        TextInputLayout txtInputError = view.findViewById(R.id.txtInputError);
        txtInputError.setError(null);

        AlertDialog dialog = new MaterialAlertDialogBuilder(mContext)
                .setTitle(title)
                .setView(view)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .setCancelable(true)
                .show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            Editable value = etValue.getText();
            if (value != null) {
                saveAction.accept(value.toString().trim());
                dialog.dismiss();
            } else {
                txtInputError.setError(Constants.MEAL_INVALID_TEXT);
            }
        });
    }

    private void saveCorrectMeal(String currentMeal, String value) {
        saveMealValue(Constants.CORRECT_MEAL, currentMeal, value);
    }

    private void saveWarningMeal(String currentMeal, String value) {
        saveMealValue(Constants.WARNING_MEAL, currentMeal, value);
    }

    private void saveWrongMeal(String currentMeal, String value) {
        saveMealValue(Constants.WRONG_MEAL, currentMeal, value);
    }

    private void saveMealValue(String mealStatus, String currentMeal, String value) {
        Log.d(TAG, "saveMealValue: " + mealStatus + " = " + value);
        long result = mMealRepository.insertMeal(currentMeal, mealStatus, value, System.currentTimeMillis());
        Log.d(TAG, "Inserted row ID: " + result);

        if (result == -1) {
            Log.e(TAG, "Failed to save meal " + mealStatus);
        }
    }

    private void enableCurrentRbMeal(View view) {
        RadioButton rbBreakfast = view.findViewById(R.id.rbBreakfast);
        RadioButton rbLunch = view.findViewById(R.id.rbLunch);
        RadioButton rbTea = view.findViewById(R.id.rbTea);
        RadioButton rbDinner = view.findViewById(R.id.rbDinner);
        RadioGroup rgMeal = view.findViewById(R.id.rgMeal);

        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        if (hour >= 5 && hour <= 10) {
            rbBreakfast.setChecked(true);
            mSelectedMeal = Constants.BREAKFAST;
        } else if (hour >= 11 && hour <= 15) {
            rbLunch.setChecked(true);
            mSelectedMeal = Constants.LUNCH;
        } else if (hour >= 16 && hour < 20) {
            rbTea.setChecked(true);
            mSelectedMeal = Constants.TEA;
        } else {
            rbDinner.setChecked(true);
            mSelectedMeal = Constants.DINNER;
        }

        rgMeal.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbBreakfast) {
                mSelectedMeal = Constants.BREAKFAST;
            } else if (checkedId == R.id.rbLunch) {
                mSelectedMeal = Constants.LUNCH;
            } else if (checkedId == R.id.rbTea) {
                mSelectedMeal = Constants.TEA;
            } else if (checkedId == R.id.rbDinner) {
                mSelectedMeal = Constants.DINNER;
            }
            Log.d(TAG, "Selected meal changed to: " + mSelectedMeal);
        });
    }

    public void closeDb() {
        mMealRepository.closeDb();
    }
}