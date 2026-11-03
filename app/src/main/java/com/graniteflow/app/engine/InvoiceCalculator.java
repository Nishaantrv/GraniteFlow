package com.graniteflow.app.engine;

import com.graniteflow.app.data.model.InvoiceDocument;
import com.graniteflow.app.data.model.InvoiceLineItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class InvoiceCalculator {

    public static double calculateLineAmount(double quantityCbm, double ratePerCbm) {
        double rawAmount = quantityCbm * ratePerCbm;
        return BigDecimal.valueOf(rawAmount).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public static void computeAndApplyInvoiceTotals(InvoiceDocument invoice) {
        if (invoice == null) return;
        List<InvoiceLineItem> items = invoice.getLineItems();
        double totalCbm = 0;
        double totalAmount = 0;
        int packageCount = 0;

        if (items != null) {
            for (InvoiceLineItem item : items) {
                if (!item.isManualAmountOverride()) {
                    item.setAmount(calculateLineAmount(item.getQuantityCbm(), item.getRatePerCbm()));
                }
                totalCbm += item.getQuantityCbm();
                totalAmount += item.getAmount();
                if (item.getAssociatedBlockNumbers() != null && !item.getAssociatedBlockNumbers().isEmpty()) {
                    packageCount += item.getAssociatedBlockNumbers().size();
                }
            }
        }

        invoice.setTotalCbm(BigDecimal.valueOf(totalCbm).setScale(3, RoundingMode.HALF_UP).doubleValue());
        invoice.setTotalAmount(BigDecimal.valueOf(totalAmount).setScale(2, RoundingMode.HALF_UP).doubleValue());
        if (packageCount > 0 && invoice.getTotalPackagesCount() == 0) {
            invoice.setTotalPackagesCount(packageCount);
        }

        invoice.setAmountInWords(CurrencyWordsConverter.convertToWords(invoice.getTotalAmount(), invoice.getCurrency()));
    }
}
