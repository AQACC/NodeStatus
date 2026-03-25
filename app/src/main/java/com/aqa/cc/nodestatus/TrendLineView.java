package com.aqa.cc.nodestatus;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class TrendLineView extends View {
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float[] points = new float[0];

    public TrendLineView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        gridPaint.setColor(Color.parseColor("#E2E8F0"));
        gridPaint.setStrokeWidth(2f);

        linePaint.setColor(Color.parseColor("#0EA5E9"));
        linePaint.setStrokeWidth(5f);
        linePaint.setStyle(Paint.Style.STROKE);

        fillPaint.setColor(Color.parseColor("#220EA5E9"));
        fillPaint.setStyle(Paint.Style.FILL);
    }

    public void setSeries(float[] series) {
        this.points = series == null ? new float[0] : series.clone();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        canvas.drawLine(0, height, width, height, gridPaint);
        canvas.drawLine(0, height / 2f, width, height / 2f, gridPaint);

        if (points.length == 0) {
            return;
        }

        float max = points[0];
        float min = points[0];
        for (float point : points) {
            max = Math.max(max, point);
            min = Math.min(min, point);
        }
        float range = Math.max(1f, max - min);

        Path linePath = new Path();
        Path fillPath = new Path();
        for (int index = 0; index < points.length; index++) {
            float x = points.length == 1 ? width / 2f : (width * index) / (points.length - 1f);
            float normalized = (points[index] - min) / range;
            float y = height - (normalized * (height - 8f)) - 4f;
            if (index == 0) {
                linePath.moveTo(x, y);
                fillPath.moveTo(x, height);
                fillPath.lineTo(x, y);
            } else {
                linePath.lineTo(x, y);
                fillPath.lineTo(x, y);
            }
        }
        fillPath.lineTo(width, height);
        fillPath.close();

        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(linePath, linePaint);
    }
}
