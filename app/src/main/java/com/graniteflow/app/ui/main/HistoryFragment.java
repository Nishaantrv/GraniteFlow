package com.graniteflow.app.ui.main;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.graniteflow.app.R;
import com.graniteflow.app.data.model.Inspection;
import com.graniteflow.app.data.repository.GraniteRepository;
import com.graniteflow.app.ui.document.UnifiedExportDocumentsActivity;
import com.graniteflow.app.ui.inspection.BlockListEditActivity;
import com.graniteflow.app.ui.inspection.InspectionWorkspaceActivity;

import java.util.List;
import java.util.Locale;

public class HistoryFragment extends Fragment {

    private RecyclerView rvHistory;
    private View layoutHistoryEmpty;
    private TextView btnToggleHistoryEdit, tvHistoryCountSubtitle;
    private GraniteRepository repository;
    private boolean isEditing = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        repository = GraniteRepository.getInstance(requireContext());

        rvHistory = view.findViewById(R.id.rvHistory);
        layoutHistoryEmpty = view.findViewById(R.id.layoutHistoryEmpty);
        btnToggleHistoryEdit = view.findViewById(R.id.btnToggleHistoryEdit);
        tvHistoryCountSubtitle = view.findViewById(R.id.tvHistoryCountSubtitle);

        rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));

        btnToggleHistoryEdit.setOnClickListener(v -> {
            isEditing = !isEditing;
            updateEditButtonState();
            loadHistory();
        });

        loadHistory();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadHistory();
    }

    private void updateEditButtonState() {
        if (isEditing) {
            btnToggleHistoryEdit.setText("✓ Done");
            btnToggleHistoryEdit.setBackgroundResource(R.drawable.shape_tab_pill_active);
            btnToggleHistoryEdit.setTextColor(requireContext().getColor(R.color.text_on_pill));
        } else {
            btnToggleHistoryEdit.setText("✎ Edit");
            btnToggleHistoryEdit.setBackgroundResource(R.drawable.shape_card_modern);
            btnToggleHistoryEdit.setTextColor(requireContext().getColor(R.color.text_primary));
        }
    }

    private void loadHistory() {
        List<Inspection> inspections = repository.getInspections();
        int count = inspections != null ? inspections.size() : 0;
        tvHistoryCountSubtitle.setText(count + (count == 1 ? " saved marking session" : " saved marking sessions"));

        if (count == 0) {
            layoutHistoryEmpty.setVisibility(View.VISIBLE);
            rvHistory.setVisibility(View.GONE);
            btnToggleHistoryEdit.setVisibility(View.GONE);
            return;
        }

        layoutHistoryEmpty.setVisibility(View.GONE);
        rvHistory.setVisibility(View.VISIBLE);
        btnToggleHistoryEdit.setVisibility(View.VISIBLE);

        rvHistory.setAdapter(new HistoryAdapter(inspections, isEditing, new HistoryAdapter.OnSessionActionListener() {
            @Override
            public void onOpenSession(Inspection insp) {
                Intent intent = new Intent(requireContext(), BlockListEditActivity.class);
                intent.putExtra(BlockListEditActivity.EXTRA_INSPECTION_ID, insp.getInspectionId());
                startActivity(intent);
            }

            @Override
            public void onExportSession(Inspection insp) {
                Intent intent = new Intent(requireContext(), UnifiedExportDocumentsActivity.class);
                intent.putExtra(UnifiedExportDocumentsActivity.EXTRA_INSPECTION_ID, insp.getInspectionId());
                startActivity(intent);
            }

            @Override
            public void onDuplicateSession(Inspection insp) {
                Inspection dup = repository.duplicateInspection(insp.getInspectionId());
                if (dup != null) {
                    Toast.makeText(requireContext(), "Duplicated session header for " + dup.getCustomer(), Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(requireContext(), InspectionWorkspaceActivity.class);
                    intent.putExtra(InspectionWorkspaceActivity.EXTRA_INSPECTION_ID, dup.getInspectionId());
                    startActivity(intent);
                }
            }

            @Override
            public void onDeleteSession(Inspection insp) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Delete Inspection Session")
                        .setMessage("Are you sure you want to delete this inspection record (" + insp.getCustomer() + ")? This action cannot be undone.")
                        .setPositiveButton("Delete", (d, w) -> {
                            repository.deleteInspection(insp.getInspectionId());
                            loadHistory();
                            Toast.makeText(requireContext(), "Deleted inspection session", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        }));
    }

    private static class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private final List<Inspection> list;
        private final boolean isEditing;
        private final OnSessionActionListener listener;

        interface OnSessionActionListener {
            void onOpenSession(Inspection insp);
            void onExportSession(Inspection insp);
            void onDuplicateSession(Inspection insp);
            void onDeleteSession(Inspection insp);
        }

        HistoryAdapter(List<Inspection> list, boolean isEditing, OnSessionActionListener listener) {
            this.list = list;
            this.isEditing = isEditing;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Inspection insp = list.get(position);
            holder.tvHistoryBlocksBadge.setText(insp.getTotalBlocksCount() + " BLOCKS");
            String mode = "gross".equalsIgnoreCase(insp.getCalculationMode()) ? "GROSS MODE" : "NET MODE";
            if (insp.isEnableEndToEnd()) mode += " • E2E";
            holder.tvHistoryCalcModeBadge.setText(mode);

            String title = (insp.getConsigneeShort() != null && !insp.getConsigneeShort().isEmpty())
                    ? insp.getConsigneeShort()
                    : (insp.getCustomer() != null && !insp.getCustomer().isEmpty() ? insp.getCustomer() : "Unnamed");
            holder.tvHistoryCustomerName.setText(title);

            String stone = (insp.getStoneDescription() != null && !insp.getStoneDescription().isEmpty())
                    ? insp.getStoneDescription() : insp.getMaterialName();
            holder.tvHistoryStoneDesc.setText(stone + " • " + (insp.getPortOfDischarge() != null ? insp.getPortOfDischarge() : ""));

            holder.tvHistoryDate.setText("📅 " + insp.getDate());
            holder.tvHistoryTotalNetCbm.setText(String.format(Locale.ENGLISH, "%.3f", insp.calculateTotalNetCbm()));

            if (isEditing) {
                holder.btnHistoryDelete.setVisibility(View.VISIBLE);
                holder.btnHistoryDuplicate.setVisibility(View.GONE);
                holder.btnHistoryExport.setVisibility(View.GONE);
                holder.tvHistoryChevron.setVisibility(View.GONE);
            } else {
                holder.btnHistoryDelete.setVisibility(View.GONE);
                holder.btnHistoryDuplicate.setVisibility(View.VISIBLE);
                holder.btnHistoryExport.setVisibility(View.VISIBLE);
                holder.tvHistoryChevron.setVisibility(View.VISIBLE);
            }

            holder.itemView.setOnClickListener(v -> {
                if (!isEditing) {
                    listener.onOpenSession(insp);
                }
            });
            holder.btnHistoryDuplicate.setOnClickListener(v -> listener.onDuplicateSession(insp));
            holder.btnHistoryExport.setOnClickListener(v -> listener.onExportSession(insp));
            holder.btnHistoryDelete.setOnClickListener(v -> listener.onDeleteSession(insp));
        }

        @Override
        public int getItemCount() {
            return list != null ? list.size() : 0;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvHistoryBlocksBadge, tvHistoryCalcModeBadge, tvHistoryCustomerName, tvHistoryStoneDesc;
            TextView tvHistoryDate, tvHistoryTotalNetCbm;
            TextView btnHistoryDuplicate, btnHistoryExport, btnHistoryDelete, tvHistoryChevron;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvHistoryBlocksBadge = itemView.findViewById(R.id.tvHistoryBlocksBadge);
                tvHistoryCalcModeBadge = itemView.findViewById(R.id.tvHistoryCalcModeBadge);
                tvHistoryCustomerName = itemView.findViewById(R.id.tvHistoryCustomerName);
                tvHistoryStoneDesc = itemView.findViewById(R.id.tvHistoryStoneDesc);
                tvHistoryDate = itemView.findViewById(R.id.tvHistoryDate);
                tvHistoryTotalNetCbm = itemView.findViewById(R.id.tvHistoryTotalNetCbm);
                btnHistoryDuplicate = itemView.findViewById(R.id.btnHistoryDuplicate);
                btnHistoryExport = itemView.findViewById(R.id.btnHistoryExport);
                btnHistoryDelete = itemView.findViewById(R.id.btnHistoryDelete);
                tvHistoryChevron = itemView.findViewById(R.id.tvHistoryChevron);
            }
        }
    }
}
