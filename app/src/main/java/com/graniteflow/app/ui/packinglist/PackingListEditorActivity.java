package com.graniteflow.app.ui.packinglist;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.graniteflow.app.R;
import com.graniteflow.app.data.model.PackingListDocument;
import com.graniteflow.app.data.model.PackingListRow;
import com.graniteflow.app.data.repository.GraniteRepository;
import com.graniteflow.app.engine.PackingListCalculator;
import com.graniteflow.app.pdf.PackingListPdfRenderer;
import com.graniteflow.app.ui.document.DocumentPreviewActivity;

import java.io.File;
import java.util.List;

public class PackingListEditorActivity extends AppCompatActivity {

    private PackingListDocument currentPackingList;
    private GraniteRepository repository;

    private EditText etPlInvoiceNo, etPlDate, etPlPortLoading, etPlPortDischarge;
    private EditText etNewPlBlockNo, etNewPlL, etNewPlH, etNewPlW;
    private TextView tvPlTotalBlocks, tvPlTotalCbm;
    private RecyclerView rvPackingRows;
    private PackingRowAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_packing_list_editor);

        repository = GraniteRepository.getInstance(this);
        List<PackingListDocument> lists = repository.getPackingLists();
        currentPackingList = lists.isEmpty() ? null : lists.get(0);

        initViews();
        populateData();
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        etPlInvoiceNo = findViewById(R.id.etPlInvoiceNo);
        etPlDate = findViewById(R.id.etPlDate);
        etPlPortLoading = findViewById(R.id.etPlPortLoading);
        etPlPortDischarge = findViewById(R.id.etPlPortDischarge);

        etNewPlBlockNo = findViewById(R.id.etNewPlBlockNo);
        etNewPlL = findViewById(R.id.etNewPlL);
        etNewPlH = findViewById(R.id.etNewPlH);
        etNewPlW = findViewById(R.id.etNewPlW);

        tvPlTotalBlocks = findViewById(R.id.tvPlTotalBlocks);
        tvPlTotalCbm = findViewById(R.id.tvPlTotalCbm);

        rvPackingRows = findViewById(R.id.rvPackingRows);
        rvPackingRows.setLayoutManager(new LinearLayoutManager(this));

        Button btnAddRow = findViewById(R.id.btnAddPlRow);
        btnAddRow.setOnClickListener(v -> addNewBlockRow());

        Button btnPreview = findViewById(R.id.btnPreviewPlPdf);
        btnPreview.setOnClickListener(v -> generateAndPreviewPdf());

        Button btnSave = findViewById(R.id.btnSavePlDraft);
        btnSave.setOnClickListener(v -> saveDraft(true));
    }

    private void populateData() {
        if (currentPackingList == null) return;

        etPlInvoiceNo.setText(currentPackingList.getInvoiceNumber());
        etPlDate.setText(currentPackingList.getDate());
        etPlPortLoading.setText(currentPackingList.getPortOfLoading());
        etPlPortDischarge.setText(currentPackingList.getPortOfDischarge());

        recalculateAndRefreshUi();
    }

    private void recalculateAndRefreshUi() {
        if (currentPackingList == null) return;
        PackingListCalculator.computeAndApplyPackingListTotals(currentPackingList);

        tvPlTotalBlocks.setText("TOTAL NO. OF BLOCKS: " + currentPackingList.getTotalBlocksCount() + " ONLY");
        tvPlTotalCbm.setText("Total CBM: " + String.format("%.3f", currentPackingList.getTotalCbm()));

        adapter = new PackingRowAdapter(currentPackingList.getRows(), row -> {
            currentPackingList.getRows().remove(row);
            recalculateAndRefreshUi();
        });
        rvPackingRows.setAdapter(adapter);
    }

    private void addNewBlockRow() {
        String num = etNewPlBlockNo.getText().toString().trim();
        String lStr = etNewPlL.getText().toString().trim();
        String hStr = etNewPlH.getText().toString().trim();
        String wStr = etNewPlW.getText().toString().trim();

        if (num.isEmpty() || lStr.isEmpty() || hStr.isEmpty() || wStr.isEmpty()) {
            Toast.makeText(this, "Please enter block number and dimensions.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double l = Double.parseDouble(lStr);
            double h = Double.parseDouble(hStr);
            double w = Double.parseDouble(wStr);

            PackingListRow newRow = new PackingListRow(num, l, h, w);
            currentPackingList.getRows().add(newRow);
            recalculateAndRefreshUi();

            etNewPlBlockNo.setText("");
            etNewPlL.setText("");
            etNewPlH.setText("");
            etNewPlW.setText("");
            Toast.makeText(this, "Block " + num + " added to packing list.", Toast.LENGTH_SHORT).show();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numeric dimensions.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveDraft(boolean showToast) {
        if (currentPackingList == null) return;
        currentPackingList.setInvoiceNumber(etPlInvoiceNo.getText().toString().trim());
        currentPackingList.setDate(etPlDate.getText().toString().trim());
        currentPackingList.setPortOfLoading(etPlPortLoading.getText().toString().trim());
        currentPackingList.setPortOfDischarge(etPlPortDischarge.getText().toString().trim());

        PackingListCalculator.computeAndApplyPackingListTotals(currentPackingList);
        repository.savePackingList(currentPackingList);

        if (showToast) {
            Toast.makeText(this, "Packing list draft saved successfully!", Toast.LENGTH_SHORT).show();
        }
    }

    private void generateAndPreviewPdf() {
        saveDraft(false);
        try {
            String fileName = "PACKING_LIST_" + currentPackingList.getDate() + ".pdf";
            File pdfFile = PackingListPdfRenderer.renderPackingListPdf(this, currentPackingList, fileName);

            Intent intent = new Intent(this, DocumentPreviewActivity.class);
            intent.putExtra("extra_file_path", pdfFile.getAbsolutePath());
            intent.putExtra("extra_doc_title", "PACKING LIST (PDF)");
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Error generating packing list PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Inner Adapter
    private static class PackingRowAdapter extends RecyclerView.Adapter<PackingRowAdapter.ViewHolder> {
        private final List<PackingListRow> list;
        private final OnDeleteListener listener;

        interface OnDeleteListener {
            void onDelete(PackingListRow row);
        }

        PackingRowAdapter(List<PackingListRow> list, OnDeleteListener listener) {
            this.list = list;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_packing_row, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            PackingListRow row = list.get(position);
            holder.tvRowDescription.setText("Block no. " + row.getBlockNumber() + "   -   " + row.getFormattedDimensions());
            holder.tvRowCbm.setText(String.format("%.3f", row.getQuantityCbm()) + " CBM");
            holder.btnDeleteRow.setOnClickListener(v -> listener.onDelete(row));
        }

        @Override
        public int getItemCount() {
            return list != null ? list.size() : 0;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvRowDescription, tvRowCbm;
            ImageButton btnDeleteRow;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvRowDescription = itemView.findViewById(R.id.tvRowDescription);
                tvRowCbm = itemView.findViewById(R.id.tvRowCbm);
                btnDeleteRow = itemView.findViewById(R.id.btnDeleteRow);
            }
        }
    }
}
