#!/usr/bin/env python3
"""
GraniteFlow - Python OCR & Extraction Risk Server.

Serves endpoints for Android app and web clients:
- POST /api/v1/ocr/extract-inspection : Upload photographed or scanned quarry slip
- POST /api/v1/ocr/evaluate-extraction-risk : Evaluate extraction risk of measurement rows
- GET  /api/v1/ocr/health : System status and active OCR engines
"""

import os
import sys
import tempfile
import uvicorn
from fastapi import FastAPI, File, UploadFile, HTTPException, Form
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Optional

# Ensure ml/src is in sys.path
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from quarry_ocr_model import QuarryOcrModel
from ocr_trainer import OcrTrainer

app = FastAPI(
    title="GraniteFlow Quarry OCR & Inspection Service",
    version="2.0.0",
    description="High-Precision Deep Neural & Vision OCR for Granite Quarry Measurement Slips"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

ocr_model = QuarryOcrModel()
ocr_trainer = OcrTrainer()


class BlockDto(BaseModel):
    blockNumber: str
    grossLength: float
    grossHeight: float
    grossWidth: float
    allowance: float
    netCbm: float
    confidence: float
    mathVerified: Optional[bool] = False
    remarks: Optional[str] = "-"


class ExtractionResponse(BaseModel):
    engine: str
    confidence: float
    customerName: Optional[str] = ""
    material: Optional[str] = "BLACK GALAXY"
    inspector: Optional[str] = "Field Inspector"
    locationCode: Optional[str] = "ONG-QUARRY-04"
    defaultAllowance: float = 10.0
    blocks: List[BlockDto]
    warnings: List[str] = []


class RiskInferenceRequest(BaseModel):
    document_type: Optional[str] = "QUARRY_MEASUREMENT_SLIP"
    ocr_engine: Optional[str] = "GraniteFlow Neural OCR"
    parser_version: Optional[str] = "v2.0.0"
    raw_text_snippet: Optional[str] = ""
    block_number: Optional[str] = ""
    gross_length: float
    gross_height: float
    gross_width: float
    allowance: float = 10.0
    reported_cbm: Optional[float] = 0.0


class RiskInferenceResponse(BaseModel):
    model_version: str = "v1.0.0"
    predicted_class: int
    predicted_label: str
    risk_probability: float
    reason_codes: List[str] = []


@app.get("/")
@app.get("/health")
@app.get("/api/v1/ocr/health")
def health_check():
    return {
        "status": "online",
        "service": "GraniteFlow Quarry OCR Model Server",
        "local_onnx_available": ocr_model.local_engine is not None,
        "gemini_api_configured": bool(ocr_model.gemini_key),
        "version": "2.0.0"
    }


@app.post("/api/v1/ocr/extract-inspection", response_model=ExtractionResponse)
async def extract_inspection(file: UploadFile = File(...)):
    """Receives quarry measurement slip image and runs local neural OCR with Vision AI fallback."""
    suffix = os.path.splitext(file.filename or "")[1] or ".jpg"
    with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
        content = await file.read()
        tmp.write(content)
        tmp_path = tmp.name

    try:
        result = ocr_model.predict(tmp_path)
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"OCR extraction failed: {str(e)}")
    finally:
        if os.path.exists(tmp_path):
            try:
                os.remove(tmp_path)
            except Exception:
                pass


@app.post("/api/v1/ocr/evaluate-extraction-risk", response_model=RiskInferenceResponse)
def evaluate_risk(req: RiskInferenceRequest):
    """Evaluates whether an extracted granite block measurement has high risk of error."""
    reasons = []

    # Check for negative or zero dimensions
    if req.gross_length <= 0 or req.gross_height <= 0 or req.gross_width <= 0:
        reasons.append("NON_POSITIVE_DIMENSIONS")

    # Check for excessive allowance
    if req.allowance >= min(req.gross_length, req.gross_height, req.gross_width):
        reasons.append("EXCESSIVE_ALLOWANCE")

    # Check block dimensions boundary
    if req.gross_length > 800 or req.gross_height > 600 or req.gross_width > 600:
        reasons.append("DIMENSION_EXCEEDS_QUARRY_MAX")
    if req.gross_length < 30 or req.gross_height < 30 or req.gross_width < 30:
        reasons.append("DIMENSION_BELOW_BLOCK_MIN")

    # CBM validation
    net_l = max(0.0, req.gross_length - req.allowance)
    net_h = max(0.0, req.gross_height - req.allowance)
    net_w = max(0.0, req.gross_width - req.allowance)
    expected_cbm = round((net_l * net_h * net_w) / 1000000.0, 3)

    if req.reported_cbm and req.reported_cbm > 0:
        diff = abs(req.reported_cbm - expected_cbm)
        if diff > 0.05:
            reasons.append(f"CBM_MISMATCH_EXPECTED_{expected_cbm}_GOT_{req.reported_cbm}")

    is_high_risk = len(reasons) > 0
    return RiskInferenceResponse(
        model_version="v2.0.0",
        predicted_class=1 if is_high_risk else 0,
        predicted_label="NEEDS_REVIEW" if is_high_risk else "LOW_OBSERVED_RISK",
        risk_probability=0.85 if is_high_risk else 0.05,
        reason_codes=reasons
    )


@app.post("/api/v1/ocr/train-feedback")
async def train_feedback(
    file: Optional[UploadFile] = File(None),
    prediction: str = Form(...),
    actual: str = Form(...),
    customer: Optional[str] = Form(""),
    document_type: Optional[str] = Form("QUARRY_MEASUREMENT_SLIP")
):
    """
    Receives confirmed inspection ground-truth data from the mobile app.
    Calculates discrepancy loss (CTC Loss, Dimensional MAE, Volumetric Loss)
    and continuously updates the local neural OCR recognizer weights.
    """
    try:
        import json
        pred_dict = json.loads(prediction) if isinstance(prediction, str) else prediction
        actual_dict = json.loads(actual) if isinstance(actual, str) else actual
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Invalid JSON in prediction or actual form fields: {e}")

    tmp_path = None
    if file is not None and file.filename:
        suffix = os.path.splitext(file.filename)[1] or ".jpg"
        with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
            content = await file.read()
            tmp.write(content)
            tmp_path = tmp.name
    else:
        # Use fallback default asset image if no file was uploaded
        default_asset = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "artifacts", "sample_quarry_slip.png")
        if not os.path.exists(default_asset):
            # Try app assets
            app_asset = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))), "app", "src", "main", "assets", "real_photographed_slip.png")
            if os.path.exists(app_asset):
                default_asset = app_asset
        tmp_path = default_asset

    try:
        feedback_result = ocr_trainer.process_and_train_feedback(
            image_path=tmp_path or "",
            predicted_json=pred_dict,
            actual_json=actual_dict,
            customer_name=customer,
            document_type=document_type or "QUARRY_MEASUREMENT_SLIP"
        )
        return feedback_result
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Feedback training failed: {str(e)}")
    finally:
        if file is not None and tmp_path and os.path.exists(tmp_path):
            try:
                os.remove(tmp_path)
            except Exception:
                pass


@app.get("/api/v1/ocr/training-status")
def get_training_status():
    """Returns continuous learning metrics and total Golden Dataset records."""
    history = []
    if os.path.exists(ocr_trainer.history_path):
        try:
            import json
            with open(ocr_trainer.history_path, "r", encoding="utf-8") as f:
                history = json.load(f)
        except Exception:
            pass

    return {
        "status": "online",
        "has_weights": os.path.exists(ocr_trainer.weights_path),
        "weights_path": ocr_trainer.weights_path,
        "total_training_sessions": len(history),
        "latest_training_session": history[-1] if history else None,
        "total_golden_records": len([d for d in os.listdir(ocr_trainer.records_dir) if os.path.isdir(os.path.join(ocr_trainer.records_dir, d))])
    }


if __name__ == "__main__":
    port = int(os.environ.get("PORT", 8080))
    print(f"Starting GraniteFlow OCR Server on 0.0.0.0:{port}...")
    uvicorn.run(app, host="0.0.0.0", port=port)
