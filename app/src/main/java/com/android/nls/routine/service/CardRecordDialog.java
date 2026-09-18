package com.android.nls.routine.service;

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
 * editor of its amount, the editor of a choice plus a text and the confirmation
 * that removes it. The chrome is the
 * same for every card - the dialog_background, the amount field with its error
 * message and the Remove/Save plus Cancel buttons - so it lives here once,
 * beside {@link CardHistoryDialog}, and a card only answers with the texts and
 * with what the typed amount means to it (millilitres, currency, ...).
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

        int spacing = Common.dpToPx(context, 6);
        for (int i = 0; i < options.size(); i++) {
            AppCompatRadioButton option = new AppCompatRadioButton(rgOptions.getContext());
            option.setText(options.get(i));
            option.setTextColor(context.getColor(R.color.text_primary));
            option.setPadding(0, spacing, 0, spacing);
            option.setChecked(i == checkedIndex);
            rgOptions.addView(option);
        }

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
            String option = null;
            for (int i = 0; i < rgOptions.getChildCount(); i++) {
                AppCompatRadioButton button = (AppCompatRadioButton) rgOptions.getChildAt(i);
                if (button.isChecked()) {
                    option = options.get(i);
                    break;
                }
            }

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
}
