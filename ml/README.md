# GraniteFlow — Machine Learning Extraction Risk Model & Golden Dataset

This directory contains the Python ML pipeline for GraniteFlow's supervised extraction-risk classification and OCR quality evaluation.

---

## 1. Problem Formulation

- **Goal**: Predict extraction risk on unreviewed OCR measurement rows.
- **Classes**:
  - `Class 1 (NEEDS_REVIEW)`: High likelihood of extraction error (misread dimensions, incorrect block numbers, or formula inconsistencies).
  - `Class 0 (LOW_OBSERVED_RISK)`: High likelihood of accurate extraction matching human-reviewed standards.
- **Zero Leakage Invariant**: Features are extracted strictly from OCR output and raw layout text **prior** to any reviewer edits or ground truth generation. No ground-truth values, reviewer tags, or correction metadata are permitted in the input feature space.
- **Grouped Holdout Invariant**: Dataset splitting uses `GroupKFold` grouped by `sourceDocumentId` to prevent data leakage between measurement rows from the same quarry slip.

---

## 2. Directory Structure

```
ml/
├── artifacts/                  # Serialized .joblib models and training_report.json
├── data/                       # Local exported Golden Dataset records
│   └── records/
│       └── <record_id>/
│           ├── prediction.json
│           ├── ground_truth_v1.json
│           └── manifest.json
├── schema/
│   └── feature_schema.json     # Feature schema specification (v1.0.0)
├── src/
│   ├── feature_builder.py      # Pre-review feature extraction (12 schema columns)
│   ├── label_builder.py        # Leakage-safe 1-to-1 ground truth alignment
│   ├── load_dataset.py         # Golden Dataset directory reader
│   ├── train_quality_model.py  # Data-sufficiency gates, GroupKFold, baseline comparison
│   ├── evaluate_quality_model.py # Standalone evaluation metrics & confusion matrix
│   ├── predict_quality.py      # Inference predictor with explainable reason codes
│   └── model_registry.py       # Artifact lineage & schema compatibility checks
├── tests/
│   └── test_ml_pipeline.py     # Unit and integration tests
└── requirements.txt            # Python dependencies (scikit-learn, numpy, joblib, pytest)
```

---

## 3. Data Sufficiency Gates

To avoid deploying models trained on unrepresentative or sparse samples:
- **Minimum total extracted rows**: 200
- **Minimum rows per class**: 50 (`Class 0` and `Class 1`)
- If gates fail, the pipeline terminates safely reporting `INSUFFICIENT_DATA` rather than publishing an unverified model. A `--force-experimental` flag is available strictly for development validation.

---

## 4. Pre-Review Feature Schema

| Feature Name | Type | Description |
|---|---|---|
| `document_type` | Categorical | Document category (e.g., `QUARRY_MEASUREMENT_SLIP`) |
| `ocr_engine` | Categorical | OCR Engine identifier (e.g., `Google ML Kit`) |
| `parser_version` | Categorical | Parser rule version (e.g., `v1.0.0`) |
| `raw_text_length` | Numeric | Character length of the OCR snippet |
| `token_count` | Numeric | Number of whitespace-separated tokens |
| `parsed_dimensions_count` | Numeric | Number of dimensions parsed with value > 0 |
| `allowance_value` | Numeric | Allowance deduction in centimetres (default 10.0 cm) |
| `has_block_number_pattern` | Binary | Matches block number pattern `^[A-Za-z0-9\-_]{2,10}$` |
| `is_cbm_formula_consistent` | Binary | `abs(reported_cbm - calc_cbm) <= 0.005` |
| `cbm_reported_vs_calc_diff` | Numeric | Absolute discrepancy between reported and calculated CBM |
| `has_zero_or_negative_dim` | Binary | `1` if any dimension <= 0, else `0` |
| `has_excessive_allowance` | Binary | `1` if allowance >= any dimension, else `0` |

---

## 5. Setup & Usage (Windows / PowerShell)

### Environment Setup
```powershell
pip install -r ml/requirements.txt
```

### Running Tests
```powershell
python -m pytest ml/tests/test_ml_pipeline.py -v
```

### Training the Quality Model
```powershell
python ml/src/train_quality_model.py
```

### Standalone Model Evaluation
```powershell
python ml/src/evaluate_quality_model.py --model ml/artifacts/graniteflow_extraction_risk_model.joblib --dataset ml/data --output ml/artifacts/eval_report.json
```

### Inference Scoring
```powershell
python ml/src/predict_quality.py --input sample_block.json --model ml/artifacts/graniteflow_extraction_risk_model.joblib
```

---

## 6. Android Integration & Safety Contract

- **Offline / Disconnected Guarantee**: When a connected backend or model service is unreachable, Android marks the service status as `NOT_CONNECTED` and falls back to deterministic local rule heuristics.
- **Human Approval Invariant**: The risk model only serves as an advisory triage signal. **It can never bypass human inspection review or auto-approve documents.**
