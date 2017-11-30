package com.dgmltn.multiseekbar;

import com.dgmltn.multiseekbar.internal.AbsMultiSeekBar;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;

/**
 * Created by doug on 11/2/15.
 */
public class PathSeekBar extends AbsMultiSeekBar {

    private final RectF mTmpRectF = new RectF();
    private float[] mTmpPoint = new float[2];
    private final Path mTmpPath = new Path();

    /**
     * Will get set in onLayout to the drawing bounds of where the path will be drawn
     */
    private final RectF mBounds = new RectF();

    /**
     * Matrix transforming bounds of path to drawing bounds of view/canvas
     */
    private final Matrix mMatrix = new Matrix();

    Paint mPaint = new Paint();

	private boolean mRotateThumbs = false;

    /**
     * String representing the bezier curve of the path. This string uses a subset
     * of SVG's "d" path string; it supports only M, L, C and z commands
     */
	private String mPathString;

	private final Path mPath = new Path();
    private final PathMeasure mPathMeasure = new PathMeasure(mPath, false);

    public PathSeekBar(Context context, AttributeSet attrs) {
		super(context, attrs);

		if (attrs != null) {
			TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.PathSeekBar, 0, 0);
			mRotateThumbs = ta.getBoolean(R.styleable.PathSeekBar_rotate_thumbs, mRotateThumbs);
			mPathString = ta.getString(R.styleable.PathSeekBar_path);
			ta.recycle();
		}
	}

	private void initPath() {
        // Heart
//        setPath("M 30,11 C 30,20 20,25 15,30 C 11,24 1,19 1,11 C 1,3 13,1 15,10 C 15,1 30,3 30,11");

        // Simple lines
//        setPath("M 0,1 L 0.1,1 L 0.2,0 L 0.8,0 L 0.9,1 L 1,1");

        // Bezier curves
//        setPath("M 0,1 C 0.2,1 0,0 0.5,0 C 1,0 0.8,1 1,1");
        setPath("M 0,2 C 3,2 0,0 6,0 C 12,0 9,2 12,2");
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mBounds.set(getPaddingLeft(), getPaddingTop(), w - getPaddingRight(), h - getPaddingBottom());
        measurePath();
    }

    public void setPath(String pathString) {
        mPathString = pathString;
        measurePath();
    }

    public String getPath() {
        return mPathString;
    }

	@Override
	protected void drawConnectingLine(Canvas canvas, float fromVal, float toVal, Paint paint) {
        float length = mPathMeasure.getLength();
        float from = fromVal / max;
        float to = toVal / max;
        mTmpPath.rewind();
        mPathMeasure.getSegment(from * length, to * length, mTmpPath, true);
        canvas.drawPath(mTmpPath, paint);
	}

	@Override
	protected float getNearestBarValue(float x, float y) {
        // An alternative method of choosing the closest point...
        // This is more mathematically correct but ends up not feeling as nice
        // return closestPoint(x, y) * max;

		return clamp((x - mBounds.left) / mBounds.width()) * max;
	}

	@Override
	protected void getPointOnBar(PointF out, float value) {
		value = clamp(value, 0f, max);
		mPathMeasure.getPosTan(value / max * mPathMeasure.getLength(), mTmpPoint, null);
		out.set(mTmpPoint[0], mTmpPoint[1]);
	}

	@Override
	protected void drawBar(Canvas canvas, Paint paint) {
	    mPaint.set(paint);
		canvas.drawPath(mPath, mPaint);
	}

	@Override
	public void onValueChange(ThumbView thumb, float oldVal, float newVal) {
		super.onValueChange(thumb, oldVal, newVal);

		if (mRotateThumbs) {
            mPathMeasure.getPosTan(newVal / max * mPathMeasure.getLength(), null, mTmpPoint);
            float angle = (float) Math.toDegrees(Math.atan(mTmpPoint[1] / mTmpPoint[0]));
            if (!Float.isNaN(angle)) {
                thumb.setRotation(angle);
            }
        }
	}

    // Private members /////////////////////////////////////////////////////////////

    private float clamp(float v) {
        return clamp(v, 0f, 1f);
    }

    private float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    // https://stackoverflow.com/questions/20378930/svg-path-to-b%C3%A9zier-path-in-android

    /**
     * An ABSOLUTE MINIMUM to converd SVG's path "d" attribute to android Path;
     * it supports only M, L, C and z commands
     * @param data
     * @param p
     */
    private void readPath(String data, Path p) {
        p.reset();
        try {
            String[] tokens = data.split("[ ,]");
            int i = 0;
            while (i < tokens.length) {
                String token = tokens[i++];
                if (token.equals("M")) {
                    float x = Float.valueOf(tokens[i++]);
                    float y = Float.valueOf(tokens[i++]);
                    p.moveTo(x, y);
                } else if (token.equals("L")) {
                    float x = Float.valueOf(tokens[i++]);
                    float y = Float.valueOf(tokens[i++]);
                    p.lineTo(x, y);
                } else if (token.equals("C")) {
                    float x1 = Float.valueOf(tokens[i++]);
                    float y1 = Float.valueOf(tokens[i++]);
                    float x2 = Float.valueOf(tokens[i++]);
                    float y2 = Float.valueOf(tokens[i++]);
                    float x3 = Float.valueOf(tokens[i++]);
                    float y3 = Float.valueOf(tokens[i++]);
                    p.cubicTo(x1, y1, x2, y2, x3, y3);
                } else if (token.equals("z")) {
                    p.close();
                } else {
                    throw new RuntimeException("unknown command [" + token + "]");
                }
            }
        } catch (IndexOutOfBoundsException e) {
            throw new RuntimeException("bad data ", e);
        }
    }

    private void measurePath() {
        mPath.reset();
        readPath(mPathString, mPath);

        mPath.computeBounds(mTmpRectF, true);
        mMatrix.setScale(mBounds.width() / mTmpRectF.width(), mBounds.height() / mTmpRectF.height(), 0f, 0f);
        mMatrix.postTranslate(mBounds.left, mBounds.top);
        mPath.transform(mMatrix);

        mPathMeasure.setPath(mPath, false);
    }

    /**
     * Finds the distance to the point on the curve at position x (normalized 0f <= x <= 1f)
     * @param value the normalized x value of the curve
     * @param x the actual x coordinate to measure against
     * @param y the actual y coordinate to measure against
     * @return the square of the distance
     */
    private float distance2(float value, float x, float y) {
        mPathMeasure.getPosTan(value * mPathMeasure.getLength(), mTmpPoint, null);
        float dx = mTmpPoint[0] - x;
        float dy = mTmpPoint[1] - y;
        return (dx * dx) + (dy * dy);
    }

    /** Find the ~closest point on the Path to a point you supply.
     * returns: The parameter x representing the x location of the closest line on the Path
     */
    private float closestPoint(final float x, final float y) {
        int scans = 25; // More scans -> better chance of being correct
        int mindex = scans;
        float min = Float.MAX_VALUE;
        for (int i = scans+1; i > 0; i--) {
            float d2 = distance2(i * 1f / scans, x, y);
            if (d2 < min) {
                min = d2;
                mindex = i;
            }
        }
        float t0 = Math.max((mindex - 1f) / scans, 0f);
        float t1 = Math.min((mindex + 1f) / scans, 1f);
        Func1 d2ForT = new Func1() {
            @Override
            public float ƒ(float v) {
                return distance2(v, x, y);
            }
        };
        return localMinimum(t0, t1, d2ForT, (float)1e-4);
    }

    /** Find a minimum point for a bounded function. May be a local minimum.
     * minX   : the smallest input value
     * maxX   : the largest input value
     * ƒ      : a function that returns a value `y` given an `x`
     * ε      : how close in `x` the bounds must be before returning
     * returns: the `x` value that produces the smallest `y`
     */
    private float localMinimum(float minX, float maxX, Func1 ƒ, float ε) {
        float m = minX;
        float n = maxX;
        float k = minX;
        while ((n - m) > ε) {
            k = (n + m) / 2;
            if (ƒ.ƒ(k-ε)<ƒ.ƒ(k+ε)) {
                n=k;
            }
            else {
                m=k;
            }
        }
        return k;
    }

    private interface Func1 {
        float ƒ(float x);
    }

}
