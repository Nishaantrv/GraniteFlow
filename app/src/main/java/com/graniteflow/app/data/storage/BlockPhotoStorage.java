package com.graniteflow.app.data.storage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class BlockPhotoStorage {

    private static final String TAG = "BlockPhotoStorage";

    public static File getPhotosDir(@NonNull Context context) {
        File dir = new File(context.getFilesDir(), "block_photos");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    @Nullable
    public static String saveBitmap(@NonNull Context context, @NonNull Bitmap bitmap, @NonNull String blockNumber, @NonNull String side) {
        try {
            File dir = getPhotosDir(context);
            String safeBlock = blockNumber.replaceAll("[^a-zA-Z0-9_-]", "_");
            String safeSide = side.replaceAll("[^a-zA-Z0-9_-]", "_");
            File file = new File(dir, "BLK_" + safeBlock + "_" + safeSide + "_" + System.currentTimeMillis() + ".jpg");

            try (FileOutputStream fos = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos);
                fos.flush();
            }
            Log.d(TAG, "Saved block photo to: " + file.getAbsolutePath() + " (" + file.length() + " bytes)");
            return file.getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, "Failed to save bitmap photo: " + e.getMessage(), e);
            return null;
        }
    }

    @Nullable
    public static String saveUri(@NonNull Context context, @NonNull Uri imageUri, @NonNull String blockNumber, @NonNull String side) {
        try {
            File dir = getPhotosDir(context);
            String safeBlock = blockNumber.replaceAll("[^a-zA-Z0-9_-]", "_");
            String safeSide = side.replaceAll("[^a-zA-Z0-9_-]", "_");
            File file = new File(dir, "BLK_" + safeBlock + "_" + safeSide + "_" + System.currentTimeMillis() + ".jpg");

            Bitmap bitmap;
            try (InputStream is = context.getContentResolver().openInputStream(imageUri)) {
                bitmap = BitmapFactory.decodeStream(is);
            }
            if (bitmap == null) {
                return null;
            }

            // Correct EXIF orientation
            try (InputStream is = context.getContentResolver().openInputStream(imageUri)) {
                if (is != null) {
                    ExifInterface exif = new ExifInterface(is);
                    int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                    if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                        bitmap = rotate(bitmap, 90);
                    } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                        bitmap = rotate(bitmap, 180);
                    } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                        bitmap = rotate(bitmap, 270);
                    }
                }
            } catch (Exception ignored) {}

            try (FileOutputStream fos = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos);
                fos.flush();
            }
            Log.d(TAG, "Saved uri photo to: " + file.getAbsolutePath() + " (" + file.length() + " bytes)");
            return file.getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, "Failed to save uri photo: " + e.getMessage(), e);
            return null;
        }
    }

    private static Bitmap rotate(Bitmap bitmap, float degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }
}
