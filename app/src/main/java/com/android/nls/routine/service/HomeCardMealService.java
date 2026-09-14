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
import com.android.nls.routine.repository.MealRepository;
import com.android.nls.routine.utils.Common;
import com.android.nls.routine.utils.Constants;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.Calendar;

public class HomeCardMealService {
    private static final String TAG = Common.generateTag(HomeCardMealService.class);
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
     * Returns the meal button matching the current time of day:
     * Breakfast (5h-10h), Lunch (11h-15h), Tea (16h-19h), Dinner otherwise.
     */
    private RadioButton getDefaultMealButton(RadioButton rbBreakfast, RadioButton rbLunch,
                                             RadioButton rbTea, RadioButton rbDinner) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        if (hour >= 5 && hour <= 10) {
            return rbBreakfast;
        } else if (hour >= 11 && hour <= 15) {
            return rbLunch;
        } else if (hour >= 16 && hour < 20) {
            return rbTea;
        }
        return rbDinner;
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

    public void closeDb() {
        mMealRepository.closeDb();
    }
}