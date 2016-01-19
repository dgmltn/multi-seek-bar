package com.dgmltn.slider;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.IntDef;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.StateSet;
import android.view.View;

import com.dgmltn.slider.internal.AnimatedPinDrawable;
import com.dgmltn.slider.internal.Utils;

/**
 * A simple View that wraps an AnimatedPinDrawable. Almost all of the functionality
 * is part of AnimatedPinDrawable.
 */
public class ThumbView extends View {

	private static final int DEFAULT_TEXT_COLOR = Color.WHITE;

	@Retention(RetentionPolicy.SOURCE)
	@IntDef({ STYLE_CIRCLE, STYLE_PIN, STYLE_NOTHING })
	public @interface ThumbStyle {
	}

	public static final int STYLE_CIRCLE = 0;
	public static final int STYLE_PIN = 1;
	public static final int STYLE_NOTHING = 2;

	private
	@ThumbStyle
	int thumbStyle = STYLE_CIRCLE;

	Drawable drawable = null;
	float value = 0f;
	private String customText = null;
	boolean useCustomText = false;

	ColorStateList mDrawableTintList = null;

	public ThumbView(Context context, AttributeSet attrs) {
		super(context, attrs);

		boolean clickable = true;

		if (attrs != null) {
			TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ThumbView, 0, 0);

			thumbStyle = validateThumbStyle(ta.getInt(R.styleable.ThumbView_thumb_style, thumbStyle));

			if (ta.hasValue(R.styleable.ThumbView_thumb_color)) {
				setImageTintList(ta.getColorStateList(R.styleable.ThumbView_thumb_color));
			}
			else {
				setImageTintList(generateDefaultColorStateListFromTheme(context));
			}

			setTextColor(ta.getColor(R.styleable.ThumbView_thumb_textColor, DEFAULT_TEXT_COLOR));

			if (ta.hasValue(R.styleable.ThumbView_android_text)) {
				setText(ta.getString(R.styleable.ThumbView_android_text));
			}

			setValue(ta.getFloat(R.styleable.ThumbView_value, value));

			clickable = ta.getBoolean(R.styleable.ThumbView_android_clickable, clickable);

			ta.recycle();
		}

		setThumbStyle(thumbStyle);
		setValue(value);
		setClickable(clickable);
	}

	public void setImageTintList(ColorStateList tint) {
		mDrawableTintList = tint;
		if (drawable != null) {
			drawable.setTintList(tint);
			drawable.invalidateSelf();
		}
	}

	@Override
	public void jumpDrawablesToCurrentState() {
		super.jumpDrawablesToCurrentState();
		if (drawable != null) drawable.jumpToCurrentState();
	}

	@Override
	protected void drawableStateChanged() {
		super.drawableStateChanged();
		Drawable d = drawable;
		if (d != null && d.isStateful()) {
			d.setState(getDrawableState());
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int w = 0;
		int h = 0;

		if (drawable != null) {
			w = drawable.getIntrinsicWidth();
			h = drawable.getIntrinsicHeight();
			if (w <= 0) w = 1;
			if (h <= 0) h = 1;
		}

		int pleft = getPaddingLeft();
		int pright = getPaddingRight();
		int ptop = getPaddingTop();
		int pbottom = getPaddingBottom();

		w += pleft + pright;
		h += ptop + pbottom;

		w = Math.max(w, getSuggestedMinimumWidth());
		h = Math.max(h, getSuggestedMinimumHeight());

		int widthSize = resolveSizeAndState(w, widthMeasureSpec, 0);
		int heightSize = resolveSizeAndState(h, heightMeasureSpec, 0);

		setMeasuredDimension(widthSize, heightSize);
	}

	public
	@ThumbStyle
	int getThumbStyle() {
		return thumbStyle;
	}

	public static
	@ThumbStyle
	int validateThumbStyle(int s) {
		return (s == STYLE_PIN || s == STYLE_NOTHING) ? s : STYLE_CIRCLE;
	}

	public void setThumbStyle(@ThumbStyle int thumbStyle) {
		this.thumbStyle = thumbStyle;
		drawable =
			thumbStyle == STYLE_PIN ? new AnimatedPinDrawable(getContext())
				: thumbStyle == STYLE_NOTHING ? null
					: ContextCompat.getDrawable(getContext(), R.drawable.seekbar_thumb_material_anim);
		if (drawable != null) {
			drawable.setTintList(mDrawableTintList);
			drawable.setState(getDrawableState());
			drawable.setVisible(getVisibility() == VISIBLE, true);
			drawable.setCallback(this);
		}
		invalidate();
	}

	@Override
	public void invalidateDrawable(Drawable dr) {
		if (dr == drawable) {
			invalidate();
		}
		else {
			super.invalidateDrawable(dr);
		}
	}

	private Rect bounds = new Rect();

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (drawable != null) {
			canvas.getClipBounds(bounds);
			drawable.setBounds(bounds);
			drawable.draw(canvas);
		}
	}

	public String getText() {
		return customText;
	}

	public void setText(String text) {
		useCustomText = true;
		customText = text;
		if (drawable != null && drawable instanceof AnimatedPinDrawable) {
			((AnimatedPinDrawable) drawable).setText(customText);
		}
	}

	public void setTextColor(int color) {
		if (drawable != null && drawable instanceof AnimatedPinDrawable) {
			((AnimatedPinDrawable) drawable).setTextColor(color);
		}
	}

	public float getValue() {
		return value;
	}

	public void setValue(float value) {
		if (this.value == value) {
			return;
		}

		float oldVal = this.value;
		this.value = value;

		// Notify listeners before .setText, because one thing the listeners
		// might want to do is set custom text.
		if (listeners != null) {
			for (OnValueChangedListener l : listeners) {
				l.onValueChange(this, oldVal, value);
			}
		}

		if (!useCustomText && drawable != null && drawable instanceof AnimatedPinDrawable) {
			((AnimatedPinDrawable) drawable).setText(Integer.toString(Math.round(value)));
		}
	}

	private ArrayList<OnValueChangedListener> listeners;

	public void addOnValueChangedListener(OnValueChangedListener listener) {
		if (listeners == null) {
			listeners = new ArrayList<>();
		}
		listeners.add(listener);
	}

	public void removeOnValueChangedListener(OnValueChangedListener listener) {
		if (listeners != null) {
			listeners.remove(listener);
		}
	}

	public static ColorStateList generateDefaultColorStateListFromTheme(Context context) {
		int[][] states = new int[][] {
			View.PRESSED_STATE_SET,
			View.ENABLED_STATE_SET,
			StateSet.WILD_CARD
		};

		int[] colors = new int[] {
			Utils.getThemeColor(context, android.R.attr.colorControlActivated),
			Utils.getThemeColor(context, android.R.attr.colorControlActivated),
			0x42000000 //TODO: get this from a theme attr?
		};

		return new ColorStateList(states, colors);
	}

	public interface OnValueChangedListener {
		/**
		 * Called upon a change of the current value.
		 *
		 * @param thumb  The ThumbView associated with this listener.
		 * @param oldVal The previous value.
		 * @param newVal The new value.
		 */
		void onValueChange(ThumbView thumb, float oldVal, float newVal);
	}

}
