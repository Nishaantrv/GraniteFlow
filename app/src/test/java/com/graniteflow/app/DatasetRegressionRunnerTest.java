package com.graniteflow.app;

import com.graniteflow.app.data.dataset.EvaluationResult;
import com.graniteflow.app.engine.DatasetRegressionRunner;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DatasetRegressionRunnerTest {

    @Test
    public void testIdenticalBaselineAndCandidatePassesWithoutRegression() {
        EvaluationResult base = new EvaluationResult();
        base.setParserVersion("v1.0.0");
        base.setPrecision(0.95);
        base.setRecall(0.92);
        base.setDimensionAccuracy(0.90);

        EvaluationResult cand = new EvaluationResult();
        cand.setParserVersion("v1.1.0");
        cand.setPrecision(0.96);
        cand.setRecall(0.93);
        cand.setDimensionAccuracy(0.91);

        List<EvaluationResult> baseList = Collections.singletonList(base);
        List<EvaluationResult> candList = Collections.singletonList(cand);

        DatasetRegressionRunner.RegressionReport report =
                DatasetRegressionRunner.runRegression(baseList, candList, new DatasetRegressionRunner.RegressionThresholds());

        assertNotNull(report);
        assertFalse("No regression should be detected on improved performance", report.isRegressionDetected);
        assertTrue(report.regressionReasons.isEmpty());
    }

    @Test
    public void testDimensionAccuracyDropTriggersRegression() {
        EvaluationResult base = new EvaluationResult();
        base.setParserVersion("v1.0.0");
        base.setDimensionAccuracy(0.95);
        base.setRecall(0.90);
        base.setPrecision(0.90);

        EvaluationResult cand = new EvaluationResult();
        cand.setParserVersion("v1.2.0");
        cand.setDimensionAccuracy(0.85); // 10% drop, exceeding 1% max drop threshold
        cand.setRecall(0.90);
        cand.setPrecision(0.90);

        DatasetRegressionRunner.RegressionThresholds thresholds = new DatasetRegressionRunner.RegressionThresholds();
        thresholds.maxDimensionAccuracyDrop = 0.01;

        DatasetRegressionRunner.RegressionReport report =
                DatasetRegressionRunner.runRegression(
                        Collections.singletonList(base),
                        Collections.singletonList(cand),
                        thresholds
                );

        assertTrue("Regression must be flagged when dimension accuracy drops", report.isRegressionDetected);
        boolean foundDimReason = false;
        for (String r : report.regressionReasons) {
            if (r.contains("Dimension Accuracy dropped")) {
                foundDimReason = true;
                break;
            }
        }
        assertTrue("Report must include dimension accuracy drop reason", foundDimReason);
    }

    @Test
    public void testCriticalErrorIncreaseTriggersRegression() {
        EvaluationResult base = new EvaluationResult();
        base.setParserVersion("v1.0.0");
        base.setDimensionAccuracy(0.90);
        base.setRecall(0.90);
        base.setPrecision(0.90);

        EvaluationResult cand = new EvaluationResult();
        cand.setParserVersion("v1.3.0");
        cand.addError(EvaluationResult.ERR_DIMENSION_EXTRACTION);
        cand.addError(EvaluationResult.ERR_CBM_MISMATCH); // Added 2 error categories
        cand.setDimensionAccuracy(0.90);
        cand.setRecall(0.90);
        cand.setPrecision(0.90);

        DatasetRegressionRunner.RegressionReport report =
                DatasetRegressionRunner.runRegression(
                        Collections.singletonList(base),
                        Collections.singletonList(cand),
                        new DatasetRegressionRunner.RegressionThresholds()
                );

        assertTrue("Regression must be flagged when critical errors increase", report.isRegressionDetected);
    }

    @Test
    public void testEmptyResultsHandledSafely() {
        DatasetRegressionRunner.RegressionReport report =
                DatasetRegressionRunner.runRegression(Collections.emptyList(), Collections.emptyList(), null);

        assertNotNull(report);
        assertFalse(report.isRegressionDetected);
        assertTrue(report.regressionReasons.get(0).contains("Insufficient baseline or candidate"));
    }
}
