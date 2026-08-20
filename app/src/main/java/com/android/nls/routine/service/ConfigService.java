package com.android.nls.routine.service;

import android.content.Context;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.android.nls.routine.R;
import com.android.nls.routine.activity.ConfigActivity;
import com.android.nls.routine.repository.ConfigRepository;
import com.android.nls.routine.utils.Common;
import com.android.nls.routine.utils.Constants;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.function.Consumer;

public class ConfigService {
    private static final String TAG = Common.generateTag(ConfigActivity.class);
    private final Context mContext;
    private final ConfigRepository mConfigRepository;

    public ConfigService(Context context) {
        mContext = context;
        mConfigRepository = new ConfigRepository(context);
    }

    public void showAlertDialog(String buttonClicked, TextView textView) {
        String title;
        Consumer<String> saveAction;
        View view = LayoutInflater.from(mContext).inflate(R.layout.dialog_config_default_values, new FrameLayout(mContext), false);
        TextInputEditText etValue = view.findViewById(R.id.etValue);

        switch (buttonClicked) {
            case Constants.DAILY_WATER:
                title = mContext.getString(R.string.daily_water);
                saveAction = value -> setDailyWater(value, textView);
                break;
            case Constants.BTN_DEFAULT_1:
                title = mContext.getString(R.string.button_1);
                saveAction = value -> setFirstBtnValue(value, textView);
                break;
            case Constants.BTN_DEFAULT_2:
                title = mContext.getString(R.string.button_2);
                saveAction = value -> setSecondBtnValue(value, textView);
                break;
            case Constants.BTN_DEFAULT_3:
                title = mContext.getString(R.string.button_3);
                saveAction = value -> setThirdBtnValue(value, textView);
                break;
            case Constants.MONTHLY_LIMIT:
                title = mContext.getString(R.string.expenses);
                saveAction = value -> setMonthlyLimit(value, textView);
                break;
            default:
                return;
        }
        showSaveDialog(title, view, etValue, saveAction);
    }

    private void setDailyWater(String value, TextView txtDailyWater) {
        saveConfigValue(Constants.COLUMN_NAME_DAILY_WATER, value, txtDailyWater, R.string.water_default_value_init);
    }

    private void setFirstBtnValue(String value, TextView txtDefaultBtn1) {
        saveConfigValue(Constants.COLUMN_NAME_BTN_1_ADD_WATER, value, txtDefaultBtn1, R.string.water_default_value_init);
    }

    private void setSecondBtnValue(String value, TextView txtDefaultBtn2) {
        saveConfigValue(Constants.COLUMN_NAME_BTN_2_ADD_WATER, value, txtDefaultBtn2, R.string.water_default_value_init);
    }

    private void setThirdBtnValue(String value, TextView txtDefaultBtn3) {
        saveConfigValue(Constants.COLUMN_NAME_BTN_3_ADD_WATER, value, txtDefaultBtn3, R.string.water_default_value_init);
    }

    private void setMonthlyLimit(String value, TextView txtMonthlyLimit) {
        saveConfigValue(Constants.COLUMN_NAME_MONTHLY_LIMIT, value, txtMonthlyLimit, R.string.total_expense_value_init);
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
            if (checkFieldValue(value)) {
                saveAction.accept(value.toString().trim());
                dialog.dismiss();
            } else {
                setFieldError(txtInputError);
            }
        });
    }

    private void saveConfigValue(String columnName, String value, TextView textView, int stringResId) {
        Log.d(TAG, "saveConfigValue: " + columnName + " = " + value);
        mConfigRepository.saveConfigValue(columnName, value);
        textView.setText(mContext.getString(stringResId, value));
    }

    public String getDailyWaterGoal() {
        return mConfigRepository.getDailyWaterGoal();
    }

    public String getDefaultBtn1Value() {
        return mConfigRepository.getDefaultBtn1Value();
    }

    public String getDefaultBtn2Value() {
        return mConfigRepository.getDefaultBtn2Value();
    }

    public String getDefaultBtn3Value() {
        return mConfigRepository.getDefaultBtn3Value();
    }

    public String getMonthlyLimitValue() {
        return mConfigRepository.getMonthlyLimitValue();
    }

    private void setFieldError(TextInputLayout txtInputError) {
        txtInputError.setError(Constants.WATER_INVALID_NUMBER);
    }

    private boolean checkFieldValue(Editable value) {
        if (value == null) {
            return false;
        } else {
            return (!value.toString().trim().isEmpty()
                    && value.toString().matches("\\d+")
                    && value.length() < 5);
        }
    }

    public void closeDb() {
        mConfigRepository.closeDb();
    }
}