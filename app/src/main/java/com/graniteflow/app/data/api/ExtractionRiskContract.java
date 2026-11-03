package com.graniteflow.app.data.api;

import com.graniteflow.app.data.model.Block;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Typed API contract for the GraniteFlow ML Extraction Risk Inference Service.
 * Implements pre-review risk scoring requests, responses, explainable reason codes,
 * and safe offline/fallback behavior.
 */
public class ExtractionRiskContract {

    public enum RiskCategory {
        LOW_OBSERVED_RISK,
        NEEDS_REVIEW
    }

    public enum ServiceStatus {
        CONNECTED,
        NOT_CONNECTED,
        ERROR
    }

    /**
     * Request payload containing unreviewed OCR extracted blocks.
     */
    public static class RiskInferenceRequest implements Serializable {
        private String documentType;
        private String ocrEngine;
        private String parserVersion;
        private List<Block> blocks;
        private long requestTimestampMs;

        public RiskInferenceRequest(String documentType, String ocrEngine, String parserVersion, List<Block> blocks) {
            this.documentType = documentType != null ? documentType : "QUARRY_MEASUREMENT_SLIP";
            this.ocrEngine = ocrEngine != null ? ocrEngine : "Google ML Kit";
            this.parserVersion = parserVersion != null ? parserVersion : "v1.0.0";
            this.blocks = blocks != null ? new ArrayList<>(blocks) : new ArrayList<>();
            this.requestTimestampMs = System.currentTimeMillis();
        }

        public String getDocumentType() { return documentType; }
        public String getOcrEngine() { return ocrEngine; }
        public String getParserVersion() { return parserVersion; }
        public List<Block> getBlocks() { return blocks; }
        public long getRequestTimestampMs() { return requestTimestampMs; }
    }

    /**
     * Assessment for an individual extracted block.
     */
    public static class BlockRiskEvaluation implements Serializable {
        private String blockNumber;
        private RiskCategory riskCategory;
        private double riskProbability;
        private List<String> reasonCodes;
        private boolean requiresHumanReview;

        public BlockRiskEvaluation(String blockNumber, RiskCategory riskCategory, double riskProbability,
                                   List<String> reasonCodes, boolean requiresHumanReview) {
            this.blockNumber = blockNumber;
            this.riskCategory = riskCategory;
            this.riskProbability = riskProbability;
            this.reasonCodes = reasonCodes != null ? reasonCodes : new ArrayList<>();
            this.requiresHumanReview = requiresHumanReview;
        }

        public String getBlockNumber() { return blockNumber; }
        public RiskCategory getRiskCategory() { return riskCategory; }
        public double getRiskProbability() { return riskProbability; }
        public List<String> getReasonCodes() { return reasonCodes; }
        public boolean isRequiresHumanReview() { return requiresHumanReview; }
    }

    /**
     * Overall inference response for the document.
     */
    public static class RiskInferenceResponse implements Serializable {
        private ServiceStatus serviceStatus;
        private String modelVersion;
        private List<BlockRiskEvaluation> blockEvaluations;
        private boolean overallNeedsReview;
        private String statusMessage;

        public RiskInferenceResponse(ServiceStatus serviceStatus, String modelVersion,
                                     List<BlockRiskEvaluation> blockEvaluations, String statusMessage) {
            this.serviceStatus = serviceStatus;
            this.modelVersion = modelVersion;
            this.blockEvaluations = blockEvaluations != null ? blockEvaluations : new ArrayList<>();
            this.statusMessage = statusMessage;

            // Compute aggregate review requirement
            boolean needsReview = false;
            for (BlockRiskEvaluation eval : this.blockEvaluations) {
                if (eval.isRequiresHumanReview() || eval.getRiskCategory() == RiskCategory.NEEDS_REVIEW) {
                    needsReview = true;
                    break;
                }
            }
            // Disconnected or error services always require human review
            if (serviceStatus != ServiceStatus.CONNECTED) {
                needsReview = true;
            }
            this.overallNeedsReview = needsReview;
        }

        public ServiceStatus getServiceStatus() { return serviceStatus; }
        public String getModelVersion() { return modelVersion; }
        public List<BlockRiskEvaluation> getBlockEvaluations() { return blockEvaluations; }
        public boolean isOverallNeedsReview() { return overallNeedsReview; }
        public String getStatusMessage() { return statusMessage; }

        /**
         * Offline fallback evaluator: safely applies deterministic physical rules
         * when backend ML service is not deployed or unreachable.
         */
        public static RiskInferenceResponse createOfflineFallbackResponse(RiskInferenceRequest request) {
            List<BlockRiskEvaluation> evaluations = new ArrayList<>();
            for (Block block : request.getBlocks()) {
                List<String> reasons = new ArrayList<>();
                boolean risky = false;

                if (block.getGrossLengthCm() <= 0 || block.getGrossHeightCm() <= 0 || block.getGrossWidthCm() <= 0) {
                    reasons.add("INVALID_DIMENSIONS");
                    risky = true;
                }
                if (block.getAllowanceCm() >= block.getGrossLengthCm() ||
                    block.getAllowanceCm() >= block.getGrossHeightCm() ||
                    block.getAllowanceCm() >= block.getGrossWidthCm()) {
                    reasons.add("EXCESSIVE_ALLOWANCE");
                    risky = true;
                }

                // CBM formula check: (L_net * H_net * W_net) / 1,000,000
                double netL = Math.max(0, block.getGrossLengthCm() - block.getAllowanceCm());
                double netH = Math.max(0, block.getGrossHeightCm() - block.getAllowanceCm());
                double netW = Math.max(0, block.getGrossWidthCm() - block.getAllowanceCm());
                double calcCbm = Math.round(((netL * netH * netW) / 1000000.0) * 1000.0) / 1000.0;
                double diff = Math.abs(block.getNetCbm() - calcCbm);
                if (diff > 0.005 && block.getNetCbm() > 0) {
                    reasons.add("MISMATCHED_CBM_FORMULA");
                    risky = true;
                }

                if (block.getBlockNumber() == null || block.getBlockNumber().trim().isEmpty()) {
                    reasons.add("MISSING_BLOCK_NUMBER");
                    risky = true;
                }

                reasons.add("OFFLINE_HEURISTIC_EVALUATED");

                RiskCategory category = risky ? RiskCategory.NEEDS_REVIEW : RiskCategory.LOW_OBSERVED_RISK;
                double prob = risky ? 0.90 : 0.15;
                // Human review is always preserved
                evaluations.add(new BlockRiskEvaluation(
                        block.getBlockNumber(), category, prob, reasons, risky
                ));
            }

            return new RiskInferenceResponse(
                    ServiceStatus.NOT_CONNECTED,
                    "offline-deterministic-fallback-v1.0",
                    evaluations,
                    "ML inference service is offline. Applied local physical validation rules. Human review required."
            );
        }
    }
}
