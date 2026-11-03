@echo off
title GraniteFlow Python OCR Server
echo ==========================================================
echo Starting GraniteFlow Python Neural OCR & Vision AI Server
echo ==========================================================
cd /d "%~dp0"
call .venv\Scripts\activate.bat
python ml\src\ocr_server.py
pause
