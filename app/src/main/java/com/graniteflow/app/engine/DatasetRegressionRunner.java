package com.graniteflow.app.engine;

import com.graniteflow.app.data.dataset.EvaluationResult;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DatasetRegressionRunner {

    public static class RegressionThresholds implements Serializable {
        public double maxPrecisionDrop = 0.02;
        public double maxRecallDrop = 0.02;
        public double maxDimensionAccuracyDrop = 0.01;
        public int maxCriticalErrorIncrease = 0;

        public RegressionThresholds() {}
    }

    public static class RegressionReport implements Serializable {
        public String baselineVersion;
        public String candidateVersion;
        public boolean isRegressionDetected;
        public List<String> regressionReasons = new ArrayList<>();
        public double baselineDimensionAccuracy;
        public double candidateDimensionAccuracy;
        public double baselineRecall;
        public double candidateRecall;
        public double baselinePrecision;
        public double candidatePrecision;
        public int baselineCriticalErrors;
        public int candidateCriticalErrors;
        public List<String> regressedRecordIds = new ArrayList<>();

        public RegressionReport() {}
    }

    /**
     * Compares evaluations from a candidate parser against a baseline over the same approved records.
     */
    public static RegressionReport runRegression(List<EvaluationResult> baselineList,
                                                 List<EvaluationResult> candidateList,
                                                 RegressionThresholds thresholds) {
        if (thresholds == null) thresholds = new RegressionThresholds();
        RegressionReport report = new RegressionReport();

        if (baselineList == null || baselineList.isEmpty() || candidateList == null || candidateList.isEmpty()) {
            report.isRegressionDetected = false;
            report.regressionReasons.add("Insufficient baseline or candidate evaluation results to compute regression.");
            return report;
        }

        report.baselineVersion = baselineList.get(0).getParserVersion();
        report.candidateVersion = candidateList.get(0).getParserVersion();

        // Calculate aggregates
        double baseDimSum = 0;
        double candDimSum = 0;
        double baseRecSum = 0;
        double candRecSum = 0;
        double basePrecSum = 0;
        double candPrecSum = 0;
        int baseErrors = 0;
        int candErrors = 0;

        for (EvaluationResult r : baselineList) {
            if (r.getDimensionAccuracy() >= 0) baseDimSum += r.getDimensionAccuracy();
            if (r.getRecall() >= 0) baseRecSum += r.getRecall();
            if (r.getPrecision() >= 0) basePrecSum += r.getPrecision();
            baseErrors += r.getMissingRows() + r.getErrorCategoryCounts().values().stream().mapToInt(Integer::intValue).sum();
        }

        for (EvaluationResult r : candidateList) {
            if (r.getDimensionAccuracy() >= 0) candDimSum += r.getDimensionAccuracy();
            if (r.getRecall() >= 0) candRecSum += r.getRecall();
            if (r.getPrecision() >= 0) candPrecSum += r.getPrecision();
            candErrors += r.getMissingRows() + r.getErrorCategoryCounts().values().stream().mapToInt(Integer::intValue).sum();
        }

        int bSize = baselineList.size();
        int cSize = candidateList.size();

        report.baselineDimensionAccuracy = baseDimSum / bSize;
        report.candidateDimensionAccuracy = candDimSum / cSize;
        report.baselineRecall = baseRecSum / bSize;
        report.candidateRecall = candRecSum / cSize;
        report.baselinePrecision = basePrecSum / bSize;
        report.candidatePrecision = candPrecSum / cSize;
        report.baselineCriticalErrors = baseErrors;
        report.candidateCriticalErrors = candErrors;

        // Check Gates
        boolean regression = false;

        // 1. Dimension Accuracy Check
        double dimDiff = report.baselineDimensionAccuracy - report.candidateDimensionAccuracy;
        if (dimDiff > thresholds.maxDimensionAccuracyDrop) {
            regression = true;
            report.regressionReasons.add(String.format("Dimension Accuracy dropped from %.2f%% to %.2f%% (Drop: %.2f%% > Threshold: %.2f%%)",
                    report.baselineDimensionAccuracy * 100, report.candidateDimensionAccuracy * 100, dimDiff * 100, thresholds.maxDimensionAccuracyDrop * 100));
        }

        // 2. Recall Check
        double recDiff = report.baselineRecall - report.candidateRecall;
        if (recDiff > thresholds.maxRecallDrop) {
            regression = true;
            report.regressionReasons.add(String.format("Row Recall dropped from %.2f%% to %.2f%% (Drop: %.2f%% > Threshold: %.2f%%)",
                    report.baselineRecall * 100, report.candidateRecall * 100, recDiff * 100, thresholds.maxRecallDrop * 100));
        }

        // 3. Precision Check
        double precDiff = report.baselinePrecision - report.candidatePrecision;
        if (precDiff > thresholds.maxPrecisionDrop) {
            regression = true;
            report.regressionReasons.add(String.format("Row Precision dropped from %.2f%% to %.2f%% (Drop: %.2f%% > Threshold: %.2f%%)",
                    report.baselinePrecision * 100, report.candidatePrecision * 100, precDiff * 100, thresholds.maxPrecisionDrop * 100));
        }

        // 4. Critical Errors Increase
        int errDiff = report.candidateCriticalErrors - report.baselineCriticalErrors;
        if (errDiff > thresholds.maxCriticalErrorIncrease) {
            regression = true;
            report.regressionReasons.add(String.format("Critical extraction errors increased from %d to %d (+%d errors)",
                    report.baselineCriticalErrors, report.candidateCriticalErrors, errDiff));
        }

        report.isRegressionDetected = regression;
        return report;
    }
}
