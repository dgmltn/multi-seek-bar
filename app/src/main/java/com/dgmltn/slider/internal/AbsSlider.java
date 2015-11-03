package com.dgmltn.slider.internal;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.StateSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.dgmltn.slider.PinView;
import com.dgmltn.slider.R;

/**
 * Created by doug on 11/1/15.
 */
public abstract class AbsSlider extends ViewGroup {

	private static final int INDIGO_500 = 0xff3f51b5;
	private static final int DEFAULT_TRACK_OFF_COLOR = 0x42000000;
	private static final int DEFAULT_TRACK_ON_COLOR = INDIGO_500;
	private static final int DEFAULT_THUMB_COLOR = INDIGO_500;
	private static final int DEFAULT_TICK_COLOR = Color.BLACK;
	private static final int DEFAULT_TEXT_COLOR = Color.WHITE;

	// Bar properties
	private boolean hasTicks = true;
	private boolean isDiscrete = true;
	protected int max = 10;
	protected PinView[] pins = new PinView[2];

	// Colors
	private ColorStateList trackColor = new ColorStateList(
		new int[][] {
			new int[] {android.R.attr.state_selected},
			StateSet.WILD_CARD
		},
		new int[] {
			DEFAULT_TRACK_ON_COLOR,
			DEFAULT_TRACK_OFF_COLOR
		}
	);
	private ColorStateList tickColor = ColorStateList.valueOf(DEFAULT_TICK_COLOR);
	private ColorStateList thumbColor = ColorStateList.valueOf(DEFAULT_THUMB_COLOR);
	private int textColor = DEFAULT_TEXT_COLOR;

	// Sizes
	private int trackOffWidth = 4;
	private int trackOnWidth = trackOffWidth;
	private int tickRadius = trackOffWidth / 2;

	public AbsSlider(Context context, AttributeSet attrs) {
		super(context, attrs);
		setClipToPadding(false);
		setClipChildren(false);
		setWillNotDraw(false);

		if (attrs != null) {
			TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.AbsSlider, 0, 0);
			hasTicks = ta.getBoolean(R.styleable.AbsSlider_hasTicks, hasTicks);
			isDiscrete = ta.getBoolean(R.styleable.AbsSlider_isDiscrete, isDiscrete);
			if (ta.hasValue(R.styleable.AbsSlider_trackColor)) {
				trackColor = ta.getColorStateList(R.styleable.AbsSlider_trackColor);
			}
			if (ta.hasValue(R.styleable.AbsSlider_tickColor)) {
				tickColor = ta.getColorStateList(R.styleable.AbsSlider_tickColor);
			}
			if (ta.hasValue(R.styleable.AbsSlider_thumbColor)) {
				thumbColor = ta.getColorStateList(R.styleable.AbsSlider_thumbColor);
			}
			textColor = ta.getColor(R.styleable.AbsSlider_textColor, textColor);
			ta.recycle();
		}

		initTrack();
		initPins();
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
			View child = getChildAt(i);
			child.layout(0, 0, child.getMeasuredWidth(), child.getMeasuredHeight());
		}
		placePins();
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

	/////////////////////////////////////////////////////////////////////////
	// Accessors
	/////////////////////////////////////////////////////////////////////////

	public void setPinValue(int index, float value) {
		value = clamp(value);
		if (value != pins[index].getValue()) {
			pins[index].setValue(value);
			pins[index].setText(Integer.toString(Math.round(value)));
			placePins();
			invalidate();
		}
	}

	private float clamp(float value) {
		return value < 0 ? 0 : value > max ? max : value;
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
			float d1 = distance(pins[0].getValue(), event.getX(), event.getY());
			float d2 = distance(pins[1].getValue(), event.getX(), event.getY());
			expanded = -1;
			if (d1 < d2) {
				pins[0].getHitRect(mTmpRect);
				if (mTmpRect.contains((int) event.getX(), (int) event.getY())) {
					expanded = 0;
				}
			}
			else {
				pins[1].getHitRect(mTmpRect);
				if (mTmpRect.contains((int) event.getX(), (int) event.getY())) {
					expanded = 1;
				}
			}
			if (expanded > -1) {
				pins[expanded].setPressed(true);
				cancelLongPress();
				attemptClaimDrag();
			}
			break;

		case MotionEvent.ACTION_MOVE:
			if (expanded > -1) {
				float value = getNearestBarValue(event.getX(), event.getY());
				setPinValue(expanded, value);
			}
			break;

		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			if (expanded > -1) {
				pins[expanded].setPressed(false);
				setPinValue(expanded, Math.round(pins[expanded].getValue()));
				expanded = -1;
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

	protected void placePins() {
		for (int i = 0; i < pins.length; i++) {
			getPointOnBar(mTmpPointF, pins[i].getValue());
			pins[i].offsetTo(mTmpPointF);
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
		// Initialize the paint.
		mTrackOffPaint = new Paint();
		mTrackOffPaint.setAntiAlias(true);
		mTrackOffPaint.setStyle(Paint.Style.STROKE);
		mTrackOffPaint.setColor(trackColor.getColorForState(ACTIVATED, trackColor.getDefaultColor()));
		mTrackOffPaint.setStrokeWidth(trackOffWidth);

		mTickPaint = new Paint();
		mTickPaint.setAntiAlias(true);
		mTickPaint.setColor(tickColor.getColorForState(ACTIVATED, tickColor.getDefaultColor()));

		// Initialize the paint, set values
		mTrackOnPaint = new Paint();
		mTrackOnPaint.setStrokeCap(Paint.Cap.ROUND);
		mTrackOnPaint.setStyle(Paint.Style.STROKE);
		mTrackOnPaint.setAntiAlias(true);
		mTrackOnPaint.setColor(trackColor.getColorForState(SELECTED, trackColor.getDefaultColor()));
		mTrackOnPaint.setStrokeWidth(trackOnWidth);
	}

	protected void initPins() {
		for (int i = 0; i < pins.length; i++) {
			pins[i] = new PinView(getContext(), null);
			pins[i].setImageTintList(thumbColor);
			pins[i].setTextColor(textColor);
			addView(pins[i]);
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		drawBar(canvas, mTrackOffPaint);
		if (hasTicks) {
			drawTicks(canvas);
		}
		drawConnectingLine(canvas, pins[0].getValue(), pins[1].getValue(), mTrackOnPaint);
	}

	/**
	 * Draws the tick marks on the bar.
	 *
	 * @param canvas Canvas to draw on; should be the Canvas passed into {#link
	 *               View#onDraw()}
	 */
	protected void drawTicks(Canvas canvas) {
		for (int i = 0; i <= max; i++) {
			getPointOnBar(mTmpPointF, i);
			canvas.drawCircle(mTmpPointF.x, mTmpPointF.y, tickRadius, mTickPaint);
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
