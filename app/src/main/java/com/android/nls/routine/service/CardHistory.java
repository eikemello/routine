package com.android.nls.routine.service;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import java.util.List;

/**
 * Contract of a home card that opens its history panel (see CardHistoryDialog).
 * Every card answers with the same pieces - the panel texts and the row colors
 * plus the rows stored for the period the card tracks - so the panel is written
 * once, in the renderer, and a new card only adds an implementation of this
 * contract. Water and expenses are the current ones.
 */
public interface CardHistory {

    /** Title of the panel, e.g. "Today water history". */
    @StringRes
    int getHistoryTitleRes();

    /** Message shown while the card has nothing to list. */
    @StringRes
    int getHistoryEmptyTextRes();

    /** Icon drawn on the left of every row. */
    @DrawableRes
    int getHistoryIconRes();

    /** Tint of the row icon, matching the color of the card. */
    @ColorRes
    int getHistoryIconTintRes();

    /** Color of the amount written on the right of every row. */
    @ColorRes
    int getHistoryValueColorRes();

    /**
     * Rows and total of the period tracked by the card. It is read when the
     * panel opens and again after every change, so it always queries the
     * database. The given refresh handle repaints the panel and notifies
     * whatever is displayed behind it: an action that edits or removes a record
     * must call it so the panel and the card keep showing the same data.
     */
    Panel getHistoryPanel(Runnable refresh);

    /**
     * Everything the panel shows at a given moment: the rows of the period and
     * the text of the total line, or null when the card has no total to show.
     */
    record Panel(List<Row> rows, String totalText) {
    }

    /**
     * One line of the panel: when the record was stored, its amount and an
     * optional detail - the bank of an expense, for instance. An action left
     * null is not offered, so a card that cannot change a record simply passes
     * no action.
     */
    record Row(String time, String value, String detail, Runnable onEdit, Runnable onDelete) {

        /** Row of a card that offers no action on the record. */
        public Row(String time, String value, String detail) {
            this(time, value, detail, null, null);
        }

        /** Row of a card that shows no detail besides the amount. */
        public Row(String time, String value, Runnable onEdit, Runnable onDelete) {
            this(time, value, null, onEdit, onDelete);
        }
    }
}
