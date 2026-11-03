"""
Model Registry and Artifact Lineage Manager for GraniteFlow Extraction Risk Model.
Verifies artifact presence, inspects metadata, checks schema compatibility,
and manages active production / candidate models.
"""

import json
import os
from typing import Dict, Any, Optional
import joblib


class ModelRegistry:
    """
    Manages versioning, lineage, and retrieval of trained GraniteFlow risk models.
    """

    def __init__(self, artifacts_dir: str):
        self.artifacts_dir = artifacts_dir
        self.report_path = os.path.join(artifacts_dir, "training_report.json")
        self.artifact_path = os.path.join(artifacts_dir, "graniteflow_extraction_risk_model.joblib")

    def has_registered_model(self) -> bool:
        """
        Checks if a trained model artifact and its training report exist.
        """
        return os.path.exists(self.report_path) and os.path.exists(self.artifact_path)

    def get_metadata(self) -> Dict[str, Any]:
        """
        Loads the training metadata report if available.
        """
        if not os.path.exists(self.report_path):
            return {
                "status": "UNREGISTERED",
                "message": "No training report found in artifacts directory."
            }

        with open(self.report_path, "r", encoding="utf-8") as f:
            return json.load(f)

    def load_active_pipeline(self) -> Optional[Any]:
        """
        Deserializes the active scikit-learn pipeline if present.
        """
        if not os.path.exists(self.artifact_path):
            return None
        return joblib.load(self.artifact_path)

    def verify_schema_compatibility(self, schema_file: str) -> bool:
        """
        Verifies that feature columns in schema match the required pre-review features.
        """
        if not os.path.exists(schema_file):
            return False

        with open(schema_file, "r", encoding="utf-8") as f:
            schema = json.load(f)

        properties = schema.get("properties", {})
        from feature_builder import FEATURE_COLUMNS
        for col in FEATURE_COLUMNS:
            if col not in properties:
                return False
        return True


if __name__ == "__main__":
    base = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
    reg = ModelRegistry(os.path.join(base, "artifacts"))
    meta = reg.get_metadata()
    print("Registry Metadata:")
    print(json.dumps(meta, indent=2))
