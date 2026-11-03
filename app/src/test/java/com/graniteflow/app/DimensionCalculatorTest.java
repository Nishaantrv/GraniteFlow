package com.graniteflow.app;

import com.graniteflow.app.data.model.Block;
import com.graniteflow.app.engine.DimensionCalculator;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DimensionCalculatorTest {

    @Test
    public void testReferenceBlock301NetDimensionsAndCbm() {
        double grossL = 280.0;
        double grossH = 185.0;
        double grossW = 185.0;
        double allowance = 10.0;

        double netL = DimensionCalculator.calculateNetDimension(grossL, allowance);
        double netH = DimensionCalculator.calculateNetDimension(grossH, allowance);
        double netW = DimensionCalculator.calculateNetDimension(grossW, allowance);

        assertEquals("Net length should be 270 cm", 270.0, netL, 0.001);
        assertEquals("Net height should be 175 cm", 175.0, netH, 0.001);
        assertEquals("Net width should be 175 cm", 175.0, netW, 0.001);

        double grossCbm = DimensionCalculator.calculateCbm(grossL, grossH, grossW);
        double netCbm = DimensionCalculator.calculateCbm(netL, netH, netW);

        assertEquals("Gross CBM must be 9.583 m³", 9.583, grossCbm, 0.0001);
        assertEquals("Net CBM must be 8.269 m³", 8.269, netCbm, 0.0001);

        Block block = new Block("301", grossL, grossH, grossW, allowance);
        assertEquals(270.0, block.getNetLengthCm(), 0.001);
        assertEquals(175.0, block.getNetHeightCm(), 0.001);
        assertEquals(175.0, block.getNetWidthCm(), 0.001);
        assertEquals(9.583, block.getGrossCbm(), 0.0001);
        assertEquals(8.269, block.getNetCbm(), 0.0001);
    }

    @Test
    public void testDimensionValidation() {
        assertNotNull(DimensionCalculator.validateDimensions(-10, 100, 100, 5));
        assertNotNull(DimensionCalculator.validateDimensions(0, 100, 100, 5));
        assertNotNull(DimensionCalculator.validateDimensions(10, 100, 100, 10));
        assertNotNull(DimensionCalculator.validateDimensions(10, 100, 100, 15));
        assertNull(DimensionCalculator.validateDimensions(280, 185, 185, 10));
    }

    @Test
    public void testReportedCbmConsistencyAndMismatchDetection() {
        assertNull("Matching reported CBM within 0.005 m³ tolerance must be accepted",
                DimensionCalculator.validateReportedCbmConsistency(270, 175, 175, 8.269));
        assertNull("Reported CBM within 0.001 m³ tolerance must pass",
                DimensionCalculator.validateReportedCbmConsistency(270, 175, 175, 8.270));

        String errorMismatch = DimensionCalculator.validateReportedCbmConsistency(270, 175, 175, 7.500);
        assertNotNull("Deliberate CBM mismatch must be flagged", errorMismatch);
        assertTrue(errorMismatch.contains("does not match calculated CBM"));

        String oldDiscrepancy = DimensionCalculator.validateReportedCbmConsistency(275, 135, 115, 3.869);
        assertNotNull("Old inaccurate reported CBM must be flagged", oldDiscrepancy);

        assertNull("Corrected 4.269 m³ must pass validation",
                DimensionCalculator.validateReportedCbmConsistency(275, 135, 115, 4.269));
    }

    @Test
    public void testAutoHealMisreadDimensions() {
        // Suppose OCR misreads Width 125 as 128 in Block BG-501 (Gross 285 x 145 x 128, Allw 10, Reported CBM 4.269)
        double[] healed = DimensionCalculator.autoHealMisreadDimensions(285, 145, 128, 10, 4.269);
        assertNotNull("Should auto-heal misread width", healed);
        assertEquals(285.0, healed[0], 0.001);
        assertEquals(145.0, healed[1], 0.001);
        assertEquals(125.0, healed[2], 0.001); // Auto-healed from 128 to 125!

        // Suppose OCR misreads Height 150 as 158 in Block BG-502 (Gross 310 x 158 x 130, Allw 10, Reported CBM 5.040)
        double[] healedH = DimensionCalculator.autoHealMisreadDimensions(310, 158, 130, 10, 5.040);
        assertNotNull("Should auto-heal misread height", healedH);
        assertEquals(310.0, healedH[0], 0.001);
        assertEquals(150.0, healedH[1], 0.001); // Auto-healed from 158 to 150!
        assertEquals(130.0, healedH[2], 0.001);
    }
}
