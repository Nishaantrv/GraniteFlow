@echo off
title Test GraniteFlow Python OCR Model
cd /d "%~dp0"
call .venv\Scripts\activate.bat
python ml\src\quarry_ocr_model.py --image app\src\main\assets\real_photographed_slip.png
pause
