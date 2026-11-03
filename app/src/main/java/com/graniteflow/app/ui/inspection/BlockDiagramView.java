package com.graniteflow.app.ui.inspection;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;

import com.graniteflow.app.R;

public class BlockDiagramView extends View {

    private double targetLength = 0;
    private double targetHeight = 0;
    private double targetWidth = 0;

    private float animLength = 0f;
    private float animHeight = 0f;
    private float animWidth = 0f;
    private float vectorAlpha = 0f;

    private ValueAnimator dimensionAnimator;

    private Paint linePaint;
    private Paint fillPaint;
    private Paint textPaint;
    private Paint titlePaint;
    private Paint emptyBorderPaint;
    private Paint emptyTextPaint;

    public BlockDiagramView(Context context) {
        super(context);
        init();
    }

    public BlockDiagramView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BlockDiagramView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private float sp(float spVal) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, spVal, getResources().getDisplayMetrics());
    }

    private float dp(float dpVal) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpVal, getResources().getDisplayMetrics());
    }

    private void init() {
        int textColor = getContext().getColor(R.color.text_primary);
        int secondaryColor = getContext().getColor(R.color.text_secondary);
        int borderColor = getContext().getColor(R.color.card_border);
        int cardColor = getContext().getColor(R.color.card_surface);
        int accent = getContext().getColor(R.color.brand_blue);

        Typeface mediumTf = Typeface.create("sans-serif-medium", Typeface.NORMAL);
        Typeface regularTf = Typeface.create("sans-serif", Typeface.NORMAL);

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(textColor);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(dp(1.6f));
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setStrokeCap(Paint.Cap.ROUND);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setColor(cardColor);
        fillPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(textColor);
        textPaint.setTextSize(sp(9.5f));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(mediumTf);

        titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(accent);
        titlePaint.setTextSize(sp(9.5f));
        titlePaint.setTextAlign(Paint.Align.CENTER);
        titlePaint.setTypeface(mediumTf);
        titlePaint.setLetterSpacing(0.06f);

        emptyBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        emptyBorderPaint.setColor(borderColor);
        emptyBorderPaint.setStyle(Paint.Style.STROKE);
        emptyBorderPaint.setStrokeWidth(dp(1.2f));
        emptyBorderPaint.setPathEffect(new DashPathEffect(new float[]{dp(5f), dp(4f)}, 0));

        emptyTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        emptyTextPaint.setColor(secondaryColor);
        emptyTextPaint.setTextSize(sp(11f));
        emptyTextPaint.setTextAlign(Paint.Align.CENTER);
        emptyTextPaint.setTypeface(regularTf);
    }

    public void setDimensions(double length, double height, double width) {
        boolean wasValid = targetLength > 0 && targetHeight > 0 && targetWidth > 0;
        boolean isNowValid = length > 0 && height > 0 && width > 0;

        this.targetLength = Math.max(0, length);
        this.targetHeight = Math.max(0, height);
        this.targetWidth = Math.max(0, width);

        if (dimensionAnimator != null && dimensionAnimator.isRunning()) {
            dimensionAnimator.cancel();
        }

        if (!isNowValid) {
            animLength = 0f;
            animHeight = 0f;
            animWidth = 0f;
            vectorAlpha = 0f;
            invalidate();
            return;
        }

        final float startL = wasValid ? animLength : (float) targetLength * 0.85f;
        final float startH = wasValid ? animHeight : (float) targetHeight * 0.85f;
        final float startW = wasValid ? animWidth : (float) targetWidth * 0.85f;
        final float startAlpha = wasValid ? vectorAlpha : 0f;

        dimensionAnimator = ValueAnimator.ofFloat(0f, 1f);
        dimensionAnimator.setDuration(200);
        dimensionAnimator.setInterpolator(new DecelerateInterpolator());
        dimensionAnimator.addUpdateListener(anim -> {
            float f = (float) anim.getAnimatedValue();
            animLength = startL + ((float) targetLength - startL) * f;
            animHeight = startH + ((float) targetHeight - startH) * f;
            animWidth = startW + ((float) targetWidth - startW) * f;
            vectorAlpha = startAlpha + (1f - startAlpha) * f;
            invalidate();
        });
        dimensionAnimator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        boolean isValid = targetLength > 0 && targetHeight > 0 && targetWidth > 0;
        if (!isValid || vectorAlpha <= 0.01f) {
            drawEmptyPlaceholder(canvas, w, h);
            return;
        }

        int alphaInt = Math.min(255, Math.max(0, (int) (vectorAlpha * 255)));
        linePaint.setAlpha(alphaInt);
        textPaint.setAlpha(alphaInt);
        titlePaint.setAlpha(alphaInt);

        float midX = w / 2f;
        float topTitleY = dp(14f);

        canvas.drawText("VIEW A", midX * 0.5f, topTitleY, titlePaint);
        canvas.drawText("VIEW B", midX * 1.5f, topTitleY, titlePaint);

        // Compute proportional dimensions matching ObliqueBlockViews.tsx
        float maxDim = Math.max(animLength, Math.max(animHeight, animWidth));
        if (maxDim <= 0) maxDim = 1f;

        float maxBoxSize = Math.min(w * 0.24f, (h - dp(38f)) * 0.62f);
        float scale = maxBoxSize / maxDim;

        // Clamp minimum visual size so very thin dimensions remain legible
        float fw = Math.max(dp(18f), animWidth * scale);
        float fh = Math.max(dp(18f), animHeight * scale);
        float depth = Math.max(dp(16f), animLength * scale);

        float centerY = topTitleY + (h - topTitleY) * 0.54f;

        // Draw View A (Left projection: front = W x H, depth = L receding left-up)
        drawObliqueBlock(canvas, midX * 0.5f, centerY, fw, fh, depth, true);

        // Draw View B (Right projection: front = W x H, depth = L receding right-up)
        drawObliqueBlock(canvas, midX * 1.5f, centerY, fw, fh, depth, false);
    }

    private void drawEmptyPlaceholder(Canvas canvas, int w, int h) {
        float padH = dp(8f);
        float padV = dp(6f);
        RectF rect = new RectF(padH, padV, w - padH, h - padV);
        canvas.drawRoundRect(rect, dp(10f), dp(10f), emptyBorderPaint);

        String msg = "Enter Length, Height & Width to generate 3D block vector";
        Paint.FontMetrics fm = emptyTextPaint.getFontMetrics();
        float textY = (h / 2f) - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(msg, w / 2f, textY, emptyTextPaint);
    }

    private void drawObliqueBlock(Canvas canvas, float cx, float cy, float fw, float fh, float depth, boolean projectLeft) {
        float ox = projectLeft ? -depth * 0.45f : depth * 0.45f;
        float oy = -depth * 0.32f; // receding upwards

        // Center the combined bounding box around (cx, cy)
        float totalW = fw + Math.abs(ox);
        float totalH = fh + Math.abs(oy);

        float startX = cx - (totalW / 2f) + (projectLeft ? Math.abs(ox) : 0f);
        float startY = cy - (totalH / 2f) + Math.abs(oy);

        // Front rectangle corners
        float fTLx = startX;
        float fTLy = startY;
        float fTRx = startX + fw;
        float fTRy = startY;
        float fBRx = startX + fw;
        float fBRy = startY + fh;
        float fBLx = startX;
        float fBLy = startY + fh;

        // Back face shifted by (ox, oy)
        float bTLx = fTLx + ox;
        float bTLy = fTLy + oy;
        float bTRx = fTRx + ox;
        float bTRy = fTRy + oy;
        float bBRx = fBRx + ox;
        float bBRy = fBRy + oy;
        float bBLx = fBLx + ox;
        float bBLy = fBLy + oy;

        // Top Face
        Path topFace = new Path();
        topFace.moveTo(fTLx, fTLy);
        topFace.lineTo(fTRx, fTRy);
        topFace.lineTo(bTRx, bTRy);
        topFace.lineTo(bTLx, bTLy);
        topFace.close();
        canvas.drawPath(topFace, fillPaint);
        canvas.drawPath(topFace, linePaint);

        // Side Face
        Path sideFace = new Path();
        if (projectLeft) {
            sideFace.moveTo(fTLx, fTLy);
            sideFace.lineTo(fBLx, fBLy);
            sideFace.lineTo(bBLx, bBLy);
            sideFace.lineTo(bTLx, bTLy);
        } else {
            sideFace.moveTo(fTRx, fTRy);
            sideFace.lineTo(fBRx, fBRy);
            sideFace.lineTo(bBRx, bBRy);
            sideFace.lineTo(bTRx, bTRy);
        }
        sideFace.close();
        canvas.drawPath(sideFace, fillPaint);
        canvas.drawPath(sideFace, linePaint);

        // Front Face
        Path frontFace = new Path();
        frontFace.moveTo(fTLx, fTLy);
        frontFace.lineTo(fTRx, fTRy);
        frontFace.lineTo(fBRx, fBRy);
        frontFace.lineTo(fBLx, fBLy);
        frontFace.close();
        canvas.drawPath(frontFace, fillPaint);
        canvas.drawPath(frontFace, linePaint);

        // Dimension labels
        String lStr = "L " + Math.round(targetLength);
        String hStr = "H " + Math.round(targetHeight);
        String wStr = "W " + Math.round(targetWidth);

        // Bottom width label
        canvas.drawText(wStr, (fBLx + fBRx) / 2f, fBLy + dp(12f), textPaint);

        // Side height label
        if (projectLeft) {
            textPaint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(hStr, fTRx + dp(5f), (fTRy + fBRy) / 2f + dp(3f), textPaint);
        } else {
            textPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(hStr, fTLx - dp(5f), (fTLy + fBLy) / 2f + dp(3f), textPaint);
        }

        // Receding length label
        textPaint.setTextAlign(Paint.Align.CENTER);
        if (projectLeft) {
            canvas.drawText(lStr, (fTLx + bTLx) / 2f - dp(10f), (fTLy + bTLy) / 2f - dp(4f), textPaint);
        } else {
            canvas.drawText(lStr, (fTLx + bTLx) / 2f - dp(6f), (fTLy + bTLy) / 2f - dp(4f), textPaint);
        }
    }
}
