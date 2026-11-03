package com.graniteflow.app.ui.document;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.graniteflow.app.R;
import com.graniteflow.app.pdf.PdfExportHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DocumentPreviewActivity extends AppCompatActivity {

    private String filePath;
    private String docTitle;
    private File pdfFile;

    private ParcelFileDescriptor pfd;
    private PdfRenderer pdfRenderer;
    private final Object renderLock = new Object();
    private ExecutorService renderExecutor;
    private Handler mainHandler;
    private LruCache<Integer, Bitmap> bitmapCache;

    private ViewPager2 viewPagerPdf;
    private TextView tvPageIndicatorPill;
    private FloatingActionButton btnPrevPage;
    private FloatingActionButton btnNextPage;
    private View layoutPlaceholder;
    private int totalPages = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_preview);

        filePath = getIntent().getStringExtra("extra_file_path");
        docTitle = getIntent().getStringExtra("extra_doc_title");
        if (filePath != null) {
            pdfFile = new File(filePath);
        }

        renderExecutor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        // Cache up to ~30MB of page bitmaps
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = Math.max(1024 * 16, maxMemory / 8);
        bitmapCache = new LruCache<Integer, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(Integer key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };

        initViews();
        loadPdfDocument();
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        if (docTitle != null && !docTitle.isEmpty()) {
            toolbar.setTitle(docTitle);
        } else {
            toolbar.setTitle("Inspection Report");
        }

        if (pdfFile != null && pdfFile.exists()) {
            long kb = Math.max(1, pdfFile.length() / 1024);
            toolbar.setSubtitle(pdfFile.getName() + " (" + kb + " KB)");
        }

        viewPagerPdf = findViewById(R.id.viewPagerPdf);
        tvPageIndicatorPill = findViewById(R.id.tvPageIndicatorPill);
        btnPrevPage = findViewById(R.id.btnPrevPage);
        btnNextPage = findViewById(R.id.btnNextPage);
        layoutPlaceholder = findViewById(R.id.layoutPlaceholder);

        // Smooth Page Transformation
        viewPagerPdf.setOffscreenPageLimit(1);
        viewPagerPdf.setPageTransformer((page, position) -> {
            float absPos = Math.abs(position);
            page.setAlpha(1.0f - (absPos * 0.25f));
            float scale = Math.max(0.93f, 1f - absPos * 0.07f);
            page.setScaleX(scale);
            page.setScaleY(scale);
        });

        viewPagerPdf.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updatePageIndicator(position);
            }
        });

        btnPrevPage.setOnClickListener(v -> {
            int cur = viewPagerPdf.getCurrentItem();
            if (cur > 0) {
                viewPagerPdf.setCurrentItem(cur - 1, true);
            }
        });

        btnNextPage.setOnClickListener(v -> {
            int cur = viewPagerPdf.getCurrentItem();
            if (cur < totalPages - 1) {
                viewPagerPdf.setCurrentItem(cur + 1, true);
            }
        });

        MaterialButton btnShare = findViewById(R.id.btnSharePdf);
        btnShare.setOnClickListener(v -> {
            if (pdfFile != null && pdfFile.exists()) {
                PdfExportHelper.sharePdf(this, pdfFile, "Share " + (docTitle != null ? docTitle : "Document"));
            } else {
                Toast.makeText(this, "PDF file not found.", Toast.LENGTH_SHORT).show();
            }
        });

        MaterialButton btnSaveDownloads = findViewById(R.id.btnSaveToDownloads);
        btnSaveDownloads.setOnClickListener(v -> saveToDownloads());
    }

    private void loadPdfDocument() {
        if (pdfFile == null || !pdfFile.exists()) {
            showPlaceholder();
            return;
        }

        try {
            pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
            pdfRenderer = new PdfRenderer(pfd);
            totalPages = pdfRenderer.getPageCount();

            if (totalPages > 0) {
                PdfPagesAdapter adapter = new PdfPagesAdapter();
                viewPagerPdf.setAdapter(adapter);
                updatePageIndicator(0);
            } else {
                showPlaceholder();
            }
        } catch (Exception e) {
            e.printStackTrace();
            showPlaceholder();
        }
    }

    private void showPlaceholder() {
        if (viewPagerPdf != null) viewPagerPdf.setVisibility(View.GONE);
        if (btnPrevPage != null) btnPrevPage.setVisibility(View.GONE);
        if (btnNextPage != null) btnNextPage.setVisibility(View.GONE);
        if (tvPageIndicatorPill != null) tvPageIndicatorPill.setVisibility(View.GONE);
        if (layoutPlaceholder != null) {
            layoutPlaceholder.setVisibility(View.VISIBLE);
            TextView tvName = layoutPlaceholder.findViewById(R.id.tvDocFileName);
            if (tvName != null && pdfFile != null) {
                tvName.setText(pdfFile.getName());
            }
        }
    }

    private void updatePageIndicator(int position) {
        if (totalPages <= 0) return;
        tvPageIndicatorPill.setText(String.format(Locale.ENGLISH, "Page %d of %d", position + 1, totalPages));

        // Manage arrow visibility smoothly
        btnPrevPage.setVisibility(position > 0 ? View.VISIBLE : View.GONE);
        btnNextPage.setVisibility(position < totalPages - 1 ? View.VISIBLE : View.GONE);
    }

    private Bitmap renderPageSync(int pageIndex) {
        synchronized (renderLock) {
            if (pdfRenderer == null || pageIndex < 0 || pageIndex >= totalPages) return null;
            PdfRenderer.Page page = null;
            try {
                page = pdfRenderer.openPage(pageIndex);
                // Crisp rendering at 2x standard resolution for sharp vector text and diagrams
                int renderW = page.getWidth() * 2;
                int renderH = page.getHeight() * 2;
                Bitmap bitmap = Bitmap.createBitmap(renderW, renderH, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                canvas.drawColor(Color.WHITE);
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                return bitmap;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            } finally {
                if (page != null) {
                    try {
                        page.close();
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    private class PdfPagesAdapter extends RecyclerView.Adapter<PdfPagesAdapter.PageViewHolder> {

        @NonNull
        @Override
        public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pdf_page, parent, false);
            return new PageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
            final int pos = position;
            Bitmap cached = bitmapCache.get(pos);
            if (cached != null) {
                holder.ivPageImage.setImageBitmap(cached);
                holder.pageLoading.setVisibility(View.GONE);
                return;
            }

            // Show loading placeholder and render in dedicated background thread
            holder.pageLoading.setVisibility(View.VISIBLE);
            holder.ivPageImage.setImageBitmap(null);

            renderExecutor.execute(() -> {
                if (isFinishing() || isDestroyed()) return;
                Bitmap bmp = renderPageSync(pos);
                if (bmp != null) {
                    bitmapCache.put(pos, bmp);
                    mainHandler.post(() -> {
                        if (!isFinishing() && !isDestroyed()) {
                            if (holder.getBindingAdapterPosition() == pos) {
                                holder.ivPageImage.setImageBitmap(bmp);
                                holder.pageLoading.setVisibility(View.GONE);
                            }
                        }
                    });
                }
            });
        }

        @Override
        public int getItemCount() {
            return totalPages;
        }

        class PageViewHolder extends RecyclerView.ViewHolder {
            final ImageView ivPageImage;
            final ProgressBar pageLoading;

            PageViewHolder(@NonNull View itemView) {
                super(itemView);
                ivPageImage = itemView.findViewById(R.id.ivPageImage);
                pageLoading = itemView.findViewById(R.id.pageLoading);
            }
        }
    }

    private void saveToDownloads() {
        if (pdfFile == null || !pdfFile.exists()) {
            Toast.makeText(this, "PDF file not available.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsDir.exists()) downloadsDir.mkdirs();

            File destFile = new File(downloadsDir, pdfFile.getName());
            FileInputStream fis = new FileInputStream(pdfFile);
            FileOutputStream fos = new FileOutputStream(destFile);

            byte[] buffer = new byte[4096];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
            fis.close();
            fos.close();

            Toast.makeText(this, "Saved to Downloads: " + destFile.getName(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Saved: " + pdfFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        synchronized (renderLock) {
            try {
                if (pdfRenderer != null) {
                    pdfRenderer.close();
                    pdfRenderer = null;
                }
            } catch (Exception ignored) {}
            try {
                if (pfd != null) {
                    pfd.close();
                    pfd = null;
                }
            } catch (Exception ignored) {}
        }
        if (renderExecutor != null) {
            renderExecutor.shutdownNow();
        }
        if (bitmapCache != null) {
            bitmapCache.evictAll();
        }
    }
}
