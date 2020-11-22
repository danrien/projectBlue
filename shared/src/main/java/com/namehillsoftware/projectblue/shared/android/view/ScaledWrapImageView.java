package com.namehillsoftware.projectblue.shared.android.view;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

public class ScaledWrapImageView extends AppCompatImageView {

    private boolean isLandscape;
    private Bitmap bitmap;

    public ScaledWrapImageView(Context context) {
        super(context);

        updateIsLandscape();
    }

    public ScaledWrapImageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        updateIsLandscape();
    }

    public ScaledWrapImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        updateIsLandscape();
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        bitmap = bm;
        super.setImageBitmap(bm);
    }

	@Override
    protected void onConfigurationChanged(Configuration newConfig) {
        updateIsLandscape(newConfig);
        super.onConfigurationChanged(newConfig);
    }

    private void updateIsLandscape() {
        updateIsLandscape(getResources().getConfiguration());
    }

    private void updateIsLandscape(Configuration configuration) {
        isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (bitmap == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        setScaleType(ScaleType.FIT_XY);

        int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        int height = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);

        if (isLandscape) {
            final int newHeight = scaleInteger(bitmap.getHeight(), (double) width / (double) bitmap.getWidth());

            if (newHeight > height) {
                width = scaleInteger(width, (double) height / (double) newHeight);
            } else {
                height = newHeight;
            }
        } else {
            final int newWidth = scaleInteger(bitmap.getWidth(), (double) height / (double) bitmap.getHeight());

            if (newWidth > width) {
                height = scaleInteger(height, (double) width / (double) newWidth );
            } else {
                width = newWidth;
            }
        }

        setMeasuredDimension(width, height);
    }

    private static int scaleInteger(int srcInt, double scaleRatio) {
        return (int) Math.round((double) srcInt * scaleRatio);
    }
}
