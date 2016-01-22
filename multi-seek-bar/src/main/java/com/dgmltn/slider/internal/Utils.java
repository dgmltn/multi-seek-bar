package com.dgmltn.slider.internal;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;

import com.dgmltn.slider.R;

/**
 * Created by doug on 11/5/15.
 */
public class Utils {

	public static int getThemeColor(Context context, int attr) {
		TypedValue typedValue = new TypedValue();
		Resources.Theme theme = context.getTheme();
		theme.resolveAttribute(attr, typedValue, true);
		return typedValue.data;
	}

	public static boolean isLightTheme(Context context) {
		TypedValue value = new TypedValue();
		return context.getTheme().resolveAttribute(R.attr.isLightTheme, value, true)
			&& value.data != 0;
	}

}
