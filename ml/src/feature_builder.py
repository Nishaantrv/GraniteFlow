"""
Pre-review feature extractor for GraniteFlow Extraction Risk Model.
Extracts input features strictly from OCR/parser predictions BEFORE human review.
Guarantees NO target leakage from ground truth or post-review edits.
"""

import re
from typing import Dict, Any


FEATURE_COLUMNS = [
    "document_type",
    "ocr_engine",
    "parser_version",
    "raw_text_length",
    "token_count",
    "parsed_dimensions_count",
    "allowance_value",
    "has_block_number_pattern",
    "is_cbm_formula_consistent",
    "cbm_reported_vs_calc_diff",
    "has_zero_or_negative_dim",
    "has_excessive_allowance",
]


def extract_features_from_prediction(
    predicted_block: Dict[str, Any],
    document_type: str = "QUARRY_MEASUREMENT_SLIP",
    ocr_engine: str = "Google ML Kit",
    parser_version: str = "v1.0.0"
) -> Dict[str, Any]:
    """
    Constructs a feature dictionary from an unreviewed predicted block.
    """
    raw_text = str(predicted_block.get("rawRowText", ""))
    block_num = str(predicted_block.get("blockNumber", "")).strip()

    gross_l = float(predicted_block.get("grossLengthCm", 0.0))
    gross_h = float(predicted_block.get("grossHeightCm", 0.0))
    gross_w = float(predicted_block.get("grossWidthCm", 0.0))
    allowance = float(predicted_block.get("allowanceCm", 10.0))
    reported_cbm = float(predicted_block.get("netCbm", 0.0))

    # Token and text statistics
    raw_len = len(raw_text) if raw_text else 20.0
    token_count = len(raw_text.split()) if raw_text else 5

    # Parsed dimension count (how many are > 0)
    parsed_dims_count = sum(1 for d in [gross_l, gross_h, gross_w] if d > 0)

    # Block number regex pattern (digits or alphanumeric like 301, B-402, G-1401)
    has_valid_num_pattern = 1 if re.match(r"^[A-Za-z0-9\-_]{2,10}$", block_num) else 0

    # Net dimension calculation and formula consistency check
    net_l = max(0.0, gross_l - allowance)
    net_h = max(0.0, gross_h - allowance)
    net_w = max(0.0, gross_w - allowance)

    calculated_cbm = round(((net_l * net_h * net_w) / 1_000_000.0), 3)
    cbm_diff = abs(reported_cbm - calculated_cbm)
    is_formula_consistent = 1 if (cbm_diff <= 0.005 and reported_cbm > 0) else 0

    # Dimension plausibility flags
    has_zero_or_neg = 1 if (gross_l <= 0 or gross_h <= 0 or gross_w <= 0) else 0
    has_excessive_allow = 1 if (allowance >= gross_l or allowance >= gross_h or allowance >= gross_w) else 0

    return {
        "document_type": document_type if document_type else "UNKNOWN",
        "ocr_engine": ocr_engine if ocr_engine else "UNKNOWN",
        "parser_version": parser_version if parser_version else "v1.0.0",
        "raw_text_length": float(raw_len),
        "token_count": int(token_count),
        "parsed_dimensions_count": int(parsed_dims_count),
        "allowance_value": float(allowance),
        "has_block_number_pattern": int(has_valid_num_pattern),
        "is_cbm_formula_consistent": int(is_formula_consistent),
        "cbm_reported_vs_calc_diff": float(cbm_diff),
        "has_zero_or_negative_dim": int(has_zero_or_neg),
        "has_excessive_allowance": int(has_excessive_allow),
    }
