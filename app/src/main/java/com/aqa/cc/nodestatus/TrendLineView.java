package com.aqa.cc.nodestatus;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.core.content.ContextCompat;

import com.google.android.material.color.MaterialColors;

public class TrendLineView extends View {
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path linePath = new Path();
    private final Path fillPath = new Path();
    private float[] points = new float[0];

    public TrendLineView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        int gridColor = MaterialColors.getColor(
                context,
                com.google.android.material.R.attr.colorOutlineVariant,
                ContextCompat.getColor(context, R.color.mist_200)
        );
        int lineColor = MaterialColors.getColor(
                context,
                androidx.appcompat.R.attr.colorPrimary,
                ContextCompat.getColor(context, R.color.teal_500)
        );
        gridPaint.setColor(gridColor);
        gridPaint.setStrokeWidth(2f);

        linePaint.setColor(lineColor);
        linePaint.setStrokeWidth(5f);
        linePaint.setStyle(Paint.Style.STROKE);

        fillPaint.setColor(ColorUtils.setAlphaComponent(lineColor, 56));
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

        linePath.reset();
        fillPath.reset();
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
