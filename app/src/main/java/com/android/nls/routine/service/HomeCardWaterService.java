package com.android.nls.routine.service;

import android.content.Context;
import android.text.InputType;
import android.util.Log;
import android.widget.TextView;
import com.android.nls.routine.R;
import com.android.nls.routine.cardhistory.CardHistory;
import com.android.nls.routine.cardhistory.CardHistoryDialog;
import com.android.nls.routine.cardhistory.CardRecordDialog;
import com.android.nls.routine.model.WaterRecord;
import com.android.nls.routine.model.WaterWidgetData;
import com.android.nls.routine.repository.ConfigRepository;
import com.android.nls.routine.repository.WaterRepository;
import com.android.nls.routine.utils.Common;
import com.android.nls.routine.utils.Constants;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import java.util.ArrayList;
import java.util.List;

public class HomeCardWaterService implements CardHistory {
    private static final String TAG = Common.generateTag(HomeCardWaterService.class);
    private final WaterRepository mWaterRepository;
    private final ConfigRepository mConfigRepository;
    private final Context mContext;

    public HomeCardWaterService(Context context) {
        mContext = context;
        mWaterRepository = new WaterRepository(mContext);
        mConfigRepository = new ConfigRepository(mContext);
    }

    public void addWater(String amount) {
        int parsedAmount = (int) Double.parseDouble(amount.replace("+", "").trim());

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

    /**
     * Opens the history panel with today's insertions. The panel is rendered by
     * CardHistoryDialog, the renderer shared by every card history.
     */
    public void showDailyHistoryDialog(Runnable onChanged) {
        new CardHistoryDialog(mContext, this).show(onChanged);
    }

    public WaterWidgetData getWidgetData(Context context) {
        int dailyWaterSum = getDailyWaterSum();
        double dailyWaterGoal = getDailyWaterGoal();

        String totalText = context.getString(R.string.widget_water_total, dailyWaterSum);
        int totalColor = dailyWaterSum >= dailyWaterGoal ? context.getColor(R.color.green_dark) : context.getColor(R.color.neon_blue);
        String goalText = context.getString(R.string.widget_water_goal, dailyWaterGoal);

        int progressPercentage = 0;
        if (dailyWaterGoal > 0) {
            progressPercentage = (int) Math.min(100, Math.round((dailyWaterSum * 100.0) / dailyWaterGoal));
        }

        String[] buttonLabels = {
                context.getString(R.string.widget_add_water_amount, getDefaultValueBtn1()),
                context.getString(R.string.widget_add_water_amount, getDefaultValueBtn2()),
                context.getString(R.string.widget_add_water_amount, getDefaultValueBtn3())
        };

        return new WaterWidgetData(totalText, totalColor, goalText, progressPercentage, buttonLabels);
    }

    /**
     * Today's insertions, newest first so the latest ones stay on top of the
     * panel, each one editable and removable, plus the day total. Both actions
     * call the refresh handle of the panel, so the list and the total are
     * rebuilt from the database after a change.
     */
    @Override
    public CardHistory.Panel getHistoryPanel(Runnable refresh) {
        List<CardHistory.Row> rows = new ArrayList<>();
        List<WaterRecord> records = getDailyWaterRecords();
        int total = 0;

        // The records are stored oldest first, so the panel walks them from the
        // end: the insertions the user edits or removes are the latest ones
        for (int i = records.size() - 1; i >= 0; i--) {
            WaterRecord record = records.get(i);
            total += record.amount();
            rows.add(new CardHistory.Row(
                    Common.getHourFromTimestamp(String.valueOf(record.timestamp())),
                    mContext.getString(R.string.water_record_amount, record.amount()),
                    () -> showEditRecordDialog(record, refresh),
                    () -> confirmDeleteRecord(record, refresh)));
        }

        return new CardHistory.Panel(rows, mContext.getString(R.string.water_sum, total));
    }

    /**
     * Prompts for a new amount of an insertion, through the shared record editor
     * of a card history. The refresh handle of the panel is what repaints the
     * list and notifies the home screen behind it.
     */
    private void showEditRecordDialog(WaterRecord record, Runnable refresh) {
        CardRecordDialog.showEditAmountDialog(
                mContext,
                R.string.water_record_edit_title,
                R.string.record_amount_hint,
                String.valueOf(record.amount()),
                InputType.TYPE_CLASS_NUMBER,
                amount -> {
                    if (!checkAmount(amount)) {
                        return Constants.WATER_INVALID_NUMBER;
                    }

                    mWaterRepository.updateWater(record.id(), Integer.parseInt(amount));
                    refresh.run();
                    return null;
                });
    }

    /**
     * Asks before removing an insertion, then refreshes the panel.
     */
    private void confirmDeleteRecord(WaterRecord record, Runnable refresh) {
        CardRecordDialog.confirmRemove(
                mContext,
                R.string.water_record_delete_title,
                mContext.getString(R.string.water_record_delete_message, record.amount()),
                () -> {
                    mWaterRepository.deleteWater(record.id());
                    refresh.run();
                });
    }

    private boolean checkAmount(String value) {
        return !value.isEmpty() && value.matches("\\d+") && value.length() <= 6;
    }

    @Override
    public int getHistoryTitleRes() {
        return R.string.water_history;
    }

    @Override
    public int getHistoryEmptyTextRes() {
        return R.string.no_water_records_today;
    }

    @Override
    public int getHistoryIconRes() {
        return R.drawable.ic_water;
    }

    @Override
    public int getHistoryIconTintRes() {
        return R.color.neon_blue_40;
    }

    @Override
    public int getHistoryValueColorRes() {
        return R.color.neon_blue;
    }

    public void closeDb() {
        mWaterRepository.closeDb();
        mConfigRepository.closeDb();
    }
}