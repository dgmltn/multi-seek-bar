package com.dgmltn.slider;

import java.util.ArrayList;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.StateSet;
import android.view.View;
import android.widget.ImageView;

import com.dgmltn.slider.internal.AbsSlider;
import com.dgmltn.slider.internal.AnimatedPinDrawable;
import com.dgmltn.slider.internal.Utils;

/**
 * A simple View that wraps an AnimatedPinDrawable. Almost all of the functionality
 * is part of AnimatedPinDrawable.
 */
public class PinView extends ImageView {

	private static final int DEFAULT_TEXT_COLOR = Color.WHITE;

	private
	@AbsSlider.SliderStyle
	int pinStyle = AbsSlider.STYLE_CONTINUOUS;

	Drawable drawable;
	float value = 0f;
	private String customText = null;
	boolean useCustomText = false;

	public PinView(Context context, AttributeSet attrs) {
		super(context, attrs);

		if (attrs != null) {
			TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.PinView, 0, 0);

			pinStyle = ta.getInt(R.styleable.AbsSlider_style, pinStyle) == 1
				? AbsSlider.STYLE_DISCRETE : AbsSlider.STYLE_CONTINUOUS;

			if (ta.hasValue(R.styleable.PinView_thumbColor)) {
				setImageTintList(ta.getColorStateList(R.styleable.PinView_thumbColor));
			}
			else {
				setImageTintList(generateDefaultColorStateListFromTheme(context));
			}

			setTextColor(ta.getColor(R.styleable.PinView_textColor, DEFAULT_TEXT_COLOR));

			if (ta.hasValue(R.styleable.PinView_android_text)) {
				setText(ta.getString(R.styleable.PinView_android_text));
			}

			setValue(ta.getFloat(R.styleable.PinView_value, value));

			ta.recycle();
		}

		setPinStyle(pinStyle);
		setValue(value);
	}

	public
	@AbsSlider.SliderStyle
	int getPinStyle() {
		return pinStyle;
	}

	public void setPinStyle(@AbsSlider.SliderStyle int pinStyle) {
		this.pinStyle = pinStyle;
		drawable = pinStyle == AbsSlider.STYLE_DISCRETE
			? new AnimatedPinDrawable(getContext())
			: ContextCompat.getDrawable(getContext(), R.drawable.seekbar_thumb_material_anim);
		setImageDrawable(drawable);
	}

	public String getText() {
		return customText;
	}

	public void setText(String text) {
		useCustomText = true;
		customText = text;
		if (drawable instanceof AnimatedPinDrawable) {
			((AnimatedPinDrawable) drawable).setText(customText);
		}
	}

	public void setTextColor(int color) {
		if (drawable instanceof AnimatedPinDrawable) {
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

		if (!useCustomText && drawable instanceof AnimatedPinDrawable) {
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
		 * @param pin    The PinView associated with this listener.
		 * @param oldVal The previous value.
		 * @param newVal The new value.
		 */
		void onValueChange(PinView pin, float oldVal, float newVal);
	}

}
