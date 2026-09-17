package com.android.nls.routine.service;

import android.content.Context;
import android.text.InputType;
import com.android.nls.routine.R;
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

public class HomeCardExpenseService implements CardHistory {
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

    public ExpenseCardSummary getExpenseCardSummary() {
        List<CardSpending> cardSpending = getCardSpending();

        if (cardSpending.isEmpty()) {
            double closingDay = parseClosingDay(mConfigRepository.getCardStatementClosingDate());
            long startOfCycle = Common.getStartOfExpenseCycleInMillis(closingDay);
            double totalSpent = mExpenseRepository.getTotalSpent(startOfCycle, System.currentTimeMillis());
            return new ExpenseCardSummary(cardSpending, totalSpent);
        }

        double totalSpent = 0.0;
        for (CardSpending spending : cardSpending) {
            totalSpent += spending.spent();
        }
        return new ExpenseCardSummary(cardSpending, totalSpent);
    }

    /**
     * Returns the spending of every configured card, each one measured inside
     * its own statement cycle (the cycle that started on the card closing day).
     */
    private List<CardSpending> getCardSpending() {
        List<CreditCard> cards = mCardRepository.getCards();
        List<CardSpending> cardSpending = new ArrayList<>();

        if (cards.isEmpty()) {
            return cardSpending;
        }

        double monthlyLimit = getMonthlyLimitValue();
        long now = System.currentTimeMillis();

        for (CreditCard card : cards) {
            long startOfCycle = Common.getStartOfExpenseCycleInMillis(card.closingDay());
            double spent = mExpenseRepository.getSpentByBank(resolveBankFilter(card), startOfCycle, now);
            cardSpending.add(new CardSpending(
                    card.bankName(), card.lastFour(), spent, calculateProgress(spent, monthlyLimit), monthlyLimit));
        }

        return cardSpending;
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

    public ExpenseRecord getLastExpenseRecord() {
        return mExpenseRepository.getLastExpenseRecord();
    }

    public void showDailyHistoryDialog(Runnable onChanged) {
        new CardHistoryDialog(mContext, this).show(onChanged);
    }

    @Override
    public CardHistory.Panel getHistoryPanel(Runnable refresh) {
        List<CardHistory.Row> rows = new ArrayList<>();
        List<ExpenseRecord> records = getDailyExpenseRecords();
        double total = 0.0;

        for (int i = records.size() - 1; i >= 0; i--) {
            ExpenseRecord record = records.get(i);
            total += record.amount();
            rows.add(new CardHistory.Row(
                    Common.getHourFromTimestamp(String.valueOf(record.timestamp())),
                    mContext.getString(R.string.expense_record_amount, record.amount()),
                    record.bank(),
                    () -> showEditRecordDialog(record, refresh),
                    () -> confirmDeleteRecord(record, refresh)));
        }

        return new CardHistory.Panel(rows, mContext.getString(R.string.expense_sum, total));
    }

    private void showEditRecordDialog(ExpenseRecord record, Runnable refresh) {
        CardRecordDialog.showEditAmountDialog(
                mContext,
                R.string.expense_record_edit_title,
                R.string.expense_record_amount_hint,
                String.valueOf(record.amount()),
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL,
                amount -> {
                    double value = parseAmount(amount);
                    if (value <= 0) {
                        return Constants.EXPENSE_INVALID_VALUE;
                    }

                    mExpenseRepository.updateExpense(record.id(), value);
                    refresh.run();
                    return null;
                });
    }

    private void confirmDeleteRecord(ExpenseRecord record, Runnable refresh) {
        CardRecordDialog.confirmRemove(
                mContext,
                R.string.expense_record_delete_title,
                mContext.getString(R.string.expense_record_delete_message, record.amount(),
                        Common.getHourFromTimestamp(String.valueOf(record.timestamp()))),
                () -> {
                    mExpenseRepository.deleteExpense(record.id());
                    refresh.run();
                });
    }

    private double parseAmount(String amount) {
        if (!amount.matches("\\d{1,7}([.,]\\d{1,2})?")) {
            return -1;
        }

        return Double.parseDouble(amount.replace(',', '.'));
    }

    public List<ExpenseRecord> getDailyExpenseRecords() {
        long startOfDay = Common.getStartOfDayInMillis();
        long endOfDay = Common.getEndOfDayInMillis();
        return mExpenseRepository.getExpenseRecords(startOfDay, endOfDay);
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

    @Override
    public int getHistoryTitleRes() {
        return R.string.expense_history;
    }

    @Override
    public int getHistoryEmptyTextRes() {
        return R.string.no_expenses_today;
    }

    @Override
    public int getHistoryIconRes() {
        return R.drawable.ic_credit_card;
    }

    @Override
    public int getHistoryIconTintRes() {
        return R.color.neon_orange_40;
    }

    @Override
    public int getHistoryValueColorRes() {
        return R.color.yellow_dark;
    }

    public void closeDb() {
        mExpenseRepository.closeDb();
        mConfigRepository.closeDb();
        mCardRepository.closeDb();
    }
}