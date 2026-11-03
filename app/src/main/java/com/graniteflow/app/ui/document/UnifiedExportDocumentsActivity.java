package com.graniteflow.app.ui.document;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.graniteflow.app.R;
import com.graniteflow.app.data.model.Block;
import com.graniteflow.app.data.model.BlockTypePreset;
import com.graniteflow.app.data.model.CompanyProfile;
import com.graniteflow.app.data.model.Inspection;
import com.graniteflow.app.data.model.InvoiceDocument;
import com.graniteflow.app.data.model.PackingListDocument;
import com.graniteflow.app.data.repository.GraniteRepository;
import com.graniteflow.app.engine.CurrencyWordsConverter;
import com.graniteflow.app.pdf.InspectionReportPdfRenderer;
import com.graniteflow.app.pdf.InvoicePdfRenderer;
import com.graniteflow.app.pdf.PackingListPdfRenderer;
import com.graniteflow.app.ui.inspection.BlockDiagramView;
import com.graniteflow.app.ui.inspection.BlockListEditActivity;
import com.graniteflow.app.ui.inspection.FinishDetailsActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UnifiedExportDocumentsActivity extends AppCompatActivity {

    public static final String EXTRA_INSPECTION_ID = "extra_inspection_id";
    public static final String EXTRA_INITIAL_TAB = "extra_initial_tab";
    public static final int TAB_PACKING_LIST = 2;
    public static final int TAB_INVOICE = 4;

    private static final int DOC_INSPECTION_REPORT = 0;
    private static final int DOC_GROSS_PACKING = 1;
    private static final int DOC_NET_PACKING = 2;
    private static final int DOC_GROSS_INVOICE = 3;
    private static final int DOC_NET_INVOICE = 4;

    private GraniteRepository repository;
    private Inspection currentInspection;
    private CompanyProfile companyProfile;
    private int activeDocTab = DOC_INSPECTION_REPORT;

    private TextView tabDocInspectionReport, tabDocGrossPacking, tabDocNetPacking, tabDocGrossInvoice, tabDocNetInvoice;
    private LinearLayout llDocumentSheetContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unified_export_documents);

        repository = GraniteRepository.getInstance(this);
        companyProfile = repository.getCompanyProfile();

        String inspId = getIntent().getStringExtra(EXTRA_INSPECTION_ID);
        if (inspId != null) {
            currentInspection = repository.getInspectionById(inspId);
        }
        if (currentInspection == null) {
            currentInspection = repository.getActiveInspection();
        }
        if (currentInspection == null) {
            currentInspection = new Inspection();
            repository.saveInspection(currentInspection);
        }

        initViews();
        int initialTab = getIntent().getIntExtra(EXTRA_INITIAL_TAB, DOC_INSPECTION_REPORT);
        selectDocTab(initialTab);
    }

    @Override
    protected void onResume() {
        super.onResume();
        companyProfile = repository.getCompanyProfile();
        if (currentInspection != null) {
            Inspection refreshed = repository.getInspectionById(currentInspection.getInspectionId());
            if (refreshed != null) {
                currentInspection = refreshed;
            }
        }
        renderActiveDocument();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btnExportBack);
        btnBack.setOnClickListener(v -> finish());

        Button btnEditDoc = findViewById(R.id.btnExportEditDoc);
        btnEditDoc.setOnClickListener(v -> showEditOptionsDialog());

        Button btnPrintPdf = findViewById(R.id.btnExportPrintPdf);
        btnPrintPdf.setOnClickListener(v -> exportActivePdf());

        Button btnBottomEditBlocks = findViewById(R.id.btnBottomEditBlocks);
        btnBottomEditBlocks.setOnClickListener(v -> {
            Intent intent = new Intent(this, BlockListEditActivity.class);
            intent.putExtra(BlockListEditActivity.EXTRA_INSPECTION_ID, currentInspection.getInspectionId());
            startActivity(intent);
        });

        Button btnBottomExportPdf = findViewById(R.id.btnBottomExportPdf);
        btnBottomExportPdf.setOnClickListener(v -> exportActivePdf());

        tabDocInspectionReport = findViewById(R.id.tabDocInspectionReport);
        tabDocGrossPacking = findViewById(R.id.tabDocGrossPacking);
        tabDocNetPacking = findViewById(R.id.tabDocNetPacking);
        tabDocGrossInvoice = findViewById(R.id.tabDocGrossInvoice);
        tabDocNetInvoice = findViewById(R.id.tabDocNetInvoice);

        llDocumentSheetContainer = findViewById(R.id.llDocumentSheetContainer);

        tabDocInspectionReport.setOnClickListener(v -> selectDocTab(DOC_INSPECTION_REPORT));
        tabDocGrossPacking.setOnClickListener(v -> selectDocTab(DOC_GROSS_PACKING));
        tabDocNetPacking.setOnClickListener(v -> selectDocTab(DOC_NET_PACKING));
        tabDocGrossInvoice.setOnClickListener(v -> selectDocTab(DOC_GROSS_INVOICE));
        tabDocNetInvoice.setOnClickListener(v -> selectDocTab(DOC_NET_INVOICE));
    }

    private void showEditOptionsDialog() {
        String[] options = new String[]{
                "Edit Blocks & Measurements",
                "Edit Header, Logistics & Commercial Info"
        };
        new AlertDialog.Builder(this)
                .setTitle("Edit Document Data")
                .setItems(options, (d, which) -> {
                    if (which == 0) {
                        Intent intent = new Intent(this, BlockListEditActivity.class);
                        intent.putExtra(BlockListEditActivity.EXTRA_INSPECTION_ID, currentInspection.getInspectionId());
                        startActivity(intent);
                    } else {
                        Intent intent = new Intent(this, FinishDetailsActivity.class);
                        intent.putExtra(FinishDetailsActivity.EXTRA_INSPECTION_ID, currentInspection.getInspectionId());
                        startActivity(intent);
                    }
                })
                .show();
    }

    private void selectDocTab(int tab) {
        activeDocTab = tab;
        TextView[] tabs = new TextView[]{
                tabDocInspectionReport, tabDocGrossPacking, tabDocNetPacking, tabDocGrossInvoice, tabDocNetInvoice
        };
        for (int i = 0; i < tabs.length; i++) {
            boolean active = (i == tab);
            tabs[i].setBackgroundResource(active ? R.drawable.shape_type_pill_active : R.drawable.shape_type_pill_inactive);
            tabs[i].setTextColor(getColor(active ? R.color.white : R.color.text_secondary));
        }
        renderActiveDocument();
    }

    private void renderActiveDocument() {
        llDocumentSheetContainer.removeAllViews();

        if (activeDocTab == DOC_INSPECTION_REPORT) {
            renderInspectionReportView();
        } else if (activeDocTab == DOC_GROSS_PACKING) {
            renderPackingListView(true);
        } else if (activeDocTab == DOC_NET_PACKING) {
            renderPackingListView(false);
        } else if (activeDocTab == DOC_GROSS_INVOICE) {
            renderInvoiceView(true);
        } else if (activeDocTab == DOC_NET_INVOICE) {
            renderInvoiceView(false);
        }
    }

    // 1. INSPECTION REPORT ABSTRACT + E2E + BLOCK DIAGRAMS
    private void renderInspectionReportView() {
        addCenteredHeader(
                companyProfile.getCompanyName(),
                "INSPECTION REPORT - ABSTRACT",
                "DATE: " + currentInspection.getDate()
        );

        addDivider();

        // Group by BlockTypes (T1, T2, T3) just like PaginatedAbstract in ExportPage.tsx
        List<Block> allBlocks = currentInspection.getBlocks();
        com.graniteflow.app.engine.BlockNumberRuleEngine.enforceSequentialBlockNumbers(
                allBlocks, currentInspection.getStartingBlockNumber() > 0 ? currentInspection.getStartingBlockNumber() : 101);
        String stoneLabel = currentInspection.getStoneDescription().isEmpty()
                ? currentInspection.getMaterialName()
                : currentInspection.getStoneDescription();

        for (BlockTypePreset preset : currentInspection.getBlockTypes()) {
            List<Block> typeBlocks = new ArrayList<>();
            double typeNetSum = 0;
            for (Block b : allBlocks) {
                if (preset.getId().equals(b.getType())) {
                    typeBlocks.add(b);
                    typeNetSum += b.getNetCbm();
                }
            }
            if (typeBlocks.isEmpty()) continue;

            addSectionPill("MATERIAL - " + stoneLabel.toUpperCase() + "  •  " + preset.getName().toUpperCase() + " (" + (int) preset.getAllowance() + " CM ALLOWANCE)");

            addTableRow(new String[]{"SR", "BLOCK #", "NET DIMS (CM)", "ALLOW", "NET CBM"}, true);

            for (int i = 0; i < typeBlocks.size(); i++) {
                Block b = typeBlocks.get(i);
                addTableRow(new String[]{
                        String.valueOf(i + 1),
                        String.format(Locale.ENGLISH, "%03d", b.getNumericBlockNo()),
                        (int) b.getNetLengthCm() + "×" + (int) b.getNetHeightCm() + "×" + (int) b.getNetWidthCm(),
                        (int) b.getAllowanceCm() + " cm",
                        String.format(Locale.ENGLISH, "%.3f", b.getNetCbm())
                }, false);
            }

            addTotalHighlightRow("TOTAL (" + typeBlocks.size() + " BLOCKS):", String.format(Locale.ENGLISH, "%.3f m³", typeNetSum));
        }

        // Grand Total
        addTotalHighlightRow(
                "GRAND TOTAL (" + currentInspection.getTotalBlocksCount() + " BLOCKS):",
                String.format(Locale.ENGLISH, "%.3f m³", currentInspection.calculateTotalNetCbm())
        );

        // Conditional End-to-End Measurement Table (Matching ExportPage.tsx)
        if (currentInspection.isEnableEndToEnd()) {
            addSectionPill("END TO END MEASUREMENT OF BLOCKS");
            addTableRow(new String[]{"SR", "BLOCK #", "PHYSICAL DIMS", "PHYS CBM", "DEFECTS / REMARKS"}, true);
            for (int i = 0; i < allBlocks.size(); i++) {
                Block b = allBlocks.get(i);
                double l1 = b.getL1EndToEnd() > 0 ? b.getL1EndToEnd() : b.getGrossLengthCm();
                double l2 = b.getL2EndToEnd() > 0 ? b.getL2EndToEnd() : b.getGrossHeightCm();
                double l3 = b.getL3EndToEnd() > 0 ? b.getL3EndToEnd() : b.getGrossWidthCm();
                double physCbm = (l1 * l2 * l3) / 1_000_000.0;
                String def = (b.getDefectDescription() != null && !b.getDefectDescription().isEmpty())
                        ? b.getDefectDescription()
                        : (b.getRemarks().isEmpty() ? "-" : b.getRemarks());
                addTableRow(new String[]{
                        String.valueOf(i + 1),
                        String.format(Locale.ENGLISH, "%03d", b.getNumericBlockNo()),
                        (int) l1 + "×" + (int) l2 + "×" + (int) l3,
                        String.format(Locale.ENGLISH, "%.3f", physCbm),
                        def
                }, false);
            }
            addTotalHighlightRow(
                    "PHYSICAL DIMS TOTAL (" + allBlocks.size() + " BLOCKS):",
                    String.format(Locale.ENGLISH, "%.3f m³", currentInspection.calculateTotalEndToEndCbm())
            );
        }

        // Individual Block Sheets Preview (with ObliqueBlockViews View A & View B)
        if (!allBlocks.isEmpty()) {
            addSectionPill("INDIVIDUAL BLOCK SHEETS (" + allBlocks.size() + " BLOCKS)");
            int limit = Math.min(allBlocks.size(), 6);
            for (int i = 0; i < limit; i++) {
                Block b = allBlocks.get(i);
                LinearLayout blockCard = new LinearLayout(this);
                blockCard.setOrientation(LinearLayout.VERTICAL);
                blockCard.setBackgroundResource(R.drawable.shape_input_rounded);
                blockCard.setPadding(dp(12), dp(12), dp(12), dp(12));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, dp(8), 0, dp(8));
                blockCard.setLayoutParams(lp);

                TextView tvBlkHeader = new TextView(this);
                tvBlkHeader.setText(String.format(Locale.ENGLISH,
                        "BLOCK NO : %03d   •   GROSS: %d×%d×%d (%s m³)   •   NET: %d×%d×%d (%.3f m³)",
                        b.getNumericBlockNo(),
                        (int) b.getGrossLengthCm(), (int) b.getGrossHeightCm(), (int) b.getGrossWidthCm(),
                        String.format(Locale.ENGLISH, "%.3f", b.getGrossCbm()),
                        (int) b.getNetLengthCm(), (int) b.getNetHeightCm(), (int) b.getNetWidthCm(),
                        b.getNetCbm()
                ));
                tvBlkHeader.setTextColor(getColor(R.color.brand_blue));
                tvBlkHeader.setTextSize(11);
                tvBlkHeader.setTypeface(null, Typeface.BOLD);
                blockCard.addView(tvBlkHeader);

                BlockDiagramView diagram = new BlockDiagramView(this);
                diagram.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(125)));
                diagram.setDimensions(b.getGrossLengthCm(), b.getGrossHeightCm(), b.getGrossWidthCm());
                blockCard.addView(diagram);

                if (!b.getRemarks().isEmpty()) {
                    TextView tvRem = new TextView(this);
                    tvRem.setText("Remarks: " + b.getRemarks());
                    tvRem.setTextColor(getColor(R.color.text_secondary));
                    tvRem.setTextSize(11);
                    tvRem.setTypeface(null, Typeface.ITALIC);
                    blockCard.addView(tvRem);
                }

                llDocumentSheetContainer.addView(blockCard);
            }
        }
    }

    // 2 & 3. GROSS / NET PACKING LIST
    private void renderPackingListView(boolean isGross) {
        addCenteredHeader(
                isGross ? "GROSS PACKING LIST" : "NET PACKING LIST",
                companyProfile.getCompanyName(),
                "INVOICE NO: " + currentInspection.getInvoiceNumber() + "  •  DTD: " + currentInspection.getInvoiceDate()
        );
        addDivider();

        addTwoColumnInfoBox(
                "EXPORTER",
                companyProfile.getFormattedAddress() + "\n" + companyProfile.getFormattedOtherReferences(),
                "CONSIGNEE & LOGISTICS",
                currentInspection.getCustomer() + "\n" +
                        currentInspection.getConsigneeAddress() + "\n" +
                        "Port: " + currentInspection.getPortOfLoading() + " → " + currentInspection.getPortOfDischarge() + "\n" +
                        "Terms: " + currentInspection.getTermsOfDelivery() + " • " + currentInspection.getTermsOfPayment()
        );

        String stoneDesc = currentInspection.getStoneDescription().isEmpty()
                ? currentInspection.getMaterialName()
                : currentInspection.getStoneDescription();
        addSectionPill("ROUGH GRANITE BLOCKS • " + stoneDesc.toUpperCase() + " • MARK: " + currentInspection.getMarksAndNos());

        addTableRow(new String[]{"BLOCK NO.", "DIMENSIONS (L × H × W)", isGross ? "GROSS CBM" : "NET CBM", "WEIGHT (MT)"}, true);

        List<Block> blocks = currentInspection.getBlocks();
        for (Block b : blocks) {
            double cbm = isGross ? b.getGrossCbm() : b.getNetCbm();
            String dims = isGross
                    ? ((int) b.getGrossLengthCm() + " × " + (int) b.getGrossHeightCm() + " × " + (int) b.getGrossWidthCm())
                    : ((int) b.getNetLengthCm() + " × " + (int) b.getNetHeightCm() + " × " + (int) b.getNetWidthCm());
            String markPrefix = currentInspection.getMarksAndNos().isEmpty() ? "" : (currentInspection.getMarksAndNos() + "-");
            addTableRow(new String[]{
                    "Block No." + markPrefix + String.format(Locale.ENGLISH, "%03d", b.getNumericBlockNo()),
                    dims,
                    String.format(Locale.ENGLISH, "%.3f", cbm),
                    String.format(Locale.ENGLISH, "%.2f", cbm * 2.85)
            }, false);
        }

        double totalCbm = isGross ? currentInspection.calculateTotalGrossCbm() : currentInspection.calculateTotalNetCbm();
        addTotalHighlightRow(
                "TOTAL (" + blocks.size() + " BLOCKS):",
                String.format(Locale.ENGLISH, "%.3f CBM", totalCbm)
        );
    }

    // 4 & 5. GROSS / NET PROFORMA INVOICE
    private void renderInvoiceView(boolean isGross) {
        addCenteredHeader(
                isGross ? "GROSS PROFORMA INVOICE" : "NET PROFORMA INVOICE",
                companyProfile.getCompanyName(),
                "INVOICE NO: " + currentInspection.getInvoiceNumber() + "  •  DTD: " + currentInspection.getInvoiceDate()
        );
        addDivider();

        addTwoColumnInfoBox(
                "EXPORTER",
                companyProfile.getFormattedAddress() + "\n" + companyProfile.getFormattedOtherReferences(),
                "CONSIGNEE & TERMS",
                currentInspection.getCustomer() + "\n" +
                        currentInspection.getConsigneeAddress() + "\n" +
                        "HS Code: " + currentInspection.getHsCode() + " • Origin: " + currentInspection.getCountryOfOrigin() + "\n" +
                        "Port: " + currentInspection.getPortOfLoading() + " → " + currentInspection.getPortOfDischarge() + "\n" +
                        "Terms: " + currentInspection.getTermsOfDelivery() + " / " + currentInspection.getTermsOfPayment()
        );

        String stoneDesc = currentInspection.getStoneDescription().isEmpty()
                ? currentInspection.getMaterialName()
                : currentInspection.getStoneDescription();
        addSectionPill("ROUGH GRANITE BLOCKS • " + stoneDesc.toUpperCase() + " • SHIPPING MARK: " + currentInspection.getMarksAndNos());

        String curr = currentInspection.getCurrency();
        addTableRow(new String[]{"DESCRIPTION / BLOCK RANGE", "QTY (CBM)", "RATE (" + curr + ")", "AMOUNT (" + curr + ")"}, true);

        List<Block> blocks = currentInspection.getBlocks();
        double defaultRate = currentInspection.getPricePerCbm() > 0 ? currentInspection.getPricePerCbm() : 700.0;

        for (BlockTypePreset preset : currentInspection.getBlockTypes()) {
            List<Block> typeBlocks = new ArrayList<>();
            double qtySum = 0;
            for (Block b : blocks) {
                if (preset.getId().equals(b.getType())) {
                    typeBlocks.add(b);
                    qtySum += isGross ? b.getGrossCbm() : b.getNetCbm();
                }
            }
            if (typeBlocks.isEmpty()) continue;

            double rate = preset.getPricePerCbm() > 0 ? preset.getPricePerCbm() : defaultRate;
            double amount = qtySum * rate;
            String firstNo = typeBlocks.get(0).getBlockNumber();
            String lastNo = typeBlocks.get(typeBlocks.size() - 1).getBlockNumber();
            String rangeStr = typeBlocks.size() == 1
                    ? ("BLOCK NO: " + firstNo)
                    : ("BLOCK NO: " + firstNo + " TO " + lastNo + " (" + typeBlocks.size() + " Blks)");

            addTableRow(new String[]{
                    rangeStr,
                    String.format(Locale.ENGLISH, "%.3f", qtySum),
                    String.format(Locale.ENGLISH, "%.2f", rate),
                    String.format(Locale.ENGLISH, "%,.2f", amount)
            }, false);
        }

        double totalCbm = isGross ? currentInspection.calculateTotalGrossCbm() : currentInspection.calculateTotalNetCbm();
        double totalAmount = isGross ? (totalCbm * defaultRate) : currentInspection.calculateTotalValue();
        if (totalAmount <= 0 && totalCbm > 0) {
            totalAmount = totalCbm * defaultRate;
        }

        addTotalHighlightRow(
                "TOTAL (" + blocks.size() + " BLOCKS • " + String.format(Locale.ENGLISH, "%.3f CBM", totalCbm) + "):",
                String.format(Locale.ENGLISH, "%s %,.2f", curr, totalAmount)
        );

        String words = CurrencyWordsConverter.convertToWords(totalAmount, curr);
        addTwoColumnInfoBox(
                "AMOUNT CHARGEABLE (IN WORDS)",
                words.toUpperCase(),
                "EXPORTER BANK DETAILS",
                companyProfile.getFormattedBankDetails()
        );
    }

    private void addCenteredHeader(String topTitle, String mainHeading, String rightDate) {
        TextView tvDate = new TextView(this);
        tvDate.setText(rightDate);
        tvDate.setGravity(Gravity.END);
        tvDate.setTextSize(10);
        tvDate.setTypeface(null, Typeface.BOLD);
        tvDate.setTextColor(getColor(R.color.brand_blue));
        llDocumentSheetContainer.addView(tvDate);

        TextView tvTop = new TextView(this);
        tvTop.setText(topTitle);
        tvTop.setGravity(Gravity.CENTER);
        tvTop.setTextSize(16);
        tvTop.setTypeface(null, Typeface.BOLD);
        tvTop.setTextColor(getColor(R.color.brand_blue));
        tvTop.setPadding(0, dp(4), 0, dp(2));
        llDocumentSheetContainer.addView(tvTop);

        TextView tvMain = new TextView(this);
        tvMain.setText(mainHeading);
        tvMain.setGravity(Gravity.CENTER);
        tvMain.setTextSize(13);
        tvMain.setTypeface(null, Typeface.BOLD);
        tvMain.setTextColor(getColor(R.color.text_primary));
        tvMain.setPadding(0, 0, 0, dp(8));
        llDocumentSheetContainer.addView(tvMain);
    }

    private void addDivider() {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(2)));
        v.setBackgroundColor(getColor(R.color.card_border));
        llDocumentSheetContainer.addView(v);
    }

    private void addSectionPill(String title) {
        TextView tv = new TextView(this);
        tv.setText(title);
        tv.setBackgroundResource(R.drawable.shape_badge_blue);
        tv.setTextColor(getColor(R.color.brand_blue));
        tv.setTextSize(10);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(dp(10), dp(6), dp(10), dp(6));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(12), 0, dp(6));
        tv.setLayoutParams(lp);
        llDocumentSheetContainer.addView(tv);
    }

    private void addTableRow(String[] cols, boolean isHeader) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(6), dp(7), dp(6), dp(7));
        if (isHeader) {
            row.setBackgroundResource(R.drawable.shape_type_pill_active);
        } else {
            row.setBackgroundResource(R.drawable.shape_input_rounded);
        }
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.setMargins(0, 0, 0, dp(3));
        row.setLayoutParams(rlp);

        for (int i = 0; i < cols.length; i++) {
            TextView tv = new TextView(this);
            tv.setText(cols[i]);
            tv.setTextSize(isHeader ? 9 : 11);
            tv.setTypeface(null, isHeader || i == cols.length - 1 ? Typeface.BOLD : Typeface.NORMAL);
            tv.setTextColor(getColor(isHeader ? R.color.white : R.color.text_primary));
            float weight = (i == 0 && cols.length == 4) ? 1.8f : (i == 2 && cols.length == 5 ? 1.5f : 1f);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight);
            tv.setLayoutParams(clp);
            if (i > 0) tv.setGravity(Gravity.CENTER);
            row.addView(tv);
        }
        llDocumentSheetContainer.addView(row);
    }

    private void addTotalHighlightRow(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBackgroundResource(R.drawable.shape_badge_blue);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.setMargins(0, dp(4), 0, dp(8));
        row.setLayoutParams(rlp);

        TextView tvL = new TextView(this);
        tvL.setText(label);
        tvL.setTextColor(getColor(R.color.brand_blue));
        tvL.setTextSize(11);
        tvL.setTypeface(null, Typeface.BOLD);
        tvL.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvV = new TextView(this);
        tvV.setText(value);
        tvV.setTextColor(getColor(R.color.brand_blue));
        tvV.setTextSize(13);
        tvV.setTypeface(null, Typeface.BOLD);

        row.addView(tvL);
        row.addView(tvV);
        llDocumentSheetContainer.addView(row);
    }

    private void addTwoColumnInfoBox(String titleLeft, String bodyLeft, String titleRight, String bodyRight) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.setMargins(0, dp(8), 0, dp(4));
        box.setLayoutParams(blp);

        box.addView(createInfoCell(titleLeft, bodyLeft, true));
        box.addView(createInfoCell(titleRight, bodyRight, false));
        llDocumentSheetContainer.addView(box);
    }

    private View createInfoCell(String title, String body, boolean marginRight) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setBackgroundResource(R.drawable.shape_input_rounded);
        cell.setPadding(dp(10), dp(8), dp(10), dp(8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        if (marginRight) lp.setMargins(0, 0, dp(6), 0);
        cell.setLayoutParams(lp);

        TextView tvT = new TextView(this);
        tvT.setText(title);
        tvT.setTextSize(9);
        tvT.setTypeface(null, Typeface.BOLD);
        tvT.setTextColor(getColor(R.color.brand_blue));
        cell.addView(tvT);

        TextView tvB = new TextView(this);
        tvB.setText(body);
        tvB.setTextSize(10);
        tvB.setTextColor(getColor(R.color.text_primary));
        tvB.setPadding(0, dp(4), 0, 0);
        cell.addView(tvB);

        return cell;
    }

    private void exportActivePdf() {
        try {
            repository.syncDocumentsFromInspection(currentInspection);
            File pdfFile;
            String docTitle;

            if (activeDocTab == DOC_INSPECTION_REPORT) {
                String fileName = "InspectionReport_" + currentInspection.getInspectionId() + ".pdf";
                pdfFile = InspectionReportPdfRenderer.renderInspectionReportPdf(this, currentInspection, fileName);
                docTitle = "Inspection Report - " + currentInspection.getCustomer();
            } else if (activeDocTab == DOC_GROSS_PACKING || activeDocTab == DOC_NET_PACKING) {
                List<PackingListDocument> pls = repository.getPackingLists();
                PackingListDocument pl = pls.isEmpty() ? new PackingListDocument() : pls.get(0);
                pl.setDocumentTitle(activeDocTab == DOC_GROSS_PACKING ? "GROSS PACKING LIST" : "NET PACKING LIST");
                String fileName = (activeDocTab == DOC_GROSS_PACKING ? "GrossPackingList_" : "NetPackingList_")
                        + currentInspection.getInspectionId() + ".pdf";
                pdfFile = PackingListPdfRenderer.renderPackingListPdf(this, pl, fileName);
                docTitle = pl.getDocumentTitle();
            } else {
                List<InvoiceDocument> invs = repository.getInvoices();
                InvoiceDocument inv = invs.isEmpty() ? new InvoiceDocument() : invs.get(0);
                inv.setInvoiceType(activeDocTab == DOC_GROSS_INVOICE ? "GROSS PROFORMA INVOICE" : "NET PROFORMA INVOICE");
                String fileName = (activeDocTab == DOC_GROSS_INVOICE ? "GrossInvoice_" : "NetInvoice_")
                        + currentInspection.getInspectionId() + ".pdf";
                pdfFile = InvoicePdfRenderer.renderInvoicePdf(this, inv, fileName);
                docTitle = inv.getInvoiceType();
            }

            Intent intent = new Intent(this, DocumentPreviewActivity.class);
            intent.putExtra("extra_file_path", pdfFile.getAbsolutePath());
            intent.putExtra("extra_doc_title", docTitle);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to render PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private int dp(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
