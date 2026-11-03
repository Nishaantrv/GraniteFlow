package com.graniteflow.app;

import com.graniteflow.app.data.model.Block;
import com.graniteflow.app.engine.DimensionCalculator;
import com.graniteflow.app.ocr.GraniteSlipParser;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit test for GraniteSlipParser and CBM validation.
 * Verifies that tabular block measurement rows and header fields
 * extracted from real physical measurement slips (both row-based and columnar formats)
 * are parsed with mathematically exact CBM values.
 */
public class GraniteSlipParserTest {

    @Test
    public void testParseActualQuarrySlipSampleTextWithExactCbm() {
        // Real OCR text recognized from sample_quarry_slip.png with exact net CBM values:
        // ((L-10) * (H-10) * (W-10)) / 1,000,000
        String realOcrText =
                "CHETTINAD GRANITES & EXPORTS LTD\n" +
                "QUARRY BLOCK MEASUREMENT SLIP\n" +
                "MATERIAL: BLACK GALAXY\n" +
                "LOCATION: ONG-QUARRY-04\n" +
                "DATE: 12/10/2026\n" +
                "ALLOWANCE: 10 CM\n" +
                "INSPECTOR: K. RAMESH\n" +
                "SL | BLOCK NO | LENGTH | HEIGHT | WIDTH | ALLOW | NET CBM\n" +
                "1 | BG-501 | 285 | 145 | 125 | 10 | 4.269\n" +
                "2 | BG-502 | 310 | 150 | 130 | 10 | 5.040\n" +
                "3 | BG-503 | 260 | 135 | 115 | 10 | 3.281\n" +
                "4 | BG-504 | 295 | 140 | 120 | 10 | 4.076\n" +
                "5 | BG-505 | 275 | 145 | 120 | 10 | 3.935\n" +
                "6 | BG-506 | 320 | 155 | 135 | 10 | 5.619\n" +
                "TOTAL BLOCKS: 6 | TOTAL VOLUME: 26.220 M3\n" +
                "REMARKS: High grade black galaxy with fine golden bronzite flakes. Verified single 10cm allowance rule.";

        GraniteSlipParser.ParsedInspectionResult result = GraniteSlipParser.parse(realOcrText);

        assertNotNull(result);
        assertEquals("BLACK GALAXY", result.materialName);
        assertEquals("ONG-QUARRY-04", result.locationCode);
        assertEquals("12/10/2026", result.date);
        assertEquals("K. RAMESH", result.inspectorName);
        assertEquals(10.0, result.defaultAllowanceCm, 0.001);

        // Verify all 6 blocks were parsed
        assertEquals(6, result.blocks.size());

        // Block 1: BG-501: Gross 285 x 145 x 125 -> Net 275 x 135 x 115 -> Net CBM 4.269 m³
        Block b1 = result.blocks.get(0);
        assertEquals("BG-501", b1.getBlockNumber());
        assertEquals(285.0, b1.getGrossLengthCm(), 0.001);
        assertEquals(145.0, b1.getGrossHeightCm(), 0.001);
        assertEquals(125.0, b1.getGrossWidthCm(), 0.001);
        assertEquals(10.0, b1.getAllowanceCm(), 0.001);
        assertEquals(275.0, b1.getNetLengthCm(), 0.001);
        assertEquals(135.0, b1.getNetHeightCm(), 0.001);
        assertEquals(115.0, b1.getNetWidthCm(), 0.001);
        assertEquals(4.269, b1.getNetCbm(), 0.001);

        // Block 2: BG-502: Gross 310 x 150 x 130 -> Net 300 x 140 x 120 -> Net CBM 5.040 m³
        Block b2 = result.blocks.get(1);
        assertEquals("BG-502", b2.getBlockNumber());
        assertEquals(5.040, b2.getNetCbm(), 0.001);

        // Block 3: BG-503: Gross 260 x 135 x 115 -> Net 250 x 125 x 105 -> Net CBM 3.281 m³
        Block b3 = result.blocks.get(2);
        assertEquals("BG-503", b3.getBlockNumber());
        assertEquals(3.281, b3.getNetCbm(), 0.001);

        // Block 4: BG-504: Gross 295 x 140 x 120 -> Net 285 x 130 x 110 -> Net CBM 4.076 m³
        Block b4 = result.blocks.get(3);
        assertEquals("BG-504", b4.getBlockNumber());
        assertEquals(4.076, b4.getNetCbm(), 0.001);

        // Block 5: BG-505: Gross 275 x 145 x 120 -> Net 265 x 135 x 110 -> Net CBM 3.935 m³
        Block b5 = result.blocks.get(4);
        assertEquals("BG-505", b5.getBlockNumber());
        assertEquals(3.935, b5.getNetCbm(), 0.001);

        // Block 6: BG-506: Gross 320 x 155 x 135 -> Net 310 x 145 x 125 -> Net CBM 5.619 m³
        Block b6 = result.blocks.get(5);
        assertEquals("BG-506", b6.getBlockNumber());
        assertEquals(5.619, b6.getNetCbm(), 0.001);

        // Total Net CBM: 4.269 + 5.040 + 3.281 + 4.076 + 3.935 + 5.619 = 26.220 m³
        assertEquals(26.220, result.calculateTotalNetCbm(), 0.005);
        assertTrue(result.warnings.isEmpty());
    }

    @Test
    public void testParseActualColumnarOcrOutputFromRealImage() {
        // Text stream extracted directly from corrected sample_quarry_slip.png
        String columnarOcrText =
                "MATERIAL: BLACK GALAXY\n" +
                "LOCATION: ONG-QUARRY-04\n" +
                "INSPECTOR: K. RAMESH\n" +
                "BG-501\n" +
                "BG-502\n" +
                "BG-503\n" +
                "BG-504\n" +
                "BG-505\n" +
                "BG-506\n" +
                "CHETTINAD GRANITES & EXPORTS LTD\n" +
                "DATE: 12/10/2026\n" +
                "ALLOWANCE: 10 CM\n" +
                "LENGTH (CM)\n" +
                "285\n" +
                "310\n" +
                "260\n" +
                "295\n" +
                "275\n" +
                "320\n" +
                "HEIGHT (CM)\n" +
                "145\n" +
                "150\n" +
                "135\n" +
                "140\n" +
                "145\n" +
                "155\n" +
                "WIDTH (CM)\n" +
                "125\n" +
                "130\n" +
                "115\n" +
                "120\n" +
                "120\n" +
                "135\n";

        GraniteSlipParser.ParsedInspectionResult result = GraniteSlipParser.parse(columnarOcrText);

        assertNotNull(result);
        assertEquals("BLACK GALAXY", result.materialName);
        assertEquals("ONG-QUARRY-04", result.locationCode);
        assertEquals("12/10/2026", result.date);
        assertEquals(6, result.blocks.size());

        assertEquals("BG-501", result.blocks.get(0).getBlockNumber());
        assertEquals(285.0, result.blocks.get(0).getGrossLengthCm(), 0.001);
        assertEquals(145.0, result.blocks.get(0).getGrossHeightCm(), 0.001);
        assertEquals(125.0, result.blocks.get(0).getGrossWidthCm(), 0.001);
        assertEquals(4.269, result.blocks.get(0).getNetCbm(), 0.001);

        assertEquals("BG-506", result.blocks.get(5).getBlockNumber());
        assertEquals(320.0, result.blocks.get(5).getGrossLengthCm(), 0.001);
        assertEquals(155.0, result.blocks.get(5).getGrossHeightCm(), 0.001);
        assertEquals(135.0, result.blocks.get(5).getGrossWidthCm(), 0.001);
        assertEquals(5.619, result.blocks.get(5).getNetCbm(), 0.001);
    }

    @Test
    public void testParseRealPhotographedInspectionSheetBlock301() {
        // Real OCR text from actual photographed user document media_1791863231835_58423abd.png
        String realPhotoOcrText =
                "BLOCK NO : 301\n" +
                "MEASUREMENT\n" +
                "GROSS NET\n" +
                "LENGTH 280 270\n" +
                "HEIGHT 185 175\n" +
                "WIDTH 185 175\n" +
                "ALLOWANCE 10 CM\n" +
                "NET CBM 8.269\n";

        // Parse through GraniteSlipParser
        GraniteSlipParser.ParsedInspectionResult result = GraniteSlipParser.parse(realPhotoOcrText);
        assertNotNull(result);
        assertEquals(1, result.blocks.size());

        Block b = result.blocks.get(0);
        assertEquals("301", b.getBlockNumber());
        assertEquals(280.0, b.getGrossLengthCm(), 0.001);
        assertEquals(185.0, b.getGrossHeightCm(), 0.001);
        assertEquals(185.0, b.getGrossWidthCm(), 0.001);
        assertEquals(10.0, b.getAllowanceCm(), 0.001);
        assertEquals(270.0, b.getNetLengthCm(), 0.001);
        assertEquals(175.0, b.getNetHeightCm(), 0.001);
        assertEquals(175.0, b.getNetWidthCm(), 0.001);
        assertEquals(9.583, b.getGrossCbm(), 0.001);
        assertEquals(8.269, b.getNetCbm(), 0.001);

        // Validate math on this real document:
        // Net: 270 x 175 x 175 -> CBM = (270 * 175 * 175) / 1,000,000 = 8.269
        double calcCbm = DimensionCalculator.calculateCbm(270, 175, 175);
        assertEquals(8.269, calcCbm, 0.0001);

        // Verify reported CBM consistency
        assertNull(DimensionCalculator.validateReportedCbmConsistency(270, 175, 175, 8.269));
        assertTrue("No warnings should be present for consistent slip", result.warnings.isEmpty());
    }

    @Test
    public void testParseHorizontalGrossRowFormat() {
        String horizontalOcr =
                "BLOCK NO : 301\n" +
                "LENGTH HEIGHT WIDTH NET MEASUREMENT ALLOWANCE (CM) (CM) (CM) CBM\n" +
                "GROSS 280 185 185 9.583\n" +
                "10 CM\n" +
                "NET 270 175 175 8.269\n" +
                "VIEW A VIEW B\n" +
                "L 280 L 280\n" +
                "H 185 H 185\n" +
                "W 185 W 185\n";

        GraniteSlipParser.ParsedInspectionResult result = GraniteSlipParser.parse(horizontalOcr);
        assertNotNull(result);
        assertEquals(1, result.blocks.size());

        Block b = result.blocks.get(0);
        assertEquals("301", b.getBlockNumber());
        assertEquals(280.0, b.getGrossLengthCm(), 0.001);
        assertEquals(185.0, b.getGrossHeightCm(), 0.001);
        assertEquals(185.0, b.getGrossWidthCm(), 0.001);
        assertEquals(10.0, b.getAllowanceCm(), 0.001);
        assertEquals(8.269, b.getNetCbm(), 0.001);
    }

    @Test
    public void testDeliberateCbmMismatchIsFlagged() {
        // Test supplying an erroneous reported CBM of 7.500 m³ when calculated is 8.269 m³
        String mismatchError = DimensionCalculator.validateReportedCbmConsistency(270, 175, 175, 7.500);
        assertNotNull("Reported CBM mismatch must be flagged", mismatchError);
        assertTrue(mismatchError.contains("does not match calculated CBM"));

        // Test supplying old erroneous CBM of 3.869 m³ when calculated is 4.269 m³ (for 275 x 135 x 115)
        String oldError = DimensionCalculator.validateReportedCbmConsistency(275, 135, 115, 3.869);
        assertNotNull("Inconsistent reported CBM must be flagged", oldError);
        assertTrue(oldError.contains("Discrepancy: 0.400"));
    }
}
