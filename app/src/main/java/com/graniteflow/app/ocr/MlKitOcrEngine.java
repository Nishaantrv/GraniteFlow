package com.graniteflow.app.ocr;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.IOException;

/**
 * Real Google ML Kit Text Recognition v2 Engine.
 * Operates on-device with zero server latency or credential exposure.
 */
public class MlKitOcrEngine {

    private static final String TAG = "GraniteFlowOCR";

    public interface OcrCallback {
        void onSuccess(String rawText, Text textResult);
        void onFailure(Exception exception);
    }

    private final TextRecognizer recognizer;

    public MlKitOcrEngine() {
        // Initialize Latin script text recognizer (bundled v2 model)
        Log.d(TAG, "[MlKitOcrEngine] Initializing Google ML Kit Text Recognition v2 (Latin model bundled)");
        this.recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    }

    /**
     * Process an Android Uri pointing to an image (from gallery, camera capture, or file provider).
     */
    public void processImage(@NonNull Context context, @NonNull Uri imageUri, @NonNull OcrCallback callback) {
        try {
            Log.d(TAG, "[MlKitOcrEngine] Creating InputImage from URI: " + imageUri);
            InputImage image = InputImage.fromFilePath(context, imageUri);
            Log.d(TAG, "[MlKitOcrEngine] Executing recognizer.process(InputImage)...");
            recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        String rawText = visionText.getText();
                        Log.d(TAG, "[RAW ML KIT TEXT] Success callback received from ML Kit (" +
                                (rawText != null ? rawText.length() : 0) + " chars):\n" + rawText);
                        callback.onSuccess(rawText, visionText);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "[MlKitOcrEngine] ML Kit recognition failed: " + e.getMessage(), e);
                        callback.onFailure(e);
                    });
        } catch (IOException e) {
            Log.e(TAG, "[MlKitOcrEngine] Failed to create InputImage from URI: " + e.getMessage(), e);
            callback.onFailure(e);
        }
    }

    /**
     * Process an in-memory Bitmap.
     */
    public void processBitmap(@NonNull Bitmap bitmap, @NonNull OcrCallback callback) {
        Log.d(TAG, "[MlKitOcrEngine] Creating InputImage from Bitmap (" + bitmap.getWidth() + "x" + bitmap.getHeight() + ")");
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        Log.d(TAG, "[MlKitOcrEngine] Executing recognizer.process(InputImage)...");
        recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    String rawText = visionText.getText();
                    Log.d(TAG, "[RAW ML KIT TEXT] Success callback received from ML Kit (" +
                            (rawText != null ? rawText.length() : 0) + " chars):\n" + rawText);
                    callback.onSuccess(rawText, visionText);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "[MlKitOcrEngine] ML Kit recognition failed: " + e.getMessage(), e);
                    callback.onFailure(e);
                });
    }

    /**
     * Closes recognizer when no longer needed.
     */
    public void close() {
        Log.d(TAG, "[MlKitOcrEngine] Closing recognizer");
        recognizer.close();
    }
}
