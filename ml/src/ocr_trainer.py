#!/usr/bin/env python3
"""
GraniteFlow - Neural Text Recognizer & Continuous Online Trainer.

Architecture:
- CRNN (CNN + BiLSTM + CTC Loss) for granite quarry slips, dimensional figures, and block numbers.
- Automated crop-to-target alignment.
- Computes dimensional MAE, Volumetric CBM error, and Character CTC Loss.
- Persists verified inspections into the Golden Dataset archive.
- Saves checkpoint weights to ml/artifacts/quarry_crnn_weights.pth.
"""

import os
import re
import json
import time
import math
from typing import Dict, Any, List, Tuple, Optional
import numpy as np
from PIL import Image

import torch
import torch.nn as nn
import torch.optim as optim

try:
    import cv2
    HAVE_CV2 = True
except ImportError:
    HAVE_CV2 = False


# Vocabulary: 0 is CTC Blank
CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz.-_/ x*+:#"
CHAR2IDX = {c: i + 1 for i, c in enumerate(CHARACTERS)}
IDX2CHAR = {i + 1: c for i, c in enumerate(CHARACTERS)}
NUM_CLASSES = len(CHARACTERS) + 1  # +1 for blank at 0


class QuarryCRNN(nn.Module):
    """
    Convolutional Recurrent Neural Network for dimension & text recognition.
    """
    def __init__(self, num_classes: int = NUM_CLASSES):
        super().__init__()
        # CNN Feature Extractor
        self.conv = nn.Sequential(
            nn.Conv2d(1, 32, 3, padding=1),
            nn.BatchNorm2d(32),
            nn.ReLU(True),
            nn.MaxPool2d(2, 2),  # H: 32 -> 16

            nn.Conv2d(32, 64, 3, padding=1),
            nn.BatchNorm2d(64),
            nn.ReLU(True),
            nn.MaxPool2d(2, 2),  # H: 16 -> 8

            nn.Conv2d(64, 128, 3, padding=1),
            nn.BatchNorm2d(128),
            nn.ReLU(True),
            nn.MaxPool2d((2, 1), (2, 1)),  # H: 8 -> 4

            nn.Conv2d(128, 256, 3, padding=1),
            nn.BatchNorm2d(256),
            nn.ReLU(True),
            nn.MaxPool2d((2, 1), (2, 1)),  # H: 4 -> 2

            nn.Conv2d(256, 256, (2, 1)),  # H: 2 -> 1
            nn.BatchNorm2d(256),
            nn.ReLU(True)
        )

        # Recurrent Sequence Modeling
        self.rnn = nn.LSTM(
            input_size=256,
            hidden_size=128,
            num_layers=2,
            bidirectional=True,
            batch_first=True,
            dropout=0.1
        )

        # Final character projection
        self.classifier = nn.Linear(128 * 2, num_classes)

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        # x: (B, 1, 32, W)
        features = self.conv(x)  # (B, 256, 1, W')
        features = features.squeeze(2)  # (B, 256, W')
        features = features.permute(0, 2, 1)  # (B, W', 256)

        recurrent_out, _ = self.rnn(features)  # (B, W', 256)
        logits = self.classifier(recurrent_out)  # (B, W', num_classes)
        return logits.log_softmax(2)


class OcrTrainer:
    """
    Manages continuous online training, loss tracking, and Golden Dataset persistence.
    """
    def __init__(self, artifacts_dir: Optional[str] = None, data_dir: Optional[str] = None):
        base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        self.artifacts_dir = artifacts_dir or os.path.join(base_dir, "artifacts")
        self.data_dir = data_dir or os.path.join(base_dir, "data")
        self.records_dir = os.path.join(self.data_dir, "records")
        os.makedirs(self.artifacts_dir, exist_ok=True)
        os.makedirs(self.records_dir, exist_ok=True)

        self.weights_path = os.path.join(self.artifacts_dir, "quarry_crnn_weights.pth")
        self.history_path = os.path.join(self.artifacts_dir, "training_history.json")

        self.device = torch.device("cpu")
        self.model = QuarryCRNN(num_classes=NUM_CLASSES).to(self.device)
        self.optimizer = optim.AdamW(self.model.parameters(), lr=1e-3, weight_decay=1e-4)
        self.ctc_loss_fn = nn.CTCLoss(blank=0, zero_infinity=True)

        self.load_checkpoint()

    def load_checkpoint(self):
        if os.path.exists(self.weights_path):
            try:
                checkpoint = torch.load(self.weights_path, map_location=self.device)
                self.model.load_state_dict(checkpoint["model_state"])
                if "optimizer_state" in checkpoint:
                    self.optimizer.load_state_dict(checkpoint["optimizer_state"])
                print(f"[OcrTrainer] Successfully loaded checkpoint from {self.weights_path}")
            except Exception as e:
                print(f"[OcrTrainer] Warning: Could not load checkpoint ({e}), initializing fresh model.")

    def save_checkpoint(self, sample_count: int, last_loss: float):
        torch.save({
            "model_state": self.model.state_dict(),
            "optimizer_state": self.optimizer.state_dict(),
            "timestamp": time.time(),
            "sample_count": sample_count,
            "last_loss": last_loss
        }, self.weights_path)
        print(f"[OcrTrainer] Checkpoint saved to {self.weights_path}")

    @staticmethod
    def preprocess_patch(img_patch: Image.Image) -> Optional[torch.Tensor]:
        """
        Resizes text crop to height 32 while preserving aspect ratio, converts to grayscale tensor.
        """
        try:
            if img_patch.mode != "L":
                img_patch = img_patch.convert("L")

            w, h = img_patch.size
            if h <= 0 or w <= 0:
                return None

            new_h = 32
            new_w = max(32, int(round(w * (new_h / float(h)))))
            new_w = min(new_w, 512)

            resized = img_patch.resize((new_w, new_h), Image.Resampling.BILINEAR)
            arr = np.array(resized, dtype=np.float32) / 255.0
            # Standard normalization
            arr = (arr - 0.5) / 0.5
            tensor = torch.from_numpy(arr).unsqueeze(0).unsqueeze(0)  # (1, 1, 32, W)
            return tensor
        except Exception:
            return None

    def decode_prediction(self, logits: torch.Tensor) -> str:
        """
        Greedy CTC decoding.
        """
        preds = torch.argmax(logits, dim=2)  # (1, W')
        preds = preds.squeeze(0).cpu().numpy()
        chars = []
        prev = 0
        for p in preds:
            if p != 0 and p != prev:
                if p in IDX2CHAR:
                    chars.append(IDX2CHAR[p])
            prev = p
        return "".join(chars)

    def train_on_pairs(self, patch_pairs: List[Tuple[Image.Image, str]]) -> float:
        """
        Trains the CRNN on a list of (image_patch, target_text) pairs.
        Returns average CTC loss.
        """
        if not patch_pairs:
            return 0.0

        self.model.train()
        total_loss = 0.0
        valid_steps = 0

        for patch, target_text in patch_pairs:
            clean_target = "".join([c for c in target_text if c in CHAR2IDX])
            if not clean_target:
                continue

            tensor = self.preprocess_patch(patch)
            if tensor is None:
                continue

            tensor = tensor.to(self.device)
            target_indices = torch.tensor([CHAR2IDX[c] for c in clean_target], dtype=torch.long, device=self.device)
            target_lengths = torch.tensor([len(clean_target)], dtype=torch.long, device=self.device)

            self.optimizer.zero_grad()
            # Forward pass: log_probs has shape (B, T, C)
            log_probs = self.model(tensor)  # (1, T, C)
            # CTCLoss expects (T, B, C)
            log_probs = log_probs.permute(1, 0, 2)
            input_lengths = torch.tensor([log_probs.size(0)], dtype=torch.long, device=self.device)

            if input_lengths.item() < target_lengths.item():
                continue

            loss = self.ctc_loss_fn(log_probs, target_indices, input_lengths, target_lengths)
            if not torch.isnan(loss) and not torch.isinf(loss):
                loss.backward()
                torch.nn.utils.clip_grad_norm_(self.model.parameters(), 5.0)
                self.optimizer.step()
                total_loss += loss.item()
                valid_steps += 1

        return total_loss / max(valid_steps, 1)

    def extract_patch_pairs_from_slip(
        self,
        slip_image_path: str,
        actual_blocks: List[Dict[str, Any]]
    ) -> List[Tuple[Image.Image, str]]:
        """
        Extracts image bounding box crops from the quarry slip matching the verified actual blocks.
        """
        pairs = []
        if not os.path.exists(slip_image_path):
            return pairs

        try:
            full_img = Image.open(slip_image_path)
            w, h = full_img.size

            # If OpenCV is available, detect text contours
            if HAVE_CV2:
                cv_img = cv2.imread(slip_image_path, cv2.IMREAD_GRAYSCALE)
                if cv_img is not None:
                    # Binarize for line detection
                    thresh = cv2.adaptiveThreshold(cv_img, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
                                                   cv2.THRESH_BINARY_INV, 15, 6)
                    # Dilate horizontally to connect number digits
                    kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (12, 4))
                    dilated = cv2.dilate(thresh, kernel, iterations=1)
                    contours, _ = cv2.findContours(dilated, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

                    detected_boxes = []
                    for c in contours:
                        bx, by, bw, bh = cv2.boundingRect(c)
                        if bw >= 20 and bh >= 10 and bw < w * 0.9 and bh < h * 0.3:
                            detected_boxes.append((bx, by, bw, bh))

                    # Sort top-to-bottom
                    detected_boxes.sort(key=lambda b: (b[1] // 30, b[0]))

                    # Extract target numbers from actual blocks
                    target_strings = []
                    for b in actual_blocks:
                        b_no = str(b.get("blockNumber", "")).strip()
                        l = str(int(round(float(b.get("grossLength", b.get("grossLengthCm", 0))))))
                        h_val = str(int(round(float(b.get("grossHeight", b.get("grossHeightCm", 0))))))
                        w_val = str(int(round(float(b.get("grossWidth", b.get("grossWidthCm", 0))))))
                        cbm = str(round(float(b.get("netCbm", 0)), 3))
                        if b_no: target_strings.append(b_no)
                        if l != "0": target_strings.append(l)
                        if h_val != "0": target_strings.append(h_val)
                        if w_val != "0": target_strings.append(w_val)
                        if cbm != "0.0": target_strings.append(cbm)

                    # Map detected boxes to targets
                    for i, tgt in enumerate(target_strings):
                        if i < len(detected_boxes):
                            bx, by, bw, bh = detected_boxes[i]
                            # Pad slightly
                            pad = 3
                            x1 = max(0, bx - pad)
                            y1 = max(0, by - pad)
                            x2 = min(w, bx + bw + pad)
                            y2 = min(h, by + bh + pad)
                            crop = full_img.crop((x1, y1, x2, y2))
                            pairs.append((crop, tgt))

            # Synthetic / fallback crops if contour detection yields few boxes
            if len(pairs) == 0:
                # Build synthetic visual samples from actual text targets
                from PIL import ImageDraw, ImageFont
                for b in actual_blocks:
                    for val in [
                        str(b.get("blockNumber", "")),
                        str(int(round(float(b.get("grossLength", b.get("grossLengthCm", 0)))))),
                        str(int(round(float(b.get("grossHeight", b.get("grossHeightCm", 0)))))),
                        str(int(round(float(b.get("grossWidth", b.get("grossWidthCm", 0)))))),
                    ]:
                        if not val or val == "0":
                            continue
                        synth = Image.new("L", (max(48, len(val) * 16), 32), color=255)
                        draw = ImageDraw.Draw(synth)
                        draw.text((6, 6), val, fill=0)
                        pairs.append((synth, val))

        except Exception as e:
            print(f"[OcrTrainer] Error extracting crops: {e}")

        return pairs

    def compute_inspection_loss(
        self,
        predicted_data: Dict[str, Any],
        actual_data: Dict[str, Any]
    ) -> Dict[str, float]:
        """
        Computes exact loss between initial OCR predictions and final verified values.
        """
        pred_blocks = predicted_data.get("blocks", [])
        actual_blocks = actual_data.get("blocks", [])

        dim_errors = []
        cbm_errors = []

        for a_idx, a_blk in enumerate(actual_blocks):
            a_l = float(a_blk.get("grossLength", a_blk.get("grossLengthCm", 0.0)))
            a_h = float(a_blk.get("grossHeight", a_blk.get("grossHeightCm", 0.0)))
            a_w = float(a_blk.get("grossWidth", a_blk.get("grossWidthCm", 0.0)))
            a_cbm = float(a_blk.get("netCbm", 0.0))

            # Match with prediction by index or block number
            p_blk = None
            if a_idx < len(pred_blocks):
                p_blk = pred_blocks[a_idx]

            if p_blk:
                p_l = float(p_blk.get("grossLength", p_blk.get("grossLengthCm", 0.0)))
                p_h = float(p_blk.get("grossHeight", p_blk.get("grossHeightCm", 0.0)))
                p_w = float(p_blk.get("grossWidth", p_blk.get("grossWidthCm", 0.0)))
                p_cbm = float(p_blk.get("netCbm", 0.0))

                dim_diff = abs(p_l - a_l) + abs(p_h - a_h) + abs(p_w - a_w)
                dim_errors.append(dim_diff)
                cbm_errors.append(abs(p_cbm - a_cbm))
            else:
                dim_errors.append(50.0)  # penalty for missed block
                cbm_errors.append(1.0)

        mae_dim_cm = float(np.mean(dim_errors)) if dim_errors else 0.0
        mae_cbm = float(np.mean(cbm_errors)) if cbm_errors else 0.0

        # Normalized loss components
        # 10 cm error -> 0.1 normalized loss
        dim_loss_norm = min(1.0, mae_dim_cm / 100.0)
        cbm_loss_norm = min(1.0, mae_cbm / 5.0)

        return {
            "dimension_mae_cm": round(mae_dim_cm, 2),
            "volume_mae_cbm": round(mae_cbm, 4),
            "dim_loss_norm": round(dim_loss_norm, 4),
            "cbm_loss_norm": round(cbm_loss_norm, 4)
        }

    def process_and_train_feedback(
        self,
        image_path: str,
        predicted_json: Dict[str, Any],
        actual_json: Dict[str, Any],
        customer_name: Optional[str] = "",
        document_type: str = "QUARRY_MEASUREMENT_SLIP"
    ) -> Dict[str, Any]:
        """
        Executes end-to-end continuous learning step:
        1. Computes discrepancy loss between predicted and actual data.
        2. Aligns text regions and trains CRNN with CTC Loss.
        3. Persists Golden Dataset record.
        4. Updates training history.
        """
        actual_blocks = actual_json.get("blocks", [])

        # 1. Compute Loss Metrics
        loss_metrics = self.compute_inspection_loss(predicted_json, actual_json)

        # 2. Extract crops and train neural recognizer
        patch_pairs = self.extract_patch_pairs_from_slip(image_path, actual_blocks)
        ctc_loss = self.train_on_pairs(patch_pairs)

        total_loss = round(
            0.5 * ctc_loss +
            0.3 * loss_metrics["dim_loss_norm"] +
            0.2 * loss_metrics["cbm_loss_norm"],
            4
        )
        loss_metrics["ctc_loss"] = round(ctc_loss, 4)
        loss_metrics["total_loss"] = total_loss

        # 3. Save Golden Dataset record
        rec_id = f"REC_{int(time.time() * 1000)}"
        rec_dir = os.path.join(self.records_dir, rec_id)
        os.makedirs(rec_dir, exist_ok=True)

        # Copy slip image
        saved_img_name = "slip_image.jpg"
        target_img_path = os.path.join(rec_dir, saved_img_name)
        if os.path.exists(image_path):
            try:
                img = Image.open(image_path)
                img.save(target_img_path, format="JPEG", quality=90)
            except Exception:
                pass

        # Save prediction.json
        with open(os.path.join(rec_dir, "prediction.json"), "w", encoding="utf-8") as f:
            json.dump(predicted_json, f, indent=2)

        # Save ground_truth_v1.json
        ground_truth = {
            "sourceDocumentId": rec_id,
            "documentType": document_type,
            "customerName": customer_name or actual_json.get("customer", "UNKNOWN"),
            "materialName": actual_json.get("materialName", "BLACK GALAXY"),
            "locationCode": actual_json.get("locationCode", "QUARRY-SITE"),
            "isApproved": True,
            "approvedAt": int(time.time() * 1000),
            "blocks": actual_blocks
        }
        with open(os.path.join(rec_dir, "ground_truth_v1.json"), "w", encoding="utf-8") as f:
            json.dump(ground_truth, f, indent=2)

        # Save manifest.json
        manifest = {
            "recordId": rec_id,
            "createdAt": int(time.time() * 1000),
            "customer": ground_truth["customerName"],
            "blocksCount": len(actual_blocks),
            "lossMetrics": loss_metrics,
            "trainedSamples": len(patch_pairs)
        }
        with open(os.path.join(rec_dir, "manifest.json"), "w", encoding="utf-8") as f:
            json.dump(manifest, f, indent=2)

        # 4. Count total approved Golden Dataset records
        total_records = len([d for d in os.listdir(self.records_dir) if os.path.isdir(os.path.join(self.records_dir, d))])

        # 5. Save model checkpoint
        self.save_checkpoint(sample_count=total_records, last_loss=total_loss)

        # 6. Update training report and history
        self._update_training_history(rec_id, total_records, loss_metrics, len(patch_pairs))

        return {
            "status": "success",
            "recordId": rec_id,
            "total_golden_records": total_records,
            "trained_crops_count": len(patch_pairs),
            "loss": loss_metrics,
            "message": f"Verified ground truth saved! CRNN trained on {len(patch_pairs)} crops. Total Loss: {total_loss}"
        }

    def _update_training_history(
        self,
        record_id: str,
        total_records: int,
        loss_metrics: Dict[str, float],
        crops_count: int
    ):
        history = []
        if os.path.exists(self.history_path):
            try:
                with open(self.history_path, "r", encoding="utf-8") as f:
                    history = json.load(f)
            except Exception:
                history = []

        history.append({
            "recordId": record_id,
            "timestamp": int(time.time() * 1000),
            "cropsCount": crops_count,
            "loss": loss_metrics
        })

        with open(self.history_path, "w", encoding="utf-8") as f:
            json.dump(history, f, indent=2)

        # Update training_report.json
        report_path = os.path.join(self.artifacts_dir, "training_report.json")
        report = {
            "dataset_summary": {
                "total_extracted_rows": total_records,
                "total_approved_records": total_records,
                "latest_record_id": record_id,
                "min_required_rows": 200,
                "status": "CONTINUOUS_LEARNING_ACTIVE"
            },
            "latest_loss": loss_metrics,
            "updated_at": time.strftime("%Y-%m-%dT%H:%M:%SZ")
        }
        with open(report_path, "w", encoding="utf-8") as f:
            json.dump(report, f, indent=2)


if __name__ == "__main__":
    trainer = OcrTrainer()
    print(f"OcrTrainer initialized. Device: {trainer.device}, Weights: {trainer.weights_path}")
