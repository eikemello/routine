package com.android.nls.routine.parser;

import android.service.notification.StatusBarNotification;
import com.android.nls.routine.model.Expense;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ItauParser implements Parser {
    private static final String BANK_NAME = "Itaú";
    private static final String PURCHASE_KEYWORD = "compra";
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
    public Expense parse(StatusBarNotification sbn) {
        return parseText(NotificationTextExtractor.extractText(sbn), sbn.getPostTime());
    }

    /**
     * Parses the text extracted from the notification. Kept apart from the
     * notification plumbing so the parsing rules can be unit tested.
     */
    Expense parseText(String text, long postTime) {
        if (text == null || !text.toLowerCase().contains(PURCHASE_KEYWORD)) {
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
        String description = extractDescription(text, matcher.start());

        return new Expense(amount, description, BANK_NAME, postTime);
    }

    private String extractDescription(String text, int amountStart) {
        String afterAmount = text.substring(amountStart);
        String description = afterAmount.replaceFirst(AMOUNT_PATTERN.pattern(), "").trim();
        return description.isEmpty() ? "" : description;
    }

    private double parseAmount(String raw) {
        // Every dot is a thousand separator ("R$ 1.234,56"); only the comma is
        // the decimal separator.
        return Double.parseDouble(raw.replace(".", "").replace(",", "."));
    }
}