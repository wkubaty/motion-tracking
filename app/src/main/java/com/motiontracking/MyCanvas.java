package com.motiontracking;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

public class MyCanvas extends View {
    private DrawCallback drawCallback;

    public MyCanvas(Context context) {
        super(context);
    }

    public MyCanvas(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setDrawCallback(DrawCallback drawCallback) {
        this.drawCallback = drawCallback;
    }

    @Override
    public synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawCallback.draw(canvas);
    }

    public interface DrawCallback {
        void draw(final Canvas canvas);
    }
}
