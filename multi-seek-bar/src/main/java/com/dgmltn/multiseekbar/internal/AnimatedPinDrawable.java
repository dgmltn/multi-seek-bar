package com.dgmltn.multiseekbar.internal;

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
import android.util.StateSet;
import android.view.animation.OvershootInterpolator;

import com.dgmltn.multiseekbar.R;

/**
 * Created by doug on 10/31/15.
 */
public class AnimatedPinDrawable extends AnimatedStateListDrawable {

	private static final int STATE_COLLAPSED_ID = 1;
	private static final int STATE_EXPANDED_ID = 2;

	private static final int[] STATE_SET_PRESSED = new int[] { android.R.attr.state_pressed };
	private static final int[] STATE_SET_UNPRESSED = StateSet.WILD_CARD;

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

		addState(STATE_SET_PRESSED, expanded, STATE_EXPANDED_ID);
		addState(STATE_SET_UNPRESSED, collapsed, STATE_COLLAPSED_ID);
		addTransition(STATE_COLLAPSED_ID, STATE_EXPANDED_ID, expanding, false);
		addTransition(STATE_EXPANDED_ID, STATE_COLLAPSED_ID, collapsing, false);

		paint.setColor(Color.BLUE);
		paint.setTextSize(24f);
		paint.setTextAlign(Paint.Align.CENTER);
		paint.setAntiAlias(true);
		duration = context.getResources().getInteger(android.R.integer.config_mediumAnimTime);
	}

	@Override
	protected boolean onStateChange(int[] stateSet) {
		boolean changed = super.onStateChange(stateSet);

		if (changed && isPressed != contains(stateSet, android.R.attr.state_pressed)) {
			isPressed = !isPressed;
			if (text != null) {
				expandText(isPressed ? 0f : 1f);
			}
		}

		return changed;
	}

	public void setText(String text) {
		this.text = text;
		textScale = 0f;
		invalidateSelf();
	}

	public void rescale() {
		textScale = 0f;
		invalidateSelf();
	}

	public void setTextColor(int color) {
		paint.setColor(color);
	}

	// These numbers were determined experimentally based on the actual pin drawable
	private static final float TEXT_EXPANSION_Y_PCT_START = 0.5f;
	private static final float TEXT_EXPANSION_Y_PCT_STOP = 0.25f;
	private static final float TEXT_SCALE_PCT = 0.35f;

	@Override
	public void draw(Canvas canvas) {
		super.draw(canvas);

		if (text != null && mExpansionPercent > 0) {
			// Recalculate text scaling if necessary
			if (textScale == 0f) {
				paint.getTextBounds(text, 0, text.length(), textBounds);
				float diameter = (float) Math.sqrt(
					textBounds.width() * textBounds.width()
						+ textBounds.height() * textBounds.height());
				textScale = canvas.getWidth() * TEXT_SCALE_PCT / diameter;
			}

			int saveCount = canvas.getSaveCount();
			canvas.save();

			float scale = textScale * mExpansionPercent;
			canvas.scale(scale, scale);

			float a = TEXT_EXPANSION_Y_PCT_START;
			float b = TEXT_EXPANSION_Y_PCT_START - TEXT_EXPANSION_Y_PCT_STOP;
			float y = canvas.getHeight() * (a - b * mExpansionPercent);

			// This translates 0,0 to the center of the balloon
			canvas.translate(canvas.getWidth() / 2 / scale, y / scale);

			canvas.drawText(text, 0, (paint.descent() + paint.ascent()) / -2f, paint);

			canvas.restoreToCount(saveCount);
		}
	}

	private void expandText(float from) {
		ObjectAnimator anim = ObjectAnimator.ofFloat(this, "expansion", from, 1f - from);
		anim.setInterpolator(overshoot);
		anim.setDuration(duration);
		anim.start();
	}

	/**
	 * Used by animator in expandText()
	 *
	 * @hide
	 */
	public void setExpansion(float f) {
		mExpansionPercent = f;
	}

	/**
	 * Used by animator in expandText()
	 *
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

}
