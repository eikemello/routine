package com.android.nls.routine.service;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
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
import com.android.nls.routine.activity.ConfigActivity;
import com.android.nls.routine.model.CreditCard;
import com.android.nls.routine.repository.CardRepository;
import com.android.nls.routine.repository.ConfigRepository;
import com.android.nls.routine.utils.Common;
import com.android.nls.routine.utils.Constants;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ConfigService {
    private static final String TAG = Common.generateTag(ConfigActivity.class);
    private final Context mContext;
    private final ConfigRepository mConfigRepository;
    private final CardRepository mCardRepository;

    public ConfigService(Context context) {
        mContext = context;
        mConfigRepository = new ConfigRepository(mContext);
        mCardRepository = new CardRepository(mContext);
    }

    public void showAlertDialog(String buttonClicked, TextView textView) {
        showAlertDialog(buttonClicked, textView, null);
    }

    /**
     * Same dialog, notifying {@code onSaved} once the value is stored. The water
     * widget reads the goal and the three button values from the config, so it
     * is repainted through this handle after a change.
     */
    public void showAlertDialog(String buttonClicked, TextView textView, Runnable onSaved) {
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
            default:
                return;
        }
        showSaveDialog(title, view, etValue, saveAction, maxLength, onSaved);
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

    private void showSaveDialog(String title, View view, TextInputEditText etValue, Consumer<String> saveAction,
                                int maxLength, Runnable onSaved) {
        TextInputLayout txtInputError = view.findViewById(R.id.txtInputError);
        txtInputError.setError(null);

        AlertDialog dialog = new MaterialAlertDialogBuilder(mContext)
                .setTitle(title)
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
            if (checkFieldValue(value, maxLength)) {
                saveAction.accept(value.toString().trim());
                dialog.dismiss();
                if (onSaved != null) {
                    onSaved.run();
                }
            } else {
                setFieldError(txtInputError);
            }
        });
    }

    private void saveConfigValue(String columnName, String value, TextView textView, int stringResId) {
        Log.d(TAG, "saveConfigValue: " + columnName + " = " + value);
        mConfigRepository.saveConfigValue(columnName, value);
        textView.setText(mContext.getString(stringResId, Double.parseDouble(value)));
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

    /**
     * Opens the dialog used to manage the user's credit cards. Each card block
     * holds the bank name, the last four digits and the statement closing day,
     * and the user can append as many cards as needed.
     */
    public void showCardsDialog(Runnable onSaved) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.dialog_cards_config, new FrameLayout(mContext), false);
        LinearLayout cardsContainer = view.findViewById(R.id.cardsContainer);
        MaterialButton btnAddAnotherCard = view.findViewById(R.id.btnAddAnotherCard);

        for (CreditCard card : getCards()) {
            addCardBlock(cardsContainer, card);
        }
        if (cardsContainer.getChildCount() == 0) {
            // Start with one empty block so the dialog can be used right away
            addCardBlock(cardsContainer, null);
        }

        btnAddAnotherCard.setOnClickListener(v -> addCardBlock(cardsContainer, null));

        AlertDialog dialog = new MaterialAlertDialogBuilder(mContext)
                .setTitle(R.string.card_statement_closing)
                .setView(view)
                .setPositiveButton(mContext.getString(R.string.save_label), null)
                .setNegativeButton(mContext.getString(R.string.cancel_label), null)
                .setCancelable(true)
                .show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(AppCompatResources.getDrawable(mContext, R.drawable.dialog_background));
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            List<CreditCard> cards = readCards(cardsContainer);
            if (cards == null) {
                return;
            }
            mCardRepository.saveCards(cards);
            syncExpenseCycleClosingDay(cards);
            dialog.dismiss();
            if (onSaved != null) {
                onSaved.run();
            }
        });
    }

    public List<CreditCard> getCards() {
        return mCardRepository.getCards();
    }

    /**
     * Inflates one card block, prefilled when a card is given (null adds a
     * clean block).
     */
    private void addCardBlock(LinearLayout cardsContainer, CreditCard card) {
        View block = LayoutInflater.from(mContext).inflate(R.layout.item_card_config, cardsContainer, false);

        if (card != null) {
            TextInputEditText etBank = block.findViewById(R.id.etCardBank);
            TextInputEditText etLastFour = block.findViewById(R.id.etCardLastFour);
            TextInputEditText etClosingDay = block.findViewById(R.id.etCardClosingDay);
            etBank.setText(card.bankName());
            etLastFour.setText(card.lastFour());
            etClosingDay.setText(String.valueOf(card.closingDay()));
        }

        block.findViewById(R.id.btnRemoveCard).setOnClickListener(v -> {
            cardsContainer.removeView(block);
            renumberCardBlocks(cardsContainer);
        });

        cardsContainer.addView(block);
        renumberCardBlocks(cardsContainer);
    }

    private void renumberCardBlocks(LinearLayout cardsContainer) {
        for (int i = 0; i < cardsContainer.getChildCount(); i++) {
            TextView txtCardBlockTitle = cardsContainer.getChildAt(i).findViewById(R.id.txtCardBlockTitle);
            txtCardBlockTitle.setText(mContext.getString(R.string.card_number, i + 1));
        }
    }

    /**
     * Reads and validates every card block.
     * @return the cards to save, or null when a field is invalid
     *         (the error is shown on the offending field)
     */
    private List<CreditCard> readCards(LinearLayout cardsContainer) {
        List<CreditCard> cards = new ArrayList<>();

        for (int i = 0; i < cardsContainer.getChildCount(); i++) {
            View block = cardsContainer.getChildAt(i);

            String bankName = getCardFieldValue(block, R.id.etCardBank);
            String lastFour = getCardFieldValue(block, R.id.etCardLastFour);
            String closingDayValue = getCardFieldValue(block, R.id.etCardClosingDay);

            if (bankName.isEmpty()) {
                setCardFieldError(block, R.id.txtInputCardBank, Constants.CARD_BANK_INVALID_TEXT);
                return null;
            }
            if (!lastFour.matches("\\d{4}")) {
                setCardFieldError(block, R.id.txtInputCardLastFour, Constants.CARD_LAST_FOUR_INVALID_TEXT);
                return null;
            }

            int closingDay = parseClosingDay(closingDayValue);
            if (closingDay == -1) {
                setCardFieldError(block, R.id.txtInputCardClosingDay, Constants.CARD_CLOSING_DAY_INVALID_TEXT);
                return null;
            }

            cards.add(new CreditCard(bankName, lastFour, closingDay));
        }

        return cards;
    }

    private String getCardFieldValue(View block, int editTextId) {
        Editable value = ((TextInputEditText) block.findViewById(editTextId)).getText();
        return value != null ? value.toString().trim() : "";
    }

    private void setCardFieldError(View block, int textInputLayoutId, String error) {
        TextInputLayout txtInputError = block.findViewById(textInputLayoutId);
        txtInputError.setError(error);
    }

    /**
     * Validates the closing day typed by the user.
     * @return the day (1-31), or -1 when the value is not valid
     */
    private int parseClosingDay(String value) {
        try {
            int day = Integer.parseInt(value);
            return (day >= 1 && day <= 31) ? day : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * The expense cycle (spent/limit on the home card) still runs from a single
     * closing day, so it is kept in sync with the configured cards: the earliest
     * closing day, or the default value when no card is configured.
     */
    private void syncExpenseCycleClosingDay(List<CreditCard> cards) {
        int closingDay = -1;
        for (CreditCard card : cards) {
            if (closingDay == -1 || card.closingDay() < closingDay) {
                closingDay = card.closingDay();
            }
        }
        if (closingDay == -1) {
            closingDay = (int) Constants.DEFAULT_CARD_STATEMENT_CLOSING;
        }
        mConfigRepository.saveConfigValue(Constants.COLUMN_NAME_CARD_STATEMENT_CLOSING, String.valueOf(closingDay));
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
        mCardRepository.closeDb();
    }
}