package com.dgmltn.slider;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PointF;
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
			setTextColor(ta.getColor(R.styleable.AbsSlider_textColor, textColor));
			ta.recycle();
		}

		setValue(value);
	}

	public void setTextColor(int color) {
		textColor = color;
		pin.setTextColor(color);
	}

	public float getValue() {
		return value;
	}

	public void setValue(float value) {
		this.value = value;
		pin.setText(Integer.toString(Math.round(value)));
		pin.invalidateSelf();
	}

}
