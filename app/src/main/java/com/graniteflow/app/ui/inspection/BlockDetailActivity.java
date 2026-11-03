package com.graniteflow.app.ui.inspection;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.graniteflow.app.R;
import com.graniteflow.app.data.model.Block;
import com.graniteflow.app.data.model.BlockPhoto;
import com.graniteflow.app.data.model.Inspection;
import com.graniteflow.app.data.repository.GraniteRepository;
import com.graniteflow.app.data.storage.BlockPhotoStorage;

import java.util.List;

public class BlockDetailActivity extends AppCompatActivity {

    private String inspectionId;
    private String blockNumber;
    private Inspection currentInspection;
    private Block currentBlock;
    private GraniteRepository repository;

    private TextView tvBlockTitle, tvGrossL, tvGrossH, tvGrossW, tvGrossCbm;
    private TextView tvNetL, tvNetH, tvNetW, tvNetCbmValue, tvAllowanceMerged;
    private EditText etRemarks;
    private BlockDiagramView diagramView;
    private RecyclerView rvPhotos;
    private PhotoAdapter photoAdapter;

    private ActivityResultLauncher<String> photoPickerLauncher;
    private Uri pendingPhotoUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_block_detail);

        repository = GraniteRepository.getInstance(this);
        inspectionId = getIntent().getStringExtra("extra_inspection_id");
        blockNumber = getIntent().getStringExtra("extra_block_number");

        currentInspection = repository.getInspectionById(inspectionId);
        if (currentInspection != null && blockNumber != null) {
            for (Block b : currentInspection.getBlocks()) {
                if (b.getBlockNumber().equals(blockNumber)) {
                    currentBlock = b;
                    break;
                }
            }
        }

        initViews();
        setupPhotoPicker();
        populateBlockData();
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        tvBlockTitle = findViewById(R.id.tvBlockTitle);
        tvGrossL = findViewById(R.id.tvGrossL);
        tvGrossH = findViewById(R.id.tvGrossH);
        tvGrossW = findViewById(R.id.tvGrossW);
        tvGrossCbm = findViewById(R.id.tvGrossCbm);

        tvNetL = findViewById(R.id.tvNetL);
        tvNetH = findViewById(R.id.tvNetH);
        tvNetW = findViewById(R.id.tvNetW);
        tvNetCbmValue = findViewById(R.id.tvNetCbmValue);
        tvAllowanceMerged = findViewById(R.id.tvAllowanceMerged);

        etRemarks = findViewById(R.id.etRemarks);
        diagramView = findViewById(R.id.diagramView);

        rvPhotos = findViewById(R.id.rvPhotos);
        rvPhotos.setLayoutManager(new LinearLayoutManager(this));

        Button btnAddPhoto = findViewById(R.id.btnAddPhoto);
        btnAddPhoto.setOnClickListener(v -> photoPickerLauncher.launch("image/*"));

        Button btnSaveBlock = findViewById(R.id.btnSaveBlock);
        btnSaveBlock.setOnClickListener(v -> saveBlockChanges());

        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setSelectedItemId(R.id.nav_marking);
        bottomNavigation.setOnItemSelectedListener(item -> {
            finish(); // return to main workflow tabs
            return true;
        });
    }

    private void setupPhotoPicker() {
        photoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        pendingPhotoUri = uri;
                        showSideLabelDialog(uri);
                    }
                }
        );
    }

    private void showSideLabelDialog(Uri uri) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_ocr_review); // reuse card or build quick dialog
        // Simple input dialog for side label
        EditText etSide = new EditText(this);
        etSide.setHint("e.g. A SIDE, B SIDE, TOP, FRONT, BACK");
        etSide.setText("A SIDE");

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Select Photo Side Label")
                .setView(etSide)
                .setPositiveButton("Attach", (d, which) -> {
                    String side = etSide.getText().toString().trim();
                    if (side.isEmpty()) side = "A SIDE";
                    String savedPath = BlockPhotoStorage.saveUri(this, uri, currentBlock.getBlockNumber(), side);
                    if (savedPath != null) {
                        BlockPhoto photo = new BlockPhoto(currentBlock.getBlockNumber(), side, savedPath, "Quarry marking face");
                        currentBlock.getPhotos().add(photo);
                        repository.saveInspection(currentInspection);
                        updatePhotosList();
                        Toast.makeText(this, "Photo added to " + currentBlock.getBlockNumber() + "/" + side, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to save photo.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void populateBlockData() {
        if (currentBlock == null) return;

        tvBlockTitle.setText("BLOCK NO : " + currentBlock.getBlockNumber());
        tvGrossL.setText(String.valueOf((int) currentBlock.getGrossLengthCm()));
        tvGrossH.setText(String.valueOf((int) currentBlock.getGrossHeightCm()));
        tvGrossW.setText(String.valueOf((int) currentBlock.getGrossWidthCm()));
        tvGrossCbm.setText(String.format("%.3f", currentBlock.getGrossCbm()));

        tvAllowanceMerged.setText((int) currentBlock.getAllowanceCm() + " CM");

        tvNetL.setText(String.valueOf((int) currentBlock.getNetLengthCm()));
        tvNetH.setText(String.valueOf((int) currentBlock.getNetHeightCm()));
        tvNetW.setText(String.valueOf((int) currentBlock.getNetWidthCm()));
        tvNetCbmValue.setText(String.format("%.3f", currentBlock.getNetCbm()));

        etRemarks.setText(currentBlock.getRemarks());

        // Update vector isometric diagrams View A & View B
        diagramView.setDimensions(
                currentBlock.getGrossLengthCm(),
                currentBlock.getGrossHeightCm(),
                currentBlock.getGrossWidthCm()
        );

        updatePhotosList();
    }

    private void updatePhotosList() {
        if (currentBlock == null) return;
        photoAdapter = new PhotoAdapter(currentBlock.getPhotos(), photo -> {
            currentBlock.getPhotos().remove(photo);
            repository.saveInspection(currentInspection);
            updatePhotosList();
            Toast.makeText(this, "Photo removed.", Toast.LENGTH_SHORT).show();
        });
        rvPhotos.setAdapter(photoAdapter);
    }

    private void saveBlockChanges() {
        if (currentBlock == null || currentInspection == null) return;
        currentBlock.setRemarks(etRemarks.getText().toString().trim());
        repository.saveInspection(currentInspection);
        Toast.makeText(this, "Block " + currentBlock.getBlockNumber() + " saved successfully!", Toast.LENGTH_SHORT).show();
        finish();
    }

    // Inner Photo Adapter
    private static class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.ViewHolder> {
        private final List<BlockPhoto> list;
        private final OnPhotoDeleteListener deleteListener;

        interface OnPhotoDeleteListener {
            void onDelete(BlockPhoto photo);
        }

        PhotoAdapter(List<BlockPhoto> list, OnPhotoDeleteListener deleteListener) {
            this.list = list;
            this.deleteListener = deleteListener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_photo, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            BlockPhoto photo = list.get(position);
            holder.tvPhotoTitle.setText(photo.getDisplayTitle());
            holder.tvPhotoCaption.setText(photo.getCaption() != null ? photo.getCaption() : "Quarry face marking");

            if (photo.getLocalUri() != null && !photo.getLocalUri().isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(photo.getLocalUri())
                        .placeholder(R.drawable.ic_camera)
                        .into(holder.ivBlockPhoto);
            } else {
                holder.ivBlockPhoto.setImageResource(R.drawable.ic_camera);
            }

            holder.btnDeletePhoto.setOnClickListener(v -> deleteListener.onDelete(photo));
        }

        @Override
        public int getItemCount() {
            return list != null ? list.size() : 0;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvPhotoTitle, tvPhotoCaption;
            ImageView ivBlockPhoto;
            Button btnDeletePhoto;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvPhotoTitle = itemView.findViewById(R.id.tvPhotoTitle);
                tvPhotoCaption = itemView.findViewById(R.id.tvPhotoCaption);
                ivBlockPhoto = itemView.findViewById(R.id.ivBlockPhoto);
                btnDeletePhoto = itemView.findViewById(R.id.btnDeletePhoto);
            }
        }
    }
}
