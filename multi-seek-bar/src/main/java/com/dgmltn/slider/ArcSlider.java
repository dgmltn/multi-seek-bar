package com.dgmltn.slider;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;

import com.dgmltn.slider.internal.AbsSlider;
import com.dgmltn.slider.internal.ArcUtils;

/**
 * Created by doug on 11/2/15.
 */
public class ArcSlider extends AbsSlider {

	private PointF mCenter = new PointF();
	private float mRadius = 1f;
	private RectF mBounds = new RectF();
	private PointF mTmpPointF = new PointF();

	private int mArcStart = 150;
	private int mArcSweep = 240;

	public ArcSlider(Context context, AttributeSet attrs) {
		super(context, attrs);

		if (attrs != null) {
			TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ArcSlider, 0, 0);
			mArcStart = ta.getInt(R.styleable.ArcSlider_arc_start, mArcStart);
			mArcSweep = ta.getInt(R.styleable.ArcSlider_arc_sweep, mArcSweep);
			ta.recycle();
		}
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		requestLayout();
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);

		float pw = getWidth() - getPaddingLeft() - getPaddingRight();
		float ph = getHeight() - getPaddingTop() - getPaddingBottom();

		calculateBounds();

		float scale = Math.min(pw / mBounds.width(), ph / mBounds.height());
		mRadius *= scale;

		calculateBounds();

		float dx = -mBounds.left + pw / 2 - mBounds.width() / 2 + getPaddingLeft();
		float dy = -mBounds.top + ph / 2 - mBounds.height() / 2 + getPaddingTop();
		mCenter.offset(dx, dy);
		mBounds.offset(dx, dy);
	}

	public int getArcStart() {
		return mArcStart;
	}

	public void setArcStart(int mArcStart) {
		this.mArcStart = mArcStart;
		requestLayout();
	}

	public int getArcSweep() {
		return mArcSweep;
	}

	public void setArcSweep(int mArcSweep) {
		this.mArcSweep = mArcSweep;
		requestLayout();
	}

	private void calculateBounds() {
		getNearestPointOnBar(mTmpPointF, mCenter.x - mRadius, mCenter.y);
		mBounds.set(mTmpPointF.x, mTmpPointF.y, mTmpPointF.x, mTmpPointF.y);

		getNearestPointOnBar(mTmpPointF, mCenter.x + mRadius, mCenter.y);
		mBounds.union(mTmpPointF.x, mTmpPointF.y);

		getNearestPointOnBar(mTmpPointF, mCenter.x, mCenter.y - mRadius);
		mBounds.union(mTmpPointF.x, mTmpPointF.y);

		getNearestPointOnBar(mTmpPointF, mCenter.x, mCenter.y + mRadius);
		mBounds.union(mTmpPointF.x, mTmpPointF.y);
	}

	private void getNearestPointOnBar(PointF out, float x, float y) {
		getPointOnBar(out, getNearestBarValue(x, y));
	}

	@Override
	protected void drawConnectingLine(Canvas canvas, float from, float to, Paint paint) {
		float angle1 = (from / max) * mArcSweep;
		float angle2 = (to / max) * mArcSweep;
		float sweep = angle2 - angle1;
		ArcUtils.drawArc(canvas, mCenter, mRadius, angle1 + mArcStart, sweep, mTrackOnPaint);
	}

	@Override
	protected float getNearestBarValue(float x, float y) {
		float normalized = (float) getNormalizedAngle(x, y);
		return normalized / mArcSweep * max;
	}

	@Override
	protected void getPointOnBar(PointF out, float value) {
		float normalized = (value / max) * mArcSweep;
		out.set(ArcUtils.pointFromAngleDegrees(mCenter, mRadius, normalized + mArcStart));
	}

	@Override
	protected void drawBar(Canvas canvas, Paint paint) {
		ArcUtils.drawArc(canvas, mCenter, mRadius, mArcStart, mArcSweep, mTrackOffPaint);
	}

	// Private members /////////////////////////////////////////////////////////////

	/**
	 * Returns the angle between 0 and the point.
	 *
	 * @param x, y
	 * @return
	 */
	private double getAngle(float x, float y) {
		return Math.toDegrees(Math.atan2(y - mCenter.y, x - mCenter.x));
	}

	/**
	 * Returns the angle between mArcStart and the point.
	 *
	 * @param x, y
	 * @return
	 */
	private double getNormalizedAngle(float x, float y) {
		double normalized = (getAngle(x, y) - mArcStart + 720f) % 360f;

		if (normalized >= mArcSweep / 2f + 180f) {
			normalized = 0f;
		}
		else if (normalized > mArcSweep) {
			normalized = mArcSweep;
		}

		return normalized;
	}

}
