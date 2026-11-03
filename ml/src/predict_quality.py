"""
Inference module for GraniteFlow Extraction Risk Model.
Scores unreviewed OCR prediction blocks to predict risk category:
NEEDS_REVIEW (1) vs LOW_OBSERVED_RISK (0).
Provides transparent reason codes and pre-review feature extraction.
"""

import argparse
import json
import os
import sys
from typing import Dict, Any, List, Optional
import joblib

from feature_builder import extract_features_from_prediction, FEATURE_COLUMNS


class ExtractionRiskPredictor:
    """
    Inference wrapper for GraniteFlow Extraction Risk model.
    """

    def __init__(self, model_path: Optional[str] = None):
        self.model = None
        self.model_version = "offline-heuristic-fallback"
        self.model_path = model_path

        if model_path and os.path.exists(model_path):
            try:
                self.model = joblib.load(model_path)
                self.model_version = "1.0.0-serialized"
            except Exception as e:
                sys.stderr.write(f"Warning: Failed to load model from {model_path}: {e}\n")
                self.model = None

    def _generate_reason_codes(self, features: Dict[str, Any], risk_prob: float) -> List[str]:
        """
        Derives human-readable reason codes explaining the risk assessment.
        """
        reasons = []

        if features.get("has_zero_or_negative_dim", 0) == 1:
            reasons.append("INVALID_DIMENSIONS")

        if features.get("is_cbm_formula_consistent", 1) == 0:
            reasons.append("MISMATCHED_CBM_FORMULA")

        if features.get("has_excessive_allowance", 0) == 1:
            reasons.append("EXCESSIVE_ALLOWANCE")

        if features.get("has_block_number_pattern", 1) == 0:
            reasons.append("MALFORMED_BLOCK_NUMBER")

        if features.get("parsed_dimensions_count", 3) < 3:
            reasons.append("INCOMPLETE_DIMENSIONS")

        if risk_prob >= 0.70:
            reasons.append("HIGH_MODEL_UNCERTAINTY")
        elif risk_prob >= 0.50:
            reasons.append("MODERATE_MODEL_RISK")

        if not reasons:
            reasons.append("CONSISTENT_PREVIEW_PROFILE")

        return reasons

    def predict_block(
        self,
        predicted_block: Dict[str, Any],
        document_type: str = "QUARRY_MEASUREMENT_SLIP",
        ocr_engine: str = "Google ML Kit",
        parser_version: str = "v1.0.0"
    ) -> Dict[str, Any]:
        """
        Predicts extraction risk for a single block prediction dictionary.
        """
        features = extract_features_from_prediction(
            predicted_block,
            document_type=document_type,
            ocr_engine=ocr_engine,
            parser_version=parser_version
        )

        # Fallback heuristic if no trained model is available
        if self.model is None:
            is_risky = (
                features["has_zero_or_negative_dim"] == 1 or
                features["is_cbm_formula_consistent"] == 0 or
                features["has_excessive_allowance"] == 1 or
                features["has_block_number_pattern"] == 0 or
                features["parsed_dimensions_count"] < 3
            )
            prob = 0.90 if is_risky else 0.10
            pred_class = 1 if is_risky else 0
            reason_codes = self._generate_reason_codes(features, prob)
            if is_risky:
                reason_codes.append("HEURISTIC_RULE_TRIGGERED")

            return {
                "prediction": "NEEDS_REVIEW" if pred_class == 1 else "LOW_OBSERVED_RISK",
                "label": int(pred_class),
                "risk_probability": round(float(prob), 4),
                "model_version": self.model_version,
                "reason_codes": reason_codes,
                "features": features
            }

        # Hard safety guardrail: deterministic physical impossibility rules always force NEEDS_REVIEW
        hard_safety_violation = (
            features["has_zero_or_negative_dim"] == 1 or
            features["has_excessive_allowance"] == 1 or
            features["parsed_dimensions_count"] < 3
        )

        # Model inference
        row_mat = [[features[c] for c in FEATURE_COLUMNS]]
        try:
            pred_arr = self.model.predict(row_mat)
            pred_class = int(pred_arr[0])

            prob = 1.0 if pred_class == 1 else 0.0
            if hasattr(self.model, "predict_proba"):
                probs = self.model.predict_proba(row_mat)
                if probs.shape[1] >= 2:
                    prob = float(probs[0, 1])

            if hard_safety_violation:
                pred_class = 1
                prob = max(prob, 0.95)

            reason_codes = self._generate_reason_codes(features, prob)
            if hard_safety_violation:
                reason_codes.append("HARD_SAFETY_GUARDRAIL_TRIGGERED")

            return {
                "prediction": "NEEDS_REVIEW" if pred_class == 1 else "LOW_OBSERVED_RISK",
                "label": int(pred_class),
                "risk_probability": round(float(prob), 4),
                "model_version": self.model_version,
                "reason_codes": reason_codes,
                "features": features
            }
        except Exception as e:
            # Safe failure fallback: when in doubt, flag for human review
            return {
                "prediction": "NEEDS_REVIEW",
                "label": 1,
                "risk_probability": 1.0,
                "model_version": f"{self.model_version}-error-fallback",
                "reason_codes": ["INFERENCE_EXCEPTION", str(e)],
                "features": features
            }

    def predict_batch(
        self,
        predicted_blocks: List[Dict[str, Any]],
        document_type: str = "QUARRY_MEASUREMENT_SLIP",
        ocr_engine: str = "Google ML Kit",
        parser_version: str = "v1.0.0"
    ) -> List[Dict[str, Any]]:
        """
        Scores multiple blocks sequentially.
        """
        return [
            self.predict_block(b, document_type, ocr_engine, parser_version)
            for b in predicted_blocks
        ]


def main():
    parser = argparse.ArgumentParser(description="Predict extraction risk for GraniteFlow blocks.")
    parser.add_argument("--model", type=str, default=None, help="Path to serialized .joblib model")
    parser.add_argument("--input", type=str, required=True, help="Path to JSON file containing a block or list of blocks")
    parser.add_argument("--output", type=str, default=None, help="Path to output JSON")

    args = parser.parse_args()

    predictor = ExtractionRiskPredictor(args.model)

    with open(args.input, "r", encoding="utf-8") as f:
        data = json.load(f)

    if isinstance(data, list):
        result = predictor.predict_batch(data)
    else:
        result = predictor.predict_block(data)

    if args.output:
        with open(args.output, "w", encoding="utf-8") as f:
            json.dump(result, f, indent=2)
    else:
        print(json.dumps(result, indent=2))


if __name__ == "__main__":
    main()
