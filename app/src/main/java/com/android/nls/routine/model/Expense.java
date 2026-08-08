package com.android.nls.routine.model;

import androidx.annotation.NonNull;

public record Expense(double amount, String description, String bank, long timestamp) {

    @NonNull
    @Override
    public String toString() {
        return "Expense{" +
                "amount=" + amount +
                ", description='" + description + '\'' +
                ", bank='" + bank + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}