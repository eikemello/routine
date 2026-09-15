package com.android.nls.routine.model;

/**
 * A credit card configured by the user: the bank it belongs to, its last four
 * digits and the day of the month when its statement closes (used by the
 * expense cycle).
 */
public record CreditCard(String bankName, String lastFour, int closingDay) {

}