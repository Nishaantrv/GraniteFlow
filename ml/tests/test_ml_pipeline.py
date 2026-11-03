"""
Unit and integration test suite for GraniteFlow ML Extraction Risk Pipeline.
Tests:
1. Pre-review feature extraction & zero target leakage.
2. Feature schema compliance against feature_schema.json.
3. Label generation, 1-to-1 ground truth alignment, and unapproved record exclusion.
4. Data sufficiency gate enforcement (>=200 rows, >=50 per class).
5. Model training pipeline, GroupKFold by document ID, baseline comparisons, and serialization.
6. Single & batch inference scoring with explainable reason codes.
"""

import json
import os
import shutil
import tempfile
import pytest

import sys
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "src")))

from feature_builder import extract_features_from_prediction, FEATURE_COLUMNS
from label_builder import build_row_labels_and_alignments
from load_dataset import load_dataset_from_dir
from train_quality_model import train_extraction_risk_model, MIN_TOTAL_ROWS, MIN_CLASS_ROWS
from predict_quality import ExtractionRiskPredictor
from evaluate_quality_model import evaluate_model_pipeline
from model_registry import ModelRegistry


def test_feature_builder_zero_leakage():
    """Ensure no ground truth or human edit fields leak into input features."""
    sample_prediction = {
        "blockNumber": "BLK-101",
        "grossLengthCm": 280.0,
        "grossHeightCm": 140.0,
        "grossWidthCm": 120.0,
        "allowanceCm": 10.0,
        "netCbm": 3.861,
        "rawRowText": "101 280 140 120 3.861",
        # Ground truth / human edit markers that must NEVER leak
        "groundTruthLength": 290.0,
        "isManuallyEdited": True,
        "reviewerNotes": "Fixed length"
    }

    features = extract_features_from_prediction(sample_prediction)

    # Features must match predefined column schema exactly
    assert set(features.keys()) == set(FEATURE_COLUMNS)

    # Must not contain any leakage keys
    assert "groundTruthLength" not in features
    assert "isManuallyEdited" not in features
    assert "reviewerNotes" not in features

    # Verify calculated values
    assert features["parsed_dimensions_count"] == 3
    assert features["has_block_number_pattern"] == 1
    assert features["is_cbm_formula_consistent"] == 1
    assert features["has_zero_or_negative_dim"] == 0
    assert features["has_excessive_allowance"] == 0


def test_feature_builder_schema_compliance():
    """Verify extracted features conform to feature_schema.json."""
    schema_path = os.path.abspath(
        os.path.join(os.path.dirname(__file__), "..", "schema", "feature_schema.json")
    )
    assert os.path.exists(schema_path), f"Missing schema file: {schema_path}"

    with open(schema_path, "r", encoding="utf-8") as f:
        schema = json.load(f)

    reg = ModelRegistry(os.path.dirname(schema_path))
    assert reg.verify_schema_compatibility(schema_path) is True


def test_label_builder_alignment_and_unapproved_exclusion():
    """Verify ground truth alignment: 0 for match, 1 for discrepancy, empty for unapproved."""
    # 1. Unapproved record must be skipped
    unapproved_record = {
        "source_document_id": "DOC-001",
        "prediction": {"blocks": [{"blockNumber": "1", "grossLengthCm": 200, "grossHeightCm": 100, "grossWidthCm": 100, "netCbm": 2.0}]},
        "ground_truth": {"isApproved": False, "blocks": [{"blockNumber": "1", "grossLengthCm": 200, "grossHeightCm": 100, "grossWidthCm": 100, "netCbm": 2.0}]}
    }
    assert build_row_labels_and_alignments(unapproved_record) == []

    # 2. Approved record with perfect match -> LOW_OBSERVED_RISK (0)
    approved_record_match = {
        "source_document_id": "DOC-002",
        "prediction": {
            "blocks": [{
                "blockNumber": "B-201",
                "grossLengthCm": 250.0,
                "grossHeightCm": 150.0,
                "grossWidthCm": 100.0,
                "netCbm": 3.024
            }]
        },
        "ground_truth": {
            "isApproved": True,
            "blocks": [{
                "blockNumber": "B-201",
                "grossLengthCm": 250.0,
                "grossHeightCm": 150.0,
                "grossWidthCm": 100.0,
                "netCbm": 3.024
            }]
        }
    }
    aligned = build_row_labels_and_alignments(approved_record_match)
    assert len(aligned) == 1
    assert aligned[0][1] == 0  # Class 0: LOW_OBSERVED_RISK
    assert aligned[0][2] == "DOC-002"

    # 3. Approved record with dimension discrepancy -> NEEDS_REVIEW (1)
    approved_record_mismatch = {
        "source_document_id": "DOC-003",
        "prediction": {
            "blocks": [{
                "blockNumber": "B-301",
                "grossLengthCm": 220.0,  # OCR misread 220 as 280
                "grossHeightCm": 150.0,
                "grossWidthCm": 100.0,
                "netCbm": 2.50
            }]
        },
        "ground_truth": {
            "isApproved": True,
            "blocks": [{
                "blockNumber": "B-301",
                "grossLengthCm": 280.0,
                "grossHeightCm": 150.0,
                "grossWidthCm": 100.0,
                "netCbm": 3.402
            }]
        }
    }
    aligned_mismatch = build_row_labels_and_alignments(approved_record_mismatch)
    assert len(aligned_mismatch) == 1
    assert aligned_mismatch[0][1] == 1  # Class 1: NEEDS_REVIEW


def test_sufficiency_gates_stop_unqualified_training():
    """Verify that insufficient data (<200 rows) halts training with INSUFFICIENT_DATA status."""
    temp_dir = tempfile.mkdtemp()
    records_dir = os.path.join(temp_dir, "records", "rec_001")
    os.makedirs(records_dir)

    pred = {"ocrEngine": "Google ML Kit", "parserVersion": "v1.0.0", "blocks": [
        {"blockNumber": "101", "grossLengthCm": 200, "grossHeightCm": 100, "grossWidthCm": 100, "netCbm": 2.0}
    ]}
    gt = {"isApproved": True, "sourceDocumentId": "DOC-1", "blocks": [
        {"blockNumber": "101", "grossLengthCm": 200, "grossHeightCm": 100, "grossWidthCm": 100, "netCbm": 2.0}
    ]}

    with open(os.path.join(records_dir, "prediction.json"), "w") as f:
        json.dump(pred, f)
    with open(os.path.join(records_dir, "ground_truth_v1.json"), "w") as f:
        json.dump(gt, f)

    artifacts_out = os.path.join(temp_dir, "artifacts")
    report = train_extraction_risk_model(temp_dir, artifacts_out, force_experimental=False)

    assert report["sufficiency_status"] == "INSUFFICIENT_DATA"
    assert report["dataset_summary"]["total_extracted_rows"] < MIN_TOTAL_ROWS
    assert not os.path.exists(os.path.join(artifacts_out, "graniteflow_extraction_risk_model.joblib"))

    shutil.rmtree(temp_dir)


def test_model_training_and_serialization_experimental():
    """Create synthetic dataset and verify model training pipeline with GroupKFold splitting."""
    temp_dir = tempfile.mkdtemp()
    artifacts_out = os.path.join(temp_dir, "artifacts")

    # Generate 12 documents across 4 distinct document IDs
    doc_ids = ["DOC-A", "DOC-B", "DOC-C", "DOC-D"]
    rec_count = 0

    for doc_id in doc_ids:
        for i in range(3):
            rec_count += 1
            rec_dir = os.path.join(temp_dir, "records", f"rec_{rec_count}")
            os.makedirs(rec_dir)

            pred_blocks = []
            gt_blocks = []

            for b in range(10):
                # Half match (0), half mismatch (1)
                is_match = (b % 2 == 0)
                p_len = 250.0 if is_match else 280.0
                g_len = 250.0
                pred_blocks.append({
                    "blockNumber": f"BLK-{b}",
                    "grossLengthCm": p_len,
                    "grossHeightCm": 150.0,
                    "grossWidthCm": 120.0,
                    "allowanceCm": 10.0,
                    "netCbm": 3.696,
                    "rawRowText": f"BLK-{b} {p_len} 150 120"
                })
                gt_blocks.append({
                    "blockNumber": f"BLK-{b}",
                    "grossLengthCm": g_len,
                    "grossHeightCm": 150.0,
                    "grossWidthCm": 120.0,
                    "allowanceCm": 10.0,
                    "netCbm": 3.696
                })

            with open(os.path.join(rec_dir, "prediction.json"), "w") as f:
                json.dump({"ocrEngine": "Google ML Kit", "parserVersion": "v1.0.0", "blocks": pred_blocks}, f)
            with open(os.path.join(rec_dir, "ground_truth_v1.json"), "w") as f:
                json.dump({"isApproved": True, "sourceDocumentId": doc_id, "blocks": gt_blocks}, f)

    # Force experimental training with these 120 rows
    report = train_extraction_risk_model(temp_dir, artifacts_out, force_experimental=True)

    assert "evaluation_on_held_out_test_groups" in report
    assert "dummy_baseline" in report["evaluation_on_held_out_test_groups"]
    assert "logistic_regression" in report["evaluation_on_held_out_test_groups"]
    assert "random_forest" in report["evaluation_on_held_out_test_groups"]

    model_path = os.path.join(artifacts_out, "graniteflow_extraction_risk_model.joblib")
    assert os.path.exists(model_path)

    # Verify standalone evaluation
    eval_report = evaluate_model_pipeline(model_path, temp_dir)
    assert eval_report["status"] == "EVALUATION_COMPLETE"
    assert "metrics" in eval_report
    assert "confusion_matrix" in eval_report["metrics"]

    # Verify inference using trained model
    predictor = ExtractionRiskPredictor(model_path)
    risky_block = {
        "blockNumber": "INVALID",
        "grossLengthCm": 0.0,
        "grossHeightCm": 0.0,
        "grossWidthCm": 0.0,
        "allowanceCm": 10.0,
        "netCbm": 0.0
    }
    pred_res = predictor.predict_block(risky_block)
    assert pred_res["prediction"] == "NEEDS_REVIEW"
    assert "INVALID_DIMENSIONS" in pred_res["reason_codes"]

    shutil.rmtree(temp_dir)
