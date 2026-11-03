package com.graniteflow.app.engine;

import com.graniteflow.app.data.model.ConsistencyResult;
import com.graniteflow.app.data.model.Inspection;
import com.graniteflow.app.data.model.InvoiceDocument;
import com.graniteflow.app.data.model.PackingListDocument;

public class ConsistencyValidator {

    public static ConsistencyResult validateDocuments(Inspection inspection, InvoiceDocument invoice, PackingListDocument packingList) {
        ConsistencyResult result = new ConsistencyResult();

        if (inspection != null && packingList != null) {
            // Check block counts
            int inspCount = inspection.getTotalBlocksCount();
            int plCount = packingList.getTotalBlocksCount();
            if (inspCount != plCount) {
                result.addDiscrepancy("Block Count", String.valueOf(inspCount), String.valueOf(plCount), "Inspection", "Packing List");
            }

            // Check CBM
            double inspCbm = inspection.calculateTotalNetCbm();
            double plCbm = packingList.getTotalCbm();
            if (Math.abs(inspCbm - plCbm) > 0.005) {
                result.addDiscrepancy("Total CBM", String.valueOf(inspCbm), String.valueOf(plCbm), "Inspection", "Packing List");
            }
        }

        if (invoice != null && packingList != null) {
            // Check Invoice vs Packing List CBM
            double invCbm = invoice.getTotalCbm();
            double plCbm = packingList.getTotalCbm();
            if (Math.abs(invCbm - plCbm) > 0.005) {
                result.addDiscrepancy("Total CBM", String.valueOf(plCbm), String.valueOf(invCbm), "Packing List", "Invoice");
            }

            // Check Ports
            if (invoice.getPortOfDischarge() != null && packingList.getPortOfDischarge() != null) {
                if (!invoice.getPortOfDischarge().trim().equalsIgnoreCase(packingList.getPortOfDischarge().trim())) {
                    result.addDiscrepancy("Port of Discharge", packingList.getPortOfDischarge(), invoice.getPortOfDischarge(), "Packing List", "Invoice");
                }
            }

            // Check Consignee
            if (invoice.getConsignee() != null && packingList.getConsignee() != null) {
                String invName = invoice.getConsignee().getCompanyName();
                String plName = packingList.getConsignee().getCompanyName();
                if (invName != null && plName != null && !invName.trim().equalsIgnoreCase(plName.trim())) {
                    result.addDiscrepancy("Consignee Name", plName, invName, "Packing List", "Invoice");
                }
            }
        }

        return result;
    }
}
