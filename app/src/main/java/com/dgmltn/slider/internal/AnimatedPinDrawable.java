package com.dgmltn.slider.internal;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.AnimatedStateListDrawable;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.view.animation.OvershootInterpolator;

import com.dgmltn.slider.R;

/**
 * Created by doug on 10/31/15.
 */
public class AnimatedPinDrawable extends AnimatedStateListDrawable {

	private static final int STATE_COLLAPSED_ID = 1;
	private static final int STATE_EXPANDED_ID = 2;

	private static final int[] STATE_SET_UNPRESSED = new int[] { -android.R.attr.state_pressed };
	private static final int[] STATE_SET_PRESSED = new int[] { android.R.attr.state_pressed };

	private TextPaint paint = new TextPaint();
	private boolean isPressed = false;
	private String text = "";
	private int duration;

	private float textScale = 0f;
	Rect textBounds = new Rect();
	private OvershootInterpolator overshoot = new OvershootInterpolator();
	private float mExpansionPercent = 0f;

	public AnimatedPinDrawable(Context context) {
		super();
		Drawable collapsed = context.getDrawable(R.drawable.pin_collapsed);
		Drawable expanded = context.getDrawable(R.drawable.pin_expanded);
		AnimatedVectorDrawable expanding = (AnimatedVectorDrawable) context
			.getDrawable(R.drawable.pin_collapsed_to_expanded_animation);
		AnimatedVectorDrawable collapsing = (AnimatedVectorDrawable) context
			.getDrawable(R.drawable.pin_expanded_to_collapsed_animation);

		addState(STATE_SET_UNPRESSED, collapsed, STATE_COLLAPSED_ID);
		addState(STATE_SET_PRESSED, expanded, STATE_EXPANDED_ID);
		addTransition(STATE_COLLAPSED_ID, STATE_EXPANDED_ID, expanding, false);
		addTransition(STATE_EXPANDED_ID, STATE_COLLAPSED_ID, collapsing, false);

		paint.setColor(Color.WHITE);
		paint.setTextSize(24f);
		paint.setTextAlign(Paint.Align.CENTER);
		paint.setAntiAlias(true);
		duration = context.getResources().getInteger(R.integer.anim_duration);
	}

	@Override
	protected boolean onStateChange(int[] stateSet) {
		boolean changed = super.onStateChange(stateSet);

		if (changed && isPressed != contains(stateSet, android.R.attr.state_pressed)) {
			isPressed = !isPressed;
			expandText(isPressed ? 0f : 1f);
		}

		return changed;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
		textScale = 0f;
	}

	public int getTextColor() {
		return paint.getColor();
	}

	public void setTextColor(int color) {
		paint.setColor(color);
	}

	public boolean isExpanded() {
		return this.isPressed;
	}

	@Override
	public void draw(Canvas canvas) {
		super.draw(canvas);

		if (mExpansionPercent >= 0) {
			// Recalculate text scaling if necessary
			if (textScale == 0f) {
				paint.getTextBounds(text, 0, text.length(), textBounds);
				float diameter = (float) Math.sqrt(
					textBounds.width() * textBounds.width()
						+ textBounds.height() * textBounds.height());
				textScale = canvas.getWidth() * 0.225f / diameter;
			}

			float scale = textScale * mExpansionPercent;
			canvas.scale(scale, scale);

			// at mExpansionPercent = 1f, y = 18% of height
			// at mExpansionPercent = 0f, y = 50% of height
			float y = canvas.getHeight() * (0.5f - .32f * mExpansionPercent);

			canvas.translate(canvas.getWidth() / 2 / scale, y / scale);
			canvas.drawText(text, 0, textBounds.height() / 2 * mExpansionPercent, paint);
			canvas.restore();
		}
	}

	private void expandText(float from) {
		ObjectAnimator anim = ObjectAnimator.ofFloat(this, "expansion", from, 1f - from);
		anim.setInterpolator(overshoot);
		anim.setDuration(duration);
		anim.start();
	}

	/**
	 * @hide
	 */
	public void setExpansion(float f) {
		mExpansionPercent = f;
	}

	/**
	 * @hide
	 */
	public float getExpansion() {
		return mExpansionPercent;
	}

	private boolean contains(int[] set, int value) {
		for (int state : set) {
			if (state == value) {
				return true;
			}
		}
		return false;
	}

	private String getStateArrayAsString(int[] state) {
		StringBuilder sb = new StringBuilder("[");
		for (int value : state) {
			switch (value) {
			case android.R.attr.state_window_focused:
				sb.append("window_focused");
				break;
			case android.R.attr.state_enabled:
				sb.append("enabled");
				break;
			case android.R.attr.state_selected:
				sb.append("selected");
				break;
			case android.R.attr.state_accelerated:
				sb.append("accelerated");
				break;
			case android.R.attr.state_pressed:
				sb.append("pressed");
				break;
			default:
				sb.append(Integer.toString(value));
				break;
			}
			sb.append(", ");
		}
		if (sb.length() > 2) {
			sb.delete(sb.length() - 2, sb.length());
		}
		sb.append("]");
		return sb.toString();
	}

}
