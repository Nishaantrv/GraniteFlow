package com.graniteflow.app;

import com.graniteflow.app.data.model.InvoiceDocument;
import com.graniteflow.app.data.model.InvoiceLineItem;
import com.graniteflow.app.engine.CurrencyWordsConverter;
import com.graniteflow.app.engine.InvoiceCalculator;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class InvoiceCalculatorTest {

    @Test
    public void testInvoiceLineCalculationsAndTotals() {
        InvoiceDocument invoice = new InvoiceDocument();
        List<InvoiceLineItem> items = new ArrayList<>();

        // From Reference Image 1:
        // Line 1: 17.023 CBM @ $1250 = $21,278.75
        InvoiceLineItem item1 = new InvoiceLineItem("Block no. 138, 139...", 17.023, 1250);
        assertEquals(21278.75, item1.getAmount(), 0.01);
        item1.setAssociatedBlockNumbers(Arrays.asList("138", "139", "140", "142", "144", "145", "146", "147", "148", "149", "150", "151", "153", "159"));
        items.add(item1);

        // Line 2: 17.323 CBM @ $1450 = $25,118.35
        InvoiceLineItem item2 = new InvoiceLineItem("Block no. 141, 143...", 17.323, 1450);
        assertEquals(25118.35, item2.getAmount(), 0.01);
        item2.setAssociatedBlockNumbers(Arrays.asList("141", "143", "152", "154", "156", "157"));
        items.add(item2);

        // Line 3: 3.578 CBM @ $1500 = $5,367.00
        InvoiceLineItem item3 = new InvoiceLineItem("Block no. 155", 3.578, 1500);
        assertEquals(5367.00, item3.getAmount(), 0.01);
        item3.setAssociatedBlockNumbers(Arrays.asList("155"));
        items.add(item3);

        // Line 4: 3.582 CBM @ $1550 = $5,552.10
        InvoiceLineItem item4 = new InvoiceLineItem("Block no. 158", 3.582, 1550);
        assertEquals(5552.10, item4.getAmount(), 0.01);
        item4.setAssociatedBlockNumbers(Arrays.asList("158"));
        items.add(item4);

        invoice.setLineItems(items);
        InvoiceCalculator.computeAndApplyInvoiceTotals(invoice);

        // Total CBM: 17.023 + 17.323 + 3.578 + 3.582 = 41.506
        assertEquals(41.506, invoice.getTotalCbm(), 0.001);

        // Total Amount: 21278.75 + 25118.35 + 5367.00 + 5552.10 = 57316.20
        assertEquals(57316.20, invoice.getTotalAmount(), 0.01);

        // Total Block count: 14 + 6 + 1 + 1 = 22 blocks
        assertEquals(22, invoice.getTotalPackagesCount());

        // Amount in Words:
        // Expected: USD FIFTY SEVEN THOUSAND THREE HUNDRED SIXTEEN AND CENTS TWENTY ONLY
        String words = invoice.getAmountInWords();
        assertEquals("USD FIFTY SEVEN THOUSAND THREE HUNDRED SIXTEEN AND CENTS TWENTY ONLY", words);
    }

    @Test
    public void testCurrencyWordsConverterVariousAmounts() {
        assertEquals("USD ONE HUNDRED ONLY", CurrencyWordsConverter.convertToWords(100.0, "USD"));
        assertEquals("USD FIVE HUNDRED AND CENTS FIFTY ONLY", CurrencyWordsConverter.convertToWords(500.50, "USD"));
        assertEquals("USD ZERO ONLY", CurrencyWordsConverter.convertToWords(0.0, "USD"));
    }
}
