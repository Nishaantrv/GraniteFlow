package com.graniteflow.app;

import com.graniteflow.app.data.api.ExtractionRiskContract;
import com.graniteflow.app.data.model.Block;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ExtractionRiskContractTest {

    @Test
    public void testCleanBlockPassesOfflineFallbackWithLowRisk() {
        List<Block> blocks = new ArrayList<>();
        Block b = new Block();
        b.setBlockNumber("101");
        b.setGrossLengthCm(280.0);
        b.setGrossHeightCm(140.0);
        b.setGrossWidthCm(120.0);
        b.setAllowanceCm(10.0);
        b.setNetCbm(3.861); // Exact match: (270 * 130 * 110) / 1,000,000 = 3.861
        blocks.add(b);

        ExtractionRiskContract.RiskInferenceRequest request =
                new ExtractionRiskContract.RiskInferenceRequest("QUARRY_MEASUREMENT_SLIP", "Google ML Kit", "v1.0.0", blocks);

        ExtractionRiskContract.RiskInferenceResponse response =
                ExtractionRiskContract.RiskInferenceResponse.createOfflineFallbackResponse(request);

        assertNotNull(response);
        assertEquals(ExtractionRiskContract.ServiceStatus.NOT_CONNECTED, response.getServiceStatus());
        assertEquals(1, response.getBlockEvaluations().size());

        ExtractionRiskContract.BlockRiskEvaluation eval = response.getBlockEvaluations().get(0);
        assertEquals(ExtractionRiskContract.RiskCategory.LOW_OBSERVED_RISK, eval.getRiskCategory());
        assertFalse("Clean block does not flag individual risk", eval.isRequiresHumanReview());
        // Since service is disconnected, overall review is safely mandated
        assertTrue("Disconnected service safely mandates human review", response.isOverallNeedsReview());
    }

    @Test
    public void testFlawedBlockFlagsNeedsReviewAndReasonCodes() {
        List<Block> blocks = new ArrayList<>();
        Block invalidBlock = new Block();
        invalidBlock.setBlockNumber("201");
        invalidBlock.setGrossLengthCm(0.0); // Zero dimension
        invalidBlock.setGrossHeightCm(140.0);
        invalidBlock.setGrossWidthCm(120.0);
        invalidBlock.setAllowanceCm(10.0);
        invalidBlock.setNetCbm(1.500); // Discrepant CBM
        blocks.add(invalidBlock);

        ExtractionRiskContract.RiskInferenceRequest request =
                new ExtractionRiskContract.RiskInferenceRequest("QUARRY_MEASUREMENT_SLIP", "Google ML Kit", "v1.0.0", blocks);

        ExtractionRiskContract.RiskInferenceResponse response =
                ExtractionRiskContract.RiskInferenceResponse.createOfflineFallbackResponse(request);

        assertNotNull(response);
        assertEquals(1, response.getBlockEvaluations().size());

        ExtractionRiskContract.BlockRiskEvaluation eval = response.getBlockEvaluations().get(0);
        assertEquals(ExtractionRiskContract.RiskCategory.NEEDS_REVIEW, eval.getRiskCategory());
        assertTrue(eval.isRequiresHumanReview());
        assertTrue(eval.getReasonCodes().contains("INVALID_DIMENSIONS"));
        assertTrue(response.isOverallNeedsReview());
    }

    @Test
    public void testExcessiveAllowanceFlagged() {
        List<Block> blocks = new ArrayList<>();
        Block b = new Block();
        b.setBlockNumber("301");
        b.setGrossLengthCm(100.0);
        b.setGrossHeightCm(100.0);
        b.setGrossWidthCm(100.0);
        b.setAllowanceCm(120.0); // Allowance exceeds dimensions
        b.setNetCbm(0.0);
        blocks.add(b);

        ExtractionRiskContract.RiskInferenceRequest request =
                new ExtractionRiskContract.RiskInferenceRequest("QUARRY_MEASUREMENT_SLIP", "Google ML Kit", "v1.0.0", blocks);

        ExtractionRiskContract.RiskInferenceResponse response =
                ExtractionRiskContract.RiskInferenceResponse.createOfflineFallbackResponse(request);

        ExtractionRiskContract.BlockRiskEvaluation eval = response.getBlockEvaluations().get(0);
        assertEquals(ExtractionRiskContract.RiskCategory.NEEDS_REVIEW, eval.getRiskCategory());
        assertTrue(eval.getReasonCodes().contains("EXCESSIVE_ALLOWANCE"));
    }
}
