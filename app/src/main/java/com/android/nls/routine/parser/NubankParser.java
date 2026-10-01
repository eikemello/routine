package com.android.nls.routine.parser;

import android.content.Context;
import android.service.notification.StatusBarNotification;
import com.android.nls.routine.model.Expense;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NubankParser implements Parser {
    private static final String BANK_NAME = "Nubank";
    private static final String PURCHASE_TITLE = "Compra aprovada";
    /**
     * The amount may carry the Brazilian thousand separator ("R$ 1.234,56"),
     * so the grouped digits are matched before the plain value alternative
     * ("R$ 45,90").
     */
    private static final Pattern AMOUNT_PATTERN =
            Pattern.compile("R\\$\\s*([0-9]{1,3}(?:\\.[0-9]{3})+(?:,[0-9]{1,2})?|[0-9]+(?:,[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE);
    /**
     * A reversed ("estorno"), cancelled ("compra cancelada") or debit ("compra
     * no débito") purchase is not credit card spending, so those notifications
     * are ignored instead of corrupting the card totals.
     */
    private static final Pattern IGNORED_PATTERN =
            Pattern.compile("estorno|cancelad|d[ée]bito", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    @Override
    public Expense parse(Context context, StatusBarNotification sbn) {
        return parseText(NotificationTextExtractor.extractText(context, sbn), sbn.getPostTime());
    }

    /**
     * Parses the text extracted from the notification. Kept apart from the
     * notification plumbing so the parsing rules can be unit tested.
     */
    Expense parseText(String text, long postTime) {
        if (text == null || !text.contains(PURCHASE_TITLE)) {
            return null;
        }

        Matcher matcher = AMOUNT_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }

        if (IGNORED_PATTERN.matcher(text).find()) {
            return null;
        }

        double amount = parseAmount(Objects.requireNonNull(matcher.group(1)));
        String description = extractDescription(text);

        return new Expense(amount, description, BANK_NAME, postTime);
    }

    private String extractDescription(String text) {
        String[] lines = text.split("\n");

        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i].trim();
            if (!line.isEmpty() && !line.startsWith("R$") && !line.equalsIgnoreCase(PURCHASE_TITLE)) {
                return line;
            }
        }
        return "";
    }

    private double parseAmount(String raw) {
        // Every dot is a thousand separator ("R$ 1.234,56"); only the comma is
        // the decimal separator.
        return Double.parseDouble(raw.replace(".", "").replace(",", "."));
    }
}