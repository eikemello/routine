package com.android.nls.routine.service;

import android.content.Context;
import android.util.Log;
import android.widget.TextView;
import com.android.nls.routine.R;
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
        String dailyWaterGoal = getDailyWaterGoal();

        setDailyWaterDrank(txtDailyWaterDrank, dailyWaterSum, dailyWaterGoal);
        updateWaterProgress(progressWater, dailyWaterSum, dailyWaterGoal);

        txtLastWaterAdded.setText(mContext.getString(R.string.last_added_at, currentTime));
        Log.d(TAG, "Added " + parsedAmount + "ml water. Total: " + dailyWaterSum + "ml");
    }

    public void setDailyWaterDrank(TextView txtDailyWaterDrank, int dailyWaterSum, String dailyWaterGoal) {
        if (dailyWaterSum >= Integer.parseInt(dailyWaterGoal)) {
            txtDailyWaterDrank.setText(mContext.getString(R.string.water_default_value_init, String.valueOf(dailyWaterSum)));
            txtDailyWaterDrank.setTextColor(mContext.getColor(R.color.green));
        } else {
            txtDailyWaterDrank.setText(mContext.getString(R.string.water_default_value_init, String.valueOf(dailyWaterSum)));
        }
    }

    public void updateWaterProgress(LinearProgressIndicator progressWater, int totalSum, String dailyWaterGoal) {
        long percentage = Math.min(100, Math.round((totalSum * 100.0) / Integer.parseInt(dailyWaterGoal)));
        progressWater.setProgress((int) percentage);
    }

    public void updateExpenseProgress(LinearProgressIndicator progressExpense, int totalSum, String dailyWaterGoal) {
        long percentage = Math.min(100, Math.round((totalSum * 100.0) / Integer.parseInt(dailyWaterGoal)));
        progressExpense.setProgress((int) percentage);
    }

    public int getDailyWaterSum() {
        long startOfDay = Common.getStartOfDayInMillis();
        long endOfDay = Common.getEndOfDayInMillis();
        return mWaterRepository.getWaterSum(startOfDay, endOfDay);
    }

    public String getLastWaterAddedTime() {
        long lastTimestamp = mWaterRepository.getLastWaterAddedTimestamp();
        if (lastTimestamp == 0) {
            return "";
        }
        return Common.getHourFromTimestamp(String.valueOf(lastTimestamp));
    }

    public String getDefaultValueBtn1() {
        return mConfigRepository.getDefaultBtn1Value();
    }

    public String getDefaultValueBtn2() {
        return mConfigRepository.getDefaultBtn2Value();
    }

    public String getDefaultValueBtn3() {
        return mConfigRepository.getDefaultBtn3Value();
    }

    public String getDailyWaterGoal() {
        return mConfigRepository.getDailyWaterGoal();
    }

    public void closeDb() {
        mWaterRepository.closeDb();
        mConfigRepository.closeDb();
    }
}