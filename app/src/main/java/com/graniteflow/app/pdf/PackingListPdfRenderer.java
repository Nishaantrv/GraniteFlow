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
import com.graniteflow.app.data.model.PackingListDocument;
import com.graniteflow.app.data.model.PackingListRow;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class PackingListPdfRenderer {

    // Standard A4 dimensions at 72 DPI
    public static final int PAGE_WIDTH = 595;
    public static final int PAGE_HEIGHT = 842;

    public static File renderPackingListPdf(Context context, PackingListDocument packingList, String fileName) throws IOException {
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

        Paint regularPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        regularPaint.setColor(Color.BLACK);
        regularPaint.setTextSize(7.5f);

        Paint regularCenterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        regularCenterPaint.setColor(Color.BLACK);
        regularCenterPaint.setTextSize(7.5f);
        regularCenterPaint.setTextAlign(Paint.Align.CENTER);

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

        String docTitle = (packingList.getDocumentTitle() != null && !packingList.getDocumentTitle().isEmpty())
                ? packingList.getDocumentTitle().toUpperCase() : "PACKING LIST";
        canvas.drawText(docTitle, PAGE_WIDTH / 2f, mTop + 15.5f, titlePaint);
        float titleTextWidth = titlePaint.measureText(docTitle);
        canvas.drawLine((PAGE_WIDTH - titleTextWidth) / 2f, mTop + 17.5f, (PAGE_WIDTH + titleTextWidth) / 2f, mTop + 17.5f, borderPaint);

        // Proportion: 50.6% from left border for Exporter / References divider (matches reference image)
        float midX = mLeft + (mRight - mLeft) * 0.506f;

        // ================= ROW 1: EXPORTER & REFERENCES =================
        float row1Bottom = titleBottom + 104f;
        canvas.drawLine(mLeft, row1Bottom, mRight, row1Bottom, borderPaint);
        canvas.drawLine(midX, titleBottom, midX, row1Bottom, borderPaint);

        // Exporter (Left)
        canvas.drawText("Exporter", mLeft + 4, titleBottom + 10f, boldLargePaint);
        if (packingList.getExporter() != null) {
            String[] exporterLines = packingList.getExporter().getFormattedAddress().split("\n");
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
        canvas.drawText(packingList.getInvoiceNumber() != null ? packingList.getInvoiceNumber() : "", midX + 4, titleBottom + 20f, regularPaint);
        canvas.drawText("Exporter Ref No:", subMidX + 4, titleBottom + 9.5f, boldPaint);

        canvas.drawText("Buyer's Order no & Date", midX + 4, r1 + 9.5f, boldPaint);

        canvas.drawText("Other References", midX + 4, r2 + 9.5f, boldPaint);
        if (packingList.getExporter() != null) {
            canvas.drawText("RBI CODE NO: " + (packingList.getExporter().getRbiCode() != null ? packingList.getExporter().getRbiCode() : ""), midX + 4, r2 + 20f, regularPaint);
            canvas.drawText("IE CODE NO  : " + (packingList.getExporter().getIecCode() != null ? packingList.getExporter().getIecCode() : ""), midX + 4, r2 + 29.5f, regularPaint);
            canvas.drawText("GSTIN NO: " + (packingList.getExporter().getGstin() != null ? packingList.getExporter().getGstin() : ""), midX + 4, r2 + 39f, regularPaint);
        }

        // ================= ROW 2: CONSIGNEE & BUYER =================
        float row2Bottom = row1Bottom + 70f;
        canvas.drawLine(mLeft, row2Bottom, mRight, row2Bottom, borderPaint);
        canvas.drawLine(midX, row1Bottom, midX, row2Bottom, borderPaint);

        canvas.drawText("Consignee", mLeft + 4, row1Bottom + 10f, boldLargePaint);
        if (packingList.getConsignee() != null) {
            String[] consLines = packingList.getConsignee().getFormattedConsignee().split("\n");
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
        // Important: Horizontal line at sRow2 only crosses left side (Terms of Delivery box on right is uninterrupted)
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
        canvas.drawText(packingList.getCountryOfOrigin() != null ? packingList.getCountryOfOrigin() : "INDIA", midX + 4, row2Bottom + 20f, regularPaint);
        canvas.drawText("Country of Final Destination", colOriginDest + 4, row2Bottom + 9.5f, boldPaint);
        canvas.drawText(packingList.getCountryOfDestination() != null ? packingList.getCountryOfDestination() : "CHINA", colOriginDest + 4, row2Bottom + 20f, regularPaint);

        // Sub-row 3B text
        canvas.drawText("Vessel/Flight No", mLeft + 4, sRow1 + 9.5f, boldPaint);
        canvas.drawText("Port of Loading", col1 + 4, sRow1 + 9.5f, boldPaint);
        canvas.drawText(packingList.getPortOfLoading() != null ? packingList.getPortOfLoading() : "KAKINADA PORT", col1 + 4, sRow1 + 20f, regularPaint);

        canvas.drawText("Terms of Delivery and Payment", midX + 4, sRow1 + 9.5f, boldPaint);
        if (packingList.getTermsOfDeliveryPayment() != null) {
            String[] tLines = packingList.getTermsOfDeliveryPayment().split("\n");
            float ty = sRow1 + 20f;
            for (String t : tLines) {
                canvas.drawText(t, midX + 4, ty, regularPaint);
                ty += 9.5f;
            }
        }

        // Sub-row 3C text
        canvas.drawText("Port of discharge", mLeft + 4, sRow2 + 9.5f, boldPaint);
        canvas.drawText(packingList.getPortOfDischarge() != null ? packingList.getPortOfDischarge() : "", mLeft + 4, sRow2 + 20f, regularPaint);
        canvas.drawText("Final Destination", col1 + 4, sRow2 + 9.5f, boldPaint);
        canvas.drawText(packingList.getFinalDestination() != null ? packingList.getFinalDestination() : "", col1 + 4, sRow2 + 20f, regularPaint);

        // ================= ROW 4: PACKING LIST TABLE (3 COLUMNS) =================
        float colPlQty = midX;
        float colPlWeight = colPlQty + (mRight - colPlQty) * 0.50f;

        float plTotalsTop = mBottom - 78f;
        float plTotalsBottom = plTotalsTop + 24f;
        float plUnderlineY = plTotalsTop - 36f;

        // Vertical divider between Description and Quantity (runs from table top to bottom of totals row)
        canvas.drawLine(colPlQty, sRow3, colPlQty, plTotalsBottom, borderPaint);
        // Vertical divider between Quantity and Weight in MT (runs from table top down to intermediate underline)
        canvas.drawLine(colPlWeight, sRow3, colPlWeight, plUnderlineY, borderPaint);

        // Header labels inside the table (NO horizontal line under header, matches reference format)
        canvas.drawText("Marks & Nos/   No & Kind if Pkgs   Description of Goods", mLeft + 4, sRow3 + 10f, boldPaint);

        canvas.drawText("Quantity", (colPlQty + colPlWeight) / 2f, sRow3 + 9f, boldCenterPaint);
        canvas.drawText("CBM", (colPlQty + colPlWeight) / 2f, sRow3 + 19f, boldCenterPaint);

        canvas.drawText("Weight in MT", (colPlWeight + mRight) / 2f, sRow3 + 14f, boldCenterPaint);

        // Table Body Header in Description Column (NO underline in Packing List)
        float curY = sRow3 + 34f;
        String descHeader = packingList.getDescriptionHeader();
        if (descHeader != null && !descHeader.trim().isEmpty()) {
            String[] lines = descHeader.split("\n");
            for (String l : lines) {
                if (!l.trim().isEmpty()) {
                    canvas.drawText(l.trim(), mLeft + 4, curY, boldLargePaint);
                    curY += 11.5f;
                }
            }
        }
        curY = Math.max(curY + 6f, sRow3 + 55f);

        // Block Rows
        List<PackingListRow> rows = packingList.getRows();
        if (rows != null && !rows.isEmpty()) {
            float availableHeight = plUnderlineY - curY - 10f;
            float rowSpacing = Math.min(13.0f, Math.max(9.5f, availableHeight / rows.size()));

            for (PackingListRow r : rows) {
                // Block number (e.g. Block no. 138)
                canvas.drawText("Block no. " + r.getBlockNumber(), mLeft + 4, curY, regularPaint);

                // Separator dash
                canvas.drawText("-", mLeft + 84f, curY, regularPaint);

                // Dimensions with tabbed alignment: L x H x W
                String dims = String.format("%3d   x   %3d   x   %3d",
                        (int) r.getLengthCm(), (int) r.getHeightCm(), (int) r.getWidthCm());
                canvas.drawText(dims, mLeft + 98f, curY, regularPaint);

                // Quantity in CBM centered in column
                canvas.drawText(String.format("%.3f", r.getQuantityCbm()), (colPlQty + colPlWeight) / 2f, curY, regularCenterPaint);

                // Weight in MT (if provided)
                if (r.getWeightMt() > 0) {
                    canvas.drawText(String.format("%.3f", r.getWeightMt()), (colPlWeight + mRight) / 2f, curY, regularCenterPaint);
                }

                curY += rowSpacing;
            }
        }

        // Horizontal line across Quantity column closing the items section before totals
        canvas.drawLine(colPlQty, plUnderlineY, colPlWeight, plUnderlineY, borderPaint);

        // ================= ROW 5: TOTALS ROW =================
        canvas.drawLine(mLeft, plTotalsTop, mRight, plTotalsTop, borderPaint);
        canvas.drawLine(mLeft, plTotalsBottom, mRight, plTotalsBottom, borderPaint);

        int totalBlocks = packingList.getTotalBlocksCount() > 0 ? packingList.getTotalBlocksCount() : (rows != null ? rows.size() : 0);
        canvas.drawText("TOTAL NO. OF BLOCKS  : " + totalBlocks + " ONLY", mLeft + 4, plTotalsTop + 15f, boldLargePaint);
        canvas.drawText(String.format("%.3f", packingList.getTotalCbm()), (colPlQty + colPlWeight) / 2f, plTotalsTop + 15f, boldCenterPaint);

        // ================= ROW 6: SIGNATURE SECTION =================
        // Notice: NO vertical line dividing left and right below plTotalsBottom!
        // Left side remains open white space.
        // Right side contains "Signature and date" and exporter stamp
        float sigLabelX = colPlWeight - 25f;
        canvas.drawText("Signature and date", sigLabelX, plTotalsBottom + 11f, boldPaint);

        boolean stampRendered = false;
        try {
            Bitmap sigBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_signature_stamp);
            if (sigBitmap != null) {
                float stampWidth = 145f;
                float stampHeight = stampWidth * ((float) sigBitmap.getHeight() / (float) sigBitmap.getWidth());
                float sigLeft = sigLabelX + 10f;
                float sigTop = plTotalsBottom + 15f;
                RectF destRect = new RectF(sigLeft, sigTop, sigLeft + stampWidth, sigTop + stampHeight);
                canvas.drawBitmap(sigBitmap, null, destRect, null);
                stampRendered = true;
            }
        } catch (Exception ignored) {}

        if (!stampRendered) {
            // High-fidelity vector fallback
            float sx = sigLabelX + 15f;
            float sy = plTotalsBottom + 23f;
            String sigCompany = (packingList.getExporter() != null && !packingList.getExporter().getCompanyName().isEmpty())
                    ? "For " + packingList.getExporter().getCompanyName()
                    : (packingList.getSignatureTitle() != null ? packingList.getSignatureTitle() : "");
            if (!sigCompany.isEmpty()) {
                canvas.drawText(sigCompany, sx, sy, bluePaint);
            }

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
