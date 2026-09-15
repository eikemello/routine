package com.android.nls.routine.model;

import java.util.List;

/**
 * Everything the expense card needs to be rendered: the spending of each
 * configured card (one progress bar each) and the total spent, which is the sum
 * of them.
 */
public record ExpenseCardSummary(List<CardSpending> cardSpendings, double totalSpent) {

}