package com.graniteflow.app.engine;

import com.graniteflow.app.data.dataset.EvaluationResult;
import com.graniteflow.app.data.dataset.GroundTruth;
import com.graniteflow.app.data.dataset.OcrPrediction;
import com.graniteflow.app.data.model.Block;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DatasetEvaluationEngine {

    public static final double CBM_TOLERANCE = 0.005; // 0.005 M³ tolerance
    public static final double DIMENSION_TOLERANCE_CM = 0.0; // Exact centimetre matching policy

    /**
     * Pure, deterministic metric comparison between OCR prediction and approved ground truth.
     * Prevents reusing one prediction row for multiple ground truth rows.
     */
    public static EvaluationResult evaluate(OcrPrediction prediction, GroundTruth groundTruth) {
        EvaluationResult result = new EvaluationResult();
        if (groundTruth != null) {
            result.setDatasetRecordId(groundTruth.getDatasetRecordId());
            result.setGroundTruthVersion(groundTruth.getVersion());
        }
        if (prediction != null) {
            result.setOcrEngine(prediction.getOcrEngine());
            result.setParserVersion(prediction.getParserVersion());
        }

        List<Block> expectedBlocks = (groundTruth != null && groundTruth.getBlocks() != null)
                ? groundTruth.getBlocks() : new ArrayList<>();
        List<OcrPrediction.PredictedBlock> predictedBlocks = (prediction != null && prediction.getBlocks() != null)
                ? prediction.getBlocks() : new ArrayList<>();

        result.setRowsExpected(expectedBlocks.size());
        result.setRowsPredicted(predictedBlocks.size());

        // Tracking matched prediction indices to ensure strict 1-to-1 matching
        Set<Integer> usedPredictionIndices = new HashSet<>();
        List<EvaluationResult.RowEvaluationDetail> details = new ArrayList<>();

        int alignedCount = 0;
        int blockNumberMatches = 0;
        int dimensionMatches = 0;
        int cbmMatches = 0;

        // Step 1: Align by exact Block Number match first
        for (Block exp : expectedBlocks) {
            int matchedPredIndex = -1;

            if (exp.getBlockNumber() != null && !exp.getBlockNumber().trim().isEmpty()) {
                String expNumClean = exp.getBlockNumber().trim();
                for (int i = 0; i < predictedBlocks.size(); i++) {
                    if (usedPredictionIndices.contains(i)) continue;
                    OcrPrediction.PredictedBlock pred = predictedBlocks.get(i);
                    if (pred.getBlockNumber() != null && pred.getBlockNumber().trim().equalsIgnoreCase(expNumClean)) {
                        matchedPredIndex = i;
                        break;
                    }
                }
            }

            // Step 2: Fallback alignment by closest dimensional match if block number was misread
            if (matchedPredIndex == -1) {
                double minDiff = Double.MAX_VALUE;
                for (int i = 0; i < predictedBlocks.size(); i++) {
                    if (usedPredictionIndices.contains(i)) continue;
                    OcrPrediction.PredictedBlock pred = predictedBlocks.get(i);
                    double diff = Math.abs(pred.getGrossLengthCm() - exp.getGrossLengthCm())
                            + Math.abs(pred.getGrossHeightCm() - exp.getGrossHeightCm())
                            + Math.abs(pred.getGrossWidthCm() - exp.getGrossWidthCm());
                    // Proximity threshold of 25cm total across 3 axes
                    if (diff <= 25.0 && diff < minDiff) {
                        minDiff = diff;
                        matchedPredIndex = i;
                    }
                }
            }

            EvaluationResult.RowEvaluationDetail row = new EvaluationResult.RowEvaluationDetail();
            row.expectedBlockNumber = exp.getBlockNumber();
            row.expectedCbm = exp.getNetCbm();

            if (matchedPredIndex != -1) {
                usedPredictionIndices.add(matchedPredIndex);
                alignedCount++;
                OcrPrediction.PredictedBlock pred = predictedBlocks.get(matchedPredIndex);

                row.predictedBlockNumber = pred.getBlockNumber();
                row.predictedCbm = pred.getNetCbm();

                // Field 1: Block number
                boolean numMatch = (exp.getBlockNumber() != null && pred.getBlockNumber() != null
                        && exp.getBlockNumber().trim().equalsIgnoreCase(pred.getBlockNumber().trim()));
                row.blockNumberMatch = numMatch;
                if (numMatch) blockNumberMatches++;
                else result.addError(EvaluationResult.ERR_BLOCK_NUMBER);

                // Field 2: Dimensions
                boolean dimMatch = Math.abs(pred.getGrossLengthCm() - exp.getGrossLengthCm()) <= DIMENSION_TOLERANCE_CM
                        && Math.abs(pred.getGrossHeightCm() - exp.getGrossHeightCm()) <= DIMENSION_TOLERANCE_CM
                        && Math.abs(pred.getGrossWidthCm() - exp.getGrossWidthCm()) <= DIMENSION_TOLERANCE_CM;
                row.dimensionsMatch = dimMatch;
                if (dimMatch) dimensionMatches++;
                else result.addError(EvaluationResult.ERR_DIMENSION_EXTRACTION);

                // Field 3: CBM
                boolean cbmMatch = Math.abs(pred.getNetCbm() - exp.getNetCbm()) <= CBM_TOLERANCE;
                row.cbmMatch = cbmMatch;
                if (cbmMatch) cbmMatches++;
                else result.addError(EvaluationResult.ERR_CBM_MISMATCH);

                if (!numMatch || !dimMatch || !cbmMatch) {
                    row.remarks = "Discrepancy detected in prediction vs ground truth.";
                } else {
                    row.remarks = "Exact match.";
                }
            } else {
                // Ground truth row was not found in prediction
                row.predictedBlockNumber = null;
                row.predictedCbm = 0.0;
                row.blockNumberMatch = false;
                row.dimensionsMatch = false;
                row.cbmMatch = false;
                row.errorCategory = EvaluationResult.ERR_MISSING_ROW;
                row.remarks = "Expected block omitted from OCR prediction.";
                result.addError(EvaluationResult.ERR_MISSING_ROW);
            }
            details.add(row);
        }

        // Extra predicted rows that did not align to any ground truth row
        int extraCount = 0;
        for (int i = 0; i < predictedBlocks.size(); i++) {
            if (!usedPredictionIndices.contains(i)) {
                extraCount++;
                OcrPrediction.PredictedBlock pred = predictedBlocks.get(i);
                EvaluationResult.RowEvaluationDetail extraRow = new EvaluationResult.RowEvaluationDetail();
                extraRow.expectedBlockNumber = null;
                extraRow.predictedBlockNumber = pred.getBlockNumber();
                extraRow.predictedCbm = pred.getNetCbm();
                extraRow.blockNumberMatch = false;
                extraRow.dimensionsMatch = false;
                extraRow.cbmMatch = false;
                extraRow.errorCategory = EvaluationResult.ERR_EXTRA_ROW;
                extraRow.remarks = "Phantom or duplicate row generated by OCR.";
                result.addError(EvaluationResult.ERR_EXTRA_ROW);
                details.add(extraRow);
            }
        }

        result.setAlignedRows(alignedCount);
        result.setMissingRows(expectedBlocks.size() - alignedCount);
        result.setExtraRows(extraCount);
        result.setBlockNumberExactMatches(blockNumberMatches);
        result.setDimensionExactMatches(dimensionMatches);
        result.setCbmMatches(cbmMatches);
        result.setRowDetails(details);

        // Safe metric calculation with zero-handling
        int totalExp = expectedBlocks.size();
        int totalPred = predictedBlocks.size();

        if (totalPred > 0) {
            result.setPrecision((double) alignedCount / totalPred);
        } else {
            result.setPrecision(-1.0); // Undefined (no predictions made)
        }

        if (totalExp > 0) {
            result.setRecall((double) alignedCount / totalExp);
            result.setDimensionAccuracy((double) dimensionMatches / totalExp);
            result.setCbmAccuracy((double) cbmMatches / totalExp);
            result.setFieldCoverage((double) alignedCount / totalExp);
        } else {
            result.setRecall(-1.0); // Undefined (ground truth is empty)
            result.setDimensionAccuracy(-1.0);
            result.setCbmAccuracy(-1.0);
            result.setFieldCoverage(-1.0);
        }

        if (result.getPrecision() > 0 && result.getRecall() > 0) {
            double p = result.getPrecision();
            double r = result.getRecall();
            result.setF1((2.0 * p * r) / (p + r));
        } else if (result.getPrecision() == 0 || result.getRecall() == 0) {
            result.setF1(0.0);
        } else {
            result.setF1(-1.0);
        }

        return result;
    }
}
