package com.android.nls.routine.service;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import com.android.nls.routine.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Central renderer of the history panels opened from a home card. The panel is
 * the same for every card - the dialog_background chrome, the empty state, the
 * height-capped scrolling list, the total line and one shared row per record -
 * so it lives here once and each card only answers through {@link CardHistory}.
 * A card that allows editing or removing a record keeps its panel in sync by
 * calling the refresh handle it receives in
 * {@link CardHistory#getHistoryPanel(Runnable)}.
 */
public class CardHistoryDialog {

    // Extra scrim applied to whatever is behind the history panel. The dialog and
    // the home cards share the same dark palette, so a stronger dim is what makes
    // it obvious that the panel is a modal opened on top of the home screen.
    private static final float HISTORY_DIALOG_DIM = 0.65f;

    private final Context mContext;
    private final CardHistory mCardHistory;
    private View mPanelView;
    private Runnable mOnChanged;

    public CardHistoryDialog(Context context, CardHistory cardHistory) {
        mContext = context;
        mCardHistory = cardHistory;
    }

    /**
     * Opens the panel with the rows the card has stored right now.
     * @param onChanged run whenever a record is edited or removed, so the caller
     *                  can refresh whatever is displayed behind the panel
     */
    public void show(Runnable onChanged) {
        mOnChanged = onChanged;
        mPanelView = LayoutInflater.from(mContext).inflate(R.layout.dialog_card_history, new FrameLayout(mContext), false);

        AlertDialog dialog = new MaterialAlertDialogBuilder(mContext)
                .setTitle(mCardHistory.getHistoryTitleRes())
                .setView(mPanelView)
                .setPositiveButton(R.string.close_label, null)
                .setCancelable(true)
                .show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(AppCompatResources.getDrawable(mContext, R.drawable.dialog_background));
            dialog.getWindow().setDimAmount(HISTORY_DIALOG_DIM);
        }

        renderPanel();
    }

    /**
     * Rebuilds the list from the card and refreshes what is displayed behind the
     * panel. Handed to the card on every read, so an action that edits or removes
     * a record leaves the panel and the card showing the same data.
     */
    private void refresh() {
        renderPanel();

        if (mOnChanged != null) {
            mOnChanged.run();
        }
    }

    /**
     * Rebuilds the record list from the card. Called on open and again after a
     * record is changed, so the rows and the total always match what is stored.
     */
    private void renderPanel() {
        CardHistory.Panel panel = mCardHistory.getHistoryPanel(this::refresh);

        LinearLayout recordsContainer = mPanelView.findViewById(R.id.historyRecordsContainer);
        TextView txtEmpty = mPanelView.findViewById(R.id.txtHistoryEmpty);
        TextView txtTotal = mPanelView.findViewById(R.id.txtHistoryTotal);

        recordsContainer.removeAllViews();
        for (CardHistory.Row row : panel.rows()) {
            recordsContainer.addView(buildRowView(recordsContainer, row));
        }

        boolean hasRecords = !panel.rows().isEmpty();
        if (hasRecords) {
            // The divider is only a separator, so the last row doesn't keep one
            recordsContainer.getChildAt(recordsContainer.getChildCount() - 1)
                    .findViewById(R.id.viewHistoryRecordDivider)
                    .setVisibility(View.GONE);
        }

        boolean hasTotal = hasRecords && panel.totalText() != null;
        txtEmpty.setText(mCardHistory.getHistoryEmptyTextRes());
        txtEmpty.setVisibility(hasRecords ? View.GONE : View.VISIBLE);
        txtTotal.setVisibility(hasTotal ? View.VISIBLE : View.GONE);
        txtTotal.setText(hasTotal ? panel.totalText() : null);
    }

    /**
     * Inflates one row - icon, time, optional detail, amount and the actions the
     * card offers - into the records container.
     */
    private View buildRowView(LinearLayout recordsContainer, CardHistory.Row row) {
        View rowView = LayoutInflater.from(mContext).inflate(R.layout.item_history_record, recordsContainer, false);

        ImageView imgIcon = rowView.findViewById(R.id.imgHistoryRecordIcon);
        TextView txtTime = rowView.findViewById(R.id.txtHistoryRecordTime);
        TextView txtDetail = rowView.findViewById(R.id.txtHistoryRecordDetail);
        TextView txtNote = rowView.findViewById(R.id.txtHistoryRecordNote);
        TextView txtValue = rowView.findViewById(R.id.txtHistoryRecordValue);

        imgIcon.setImageResource(mCardHistory.getHistoryIconRes());
        imgIcon.setImageTintList(ColorStateList.valueOf(mContext.getColor(mCardHistory.getHistoryIconTintRes())));

        txtTime.setText(row.time());
        txtValue.setText(row.value());
//        txtValue.setTextColor(mContext.getColor(mCardHistory.getHistoryValueColorRes()));

        if (row.detail() != null) {
            txtDetail.setText(row.detail());
            txtDetail.setVisibility(View.VISIBLE);
        }

        if (row.note() != null && !row.note().isBlank()) {
            txtNote.setText(row.note());
            txtNote.setVisibility(View.VISIBLE);
        }

        bindAction(rowView.findViewById(R.id.btnHistoryRecordEdit), row.onEdit());
        bindAction(rowView.findViewById(R.id.btnHistoryRecordDelete), row.onDelete());

        return rowView;
    }

    /** Hides an action the card does not offer, e.g. removing an expense. */
    private void bindAction(ImageButton button, Runnable action) {
        if (action == null) {
            button.setVisibility(View.GONE);
            return;
        }

        button.setOnClickListener(v -> action.run());
    }
}