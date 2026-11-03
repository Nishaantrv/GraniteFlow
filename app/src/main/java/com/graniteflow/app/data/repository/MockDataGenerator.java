package com.graniteflow.app.data.repository;

import com.graniteflow.app.data.model.Block;
import com.graniteflow.app.data.model.BlockPhoto;
import com.graniteflow.app.data.model.CompanyProfile;
import com.graniteflow.app.data.model.Customer;
import com.graniteflow.app.data.model.Inspection;
import com.graniteflow.app.data.model.InvoiceDocument;
import com.graniteflow.app.data.model.InvoiceLineItem;
import com.graniteflow.app.data.model.Order;
import com.graniteflow.app.data.model.PackingListDocument;
import com.graniteflow.app.data.model.PackingListRow;
import com.graniteflow.app.engine.InvoiceCalculator;
import com.graniteflow.app.engine.PackingListCalculator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MockDataGenerator {

    public static CompanyProfile createDefaultProfile() {
        return new CompanyProfile();
    }

    public static Customer createDefaultCustomer() {
        return new Customer();
    }

    public static Order createDefaultOrder() {
        return new Order("ORD-2026-0914", "SHISHI YINSHIHUWANG IMPORT AND EXPORT LTD", "KATTIMA BLACK", "SHIHU");
    }

    public static Inspection createSampleInspection() {
        Inspection inspection = new Inspection();
        inspection.setInspectionId("INSP-KATTIMA-0914");
        inspection.setOrderId("ORD-2026-0914");
        inspection.setDate("14.09.2026");
        inspection.setInspectorName("Lead Inspector");
        inspection.setLocationCode("XOW/XMN");
        inspection.setMaterialName("KATTIMA BLACK");
        inspection.setDefaultAllowanceCm(10.0);
        inspection.setApprovalState("APPROVED");

        List<Block> blocks = new ArrayList<>();

        // Detailed blocks from packing list screenshot (net dimensions shown in reference PL)
        // We'll store gross = net + 10 allowance so recalculation accurately reproduces the exact net CBM
        blocks.add(createBlock("138", 105, 85, 60, 10));
        blocks.add(createBlock("139", 185, 120, 45, 10));
        blocks.add(createBlock("140", 220, 85, 85, 10));
        blocks.add(createBlock("141", 235, 105, 85, 10));
        blocks.add(createBlock("142", 160, 155, 100, 10));
        blocks.add(createBlock("143", 205, 125, 55, 10));
        blocks.add(createBlock("144", 155, 85, 75, 10));
        blocks.add(createBlock("145", 155, 80, 70, 10));
        blocks.add(createBlock("146", 180, 85, 75, 10));
        blocks.add(createBlock("147", 140, 115, 45, 10));
        blocks.add(createBlock("148", 165, 75, 55, 10));
        blocks.add(createBlock("149", 135, 95, 75, 10));
        blocks.add(createBlock("150", 165, 125, 80, 10));
        blocks.add(createBlock("151", 190, 85, 70, 10));
        blocks.add(createBlock("152", 255, 115, 110, 10));
        blocks.add(createBlock("153", 125, 125, 95, 10));
        blocks.add(createBlock("154", 265, 125, 85, 10));
        blocks.add(createBlock("155", 235, 145, 105, 10));
        blocks.add(createBlock("156", 215, 140, 115, 10));
        blocks.add(createBlock("157", 230, 150, 125, 10));
        blocks.add(createBlock("158", 260, 145, 95, 10));
        blocks.add(createBlock("159", 190, 125, 75, 10));

        // Add Reference Block 301 (from Image 3 & Image 4)
        Block block301 = new Block("301", 280, 185, 185, 10);
        block301.getPhotos().add(new BlockPhoto("301", "A SIDE", "", "Quarry front face marking"));
        block301.getPhotos().add(new BlockPhoto("301", "B SIDE", "", "Quarry reverse face inspection"));
        blocks.add(block301);

        inspection.setBlocks(blocks);
        return inspection;
    }

    private static Block createBlock(String number, double netL, double netH, double netW, double allowance) {
        // Gross dimensions = net + allowance
        Block b = new Block(number, netL + allowance, netH + allowance, netW + allowance, allowance);
        b.getPhotos().add(new BlockPhoto(number, "A SIDE", "", "Quarry face"));
        return b;
    }

    public static InvoiceDocument createSampleInvoice() {
        InvoiceDocument invoice = new InvoiceDocument();
        invoice.setInvoiceType("PROFORMA INVOICE");
        invoice.setInvoiceNumber("DE- / DT : 14.09.2026");
        invoice.setDate("14.09.2026");
        invoice.setExporter(createDefaultProfile());
        invoice.setConsignee(createDefaultCustomer());
        invoice.setPortOfLoading("KAKINADA");
        invoice.setPortOfDischarge("SHIHU , CHINA");
        invoice.setFinalDestination("SHIHU , CHINA");
        invoice.setTermsOfDeliveryPayment("FOB KAKINADA PORT, INDIA\nTT PAYMENT");
        invoice.setDescriptionHeader("ROUGH GRANITE BLOCKS\nKATTIMA BLACK\nKND");
        invoice.setTotalPackagesCount(22);

        List<InvoiceLineItem> items = new ArrayList<>();

        InvoiceLineItem item1 = new InvoiceLineItem("Block no. 138, 139, 140, 142, 144 to 151, 153, 159", 17.023, 1250);
        item1.setAssociatedBlockNumbers(Arrays.asList("138", "139", "140", "142", "144", "145", "146", "147", "148", "149", "150", "151", "153", "159"));
        items.add(item1);

        InvoiceLineItem item2 = new InvoiceLineItem("Block no. 141, 143, 152, 154, 156, 157", 17.323, 1450);
        item2.setAssociatedBlockNumbers(Arrays.asList("141", "143", "152", "154", "156", "157"));
        items.add(item2);

        InvoiceLineItem item3 = new InvoiceLineItem("Block no. 155", 3.578, 1500);
        item3.setAssociatedBlockNumbers(Arrays.asList("155"));
        items.add(item3);

        InvoiceLineItem item4 = new InvoiceLineItem("Block no. 158", 3.582, 1550);
        item4.setAssociatedBlockNumbers(Arrays.asList("158"));
        items.add(item4);

        invoice.setLineItems(items);
        InvoiceCalculator.computeAndApplyInvoiceTotals(invoice);
        invoice.setStatus("APPROVED");
        return invoice;
    }

    public static PackingListDocument createSamplePackingList() {
        PackingListDocument pl = new PackingListDocument();
        pl.setDocumentTitle("PACKING LIST");
        pl.setInvoiceNumber("DE- / DT : 14.09.2026");
        pl.setDate("14.09.2026");
        pl.setExporter(createDefaultProfile());
        pl.setConsignee(createDefaultCustomer());
        pl.setPortOfLoading("KAKINADA PORT");
        pl.setPortOfDischarge("SHIHU , CHINA");
        pl.setFinalDestination("SHIHU , CHINA");
        pl.setTermsOfDeliveryPayment("FOB KAKINADA PORT,INDIA\nTT PAYMENT");
        pl.setDescriptionHeader("ROUGH GRANITE BLOCKS\nKATTIMA BLACK\nKND");

        List<PackingListRow> rows = new ArrayList<>();
        rows.add(new PackingListRow("138", 105, 85, 60, 0.536));
        rows.add(new PackingListRow("139", 185, 120, 45, 0.999));
        rows.add(new PackingListRow("140", 220, 85, 85, 1.590));
        rows.add(new PackingListRow("141", 235, 105, 85, 2.097));
        rows.add(new PackingListRow("142", 160, 155, 100, 2.480));
        rows.add(new PackingListRow("143", 205, 125, 55, 1.409));
        rows.add(new PackingListRow("144", 155, 85, 75, 0.988));
        rows.add(new PackingListRow("145", 155, 80, 70, 0.868));
        rows.add(new PackingListRow("146", 180, 85, 75, 1.148));
        rows.add(new PackingListRow("147", 140, 115, 45, 0.725));
        rows.add(new PackingListRow("148", 165, 75, 55, 0.681));
        rows.add(new PackingListRow("149", 135, 95, 75, 0.962));
        rows.add(new PackingListRow("150", 165, 125, 80, 1.650));
        rows.add(new PackingListRow("151", 190, 85, 70, 1.131));
        rows.add(new PackingListRow("152", 255, 115, 110, 3.226));
        rows.add(new PackingListRow("153", 125, 125, 95, 1.484));
        rows.add(new PackingListRow("154", 265, 125, 85, 2.816));
        rows.add(new PackingListRow("155", 235, 145, 105, 3.578));
        rows.add(new PackingListRow("156", 215, 140, 115, 3.462));
        rows.add(new PackingListRow("157", 230, 150, 125, 4.313));
        rows.add(new PackingListRow("158", 260, 145, 95, 3.582));
        rows.add(new PackingListRow("159", 190, 125, 75, 1.781));

        pl.setRows(rows);
        PackingListCalculator.computeAndApplyPackingListTotals(pl);
        pl.setStatus("APPROVED");
        return pl;
    }
}
