#!/usr/bin/env python3
"""
Test continuous learning loop and feedback loss computation.
"""
import os
import sys
import json

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "src"))
from ocr_trainer import OcrTrainer

def test_trainer_feedback():
    trainer = OcrTrainer()

    # Sample predicted (with misread dimensions)
    pred_data = {
        "customer": "DAKSHIN EXPORTS",
        "materialName": "BLACK GALAXY",
        "blocks": [
            {"blockNumber": "101", "grossLength": 280, "grossHeight": 140, "grossWidth": 120, "allowance": 10, "netCbm": 3.861},
            {"blockNumber": "102", "grossLength": 290, "grossHeight": 150, "grossWidth": 130, "allowance": 10, "netCbm": 4.704}
        ]
    }

    # Actual verified values confirmed by inspector
    actual_data = {
        "customer": "DAKSHIN EXPORTS",
        "materialName": "BLACK GALAXY",
        "locationCode": "ONG-QUARRY-04",
        "blocks": [
            {"blockNumber": "101", "grossLength": 285, "grossHeight": 145, "grossWidth": 125, "allowance": 10, "netCbm": 4.269},
            {"blockNumber": "102", "grossLength": 290, "grossHeight": 150, "grossWidth": 130, "allowance": 10, "netCbm": 4.704}
        ]
    }

    sample_slip = os.path.join(os.path.dirname(__file__), "..", "..", "app", "src", "main", "assets", "real_photographed_slip.png")

    result = trainer.process_and_train_feedback(
        image_path=sample_slip,
        predicted_json=pred_data,
        actual_json=actual_data,
        customer_name="DAKSHIN EXPORTS",
        document_type="QUARRY_MEASUREMENT_SLIP"
    )

    print("Trainer Feedback Result:")
    print(json.dumps(result, indent=2))

    assert result["status"] == "success"
    assert "loss" in result
    assert result["total_golden_records"] >= 1
    assert os.path.exists(trainer.weights_path)
    print("\n[SUCCESS] Continuous learning loop successfully tested!")

if __name__ == "__main__":
    test_trainer_feedback()
