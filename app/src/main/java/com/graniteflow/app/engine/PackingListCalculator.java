package com.graniteflow.app.engine;

import com.graniteflow.app.data.model.PackingListDocument;
import com.graniteflow.app.data.model.PackingListRow;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class PackingListCalculator {

    public static double calculateRowCbm(double lengthCm, double heightCm, double widthCm) {
        return DimensionCalculator.calculateCbm(lengthCm, heightCm, widthCm);
    }

    public static void computeAndApplyPackingListTotals(PackingListDocument packingList) {
        if (packingList == null) return;
        List<PackingListRow> rows = packingList.getRows();
        double totalCbm = 0;
        double totalWeight = 0;

        if (rows != null) {
            for (PackingListRow row : rows) {
                totalCbm += row.getQuantityCbm();
                totalWeight += row.getWeightMt();
            }
            packingList.setTotalBlocksCount(rows.size());
        } else {
            packingList.setTotalBlocksCount(0);
        }

        packingList.setTotalCbm(BigDecimal.valueOf(totalCbm).setScale(3, RoundingMode.HALF_UP).doubleValue());
        packingList.setTotalWeightMt(BigDecimal.valueOf(totalWeight).setScale(3, RoundingMode.HALF_UP).doubleValue());
    }
}
