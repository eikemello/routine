package com.android.nls.routine.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/**
 * The joining of the notification fragments and the amount detection are the
 * pure Java parts of the extractor, so they run without an Android runtime.
 */
public class NotificationTextExtractorTest {

    @Test
    public void joinParts_joinsEveryFragmentInOrder() {
        List<CharSequence> parts = List.<CharSequence>of(
                "Compra aprovada", "R$ 45,90", "Mercado X");

        assertEquals("Compra aprovada\nR$ 45,90\nMercado X",
                NotificationTextExtractor.joinParts(parts));
    }

    @Test
    public void joinParts_skipsNullAndEmptyFragments() {
        List<CharSequence> parts = new ArrayList<>();
        parts.add(null);
        parts.add("Compra aprovada");
        parts.add("   ");
        parts.add("");

        assertEquals("Compra aprovada", NotificationTextExtractor.joinParts(parts));
    }

    @Test
    public void joinParts_skipsAFragmentAlreadyPresent() {
        // The expanded view repeats the collapsed text and a smaller fragment
        // is contained in the bigger one: neither is joined twice.
        List<CharSequence> parts = List.<CharSequence>of(
                "Sua compra de R$ 62,78 em Kuroda Atacarejo foi autorizada.",
                "Sua compra de R$ 62,78 em Kuroda Atacarejo foi autorizada.",
                "Sua compra");

        assertEquals("Sua compra de R$ 62,78 em Kuroda Atacarejo foi autorizada.",
                NotificationTextExtractor.joinParts(parts));
    }

    @Test
    public void joinParts_withoutFragments_returnsAnEmptyText() {
        assertEquals("", NotificationTextExtractor.joinParts(new ArrayList<>()));
    }

    @Test
    public void hasAmount_findsTheAmountWithTheBrazilianFormat() {
        assertTrue(NotificationTextExtractor.hasAmount("Compra autorizada\nR$ 25,90"));
        assertTrue(NotificationTextExtractor.hasAmount("Compra aprovada de R$1.234,56"));
        assertTrue(NotificationTextExtractor.hasAmount("Compra aprovada de r$ 9,90"));
    }

    @Test
    public void hasAmount_withoutAnAmount_isFalse() {
        // The BTG notification: the extras carry only the announcement, the
        // body with the amount exists inside the custom layout.
        assertFalse(NotificationTextExtractor.hasAmount("Compra autorizada"));
        assertFalse(NotificationTextExtractor.hasAmount("R$ autorizada"));
        assertFalse(NotificationTextExtractor.hasAmount(""));
        assertFalse(NotificationTextExtractor.hasAmount(null));
    }
}
