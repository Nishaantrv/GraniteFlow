package com.graniteflow.app.pdf;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;

import com.graniteflow.app.data.model.Block;
import com.graniteflow.app.data.model.BlockPhoto;
import com.graniteflow.app.data.model.CompanyProfile;
import com.graniteflow.app.data.model.Inspection;
import com.graniteflow.app.data.repository.GraniteRepository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

/**
 * Inspection Report PDF Renderer - SRE Himalayan Blue replica.
 * Generates exact SRE-style multi-page inspection report:
 * 1. Abstract Summary table with material header, location, and grand total.
 * 2. Per-Block Measurement specification with crisp vector isometric diagrams (VIEW A & VIEW B) and remarks.
 * 3. High-resolution dedicated photo pages for attached block inspection angles (A SIDE, B SIDE, etc.).
 */
public class InspectionReportPdfRenderer {

    public static final int PAGE_WIDTH = 595;
    public static final int PAGE_HEIGHT = 842;

    private static void drawFormalPageBorder(Canvas canvas) {
        // Outer formal border (thick navy)
        Paint outerBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
        outerBorder.setColor(Color.parseColor("#0F2C59"));
        outerBorder.setStyle(Paint.Style.STROKE);
        outerBorder.setStrokeWidth(2.0f);
        canvas.drawRect(26f, 26f, PAGE_WIDTH - 26f, PAGE_HEIGHT - 26f, outerBorder);

        // Inner formal border (fine precision rule)
        Paint innerBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerBorder.setColor(Color.parseColor("#0F2C59"));
        innerBorder.setStyle(Paint.Style.STROKE);
        innerBorder.setStrokeWidth(0.8f);
        canvas.drawRect(30f, 30f, PAGE_WIDTH - 30f, PAGE_HEIGHT - 30f, innerBorder);
    }

    public static File renderInspectionReportPdf(Context context, Inspection inspection, String fileName) throws IOException {
        if (inspection != null && inspection.getBlocks() != null) {
            com.graniteflow.app.engine.BlockNumberRuleEngine.enforceSequentialBlockNumbers(
                    inspection.getBlocks(),
                    inspection.getStartingBlockNumber() > 0 ? inspection.getStartingBlockNumber() : 101);
        }

        PdfDocument document = new PdfDocument();
        int pageIndex = 1;

        // ================= PAGE 1: ABSTRACT / SUMMARY =================
        PdfDocument.PageInfo abstractPageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex++).create();
        PdfDocument.Page abstractPage = document.startPage(abstractPageInfo);
        renderAbstractPage(context, abstractPage.getCanvas(), inspection);
        document.finishPage(abstractPage);

        // ================= DETAILED BLOCK PAGES & PHOTO PAGES =================
        List<Block> blocks = inspection.getBlocks();
        if (blocks != null) {
            for (Block block : blocks) {
                // 1. Detailed measurement page for this block
                PdfDocument.PageInfo blockPageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex++).create();
                PdfDocument.Page blockPage = document.startPage(blockPageInfo);
                renderBlockMeasurementPage(blockPage.getCanvas(), block);
                document.finishPage(blockPage);

                // 2. Photo pages for this block
                List<BlockPhoto> photos = block.getPhotos();
                if (photos != null && !photos.isEmpty()) {
                    for (BlockPhoto photo : photos) {
                        PdfDocument.PageInfo photoPageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex++).create();
                        PdfDocument.Page photoPage = document.startPage(photoPageInfo);
                        renderPhotoPage(context, photoPage.getCanvas(), block, photo);
                        document.finishPage(photoPage);
                    }
                }
            }
        }

        File pdfFile = new File(context.getCacheDir(), fileName);
        FileOutputStream fos = new FileOutputStream(pdfFile);
        document.writeTo(fos);
        document.close();
        fos.close();

        return pdfFile;
    }

    private static void renderAbstractPage(Context context, Canvas canvas, Inspection inspection) {
        // Page background
        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.WHITE);
        canvas.drawRect(0, 0, PAGE_WIDTH, PAGE_HEIGHT, bgPaint);

        // Draw outer formal double border framing the whole page
        drawFormalPageBorder(canvas);

        // Outer margin guide within the border
        float mLeft = 40f;
        float mRight = PAGE_WIDTH - 40f;

        // Fonts & Paints
        Paint topMetaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        topMetaPaint.setColor(Color.parseColor("#334155"));
        topMetaPaint.setTextSize(9f);

        Paint companyTitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        companyTitlePaint.setColor(Color.parseColor("#0F2C59"));
        companyTitlePaint.setFakeBoldText(true);
        companyTitlePaint.setTextSize(15f);
        companyTitlePaint.setTextAlign(Paint.Align.CENTER);

        Paint companySubPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        companySubPaint.setColor(Color.parseColor("#475569"));
        companySubPaint.setTextSize(8f);
        companySubPaint.setTextAlign(Paint.Align.CENTER);

        Paint bannerBg = new Paint();
        bannerBg.setColor(Color.parseColor("#0F2C59"));
        bannerBg.setStyle(Paint.Style.FILL);

        Paint bannerText = new Paint(Paint.ANTI_ALIAS_FLAG);
        bannerText.setColor(Color.WHITE);
        bannerText.setFakeBoldText(true);
        bannerText.setTextSize(10.5f);
        bannerText.setTextAlign(Paint.Align.CENTER);

        Paint borderPaint = new Paint();
        borderPaint.setColor(Color.parseColor("#94A3B8"));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(0.8f);

        Paint ruleThick = new Paint();
        ruleThick.setColor(Color.parseColor("#0F2C59"));
        ruleThick.setStrokeWidth(1.4f);

        Paint ruleThin = new Paint();
        ruleThin.setColor(Color.parseColor("#0F2C59"));
        ruleThin.setStrokeWidth(0.6f);

        Paint metaBg = new Paint();
        metaBg.setColor(Color.parseColor("#F8FAFC"));
        metaBg.setStyle(Paint.Style.FILL);

        Paint metaLabel = new Paint(Paint.ANTI_ALIAS_FLAG);
        metaLabel.setColor(Color.parseColor("#475569"));
        metaLabel.setFakeBoldText(true);
        metaLabel.setTextSize(7.8f);

        Paint metaVal = new Paint(Paint.ANTI_ALIAS_FLAG);
        metaVal.setColor(Color.parseColor("#0F172A"));
        metaVal.setTextSize(8.2f);

        Paint tableHeaderBg = new Paint();
        tableHeaderBg.setColor(Color.parseColor("#0F2C59"));
        tableHeaderBg.setStyle(Paint.Style.FILL);

        Paint tableHeaderText = new Paint(Paint.ANTI_ALIAS_FLAG);
        tableHeaderText.setColor(Color.WHITE);
        tableHeaderText.setFakeBoldText(true);
        tableHeaderText.setTextSize(8f);

        Paint rowTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rowTextPaint.setColor(Color.parseColor("#1E293B"));
        rowTextPaint.setTextSize(8.5f);

        Paint boldRowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        boldRowPaint.setColor(Color.parseColor("#0F2C59"));
        boldRowPaint.setFakeBoldText(true);
        boldRowPaint.setTextSize(8.5f);

        float y = 50f;

        // Top line: Ref on left, Date on right
        String dateStr = (inspection.getDate() != null && !inspection.getDate().isEmpty())
                ? inspection.getDate() : "16/10/2026";
        String refStr = (inspection.getInspectionId() != null && !inspection.getInspectionId().isEmpty())
                ? "REF: " + inspection.getInspectionId() : "REF: GF-IR-2026-EXPORT";

        topMetaPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(refStr, mLeft, y, topMetaPaint);
        topMetaPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("DATE: " + dateStr, mRight, y, topMetaPaint);

        y += 18f;

        // Official Company Header / Letterhead (clean without spaced chars)
        CompanyProfile cp = GraniteRepository.getInstance(context).getCompanyProfile();
        String compName = (cp != null && cp.getCompanyName() != null && !cp.getCompanyName().isEmpty())
                ? cp.getCompanyName().trim().toUpperCase() : "";
        canvas.drawText(compName, PAGE_WIDTH / 2f, y, companyTitlePaint);

        y += 13f;
        canvas.drawText("QUARRYING, PROCESSING & NATURAL STONE EXPORT DIVISION", PAGE_WIDTH / 2f, y, companySubPaint);

        y += 7f;
        // Double formal divider line
        canvas.drawLine(mLeft, y, mRight, y, ruleThick);
        canvas.drawLine(mLeft, y + 2.5f, mRight, y + 2.5f, ruleThin);

        y += 10f;
        // Formal Title Banner
        RectF bannerRect = new RectF(mLeft, y, mRight, y + 22f);
        canvas.drawRect(bannerRect, bannerBg);
        canvas.drawText("QUARRY BLOCK INSPECTION REPORT - ABSTRACT", bannerRect.centerX(), bannerRect.centerY() + 3.8f, bannerText);

        y += 28f;
        // 2-Column Structured Metadata Box
        float metaBoxH = 44f;
        RectF metaRect = new RectF(mLeft, y, mRight, y + metaBoxH);
        canvas.drawRect(metaRect, metaBg);
        canvas.drawRect(metaRect, borderPaint);
        canvas.drawLine(PAGE_WIDTH / 2f, y, PAGE_WIDTH / 2f, y + metaBoxH, borderPaint);
        canvas.drawLine(mLeft, y + 22f, mRight, y + 22f, borderPaint);

        String buyer = (inspection.getBuyer() != null && !inspection.getBuyer().isEmpty())
                ? inspection.getBuyer() : "EXPORT CONSIGNMENT / GENERAL";
        String mat = inspection.getStoneDescription() != null && !inspection.getStoneDescription().isEmpty()
                ? inspection.getStoneDescription()
                : (inspection.getMaterialName() != null ? inspection.getMaterialName() : "SRE HIMALAYAN BLUE");
        String loc = inspection.getLocationCode() != null && !inspection.getLocationCode().isEmpty()
                ? inspection.getLocationCode() : "XOW / XMN";
        String eng = inspection.getScanEngineUsed() != null && !inspection.getScanEngineUsed().isEmpty()
                ? inspection.getScanEngineUsed() : "GraniteFlow Vision AI";

        // Left Col
        canvas.drawText("BUYER / CONSIGNEE:", mLeft + 8f, y + 14.5f, metaLabel);
        canvas.drawText(buyer, mLeft + 105f, y + 14.5f, metaVal);
        canvas.drawText("MATERIAL / STONE:", mLeft + 8f, y + 36.5f, metaLabel);
        canvas.drawText(mat, mLeft + 105f, y + 36.5f, metaVal);

        // Right Col
        float col2X = PAGE_WIDTH / 2f + 8f;
        canvas.drawText("PORT / LOCATION:", col2X, y + 14.5f, metaLabel);
        canvas.drawText(loc, col2X + 90f, y + 14.5f, metaVal);
        canvas.drawText("INGESTION ENGINE:", col2X, y + 36.5f, metaLabel);
        canvas.drawText(eng, col2X + 90f, y + 36.5f, metaVal);

        y += metaBoxH + 10f;

        // Abstract Measurement Table
        float tLeft = mLeft;
        float tRight = mRight;
        float colSr = tLeft + 36f;
        float colBlock = colSr + 75f;
        float colMeas = colBlock + 185f;
        float colAllow = colMeas + 95f;
        float thHeight = 20f;

        // Draw Table Header
        canvas.drawRect(tLeft, y, tRight, y + thHeight, tableHeaderBg);
        canvas.drawRect(tLeft, y, tRight, y + thHeight, borderPaint);

        // Header dividers
        canvas.drawLine(colSr, y, colSr, y + thHeight, borderPaint);
        canvas.drawLine(colBlock, y, colBlock, y + thHeight, borderPaint);
        canvas.drawLine(colMeas, y, colMeas, y + thHeight, borderPaint);
        canvas.drawLine(colAllow, y, colAllow, y + thHeight, borderPaint);

        tableHeaderText.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("SR NO", (tLeft + colSr) / 2f, y + 13.5f, tableHeaderText);
        canvas.drawText("BLOCK NO", (colSr + colBlock) / 2f, y + 13.5f, tableHeaderText);
        canvas.drawText("NET MEASUREMENTS (L x H x W CM)", (colBlock + colMeas) / 2f, y + 13.5f, tableHeaderText);
        canvas.drawText("ALLOWANCE", (colMeas + colAllow) / 2f, y + 13.5f, tableHeaderText);
        canvas.drawText("NET VOL (M³)", (colAllow + tRight) / 2f, y + 13.5f, tableHeaderText);

        y += thHeight;

        List<Block> blocks = inspection.getBlocks();
        int sr = 1;
        float rowH = 17.5f;
        double totalVolume = 0.0;

        if (blocks != null && !blocks.isEmpty()) {
            for (Block b : blocks) {
                totalVolume += b.getNetCbm();

                // Zebra row tint
                if (sr % 2 == 0) {
                    Paint zebra = new Paint();
                    zebra.setColor(Color.parseColor("#F8FAFC"));
                    canvas.drawRect(tLeft, y, tRight, y + rowH, zebra);
                }

                canvas.drawRect(tLeft, y, tRight, y + rowH, borderPaint);
                canvas.drawLine(colSr, y, colSr, y + rowH, borderPaint);
                canvas.drawLine(colBlock, y, colBlock, y + rowH, borderPaint);
                canvas.drawLine(colMeas, y, colMeas, y + rowH, borderPaint);
                canvas.drawLine(colAllow, y, colAllow, y + rowH, borderPaint);

                // Row content
                rowTextPaint.setTextAlign(Paint.Align.CENTER);
                boldRowPaint.setTextAlign(Paint.Align.CENTER);

                canvas.drawText(String.valueOf(sr), (tLeft + colSr) / 2f, y + 12f, rowTextPaint);
                canvas.drawText(b.getBlockNumber(), (colSr + colBlock) / 2f, y + 12f, boldRowPaint);

                String netDim = String.format(Locale.ENGLISH, "%.0f x %.0f x %.0f",
                        b.getNetLengthCm(), b.getNetHeightCm(), b.getNetWidthCm());
                canvas.drawText(netDim, (colBlock + colMeas) / 2f, y + 12f, rowTextPaint);

                canvas.drawText((int) b.getAllowanceCm() + " cm", (colMeas + colAllow) / 2f, y + 12f, rowTextPaint);

                boldRowPaint.setTextAlign(Paint.Align.RIGHT);
                canvas.drawText(String.format(Locale.ENGLISH, "%.3f", b.getNetCbm()), tRight - 16f, y + 12f, boldRowPaint);

                y += rowH;
                sr++;
                if (y > PAGE_HEIGHT - 170f) break;
            }
        }

        // Subtotal Summary Bar
        int count = blocks != null ? blocks.size() : 0;
        Paint subtotalBg = new Paint();
        subtotalBg.setColor(Color.parseColor("#F1F5F9"));
        canvas.drawRect(tLeft, y, tRight, y + 18f, subtotalBg);
        canvas.drawRect(tLeft, y, tRight, y + 18f, borderPaint);

        Paint subtotalText = new Paint(Paint.ANTI_ALIAS_FLAG);
        subtotalText.setColor(Color.parseColor("#0F2C59"));
        subtotalText.setFakeBoldText(true);
        subtotalText.setTextSize(9f);

        subtotalText.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("TOTAL INSPECTED (" + count + " BLOCKS)", tLeft + 10f, y + 12.5f, subtotalText);

        subtotalText.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(String.format(Locale.ENGLISH, "%.3f M³", totalVolume), tRight - 16f, y + 12.5f, subtotalText);

        y += 18f + 12f;

        // Grand Total Highlight Box
        float boxWidth = 340f;
        float boxHeight = 28f;
        float boxLeft = (PAGE_WIDTH - boxWidth) / 2f;
        RectF gtBox = new RectF(boxLeft, y, boxLeft + boxWidth, y + boxHeight);

        Paint gtBg = new Paint();
        gtBg.setColor(Color.parseColor("#E0F2FE"));
        canvas.drawRoundRect(gtBox, 4, 4, gtBg);

        Paint gtBorder = new Paint();
        gtBorder.setColor(Color.parseColor("#0F2C59"));
        gtBorder.setStyle(Paint.Style.STROKE);
        gtBorder.setStrokeWidth(1.2f);
        canvas.drawRoundRect(gtBox, 4, 4, gtBorder);

        Paint gtText = new Paint(Paint.ANTI_ALIAS_FLAG);
        gtText.setColor(Color.parseColor("#0F2C59"));
        gtText.setFakeBoldText(true);
        gtText.setTextSize(10.5f);
        gtText.setTextAlign(Paint.Align.CENTER);

        String grandTotalStr = String.format(Locale.ENGLISH, "GRAND TOTAL (%d BLOCKS):  %.3f M³", count, totalVolume);
        canvas.drawText(grandTotalStr, gtBox.centerX(), gtBox.centerY() + 3.8f, gtText);

        // Dual Official Sign-off Blocks at bottom
        float signTop = Math.max(y + boxHeight + 14f, PAGE_HEIGHT - 135f);
        float signH = 68f;
        float signMid = PAGE_WIDTH / 2f;

        Paint signBg = new Paint();
        signBg.setColor(Color.parseColor("#FAFAFA"));

        Paint signHdrBg = new Paint();
        signHdrBg.setColor(Color.parseColor("#F1F5F9"));

        Paint signHdrText = new Paint(Paint.ANTI_ALIAS_FLAG);
        signHdrText.setColor(Color.parseColor("#0F2C59"));
        signHdrText.setFakeBoldText(true);
        signHdrText.setTextSize(7.8f);
        signHdrText.setTextAlign(Paint.Align.CENTER);

        Paint signBody = new Paint(Paint.ANTI_ALIAS_FLAG);
        signBody.setColor(Color.parseColor("#334155"));
        signBody.setTextSize(7.8f);

        Paint signMuted = new Paint(Paint.ANTI_ALIAS_FLAG);
        signMuted.setColor(Color.parseColor("#64748B"));
        signMuted.setTextSize(7.2f);

        // Left Box: Certified Inspector
        RectF leftSignBox = new RectF(mLeft, signTop, signMid - 8f, signTop + signH);
        canvas.drawRect(leftSignBox, signBg);
        canvas.drawRect(leftSignBox, borderPaint);
        canvas.drawRect(leftSignBox.left, leftSignBox.top, leftSignBox.right, leftSignBox.top + 15f, signHdrBg);
        canvas.drawRect(leftSignBox.left, leftSignBox.top, leftSignBox.right, leftSignBox.top + 15f, borderPaint);
        canvas.drawText("INSPECTED & CERTIFIED BY", leftSignBox.centerX(), leftSignBox.top + 11f, signHdrText);

        canvas.drawText("Lead Quarry Surveyor: _______________________", leftSignBox.left + 8f, leftSignBox.top + 34f, signBody);
        canvas.drawText("GraniteFlow Verified Survey & Measurement Unit", leftSignBox.left + 8f, leftSignBox.top + 48f, signMuted);
        canvas.drawText("Inspection Survey Date: " + dateStr, leftSignBox.left + 8f, leftSignBox.top + 60f, signMuted);

        // Right Box: Quarry Exporter
        RectF rightSignBox = new RectF(signMid + 8f, signTop, mRight, signTop + signH);
        canvas.drawRect(rightSignBox, signBg);
        canvas.drawRect(rightSignBox, borderPaint);
        canvas.drawRect(rightSignBox.left, rightSignBox.top, rightSignBox.right, rightSignBox.top + 15f, signHdrBg);
        canvas.drawRect(rightSignBox.left, rightSignBox.top, rightSignBox.right, rightSignBox.top + 15f, borderPaint);
        canvas.drawText("FOR & ON BEHALF OF EXPORTER", rightSignBox.centerX(), rightSignBox.top + 11f, signHdrText);

        canvas.drawText("Authorized Signatory: _______________________", rightSignBox.left + 8f, rightSignBox.top + 34f, signBody);
        canvas.drawText(compName, rightSignBox.left + 8f, rightSignBox.top + 48f, signBody);
        canvas.drawText("Official Quarry Seal & Assurance", rightSignBox.left + 8f, rightSignBox.top + 60f, signMuted);

        // Document Footer
        Paint footPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        footPaint.setColor(Color.parseColor("#94A3B8"));
        footPaint.setTextSize(7.5f);

        canvas.drawLine(mLeft, PAGE_HEIGHT - 44f, mRight, PAGE_HEIGHT - 44f, borderPaint);
        footPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("Official Quarry Inspection Abstract • Certified Dimensional Record", mLeft, PAGE_HEIGHT - 34f, footPaint);
        footPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("GraniteFlow Professional Mobile Suite", mRight, PAGE_HEIGHT - 34f, footPaint);
    }

    private static void renderBlockMeasurementPage(Canvas canvas, Block block) {
        // Page background
        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.WHITE);
        canvas.drawRect(0, 0, PAGE_WIDTH, PAGE_HEIGHT, bgPaint);

        // Draw outer formal double border framing the page
        drawFormalPageBorder(canvas);

        float mLeft = 50f;
        float mRight = PAGE_WIDTH - 50f;
        float y = 70f;

        // Block Title: BLOCK NO : 187
        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(Color.BLACK);
        titlePaint.setFakeBoldText(true);
        titlePaint.setTextSize(16f);
        titlePaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("BLOCK NO : " + block.getBlockNumber(), PAGE_WIDTH / 2f, y + 16, titlePaint);

        y += 36f;

        // Measurement Table
        float tLeft = mLeft;
        float tRight = mRight;
        float tableW = tRight - tLeft;
        float colW0 = tableW * 0.22f; // MEASUREMENT
        float colW1 = tableW * 0.15f; // LENGTH
        float colW2 = tableW * 0.15f; // HEIGHT
        float colW3 = tableW * 0.15f; // WIDTH
        float colW4 = tableW * 0.17f; // ALLOWANCE
        float colW5 = tableW * 0.16f; // NET CBM

        float c0 = tLeft;
        float c1 = c0 + colW0;
        float c2 = c1 + colW1;
        float c3 = c2 + colW2;
        float c4 = c3 + colW3;
        float c5 = c4 + colW4;

        float thH = 28f;
        float rowH = 26f;

        Paint borderThin = new Paint();
        borderThin.setColor(Color.BLACK);
        borderThin.setStyle(Paint.Style.STROKE);
        borderThin.setStrokeWidth(0.9f);

        Paint thBg = new Paint();
        thBg.setColor(Color.parseColor("#E0F2FE"));

        Paint netBg = new Paint();
        netBg.setColor(Color.parseColor("#E8F5E9"));

        // Header background
        canvas.drawRect(tLeft, y, tRight, y + thH, thBg);
        canvas.drawRect(tLeft, y, tRight, y + thH + rowH * 2, borderThin);

        // Header dividers
        canvas.drawLine(c1, y, c1, y + thH + rowH * 2, borderThin);
        canvas.drawLine(c2, y, c2, y + thH + rowH * 2, borderThin);
        canvas.drawLine(c3, y, c3, y + thH + rowH * 2, borderThin);
        canvas.drawLine(c4, y, c4, y + thH + rowH * 2, borderThin);
        canvas.drawLine(c5, y, c5, y + thH + rowH * 2, borderThin);

        // Row dividers
        canvas.drawLine(tLeft, y + thH, tRight, y + thH, borderThin);
        canvas.drawLine(tLeft, y + thH + rowH, tRight, y + thH + rowH, borderThin);

        Paint thText = new Paint(Paint.ANTI_ALIAS_FLAG);
        thText.setColor(Color.parseColor("#0F2C59"));
        thText.setFakeBoldText(true);
        thText.setTextSize(7.5f);
        thText.setTextAlign(Paint.Align.CENTER);

        canvas.drawText("MEASUREMENT", (c0 + c1) / 2f, y + 17f, thText);
        canvas.drawText("LENGTH (CM)", (c1 + c2) / 2f, y + 17f, thText);
        canvas.drawText("HEIGHT (CM)", (c2 + c3) / 2f, y + 17f, thText);
        canvas.drawText("WIDTH (CM)", (c3 + c4) / 2f, y + 17f, thText);
        canvas.drawText("ALLOWANCE", (c4 + c5) / 2f, y + 17f, thText);
        canvas.drawText("NET CBM", (c5 + tRight) / 2f, y + 17f, thText);

        // GROSS Row
        float grossY = y + thH;
        Paint cellText = new Paint(Paint.ANTI_ALIAS_FLAG);
        cellText.setColor(Color.BLACK);
        cellText.setTextSize(9.5f);
        cellText.setTextAlign(Paint.Align.CENTER);

        Paint cellBold = new Paint(Paint.ANTI_ALIAS_FLAG);
        cellBold.setColor(Color.BLACK);
        cellBold.setFakeBoldText(true);
        cellBold.setTextSize(9.5f);
        cellBold.setTextAlign(Paint.Align.CENTER);

        canvas.drawText("GROSS", (c0 + c1) / 2f, grossY + 17f, cellBold);
        canvas.drawText(String.valueOf((int) block.getGrossLengthCm()), (c1 + c2) / 2f, grossY + 17f, cellText);
        canvas.drawText(String.valueOf((int) block.getGrossHeightCm()), (c2 + c3) / 2f, grossY + 17f, cellText);
        canvas.drawText(String.valueOf((int) block.getGrossWidthCm()), (c3 + c4) / 2f, grossY + 17f, cellText);
        canvas.drawText((int) block.getAllowanceCm() + " CM", (c4 + c5) / 2f, grossY + 17f, cellText);
        canvas.drawText(String.format(Locale.ENGLISH, "%.3f", block.getGrossCbm()), (c5 + tRight) / 2f, grossY + 17f, cellText);

        // NET Row (tinted pale green)
        float netY = grossY + rowH;
        canvas.drawRect(tLeft, netY, tRight, netY + rowH, netBg);
        canvas.drawRect(tLeft, netY, tRight, netY + rowH, borderThin);
        canvas.drawLine(c1, netY, c1, netY + rowH, borderThin);
        canvas.drawLine(c2, netY, c2, netY + rowH, borderThin);
        canvas.drawLine(c3, netY, c3, netY + rowH, borderThin);
        canvas.drawLine(c4, netY, c4, netY + rowH, borderThin);
        canvas.drawLine(c5, netY, c5, netY + rowH, borderThin);

        Paint greenBold = new Paint(Paint.ANTI_ALIAS_FLAG);
        greenBold.setColor(Color.parseColor("#15803D"));
        greenBold.setFakeBoldText(true);
        greenBold.setTextSize(10f);
        greenBold.setTextAlign(Paint.Align.CENTER);

        canvas.drawText("NET", (c0 + c1) / 2f, netY + 17f, greenBold);
        canvas.drawText(String.valueOf((int) block.getNetLengthCm()), (c1 + c2) / 2f, netY + 17f, greenBold);
        canvas.drawText(String.valueOf((int) block.getNetHeightCm()), (c2 + c3) / 2f, netY + 17f, greenBold);
        canvas.drawText(String.valueOf((int) block.getNetWidthCm()), (c3 + c4) / 2f, netY + 17f, greenBold);
        canvas.drawText((int) block.getAllowanceCm() + " CM", (c4 + c5) / 2f, netY + 17f, cellText);
        canvas.drawText(String.format(Locale.ENGLISH, "%.3f", block.getNetCbm()), (c5 + tRight) / 2f, netY + 17f, greenBold);

        y = netY + rowH + 28f;

        // Card 2: Diagrams Box (VIEW A & VIEW B - SRE clean wireframe cuboid)
        float diagramBoxH = 200f;
        RectF diagBox = new RectF(tLeft, y, tRight, y + diagramBoxH);

        Paint diagBoxBorder = new Paint();
        diagBoxBorder.setColor(Color.parseColor("#94A3B8"));
        diagBoxBorder.setStyle(Paint.Style.STROKE);
        diagBoxBorder.setStrokeWidth(1.0f);
        canvas.drawRoundRect(diagBox, 8, 8, diagBoxBorder);

        Paint viewHeader = new Paint(Paint.ANTI_ALIAS_FLAG);
        viewHeader.setColor(Color.parseColor("#475569"));
        viewHeader.setFakeBoldText(true);
        viewHeader.setTextSize(11f);
        viewHeader.setTextAlign(Paint.Align.CENTER);

        float midX = (tLeft + tRight) / 2f;
        canvas.drawText("VIEW A", (tLeft + midX) / 2f, y + 24f, viewHeader);
        canvas.drawText("VIEW B", (midX + tRight) / 2f, y + 24f, viewHeader);

        // Vector Wireframe Cuboids exactly matching SRE report
        drawSreCuboidA(canvas, (tLeft + midX) / 2f, y + 70f, block);
        drawSreCuboidB(canvas, (midX + tRight) / 2f, y + 70f, block);

        y += diagramBoxH + 24f;

        // Card 3: REMARKS box (Matching SRE report)
        float remarksH = 65f;
        RectF remBox = new RectF(tLeft, y, tRight, y + remarksH);
        canvas.drawRoundRect(remBox, 6, 6, borderThin);

        Paint tabPaint = new Paint();
        tabPaint.setColor(Color.BLACK);
        canvas.drawRect(tLeft + 12, y - 9, tLeft + 75, y + 5, tabPaint);

        Paint tabText = new Paint(Paint.ANTI_ALIAS_FLAG);
        tabText.setColor(Color.WHITE);
        tabText.setFakeBoldText(true);
        tabText.setTextSize(7.5f);
        canvas.drawText("REMARKS", tLeft + 20, y + 1, tabText);

        Paint remBody = new Paint(Paint.ANTI_ALIAS_FLAG);
        remBody.setColor(Color.BLACK);
        remBody.setTextSize(9.5f);
        String rem = (block.getRemarks() != null && !block.getRemarks().isEmpty()) ? block.getRemarks() : "-";
        canvas.drawText(rem, tLeft + 16, y + 36, remBody);
    }

    private static void drawSreCuboidA(Canvas canvas, float centerX, float topY, Block block) {
        Paint wirePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        wirePaint.setColor(Color.BLACK);
        wirePaint.setStyle(Paint.Style.STROKE);
        wirePaint.setStrokeWidth(1.4f);

        Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(Color.BLACK);
        labelPaint.setFakeBoldText(true);
        labelPaint.setTextSize(9.5f);

        float fw = 58f;
        float fh = 52f;
        float dx = 42f;
        float dy = 28f;

        float fx = centerX - fw / 2f + 16f;
        float fy = topY + 40f;

        // Front Face
        canvas.drawRect(fx, fy, fx + fw, fy + fh, wirePaint);

        // Top Face (slants up and left)
        canvas.drawLine(fx, fy, fx - dx, fy - dy, wirePaint);
        canvas.drawLine(fx + fw, fy, fx + fw - dx, fy - dy, wirePaint);
        canvas.drawLine(fx - dx, fy - dy, fx + fw - dx, fy - dy, wirePaint);

        // Left Face
        canvas.drawLine(fx, fy + fh, fx - dx, fy + fh - dy, wirePaint);
        canvas.drawLine(fx - dx, fy - dy, fx - dx, fy + fh - dy, wirePaint);

        // Labels
        labelPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("L " + (int) block.getGrossLengthCm(), fx - dx - 4f, fy - dy + 14f, labelPaint);

        labelPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("H " + (int) block.getGrossHeightCm(), fx + fw + 8f, fy + fh / 2f + 4f, labelPaint);

        labelPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("W " + (int) block.getGrossWidthCm(), fx + fw / 2f, fy + fh + 18f, labelPaint);
    }

    private static void drawSreCuboidB(Canvas canvas, float centerX, float topY, Block block) {
        Paint wirePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        wirePaint.setColor(Color.BLACK);
        wirePaint.setStyle(Paint.Style.STROKE);
        wirePaint.setStrokeWidth(1.4f);

        Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(Color.BLACK);
        labelPaint.setFakeBoldText(true);
        labelPaint.setTextSize(9.5f);

        float fw = 58f;
        float fh = 52f;
        float dx = 42f;
        float dy = 28f;

        float fx = centerX - fw / 2f - 16f;
        float fy = topY + 40f;

        // Front Face
        canvas.drawRect(fx, fy, fx + fw, fy + fh, wirePaint);

        // Top Face (slants up and right)
        canvas.drawLine(fx, fy, fx + dx, fy - dy, wirePaint);
        canvas.drawLine(fx + fw, fy, fx + fw + dx, fy - dy, wirePaint);
        canvas.drawLine(fx + dx, fy - dy, fx + fw + dx, fy - dy, wirePaint);

        // Right Face
        canvas.drawLine(fx + fw, fy + fh, fx + fw + dx, fy + fh - dy, wirePaint);
        canvas.drawLine(fx + fw + dx, fy - dy, fx + fw + dx, fy + fh - dy, wirePaint);

        // Labels
        labelPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("L " + (int) block.getGrossLengthCm(), fx + fw + dx + 4f, fy - dy + 14f, labelPaint);

        labelPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("H " + (int) block.getGrossHeightCm(), fx - 8f, fy + fh / 2f + 4f, labelPaint);

        labelPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("W " + (int) block.getGrossWidthCm(), fx + fw / 2f, fy + fh + 18f, labelPaint);
    }

    private static void renderPhotoPage(Context context, Canvas canvas, Block block, BlockPhoto photo) {
        // Page background
        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.WHITE);
        canvas.drawRect(0, 0, PAGE_WIDTH, PAGE_HEIGHT, bgPaint);

        // Draw outer formal double border framing the photo page
        drawFormalPageBorder(canvas);

        // Title: BLOCK NO : 187  •  A SIDE
        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(Color.parseColor("#0F2C59"));
        titlePaint.setFakeBoldText(true);
        titlePaint.setTextSize(14f);
        titlePaint.setTextAlign(Paint.Align.CENTER);

        String sideLabel = (photo.getSideLabel() != null && !photo.getSideLabel().isEmpty())
                ? photo.getSideLabel().toUpperCase() : "A SIDE";
        String blockNo = block != null ? block.getBlockNumber() : photo.getBlockNumber();
        String title = "BLOCK NO : " + blockNo + "  •  " + sideLabel;
        canvas.drawText(title, PAGE_WIDTH / 2f, 75f, titlePaint);

        // Large photo frame container
        float frameLeft = 65f;
        float frameTop = 130f;
        float frameRight = PAGE_WIDTH - 65f;
        float frameBottom = PAGE_HEIGHT - 90f;
        RectF frameRect = new RectF(frameLeft, frameTop, frameRight, frameBottom);

        Paint frameBorder = new Paint();
        frameBorder.setColor(Color.parseColor("#0F172A"));
        frameBorder.setStyle(Paint.Style.STROKE);
        frameBorder.setStrokeWidth(1.2f);
        canvas.drawRect(frameRect, frameBorder);

        // Load image from persistent local file or URI
        Bitmap bitmap = null;
        if (photo.getLocalUri() != null && !photo.getLocalUri().trim().isEmpty()) {
            String uriStr = photo.getLocalUri().trim();
            try {
                File file = new File(uriStr);
                if (file.exists() && file.length() > 0) {
                    bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                } else if (uriStr.startsWith("content://") || uriStr.startsWith("file://")) {
                    try (InputStream is = context.getContentResolver().openInputStream(Uri.parse(uriStr))) {
                        bitmap = BitmapFactory.decodeStream(is);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (bitmap != null) {
            float availableW = frameRight - frameLeft - 8f;
            float availableH = frameBottom - frameTop - 8f;
            float scale = Math.min(availableW / bitmap.getWidth(), availableH / bitmap.getHeight());

            int drawW = Math.round(bitmap.getWidth() * scale);
            int drawH = Math.round(bitmap.getHeight() * scale);

            int drawLeft = (int) (frameLeft + 4f + (availableW - drawW) / 2f);
            int drawTop = (int) (frameTop + 4f + (availableH - drawH) / 2f);

            Rect src = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
            Rect dest = new Rect(drawLeft, drawTop, drawLeft + drawW, drawTop + drawH);

            Paint bmPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            canvas.drawBitmap(bitmap, src, dest, bmPaint);
        } else {
            // Muted placeholder if no photo was saved
            Paint phPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            phPaint.setColor(Color.parseColor("#94A3B8"));
            phPaint.setTextSize(11f);
            phPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("[ Attached Inspection Photo: " + sideLabel + " ]",
                    frameRect.centerX(), frameRect.centerY(), phPaint);
        }
    }

    private static String spacedString(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            sb.append(c);
            if (i < text.length() - 1 && c != ' ') {
                sb.append(' ');
            }
        }
        return sb.toString();
    }
}
