package com.graniteflow.app;

import com.graniteflow.app.data.model.ConsistencyResult;
import com.graniteflow.app.data.model.Inspection;
import com.graniteflow.app.data.model.InvoiceDocument;
import com.graniteflow.app.data.model.PackingListDocument;
import com.graniteflow.app.data.repository.MockDataGenerator;
import com.graniteflow.app.engine.ConsistencyValidator;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConsistencyValidatorTest {

    @Test
    public void testMatchingDocumentsAreValid() {
        Inspection inspection = MockDataGenerator.createSampleInspection();
        InvoiceDocument invoice = MockDataGenerator.createSampleInvoice();
        PackingListDocument pl = MockDataGenerator.createSamplePackingList();

        // Check consistency between matching invoice and packing list
        ConsistencyResult result = ConsistencyValidator.validateDocuments(null, invoice, pl);
        assertTrue("Matching invoice and packing list should have zero discrepancies", result.isValid());
    }

    @Test
    public void testDiscrepancyDetection() {
        InvoiceDocument invoice = MockDataGenerator.createSampleInvoice();
        PackingListDocument pl = MockDataGenerator.createSamplePackingList();

        // Introduce deliberate mismatch in port of discharge
        invoice.setPortOfDischarge("XIAMEN , CHINA");

        ConsistencyResult result = ConsistencyValidator.validateDocuments(null, invoice, pl);
        assertFalse("Mismatched discharge ports must be flagged", result.isValid());
        assertTrue(result.getDiscrepancies().get(0).getFormattedMessage().contains("Port of Discharge"));
    }
}
