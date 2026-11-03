package com.graniteflow.app.data.api;

import java.io.Serializable;
import java.util.List;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface DocumentIntelligenceService {

    class OcrExtractionResult implements Serializable {
        public String engine; // "Azure Document Intelligence" or "Google Document AI"
        public double confidence; // e.g. 0.94
        public String material;
        public String inspector;
        public String locationCode;
        public double defaultAllowance;
        public List<ExtractedBlockDto> blocks;
        public List<String> warnings;
    }

    class ExtractedBlockDto implements Serializable {
        public String blockNumber;
        public double grossLength;
        public double grossHeight;
        public double grossWidth;
        public double allowance;
        public double netCbm;
        public double confidence;
        public String remarks;
    }

    @Multipart
    @POST("api/v1/ocr/extract-inspection")
    Call<OcrExtractionResult> extractInspectionDocument(@Part MultipartBody.Part file);

    @retrofit2.http.POST("api/v1/ocr/evaluate-extraction-risk")
    Call<ExtractionRiskContract.RiskInferenceResponse> evaluateExtractionRisk(@retrofit2.http.Body ExtractionRiskContract.RiskInferenceRequest request);
}
