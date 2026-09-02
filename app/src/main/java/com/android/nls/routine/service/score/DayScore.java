package com.android.nls.routine.service.score;

import com.android.nls.routine.model.DayStatus;
import com.android.nls.routine.model.TrackerType;
import com.android.nls.routine.utils.Constants;
import java.util.Map;

/**
 * Computes the daily score based on the user's enabled tracker cards.
 * <p>
 * The total 100% is divided equally among the enabled cards (excluding EXPENSES).
 * For each card:
 *   - WATER:      score = consumed / goal (proportion, e.g. 80% of goal -> 80%)
 *   - MEALS:      the card's share is divided among the 4 meals
 *                 (Breakfast, Lunch, Tea, Dinner). Each meal:
 *                 correct = 100%, warning = 50%, wrong = 0%
 *   - WORKOUT:    completed = 100%, not completed = 0%
 *   - MEDICATION: completed = 100%, not completed = 0%
 *   - SUPPLEMENT: completed = 100%, not completed = 0%
 * <p>
 * Color thresholds:
 *   GREEN  (Good)           -> 80-100%
 *   YELLOW (Warning) -> 50-79%
 *   RED    (Bad)          -> 0-49%
 */
public class DayScore {
    private static final String[] MEAL_TYPES = {
            Constants.BREAKFAST,
            Constants.LUNCH,
            Constants.TEA,
            Constants.DINNER
    };

    /**
     * Computes the DayStatus for a single day.
     *
     * @param waterSum           total ml of water consumed that day
     * @param dailyGoal          configured daily water goal in ml
     * @param mealCountsByType   map of meal type -> [correct, warning, wrong] counts
     * @param trackerCompletions map of tracker type -> completed flag
     * @param enabledTrackers    set of enabled tracker types (excluding EXPENSES)
     * @return the DayStatus for the day
     */
    public static DayStatus compute(int waterSum, double dailyGoal,
                                    Map<String, int[]> mealCountsByType,
                                    Map<TrackerType, Boolean> trackerCompletions,
                                    java.util.Set<TrackerType> enabledTrackers) {
        if (enabledTrackers == null || enabledTrackers.isEmpty()) {
            return DayStatus.NONE;
        }

        boolean hasAnyData = waterSum > 0
                || (mealCountsByType != null && !mealCountsByType.isEmpty())
                || (trackerCompletions != null && !trackerCompletions.isEmpty());

        // No data at all -> don't color the day
        if (!hasAnyData) {
            return DayStatus.NONE;
        }

        double cardWeight = 1.0 / enabledTrackers.size();
        double totalScore = 0.0;

        for (TrackerType type : enabledTrackers) {
            switch (type) {
                case WATER:
                    double waterScore = dailyGoal > 0 ? Math.min(waterSum / dailyGoal, 1.0) : 0.0;
                    totalScore += waterScore * cardWeight;
                    break;

                case MEALS:
                    totalScore += computeMealsScore(mealCountsByType) * cardWeight;
                    break;

                case WORKOUT:
                case MEDICATION:
                case SUPPLEMENT:
                    boolean completed = trackerCompletions != null
                            && Boolean.TRUE.equals(trackerCompletions.get(type));
                    totalScore += (completed ? 1.0 : 0.0) * cardWeight;
                    break;

                case EXPENSES:
                    // Not included in the score calculation
                    break;
            }
        }

        double percentage = totalScore * 100.0;

        if (percentage >= 80.0) {
            return DayStatus.GREEN;
        } else if (percentage >= 50.0) {
            return DayStatus.YELLOW;
        } else {
            return DayStatus.RED;
        }
    }

    /**
     * Builds a human-readable breakdown of the score calculation for a day,
     * including the final weighted average total.
     */
    public static String getBreakdown(int waterSum, double dailyGoal,
                                      Map<String, int[]> mealCountsByType,
                                      Map<TrackerType, Boolean> trackerCompletions,
                                      java.util.Set<TrackerType> enabledTrackers) {
        if (enabledTrackers == null || enabledTrackers.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        double cardWeight = 1.0 / enabledTrackers.size();
        double totalScore = 0.0;

        for (TrackerType type : enabledTrackers) {
            if (sb.length() > 0) {
                sb.append(", ");
            }

            switch (type) {
                case WATER:
                    double waterPct = dailyGoal > 0
                            ? Math.min(waterSum / dailyGoal, 1.0) * 100.0
                            : 0.0;
                    sb.append("Water ").append(Math.round(waterPct)).append("%");
                    totalScore += (waterPct / 100.0) * cardWeight;
                    break;

                case MEALS:
                    double mealsPct = computeMealsScore(mealCountsByType) * 100.0;
                    sb.append(appendMealsBreakdown(mealCountsByType, cardWeight));
                    totalScore += (mealsPct / 100.0) * cardWeight;
                    break;

                case WORKOUT:
                    boolean workoutDone = trackerCompletions != null
                            && Boolean.TRUE.equals(trackerCompletions.get(TrackerType.WORKOUT));
                    sb.append("Workout ").append(workoutDone ? "100%" : "0%");
                    totalScore += (workoutDone ? 1.0 : 0.0) * cardWeight;
                    break;

                case MEDICATION:
                    boolean medDone = trackerCompletions != null
                            && Boolean.TRUE.equals(trackerCompletions.get(TrackerType.MEDICATION));
                    sb.append("Medication ").append(medDone ? "100%" : "0%");
                    totalScore += (medDone ? 1.0 : 0.0) * cardWeight;
                    break;

                case SUPPLEMENT:
                    boolean suppDone = trackerCompletions != null
                            && Boolean.TRUE.equals(trackerCompletions.get(TrackerType.SUPPLEMENT));
                    sb.append("Supplement ").append(suppDone ? "100%" : "0%");
                    totalScore += (suppDone ? 1.0 : 0.0) * cardWeight;
                    break;

                case EXPENSES:
                    // Not included
                    break;
            }
        }

        double totalPct = totalScore * 100.0;
        sb.append("\nTotal: ").append(Math.round(totalPct)).append("%");

        return sb.toString();
    }

    /**
     * Appends the meals breakdown to the given StringBuilder.
     * Example -> "Meals 40% (Breakfast 10%, Lunch 10%, Tea 10%, Dinner 10%)"
     */
    private static String appendMealsBreakdown(Map<String, int[]> mealCountsByType, double cardWeight) {
        double mealsPct = computeMealsScore(mealCountsByType) * 100.0;
        double mealWeightPct = mealWeightPercentage(cardWeight);

        StringBuilder sb = new StringBuilder();
        sb.append("Meals ").append(Math.round(mealsPct)).append("%");

        boolean hasAnyMeal = false;
        for (String mealType : MEAL_TYPES) {
            int[] counts = mealCountsByType != null ? mealCountsByType.get(mealType) : null;
            if (counts != null) {
                hasAnyMeal = true;
                break;
            }
        }

        if (hasAnyMeal) {
            sb.append(" (");
            boolean first = true;
            for (String mealType : MEAL_TYPES) {
                int[] counts = mealCountsByType.get(mealType);
                if (counts != null) {
                    if (!first) {
                        sb.append(", ");
                    }
                    sb.append(mealType).append(" ").append(Math.round(mealWeightPct)).append("%");
                    first = false;
                }
            }
            sb.append(")");
        }

        return sb.toString();
    }

    /**
     * Converts a card weight into the percentage each meal contributes to the total score.
     * E.g. card weight 0.333 -> each of 4 meals = 8.33%
     */
    private static double mealWeightPercentage(double cardWeight) {
        return cardWeight / MEAL_TYPES.length * 100.0;
    }

    /**
     * Computes the meals sub-score (0.0 to 1.0).
     * The 4 meals (Breakfast, Lunch, Tea, Dinner) each contribute 25% of the meals score.
     * A meal is scored by its best status: correct = 1.0, warning = 0.5, wrong = 0.0.
     */
    private static double computeMealsScore(Map<String, int[]> mealCountsByType) {
        if (mealCountsByType == null || mealCountsByType.isEmpty()) {
            return 0.0;
        }

        double mealWeight = 1.0 / MEAL_TYPES.length;
        double mealsScore = 0.0;

        for (String mealType : MEAL_TYPES) {
            int[] counts = mealCountsByType.get(mealType);
            if (counts == null) {
                continue; // meal not logged -> no contribution
            }
            double mealScore;
            if (counts[0] > 0) {
                mealScore = 1.0;   // correct meal
            } else if (counts[1] > 0) {
                mealScore = 0.5;   // warning meal
            } else {
                mealScore = 0.0;   // wrong meal
            }
            mealsScore += mealScore * mealWeight;
        }
        return mealsScore;
    }
}