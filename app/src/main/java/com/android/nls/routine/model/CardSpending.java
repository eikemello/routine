package com.android.nls.routine.model;

public record CardSpending(String bankName, String lastFour, double spent, int progress, double monthlyLimit) {

}