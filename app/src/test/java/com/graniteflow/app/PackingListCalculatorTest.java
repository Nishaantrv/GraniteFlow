package com.graniteflow.app;

import com.graniteflow.app.data.model.PackingListDocument;
import com.graniteflow.app.data.model.PackingListRow;
import com.graniteflow.app.engine.PackingListCalculator;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class PackingListCalculatorTest {

    @Test
    public void testPackingListTotalsAndBlockCbm() {
        PackingListDocument pl = new PackingListDocument();
        List<PackingListRow> rows = new ArrayList<>();

        // Test block 138: 105 x 85 x 60 -> 0.536
        PackingListRow r1 = new PackingListRow("138", 105, 85, 60);
        assertEquals(0.536, r1.getQuantityCbm(), 0.001);
        rows.add(r1);

        // Test block 139: 185 x 120 x 45 -> 0.999
        PackingListRow r2 = new PackingListRow("139", 185, 120, 45);
        assertEquals(0.999, r2.getQuantityCbm(), 0.001);
        rows.add(r2);

        // Test block 140: 220 x 85 x 85 -> 1.590
        PackingListRow r3 = new PackingListRow("140", 220, 85, 85);
        assertEquals(1.590, r3.getQuantityCbm(), 0.001);
        rows.add(r3);

        pl.setRows(rows);
        PackingListCalculator.computeAndApplyPackingListTotals(pl);

        assertEquals(3, pl.getTotalBlocksCount());
        // 0.536 + 0.999 + 1.590 = 3.125
        assertEquals(3.125, pl.getTotalCbm(), 0.001);
    }
}
