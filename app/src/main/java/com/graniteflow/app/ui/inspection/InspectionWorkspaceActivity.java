package com.graniteflow.app.ui.inspection;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.graniteflow.app.R;
import com.graniteflow.app.data.model.Block;
import com.graniteflow.app.data.model.BlockPhoto;
import com.graniteflow.app.data.model.BlockTypePreset;
import com.graniteflow.app.data.model.Inspection;
import com.graniteflow.app.data.repository.GraniteRepository;
import com.graniteflow.app.data.storage.BlockPhotoStorage;
import com.graniteflow.app.ocr.GeminiAiScanEngine;
import com.graniteflow.app.ocr.GraniteSlipParser;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import android.graphics.BitmapFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InspectionWorkspaceActivity extends AppCompatActivity {

    public static final String EXTRA_INSPECTION_ID = "extra_inspection_id";
    public static final String EXTRA_BLOCK_INDEX = "extra_block_index";

    private GraniteRepository repository;
    private Inspection currentInspection;
    private GeminiAiScanEngine aiScanEngine;

    private String activeTypeId = "1";
    private int currentIndex = -1; // -1 = new block, 0..n = editing block at index
    private final List<BlockPhoto> pendingPhotos = new ArrayList<>();

    // Views
    private TextView tvWorkspaceConsigneePill;
    private LinearLayout llDynamicTypePills;
    private TextView tvWatermarkBlockNo, tvBlockTypeName, tvBlockNumberHeader;
    private TextView tvWorkspaceSubtitle, tvLiveNetCbmPill, tvPhotoCountBadge, tvWorkspaceEngineBadge;
    private BlockDiagramView blockDiagramVisualizer;
    private EditText etLength, etHeight, etWidth, etBlockRemarks;
    private LinearLayout llWorkspaceEndToEndSection;
    private EditText etE2ELength, etE2EHeight, etE2EWidth, etDefectDescription;
    private Button btnWorkspacePrevBlock, btnWorkspaceNextBlock, btnWorkspaceFinishDetails;
    private TextView tvSummaryBlocks, tvSummaryGross, tvSummaryNet, tvSummaryValue;

    private ActivityResultLauncher<String> photoGalleryLauncher;
    private ActivityResultLauncher<Void> photoCameraLauncher;
    private ActivityResultLauncher<String> ocrGalleryLauncher;
    private ActivityResultLauncher<Void> ocrCameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inspection_workspace);

        repository = GraniteRepository.getInstance(this);
        aiScanEngine = new GeminiAiScanEngine();

        String inspectionId = getIntent().getStringExtra(EXTRA_INSPECTION_ID);
        if (inspectionId != null) {
            currentInspection = repository.getInspectionById(inspectionId);
        }
        if (currentInspection == null) {
            currentInspection = repository.getActiveInspection();
        }
        if (currentInspection == null) {
            currentInspection = new Inspection();
            repository.saveInspection(currentInspection);
        }

        if (currentInspection != null && currentInspection.getBlocks() != null && !currentInspection.getBlocks().isEmpty()) {
            com.graniteflow.app.engine.BlockNumberRuleEngine.enforceSequentialBlockNumbers(
                    currentInspection.getBlocks(),
                    currentInspection.getStartingBlockNumber() > 0 ? currentInspection.getStartingBlockNumber() : 101);
            repository.saveInspection(currentInspection);
        }

        setupActivityLaunchers();
        initViews();
        setupInputListeners();
        renderTypePills();

        int startBlockIndex = getIntent().getIntExtra(EXTRA_BLOCK_INDEX, 0);
        if (!currentInspection.getBlocks().isEmpty()) {
            if (startBlockIndex < 0 || startBlockIndex >= currentInspection.getBlocks().size()) {
                startBlockIndex = 0;
            }
            loadBlock(startBlockIndex);
        } else {
            currentIndex = -1;
            updateHud();
        }
        updateSummaryBar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentInspection != null) {
            Inspection refreshed = repository.getInspectionById(currentInspection.getInspectionId());
            if (refreshed != null) {
                currentInspection = refreshed;
                renderTypePills();
                updateHud();
                updateSummaryBar();
            }
        }
    }

    private void setupActivityLaunchers() {
        photoGalleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                String blockNo = getTargetBlockNumber();
                String side = getNextSideLabel();
                String savedPath = BlockPhotoStorage.saveUri(this, uri, blockNo, side);
                if (savedPath != null) {
                    addPhotoForCurrentBlock(savedPath, "Gallery Photo (" + side + ")");
                } else {
                    Toast.makeText(this, "Failed to save photo.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        photoCameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
            if (bitmap != null) {
                String blockNo = getTargetBlockNumber();
                String side = getNextSideLabel();
                String savedPath = BlockPhotoStorage.saveBitmap(this, bitmap, blockNo, side);
                if (savedPath != null) {
                    addPhotoForCurrentBlock(savedPath, "Camera Photo (" + side + ")");
                } else {
                    Toast.makeText(this, "Failed to save camera photo.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        ocrGalleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) processOcrSlip(uri, null, null);
        });

        ocrCameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
            if (bitmap != null) processOcrSlip(null, bitmap, null);
        });
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btnWorkspaceBack);
        btnBack.setOnClickListener(v -> finish());

        tvWorkspaceConsigneePill = findViewById(R.id.tvWorkspaceConsigneePill);

        llDynamicTypePills = findViewById(R.id.llDynamicTypePills);
        View btnAddNewType = findViewById(R.id.btnAddNewType);
        btnAddNewType.setOnClickListener(v -> {
            int nextId = currentInspection.getBlockTypes().size() + 1;
            showEditTypeModal(String.valueOf(nextId), true);
        });

        ImageButton btnHeaderSettings = findViewById(R.id.btnHeaderSettings);
        btnHeaderSettings.setOnClickListener(v -> showEditSessionHeaderDialog());

        ImageButton btnWorkspaceOpenList = findViewById(R.id.btnWorkspaceOpenList);
        btnWorkspaceOpenList.setOnClickListener(v -> {
            Intent intent = new Intent(this, BlockListEditActivity.class);
            intent.putExtra(BlockListEditActivity.EXTRA_INSPECTION_ID, currentInspection.getInspectionId());
            startActivity(intent);
        });

        tvWatermarkBlockNo = findViewById(R.id.tvWatermarkBlockNo);
        tvBlockTypeName = findViewById(R.id.tvBlockTypeName);
        tvBlockNumberHeader = findViewById(R.id.tvBlockNumberHeader);
        tvWorkspaceSubtitle = findViewById(R.id.tvWorkspaceSubtitle);
        tvLiveNetCbmPill = findViewById(R.id.tvLiveNetCbmPill);
        tvPhotoCountBadge = findViewById(R.id.tvPhotoCountBadge);
        tvWorkspaceEngineBadge = findViewById(R.id.tvWorkspaceEngineBadge);
        blockDiagramVisualizer = findViewById(R.id.blockDiagramVisualizer);

        etLength = findViewById(R.id.etLength);
        etHeight = findViewById(R.id.etHeight);
        etWidth = findViewById(R.id.etWidth);
        etBlockRemarks = findViewById(R.id.etBlockRemarks);

        llWorkspaceEndToEndSection = findViewById(R.id.llWorkspaceEndToEndSection);
        etE2ELength = findViewById(R.id.etE2ELength);
        etE2EHeight = findViewById(R.id.etE2EHeight);
        etE2EWidth = findViewById(R.id.etE2EWidth);
        etDefectDescription = findViewById(R.id.etDefectDescription);

        btnWorkspacePrevBlock = findViewById(R.id.btnWorkspacePrevBlock);
        btnWorkspaceNextBlock = findViewById(R.id.btnWorkspaceNextBlock);
        btnWorkspaceFinishDetails = findViewById(R.id.btnWorkspaceFinishDetails);

        tvSummaryBlocks = findViewById(R.id.tvSummaryBlocks);
        tvSummaryGross = findViewById(R.id.tvSummaryGross);
        tvSummaryNet = findViewById(R.id.tvSummaryNet);
        tvSummaryValue = findViewById(R.id.tvSummaryValue);

        btnWorkspacePrevBlock.setOnClickListener(v -> handlePrevBlock());
        btnWorkspaceNextBlock.setOnClickListener(v -> handleNextBlock());
        btnWorkspaceFinishDetails.setOnClickListener(v -> {
            Intent intent = new Intent(this, FinishDetailsActivity.class);
            intent.putExtra(FinishDetailsActivity.EXTRA_INSPECTION_ID, currentInspection.getInspectionId());
            startActivity(intent);
        });

        View btnWorkspaceAiOcr = findViewById(R.id.btnWorkspaceAiOcr);
        btnWorkspaceAiOcr.setOnClickListener(v -> showOcrPickerDialog());

        View btnWorkspaceCamera = findViewById(R.id.btnWorkspaceCamera);
        btnWorkspaceCamera.setOnClickListener(v -> photoCameraLauncher.launch(null));

        View btnWorkspaceGallery = findViewById(R.id.btnWorkspaceGallery);
        btnWorkspaceGallery.setOnClickListener(v -> photoGalleryLauncher.launch("image/*"));

        if (tvWorkspaceSubtitle != null) {
            tvWorkspaceSubtitle.setOnClickListener(v -> showBlockSwitcherDialog());
        }
        if (tvPhotoCountBadge != null) {
            tvPhotoCountBadge.setOnClickListener(v -> showBlockPhotosDialog());
        }
    }

    private String getTargetBlockNumber() {
        boolean isEditing = currentIndex >= 0 && currentIndex < currentInspection.getBlocks().size();
        if (isEditing) {
            return currentInspection.getBlocks().get(currentIndex).getBlockNumber();
        } else {
            return String.valueOf(currentInspection.getNextBlockNumber(activeTypeId));
        }
    }

    private String getNextSideLabel() {
        boolean isEditing = currentIndex >= 0 && currentIndex < currentInspection.getBlocks().size();
        int count = isEditing ? currentInspection.getBlocks().get(currentIndex).getPhotos().size() : pendingPhotos.size();
        char sideChar = (char) ('A' + count);
        return sideChar + " SIDE";
    }

    private void addPhotoForCurrentBlock(String savedPath, String label) {
        boolean isEditing = currentIndex >= 0 && currentIndex < currentInspection.getBlocks().size();
        if (isEditing) {
            Block b = currentInspection.getBlocks().get(currentIndex);
            char sideChar = (char) ('A' + b.getPhotos().size());
            b.getPhotos().add(new BlockPhoto(b.getBlockNumber(), sideChar + " SIDE", savedPath, label));
            repository.saveInspection(currentInspection);
            updatePhotoBadge(b.getPhotos().size());
            Toast.makeText(this, "Photo attached to Block #" + b.getBlockNumber(), Toast.LENGTH_SHORT).show();
        } else {
            int nextNo = currentInspection.getNextBlockNumber(activeTypeId);
            char sideChar = (char) ('A' + pendingPhotos.size());
            pendingPhotos.add(new BlockPhoto(String.valueOf(nextNo), sideChar + " SIDE", savedPath, label));
            updatePhotoBadge(pendingPhotos.size());
            Toast.makeText(this, "Photo queued (" + pendingPhotos.size() + " attached)", Toast.LENGTH_SHORT).show();
        }
    }

    private void updatePhotoBadge(int count) {
        if (tvPhotoCountBadge != null) {
            if (count > 0) {
                tvPhotoCountBadge.setVisibility(View.VISIBLE);
                tvPhotoCountBadge.setText(count + (count == 1 ? " PHOTO" : " PHOTOS"));
            } else {
                tvPhotoCountBadge.setVisibility(View.GONE);
            }
        }
    }

    private void renderTypePills() {
        llDynamicTypePills.removeAllViews();
        List<BlockTypePreset> presets = currentInspection.getBlockTypes();
        android.graphics.Typeface mediumTf = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL);

        for (BlockTypePreset p : presets) {
            TextView btn = new TextView(this);
            btn.setText(p.getName() + " • " + (int) p.getAllowance() + "cm");
            btn.setTextSize(10f);
            btn.setTypeface(mediumTf);
            btn.setGravity(android.view.Gravity.CENTER);
            btn.setPadding(dpToPx(12), 0, dpToPx(12), 0);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dpToPx(30)
            );
            params.setMargins(0, 0, dpToPx(6), 0);
            btn.setLayoutParams(params);

            boolean isActive = p.getId().equals(activeTypeId);
            if (isActive) {
                btn.setBackgroundResource(R.drawable.shape_type_pill_active);
                btn.setTextColor(getResources().getColor(R.color.white));
            } else {
                btn.setBackgroundResource(R.drawable.shape_type_pill_inactive);
                btn.setTextColor(getResources().getColor(R.color.text_secondary));
            }

            btn.setOnClickListener(v -> {
                if (activeTypeId.equals(p.getId())) {
                    showEditTypeModal(p.getId(), false);
                } else {
                    activeTypeId = p.getId();
                    renderTypePills();
                    updateHud();
                }
            });

            llDynamicTypePills.addView(btn);
        }
    }

    private void showEditSessionHeaderDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_edit_session_header);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.94);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        EditText etConsignee = dialog.findViewById(R.id.etHeaderConsignee);
        EditText etStoneType = dialog.findViewById(R.id.etHeaderStoneType);
        EditText etMarks = dialog.findViewById(R.id.etHeaderMarks);
        EditText etPrice = dialog.findViewById(R.id.etHeaderPrice);
        EditText etStartNo = dialog.findViewById(R.id.etHeaderStartBlockNo);
        TextView btnModeNet = dialog.findViewById(R.id.btnHeaderModeNet);
        TextView btnModeGross = dialog.findViewById(R.id.btnHeaderModeGross);
        LinearLayout cardToggleE2E = dialog.findViewById(R.id.cardHeaderToggleE2E);
        TextView tvE2ETitle = dialog.findViewById(R.id.tvHeaderE2ETitle);
        TextView tvE2EStatus = dialog.findViewById(R.id.tvHeaderE2EStatus);

        etConsignee.setText(currentInspection.getCustomer());
        etStoneType.setText(currentInspection.getMaterialName());
        etMarks.setText(currentInspection.getMarksAndNos());
        etPrice.setText(String.valueOf((int) currentInspection.getPricePerCbm()));
        etStartNo.setText(String.valueOf(currentInspection.getStartingBlockNumber()));

        final String[] tempMode = { currentInspection.getCalculationMode() };
        final boolean[] tempE2E = { currentInspection.isEnableEndToEnd() };

        Runnable updateDialogState = () -> {
            boolean isNet = "net".equalsIgnoreCase(tempMode[0]);
            btnModeNet.setBackgroundResource(isNet ? R.drawable.shape_badge_blue : R.drawable.shape_input_rounded);
            btnModeNet.setTextColor(getColor(isNet ? R.color.brand_blue : R.color.text_primary));
            btnModeNet.setText(isNet ? "✓ Net (Standard)" : "Net (Standard)");

            btnModeGross.setBackgroundResource(!isNet ? R.drawable.shape_badge_blue : R.drawable.shape_input_rounded);
            btnModeGross.setTextColor(getColor(!isNet ? R.color.brand_blue : R.color.text_primary));
            btnModeGross.setText(!isNet ? "✓ Gross (Round Up)" : "Gross (Round Up)");

            cardToggleE2E.setBackgroundResource(tempE2E[0] ? R.drawable.shape_badge_blue : R.drawable.shape_input_rounded);
            tvE2ETitle.setTextColor(getColor(tempE2E[0] ? R.color.brand_blue : R.color.text_primary));
            tvE2EStatus.setText(tempE2E[0] ? "✓ ON" : "OFF");
            tvE2EStatus.setTextColor(getColor(tempE2E[0] ? R.color.brand_blue : R.color.text_secondary));
        };
        updateDialogState.run();

        btnModeNet.setOnClickListener(v -> {
            tempMode[0] = "net";
            updateDialogState.run();
        });
        btnModeGross.setOnClickListener(v -> {
            tempMode[0] = "gross";
            updateDialogState.run();
        });
        cardToggleE2E.setOnClickListener(v -> {
            tempE2E[0] = !tempE2E[0];
            updateDialogState.run();
        });

        Button btnCancel = dialog.findViewById(R.id.btnCancelHeaderEdit);
        Button btnSave = dialog.findViewById(R.id.btnSaveHeaderEdit);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            int oldStartNo = currentInspection.getStartingBlockNumber();
            int newStartNo = oldStartNo;
            try { newStartNo = Integer.parseInt(etStartNo.getText().toString().trim()); } catch (Exception ignored) {}

            double newPrice = currentInspection.getPricePerCbm();
            try { newPrice = Double.parseDouble(etPrice.getText().toString().trim()); } catch (Exception ignored) {}

            currentInspection.setCustomer(etConsignee.getText().toString().trim());
            currentInspection.setMaterialName(etStoneType.getText().toString().trim());
            currentInspection.setStoneDescription(etStoneType.getText().toString().trim());
            currentInspection.setMarksAndNos(etMarks.getText().toString().trim());
            currentInspection.setPricePerCbm(newPrice);
            currentInspection.setStartingBlockNumber(newStartNo);
            currentInspection.setCalculationMode(tempMode[0]);
            currentInspection.setEnableEndToEnd(tempE2E[0]);

            boolean renumber = newStartNo != oldStartNo;
            currentInspection.recalculateAllBlocks(renumber);
            repository.saveInspection(currentInspection);

            dialog.dismiss();
            updateHud();
            updateSummaryBar();
            Toast.makeText(this, "Session Updated", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void showEditTypeModal(String typeId, boolean isNew) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_edit_block_type);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.92);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        BlockTypePreset existing = currentInspection.getPresetById(typeId);
        BlockTypePreset preset = existing != null
                ? existing
                : new BlockTypePreset(typeId, "T" + typeId, 15.0, currentInspection.getPricePerCbm(), 1);

        TextView tvTitle = dialog.findViewById(R.id.tvModalTitle);
        EditText etName = dialog.findViewById(R.id.etTypeName);
        EditText etAllowance = dialog.findViewById(R.id.etTypeAllowance);
        EditText etStartingNumber = dialog.findViewById(R.id.etTypeStartingNumber);
        EditText etPrice = dialog.findViewById(R.id.etTypePrice);
        Button btnCancel = dialog.findViewById(R.id.btnCancelEditType);
        Button btnSave = dialog.findViewById(R.id.btnSaveEditType);

        tvTitle.setText(isNew ? "Create Type T" + typeId : "Configure " + preset.getName());
        etName.setText(preset.getName());
        etAllowance.setText(String.valueOf((int) preset.getAllowance()));
        etStartingNumber.setText(String.valueOf(preset.getStartingNumber()));
        etPrice.setText(String.valueOf((int) preset.getPricePerCbm()));

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) name = "T" + typeId;

            double allw = 15.0;
            try { allw = Double.parseDouble(etAllowance.getText().toString()); } catch (Exception ignored) {}

            int startNo = 1;
            try { startNo = Integer.parseInt(etStartingNumber.getText().toString()); } catch (Exception ignored) {}

            double pr = 0.0;
            try { pr = Double.parseDouble(etPrice.getText().toString()); } catch (Exception ignored) {}

            BlockTypePreset updated = new BlockTypePreset(typeId, name, allw, pr, startNo);
            currentInspection.updateBlockTypePreset(updated);
            repository.saveInspection(currentInspection);

            activeTypeId = typeId;
            dialog.dismiss();
            renderTypePills();
            updateHud();
            updateSummaryBar();
            Toast.makeText(this, "Type " + name + " saved (" + (int) allw + "cm allowance)", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void updateHud() {
        String consigneeLabel = currentInspection.getCustomer().isEmpty()
                ? currentInspection.getLocationCode()
                : currentInspection.getCustomer();
        if (tvWorkspaceConsigneePill != null) {
            tvWorkspaceConsigneePill.setText(consigneeLabel.isEmpty() ? "SESSION" : consigneeLabel.toUpperCase());
        }

        boolean isEditing = currentIndex >= 0 && currentIndex < currentInspection.getBlocks().size();
        Block currentBlock = isEditing ? currentInspection.getBlocks().get(currentIndex) : null;
        int total = currentInspection.getBlocks().size();

        BlockTypePreset preset = currentInspection.getPresetById(activeTypeId);
        if (preset == null) {
            preset = currentInspection.getBlockTypes().get(0);
            activeTypeId = preset.getId();
        }

        String blockNoStr;
        if (isEditing && currentBlock != null) {
            blockNoStr = currentBlock.getBlockNumber();
            tvWorkspaceSubtitle.setText("✎ BLOCK " + (currentIndex + 1) + " OF " + total + " (ID #" + blockNoStr + ") ▾");
            btnWorkspaceNextBlock.setText(currentIndex < total - 1 ? "NEXT BLOCK ›" : "FINISH / ADD NEW ›");
            btnWorkspacePrevBlock.setVisibility(View.VISIBLE);
            btnWorkspacePrevBlock.setEnabled(currentIndex > 0);
            btnWorkspacePrevBlock.setAlpha(currentIndex > 0 ? 1.0f : 0.35f);
            updatePhotoBadge(currentBlock.getPhotos().size());
        } else {
            int nextNo = currentInspection.getNextBlockNumber(activeTypeId);
            blockNoStr = String.valueOf(nextNo);
            tvWorkspaceSubtitle.setText("› NEW BLOCK " + preset.getName() + " #" + blockNoStr + (total > 0 ? " ▾" : ""));
            btnWorkspaceNextBlock.setText("ADD BLOCK ›");
            btnWorkspacePrevBlock.setVisibility(total > 0 ? View.VISIBLE : View.GONE);
            btnWorkspacePrevBlock.setEnabled(total > 0);
            btnWorkspacePrevBlock.setAlpha(1.0f);
            updatePhotoBadge(pendingPhotos.size());
        }

        String paddedNo = String.format(Locale.ENGLISH, "%03d", parseBlockInt(blockNoStr));
        tvWatermarkBlockNo.setText(paddedNo);
        tvBlockTypeName.setText(preset.getName());
        tvBlockNumberHeader.setText("#" + paddedNo);

        if (llWorkspaceEndToEndSection != null) {
            llWorkspaceEndToEndSection.setVisibility(currentInspection.isEnableEndToEnd() ? View.VISIBLE : View.GONE);
        }

        if (btnWorkspaceFinishDetails != null) {
            btnWorkspaceFinishDetails.setVisibility(total > 0 ? View.VISIBLE : View.GONE);
        }

        // Display scan engine pill on HUD
        String engine = currentInspection.getScanEngineUsed();
        if (tvWorkspaceEngineBadge != null) {
            if (engine != null && !engine.equalsIgnoreCase("Manual Entry")) {
                tvWorkspaceEngineBadge.setVisibility(View.VISIBLE);
                String shortEngine = engine;
                if (engine.contains("Gemini")) shortEngine = "✨ Gemini AI";
                else if (engine.contains("Python")) shortEngine = "⚡ Python OCR";
                else if (engine.contains("ML Kit")) shortEngine = "📱 ML Kit OCR";
                tvWorkspaceEngineBadge.setText(shortEngine);
                tvWorkspaceEngineBadge.setOnClickListener(v -> {
                    Toast.makeText(this, "Slip parsed with: " + engine, Toast.LENGTH_LONG).show();
                });
            } else {
                tvWorkspaceEngineBadge.setVisibility(View.GONE);
            }
        }

        calculateLiveNetCbm();
    }

    private int parseBlockInt(String str) {
        try { return Integer.parseInt(str.trim()); } catch (Exception e) { return 1; }
    }

    private void setupInputListeners() {
        TextWatcher liveWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int count, int after) {
                calculateLiveNetCbm();
            }
            @Override public void afterTextChanged(Editable s) {}
        };

        etLength.addTextChangedListener(liveWatcher);
        etHeight.addTextChangedListener(liveWatcher);
        etWidth.addTextChangedListener(liveWatcher);

        etLength.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                etHeight.requestFocus();
                etHeight.selectAll();
                return true;
            }
            return false;
        });

        etHeight.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                etWidth.requestFocus();
                etWidth.selectAll();
                return true;
            }
            return false;
        });

        etWidth.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                handleNextBlock();
                return true;
            }
            return false;
        });
    }

    private void calculateLiveNetCbm() {
        double l = parseDouble(etLength.getText().toString());
        double h = parseDouble(etHeight.getText().toString());
        double w = parseDouble(etWidth.getText().toString());

        if (blockDiagramVisualizer != null) {
            blockDiagramVisualizer.setDimensions(l, h, w);
        }

        if (l <= 0 || h <= 0 || w <= 0) {
            if (tvLiveNetCbmPill != null) {
                tvLiveNetCbmPill.setVisibility(View.GONE);
            }
            return;
        }

        BlockTypePreset preset = currentInspection.getPresetById(activeTypeId);
        double allw = preset != null ? preset.getAllowance() : 15.0;

        double grossCbm = (l * h * w) / 1_000_000.0;
        double netCbm;

        if ("gross".equalsIgnoreCase(currentInspection.getCalculationMode())) {
            netCbm = Math.ceil(grossCbm * 1000.0) / 1000.0;
        } else {
            double nl = Math.max(0, l - allw);
            double nh = Math.max(0, h - allw);
            double nw = Math.max(0, w - allw);
            netCbm = (nl * nh * nw) / 1_000_000.0;
        }

        if (tvLiveNetCbmPill != null) {
            tvLiveNetCbmPill.setVisibility(View.VISIBLE);
            tvLiveNetCbmPill.setText(String.format(Locale.ENGLISH, "~%.3f m³", netCbm));
        }
    }

    private boolean saveCurrentBlockChanges() {
        boolean isEditing = currentIndex >= 0 && currentIndex < currentInspection.getBlocks().size();
        if (!isEditing) return false;

        double l = parseDouble(etLength.getText().toString());
        double h = parseDouble(etHeight.getText().toString());
        double w = parseDouble(etWidth.getText().toString());
        String remarks = etBlockRemarks.getText().toString().trim();

        if (l <= 0 || h <= 0 || w <= 0) {
            return false;
        }

        BlockTypePreset preset = currentInspection.getPresetById(activeTypeId);
        double allw = preset != null ? preset.getAllowance() : 15.0;
        double price = (preset != null && preset.getPricePerCbm() > 0)
                ? preset.getPricePerCbm()
                : currentInspection.getPricePerCbm();

        Block block = currentInspection.getBlocks().get(currentIndex);
        block.setGrossLengthCm(l);
        block.setGrossHeightCm(h);
        block.setGrossWidthCm(w);
        block.setAllowanceCm(allw);
        block.setType(activeTypeId);
        block.setPricePerCbm(price);
        block.setCalculationMode(currentInspection.getCalculationMode());
        block.setRemarks(remarks);

        if (currentInspection.isEnableEndToEnd()) {
            block.setL1EndToEnd(parseDouble(etE2ELength.getText().toString()));
            block.setL2EndToEnd(parseDouble(etE2EHeight.getText().toString()));
            block.setL3EndToEnd(parseDouble(etE2EWidth.getText().toString()));
            block.setDefectDescription(etDefectDescription.getText().toString().trim());
        }
        block.recalculate();

        repository.saveInspection(currentInspection);
        updateSummaryBar();
        return true;
    }

    private void handleNextBlock() {
        boolean isEditing = currentIndex >= 0 && currentIndex < currentInspection.getBlocks().size();

        if (isEditing) {
            if (!saveCurrentBlockChanges()) {
                Toast.makeText(this, "Please enter positive dimensions for Length, Height, and Width.", Toast.LENGTH_SHORT).show();
                return;
            }
            Block block = currentInspection.getBlocks().get(currentIndex);
            Toast.makeText(this, "Block #" + block.getBlockNumber() + " saved (" + String.format(Locale.ENGLISH, "%.3f m³", block.getNetCbm()) + ")", Toast.LENGTH_SHORT).show();

            if (currentIndex < currentInspection.getBlocks().size() - 1) {
                loadBlock(currentIndex + 1);
            } else {
                showLastBlockCompletionDialog();
            }
        } else {
            double l = parseDouble(etLength.getText().toString());
            double h = parseDouble(etHeight.getText().toString());
            double w = parseDouble(etWidth.getText().toString());
            String remarks = etBlockRemarks.getText().toString().trim();

            if (l <= 0 || h <= 0 || w <= 0) {
                Toast.makeText(this, "Please enter positive dimensions for Length, Height, and Width.", Toast.LENGTH_SHORT).show();
                return;
            }

            BlockTypePreset preset = currentInspection.getPresetById(activeTypeId);
            double allw = preset != null ? preset.getAllowance() : 15.0;
            double price = (preset != null && preset.getPricePerCbm() > 0)
                    ? preset.getPricePerCbm()
                    : currentInspection.getPricePerCbm();

            int nextNo = currentInspection.getNextBlockNumber(activeTypeId);
            Block newBlock = new Block(
                    String.valueOf(nextNo),
                    l, h, w,
                    allw,
                    activeTypeId,
                    price,
                    currentInspection.getCalculationMode()
            );
            newBlock.setRemarks(remarks);

            if (currentInspection.isEnableEndToEnd()) {
                newBlock.setL1EndToEnd(parseDouble(etE2ELength.getText().toString()));
                newBlock.setL2EndToEnd(parseDouble(etE2EHeight.getText().toString()));
                newBlock.setL3EndToEnd(parseDouble(etE2EWidth.getText().toString()));
                newBlock.setDefectDescription(etDefectDescription.getText().toString().trim());
            }
            if (!pendingPhotos.isEmpty()) {
                newBlock.getPhotos().addAll(pendingPhotos);
                pendingPhotos.clear();
            }
            newBlock.recalculate();

            currentInspection.getBlocks().add(newBlock);
            repository.saveInspection(currentInspection);

            Toast.makeText(this, "Block #" + String.format(Locale.ENGLISH, "%03d", nextNo) + " Added • " + String.format(Locale.ENGLISH, "%.3f m³", newBlock.getNetCbm()), Toast.LENGTH_SHORT).show();
            resetForm();
        }

        updateSummaryBar();
    }

    private void showLastBlockCompletionDialog() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Completed All " + currentInspection.getBlocks().size() + " Blocks")
                .setMessage("You have reviewed the last block in this session. What would you like to do?")
                .setPositiveButton("Finish Details ›", (dlg, w) -> {
                    Intent intent = new Intent(this, FinishDetailsActivity.class);
                    intent.putExtra(FinishDetailsActivity.EXTRA_INSPECTION_ID, currentInspection.getInspectionId());
                    startActivity(intent);
                })
                .setNegativeButton("+ Add Block #" + (currentInspection.getBlocks().size() + 1), (dlg, w) -> {
                    resetForm();
                })
                .setNeutralButton("Stay Here", null)
                .show();
    }

    private void handlePrevBlock() {
        saveCurrentBlockChanges();
        if (currentIndex > 0) {
            loadBlock(currentIndex - 1);
        } else if (currentIndex == -1 && !currentInspection.getBlocks().isEmpty()) {
            loadBlock(currentInspection.getBlocks().size() - 1);
        } else {
            Toast.makeText(this, "Already on first block", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadBlock(int index) {
        if (index < 0 || index >= currentInspection.getBlocks().size()) return;
        currentIndex = index;
        Block b = currentInspection.getBlocks().get(index);
        activeTypeId = b.getType() != null ? b.getType() : "1";

        etLength.setText(formatDimension(b.getGrossLengthCm()));
        etHeight.setText(formatDimension(b.getGrossHeightCm()));
        etWidth.setText(formatDimension(b.getGrossWidthCm()));
        etBlockRemarks.setText(b.getRemarks() != null ? b.getRemarks() : "");

        if (currentInspection.isEnableEndToEnd()) {
            etE2ELength.setText(b.getL1EndToEnd() > 0 ? formatDimension(b.getL1EndToEnd()) : "");
            etE2EHeight.setText(b.getL2EndToEnd() > 0 ? formatDimension(b.getL2EndToEnd()) : "");
            etE2EWidth.setText(b.getL3EndToEnd() > 0 ? formatDimension(b.getL3EndToEnd()) : "");
            etDefectDescription.setText(b.getDefectDescription() != null ? b.getDefectDescription() : "");
        }

        renderTypePills();
        updateHud();
        calculateLiveNetCbm();
    }

    private String formatDimension(double val) {
        if (val <= 0) return "";
        if (val == (long) val) {
            return String.valueOf((long) val);
        } else {
            return String.format(Locale.ENGLISH, "%.1f", val);
        }
    }

    private void showBlockSwitcherDialog() {
        if (currentInspection.getBlocks().isEmpty()) return;
        List<Block> blocks = currentInspection.getBlocks();
        String[] items = new String[blocks.size() + 1];
        for (int i = 0; i < blocks.size(); i++) {
            Block b = blocks.get(i);
            items[i] = (i == currentIndex ? "✓ " : "  ")
                    + "Block #" + b.getBlockNumber() + ": "
                    + (int) b.getGrossLengthCm() + "×" + (int) b.getGrossHeightCm() + "×" + (int) b.getGrossWidthCm()
                    + " (" + String.format(Locale.ENGLISH, "%.3f m³", b.getNetCbm()) + ")"
                    + (b.getPhotos().isEmpty() ? "" : " • 📷 " + b.getPhotos().size());
        }
        items[blocks.size()] = "+ Add New Block...";

        new android.app.AlertDialog.Builder(this)
                .setTitle("Select Block (" + blocks.size() + " Total)")
                .setItems(items, (dialog, which) -> {
                    saveCurrentBlockChanges();
                    if (which < blocks.size()) {
                        loadBlock(which);
                    } else {
                        resetForm();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showBlockPhotosDialog() {
        boolean isEditing = currentIndex >= 0 && currentIndex < currentInspection.getBlocks().size();
        Block b = isEditing ? currentInspection.getBlocks().get(currentIndex) : null;
        List<BlockPhoto> photos = (b != null) ? b.getPhotos() : pendingPhotos;

        if (photos.isEmpty()) {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Photos for " + (b != null ? "Block #" + b.getBlockNumber() : "New Block"))
                    .setMessage("No photos attached to this block yet. Take or select a photo now?")
                    .setPositiveButton("Take Photo", (dlg, w) -> photoCameraLauncher.launch(null))
                    .setNegativeButton("From Gallery", (dlg, w) -> photoGalleryLauncher.launch("image/*"))
                    .setNeutralButton("Cancel", null)
                    .show();
            return;
        }

        String blockNo = (b != null) ? b.getBlockNumber() : "New Block";
        String[] photoItems = new String[photos.size() + 2];
        for (int i = 0; i < photos.size(); i++) {
            BlockPhoto p = photos.get(i);
            String side = p.getSideLabel() != null ? p.getSideLabel() : "Angle " + (i + 1);
            photoItems[i] = "📷 " + side + " (Tap to delete)";
        }
        photoItems[photos.size()] = "📷 Take Another Photo";
        photoItems[photos.size() + 1] = "🖼️ Choose from Gallery";

        new android.app.AlertDialog.Builder(this)
                .setTitle("Photos for Block #" + blockNo + " (" + photos.size() + ")")
                .setItems(photoItems, (dialog, which) -> {
                    if (which < photos.size()) {
                        new android.app.AlertDialog.Builder(this)
                                .setTitle("Delete Photo?")
                                .setMessage("Remove photo " + (which + 1) + " from Block #" + blockNo + "?")
                                .setPositiveButton("Delete", (d, w) -> {
                                    photos.remove(which);
                                    if (b != null) repository.saveInspection(currentInspection);
                                    updatePhotoBadge(photos.size());
                                    Toast.makeText(this, "Photo removed", Toast.LENGTH_SHORT).show();
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    } else if (which == photos.size()) {
                        photoCameraLauncher.launch(null);
                    } else {
                        photoGalleryLauncher.launch("image/*");
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void resetForm() {
        currentIndex = -1;
        pendingPhotos.clear();
        etLength.setText("");
        etHeight.setText("");
        etWidth.setText("");
        etBlockRemarks.setText("");
        if (etE2ELength != null) etE2ELength.setText("");
        if (etE2EHeight != null) etE2EHeight.setText("");
        if (etE2EWidth != null) etE2EWidth.setText("");
        if (etDefectDescription != null) etDefectDescription.setText("");
        etLength.requestFocus();

        updateHud();
    }

    private void updateSummaryBar() {
        tvSummaryBlocks.setText(String.valueOf(currentInspection.getTotalBlocksCount()));
        tvSummaryGross.setText(String.format(Locale.ENGLISH, "%.3f m³", currentInspection.calculateTotalGrossCbm()));
        tvSummaryNet.setText(String.format(Locale.ENGLISH, "%.3f m³", currentInspection.calculateTotalNetCbm()));
        tvSummaryValue.setText(String.format(Locale.ENGLISH, "%.0f %s", currentInspection.calculateTotalValue(), currentInspection.getCurrency()));
    }

    private void showOcrPickerDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_ocr_source_picker);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.92);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        View btnCamera = dialog.findViewById(R.id.btnPickerCamera);
        View btnGallery = dialog.findViewById(R.id.btnPickerGallery);
        View btnDemo = dialog.findViewById(R.id.btnPickerDemo);
        View btnCancel = dialog.findViewById(R.id.btnPickerCancel);

        if (btnCamera != null) {
            btnCamera.setOnClickListener(v -> {
                dialog.dismiss();
                ocrCameraLauncher.launch(null);
            });
        }
        if (btnGallery != null) {
            btnGallery.setOnClickListener(v -> {
                dialog.dismiss();
                ocrGalleryLauncher.launch("image/*");
            });
        }
        if (btnDemo != null) {
            btnDemo.setOnClickListener(v -> {
                dialog.dismiss();
                processOcrSlip(null, null, "sample_quarry_slip.png");
            });
        }
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }
        dialog.show();
    }

    private void processOcrSlip(@Nullable Uri imageUri, @Nullable Bitmap bitmap, @Nullable String assetFileName) {
        Dialog progress = new Dialog(this);
        progress.setContentView(android.R.layout.simple_list_item_1);
        TextView tv = progress.findViewById(android.R.id.text1);
        if (tv != null) {
            tv.setText("✨ Scanning quarry slip with Gemini Vision AI...");
            tv.setPadding(36, 36, 36, 36);
        }
        progress.setCancelable(false);
        progress.show();

        GeminiAiScanEngine.ScanCallback callback = new GeminiAiScanEngine.ScanCallback() {
            @Override
            public void onSuccess(GraniteSlipParser.ParsedInspectionResult parsed, String engineUsed) {
                progress.dismiss();
                if (parsed.blocks.isEmpty()) {
                    Toast.makeText(InspectionWorkspaceActivity.this, "No block dimensions identified on slip.", Toast.LENGTH_SHORT).show();
                    return;
                }
                BlockTypePreset preset = currentInspection.getPresetById(activeTypeId);
                double allw = preset != null ? preset.getAllowance() : 15.0;
                double price = preset != null ? preset.getPricePerCbm() : currentInspection.getPricePerCbm();

                int firstNewIndex = currentInspection.getBlocks().size();
                for (Block b : parsed.blocks) {
                    b.setAllowanceCm(allw);
                    b.setType(activeTypeId);
                    b.setPricePerCbm(price);
                    b.setCalculationMode(currentInspection.getCalculationMode());
                    b.recalculate();
                    currentInspection.getBlocks().add(b);
                }
                currentInspection.setScanEngineUsed(engineUsed);

                // Cache slip image and serialize original predictions for continuous training
                String cachedPath = cacheSlipImageLocally(imageUri, bitmap, assetFileName);
                if (cachedPath != null) {
                    currentInspection.setOriginalSlipImagePath(cachedPath);
                }
                try {
                    currentInspection.setRawOcrPredictionJson(new Gson().toJson(parsed));
                } catch (Exception ignored) {}

                repository.saveInspection(currentInspection);
                updateSummaryBar();
                Toast.makeText(InspectionWorkspaceActivity.this,
                        "✨ " + engineUsed + ": Extracted " + parsed.blocks.size() + " blocks!",
                        Toast.LENGTH_SHORT).show();
                loadBlock(firstNewIndex);
            }

            @Override
            public void onFailure(Exception e) {
                progress.dismiss();
                Toast.makeText(InspectionWorkspaceActivity.this, "AI Scan failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        if (assetFileName != null) {
            aiScanEngine.processAssetImage(this, assetFileName, callback);
        } else if (bitmap != null) {
            aiScanEngine.processBitmap(bitmap, callback);
        } else if (imageUri != null) {
            aiScanEngine.processUri(this, imageUri, callback);
        } else {
            progress.dismiss();
        }
    }

    private String cacheSlipImageLocally(@Nullable Uri uri, @Nullable Bitmap bmp, @Nullable String asset) {
        try {
            File dir = new File(getFilesDir(), "quarry_slips");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, "slip_" + currentInspection.getInspectionId() + ".jpg");

            Bitmap toSave = bmp;
            if (toSave == null && uri != null) {
                try (InputStream is = getContentResolver().openInputStream(uri)) {
                    toSave = BitmapFactory.decodeStream(is);
                }
            }
            if (toSave == null && asset != null) {
                try (InputStream is = getAssets().open(asset)) {
                    toSave = BitmapFactory.decodeStream(is);
                }
            }
            if (toSave != null) {
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    toSave.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                }
                return file.getAbsolutePath();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private double parseDouble(String str) {
        try {
            return Double.parseDouble(str.trim());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (aiScanEngine != null) aiScanEngine.close();
    }
}
