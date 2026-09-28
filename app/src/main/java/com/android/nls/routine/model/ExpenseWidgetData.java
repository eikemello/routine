package com.android.nls.routine.model;

/**
 * Data transfer object containing processed information for the expense widget.
 *
 * @param lastExpenseText the last expense (bank and value), already masked
 *                        while the values are hidden
 * @param totalSpentText  the total spent this cycle, already masked while the
 *                        values are hidden
 * @param hidden          whether the bank name and the amounts are hidden (the
 *                        eye toggle), so the widget paints the crossed eye
 */
public record ExpenseWidgetData(
    String lastExpenseText,
    String totalSpentText,
    boolean hidden
) {}
