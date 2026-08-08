package com.android.nls.routine.parser;

import android.service.notification.StatusBarNotification;
import com.android.nls.routine.model.Expense;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InterParser implements Parser {
    private static final String BANK_NAME = "Inter";
    private static final String PURCHASE_KEYWORD = "compra";
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("R\\$\\s*([0-9]+(?:[.,][0-9]{1,2})?)");

    @Override
    public Expense parse(StatusBarNotification sbn) {
        String text = NotificationTextExtractor.extractText(sbn);
        if (!text.toLowerCase().contains(PURCHASE_KEYWORD)) {
            return null;
        }

        Matcher matcher = AMOUNT_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }

        double amount = parseAmount(Objects.requireNonNull(matcher.group(1)));
        String description = extractDescription(text);

        return new Expense(amount, description, BANK_NAME, sbn.getPostTime());
    }

    private String extractDescription(String text) {
        String[] lines = text.split("\n");
        // A descrição geralmente é a última linha após o valor
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i].trim();
            if (!line.isEmpty() && !line.startsWith("R$") && !line.toLowerCase().contains(PURCHASE_KEYWORD)) {
                return line;
            }
        }
        return "";
    }

    private double parseAmount(String raw) {
        return Double.parseDouble(raw.replace(",", "."));
    }
}