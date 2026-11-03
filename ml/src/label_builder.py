"""
Target label generator for GraniteFlow Extraction Risk Model.
Builds binary target: 1 = NEEDS_REVIEW, 0 = LOW_OBSERVED_RISK.
Excludes unreviewed or ambiguous records to prevent label corruption.
"""

from typing import Dict, Any, List, Tuple, Optional


def build_row_labels_and_alignments(
    record: Dict[str, Any]
) -> List[Tuple[Dict[str, Any], int, str]]:
    """
    Aligns pre-review prediction blocks with human-approved ground truth blocks.
    Returns: List of tuples (predicted_block_dict, target_label, source_document_id)
    target_label: 1 for NEEDS_REVIEW, 0 for LOW_OBSERVED_RISK.
    """
    gt = record.get("ground_truth", {})
    pred = record.get("prediction", {})
    doc_id = record.get("source_document_id", "DOC_UNKNOWN")

    # Gate: Exclude unapproved or rejected ground truth records from supervised training
    if not gt.get("isApproved", False):
        return []

    gt_blocks = gt.get("blocks", [])
    pred_blocks = pred.get("blocks", [])

    if not pred_blocks:
        return []

    results = []
    used_gt_indices = set()

    for p_block in pred_blocks:
        p_num = str(p_block.get("blockNumber", "")).strip()
        p_len = float(p_block.get("grossLengthCm", 0.0))
        p_hgt = float(p_block.get("grossHeightCm", 0.0))
        p_wdt = float(p_block.get("grossWidthCm", 0.0))
        p_cbm = float(p_block.get("netCbm", 0.0))

        # Check physical dimension plausibility before alignment
        if p_len <= 0 or p_hgt <= 0 or p_wdt <= 0:
            results.append((p_block, 1, doc_id))
            continue

        # Find matching ground truth block
        matched_gt = None
        matched_gt_idx = -1

        for idx, g_block in enumerate(gt_blocks):
            if idx in used_gt_indices:
                continue
            g_num = str(g_block.get("blockNumber", "")).strip()
            if p_num and g_num and p_num.lower() == g_num.lower():
                matched_gt = g_block
                matched_gt_idx = idx
                break

        # Fallback dimensional alignment if block number was misread
        if matched_gt is None:
            min_diff = float("inf")
            for idx, g_block in enumerate(gt_blocks):
                if idx in used_gt_indices:
                    continue
                g_len = float(g_block.get("grossLengthCm", 0.0))
                g_hgt = float(g_block.get("grossHeightCm", 0.0))
                g_wdt = float(g_block.get("grossWidthCm", 0.0))
                diff = abs(p_len - g_len) + abs(p_hgt - g_hgt) + abs(p_wdt - g_wdt)
                if diff <= 25.0 and diff < min_diff:
                    min_diff = diff
                    matched_gt = g_block
                    matched_gt_idx = idx

        if matched_gt is None:
            # Extra or completely unaligned predicted block -> NEEDS_REVIEW (1)
            results.append((p_block, 1, doc_id))
        else:
            used_gt_indices.add(matched_gt_idx)
            g_num = str(matched_gt.get("blockNumber", "")).strip()
            g_len = float(matched_gt.get("grossLengthCm", 0.0))
            g_hgt = float(matched_gt.get("grossHeightCm", 0.0))
            g_wdt = float(matched_gt.get("grossWidthCm", 0.0))
            g_cbm = float(matched_gt.get("netCbm", 0.0))

            num_match = (p_num.lower() == g_num.lower())
            dim_match = (abs(p_len - g_len) == 0 and abs(p_hgt - g_hgt) == 0 and abs(p_wdt - g_wdt) == 0)
            cbm_match = (abs(p_cbm - g_cbm) <= 0.005)

            if num_match and dim_match and cbm_match:
                # Prediction cleanly matches approved truth -> LOW_OBSERVED_RISK (0)
                results.append((p_block, 0, doc_id))
            else:
                # Discrepancy detected -> NEEDS_REVIEW (1)
                results.append((p_block, 1, doc_id))

    return results
