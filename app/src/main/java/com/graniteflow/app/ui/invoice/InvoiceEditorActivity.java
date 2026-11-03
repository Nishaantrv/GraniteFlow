package com.graniteflow.app.ui.invoice;

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
import com.graniteflow.app.data.model.InvoiceDocument;
import com.graniteflow.app.data.model.InvoiceLineItem;
import com.graniteflow.app.data.repository.GraniteRepository;
import com.graniteflow.app.engine.InvoiceCalculator;
import com.graniteflow.app.pdf.InvoicePdfRenderer;
import com.graniteflow.app.ui.document.DocumentPreviewActivity;

import java.io.File;
import java.util.List;

public class InvoiceEditorActivity extends AppCompatActivity {

    private InvoiceDocument currentInvoice;
    private GraniteRepository repository;

    private EditText etInvoiceType, etInvoiceDate, etInvoiceNo, etPortLoading, etPortDischarge, etTerms;
    private EditText etNewLineMarks, etNewLineQty, etNewLineRate;
    private TextView tvTotalPackages, tvTotalCbm, tvTotalAmount, tvAmountWords;
    private RecyclerView rvLineItems;
    private LineItemAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invoice_editor);

        repository = GraniteRepository.getInstance(this);
        List<InvoiceDocument> invoices = repository.getInvoices();
        currentInvoice = invoices.isEmpty() ? null : invoices.get(0);

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

        etInvoiceType = findViewById(R.id.etInvoiceType);
        etInvoiceDate = findViewById(R.id.etInvoiceDate);
        etInvoiceNo = findViewById(R.id.etInvoiceNo);
        etPortLoading = findViewById(R.id.etPortLoading);
        etPortDischarge = findViewById(R.id.etPortDischarge);
        etTerms = findViewById(R.id.etTerms);

        etNewLineMarks = findViewById(R.id.etNewLineMarks);
        etNewLineQty = findViewById(R.id.etNewLineQty);
        etNewLineRate = findViewById(R.id.etNewLineRate);

        tvTotalPackages = findViewById(R.id.tvTotalPackages);
        tvTotalCbm = findViewById(R.id.tvTotalCbm);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        tvAmountWords = findViewById(R.id.tvAmountWords);

        rvLineItems = findViewById(R.id.rvLineItems);
        rvLineItems.setLayoutManager(new LinearLayoutManager(this));

        Button btnAddLine = findViewById(R.id.btnAddLineItem);
        btnAddLine.setOnClickListener(v -> addNewLineItem());

        Button btnPreview = findViewById(R.id.btnPreviewInvoicePdf);
        btnPreview.setOnClickListener(v -> generateAndPreviewPdf());

        Button btnSave = findViewById(R.id.btnSaveInvoiceDraft);
        btnSave.setOnClickListener(v -> saveDraft(true));
    }

    private void populateData() {
        if (currentInvoice == null) return;

        etInvoiceType.setText(currentInvoice.getInvoiceType());
        etInvoiceDate.setText(currentInvoice.getDate());
        etInvoiceNo.setText(currentInvoice.getInvoiceNumber());
        etPortLoading.setText(currentInvoice.getPortOfLoading());
        etPortDischarge.setText(currentInvoice.getPortOfDischarge());
        etTerms.setText(currentInvoice.getTermsOfDeliveryPayment());

        recalculateAndRefreshUi();
    }

    private void recalculateAndRefreshUi() {
        if (currentInvoice == null) return;
        InvoiceCalculator.computeAndApplyInvoiceTotals(currentInvoice);

        tvTotalPackages.setText("TOTAL BLOCKS: " + currentInvoice.getTotalPackagesCount() + " ONLY");
        tvTotalCbm.setText("Total CBM: " + String.format("%.3f", currentInvoice.getTotalCbm()));
        tvTotalAmount.setText("$" + String.format("%,.2f", currentInvoice.getTotalAmount()));
        tvAmountWords.setText(currentInvoice.getAmountInWords());

        adapter = new LineItemAdapter(currentInvoice.getLineItems(), item -> {
            currentInvoice.getLineItems().remove(item);
            recalculateAndRefreshUi();
        });
        rvLineItems.setAdapter(adapter);
    }

    private void addNewLineItem() {
        String marks = etNewLineMarks.getText().toString().trim();
        String qtyStr = etNewLineQty.getText().toString().trim();
        String rateStr = etNewLineRate.getText().toString().trim();

        if (marks.isEmpty() || qtyStr.isEmpty() || rateStr.isEmpty()) {
            Toast.makeText(this, "Please fill marks, quantity, and rate.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double qty = Double.parseDouble(qtyStr);
            double rate = Double.parseDouble(rateStr);

            InvoiceLineItem newItem = new InvoiceLineItem(marks, qty, rate);
            currentInvoice.getLineItems().add(newItem);
            recalculateAndRefreshUi();

            etNewLineMarks.setText("");
            etNewLineQty.setText("");
            etNewLineRate.setText("");
            Toast.makeText(this, "Line item added.", Toast.LENGTH_SHORT).show();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numeric quantity and rate.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveDraft(boolean showToast) {
        if (currentInvoice == null) return;
        currentInvoice.setInvoiceType(etInvoiceType.getText().toString().trim());
        currentInvoice.setDate(etInvoiceDate.getText().toString().trim());
        currentInvoice.setInvoiceNumber(etInvoiceNo.getText().toString().trim());
        currentInvoice.setPortOfLoading(etPortLoading.getText().toString().trim());
        currentInvoice.setPortOfDischarge(etPortDischarge.getText().toString().trim());
        currentInvoice.setTermsOfDeliveryPayment(etTerms.getText().toString().trim());

        InvoiceCalculator.computeAndApplyInvoiceTotals(currentInvoice);
        repository.saveInvoice(currentInvoice);

        if (showToast) {
            Toast.makeText(this, "Invoice draft saved successfully!", Toast.LENGTH_SHORT).show();
        }
    }

    private void generateAndPreviewPdf() {
        saveDraft(false);
        try {
            String fileName = "PROFORMA_INVOICE_" + currentInvoice.getDate() + ".pdf";
            File pdfFile = InvoicePdfRenderer.renderInvoicePdf(this, currentInvoice, fileName);

            Intent intent = new Intent(this, DocumentPreviewActivity.class);
            intent.putExtra("extra_file_path", pdfFile.getAbsolutePath());
            intent.putExtra("extra_doc_title", currentInvoice.getInvoiceType() + " (PDF)");
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Error generating invoice PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Inner Adapter
    private static class LineItemAdapter extends RecyclerView.Adapter<LineItemAdapter.ViewHolder> {
        private final List<InvoiceLineItem> list;
        private final OnDeleteListener listener;

        interface OnDeleteListener {
            void onDelete(InvoiceLineItem item);
        }

        LineItemAdapter(List<InvoiceLineItem> list, OnDeleteListener listener) {
            this.list = list;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_invoice_line, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            InvoiceLineItem item = list.get(position);
            holder.tvLineMarks.setText(item.getMarksAndNos());
            holder.tvLineQtyRate.setText("Qty: " + String.format("%.3f", item.getQuantityCbm()) + " CBM  |  Rate: $" + String.format("%.0f", item.getRatePerCbm()));
            holder.tvLineAmount.setText("$" + String.format("%,.2f", item.getAmount()));
            holder.btnDeleteLine.setOnClickListener(v -> listener.onDelete(item));
        }

        @Override
        public int getItemCount() {
            return list != null ? list.size() : 0;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvLineMarks, tvLineQtyRate, tvLineAmount;
            ImageButton btnDeleteLine;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvLineMarks = itemView.findViewById(R.id.tvLineMarks);
                tvLineQtyRate = itemView.findViewById(R.id.tvLineQtyRate);
                tvLineAmount = itemView.findViewById(R.id.tvLineAmount);
                btnDeleteLine = itemView.findViewById(R.id.btnDeleteLine);
            }
        }
    }
}
