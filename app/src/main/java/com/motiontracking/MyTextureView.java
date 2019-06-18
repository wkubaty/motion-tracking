package com.motiontracking;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;

public class MyTextureView extends TextureView {

    public MyTextureView(final Context context) {
        this(context, null);
    }

    public MyTextureView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MyTextureView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }


    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int width = MeasureSpec.getSize(widthMeasureSpec);
        final int height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width, height);
    }
}
