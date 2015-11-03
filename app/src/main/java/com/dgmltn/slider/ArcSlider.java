package com.dgmltn.slider;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;

import com.dgmltn.slider.internal.AbsSlider;
import com.dgmltn.slider.internal.ArcUtils;

/**
 * Created by doug on 11/2/15.
 */
public class ArcSlider extends AbsSlider {

	private static final float ARC_START = 150f;
	private static final float ARC_SWEEP = 240f;

	private PointF mCenter = new PointF();
	private float mRadius = 1f;
	private RectF mBounds = new RectF();

	public ArcSlider(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);

		float pw = w - getPaddingLeft() - getPaddingRight();
		float ph = h - getPaddingTop() - getPaddingBottom();

		calculateBounds();

		float scale = Math.min(pw / mBounds.width(), ph / mBounds.height());
		mRadius *= scale;

		calculateBounds();

		float dx = -mBounds.left + pw / 2 - mBounds.width() / 2 + getPaddingLeft();
		float dy = -mBounds.top + ph / 2 - mBounds.height() / 2 + getPaddingTop();
		mCenter.offset(dx, dy);
		mBounds.offset(dx, dy);
	}

	private void calculateBounds() {
		PointF test = new PointF();

		getNearestPointOnBar(test, mCenter.x - mRadius, mCenter.y);
		mBounds.set(test.x, test.y, test.x, test.y);

		getNearestPointOnBar(test, mCenter.x + mRadius, mCenter.y);
		mBounds.union(test.x, test.y);

		getNearestPointOnBar(test, mCenter.x, mCenter.y - mRadius);
		mBounds.union(test.x, test.y);

		getNearestPointOnBar(test, mCenter.x, mCenter.y + mRadius);
		mBounds.union(test.x, test.y);
	}

	private void getNearestPointOnBar(PointF out, float x, float y) {
		getPointOnBar(out, getNearestBarValue(x, y));
	}

	@Override
	protected void drawConnectingLine(Canvas canvas, float from, float to, Paint paint) {
		float angle1 = (from / max) * ARC_SWEEP;
		float angle2 = (to / max) * ARC_SWEEP;
		float sweep = angle2 - angle1;
		ArcUtils.drawArc(canvas, mCenter, mRadius, angle1 + ARC_START, sweep, mTrackOnPaint);
	}

	@Override
	protected float getNearestBarValue(float x, float y) {
		float normalized = getNormalizedAngle(x, y);
		return normalized / ARC_SWEEP * max;
	}

	@Override
	protected void getPointOnBar(PointF out, float value) {
		float normalized = (value / max) * ARC_SWEEP;
		out.set(ArcUtils.pointFromAngleDegrees(mCenter, mRadius, normalized + ARC_START));
	}

	@Override
	protected void drawBar(Canvas canvas, Paint paint) {
		ArcUtils.drawArc(canvas, mCenter, mRadius, ARC_START, ARC_SWEEP, mTrackOffPaint);
	}

	// Private members /////////////////////////////////////////////////////////////

	/**
	 * Returns the angle between 0 and the point.
	 *
	 * @param x, y
	 * @return
	 */
	private float getAngle(float x, float y) {
		return (float) Math.toDegrees(Math.atan2(y - mCenter.y, x - mCenter.x));
	}

	/**
	 * Returns the angle between ARC_START and the point.
	 *
	 * @param x, y
	 * @return
	 */
	private float getNormalizedAngle(float x, float y) {
		float normalized = (getAngle(x, y) - ARC_START + 720f) % 360f;

		if (normalized >= ARC_SWEEP / 2f + 180f) {
			normalized = 0f;
		}
		else if (normalized > ARC_SWEEP) {
			normalized = ARC_SWEEP;
		}

		return normalized;
	}

}
