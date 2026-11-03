package com.graniteflow.app.ocr;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.mlkit.vision.text.Text;
import com.graniteflow.app.data.model.Block;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Gemini Vision AI Scanner for Quarry Measurement Slips.
 * Supports multi-model retry (gemini-flash-latest, gemini-3.8-flash, gemini-3.5-flash)
 * with EXIF orientation correction and robust fallback to Google ML Kit Text Recognition v2.
 */
public class GeminiAiScanEngine {

    private static final String TAG = "GraniteFlowGeminiAI";

    private static final String GEMINI_API_KEY = System.getenv("GEMINI_API_KEY") != null ? System.getenv("GEMINI_API_KEY") : "";
    private static final String[] CANDIDATE_MODELS = {
            "gemini-3.8-flash",
            "gemini-flash-latest",
            "gemini-3.5-flash"
    };

    private static final String EXTRACTION_PROMPT =
            "You are an expert Indian granite quarry measurement slip reader and dimensional surveyor.\n" +
            "This image is a photographed handwritten or printed quarry measurement slip / block dispatch challan / inspection abstract from an Indian granite quarry (Andhra Pradesh, Tamil Nadu, or Karnataka).\n\n" +
            "FEW-SHOT REGIONAL FORMAT EXAMPLES:\n" +
            "Example A (Ongole / Chimakurthy Black Galaxy dispatch):\n" +
            "Header: CHETTINAD GRANITES & EXPORTS LTD | MATERIAL: BLACK GALAXY | LOCATION: ONG-QUARRY-04 | ALLOWANCE: 10 CM\n" +
            "Row: 1 | BG-501 | 285 | 145 | 125 | 10 | 4.269 -> Length: 285, Height: 145, Width: 125, Allowance: 10, CBM: 4.269\n\n" +
            "Example B (Kanakapura / Bangalore SRE Himalayan Blue inspection):\n" +
            "Header: DAKSHIN EXPORTS | MATERIAL: SRE HIMALAYAN BLUE | LOCATION: XOW/XMN | ALLOW: 5 CM\n" +
            "Row: 1 | 187 | 275 x 155 x 175 | 5 cm | 7.459 -> Block: 187, Length: 280 (Gross) or 275 (Net), Height: 160/155, Width: 180/175\n\n" +
            "Example C (Single Block Slip / Diagram challan):\n" +
            "Header: BLOCK NO : 301 | GROSS 280 185 185 9.583 | 10 CM | NET 270 175 175 8.269 | L280 H185 W185\n" +
            "-> Block: 301, Gross Length: 280, Gross Height: 185, Gross Width: 185, Allowance: 10, Net CBM: 8.269\n\n" +
            "Analyze the image and extract all data into strict JSON with the following structure:\n" +
            "{\n" +
            "  \"customerName\": \"string (buyer/consignee e.g. DAKSHIN EXPORTS, VENKAT SAI MINERALS, CHETTINAD)\",\n" +
            "  \"materialName\": \"string (e.g. BLACK GALAXY, SRE HIMALAYAN BLUE, KATTIMA BLACK, STEEL GREY)\",\n" +
            "  \"locationCode\": \"string (quarry code, site, port e.g. ONG-QUARRY-04, XOW/XMN, CHIKKA-02)\",\n" +
            "  \"date\": \"string (DD/MM/YYYY)\",\n" +
            "  \"inspectorName\": \"string (inspector/surveyor name)\",\n" +
            "  \"defaultAllowanceCm\": 10.0,\n" +
            "  \"blocks\": [\n" +
            "    {\n" +
            "      \"blockNumber\": \"string (NUMERIC ONLY in sequential order, e.g. 101, 102, 103, 104. Never output letters or garbled words)\",\n" +
            "      \"grossLengthCm\": 285.0,\n" +
            "      \"grossHeightCm\": 145.0,\n" +
            "      \"grossWidthCm\": 125.0,\n" +
            "      \"allowanceCm\": 10.0,\n" +
            "      \"reportedNetCbm\": 4.269,\n" +
            "      \"remarks\": \"string\"\n" +
            "    }\n" +
            "  ]\n" +
            "}\n" +
            "MATHEMATICAL CHECKSUM INSTRUCTION:\n" +
            "Verify your reading: Net CBM = ((grossLengthCm - allowanceCm) * (grossHeightCm - allowanceCm) * (grossWidthCm - allowanceCm)) / 1000000.\n" +
            "If handwritten digits look ambiguous (like 3 vs 8, or 1 vs 7, or 0 vs 6), pick the digit that makes the CBM formula mathematically consistent.\n" +
            "Dimensions must be in centimeters (cm). Return ONLY valid JSON.";

    public interface ScanCallback {
        void onSuccess(GraniteSlipParser.ParsedInspectionResult result, String engineUsed);
        void onFailure(Exception exception);
    }

    private final OkHttpClient httpClient;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private final MlKitOcrEngine fallbackOcrEngine;

    public GeminiAiScanEngine() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(35, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .build();
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.fallbackOcrEngine = new MlKitOcrEngine();
    }

    public void processUri(@NonNull Context context, @NonNull Uri imageUri, @NonNull ScanCallback callback) {
        Context appContext = context.getApplicationContext();
        executor.execute(() -> {
            Bitmap bitmap = null;
            try {
                try (InputStream is = appContext.getContentResolver().openInputStream(imageUri)) {
                    bitmap = BitmapFactory.decodeStream(is);
                }
                if (bitmap == null) {
                    throw new IllegalArgumentException("Could not decode image from URI: " + imageUri);
                }
                bitmap = rotateBitmapIfRequired(appContext, imageUri, bitmap);

                // Tier 1: Gemini Vision AI (3.8-flash)
                ExtractionResult ext = callGeminiWithModelFallback(bitmap);
                mainHandler.post(() -> callback.onSuccess(ext.result, ext.engineUsed));
            } catch (Exception geminiErr) {
                Log.w(TAG, "Gemini Vision calls failed: " + geminiErr.getMessage() + ", trying Python OCR Server...");
                final Bitmap finalBitmap = bitmap;
                if (finalBitmap != null) {
                    try {
                        // Tier 2: Python OCR Model Server
                        GraniteSlipParser.ParsedInspectionResult pyResult = callPythonOcrServer(appContext, finalBitmap);
                        pyResult.engineUsed = "GraniteFlow Python OCR (Local Model Server)";
                        mainHandler.post(() -> callback.onSuccess(pyResult, pyResult.engineUsed));
                        return;
                    } catch (Exception pyErr) {
                        Log.w(TAG, "Python OCR server call failed: " + pyErr.getMessage() + ", falling back to ML Kit...");
                    }
                }

                // Tier 3: On-Device Google ML Kit Text Recognition v2
                mainHandler.post(() -> fallbackOcrEngine.processImage(appContext, imageUri, new MlKitOcrEngine.OcrCallback() {
                    @Override
                    public void onSuccess(String rawText, Text textResult) {
                        GraniteSlipParser.ParsedInspectionResult parsed = GraniteSlipParser.parse(rawText);
                        parsed.engineUsed = "Google ML Kit (Offline Local OCR)";
                        callback.onSuccess(parsed, parsed.engineUsed);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        callback.onFailure(geminiErr);
                    }
                }));
            }
        });
    }

    public void processBitmap(@NonNull Bitmap bitmap, @NonNull ScanCallback callback) {
        executor.execute(() -> {
            try {
                // Tier 1: Gemini Vision AI
                ExtractionResult ext = callGeminiWithModelFallback(bitmap);
                mainHandler.post(() -> callback.onSuccess(ext.result, ext.engineUsed));
            } catch (Exception geminiErr) {
                Log.w(TAG, "Gemini Vision calls failed, trying Python OCR Server...");
                try {
                    // Tier 2: Python OCR Model Server
                    // Use Application Context if available, else standard fallback
                    GraniteSlipParser.ParsedInspectionResult pyResult = callPythonOcrServerDirect(bitmap, "http://10.0.2.2:8080/");
                    pyResult.engineUsed = "GraniteFlow Python OCR (Local Model Server)";
                    mainHandler.post(() -> callback.onSuccess(pyResult, pyResult.engineUsed));
                    return;
                } catch (Exception pyErr) {
                    Log.w(TAG, "Python OCR server call failed: " + pyErr.getMessage() + ", falling back to ML Kit...");
                }

                // Tier 3: On-Device Google ML Kit
                mainHandler.post(() -> fallbackOcrEngine.processBitmap(bitmap, new MlKitOcrEngine.OcrCallback() {
                    @Override
                    public void onSuccess(String rawText, Text textResult) {
                        GraniteSlipParser.ParsedInspectionResult parsed = GraniteSlipParser.parse(rawText);
                        parsed.engineUsed = "Google ML Kit (Offline Local OCR)";
                        callback.onSuccess(parsed, parsed.engineUsed);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        callback.onFailure(geminiErr);
                    }
                }));
            }
        });
    }

    public void processAssetImage(@NonNull Context context, @NonNull String assetFileName, @NonNull ScanCallback callback) {
        Context appContext = context.getApplicationContext();
        executor.execute(() -> {
            Bitmap bitmap = null;
            try (InputStream is = appContext.getAssets().open(assetFileName)) {
                bitmap = BitmapFactory.decodeStream(is);
                if (bitmap == null) {
                    throw new IllegalArgumentException("Asset image could not be decoded: " + assetFileName);
                }
                // Tier 1: Gemini Vision AI
                ExtractionResult ext = callGeminiWithModelFallback(bitmap);
                mainHandler.post(() -> callback.onSuccess(ext.result, ext.engineUsed));
            } catch (Exception geminiErr) {
                Log.w(TAG, "Gemini Vision asset scan failed: " + geminiErr.getMessage() + ", trying Python OCR Server...");
                final Bitmap finalBitmap = bitmap;
                if (finalBitmap != null) {
                    try {
                        // Tier 2: Python OCR Model Server
                        GraniteSlipParser.ParsedInspectionResult pyResult = callPythonOcrServer(appContext, finalBitmap);
                        pyResult.engineUsed = "GraniteFlow Python OCR (Local Model Server)";
                        mainHandler.post(() -> callback.onSuccess(pyResult, pyResult.engineUsed));
                        return;
                    } catch (Exception pyErr) {
                        Log.w(TAG, "Python OCR server call failed: " + pyErr.getMessage() + ", falling back to ML Kit...");
                    }

                    // Tier 3: On-Device Google ML Kit
                    mainHandler.post(() -> fallbackOcrEngine.processBitmap(finalBitmap, new MlKitOcrEngine.OcrCallback() {
                        @Override
                        public void onSuccess(String rawText, Text textResult) {
                            GraniteSlipParser.ParsedInspectionResult parsed = GraniteSlipParser.parse(rawText);
                            parsed.engineUsed = "Google ML Kit (Offline Local OCR)";
                            callback.onSuccess(parsed, parsed.engineUsed);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            callback.onFailure(geminiErr);
                        }
                    }));
                } else {
                    mainHandler.post(() -> callback.onFailure(geminiErr));
                }
            }
        });
    }

    private GraniteSlipParser.ParsedInspectionResult callPythonOcrServer(@NonNull Context context, @NonNull Bitmap sourceBitmap) throws Exception {
        String base = new com.graniteflow.app.data.storage.LocalDataManager(context).getBackendUrl();
        return callPythonOcrServerDirect(sourceBitmap, base);
    }

    private GraniteSlipParser.ParsedInspectionResult callPythonOcrServerDirect(@NonNull Bitmap sourceBitmap, String baseUrl) throws Exception {
        String base = (baseUrl != null && !baseUrl.isEmpty()) ? baseUrl : "http://10.0.2.2:8080/";
        if (!base.endsWith("/")) base += "/";
        String endpoint = base + "api/v1/ocr/extract-inspection";

        Bitmap scaled = scaleDownIfNeeded(sourceBitmap, 1600);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        scaled.compress(Bitmap.CompressFormat.JPEG, 90, baos);
        byte[] imageBytes = baos.toByteArray();

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "quarry_slip.jpg",
                        RequestBody.create(imageBytes, MediaType.parse("image/jpeg")))
                .build();

        Request request = new Request.Builder()
                .url(endpoint)
                .post(requestBody)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new java.io.IOException("Python OCR server HTTP " + response.code());
            }
            String respBody = response.body() != null ? response.body().string() : "";
            GraniteSlipParser.ParsedInspectionResult parsed = parseGeminiJson(respBody);
            if (parsed.blocks.isEmpty()) {
                throw new RuntimeException("Python OCR server returned 0 blocks");
            }
            return parsed;
        }
    }

    public static class ExtractionResult {
        public final GraniteSlipParser.ParsedInspectionResult result;
        public final String engineUsed;

        public ExtractionResult(GraniteSlipParser.ParsedInspectionResult result, String engineUsed) {
            this.result = result;
            this.engineUsed = engineUsed;
        }
    }

    private ExtractionResult callGeminiWithModelFallback(@NonNull Bitmap sourceBitmap) throws Exception {
        Exception lastException = null;
        for (String model : CANDIDATE_MODELS) {
            try {
                Log.d(TAG, "Attempting Gemini Vision extraction with model: " + model);
                GraniteSlipParser.ParsedInspectionResult parsed = executeGeminiCall(sourceBitmap, model);
                String engineName = "Gemini Vision AI (" + model + ")";
                parsed.engineUsed = engineName;
                return new ExtractionResult(parsed, engineName);
            } catch (Exception e) {
                Log.w(TAG, "Model " + model + " attempt failed (" + e.getMessage() + "), trying next candidate...");
                lastException = e;
            }
        }
        throw lastException != null ? lastException : new RuntimeException("All Gemini models failed.");
    }

    private GraniteSlipParser.ParsedInspectionResult executeGeminiCall(@NonNull Bitmap sourceBitmap, String modelName) throws Exception {
        Bitmap scaled = scaleDownIfNeeded(sourceBitmap, 1600);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, baos);
        String base64Image = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);

        JsonObject inlineData = new JsonObject();
        inlineData.addProperty("mimeType", "image/jpeg");
        inlineData.addProperty("data", base64Image);

        JsonObject imagePart = new JsonObject();
        imagePart.add("inlineData", inlineData);

        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", EXTRACTION_PROMPT);

        JsonArray parts = new JsonArray();
        parts.add(textPart);
        parts.add(imagePart);

        JsonObject content = new JsonObject();
        content.addProperty("role", "user");
        content.add("parts", parts);

        JsonArray contents = new JsonArray();
        contents.add(content);

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("responseMimeType", "application/json");
        generationConfig.addProperty("temperature", 0.1);

        JsonObject requestPayload = new JsonObject();
        requestPayload.add("contents", contents);
        requestPayload.add("generationConfig", generationConfig);

        RequestBody body = RequestBody.create(
                requestPayload.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=" + GEMINI_API_KEY;

        Request request = new Request.Builder()
                .url(endpoint)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String respBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new RuntimeException("HTTP " + response.code() + " on " + modelName + ": " + respBody);
            }

            JsonObject root = JsonParser.parseString(respBody).getAsJsonObject();
            JsonArray candidates = root.getAsJsonArray("candidates");
            if (candidates == null || candidates.isEmpty()) {
                throw new RuntimeException("No candidates returned from " + modelName);
            }

            JsonObject candidateContent = candidates.get(0).getAsJsonObject().getAsJsonObject("content");
            JsonArray respParts = candidateContent.getAsJsonArray("parts");
            String jsonText = respParts.get(0).getAsJsonObject().get("text").getAsString();
            Log.d(TAG, "[" + modelName + "] Gemini Vision extracted JSON: " + jsonText);

            return parseGeminiJson(jsonText);
        }
    }

    private GraniteSlipParser.ParsedInspectionResult parseGeminiJson(String jsonText) {
        GraniteSlipParser.ParsedInspectionResult result = new GraniteSlipParser.ParsedInspectionResult();
        JsonObject obj = JsonParser.parseString(jsonText.trim()).getAsJsonObject();

        String cust = optString(obj, "customerName", "");
        if (!cust.isEmpty()) {
            result.inspectorName = cust; // Used to seed customer if not set
        }

        String mat = optString(obj, "materialName", optString(obj, "material", ""));
        if (!mat.isEmpty()) {
            result.materialName = mat.toUpperCase();
        }

        String loc = optString(obj, "locationCode", "");
        if (!loc.isEmpty()) {
            result.locationCode = loc.toUpperCase();
        }

        String dt = optString(obj, "date", "");
        if (!dt.isEmpty()) {
            result.date = dt;
        }

        String insp = optString(obj, "inspectorName", optString(obj, "inspector", ""));
        if (!insp.isEmpty()) {
            result.inspectorName = insp;
        }

        double defAllw = optDouble(obj, "defaultAllowanceCm", optDouble(obj, "defaultAllowance", 10.0));
        if (defAllw > 0) {
            result.defaultAllowanceCm = defAllw;
        }

        StringBuilder rawTranscript = new StringBuilder();
        if (!cust.isEmpty()) rawTranscript.append("PARTY: ").append(cust).append("\n");
        if (!mat.isEmpty()) rawTranscript.append("MATERIAL: ").append(mat).append("\n");
        if (!loc.isEmpty()) rawTranscript.append("LOCATION: ").append(loc).append("\n");

        if (obj.has("blocks") && obj.get("blocks").isJsonArray()) {
            JsonArray arr = obj.getAsJsonArray("blocks");
            int idx = 1;
            for (JsonElement el : arr) {
                if (!el.isJsonObject()) continue;
                JsonObject bObj = el.getAsJsonObject();
                String blockNo = optString(bObj, "blockNumber", String.valueOf(100 + idx));
                double l = optDouble(bObj, "grossLengthCm", optDouble(bObj, "grossLength", 0));
                double h = optDouble(bObj, "grossHeightCm", optDouble(bObj, "grossHeight", 0));
                double w = optDouble(bObj, "grossWidthCm", optDouble(bObj, "grossWidth", 0));
                double allw = optDouble(bObj, "allowanceCm", optDouble(bObj, "allowance", result.defaultAllowanceCm));
                String remarks = optString(bObj, "remarks", "");
                if ("-".equals(remarks)) remarks = "";

                // Normalize if returned in meters (e.g. 2.80)
                if (l > 0 && l < 10) l = Math.round(l * 100);
                if (h > 0 && h < 10) h = Math.round(h * 100);
                if (w > 0 && w < 10) w = Math.round(w * 100);

                double reportedCbm = optDouble(bObj, "reportedNetCbm", optDouble(bObj, "netCbm", -1.0));
                boolean verified = false;
                if (reportedCbm > 0 && l > 0 && h > 0 && w > 0) {
                    double[] healed = com.graniteflow.app.engine.DimensionCalculator.autoHealMisreadDimensions(l, h, w, allw, reportedCbm);
                    if (healed != null) {
                        l = healed[0];
                        h = healed[1];
                        w = healed[2];
                        verified = true;
                    }
                }

                if (l > 0 && h > 0 && w > 0) {
                    Block block = new Block(blockNo, l, h, w, allw);
                    block.setRemarks(remarks);
                    block.setMathVerified(verified);
                    block.recalculate();
                    result.blocks.add(block);
                    String rowLine = String.format("#%s: %.0f x %.0f x %.0f cm (Allw %.0fcm) -> %.3f m³ %s",
                            blockNo, l, h, w, allw, block.getNetCbm(), (verified ? "✓ [Math Verified]" : ""));
                    result.rawRowTexts.add(rowLine);
                    rawTranscript.append(rowLine).append("\n");
                    idx++;
                }
            }
        }

        // Enforce rule-based numeric only and ordered sequence
        com.graniteflow.app.engine.BlockNumberRuleEngine.enforceSequentialBlockNumbers(result.blocks, 101);

        result.rawOcrText = rawTranscript.toString().trim();
        return result;
    }

    private static String optString(JsonObject obj, String key, String fallback) {
        if (obj != null && obj.has(key) && !obj.get(key).isJsonNull()) {
            try {
                return obj.get(key).getAsString().trim();
            } catch (Exception ignored) {}
        }
        return fallback;
    }

    private static double optDouble(JsonObject obj, String key, double fallback) {
        if (obj != null && obj.has(key) && !obj.get(key).isJsonNull()) {
            try {
                return obj.get(key).getAsDouble();
            } catch (Exception ignored) {}
        }
        return fallback;
    }

    private static Bitmap rotateBitmapIfRequired(Context context, Uri uri, Bitmap bitmap) {
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            if (is != null) {
                ExifInterface exif = new ExifInterface(is);
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        return rotateBitmap(bitmap, 90);
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        return rotateBitmap(bitmap, 180);
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        return rotateBitmap(bitmap, 270);
                    default:
                        return bitmap;
                }
            }
        } catch (Exception ignored) {}
        return bitmap;
    }

    private static Bitmap rotateBitmap(Bitmap bitmap, float degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private static Bitmap scaleDownIfNeeded(Bitmap bitmap, int maxDimension) {
        Bitmap enhanced = enhanceSlipContrastAndLighting(bitmap);
        int width = enhanced.getWidth();
        int height = enhanced.getHeight();
        if (width <= maxDimension && height <= maxDimension) {
            return enhanced;
        }
        float ratio = Math.min((float) maxDimension / width, (float) maxDimension / height);
        int newWidth = Math.max(1, Math.round(width * ratio));
        int newHeight = Math.max(1, Math.round(height * ratio));
        return Bitmap.createScaledBitmap(enhanced, newWidth, newHeight, true);
    }

    /**
     * Adaptive contrast and shadow filter to make faded ballpoint pen,
     * dot-matrix printing, and low-contrast measurement slips pop out under harsh sunlight or shadows.
     */
    private static Bitmap enhanceSlipContrastAndLighting(Bitmap bitmap) {
        try {
            Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig() != null ? bitmap.getConfig() : Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(output);
            android.graphics.Paint paint = new android.graphics.Paint();
            android.graphics.ColorMatrix cm = new android.graphics.ColorMatrix();

            float contrast = 1.25f;
            float brightness = 6.0f;
            cm.set(new float[] {
                    contrast, 0, 0, 0, brightness,
                    0, contrast, 0, 0, brightness,
                    0, 0, contrast, 0, brightness,
                    0, 0, 0, 1, 0
            });
            paint.setColorFilter(new android.graphics.ColorMatrixColorFilter(cm));
            canvas.drawBitmap(bitmap, 0, 0, paint);
            return output;
        } catch (Exception e) {
            return bitmap;
        }
    }

    public void close() {
        fallbackOcrEngine.close();
        executor.shutdownNow();
    }
}
