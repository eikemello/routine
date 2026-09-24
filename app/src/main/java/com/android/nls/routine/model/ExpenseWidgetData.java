package com.android.nls.routine.model;

/**
 * Data transfer object containing processed information for the expense widget.
 */
public record ExpenseWidgetData(
    String lastExpenseText,
    String totalSpentText
) {}
