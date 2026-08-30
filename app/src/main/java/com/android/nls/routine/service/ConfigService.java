package com.android.nls.routine.service;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
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
        mConfigRepository = new ConfigRepository(mContext);
    }

    public void showAlertDialog(String buttonClicked, TextView textView) {
        String title;
        Consumer<String> saveAction;
        int maxLength = 6;
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
            case Constants.CARD_STATEMENT_CLOSING:
                title = mContext.getString(R.string.card_statement_closing);
                maxLength = 2;
                saveAction = value -> setCardStatementClosingDate(value, textView);
                break;
            default:
                return;
        }
        showSaveDialog(title, view, etValue, saveAction, maxLength);
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

    private void setCardStatementClosingDate(String value, TextView txtCardStatementClosingDate) {
        saveConfigValue(Constants.COLUMN_NAME_CARD_STATEMENT_CLOSING, value, txtCardStatementClosingDate, R.string.card_statement_closing_date_init_value);
    }

    private void showSaveDialog(String title, View view, TextInputEditText etValue, Consumer<String> saveAction, int maxLength) {
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
            if (checkFieldValue(value, maxLength)) {
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

    public double getDailyWaterGoal() {
        return mConfigRepository.getDailyWaterGoal();
    }

    public double getDefaultBtn1Value() {
        return mConfigRepository.getDefaultBtn1Value();
    }

    public double getDefaultBtn2Value() {
        return mConfigRepository.getDefaultBtn2Value();
    }

    public double getDefaultBtn3Value() {
        return mConfigRepository.getDefaultBtn3Value();
    }

    public double getMonthlyLimitValue() {
        return mConfigRepository.getMonthlyLimitValue();
    }

    public double getCardStatementClosingDate() {
        return mConfigRepository.getCardStatementClosingDate();
    }

    public void setNotifyAccess() {
        Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
        mContext.startActivity(intent);
    }

    public boolean isNotifyAccessEnabled() {
        String enabledListeners = Settings.Secure.getString(
                mContext.getContentResolver(),
                "enabled_notification_listeners"
        );
        return enabledListeners != null && enabledListeners.contains(mContext.getPackageName());
    }

    private void setFieldError(TextInputLayout txtInputError) {
        txtInputError.setError(Constants.WATER_INVALID_NUMBER);
    }

    private boolean checkFieldValue(Editable value, int maxLength) {
        if (value == null) {
            return false;
        } else {
            return (!value.toString().trim().isEmpty()
                    && value.toString().matches("\\d+")
                    && value.length() <= maxLength);
        }
    }

    public void closeDb() {
        mConfigRepository.closeDb();
    }
}