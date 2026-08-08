package com.android.nls.routine.service;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.android.nls.routine.R;
import com.android.nls.routine.activity.ConfigActivity;
import com.android.nls.routine.service.database.DatabaseHelper;
import com.android.nls.routine.utils.Common;
import com.android.nls.routine.utils.Constants;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.function.Consumer;

public class ConfigService {
    private static final String TAG = Common.generateTag(ConfigActivity.class);
    private final Context mContext;
    private final DatabaseHelper mDatabaseHelper;
    private final SQLiteDatabase mSqliteDatabase;

    public ConfigService(Context context) {
        mContext = context;
        mDatabaseHelper = new DatabaseHelper(mContext);
        mSqliteDatabase = mDatabaseHelper.getWritableDatabase();
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
                title = mContext.getString(R.string.monthly_expenses);
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
        saveConfigValue(Constants.COLUMN_NAME_MONTHLY_LIMIT, value, txtMonthlyLimit, R.string.monthly_limit_value_init);
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
        long result;
        ContentValues contentValues = new ContentValues();
        contentValues.put(columnName, value);

        // Check if a row already exists in the table
        Cursor cursor = mSqliteDatabase.rawQuery(
                "SELECT " + BaseColumns._ID + " FROM " + Constants.TABLE_NAME_USER_CONFIG + " LIMIT 1", null);

        if (cursor.moveToFirst() && cursor.getString(0) != null) {
            // Row exists: UPDATE it
            long id = cursor.getLong(0);
            result = mSqliteDatabase.update(Constants.TABLE_NAME_USER_CONFIG,
                    contentValues, BaseColumns._ID + " = ?", new String[]{String.valueOf(id)});
            Log.d(TAG, "Updated row ID: " + id);
        } else {
            // No row exists: INSERT a new one
            result = mSqliteDatabase.insert(Constants.TABLE_NAME_USER_CONFIG, null, contentValues);
            Log.d(TAG, "Inserted row ID: " + result);
        }
        cursor.close();

        if (result == -1) {
            Log.e(TAG, "Failed to configure " + columnName);
        } else {
            textView.setText(mContext.getString(stringResId, value));
        }
    }

    public String getDailyWaterGoal() {
        String query = "SELECT " + Constants.COLUMN_NAME_DAILY_WATER +
                " FROM " + Constants.TABLE_NAME_USER_CONFIG;

        try (Cursor cursor = mSqliteDatabase.rawQuery(query, null)) {
            if (cursor.moveToFirst() && cursor.getString(0) != null) {
                String dailyWater = (cursor.getString(0));
                Log.d(TAG, "Daily water configured: " + dailyWater);
                return dailyWater;
            } else {
                return Constants.DEFAULT_DAILY_WATER;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting Daily water configured: " + e.getMessage());
        }

        return Constants.DEFAULT_DAILY_WATER;
    }

    public String getDefaultBtn1Value() {
        String query = "SELECT " + Constants.COLUMN_NAME_BTN_1_ADD_WATER +
                " FROM " + Constants.TABLE_NAME_USER_CONFIG;

        try (Cursor cursor = mSqliteDatabase.rawQuery(query, null)) {
            if (cursor.moveToFirst() && cursor.getString(0) != null) {
                String btn1Value = (cursor.getString(0));
                Log.d(TAG, "Default button 1 value configured: " + btn1Value);
                return btn1Value;
            } else {
                return Constants.DEFAULT_BTN_1_VALUE;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting Default button 1 value configured: " + e.getMessage());
        }

        return Constants.DEFAULT_BTN_1_VALUE;
    }

    public String getDefaultBtn2Value() {
        String query = "SELECT " + Constants.COLUMN_NAME_BTN_2_ADD_WATER +
                " FROM " + Constants.TABLE_NAME_USER_CONFIG;

        try (Cursor cursor = mSqliteDatabase.rawQuery(query, null)) {
            if (cursor.moveToFirst() && cursor.getString(0) != null) {
                String btn2Value = (cursor.getString(0));
                Log.d(TAG, "Default button 2 value configured: " + btn2Value);
                return btn2Value;
            } else {
                return Constants.DEFAULT_BTN_2_VALUE;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting Default button 2 value configured: " + e.getMessage());
        }

        return Constants.DEFAULT_BTN_2_VALUE;
    }

    public String getDefaultBtn3Value() {
        String query = "SELECT " + Constants.COLUMN_NAME_BTN_3_ADD_WATER +
                " FROM " + Constants.TABLE_NAME_USER_CONFIG;

        try (Cursor cursor = mSqliteDatabase.rawQuery(query, null)) {
            if (cursor.moveToFirst() && cursor.getString(0) != null) {
                String btn3Value = (cursor.getString(0));
                Log.d(TAG, "Default button 3 value configured: " + btn3Value);
                return btn3Value;
            } else {
                return Constants.DEFAULT_BTN_3_VALUE;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting Default button 3 value configured: " + e.getMessage());
        }

        return Constants.DEFAULT_BTN_2_VALUE;
    }

    public String getMonthlyLimitValue() {
        String query = "SELECT " + Constants.COLUMN_NAME_MONTHLY_LIMIT +
                " FROM " + Constants.TABLE_NAME_USER_CONFIG;

        try (Cursor cursor = mSqliteDatabase.rawQuery(query, null)) {
            if (cursor.moveToFirst() && cursor.getString(0) != null) {
                String monthlyLimit = (cursor.getString(0));
                Log.d(TAG, "Monthly limit configured: " + monthlyLimit);
                return monthlyLimit;
            } else {
                return Constants.DEFAULT_MONTHLY_LIMIT_VALUE;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting Monthly limit configured: " + e.getMessage());
        }

        return Constants.DEFAULT_MONTHLY_LIMIT_VALUE;
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
        mDatabaseHelper.close();
    }
}