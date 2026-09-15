package com.android.nls.routine.model;

/**
 * The current spending of one configured credit card: how much was spent inside
 * the card's own statement cycle and how much of the monthly limit that
 * represents (0-100).
 */
public record CardSpending(String bankName, String lastFour, double spent, int progress) {

}