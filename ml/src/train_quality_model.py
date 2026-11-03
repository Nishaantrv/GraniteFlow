"""
Model training pipeline for GraniteFlow Extraction Risk Model.
Implements:
1. Data sufficiency gate check (>=200 rows, >=50 per class) with INSUFFICIENT_DATA reporting.
2. Leakage-free Grouped holdout splitting by sourceDocumentId.
3. Baseline comparison: DummyClassifier vs LogisticRegression vs RandomForestClassifier.
4. Evaluation on held-out test groups.
5. Model artifact & metadata serialization.
"""

import json
import os
import sys
from typing import Dict, Any, List, Tuple
import numpy as np
import joblib

from sklearn.dummy import DummyClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.ensemble import RandomForestClassifier
from sklearn.pipeline import Pipeline
from sklearn.compose import ColumnTransformer
from sklearn.preprocessing import StandardScaler, OneHotEncoder
from sklearn.model_selection import GroupKFold
from sklearn.metrics import (
    precision_score, recall_score, f1_score, confusion_matrix,
    balanced_accuracy_score, precision_recall_curve, auc
)

from load_dataset import load_dataset_from_dir
from label_builder import build_row_labels_and_alignments
from feature_builder import extract_features_from_prediction, FEATURE_COLUMNS


# Data sufficiency gates
MIN_TOTAL_ROWS = 200
MIN_CLASS_ROWS = 50


def train_extraction_risk_model(
    dataset_dir: str,
    output_artifacts_dir: str,
    force_experimental: bool = False
) -> Dict[str, Any]:
    """
    Executes training workflow over approved Golden Dataset records.
    """
    os.makedirs(output_artifacts_dir, exist_ok=True)
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

    total_rows = len(all_labels)
    class_0_count = sum(1 for y in all_labels if y == 0)
    class_1_count = sum(1 for y in all_labels if y == 1)
    unique_docs = len(set(all_groups))

    gate_passed = (total_rows >= MIN_TOTAL_ROWS and class_0_count >= MIN_CLASS_ROWS and class_1_count >= MIN_CLASS_ROWS)

    report: Dict[str, Any] = {
        "dataset_summary": {
            "total_extracted_rows": total_rows,
            "class_0_low_risk": class_0_count,
            "class_1_needs_review": class_1_count,
            "unique_source_documents": unique_docs,
            "min_required_rows": MIN_TOTAL_ROWS,
            "min_required_per_class": MIN_CLASS_ROWS,
        },
        "sufficiency_status": "DATA_GATES_PASSED" if gate_passed else "INSUFFICIENT_DATA",
    }

    if not gate_passed and not force_experimental:
        report["message"] = (
            "Insufficient approved ground-truth data to train a production-grade model. "
            f"Found {total_rows} rows (minimum required: {MIN_TOTAL_ROWS}). "
            "Model training stopped safely to prevent unreliable predictions."
        )
        report_file = os.path.join(output_artifacts_dir, "training_report.json")
        with open(report_file, "w", encoding="utf-8") as f:
            json.dump(report, f, indent=2)
        return report

    # Prepare tabular data
    X = all_features
    y = np.array(all_labels)
    groups = np.array(all_groups)

    # Preprocessing
    cat_cols = ["document_type", "ocr_engine", "parser_version"]
    num_cols = [c for c in FEATURE_COLUMNS if c not in cat_cols]

    preprocessor = ColumnTransformer(
        transformers=[
            ("num", StandardScaler(), num_cols),
            ("cat", OneHotEncoder(handle_unknown="ignore"), cat_cols),
        ]
    )

    # Leakage-safe GroupKFold holdout (grouped by source document)
    n_splits = min(3, max(2, unique_docs))
    gkf = GroupKFold(n_splits=n_splits)
    train_idx, test_idx = next(gkf.split(X, y, groups=groups))

    X_train = [X[i] for i in train_idx]
    y_train = y[train_idx]
    X_test = [X[i] for i in test_idx]
    y_test = y[test_idx]

    # Convert to structured record matrices
    def to_matrix(rows: List[Dict[str, Any]]):
        return [[r[c] for c in FEATURE_COLUMNS] for r in rows]

    X_train_mat = to_matrix(X_train)
    X_test_mat = to_matrix(X_test)

    # 1. Baseline: Dummy Classifier
    dummy = DummyClassifier(strategy="most_frequent")
    dummy.fit(X_train_mat, y_train)
    dummy_pred = dummy.predict(X_test_mat)

    # 2. Candidate: Logistic Regression
    lr_pipe = Pipeline([
        ("prep", ColumnTransformer(
            transformers=[
                ("num", StandardScaler(), [FEATURE_COLUMNS.index(c) for c in num_cols]),
                ("cat", OneHotEncoder(handle_unknown="ignore"), [FEATURE_COLUMNS.index(c) for c in cat_cols])
            ]
        )),
        ("clf", LogisticRegression(class_weight="balanced", max_iter=500, random_state=42))
    ])
    lr_pipe.fit(X_train_mat, y_train)
    lr_pred = lr_pipe.predict(X_test_mat)

    # 3. Candidate: Random Forest Classifier
    rf_pipe = Pipeline([
        ("prep", ColumnTransformer(
            transformers=[
                ("num", StandardScaler(), [FEATURE_COLUMNS.index(c) for c in num_cols]),
                ("cat", OneHotEncoder(handle_unknown="ignore"), [FEATURE_COLUMNS.index(c) for c in cat_cols])
            ]
        )),
        ("clf", RandomForestClassifier(class_weight="balanced", n_estimators=100, random_state=42))
    ])
    rf_pipe.fit(X_train_mat, y_train)
    rf_pred = rf_pipe.predict(X_test_mat)

    # Evaluation on locked test partition
    def compute_metrics(y_true, y_pred):
        cm = confusion_matrix(y_true, y_pred, labels=[0, 1]).tolist()
        return {
            "precision_needs_review": float(precision_score(y_true, y_pred, pos_label=1, zero_division=0)),
            "recall_needs_review": float(recall_score(y_true, y_pred, pos_label=1, zero_division=0)),
            "f1_needs_review": float(f1_score(y_true, y_pred, pos_label=1, zero_division=0)),
            "balanced_accuracy": float(balanced_accuracy_score(y_true, y_pred)),
            "confusion_matrix": cm,
        }

    report["evaluation_on_held_out_test_groups"] = {
        "dummy_baseline": compute_metrics(y_test, dummy_pred),
        "logistic_regression": compute_metrics(y_test, lr_pred),
        "random_forest": compute_metrics(y_test, rf_pred),
    }

    # Best model selection based on Recall for NEEDS_REVIEW (catching extraction errors)
    rf_recall = report["evaluation_on_held_out_test_groups"]["random_forest"]["recall_needs_review"]
    lr_recall = report["evaluation_on_held_out_test_groups"]["logistic_regression"]["recall_needs_review"]

    selected_model_name = "random_forest" if rf_recall >= lr_recall else "logistic_regression"
    selected_pipeline = rf_pipe if selected_model_name == "random_forest" else lr_pipe

    report["selected_model"] = selected_model_name
    report["model_version"] = "1.0.0-experimental" if force_experimental else "1.0.0-candidate"

    # Save artifact
    artifact_path = os.path.join(output_artifacts_dir, "graniteflow_extraction_risk_model.joblib")
    joblib.dump(selected_pipeline, artifact_path)
    report["artifact_path"] = artifact_path

    report_file = os.path.join(output_artifacts_dir, "training_report.json")
    with open(report_file, "w", encoding="utf-8") as f:
        json.dump(report, f, indent=2)

    return report


if __name__ == "__main__":
    base_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
    dataset_folder = os.path.join(base_dir, "data")
    artifacts_folder = os.path.join(base_dir, "artifacts")

    res = train_extraction_risk_model(dataset_folder, artifacts_folder, force_experimental=False)
    print(json.dumps(res, indent=2))
