/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sl.utakephoto.crop;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.sl.utakephoto_lib.R;


public class CropView extends View {
    private static final String LOGTAG = "CropView";

    private RectF mImageBounds = new RectF();
    private RectF mScreenBounds = new RectF();
    private RectF mScreenImageBounds = new RectF();
    private RectF mScreenCropBounds = new RectF();
    private Rect mShadowBounds = new Rect();

    private Bitmap mBitmap;
    private Paint mPaint = new Paint();

    private NinePatchDrawable mShadow;
    private CropObject mCropObj = null;
    private Drawable mCropIndicator;
    private int mIndicatorSize;
    private int mRotation = 0;
    private boolean mMovingBlock = false;
    private Matrix mDisplayMatrix = null;
    private Matrix mDisplayMatrixInverse = null;
    private boolean mDirty = false;

    private float mPrevX = 0;
    private float mPrevY = 0;
    private float mSpotX = 0;
    private float mSpotY = 0;
    private boolean mDoSpot = false;

    private int mShadowMargin = 15;
    private int mMargin = 32;
    private int mOverlayShadowColor = 0xCF000000;
    private int mOverlayWPShadowColor = 0x5F000000;
    private int mWPMarkerColor = 0x7FFFFFFF;
    private int mMinSideSize = 90;
    private int mTouchTolerance = 40;
    private float mDashOnLength = 20;
    private float mDashOffLength = 10;

    private enum Mode {
        NONE, MOVE
    }

    private Mode mState = Mode.NONE;

    public CropView(Context context) {
        super(context);
        setup(context);
    }

    public CropView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup(context);
    }

    public CropView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setup(context);
    }

    private void setup(Context context) {
        Resources rsc = context.getResources();
        mShadow = (NinePatchDrawable) rsc.getDrawable(R.drawable.geometry_shadow);
        mCropIndicator = rsc.getDrawable(R.drawable.camera_crop);
        mIndicatorSize = (int) rsc.getDimension(R.dimen.crop_indicator_size);
        mShadowMargin = (int) rsc.getDimension(R.dimen.shadow_margin);
        mMargin = (int) rsc.getDimension(R.dimen.preview_margin);
        mMinSideSize = (int) rsc.getDimension(R.dimen.crop_min_side);
        mTouchTolerance = (int) rsc.getDimension(R.dimen.crop_touch_tolerance);
        mOverlayShadowColor = (int) rsc.getColor(R.color.crop_shadow_color);
        mOverlayWPShadowColor = (int) rsc.getColor(R.color.crop_shadow_wp_color);
        mWPMarkerColor = (int) rsc.getColor(R.color.crop_wp_markers);
        mDashOnLength = rsc.getDimension(R.dimen.wp_selector_dash_length);
        mDashOffLength = rsc.getDimension(R.dimen.wp_selector_off_length);
    }

    public void initialize(Bitmap image, RectF newCropBounds, RectF newPhotoBounds, int rotation) {
        mBitmap = image;
        if (mCropObj != null) {
            RectF crop = mCropObj.getInnerBounds();
            RectF containing = mCropObj.getOuterBounds();
            if (crop != newCropBounds || containing != newPhotoBounds
                    || mRotation != rotation) {
                mRotation = rotation;
                mCropObj.resetBoundsTo(newCropBounds, newPhotoBounds);
                clearDisplay();
            }
        } else {
            mRotation = rotation;
            mCropObj = new CropObject(newPhotoBounds, newCropBounds, 0);
            clearDisplay();
        }
    }

    public RectF getCrop() {
        return mCropObj.getInnerBounds();
    }

    public RectF getPhoto() {
        return mCropObj.getOuterBounds();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        if (mDisplayMatrix == null || mDisplayMatrixInverse == null) {
            return true;
        }
        float[] touchPoint = {
                x, y
        };
        mDisplayMatrixInverse.mapPoints(touchPoint);
        x = touchPoint[0];
        y = touchPoint[1];
        switch (event.getActionMasked()) {
            case (MotionEvent.ACTION_DOWN):
                if (mState == Mode.NONE) {
                    if (!mCropObj.selectEdge(x, y)) {
                        mMovingBlock = mCropObj.selectEdge(CropObject.MOVE_BLOCK);
                    }
                    mPrevX = x;
                    mPrevY = y;
                    mState = Mode.MOVE;
                }
                break;
            case (MotionEvent.ACTION_UP):
                if (mState == Mode.MOVE) {
                    mCropObj.selectEdge(CropObject.MOVE_NONE);
                    mMovingBlock = false;
                    mPrevX = x;
                    mPrevY = y;
                    mState = Mode.NONE;
                }
                break;
            case (MotionEvent.ACTION_MOVE):
                if (mState == Mode.MOVE) {
                    float dx = x - mPrevX;
                    float dy = y - mPrevY;
                    mCropObj.moveCurrentSelection(dx, dy);
                    mPrevX = x;
                    mPrevY = y;
                }
                break;
            default:
                break;
        }
        invalidate();
        return true;
    }

    private void reset() {
        Log.w(LOGTAG, "crop reset called");
        mState = Mode.NONE;
        mCropObj = null;
        mRotation = 0;
        mMovingBlock = false;
        clearDisplay();
    }

    private void clearDisplay() {
        mDisplayMatrix = null;
        mDisplayMatrixInverse = null;
        invalidate();
    }

    protected void configChanged() {
        mDirty = true;
    }

    public void applyFreeAspect() {
        mCropObj.unsetAspectRatio();
        invalidate();
    }

    public void applyOriginalAspect() {
        RectF outer = mCropObj.getOuterBounds();
        float w = outer.width();
        float h = outer.height();
        if (w > 0 && h > 0) {
            applyAspect(w, h);
            mCropObj.resetBoundsTo(outer, outer);
        } else {
            Log.w(LOGTAG, "failed to set aspect ratio original");
        }
    }

    public void applySquareAspect() {
        applyAspect(1, 1);
    }

    public void applyAspect(float x, float y) {
        if (x <= 0 || y <= 0) {
            throw new IllegalArgumentException("Bad arguments to applyAspect");
        }
        // If we are rotated by 90 degrees from horizontal, swap x and y
        if (((mRotation < 0) ? -mRotation : mRotation) % 180 == 90) {
            float tmp = x;
            x = y;
            y = tmp;
        }
        if (!mCropObj.setInnerAspectRatio(x, y)) {
            Log.w(LOGTAG, "failed to set aspect ratio");
        }
        invalidate();
    }

    public void setWallpaperSpotlight(float spotlightX, float spotlightY) {
        mSpotX = spotlightX;
        mSpotY = spotlightY;
        if (mSpotX > 0 && mSpotY > 0) {
            mDoSpot = true;
        }
    }

    public void unsetWallpaperSpotlight() {
        mDoSpot = false;
    }

    /**
     * Rotates first d bits in integer x to the left some number of times.
     */
    private int bitCycleLeft(int x, int times, int d) {
        int mask = (1 << d) - 1;
        int mout = x & mask;
        times %= d;
        int hi = mout >> (d - times);
        int low = (mout << times) & mask;
        int ret = x & ~mask;
        ret |= low;
        ret |= hi;
        return ret;
    }

    /**
     * Find the selected edge or corner in screen coordinates.
     */
    private int decode(int movingEdges, float rotation) {
        int rot = CropMath.constrainedRotation(rotation);
        switch (rot) {
            case 90:
                return bitCycleLeft(movingEdges, 1, 4);
            case 180:
                return bitCycleLeft(movingEdges, 2, 4);
            case 270:
                return bitCycleLeft(movingEdges, 3, 4);
            default:
                return movingEdges;
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (mBitmap == null) {
            return;
        }
        if (mDirty) {
            mDirty = false;
            clearDisplay();
        }

        mImageBounds = new RectF(0, 0, mBitmap.getWidth(), mBitmap.getHeight());
        mScreenBounds = new RectF(0, 0, canvas.getWidth(), canvas.getHeight());
        mScreenBounds.inset(mMargin, mMargin);

        // If crop object doesn't exist, create it and update it from master
        // state
        if (mCropObj == null) {
            reset();
            mCropObj = new CropObject(mImageBounds, mImageBounds, 0);
        }

        // If display matrix doesn't exist, create it and its dependencies
        if (mDisplayMatrix == null || mDisplayMatrixInverse == null) {
            mDisplayMatrix = new Matrix();
            mDisplayMatrix.reset();
            if (!CropDrawingUtils.setImageToScreenMatrix(mDisplayMatrix, mImageBounds, mScreenBounds,
                    mRotation)) {
                Log.w(LOGTAG, "failed to get screen matrix");
                mDisplayMatrix = null;
                return;
            }
            mDisplayMatrixInverse = new Matrix();
            mDisplayMatrixInverse.reset();
            if (!mDisplayMatrix.invert(mDisplayMatrixInverse)) {
                Log.w(LOGTAG, "could not invert display matrix");
                mDisplayMatrixInverse = null;
                return;
            }
            // Scale min side and tolerance by display matrix scale factor
            mCropObj.setMinInnerSideSize(mDisplayMatrixInverse.mapRadius(mMinSideSize));
            mCropObj.setTouchTolerance(mDisplayMatrixInverse.mapRadius(mTouchTolerance));
        }

        mScreenImageBounds.set(mImageBounds);

        // Draw background shadow
        if (mDisplayMatrix.mapRect(mScreenImageBounds)) {
            int margin = (int) mDisplayMatrix.mapRadius(mShadowMargin);
            mScreenImageBounds.roundOut(mShadowBounds);
            mShadowBounds.set(mShadowBounds.left - margin, mShadowBounds.top -
                    margin, mShadowBounds.right + margin, mShadowBounds.bottom + margin);
            mShadow.setBounds(mShadowBounds);
            mShadow.draw(canvas);
        }

        mPaint.setAntiAlias(true);
        mPaint.setFilterBitmap(true);
        // Draw actual bitmap
        canvas.drawBitmap(mBitmap, mDisplayMatrix, mPaint);

        mCropObj.getInnerBounds(mScreenCropBounds);

        if (mDisplayMatrix.mapRect(mScreenCropBounds)) {

            // Draw overlay shadows
            Paint p = new Paint();
            p.setColor(mOverlayShadowColor);
            p.setStyle(Paint.Style.FILL);
            CropDrawingUtils.drawShadows(canvas, p, mScreenCropBounds, mScreenImageBounds);

            // Draw crop rect and markers
            CropDrawingUtils.drawCropRect(canvas, mScreenCropBounds);
            if (!mDoSpot) {
                CropDrawingUtils.drawRuleOfThird(canvas, mScreenCropBounds);
            } else {
                Paint wpPaint = new Paint();
                wpPaint.setColor(mWPMarkerColor);
                wpPaint.setStrokeWidth(3);
                wpPaint.setStyle(Paint.Style.STROKE);
                wpPaint.setPathEffect(new DashPathEffect(new float[]
                        {mDashOnLength, mDashOnLength + mDashOffLength}, 0));
                p.setColor(mOverlayWPShadowColor);
                CropDrawingUtils.drawWallpaperSelectionFrame(canvas, mScreenCropBounds,
                        mSpotX, mSpotY, wpPaint, p);
            }
            CropDrawingUtils.drawIndicators(canvas, mCropIndicator, mIndicatorSize,
                    mScreenCropBounds, mCropObj.isFixedAspect(), decode(mCropObj.getSelectState(), mRotation));
        }

    }
}
