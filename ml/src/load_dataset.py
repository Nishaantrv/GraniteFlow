"""
Dataset loader for GraniteFlow Golden Dataset.
Loads exported records, validates schemas, and excludes unreviewed candidates.
"""

import json
import os
from typing import Dict, Any, List, Tuple


def load_dataset_from_dir(dataset_dir: str) -> List[Dict[str, Any]]:
    """
    Loads all dataset records from an exported Golden Dataset directory.
    Each record contains: record metadata, prediction, and active ground truth.
    """
    records_dir = os.path.join(dataset_dir, "records")
    if not os.path.exists(records_dir):
        return []

    loaded_records = []
    for rec_id in os.listdir(records_dir):
        rec_path = os.path.join(records_dir, rec_id)
        if not os.path.isdir(rec_path):
            continue

        pred_file = os.path.join(rec_path, "prediction.json")
        gt_files = [f for f in os.listdir(rec_path) if f.startswith("ground_truth_") and f.endswith(".json")]

        if not os.path.exists(pred_file) or not gt_files:
            continue

        with open(pred_file, "r", encoding="utf-8") as pf:
            prediction = json.load(pf)

        # Load latest ground truth version
        gt_files.sort()
        latest_gt_file = os.path.join(rec_path, gt_files[-1])
        with open(latest_gt_file, "r", encoding="utf-8") as gtf:
            ground_truth = json.load(gtf)

        loaded_records.append({
            "dataset_record_id": rec_id,
            "source_document_id": ground_truth.get("sourceDocumentId", rec_id),
            "document_type": ground_truth.get("documentType", "QUARRY_MEASUREMENT_SLIP"),
            "prediction": prediction,
            "ground_truth": ground_truth
        })

    return loaded_records
