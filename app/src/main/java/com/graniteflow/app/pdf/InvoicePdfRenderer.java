package com.graniteflow.app.pdf;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;

import com.graniteflow.app.R;
import com.graniteflow.app.data.model.InvoiceDocument;
import com.graniteflow.app.data.model.InvoiceLineItem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class InvoicePdfRenderer {

    // Standard A4 dimensions at 72 DPI
    public static final int PAGE_WIDTH = 595;
    public static final int PAGE_HEIGHT = 842;

    public static File renderInvoicePdf(Context context, InvoiceDocument invoice, String fileName) throws IOException {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // Fill background white
        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.WHITE);
        canvas.drawRect(0, 0, PAGE_WIDTH, PAGE_HEIGHT, bgPaint);

        Paint borderPaint = new Paint();
        borderPaint.setColor(Color.BLACK);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1.0f);

        Paint boldPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        boldPaint.setColor(Color.BLACK);
        boldPaint.setFakeBoldText(true);
        boldPaint.setTextSize(7.5f);

        Paint boldLargePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        boldLargePaint.setColor(Color.BLACK);
        boldLargePaint.setFakeBoldText(true);
        boldLargePaint.setTextSize(8.5f);

        Paint boldCenterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        boldCenterPaint.setColor(Color.BLACK);
        boldCenterPaint.setFakeBoldText(true);
        boldCenterPaint.setTextSize(7.5f);
        boldCenterPaint.setTextAlign(Paint.Align.CENTER);

        Paint boldRightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        boldRightPaint.setColor(Color.BLACK);
        boldRightPaint.setFakeBoldText(true);
        boldRightPaint.setTextSize(8f);
        boldRightPaint.setTextAlign(Paint.Align.RIGHT);

        Paint regularPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        regularPaint.setColor(Color.BLACK);
        regularPaint.setTextSize(7.5f);

        Paint regularCenterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        regularCenterPaint.setColor(Color.BLACK);
        regularCenterPaint.setTextSize(7.5f);
        regularCenterPaint.setTextAlign(Paint.Align.CENTER);

        Paint regularRightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        regularRightPaint.setColor(Color.BLACK);
        regularRightPaint.setTextSize(7.5f);
        regularRightPaint.setTextAlign(Paint.Align.RIGHT);

        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(Color.BLACK);
        titlePaint.setFakeBoldText(true);
        titlePaint.setTextSize(10.5f);
        titlePaint.setTextAlign(Paint.Align.CENTER);

        Paint bluePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bluePaint.setColor(Color.parseColor("#1868B7"));
        bluePaint.setFakeBoldText(true);
        bluePaint.setTextSize(9f);

        // Document Outer Frame Margins
        float mLeft = 20f;
        float mTop = 22f;
        float mRight = PAGE_WIDTH - 20f;
        float mBottom = PAGE_HEIGHT - 22f;

        // Draw Outer Document Rectangle enclosing the entire layout
        canvas.drawRect(mLeft, mTop, mRight, mBottom, borderPaint);

        // ================= TOP TITLE BAR =================
        float titleBottom = mTop + 24f;
        canvas.drawLine(mLeft, titleBottom, mRight, titleBottom, borderPaint);

        String docTitle = (invoice.getInvoiceType() != null && !invoice.getInvoiceType().isEmpty())
                ? invoice.getInvoiceType().toUpperCase() : "PROFORMA INVOICE";
        canvas.drawText(docTitle, PAGE_WIDTH / 2f, mTop + 15.5f, titlePaint);
        float titleTextWidth = titlePaint.measureText(docTitle);
        canvas.drawLine((PAGE_WIDTH - titleTextWidth) / 2f, mTop + 17.5f, (PAGE_WIDTH + titleTextWidth) / 2f, mTop + 17.5f, borderPaint);

        // Proportion: 53.6% from left border for Exporter / Consignee divider
        float midX = mLeft + (mRight - mLeft) * 0.536f;

        // ================= ROW 1: EXPORTER & REFERENCES =================
        float row1Bottom = titleBottom + 104f;
        canvas.drawLine(mLeft, row1Bottom, mRight, row1Bottom, borderPaint);
        canvas.drawLine(midX, titleBottom, midX, row1Bottom, borderPaint);

        // Exporter (Left)
        canvas.drawText("Exporter", mLeft + 4, titleBottom + 10f, boldLargePaint);
        if (invoice.getExporter() != null) {
            String[] exporterLines = invoice.getExporter().getFormattedAddress().split("\n");
            float y = titleBottom + 21f;
            for (String l : exporterLines) {
                canvas.drawText(l, mLeft + 4, y, regularPaint);
                y += 10.5f;
            }
        }

        // Invoice No, Date & References (Right)
        float r1 = titleBottom + 26f;
        float r2 = r1 + 22f;
        canvas.drawLine(midX, r1, mRight, r1, borderPaint);
        canvas.drawLine(midX, r2, mRight, r2, borderPaint);

        float subMidX = midX + (mRight - midX) * 0.54f;
        canvas.drawLine(subMidX, titleBottom, subMidX, r2, borderPaint);

        canvas.drawText("Invoice no & Date", midX + 4, titleBottom + 9.5f, boldPaint);
        canvas.drawText(invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : "", midX + 4, titleBottom + 20f, regularPaint);
        canvas.drawText("Exporter Ref No:", subMidX + 4, titleBottom + 9.5f, boldPaint);

        canvas.drawText("Buyer's Order no & Date", midX + 4, r1 + 9.5f, boldPaint);

        canvas.drawText("Other References", midX + 4, r2 + 9.5f, boldPaint);
        if (invoice.getExporter() != null) {
            canvas.drawText("RBI CODE NO: " + (invoice.getExporter().getRbiCode() != null ? invoice.getExporter().getRbiCode() : ""), midX + 4, r2 + 20f, regularPaint);
            canvas.drawText("IE CODE NO  : " + (invoice.getExporter().getIecCode() != null ? invoice.getExporter().getIecCode() : ""), midX + 4, r2 + 29.5f, regularPaint);
            canvas.drawText("GSTIN NO: " + (invoice.getExporter().getGstin() != null ? invoice.getExporter().getGstin() : ""), midX + 4, r2 + 39f, regularPaint);
        }

        // ================= ROW 2: CONSIGNEE & BUYER =================
        float row2Bottom = row1Bottom + 70f;
        canvas.drawLine(mLeft, row2Bottom, mRight, row2Bottom, borderPaint);
        canvas.drawLine(midX, row1Bottom, midX, row2Bottom, borderPaint);

        canvas.drawText("Consignee", mLeft + 4, row1Bottom + 10f, boldLargePaint);
        if (invoice.getConsignee() != null) {
            String[] consLines = invoice.getConsignee().getFormattedConsignee().split("\n");
            float y = row1Bottom + 21f;
            for (String l : consLines) {
                canvas.drawText(l, mLeft + 4, y, regularPaint);
                y += 10.5f;
            }
        }
        canvas.drawText("Buyer if other than consignee", midX + 4, row1Bottom + 10f, boldPaint);

        // ================= ROW 3: SHIPPING & DELIVERY =================
        float sRow1 = row2Bottom + 25f;
        float sRow2 = sRow1 + 25f;
        float sRow3 = sRow2 + 25f; // row 3 bottom / table top

        canvas.drawLine(mLeft, sRow1, mRight, sRow1, borderPaint);
        // Important: Horizontal line at sRow2 only crosses the left side (Terms of Delivery box on right is uninterrupted)
        canvas.drawLine(mLeft, sRow2, midX, sRow2, borderPaint);
        canvas.drawLine(mLeft, sRow3, mRight, sRow3, borderPaint);

        float col1 = mLeft + (midX - mLeft) * 0.48f;
        canvas.drawLine(col1, row2Bottom, col1, sRow3, borderPaint);
        canvas.drawLine(midX, row2Bottom, midX, sRow3, borderPaint);

        // Sub-divider in Sub-row 3A right column
        float colOriginDest = midX + (mRight - midX) * 0.50f;
        canvas.drawLine(colOriginDest, row2Bottom, colOriginDest, sRow1, borderPaint);

        // Sub-row 3A text
        canvas.drawText("Pre-Carriage by", mLeft + 4, row2Bottom + 9.5f, boldPaint);
        canvas.drawText("Place of Receipt by Pre-Carrier", col1 + 4, row2Bottom + 9.5f, boldPaint);
        canvas.drawText("Country of origin of goods", midX + 4, row2Bottom + 9.5f, boldPaint);
        canvas.drawText(invoice.getCountryOfOrigin() != null ? invoice.getCountryOfOrigin() : "INDIA", midX + 4, row2Bottom + 20f, regularPaint);
        canvas.drawText("Country of Final Destination", colOriginDest + 4, row2Bottom + 9.5f, boldPaint);
        canvas.drawText(invoice.getCountryOfDestination() != null ? invoice.getCountryOfDestination() : "CHINA", colOriginDest + 4, row2Bottom + 20f, regularPaint);

        // Sub-row 3B text
        canvas.drawText("Vessel/Flight No", mLeft + 4, sRow1 + 9.5f, boldPaint);
        canvas.drawText("Port of Loading", col1 + 4, sRow1 + 9.5f, boldPaint);
        canvas.drawText(invoice.getPortOfLoading() != null ? invoice.getPortOfLoading() : "", col1 + 4, sRow1 + 20f, regularPaint);

        canvas.drawText("Terms of Delivery and Payment", midX + 4, sRow1 + 9.5f, boldPaint);
        if (invoice.getTermsOfDeliveryPayment() != null) {
            String[] tLines = invoice.getTermsOfDeliveryPayment().split("\n");
            float ty = sRow1 + 20f;
            for (String t : tLines) {
                canvas.drawText(t, midX + 4, ty, regularPaint);
                ty += 9.5f;
            }
        }

        // Sub-row 3C text
        canvas.drawText("Port of Discharge", mLeft + 4, sRow2 + 9.5f, boldPaint);
        canvas.drawText(invoice.getPortOfDischarge() != null ? invoice.getPortOfDischarge() : "", mLeft + 4, sRow2 + 20f, regularPaint);
        canvas.drawText("Final destination", col1 + 4, sRow2 + 9.5f, boldPaint);
        canvas.drawText(invoice.getFinalDestination() != null ? invoice.getFinalDestination() : "", col1 + 4, sRow2 + 20f, regularPaint);

        // ================= ROW 4: LINE ITEMS TABLE =================
        // Column positions
        float colQty = midX;
        float colRate = midX + 68f;
        float colAmt = colRate + 56f;

        // Bottom section coordinates
        float totalsTop = mBottom - 110f;
        float wordsTop = totalsTop + 24f;
        float wordsBottom = wordsTop + 26f;

        // Vertical column divider lines extending from sRow3 down to totalsTop
        canvas.drawLine(colQty, sRow3, colQty, totalsTop, borderPaint);
        canvas.drawLine(colRate, sRow3, colRate, totalsTop, borderPaint);
        canvas.drawLine(colAmt, sRow3, colAmt, totalsTop, borderPaint);

        // Header labels inside the table (NO horizontal line under header, matches reference format)
        canvas.drawText("Marks & Nos/        No & Kind if Pkgs        Description of Goods", mLeft + 4, sRow3 + 10f, boldPaint);

        canvas.drawText("Quantity", (colQty + colRate) / 2f, sRow3 + 9f, boldCenterPaint);
        canvas.drawText("CBM", (colQty + colRate) / 2f, sRow3 + 19f, boldCenterPaint);

        canvas.drawText("Rate", (colRate + colAmt) / 2f, sRow3 + 9f, boldCenterPaint);
        canvas.drawText("USD", (colRate + colAmt) / 2f, sRow3 + 19f, boldCenterPaint);

        canvas.drawText("Amount", (colAmt + mRight) / 2f, sRow3 + 9f, boldCenterPaint);
        canvas.drawText("USD", (colAmt + mRight) / 2f, sRow3 + 19f, boldCenterPaint);

        // Table Body Header inside Description Column
        float itemStartY = sRow3 + 36f;
        String descHeader = invoice.getDescriptionHeader();
        if (descHeader != null && !descHeader.trim().isEmpty()) {
            String[] lines = descHeader.split("\n");
            for (String l : lines) {
                if (!l.trim().isEmpty()) {
                    canvas.drawText(l.trim(), mLeft + 4, itemStartY, boldLargePaint);
                    itemStartY += 12f;
                }
            }
        }

        // Line Items
        float curY = Math.max(itemStartY + 8f, sRow3 + 60f);
        List<InvoiceLineItem> items = invoice.getLineItems();
        if (items != null) {
            for (InvoiceLineItem it : items) {
                canvas.drawText(it.getMarksAndNos() != null ? it.getMarksAndNos() : "", mLeft + 4, curY, regularPaint);
                canvas.drawText(String.format("%.3f", it.getQuantityCbm()), (colQty + colRate) / 2f, curY, regularCenterPaint);
                canvas.drawText(String.format("%.0f", it.getRatePerCbm()), (colRate + colAmt) / 2f, curY, regularCenterPaint);
                canvas.drawText(String.format("%.2f", it.getAmount()), mRight - 28f, curY, regularRightPaint);
                curY += 26f;
            }
        }

        // Bank Details block inside Description Column
        float bankY = totalsTop - 85f;
        // Accent bar next to BANK DETAILS
        Paint fillAccent = new Paint();
        fillAccent.setColor(Color.BLACK);
        fillAccent.setStyle(Paint.Style.FILL);
        canvas.drawRect(mLeft + 4, bankY - 9f, mLeft + 6.5f, bankY + 2f, fillAccent);

        canvas.drawText("BANK DETAILS :", mLeft + 10f, bankY, boldLargePaint);
        float bdWidth = boldLargePaint.measureText("BANK DETAILS :");
        canvas.drawLine(mLeft + 10f, bankY + 1.5f, mLeft + 10f + bdWidth, bankY + 1.5f, borderPaint);

        if (invoice.getExporter() != null) {
            float by = bankY + 11.5f;
            canvas.drawText(invoice.getExporter().getBankName(), mLeft + 4, by, regularPaint);
            by += 10f;
            canvas.drawText(invoice.getExporter().getBankBranch(), mLeft + 4, by, regularPaint);
            by += 10f;
            canvas.drawText(invoice.getExporter().getBankAddress(), mLeft + 4, by, regularPaint);
            by += 10f;
            canvas.drawText("ACCOUNT : " + invoice.getExporter().getAccountHolder(), mLeft + 4, by, regularPaint);
            by += 10f;
            canvas.drawText("A/C.NO." + invoice.getExporter().getAccountNumber(), mLeft + 4, by, regularPaint);
            by += 10f;
            canvas.drawText("SWIFT: " + invoice.getExporter().getSwiftCode(), mLeft + 4, by, regularPaint);
        }

        // ================= ROW 5: TOTALS ROW =================
        canvas.drawLine(mLeft, totalsTop, mRight, totalsTop, borderPaint);
        canvas.drawLine(mLeft, wordsTop, mRight, wordsTop, borderPaint);

        canvas.drawText("TOTAL NO. OF BLOCKS  : " + invoice.getTotalPackagesCount() + " ONLY", mLeft + 4, totalsTop + 15f, boldLargePaint);
        canvas.drawText(String.format("%.3f", invoice.getTotalCbm()), (colQty + colRate) / 2f, totalsTop + 15f, boldCenterPaint);
        canvas.drawText("Total", (colRate + colAmt) / 2f, totalsTop + 15f, boldCenterPaint);
        canvas.drawText(String.format("%.2f", invoice.getTotalAmount()), mRight - 28f, totalsTop + 15f, boldRightPaint);

        // ================= ROW 6: AMOUNT IN WORDS =================
        canvas.drawLine(mLeft, wordsBottom, mRight, wordsBottom, borderPaint);
        canvas.drawText("Amount Chargeable (in Words)", mLeft + 4, wordsTop + 9.5f, boldPaint);
        canvas.drawText(invoice.getAmountInWords() != null ? invoice.getAmountInWords() : "", mLeft + 4, wordsTop + 20.5f, boldLargePaint);

        // ================= ROW 7: DECLARATION & SIGNATURE =================
        canvas.drawLine(midX, wordsBottom, midX, mBottom, borderPaint);

        // Declaration (Left)
        canvas.drawText("Declaration", mLeft + 4, wordsBottom + 10f, boldPaint);
        canvas.drawText("We declare that this invoice shows the actual price of the goods.", mLeft + 4, wordsBottom + 21f, regularPaint);
        canvas.drawText("Described and that all particulars are true and correct.", mLeft + 4, wordsBottom + 31.5f, regularPaint);

        // Signature (Right)
        canvas.drawText("Signature and Date", midX + 4, wordsBottom + 10f, boldPaint);

        // Render authentic exporter signature stamp
        boolean stampRendered = false;
        try {
            Bitmap sigBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_signature_stamp);
            if (sigBitmap != null) {
                float stampWidth = 148f;
                float stampHeight = stampWidth * ((float) sigBitmap.getHeight() / (float) sigBitmap.getWidth());
                float sigLeft = midX + (mRight - midX - stampWidth) / 2f;
                float sigTop = wordsBottom + 15f;
                RectF destRect = new RectF(sigLeft, sigTop, sigLeft + stampWidth, sigTop + stampHeight);
                canvas.drawBitmap(sigBitmap, null, destRect, null);
                stampRendered = true;
            }
        } catch (Exception ignored) {}

        if (!stampRendered) {
            // High-fidelity vector fallback
            float sx = midX + 35f;
            float sy = wordsBottom + 23f;
            String sigCompany = (invoice.getExporter() != null && !invoice.getExporter().getCompanyName().isEmpty()) 
                    ? "For " + invoice.getExporter().getCompanyName() 
                    : (invoice.getSignatureTitle() != null ? invoice.getSignatureTitle() : "");
            if (!sigCompany.isEmpty()) {
                canvas.drawText(sigCompany, sx, sy, bluePaint);
            }

            // Vector signature flourish
            Paint sigStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
            sigStroke.setColor(Color.parseColor("#1868B7"));
            sigStroke.setStyle(Paint.Style.STROKE);
            sigStroke.setStrokeWidth(1.2f);

            Path sigPath = new Path();
            sigPath.moveTo(sx + 10f, sy + 14f);
            sigPath.cubicTo(sx + 35f, sy + 3f, sx + 45f, sy + 25f, sx + 75f, sy + 15f);
            sigPath.cubicTo(sx + 95f, sy + 8f, sx + 115f, sy + 22f, sx + 135f, sy + 16f);
            canvas.drawPath(sigPath, sigStroke);

            canvas.drawText("Partner", sx + 60f, sy + 34f, bluePaint);
        }

        document.finishPage(page);

        File pdfFile = new File(context.getCacheDir(), fileName);
        FileOutputStream fos = new FileOutputStream(pdfFile);
        document.writeTo(fos);
        document.close();
        fos.close();

        return pdfFile;
    }
}
