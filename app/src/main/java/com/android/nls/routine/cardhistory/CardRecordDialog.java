package com.android.nls.routine.cardhistory;

import android.content.Context;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RadioGroup;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatRadioButton;
import com.android.nls.routine.R;
import com.android.nls.routine.utils.Common;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.List;

/**
 * Central renderer of the dialogs a card history uses to change one record: the
 * editor of its amount, the editors of a choice plus a text or plus an amount -
 * which is also what opens when a record is added by hand - and the
 * confirmation that removes it. The chrome is the same for every card - the
 * dialog_background, the fields with their error message and the Remove/Save
 * plus Cancel buttons - so it lives here once, beside {@link CardHistoryDialog},
 * and a card only answers with the texts and with what the typed amount means to
 * it (millilitres, currency, ...).
 */
public final class CardRecordDialog {

    private CardRecordDialog() {
    }

    /** Saves the amount typed in the editor of a record. */
    @FunctionalInterface
    public interface AmountSaver {
        /**
         * @param amount text typed by the user, already trimmed
         * @return the error to display - keeping the editor open - or null when
         *         the amount was accepted and stored
         */
        String save(String amount);
    }

    /**
     * Opens the editor of the amount of a record. The dialog closes as soon as
     * the given saver accepts the typed amount; while the saver refuses it, the
     * returned message is shown inside the field.
     *
     * @param initialValue amount shown by default, in the format the card reads
     * @param inputType    keyboard accepted by the field, e.g. with or without decimals
     */
    public static void showEditAmountDialog(Context context,
                                            @StringRes int titleRes,
                                            @StringRes int hintRes,
                                            String initialValue,
                                            int inputType,
                                            AmountSaver saver) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_config_default_values, new FrameLayout(context), false);
        TextInputLayout txtInputError = view.findViewById(R.id.txtInputError);
        TextInputEditText etValue = view.findViewById(R.id.etValue);

        txtInputError.setHint(context.getString(hintRes));
        txtInputError.setError(null);
        etValue.setInputType(inputType);
        etValue.setText(initialValue);

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(titleRes)
                .setView(view)
                .setPositiveButton(context.getString(R.string.save_label), null)
                .setNegativeButton(context.getString(R.string.cancel_label), null)
                .setCancelable(true)
                .show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(AppCompatResources.getDrawable(context, R.drawable.dialog_background));
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            Editable value = etValue.getText();
            String error = saver.save(value == null ? "" : value.toString().trim());

            if (error != null) {
                // The card refused the amount, so the editor stays open with the
                // reason written under the field
                txtInputError.setError(error);
                return;
            }

            dialog.dismiss();
        });
    }

    /**
     * Asks before removing a record and runs the given action once the user
     * confirms it. The caller is the one that removes the record and refreshes
     * the panel and the card behind it.
     */
    public static void confirmRemove(Context context,
                                     @StringRes int titleRes,
                                     String message,
                                     Runnable onConfirm) {
        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(titleRes)
                .setMessage(message)
                .setPositiveButton(context.getString(R.string.remove_label), (d, which) -> onConfirm.run())
                .setNegativeButton(context.getString(R.string.cancel_label), null)
                .setCancelable(true)
                .show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(AppCompatResources.getDrawable(context, R.drawable.dialog_background));
        }
    }

    /** Saves the option picked and the text typed in the editor of a record. */
    @FunctionalInterface
    public interface OptionTextSaver {
        /**
         * @param option label of the picked option, or null when none is picked
         * @param text   text typed by the user, already trimmed
         * @return the error to display - keeping the editor open - or null when
         *         both were accepted and stored
         */
        String save(String option, String text);
    }

    /**
     * Opens the editor of a record whose value is a choice among fixed options
     * plus a free text - the status and the observation of a meal, for
     * instance. The dialog closes as soon as the given saver accepts the
     * change; while the saver refuses it, the returned message is shown inside
     * the field.
     *
     * @param options      labels of the selectable options
     * @param checkedIndex option shown as selected by default
     */
    public static void showEditOptionTextDialog(Context context,
                                                @StringRes int titleRes,
                                                List<String> options,
                                                int checkedIndex,
                                                @StringRes int hintRes,
                                                String initialText,
                                                OptionTextSaver saver) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_record_option_text, new FrameLayout(context), false);
        TextInputLayout txtInputError = view.findViewById(R.id.txtInputError);
        TextInputEditText etValue = view.findViewById(R.id.etValue);
        RadioGroup rgOptions = view.findViewById(R.id.rgRecordOptions);

        txtInputError.setHint(context.getString(hintRes));
        txtInputError.setError(null);
        etValue.setText(initialText);
        addOptions(context, rgOptions, options, checkedIndex);

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(titleRes)
                .setView(view)
                .setPositiveButton(context.getString(R.string.save_label), null)
                .setNegativeButton(context.getString(R.string.cancel_label), null)
                .setCancelable(true)
                .show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(AppCompatResources.getDrawable(context, R.drawable.dialog_background));
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String option = getCheckedOption(rgOptions, options);

            Editable text = etValue.getText();
            String error = saver.save(option, text == null ? "" : text.toString().trim());

            if (error != null) {
                // The card refused the change, so the editor stays open with
                // the reason written under the field
                txtInputError.setError(error);
                return;
            }

            dialog.dismiss();
        });
    }

    /** Saves the option picked and the amount typed in the editor of a record. */
    @FunctionalInterface
    public interface OptionAmountSaver {
        /**
         * @param option label of the picked option, or null when none is picked
         * @param amount text typed by the user, already trimmed
         * @return the error to display - keeping the editor open - or null when
         *         both were accepted and stored
         */
        String save(String option, String amount);
    }

    /**
     * Opens the editor of a record whose value is a choice among fixed options
     * plus an amount - the bank and the value of an expense, for instance. It is
     * also the dialog used to add a record by hand, which opens it with no
     * amount written. The dialog closes as soon as the given saver accepts the
     * change; while the saver refuses it, the returned message is shown inside
     * the amount field.
     *
     * @param options      labels of the selectable options
     * @param checkedIndex option shown as selected by default
     * @param inputType    keyboard accepted by the amount field, e.g. with or without decimals
     */
    public static void showEditOptionAmountDialog(Context context,
                                                  @StringRes int titleRes,
                                                  List<String> options,
                                                  int checkedIndex,
                                                  @StringRes int hintRes,
                                                  String initialAmount,
                                                  int inputType,
                                                  OptionAmountSaver saver) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_record_option_amount, new FrameLayout(context), false);
        TextInputLayout txtInputError = view.findViewById(R.id.txtInputError);
        TextInputEditText etValue = view.findViewById(R.id.etValue);
        RadioGroup rgOptions = view.findViewById(R.id.rgRecordOptions);

        txtInputError.setHint(context.getString(hintRes));
        txtInputError.setError(null);
        etValue.setInputType(inputType);
        etValue.setText(initialAmount);
        addOptions(context, rgOptions, options, checkedIndex);

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(titleRes)
                .setView(view)
                .setPositiveButton(context.getString(R.string.save_label), null)
                .setNegativeButton(context.getString(R.string.cancel_label), null)
                .setCancelable(true)
                .show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(AppCompatResources.getDrawable(context, R.drawable.dialog_background));
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String option = getCheckedOption(rgOptions, options);

            Editable amount = etValue.getText();
            String error = saver.save(option, amount == null ? "" : amount.toString().trim());

            if (error != null) {
                // The card refused the change, so the editor stays open with
                // the reason written under the field
                txtInputError.setError(error);
                return;
            }

            dialog.dismiss();
        });
    }

    /**
     * Fills the group with one option each, the one in the given index checked by
     * default. Every option gets an id of its own, which is what lets the group
     * uncheck the previous one when another is picked: a view added without an id
     * cannot be found again by the group, so the options would all stay checked
     * and the first one would always be read back.
     */
    private static void addOptions(Context context, RadioGroup group, List<String> options, int checkedIndex) {
        int spacing = Common.dpToPx(context, 6);

        for (int i = 0; i < options.size(); i++) {
            AppCompatRadioButton option = new AppCompatRadioButton(group.getContext());
            option.setId(View.generateViewId());
            option.setText(options.get(i));
            option.setTextColor(context.getColor(R.color.text_primary));
            option.setPadding(0, spacing, 0, spacing);
            group.addView(option);

            if (i == checkedIndex) {
                // Checked through the group, which is what remembers the option
                // picked and clears it when another one is selected
                group.check(option.getId());
            }
        }
    }

    /**
     * Label of the option picked in the group, or null when none is picked - an
     * index out of the offered ones leaves the whole group empty.
     */
    private static String getCheckedOption(RadioGroup group, List<String> options) {
        int checkedId = group.getCheckedRadioButtonId();
        int checkedIndex = checkedId == View.NO_ID ? -1 : group.indexOfChild(group.findViewById(checkedId));

        return checkedIndex < 0 ? null : options.get(checkedIndex);
    }
}
