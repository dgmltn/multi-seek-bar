package com.dgmltn.slider;

import java.util.ArrayList;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.dgmltn.slider.internal.AnimatedPinDrawable;

/**
 * A simple View that wraps an AnimatedPinDrawable. Almost all of the functionality
 * is part of AnimatedPinDrawable.
 */
public class PinView extends ImageView {

	private static final int INDIGO_500 = 0xff3f51b5;
	private static final int DEFAULT_THUMB_COLOR = INDIGO_500;
	private static final int DEFAULT_TEXT_COLOR = Color.WHITE;

	private ColorStateList thumbColor = ColorStateList.valueOf(DEFAULT_THUMB_COLOR);
	private int textColor = DEFAULT_TEXT_COLOR;

	AnimatedPinDrawable pin;
	float value = 0f;
	private String customText = null;
	boolean useCustomText = false;

	public PinView(Context context, AttributeSet attrs) {
		super(context, attrs);
		pin = new AnimatedPinDrawable(context);
		setImageDrawable(pin);

		if (attrs != null) {
			TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.PinView, 0, 0);
			if (ta.hasValue(R.styleable.PinView_thumbColor)) {
				thumbColor = ta.getColorStateList(R.styleable.PinView_thumbColor);
			}
			setImageTintList(thumbColor);
			setTextColor(ta.getColor(R.styleable.PinView_textColor, textColor));
			if (ta.hasValue(R.styleable.PinView_android_text)) {
				setText(ta.getString(R.styleable.PinView_android_text));
			}
			ta.recycle();
		}

		setValue(value);
	}

	public String getText() {
		return customText;
	}

	public void setText(String text) {
		useCustomText = true;
		customText = text;
		pin.setText(customText);
		pin.invalidateSelf();
	}

	public void setTextColor(int color) {
		textColor = color;
		pin.setTextColor(color);
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

		if (!useCustomText) {
			pin.setText(Integer.toString(Math.round(value)));
			pin.invalidateSelf();
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
