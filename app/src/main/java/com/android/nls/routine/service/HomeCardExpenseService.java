package com.android.nls.routine.service;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import com.android.nls.routine.model.CardSpending;
import com.android.nls.routine.model.CreditCard;
import com.android.nls.routine.model.Expense;
import com.android.nls.routine.model.ExpenseCardSummary;
import com.android.nls.routine.model.ExpenseRecord;
import com.android.nls.routine.repository.CardRepository;
import com.android.nls.routine.repository.ConfigRepository;
import com.android.nls.routine.repository.ExpenseRepository;
import com.android.nls.routine.utils.Common;
import com.android.nls.routine.utils.Constants;
import java.util.ArrayList;
import java.util.List;

public class HomeCardExpenseService {
    private static final String TAG = Common.generateTag(HomeCardExpenseService.class);
    private final ExpenseRepository mExpenseRepository;
    private final ConfigRepository mConfigRepository;
    private final CardRepository mCardRepository;
    private final Context mContext;

    public HomeCardExpenseService(Context context) {
        mContext = context;
        mExpenseRepository = new ExpenseRepository(mContext);
        mConfigRepository = new ConfigRepository(mContext);
        mCardRepository = new CardRepository(mContext);
    }

    public void saveExpenseTest(Expense expense) {
        mExpenseRepository.insertExpense(expense);
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

    /**
     * Everything the expense card needs: the spending of each configured card,
     * measured inside the card's own statement cycle, and the total spent (the
     * sum of the cards).
     * When no card is configured, every expense of the cycle is summed instead
     * and no per-card row is returned.
     */
    public ExpenseCardSummary getExpenseCardSummary() {
        List<CardSpending> cardSpendings = getCardSpendings();

        if (cardSpendings.isEmpty()) {
            double closingDay = parseClosingDay(mConfigRepository.getCardStatementClosingDate());
            long startOfCycle = Common.getStartOfExpenseCycleInMillis(closingDay);
            double totalSpent = mExpenseRepository.getTotalSpent(startOfCycle, System.currentTimeMillis());
            return new ExpenseCardSummary(cardSpendings, totalSpent);
        }

        double totalSpent = 0.0;
        for (CardSpending cardSpending : cardSpendings) {
            totalSpent += cardSpending.spent();
        }
        return new ExpenseCardSummary(cardSpendings, totalSpent);
    }

    /**
     * Returns the spending of every configured card, each one measured inside
     * its own statement cycle (the cycle that started on the card closing day).
     */
    private List<CardSpending> getCardSpendings() {
        List<CreditCard> cards = mCardRepository.getCards();
        List<CardSpending> cardSpendings = new ArrayList<>();

        if (cards.isEmpty()) {
            return cardSpendings;
        }

        double monthlyLimit = getMonthlyLimitValue();
        long now = System.currentTimeMillis();

        for (CreditCard card : cards) {
            long startOfCycle = Common.getStartOfExpenseCycleInMillis(card.closingDay());
            double spent = mExpenseRepository.getSpentByBank(resolveBankFilter(card), startOfCycle, now);
            cardSpendings.add(new CardSpending(card.bankName(), card.lastFour(), spent,
                    calculateProgress(spent, monthlyLimit)));
        }

        return cardSpendings;
    }

    /**
     * Resolves the bank value used to match a card against the stored expenses.
     * The card bank name is typed by the user ("XP BANK") while the expense
     * stores a short bank name ("xp"), so the known banks are looked up first
     * and the typed name is used as a fallback.
     */
    private String resolveBankFilter(CreditCard card) {
        String normalizedBankName = card.bankName().toLowerCase().replaceAll("[^a-z0-9]", "");

        for (String bank : Constants.KNOWN_BANKS) {
            if (normalizedBankName.contains(bank)) {
                return bank;
            }
        }

        return card.bankName();
    }

    /**
     * Percentage of the monthly limit spent, capped at 100.
     */
    private int calculateProgress(double spent, double monthlyLimit) {
        if (monthlyLimit <= 0) {
            return 0;
        }
        return (int) Math.min(100L, Math.round(spent * 100.0 / monthlyLimit));
    }

    public ExpenseRecord getLastExpenseRecord(){
        return mExpenseRepository.getLastExpenseRecord();
    }

    /**
     * Parses the saved closing day (e.g. "05" or "01/xx") into an int.
     * Falls back to 1 if the value is missing or invalid.
     */
    private int parseClosingDay(double value) {
        String valueToString = String.valueOf(value);
        try {
            // The closing day is stored either as a plain day ("8") or as the
            // legacy "08/xx" value, and it is read back as a double ("8.0")
            String firstPart = valueToString.split("/")[0].trim();
            int day = (int) Double.parseDouble(firstPart);
            if (day >= 1 && day <= 31) {
                return day;
            }
        } catch (NumberFormatException ignored) {
        }
        return 1;
    }

    public double getMonthlyLimitValue() {
        return mConfigRepository.getMonthlyLimitValue();
    }

    public void closeDb() {
        mExpenseRepository.closeDb();
        mConfigRepository.closeDb();
        mCardRepository.closeDb();
    }
}