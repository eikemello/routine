package com.android.nls.routine.parser;

import android.service.notification.StatusBarNotification;
import com.android.nls.routine.model.Expense;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BtgParser implements Parser {
    private static final String BANK_NAME = "BTG";
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
    /**
     * Connector that links the value to the merchant ("R$ 45,90 em ...").
     */
    private static final Pattern CONNECTOR_PATTERN =
            Pattern.compile("^(?:em|no|na|[-–•])\\s+", Pattern.CASE_INSENSITIVE);
    /**
     * Fragments of the purchase announcement itself ("compra aprovada",
     * "cartão final 1234") carry no merchant information and must not become
     * the description.
     */
    private static final Pattern ANNOUNCEMENT_PATTERN =
            Pattern.compile("aprovada|autorizada|cart[ãa]o", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    /**
     * Closing sentence of the announcement ("Sua compra de R$ 62,78 em Kuroda
     * Atacarejo foi autorizada."): it follows the merchant and must not be
     * appended to the description.
     */
    private static final Pattern TRAILING_ANNOUNCEMENT_PATTERN =
            Pattern.compile("\\s+foi\\s+(?:autorizad|aprovad)\\w*\\s*[.!]?\\s*$",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    @Override
    public Expense parse(StatusBarNotification sbn) {
        return parseText(NotificationTextExtractor.extractText(sbn), sbn.getPostTime());
    }

    /**
     * Parses the text extracted from the notification. Kept apart from the
     * notification plumbing so the parsing rules can be unit tested.
     */
    Expense parseText(String text, long postTime) {
        if (text == null) {
            return null;
        }

        Matcher matcher = AMOUNT_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }

        // Only ignore if it's explicitly a non-credit purchase
        if (IGNORED_PATTERN.matcher(text).find()) {
            return null;
        }

        // Ensure it's actually a purchase announcement (contains 'compra' or 'autorizada'/'aprovada')
        String lowerText = text.toLowerCase();
        if (!lowerText.contains(PURCHASE_KEYWORD) &&
            !lowerText.contains("autorizada") &&
            !lowerText.contains("aprovada")) {
            return null;
        }

        double amount = parseAmount(Objects.requireNonNull(matcher.group(1)));
        String description = extractDescription(text, matcher.start());

        return new Expense(amount, description, BANK_NAME, postTime);
    }

    /**
     * The merchant usually comes right after the value ("R$ 45,90 em ...").
     * When the value stands alone on its line, the other lines are scanned
     * from the bottom up looking for the merchant.
     */
    private String extractDescription(String text, int amountStart) {
        String afterAmount = text.substring(amountStart)
                .replaceFirst(AMOUNT_PATTERN.pattern(), "")
                .trim();

        String description = cleanDescription(afterAmount.split("\n")[0]);
        if (!description.isEmpty()) {
            return description;
        }

        String[] lines = text.split("\n");
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.startsWith("R$") || line.toLowerCase().contains(PURCHASE_KEYWORD)) {
                continue;
            }

            description = cleanDescription(line);
            if (!description.isEmpty()) {
                return description;
            }
        }

        return "";
    }

    /**
     * Removes the connector that links the value to the merchant, the closing
     * sentence of the announcement and the purchase announcement fragments.
     */
    private String cleanDescription(String value) {
        String description = CONNECTOR_PATTERN.matcher(value.trim()).replaceFirst("").trim();
        description = TRAILING_ANNOUNCEMENT_PATTERN.matcher(description).replaceFirst("").trim();

        if (description.isEmpty() || ANNOUNCEMENT_PATTERN.matcher(description).find()) {
            return "";
        }

        return description;
    }

    private double parseAmount(String raw) {
        return Double.parseDouble(raw.replace(".", "").replace(",", "."));
    }
}
