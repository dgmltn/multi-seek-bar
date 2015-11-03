package com.dgmltn.slider;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.dgmltn.slider.internal.AnimatedPinDrawable;

/**
 * A simple View that wraps an AnimatedPinDrawable. Almost all of the functionality
 * is part of AnimatedPinDrawable.
 */
public class PinView extends ImageView {

	AnimatedPinDrawable pin;
	float value;

	public PinView(Context context, AttributeSet attrs) {
		super(context, attrs);
		pin = new AnimatedPinDrawable(context);
		setImageDrawable(pin);

		if (attrs != null) {
			TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.PinView, 0, 0);
			setText(ta.getString(R.styleable.PinView_android_text));
			ta.recycle();
		}
	}

	public void setText(String text) {
		pin.setText(text);
		pin.invalidateSelf();
	}

	public String getText() {
		return pin.getText();
	}

	public float getValue() {
		return value;
	}

	public void setValue(float value) {
		this.value = value;
	}

	public void setTextColor(int color) {
		pin.setTextColor(color);
	}

	public int getTextColor() {
		return pin.getTextColor();
	}

	/**
	 * Move this pin so it's centered (or at least the point part of the pin is centered)
	 * on the provided point.
	 * @param point
	 */
	public void offsetTo(PointF point) {
		float x = getMeasuredWidth() / 2;
		float y = getMeasuredHeight() / 2;
		offsetLeftAndRight((int)(point.x - x - getLeft()));
		offsetTopAndBottom((int) (point.y - y - getTop()));
	}

}
