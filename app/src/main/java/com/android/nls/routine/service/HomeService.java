package com.android.nls.routine.service;

import android.content.Context;
import com.android.nls.routine.model.MealRecord;
import com.android.nls.routine.model.TrackerRecord;
import com.android.nls.routine.model.TrackerType;
import com.android.nls.routine.repository.MealRepository;
import com.android.nls.routine.repository.TrackerRepository;
import com.android.nls.routine.utils.Common;
import com.android.nls.routine.utils.Constants;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HomeService {

    private final Context mContext;
    private final HomeCardWaterService mHomeCardWaterService;
    private final MealRepository mMealRepository;
    private final TrackerRepository mTrackerRepository;


    public HomeService(Context context) {
        mContext = context;
        mHomeCardWaterService = new HomeCardWaterService(mContext);
        mMealRepository = new MealRepository(mContext);
        mTrackerRepository = new TrackerRepository(mContext);
    }


    public Set<TrackerType> getCompletedTrackersToday() {
        Set<TrackerType> completed = new HashSet<>();

        // Water
        double dailyGoal = mHomeCardWaterService.getDailyWaterGoal();
        int waterSum = mHomeCardWaterService.getDailyWaterSum();
        if (waterSum >= dailyGoal) {
            completed.add(TrackerType.WATER);
        }

        // Meals - all 4 meals logged
        if (areAllMealsLogged()) {
            completed.add(TrackerType.MEALS);
        }

        // Workout, Medication, Supplement
        TrackerRecord workout = mTrackerRepository.getTrackerRecordForDay(TrackerType.WORKOUT, System.currentTimeMillis());
        if (workout != null && workout.completed()) {
            completed.add(TrackerType.WORKOUT);
        }

        TrackerRecord medication = mTrackerRepository.getTrackerRecordForDay(TrackerType.MEDICATION, System.currentTimeMillis());
        if (medication != null && medication.completed()) {
            completed.add(TrackerType.MEDICATION);
        }

        TrackerRecord supplement = mTrackerRepository.getTrackerRecordForDay(TrackerType.SUPPLEMENT, System.currentTimeMillis());
        if (supplement != null && supplement.completed()) {
            completed.add(TrackerType.SUPPLEMENT);
        }

        return completed;
    }

    public boolean areAllMealsLogged() {
        long startOfDay = Common.getStartOfDayInMillis();
        long endOfDay = Common.getEndOfDayInMillis();
        List<MealRecord> mealRecords = mMealRepository.getMealRecords(startOfDay, endOfDay);
        Set<String> loggedMeals = new HashSet<>();
        for (MealRecord record : mealRecords) {
            loggedMeals.add(record.meal());
        }
        return loggedMeals.contains(Constants.BREAKFAST)
                && loggedMeals.contains(Constants.LUNCH)
                && loggedMeals.contains(Constants.TEA)
                && loggedMeals.contains(Constants.DINNER);
    }
}
