package com.dgmltn.slider.internal;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.support.annotation.IntDef;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.DecelerateInterpolator;

import com.dgmltn.slider.PinView;
import com.dgmltn.slider.R;

/**
 * Created by doug on 11/1/15.
 */
public abstract class AbsSlider extends ViewGroup implements PinView.OnValueChangedListener {

	private static final int DEFAULT_TICK_COLOR = Color.BLACK;
	private static final int DEFAULT_TEXT_COLOR = Color.WHITE;

	private static final float TOUCH_RADIUS_DP = 24;
	private static final float TRACK_WIDTH_DP = 2;
	private static final float TICK_RADIUS_DP = TRACK_WIDTH_DP / 2;

	@Retention(RetentionPolicy.SOURCE)
	@IntDef({ STYLE_CONTINUOUS, STYLE_DISCRETE })
	public @interface SliderStyle {}
	public static final int STYLE_CONTINUOUS = 0;
	public static final int STYLE_DISCRETE = 1;

	// Bar properties
	private boolean hasTicks = true;
	private @SliderStyle int sliderStyle = STYLE_CONTINUOUS;
	protected int max = 10;
	protected int thumbs = 2;

	// Colors
	private ColorStateList trackColor;
	private ColorStateList tickColor = ColorStateList.valueOf(DEFAULT_TICK_COLOR);
	private ColorStateList thumbColor;
	private int textColor = DEFAULT_TEXT_COLOR;

	public AbsSlider(Context context, AttributeSet attrs) {
		super(context, attrs);
		setClipToPadding(false);
		setClipChildren(false);
		setWillNotDraw(false);

		if (attrs != null) {
			TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.AbsSlider, 0, 0);
			max = ta.getInteger(R.styleable.AbsSlider_max, max);
			thumbs = ta.getInteger(R.styleable.AbsSlider_thumbs, thumbs);
			hasTicks = ta.getBoolean(R.styleable.AbsSlider_hasTicks, hasTicks);
			sliderStyle = ta.getInt(R.styleable.AbsSlider_style, sliderStyle) == 1
				? STYLE_DISCRETE : STYLE_CONTINUOUS;

			if (ta.hasValue(R.styleable.AbsSlider_trackColor)) {
				trackColor = ta.getColorStateList(R.styleable.AbsSlider_trackColor);
			}
			else {
				trackColor = getResources().getColorStateList(R.color.track);
			}

			if (ta.hasValue(R.styleable.AbsSlider_tickColor)) {
				tickColor = ta.getColorStateList(R.styleable.AbsSlider_tickColor);
			}

			if (ta.hasValue(R.styleable.AbsSlider_thumbColor)) {
				thumbColor = ta.getColorStateList(R.styleable.AbsSlider_thumbColor);
			}
			else {
				thumbColor = getResources().getColorStateList(R.color.thumb);
			}

			textColor = ta.getColor(R.styleable.AbsSlider_textColor, textColor);
			ta.recycle();
		}

		initTrack();
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();

		// Don't create any thumbs automatically if the user created his own.
		// Let's still listen to value changed.
		if (getChildCount() != 0) {
			thumbs = 0;
			for (int i = 0; i < getChildCount(); i++) {
				getChildAt(i).addOnValueChangedListener(this);
			}
		}
		else {
			for (int i = 0; i < thumbs; i++) {
				PinView pin = new PinView(getContext(), null);
				pin.setPinStyle(sliderStyle);
				pin.setImageTintList(thumbColor);
				pin.setTextColor(textColor);
				pin.addOnValueChangedListener(this);
				addView(pin);
			}
		}
	}

	/////////////////////////////////////////////////////////////////////////
	// Layout
	/////////////////////////////////////////////////////////////////////////

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
		int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);

		for (int i = 0; i < getChildCount(); i++) {
			View child = getChildAt(i);
			child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
		}
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		for (int i = 0; i < getChildCount(); i++) {
			PinView child = getChildAt(i);
			getPointOnBar(mTmpPointF, child.getValue());
			mTmpPointF.x -= child.getMeasuredWidth() / 2f;
			mTmpPointF.y -= child.getMeasuredHeight() / 2f;
			child.layout(
				(int) mTmpPointF.x,
				(int) mTmpPointF.y,
				(int) mTmpPointF.x + child.getMeasuredWidth(),
				(int) mTmpPointF.y + child.getMeasuredHeight()
			);
		}
	}

	@Override
	protected LayoutParams generateDefaultLayoutParams() {
		return new MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
	}

	@Override
	protected LayoutParams generateLayoutParams(LayoutParams p) {
		return new MarginLayoutParams(p);
	}

	@Override
	public LayoutParams generateLayoutParams(AttributeSet attrs) {
		return new MarginLayoutParams(getContext(), attrs);
	}

	@Override
	public PinView getChildAt(int index) {
		return (PinView) super.getChildAt(index);
	}

	@Override
	public void onValueChange(PinView pin, float oldVal, float newVal) {
		getPointOnBar(mTmpPointF, newVal);
		float dx = mTmpPointF.x - pin.getMeasuredWidth() / 2f - pin.getLeft();
		float dy = mTmpPointF.y - pin.getMeasuredHeight() / 2f - pin.getTop();
		pin.offsetLeftAndRight((int) dx);
		pin.offsetTopAndBottom((int) dy);
		invalidate();
	}

	/////////////////////////////////////////////////////////////////////////
	// Taps
	/////////////////////////////////////////////////////////////////////////

	private int expanded = -1;

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		super.onTouchEvent(event);

		// If this View is not enabled, don't allow for touch interactions.
		if (!isEnabled()) {
			return false;
		}

		switch (event.getAction()) {

		case MotionEvent.ACTION_DOWN:
			float dmin = Float.MAX_VALUE;
			expanded = -1;
			for (int i = 0; i < getChildCount(); i++) {
				PinView pin = getChildAt(i);
				float d = distance(pin.getValue(), event.getX(), event.getY());
				if (d < dmin) {
					dmin = d;
					// 48dp touch region
					if (d <= TOUCH_RADIUS_DP * getResources().getDisplayMetrics().density) {
						expanded = i;
					}
				}
			}
			if (expanded > -1) {
				PinView pin = getChildAt(expanded);
				pin.setPressed(true);
				cancelLongPress();
				attemptClaimDrag();
			}
			break;

		case MotionEvent.ACTION_MOVE:
			if (expanded > -1) {
				float value = getNearestBarValue(event.getX(), event.getY());
				getChildAt(expanded).setValue(value);
			}
			break;

		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			if (expanded > -1) {
				PinView pin = getChildAt(expanded);
				pin.setPressed(false);
				expanded = -1;
				if (sliderStyle == STYLE_DISCRETE) {
					float value = Math.round(pin.getValue());
					ObjectAnimator anim = ObjectAnimator.ofFloat(pin, "value", value).setDuration(100);
					anim.setInterpolator(new DecelerateInterpolator());
					anim.start();
				}
			}
			break;
		}

		return true;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		return true;
	}

	private float distance(float value, float x, float y) {
		getPointOnBar(mTmpPointF, value);
		float m = mTmpPointF.x - x;
		float n = mTmpPointF.y - y;
		return (float) Math.sqrt(m * m + n * n);
	}

	/**
	 * Tries to claim the user's drag motion, and requests disallowing any
	 * ancestors from stealing events in the drag.
	 * https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/widget/AbsSeekBar.java
	 */
	private void attemptClaimDrag() {
		ViewParent parent = getParent();
		if (parent != null) {
			parent.requestDisallowInterceptTouchEvent(true);
		}
	}

	/////////////////////////////////////////////////////////////////////////
	// Rendering
	/////////////////////////////////////////////////////////////////////////

	protected Paint mTrackOffPaint;
	protected Paint mTrackOnPaint;
	protected Paint mTickPaint;

	PointF mTmpPointF = new PointF();
	Rect mTmpRect = new Rect();

	private static final int[] SELECTED = new int[] { android.R.attr.state_selected };
	private static final int[] ACTIVATED = new int[] { android.R.attr.state_activated };

	protected void initTrack() {
		float density = getResources().getDisplayMetrics().density;

		// Initialize the paint.
		mTrackOffPaint = new Paint();
		mTrackOffPaint.setAntiAlias(true);
		mTrackOffPaint.setStyle(Paint.Style.STROKE);
		mTrackOffPaint.setColor(trackColor.getColorForState(ACTIVATED, trackColor.getDefaultColor()));
		mTrackOffPaint.setStrokeWidth(TRACK_WIDTH_DP * density);

		mTickPaint = new Paint();
		mTickPaint.setAntiAlias(true);
		mTickPaint.setColor(tickColor.getColorForState(ACTIVATED, tickColor.getDefaultColor()));

		// Initialize the paint, set values
		mTrackOnPaint = new Paint();
		mTrackOnPaint.setStrokeCap(Paint.Cap.ROUND);
		mTrackOnPaint.setStyle(Paint.Style.STROKE);
		mTrackOnPaint.setAntiAlias(true);
		mTrackOnPaint.setColor(trackColor.getColorForState(SELECTED, trackColor.getDefaultColor()));
		mTrackOnPaint.setStrokeWidth(TRACK_WIDTH_DP * density);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		drawBar(canvas, mTrackOffPaint);
		if (hasTicks) {
			drawTicks(canvas);
		}
		if (getChildCount() == 1) {
			drawConnectingLine(canvas, 0f, getChildAt(0).getValue(), mTrackOnPaint);
		}
		else if (getChildCount() == 2) {
			drawConnectingLine(canvas, getChildAt(0).getValue(), getChildAt(1).getValue(), mTrackOnPaint);
		}
	}

	/**
	 * Draws the tick marks on the bar.
	 *
	 * @param canvas Canvas to draw on; should be the Canvas passed into {#link
	 *               View#onDraw()}
	 */
	protected void drawTicks(Canvas canvas) {
		float density = getResources().getDisplayMetrics().density;

		for (int i = 0; i <= max; i++) {
			getPointOnBar(mTmpPointF, i);
			canvas.drawCircle(mTmpPointF.x, mTmpPointF.y, TICK_RADIUS_DP * density, mTickPaint);
		}
	}

	/**
	 * Draw the connecting line between the two thumbs in RangeBar.
	 *
	 * @param canvas the Canvas to draw on
	 * @param from   the lower bar value of the connecting line
	 * @param to     the upper bar value of the connecting line
	 */
	protected abstract void drawConnectingLine(Canvas canvas, float from, float to, Paint paint);

	/**
	 * Gets the value of the bar nearest to the passed point.
	 *
	 * @param x the x value to snap to the bar
	 * @param y the y value to snap to the bar
	 */
	protected abstract float getNearestBarValue(float x, float y);

	/**
	 * Gets the coordinates of the bar value.
	 */
	protected abstract void getPointOnBar(PointF out, float value);

	/**
	 * Draws the bar on the given Canvas.
	 *
	 * @param canvas Canvas to draw on; should be the Canvas passed into {#link
	 *               View#onDraw()}
	 */
	protected abstract void drawBar(Canvas canvas, Paint paint);
}
