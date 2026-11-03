package com.graniteflow.app.pdf;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.core.content.FileProvider;

import java.io.File;

public class PdfExportHelper {

    public static void sharePdf(Context context, File pdfFile, String shareTitle) {
        if (pdfFile == null || !pdfFile.exists()) return;

        Uri contentUri = FileProvider.getUriForFile(
                context,
                "com.graniteflow.app.fileprovider",
                pdfFile
        );

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, pdfFile.getName());
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Intent chooser = Intent.createChooser(shareIntent, shareTitle);
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(chooser);
    }
}
