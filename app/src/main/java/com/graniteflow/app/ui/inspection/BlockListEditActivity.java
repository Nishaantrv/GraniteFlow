package com.graniteflow.app.ui.inspection;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.graniteflow.app.R;
import com.graniteflow.app.data.model.Block;
import com.graniteflow.app.data.model.BlockTypePreset;
import com.graniteflow.app.data.model.Inspection;
import com.graniteflow.app.data.repository.GraniteRepository;
import com.graniteflow.app.ui.document.UnifiedExportDocumentsActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BlockListEditActivity extends AppCompatActivity {

    public static final String EXTRA_INSPECTION_ID = "extra_inspection_id";

    private GraniteRepository repository;
    private Inspection inspection;
    private String searchQuery = "";

    private TextView tvEditListTitle, tvEditStatCount, tvEditStatGross, tvEditStatNet, tvEditStatValue;
    private EditText etSearchBlocks;
    private RecyclerView rvEditBlocksList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_block_list_edit);

        repository = GraniteRepository.getInstance(this);
        String inspId = getIntent().getStringExtra(EXTRA_INSPECTION_ID);
        if (inspId != null) {
            inspection = repository.getInspectionById(inspId);
        }
        if (inspection == null) {
            inspection = repository.getActiveInspection();
        }
        if (inspection == null) {
            finish();
            return;
        }

        initViews();
        refreshUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (inspection != null) {
            Inspection refreshed = repository.getInspectionById(inspection.getInspectionId());
            if (refreshed != null) {
                inspection = refreshed;
                refreshUi();
            }
        }
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btnEditListBack);
        btnBack.setOnClickListener(v -> finish());

        tvEditListTitle = findViewById(R.id.tvEditListTitle);
        tvEditStatCount = findViewById(R.id.tvEditStatCount);
        tvEditStatGross = findViewById(R.id.tvEditStatGross);
        tvEditStatNet = findViewById(R.id.tvEditStatNet);
        tvEditStatValue = findViewById(R.id.tvEditStatValue);

        Button btnAddMore = findViewById(R.id.btnEditListAddMore);
        btnAddMore.setOnClickListener(v -> {
            Intent intent = new Intent(this, InspectionWorkspaceActivity.class);
            intent.putExtra(InspectionWorkspaceActivity.EXTRA_INSPECTION_ID, inspection.getInspectionId());
            startActivity(intent);
        });

        ImageButton btnExport = findViewById(R.id.btnEditListExport);
        btnExport.setOnClickListener(v -> {
            Intent intent = new Intent(this, UnifiedExportDocumentsActivity.class);
            intent.putExtra(UnifiedExportDocumentsActivity.EXTRA_INSPECTION_ID, inspection.getInspectionId());
            startActivity(intent);
        });

        etSearchBlocks = findViewById(R.id.etSearchBlocks);
        etSearchBlocks.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int count, int after) {
                searchQuery = s.toString().trim();
                renderBlockList();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        rvEditBlocksList = findViewById(R.id.rvEditBlocksList);
        rvEditBlocksList.setLayoutManager(new LinearLayoutManager(this));
    }

    private void refreshUi() {
        String title = inspection.getCustomer().isEmpty() ? inspection.getInspectionId() : inspection.getCustomer();
        tvEditListTitle.setText(title.toUpperCase());

        tvEditStatCount.setText(String.valueOf(inspection.getTotalBlocksCount()));
        tvEditStatGross.setText(String.format(Locale.ENGLISH, "%.2f", inspection.calculateTotalGrossCbm()));
        tvEditStatNet.setText(String.format(Locale.ENGLISH, "%.2f", inspection.calculateTotalNetCbm()));
        tvEditStatValue.setText(String.format(Locale.ENGLISH, "%s %,.0f", inspection.getCurrency(), inspection.calculateTotalValue()));

        renderBlockList();
    }

    private void renderBlockList() {
        List<Block> filtered = new ArrayList<>();
        for (Block b : inspection.getBlocks()) {
            if (searchQuery.isEmpty() ||
                    (b.getBlockNumber() != null && b.getBlockNumber().toLowerCase().contains(searchQuery.toLowerCase())) ||
                    (b.getRemarks() != null && b.getRemarks().toLowerCase().contains(searchQuery.toLowerCase()))) {
                filtered.add(b);
            }
        }

        rvEditBlocksList.setAdapter(new EditBlockAdapter(filtered, new EditBlockAdapter.OnBlockActionListener() {
            @Override
            public void onEditBlock(Block block) {
                showEditBlockDialog(block);
            }

            @Override
            public void onDeleteBlock(Block block) {
                new AlertDialog.Builder(BlockListEditActivity.this)
                        .setTitle("Delete Block #" + block.getBlockNumber() + "?")
                        .setMessage("This action cannot be undone. This block will be permanently removed from the inspection.")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Delete", (d, w) -> {
                            inspection.getBlocks().remove(block);
                            repository.saveInspection(inspection);
                            refreshUi();
                            Toast.makeText(BlockListEditActivity.this, "Block #" + block.getBlockNumber() + " deleted", Toast.LENGTH_SHORT).show();
                        })
                        .show();
            }
        }));
    }

    private void showEditBlockDialog(Block block) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_edit_single_block);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.94);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvTitle = dialog.findViewById(R.id.tvDialogEditBlockTitle);
        EditText etL1 = dialog.findViewById(R.id.etEditBlockL1);
        EditText etL2 = dialog.findViewById(R.id.etEditBlockL2);
        EditText etL3 = dialog.findViewById(R.id.etEditBlockL3);
        EditText etAllw = dialog.findViewById(R.id.etEditBlockAllowance);
        EditText etType = dialog.findViewById(R.id.etEditBlockType);
        EditText etRemarks = dialog.findViewById(R.id.etEditBlockRemarks);
        LinearLayout llE2E = dialog.findViewById(R.id.llDialogEndToEndSection);
        EditText etE2EL1 = dialog.findViewById(R.id.etEditBlockE2EL1);
        EditText etE2EL2 = dialog.findViewById(R.id.etEditBlockE2EL2);
        EditText etE2EL3 = dialog.findViewById(R.id.etEditBlockE2EL3);
        EditText etDefects = dialog.findViewById(R.id.etEditBlockDefects);

        tvTitle.setText("EDIT BLOCK #" + block.getBlockNumber());
        etL1.setText(String.valueOf((int) block.getGrossLengthCm()));
        etL2.setText(String.valueOf((int) block.getGrossHeightCm()));
        etL3.setText(String.valueOf((int) block.getGrossWidthCm()));
        etAllw.setText(String.valueOf((int) block.getAllowanceCm()));
        etType.setText(block.getType());
        etRemarks.setText(block.getRemarks());

        if (inspection.isEnableEndToEnd()) {
            llE2E.setVisibility(View.VISIBLE);
            if (block.getL1EndToEnd() > 0) etE2EL1.setText(String.valueOf((int) block.getL1EndToEnd()));
            if (block.getL2EndToEnd() > 0) etE2EL2.setText(String.valueOf((int) block.getL2EndToEnd()));
            if (block.getL3EndToEnd() > 0) etE2EL3.setText(String.valueOf((int) block.getL3EndToEnd()));
            if (block.getDefectDescription() != null) etDefects.setText(block.getDefectDescription());
        } else {
            llE2E.setVisibility(View.GONE);
        }

        Button btnCancel = dialog.findViewById(R.id.btnCancelEditBlock);
        Button btnSave = dialog.findViewById(R.id.btnConfirmEditBlock);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            try {
                double l1 = Double.parseDouble(etL1.getText().toString().trim());
                double l2 = Double.parseDouble(etL2.getText().toString().trim());
                double l3 = Double.parseDouble(etL3.getText().toString().trim());
                double allw = Double.parseDouble(etAllw.getText().toString().trim());
                String typeId = etType.getText().toString().trim().isEmpty() ? "1" : etType.getText().toString().trim();

                block.setGrossLengthCm(l1);
                block.setGrossHeightCm(l2);
                block.setGrossWidthCm(l3);
                block.setAllowanceCm(allw);
                block.setType(typeId);
                block.setRemarks(etRemarks.getText().toString().trim());
                block.setCalculationMode(inspection.getCalculationMode());

                if (inspection.isEnableEndToEnd()) {
                    try { block.setL1EndToEnd(Double.parseDouble(etE2EL1.getText().toString().trim())); } catch (Exception ignored) {}
                    try { block.setL2EndToEnd(Double.parseDouble(etE2EL2.getText().toString().trim())); } catch (Exception ignored) {}
                    try { block.setL3EndToEnd(Double.parseDouble(etE2EL3.getText().toString().trim())); } catch (Exception ignored) {}
                    block.setDefectDescription(etDefects.getText().toString().trim());
                }

                block.recalculate();
                repository.saveInspection(inspection);
                dialog.dismiss();
                refreshUi();
                Toast.makeText(this, "Block #" + block.getBlockNumber() + " updated!", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Please enter valid numeric dimensions.", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private class EditBlockAdapter extends RecyclerView.Adapter<EditBlockAdapter.ViewHolder> {
        private final List<Block> list;
        private final OnBlockActionListener listener;

        interface OnBlockActionListener {
            void onEditBlock(Block block);
            void onDeleteBlock(Block block);
        }

        EditBlockAdapter(List<Block> list, OnBlockActionListener listener) {
            this.list = list;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_edit_block, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Block b = list.get(position);
            BlockTypePreset preset = inspection.getPresetById(b.getType());
            String typeLabel = preset != null ? preset.getName() : ("T" + b.getType());

            holder.tvTypeBadge.setText(typeLabel);
            holder.tvNumberBadge.setText("#" + b.getBlockNumber());
            holder.tvTitle.setText("Block #" + b.getBlockNumber());
            holder.tvDims.setText((int) b.getGrossLengthCm() + " × " + (int) b.getGrossHeightCm() + " × " + (int) b.getGrossWidthCm() + " cm");

            if (b.getRemarks() != null && !b.getRemarks().isEmpty()) {
                holder.tvRemarks.setVisibility(View.VISIBLE);
                holder.tvRemarks.setText(b.getRemarks());
            } else {
                holder.tvRemarks.setVisibility(View.GONE);
            }

            holder.tvNetCbm.setText(String.format(Locale.ENGLISH, "%.3f", b.getNetCbm()));
            holder.tvAllowanceInfo.setText(typeLabel + ": " + (int) b.getAllowanceCm() + "cm");

            holder.itemView.setOnClickListener(v -> listener.onEditBlock(b));
            holder.btnDelete.setOnClickListener(v -> listener.onDeleteBlock(b));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTypeBadge, tvNumberBadge, tvTitle, tvDims, tvRemarks, tvNetCbm, tvAllowanceInfo;
            ImageButton btnDelete;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTypeBadge = itemView.findViewById(R.id.tvItemBlockTypeBadge);
                tvNumberBadge = itemView.findViewById(R.id.tvItemBlockNumberBadge);
                tvTitle = itemView.findViewById(R.id.tvItemBlockTitle);
                tvDims = itemView.findViewById(R.id.tvItemBlockDims);
                tvRemarks = itemView.findViewById(R.id.tvItemBlockRemarks);
                tvNetCbm = itemView.findViewById(R.id.tvItemBlockNetCbm);
                tvAllowanceInfo = itemView.findViewById(R.id.tvItemBlockAllowanceInfo);
                btnDelete = itemView.findViewById(R.id.btnItemDeleteBlock);
            }
        }
    }
}
