package com.dgmltn.slider;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;

import com.dgmltn.slider.internal.AbsSlider;

/**
 * Created by doug on 11/1/15.
 */
public class HorizontalSlider extends AbsSlider {

	private int mLeftX;
	private int mRightX;
	private int mY;

	public HorizontalSlider(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);

		mLeftX = getPaddingLeft();
		mRightX = w - getPaddingRight();
		mY = (getPaddingTop() + h - getPaddingBottom()) / 2;
	}

	@Override
	protected void drawConnectingLine(Canvas canvas, float from, float to, Paint paint) {
		canvas.drawLine(getXOnBar(from), mY, getXOnBar(to), mY, paint);
	}

	@Override
	public float getNearestBarValue(float x, float y) {
		x = Math.min(mRightX, Math.max(mLeftX, x));
		return (x - mLeftX) / (mRightX - mLeftX) * max;
	}

	@Override
	protected void getPointOnBar(PointF out, float value) {
		out.set(getXOnBar(value), mY);
	}

	private float getXOnBar(float value) {
		return mLeftX + value * (mRightX - mLeftX) / max;
	}

	@Override
	protected void drawBar(Canvas canvas, Paint paint) {
		canvas.drawLine(mLeftX, mY, mRightX, mY, paint);
	}
}
