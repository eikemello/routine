package com.android.nls.routine.service;

import android.content.Context;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import com.android.nls.routine.R;
import com.android.nls.routine.model.WaterRecord;
import com.android.nls.routine.repository.ConfigRepository;
import com.android.nls.routine.repository.WaterRepository;
import com.android.nls.routine.utils.Common;
import com.android.nls.routine.utils.Constants;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.List;

public class HomeCardWaterService {
    private static final String TAG = Common.generateTag(HomeCardWaterService.class);

    // Extra scrim applied to whatever is behind the history panel. The dialog and
    // the home cards share the same dark palette, so a stronger dim is what makes
    // it obvious that the panel is a modal opened on top of the home screen.
    private static final float HISTORY_DIALOG_DIM = 0.65f;
    private final WaterRepository mWaterRepository;
    private final ConfigRepository mConfigRepository;
    private final Context mContext;

    public HomeCardWaterService(Context context) {
        mContext = context;
        mWaterRepository = new WaterRepository(mContext);
        mConfigRepository = new ConfigRepository(mContext);
    }

    public void addWater(String amount) {
        int parsedAmount = Integer.parseInt(amount.replace("+", "").trim());

        long newRowId = mWaterRepository.insertWater(parsedAmount, System.currentTimeMillis());

        if (newRowId == -1) {
            Log.e(TAG, "Failed to insert water record");
            return;
        }

        Log.d(TAG, "Added " + parsedAmount + "ml water. Total: " + getDailyWaterSum() + "ml");
    }

    public void setDailyWaterDrank(TextView txtDailyWaterDrank, double dailyWaterSum, double dailyWaterGoal) {
        txtDailyWaterDrank.setText(mContext.getString(R.string.water_default_value_init, dailyWaterSum));
        if (dailyWaterSum >= dailyWaterGoal) {
            txtDailyWaterDrank.setTextColor(mContext.getColor(R.color.green_dark));
        } else {
            // Reset it, otherwise the sum would stay green after an insertion is
            // removed and the daily goal is no longer reached
            txtDailyWaterDrank.setTextColor(mContext.getColor(R.color.neon_blue));
        }
    }

    public void updateWaterProgress(LinearProgressIndicator progressWater, int totalSum, double dailyWaterGoal) {
        long percentage = Math.min(100, Math.round((totalSum * 100.0) / dailyWaterGoal));
        progressWater.setProgress((int) percentage);
    }

    public void updateExpenseProgress(LinearProgressIndicator progressExpense, double totalSum, double dailyWaterGoal) {
        long percentage = Math.min(100, Math.round((totalSum * 100.0) / dailyWaterGoal));
        progressExpense.setProgress((int) percentage);
    }

    public int getDailyWaterSum() {
        long startOfDay = Common.getStartOfDayInMillis();
        long endOfDay = Common.getEndOfDayInMillis();
        return mWaterRepository.getWaterSum(startOfDay, endOfDay);
    }

    public WaterRecord getLastWaterAddedRecord() {
        return mWaterRepository.getLastWaterAddedRecord();
    }

    public double getDefaultValueBtn1() {
        return mConfigRepository.getDefaultBtn1Value();
    }

    public double getDefaultValueBtn2() {
        return mConfigRepository.getDefaultBtn2Value();
    }

    public double getDefaultValueBtn3() {
        return mConfigRepository.getDefaultBtn3Value();
    }

    public double getDailyWaterGoal() {
        return mConfigRepository.getDailyWaterGoal();
    }

    public List<WaterRecord> getDailyWaterRecords() {
        long startOfDay = Common.getStartOfDayInMillis();
        long endOfDay = Common.getEndOfDayInMillis();
        return mWaterRepository.getWaterRecords(startOfDay, endOfDay);
    }

    public void showDailyHistoryDialog(Runnable onChanged) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.dialog_water_history, new FrameLayout(mContext), false);

        AlertDialog dialog = new MaterialAlertDialogBuilder(mContext)
                .setTitle(R.string.water_history)
                .setView(view)
                .setPositiveButton(R.string.close_label, null)
                .setCancelable(true)
                .show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(AppCompatResources.getDrawable(mContext, R.drawable.dialog_background));
            // Dims the screen behind the panel further than the default scrim,
            // so the modal is not mistaken for part of the home screen
            dialog.getWindow().setDimAmount(HISTORY_DIALOG_DIM);
        }

        renderRecords(view, onChanged);
    }

    /**
     * Rebuilds the record list inside the dialog view. Called on open and again
     * after an insertion is edited or removed, so the rows and the day total
     * always match what is stored.
     */
    private void renderRecords(View view, Runnable onChanged) {
        TextView txtEmpty = view.findViewById(R.id.txtWaterHistoryEmpty);
        TextView txtTotal = view.findViewById(R.id.txtWaterHistoryTotal);
        LinearLayout recordsContainer = view.findViewById(R.id.waterRecordsContainer);

        recordsContainer.removeAllViews();

        List<WaterRecord> records = getDailyWaterRecords();
        boolean hasRecords = !records.isEmpty();

        int total = 0;
        for (WaterRecord record : records) {
            total += record.amount();
            addRecordRow(recordsContainer, record, view, onChanged);
        }

        if (hasRecords) {
            recordsContainer.getChildAt(recordsContainer.getChildCount() - 1)
                    .findViewById(R.id.viewWaterRecordDivider)
                    .setVisibility(View.GONE);
        }

        txtEmpty.setVisibility(hasRecords ? View.GONE : View.VISIBLE);
        txtTotal.setVisibility(hasRecords ? View.VISIBLE : View.GONE);
        txtTotal.setText(mContext.getString(R.string.water_sum, total));
    }

    private void addRecordRow(LinearLayout recordsContainer, WaterRecord record, View dialogView, Runnable onChanged) {
        View row = LayoutInflater.from(mContext).inflate(R.layout.item_water_record, recordsContainer, false);
        TextView txtTime = row.findViewById(R.id.txtWaterRecordTime);
        TextView txtAmount = row.findViewById(R.id.txtWaterRecordAmount);

        txtTime.setText(Common.getHourFromTimestamp(String.valueOf(record.timestamp())));
        txtAmount.setText(mContext.getString(R.string.water_record_amount, record.amount()));

        row.findViewById(R.id.btnWaterRecordEdit).setOnClickListener(v -> showEditRecordDialog(record, dialogView, onChanged));
        row.findViewById(R.id.btnWaterRecordDelete).setOnClickListener(v -> confirmDeleteRecord(record, dialogView, onChanged));

        recordsContainer.addView(row);
    }

    private void showEditRecordDialog(WaterRecord record, View dialogView, Runnable onChanged) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.dialog_config_default_values, new FrameLayout(mContext), false);
        TextInputLayout txtInputError = view.findViewById(R.id.txtInputError);
        TextInputEditText etValue = view.findViewById(R.id.etValue);

        txtInputError.setHint(mContext.getString(R.string.record_amount_hint));
        txtInputError.setError(null);
        etValue.setText(String.valueOf(record.amount()));

        AlertDialog dialog = new MaterialAlertDialogBuilder(mContext)
                .setTitle(R.string.water_record_edit_title)
                .setView(view)
                .setPositiveButton(mContext.getString(R.string.save_label), null)
                .setNegativeButton(mContext.getString(R.string.cancel_label), null)
                .setCancelable(true)
                .show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(AppCompatResources.getDrawable(mContext, R.drawable.dialog_background));
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            Editable value = etValue.getText();
            if (!checkAmount(value)) {
                txtInputError.setError(Constants.WATER_INVALID_NUMBER);
                return;
            }

            mWaterRepository.updateWater(record.id(), Integer.parseInt(value.toString().trim()));
            dialog.dismiss();
            renderRecords(dialogView, onChanged);
            onChanged.run();
        });
    }

    private void confirmDeleteRecord(WaterRecord record, View dialogView, Runnable onChanged) {
        AlertDialog dialog = new MaterialAlertDialogBuilder(mContext)
                .setTitle(R.string.water_record_delete_title)
                .setMessage(mContext.getString(R.string.water_record_delete_message, record.amount()))
                .setPositiveButton(mContext.getString(R.string.remove_label), (d, which) -> {
                    mWaterRepository.deleteWater(record.id());
                    renderRecords(dialogView, onChanged);
                    onChanged.run();
                })
                .setNegativeButton(mContext.getString(R.string.cancel_label), null)
                .setCancelable(true)
                .show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(AppCompatResources.getDrawable(mContext, R.drawable.dialog_background));
        }
    }

    private boolean checkAmount(Editable value) {
        return value != null
                && !value.toString().trim().isEmpty()
                && value.toString().trim().matches("\\d+")
                && value.length() <= 6;
    }

    public void closeDb() {
        mWaterRepository.closeDb();
        mConfigRepository.closeDb();
    }
}