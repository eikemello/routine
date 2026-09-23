package com.android.nls.routine.service;

import android.content.Context;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import com.android.nls.routine.R;
import com.android.nls.routine.cardhistory.CardHistory;
import com.android.nls.routine.cardhistory.CardHistoryDialog;
import com.android.nls.routine.cardhistory.CardRecordDialog;
import com.android.nls.routine.model.MealRecord;
import com.android.nls.routine.repository.MealRepository;
import com.android.nls.routine.utils.Common;
import com.android.nls.routine.utils.Constants;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HomeCardMealService implements CardHistory {
    private static final String TAG = Common.generateTag(HomeCardMealService.class);

    /** The four regular daily slots, in the order the widget shows them. */
    private static final String[] REGULAR_MEALS = {
            Constants.BREAKFAST, Constants.LUNCH, Constants.TEA, Constants.DINNER
    };
    private final Context mContext;
    private final MealRepository mMealRepository;

    public HomeCardMealService(Context context) {
        mContext = context;
        mMealRepository = new MealRepository(mContext);
    }

    public void showAlertDialog(String buttonClicked, Runnable onSaved) {
        String title;
        switch (buttonClicked) {
            case Constants.CORRECT_MEAL:
                title = mContext.getString(R.string.correct_meal);
                break;
            case Constants.WARNING_MEAL:
                title = mContext.getString(R.string.warning_meal);
                break;
            case Constants.WRONG_MEAL:
                title = mContext.getString(R.string.wrong_meal);
                break;
            default:
                return;
        }

        View view = LayoutInflater.from(mContext).inflate(R.layout.dialog_meal, new FrameLayout(mContext), false);
        setupMealSelection(view);
        setupDifferentMealInput(view);

        showSaveDialog(title, view, buttonClicked, onSaved);
    }

    /**
     * Logs a meal straight from the widget, without the dialog: the slot is
     * the one matching the current time of day (the same the dialog
     * pre-selects) and the status is the widget button that was tapped.
     * A regular meal is unique per day, so a slot already logged today is
     * updated in place - its observation is kept, since there is no dialog to
     * ask the update the way the home card does.
     */
    public void saveQuickMeal(String mealStatus) {
        String meal = getDefaultMealName();
        long existingMealId = mMealRepository.getMealIdForToday(meal);

        if (existingMealId != -1) {
            String observation = "";
            for (MealRecord record : mMealRepository.getMealRecords(
                    Common.getStartOfDayInMillis(), Common.getEndOfDayInMillis())) {
                if (record.id() == existingMealId) {
                    observation = record.observation() == null ? "" : record.observation();
                    break;
                }
            }
            mMealRepository.updateMeal(existingMealId, mealStatus, observation, System.currentTimeMillis());
        } else {
            mMealRepository.insertMeal(meal, mealStatus, "", System.currentTimeMillis());
        }

        Log.d(TAG, "Quick meal " + meal + " saved as " + mealStatus);
    }

    /**
     * Status of each of the four regular meal slots for today (Breakfast,
     * Lunch, Tea and Dinner, in that order), null when the slot was not logged
     * yet. Irregular ("different") meals are ignored, the same rule the home
     * progress uses: they do not count toward the 4 regular slots. Used by the
     * widget to paint its four segments and the day's "x/4" count.
     */
    public String[] getLoggedMealStatusesToday() {
        String[] statuses = new String[REGULAR_MEALS.length];

        for (MealRecord record : mMealRepository.getMealRecords(
                Common.getStartOfDayInMillis(), Common.getEndOfDayInMillis())) {
            if (Constants.OTHER_MEAL.equals(record.status())) {
                continue; // irregular meals don't count toward the 4 regular ones
            }

            for (int i = 0; i < REGULAR_MEALS.length; i++) {
                if (REGULAR_MEALS[i].equals(record.meal())) {
                    // Records come ordered by timestamp and a regular slot is
                    // unique per day, so the last match is the current one
                    statuses[i] = record.status();
                    break;
                }
            }
        }

        return statuses;
    }

    private void saveMealWithCheck(String mealStatus, String currentMeal, String value, Runnable onSaved) {
        if (mMealRepository.hasMealForToday(currentMeal)) {
            // Meal already exists for today - get the existing meal ID and show confirmation dialog
            long existingMealId = mMealRepository.getMealIdForToday(currentMeal);
            if (existingMealId != -1) {
                new MaterialAlertDialogBuilder(mContext)
                        .setTitle(R.string.meal_exists_title)
                        .setMessage(getString(R.string.meal_exists_message, currentMeal))
                        .setPositiveButton(R.string.update_label, (dialog, which) -> {
                            dialog.dismiss();
                            // Update the existing meal instead of inserting a new one
                            saveMealValue(mealStatus, currentMeal, value, existingMealId);
                            if (onSaved != null) {
                                onSaved.run();
                            }
                        })
                        .setNegativeButton(R.string.cancel_label, (dialog, which) -> {
                            dialog.dismiss();
                        })
                        .setCancelable(true)
                        .show()
                        .getWindow().setBackgroundDrawable(AppCompatResources.getDrawable(mContext, R.drawable.dialog_background));
            } else {
                // Should not happen, but if ID not found, insert as new
                saveMealValue(mealStatus, currentMeal, value, -1);
                if (onSaved != null) {
                    onSaved.run();
                }
            }
        } else {
            // No existing meal - save directly as new
            saveMealValue(mealStatus, currentMeal, value, -1);
            if (onSaved != null) {
                onSaved.run();
            }
        }
    }

    private String getString(int resId, Object... formatArgs) {
        return mContext.getString(resId, formatArgs);
    }


    private void showSaveDialog(String title, View view, String mealStatus, Runnable onSaved) {
        TextInputLayout txtInputError = view.findViewById(R.id.txtInputError);
        TextInputEditText etValue = view.findViewById(R.id.etMealObservation);
        TextInputLayout txtInputDifferentMeal = view.findViewById(R.id.txtInputDifferentMeal);
        TextInputEditText etDifferentMealName = view.findViewById(R.id.etDifferentMealName);

        txtInputError.setError(null);
        txtInputDifferentMeal.setError(null);

        AlertDialog dialog = new MaterialAlertDialogBuilder(mContext)
                .setTitle(title)
                .setView(view)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .setCancelable(true)
                .show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(AppCompatResources.getDrawable(mContext, R.drawable.dialog_background));
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            Editable value = etValue.getText();
            
            String observation = value != null ? value.toString().trim() : "";

            if (isDifferentMealVisible(view)) {
                // Different meal - the typed name is stored as the meal and the
                // clicked button (correct/warning/wrong) as its status. Both the
                // name and the observation are required for irregular meals.
                Editable differentMealName = etDifferentMealName.getText();
                String mealName = differentMealName != null ? differentMealName.toString().trim() : "";

                if (mealName.isEmpty()) {
                    txtInputError.setError(null);
                    txtInputDifferentMeal.setError(Constants.CUSTOM_MEAL_INVALID_TEXT);
                    return;
                }
                if (observation.isEmpty()) {
                    txtInputDifferentMeal.setError(null);
                    txtInputError.setError(Constants.CUSTOM_MEAL_OBSERVATION_REQUIRED);
                    return;
                }
                saveMealWithCheck(mealStatus, mealName, observation, onSaved);
            } else {
                String selectedMeal = getSelectedMeal(view);
                if (selectedMeal == null) {
                    // No meal selected - an option must be chosen to save
                    txtInputError.setError(Constants.MEAL_SELECTION_REQUIRED);
                    return;
                }
                saveMealWithCheck(mealStatus, selectedMeal, observation, onSaved);
            }

            dialog.dismiss();

            if (onSaved != null) {
                onSaved.run();
            }
        });
    }

    private void saveMealValue(String mealStatus, String currentMeal, String value, long existingMealId) {
        Log.d(TAG, "saveMealValue: " + mealStatus + " = " + value);
        
        if (existingMealId != -1) {
            // Update existing meal
            int rowsUpdated = mMealRepository.updateMeal(existingMealId, mealStatus, value, System.currentTimeMillis());
            Log.d(TAG, "Updated meal row ID: " + existingMealId + ", rows updated: " + rowsUpdated);
            
            if (rowsUpdated == 0) {
                Log.e(TAG, "Failed to update meal with ID: " + existingMealId);
            }
        } else {
            // Insert new meal
            long result = mMealRepository.insertMeal(currentMeal, mealStatus, value, System.currentTimeMillis());
            Log.d(TAG, "Inserted row ID: " + result);
            
            if (result == -1) {
                Log.e(TAG, "Failed to save meal " + mealStatus);
            }
        }
    }

    /**
     * Sets up the 2x2 meal selection. The buttons live in a GridLayout instead
     * of a RadioGroup (a RadioGroup is a LinearLayout, so it cannot place its
     * buttons in two rows), which means the mutual exclusivity is enforced
     * here. The meal matching the current time of day is selected by default.
     */
    private void setupMealSelection(View view) {
        RadioButton rbBreakfast = view.findViewById(R.id.rbBreakfast);
        RadioButton rbLunch = view.findViewById(R.id.rbLunch);
        RadioButton rbTea = view.findViewById(R.id.rbTea);
        RadioButton rbDinner = view.findViewById(R.id.rbDinner);
        RadioButton[] mealButtons = {rbBreakfast, rbLunch, rbTea, rbDinner};

        getDefaultMealButton(rbBreakfast, rbLunch, rbTea, rbDinner).setChecked(true);

        for (RadioButton button : mealButtons) {
            // Listeners are attached after the default selection is applied so
            // they only react to user interaction. Checked state changes are
            // used instead of click listeners because CompoundButton toggles
            // the button BEFORE invoking an OnClickListener.
            button.setOnCheckedChangeListener((checkedButton, isChecked) -> {
                if (!isChecked) {
                    return;
                }
                for (RadioButton other : mealButtons) {
                    if (other != checkedButton) {
                        other.setChecked(false);
                    }
                }
                // A regular meal was picked - the different meal input is not needed
                setDifferentMealVisible(view, false);
            });
        }
    }

    /**
     * The meal slot matching the current time of day: Breakfast (5h-10h),
     * Lunch (11h-15h), Tea (16h-19h), Dinner otherwise. The meal dialog
     * pre-selects it and the widget quick buttons log into it directly.
     */
    public String getDefaultMealName() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        if (hour >= 5 && hour <= 10) {
            return Constants.BREAKFAST;
        } else if (hour >= 11 && hour <= 15) {
            return Constants.LUNCH;
        } else if (hour >= 16 && hour < 20) {
            return Constants.TEA;
        }
        return Constants.DINNER;
    }

    private RadioButton getDefaultMealButton(RadioButton rbBreakfast, RadioButton rbLunch,
                                             RadioButton rbTea, RadioButton rbDinner) {
        return switch (getDefaultMealName()) {
            case Constants.BREAKFAST -> rbBreakfast;
            case Constants.LUNCH -> rbLunch;
            case Constants.TEA -> rbTea;
            default -> rbDinner;
        };
    }

    /**
     * Sets up the "different meal" button. Pressing it clears the regular meal
     * selection and reveals a text input where the user can name a meal that is
     * not one of the four regular ones. Pressing it again closes the input.
     */
    private void setupDifferentMealInput(View view) {
        MaterialButton btnAddDifferentMeal = view.findViewById(R.id.btnAddDifferentMeal);

        btnAddDifferentMeal.setOnClickListener(v -> {
            boolean showInput = !isDifferentMealVisible(view);
            if (showInput) {
                clearMealSelection(view);
            }
            setDifferentMealVisible(view, showInput);
        });
    }

    private boolean isDifferentMealVisible(View view) {
        return view.findViewById(R.id.txtInputDifferentMeal).getVisibility() == View.VISIBLE;
    }

    private void setDifferentMealVisible(View view, boolean visible) {
        TextInputLayout txtInputDifferentMeal = view.findViewById(R.id.txtInputDifferentMeal);
        txtInputDifferentMeal.setError(null);
        txtInputDifferentMeal.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void clearMealSelection(View view) {
        ((RadioButton) view.findViewById(R.id.rbBreakfast)).setChecked(false);
        ((RadioButton) view.findViewById(R.id.rbLunch)).setChecked(false);
        ((RadioButton) view.findViewById(R.id.rbTea)).setChecked(false);
        ((RadioButton) view.findViewById(R.id.rbDinner)).setChecked(false);
    }

    /**
     * Returns the meal type of the checked meal button, or null when none is selected.
     */
    private String getSelectedMeal(View view) {
        if (((RadioButton) view.findViewById(R.id.rbBreakfast)).isChecked()) {
            return Constants.BREAKFAST;
        }
        if (((RadioButton) view.findViewById(R.id.rbLunch)).isChecked()) {
            return Constants.LUNCH;
        }
        if (((RadioButton) view.findViewById(R.id.rbTea)).isChecked()) {
            return Constants.TEA;
        }
        if (((RadioButton) view.findViewById(R.id.rbDinner)).isChecked()) {
            return Constants.DINNER;
        }
        return null;
    }

    public void showDailyHistoryDialog(Runnable onChanged) {
        new CardHistoryDialog(mContext, this).show(onChanged);
    }

    @Override
    public CardHistory.Panel getHistoryPanel(Runnable refresh) {
        List<CardHistory.Row> rows = new ArrayList<>();
        List<MealRecord> records = getDailyMealRecords();
        int correct = 0;
        int warning = 0;
        int wrong = 0;

        // The records are stored oldest first, so the panel walks them from
        // the end: the meals the user edits or removes are the latest ones
        for (int i = records.size() - 1; i >= 0; i--) {
            MealRecord record = records.get(i);
            switch (record.status()) {
                case Constants.CORRECT_MEAL -> correct++;
                case Constants.WARNING_MEAL -> warning++;
                case Constants.WRONG_MEAL -> wrong++;
            }

            rows.add(new CardHistory.Row(
                    Common.getHourFromTimestamp(String.valueOf(record.timestamp())),
                    mealDisplayName(record),
                    statusLabel(record.status()),
                    mealNote(record),
                    () -> showEditRecordDialog(record, refresh),
                    () -> confirmDeleteRecord(record, refresh)));
        }

        return new CardHistory.Panel(rows,
                mContext.getString(R.string.meal_history_sum, correct, warning, wrong));
    }

    @Override
    public int getHistoryTitleRes() {
        return R.string.meal_history;
    }

    @Override
    public int getHistoryEmptyTextRes() {
        return R.string.no_meals_today;
    }

    @Override
    public int getHistoryIconRes() {
        return R.drawable.ic_meal;
    }

    @Override
    public int getHistoryIconTintRes() {
        return R.color.neon_green_40;
    }

    @Override
    public int getHistoryValueColorRes() {
        return R.color.green_dark;
    }

    public List<MealRecord> getDailyMealRecords() {
        long startOfDay = Common.getStartOfDayInMillis();
        long endOfDay = Common.getEndOfDayInMillis();
        return mMealRepository.getMealRecords(startOfDay, endOfDay);
    }

    private String mealDisplayName(MealRecord record) {
        return switch (record.meal()) {
            case Constants.BREAKFAST -> mContext.getString(R.string.breakfast);
            case Constants.LUNCH -> mContext.getString(R.string.lunch);
            case Constants.TEA -> mContext.getString(R.string.tea);
            case Constants.DINNER -> mContext.getString(R.string.dinner);
            default -> record.meal();
        };
    }

    /** The observation of a meal, shown wrapping under its status when there is one. */
    private String mealNote(MealRecord record) {
        String observation = record.observation();

        if (observation != null && !observation.isBlank()) {
            return observation;
        }
        return null;
    }

    private String statusLabel(String status) {
        return switch (status) {
            case Constants.CORRECT_MEAL -> mContext.getString(R.string.meal_correct_short);
            case Constants.WARNING_MEAL -> mContext.getString(R.string.meal_warning_short);
            case Constants.WRONG_MEAL -> mContext.getString(R.string.meal_wrong_short);
            default -> status;
        };
    }

    private int statusIndex(String status) {
        if (Constants.WARNING_MEAL.equals(status)) {
            return 1;
        }
        if (Constants.WRONG_MEAL.equals(status)) {
            return 2;
        }
        return 0;
    }

    private String statusFromLabel(String label) {
        if (mContext.getString(R.string.meal_warning_short).equals(label)) {
            return Constants.WARNING_MEAL;
        }
        if (mContext.getString(R.string.meal_wrong_short).equals(label)) {
            return Constants.WRONG_MEAL;
        }
        return Constants.CORRECT_MEAL;
    }

    /**
     * A regular meal is one of the four daily slots; any other meal name is an
     * irregular ("different") meal, whose observation is required.
     */
    private boolean isRegularMeal(String meal) {
        return Constants.BREAKFAST.equals(meal) || Constants.LUNCH.equals(meal)
                || Constants.TEA.equals(meal) || Constants.DINNER.equals(meal);
    }

    private void showEditRecordDialog(MealRecord record, Runnable refresh) {
        List<String> options = List.of(
                mContext.getString(R.string.meal_correct_short),
                mContext.getString(R.string.meal_warning_short),
                mContext.getString(R.string.meal_wrong_short));

        CardRecordDialog.showEditOptionTextDialog(
                mContext,
                R.string.meal_record_edit_title,
                options,
                statusIndex(record.status()),
                R.string.add_an_observation,
                record.observation() == null ? "" : record.observation(),
                (statusLabel, observation) -> {
                    if (statusLabel == null) {
                        return Constants.MEAL_SELECTION_REQUIRED;
                    }

                    // Irregular meals keep the observation required, the same
                    // rule the save dialog applies when they are logged
                    if (!isRegularMeal(record.meal()) && observation.isEmpty()) {
                        return Constants.CUSTOM_MEAL_OBSERVATION_REQUIRED;
                    }

                    // The timestamp is kept, so the meal stays in its place of
                    // the list after the edit
                    mMealRepository.updateMeal(record.id(), statusFromLabel(statusLabel),
                            observation, record.timestamp());
                    refresh.run();
                    return null;
                });
    }

    private void confirmDeleteRecord(MealRecord record, Runnable refresh) {
        CardRecordDialog.confirmRemove(
                mContext,
                R.string.meal_record_delete_title,
                mContext.getString(R.string.meal_record_delete_message,
                        mealDisplayName(record),
                        Common.getHourFromTimestamp(String.valueOf(record.timestamp()))),
                () -> {
                    mMealRepository.deleteMeal(record.id());
                    refresh.run();
                });
    }

    public void closeDb() {
        mMealRepository.closeDb();
    }
}