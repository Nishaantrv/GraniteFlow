"""
Evaluation script for GraniteFlow Extraction Risk Model.
Evaluates a serialized scikit-learn pipeline against an exported Golden Dataset.
Computes precision, recall, F1, balanced accuracy, PR-AUC, and confusion matrix.
"""

import argparse
import json
import os
import sys
from typing import Dict, Any, List
import numpy as np
import joblib

from sklearn.metrics import (
    precision_score, recall_score, f1_score, confusion_matrix,
    balanced_accuracy_score, precision_recall_curve, auc, brier_score_loss
)

from load_dataset import load_dataset_from_dir
from label_builder import build_row_labels_and_alignments
from feature_builder import extract_features_from_prediction, FEATURE_COLUMNS


def evaluate_model_pipeline(
    model_path: str,
    dataset_dir: str,
    output_report_path: str = None
) -> Dict[str, Any]:
    """
    Evaluates a saved model pipeline against the specified dataset directory.
    """
    if not os.path.exists(model_path):
        raise FileNotFoundError(f"Model file not found: {model_path}")

    model = joblib.load(model_path)
    records = load_dataset_from_dir(dataset_dir)

    all_features: List[Dict[str, Any]] = []
    all_labels: List[int] = []
    all_groups: List[str] = []

    for rec in records:
        aligned_rows = build_row_labels_and_alignments(rec)
        doc_type = rec.get("document_type", "QUARRY_MEASUREMENT_SLIP")
        ocr_engine = rec.get("prediction", {}).get("ocrEngine", "Google ML Kit")
        parser_ver = rec.get("prediction", {}).get("parserVersion", "v1.0.0")

        for pred_block, label, doc_id in aligned_rows:
            feat = extract_features_from_prediction(
                pred_block,
                document_type=doc_type,
                ocr_engine=ocr_engine,
                parser_version=parser_ver
            )
            all_features.append(feat)
            all_labels.append(label)
            all_groups.append(doc_id)

    if not all_labels:
        result = {
            "status": "NO_EVALUATION_DATA",
            "message": "No valid approved ground-truth rows found in dataset."
        }
        if output_report_path:
            with open(output_report_path, "w", encoding="utf-8") as f:
                json.dump(result, f, indent=2)
        return result

    X_mat = [[r[c] for c in FEATURE_COLUMNS] for r in all_features]
    y_true = np.array(all_labels)

    y_pred = model.predict(X_mat)

    # Check if predict_proba is supported
    has_proba = hasattr(model, "predict_proba")
    y_prob = None
    pr_auc_val = None
    brier_val = None

    if has_proba:
        try:
            probs = model.predict_proba(X_mat)
            if probs.shape[1] >= 2:
                y_prob = probs[:, 1]
                precision_pts, recall_pts, _ = precision_recall_curve(y_true, y_prob)
                pr_auc_val = float(auc(recall_pts, precision_pts))
                brier_val = float(brier_score_loss(y_true, y_prob))
        except Exception:
            pass

    cm = confusion_matrix(y_true, y_pred, labels=[0, 1]).tolist()

    report: Dict[str, Any] = {
        "status": "EVALUATION_COMPLETE",
        "sample_counts": {
            "total_rows": len(y_true),
            "class_0_low_risk": int(sum(1 for y in y_true if y == 0)),
            "class_1_needs_review": int(sum(1 for y in y_true if y == 1)),
            "unique_source_documents": len(set(all_groups))
        },
        "metrics": {
            "precision_needs_review": float(precision_score(y_true, y_pred, pos_label=1, zero_division=0)),
            "recall_needs_review": float(recall_score(y_true, y_pred, pos_label=1, zero_division=0)),
            "f1_needs_review": float(f1_score(y_true, y_pred, pos_label=1, zero_division=0)),
            "balanced_accuracy": float(balanced_accuracy_score(y_true, y_pred)),
            "pr_auc": pr_auc_val,
            "brier_score": brier_val,
            "confusion_matrix": cm
        }
    }

    if output_report_path:
        os.makedirs(os.path.dirname(os.path.abspath(output_report_path)), exist_ok=True)
        with open(output_report_path, "w", encoding="utf-8") as f:
            json.dump(report, f, indent=2)

    return report


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Evaluate GraniteFlow Extraction Risk Model.")
    parser.add_argument("--model", type=str, required=True, help="Path to serialized .joblib model")
    parser.add_argument("--dataset", type=str, required=True, help="Path to exported dataset directory")
    parser.add_argument("--output", type=str, default=None, help="Optional output JSON report path")

    args = parser.parse_args()
    res = evaluate_model_pipeline(args.model, args.dataset, args.output)
    print(json.dumps(res, indent=2))
