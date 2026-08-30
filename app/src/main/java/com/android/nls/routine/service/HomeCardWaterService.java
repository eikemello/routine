package com.android.nls.routine.service;

import android.content.Context;
import android.util.Log;
import android.widget.TextView;
import com.android.nls.routine.R;
import com.android.nls.routine.model.WaterRecord;
import com.android.nls.routine.repository.ConfigRepository;
import com.android.nls.routine.repository.WaterRepository;
import com.android.nls.routine.utils.Common;
import com.google.android.material.progressindicator.LinearProgressIndicator;

public class HomeCardWaterService {
    private static final String TAG = Common.generateTag(HomeCardWaterService.class);
    private final WaterRepository mWaterRepository;
    private final ConfigRepository mConfigRepository;
    private final Context mContext;

    public HomeCardWaterService(Context context) {
        mContext = context;
        mWaterRepository = new WaterRepository(mContext);
        mConfigRepository = new ConfigRepository(mContext);
    }

    public void addWater(TextView txtDailyWaterDrank, TextView txtLastWaterAdded, LinearProgressIndicator progressWater, String amount) {
        int parsedAmount = Integer.parseInt(amount.replace("+", "").trim());
        long currentTimeMillis = System.currentTimeMillis();
        String currentTime = Common.getHourFromTimestamp(String.valueOf(currentTimeMillis));

        long newRowId = mWaterRepository.insertWater(parsedAmount, currentTimeMillis);

        if (newRowId == -1) {
            Log.e(TAG, "Failed to insert water record");
            return;
        }

        int dailyWaterSum = getDailyWaterSum();
        double dailyWaterGoal = getDailyWaterGoal();

        setDailyWaterDrank(txtDailyWaterDrank, dailyWaterSum, dailyWaterGoal);
        updateWaterProgress(progressWater, dailyWaterSum, dailyWaterGoal);

        txtLastWaterAdded.setText(mContext.getString(R.string.last_added_at, currentTime, String.valueOf(parsedAmount)));
        Log.d(TAG, "Added " + parsedAmount + "ml water. Total: " + dailyWaterSum + "ml");
    }

    public void setDailyWaterDrank(TextView txtDailyWaterDrank, double dailyWaterSum, double dailyWaterGoal) {
        if (dailyWaterSum >= dailyWaterGoal) {
            txtDailyWaterDrank.setText(mContext.getString(R.string.water_default_value_init, dailyWaterSum));
            txtDailyWaterDrank.setTextColor(mContext.getColor(R.color.green));
        } else {
            txtDailyWaterDrank.setText(mContext.getString(R.string.water_default_value_init, dailyWaterSum));
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

    public void closeDb() {
        mWaterRepository.closeDb();
        mConfigRepository.closeDb();
    }
}