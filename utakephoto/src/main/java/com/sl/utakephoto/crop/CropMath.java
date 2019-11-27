/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;

import java.util.Arrays;

public class CropMath {

    /**
     * Gets a float array of the 2D coordinates representing a rectangles
     * corners.
     * The order of the corners in the float array is:
     * 0------->1
     * ^        |
     * |        v
     * 3<-------2
     *
     * @param r the rectangle to get the corners of
     * @return the float array of corners (8 floats)
     */

    public static float[] getCornersFromRect(RectF r) {
        float[] corners = {
                r.left, r.top,
                r.right, r.top,
                r.right, r.bottom,
                r.left, r.bottom
        };
        return corners;
    }

    /**
     * Returns true iff point (x, y) is within or on the rectangle's bounds.
     * RectF's "contains" function treats points on the bottom and right bound
     * as not being contained.
     *
     * @param r the rectangle
     * @param x the x value of the point
     * @param y the y value of the point
     * @return
     */
    public static boolean inclusiveContains(RectF r, float x, float y) {
        return !(x > r.right || x < r.left || y > r.bottom || y < r.top);
    }

    /**
     * Takes an array of 2D coordinates representing corners and returns the
     * smallest rectangle containing those coordinates.
     *
     * @param array array of 2D coordinates
     * @return smallest rectangle containing coordinates
     */
    public static RectF trapToRect(float[] array) {
        RectF r = new RectF(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
        for (int i = 1; i < array.length; i += 2) {
            float x = array[i - 1];
            float y = array[i];
            r.left = (x < r.left) ? x : r.left;
            r.top = (y < r.top) ? y : r.top;
            r.right = (x > r.right) ? x : r.right;
            r.bottom = (y > r.bottom) ? y : r.bottom;
        }
        r.sort();
        return r;
    }

    /**
     * If edge point [x, y] in array [x0, y0, x1, y1, ...] is outside of the
     * image bound rectangle, clamps it to the edge of the rectangle.
     *
     * @param imageBound the rectangle to clamp edge points to.
     * @param array      an array of points to clamp to the rectangle, gets set to
     *                   the clamped values.
     */
    public static void getEdgePoints(RectF imageBound, float[] array) {
        if (array.length < 2)
            return;
        for (int x = 0; x < array.length; x += 2) {
            array[x] = GeometryMathUtils.clamp(array[x], imageBound.left, imageBound.right);
            array[x + 1] = GeometryMathUtils.clamp(array[x + 1], imageBound.top, imageBound.bottom);
        }
    }

    /**
     * Takes a point and the corners of a rectangle and returns the two corners
     * representing the side of the rectangle closest to the point.
     *
     * @param point   the point which is being checked
     * @param corners the corners of the rectangle
     * @return two corners representing the side of the rectangle
     */
    public static float[] closestSide(float[] point, float[] corners) {
        int len = corners.length;
        float oldMag = Float.POSITIVE_INFINITY;
        float[] bestLine = null;
        for (int i = 0; i < len; i += 2) {
            float[] line = {
                    corners[i], corners[(i + 1) % len],
                    corners[(i + 2) % len], corners[(i + 3) % len]
            };
            float mag = GeometryMathUtils.vectorLength(
                    GeometryMathUtils.shortestVectorFromPointToLine(point, line));
            if (mag < oldMag) {
                oldMag = mag;
                bestLine = line;
            }
        }
        return bestLine;
    }

    /**
     * Checks if a given point is within a rotated rectangle.
     *
     * @param point 2D point to check
     * @param bound rectangle to rotate
     * @param rot   angle of rotation about rectangle center
     * @return true if point is within rotated rectangle
     */
    public static boolean pointInRotatedRect(float[] point, RectF bound, float rot) {
        Matrix m = new Matrix();
        float[] p = Arrays.copyOf(point, 2);
        m.setRotate(rot, bound.centerX(), bound.centerY());
        Matrix m0 = new Matrix();
        if (!m.invert(m0))
            return false;
        m0.mapPoints(p);
        return inclusiveContains(bound, p[0], p[1]);
    }

    /**
     * Checks if a given point is within a rotated rectangle.
     *
     * @param point       2D point to check
     * @param rotatedRect corners of a rotated rectangle
     * @param center      center of the rotated rectangle
     * @return true if point is within rotated rectangle
     */
    public static boolean pointInRotatedRect(float[] point, float[] rotatedRect, float[] center) {
        RectF unrotated = new RectF();
        float angle = getUnrotated(rotatedRect, center, unrotated);
        return pointInRotatedRect(point, unrotated, angle);
    }

    /**
     * Resizes rectangle to have a certain aspect ratio (center remains
     * stationary).
     *
     * @param r rectangle to resize
     * @param w new width aspect
     * @param h new height aspect
     */
    public static void fixAspectRatio(RectF r, float w, float h) {
        float scale = Math.min(r.width() / w, r.height() / h);
        float centX = r.centerX();
        float centY = r.centerY();
        float hw = scale * w / 2;
        float hh = scale * h / 2;
        r.set(centX - hw, centY - hh, centX + hw, centY + hh);
    }

    /**
     * Resizes rectangle to have a certain aspect ratio (center remains
     * stationary) while constraining it to remain within the original rect.
     *
     * @param r rectangle to resize
     * @param w new width aspect
     * @param h new height aspect
     */
    public static void fixAspectRatioContained(RectF r, float w, float h) {
        float origW = r.width();
        float origH = r.height();
        float origA = origW / origH;
        float a = w / h;
        float finalW = origW;
        float finalH = origH;
        if (origA < a) {
            finalH = origW / a;
            r.top = r.centerY() - finalH / 2;
            r.bottom = r.top + finalH;
        } else {
            finalW = origH * a;
            r.left = r.centerX() - finalW / 2;
            r.right = r.left + finalW;
        }

    }

    /**
     * Resizes rectangle to have a certain aspect ratio (center remains
     * stationary) while constraining it to remain within the original rect.
     *
     * @param r rectangle to resize
     * @param w new width aspect
     * @param h new height aspect
     */
    public static void fixScaleAspectRatioContained(RectF r, float w, float h) {
        float origW = r.width();
        float origH = r.height();
        float origA = origW / origH;
        float a = w / h;
        float finalW = origW;
        float finalH = origH;
        if (origA < a) {
            finalH = origW / a;
            r.top = r.centerY() - finalH / 2;
            r.bottom = r.top + finalH;
        } else {
            finalW = origH * a;
            r.left = r.centerX() - finalW / 2;
            r.right = r.left + finalW;
        }
        r.left=r.centerX()-r.width()*0.35f;
        r.right=r.centerX()+r.width()*0.35f;
        r.top=r.centerY()-r.height()*0.35f;
        r.bottom=r.centerY()+r.height()*0.35f;

    }

    /**
     * Stretches/Scales/Translates photoBounds to match displayBounds, and
     * and returns an equivalent stretched/scaled/translated cropBounds or null
     * if the mapping is invalid.
     *
     * @param cropBounds    cropBounds to transform
     * @param photoBounds   original bounds containing crop bounds
     * @param displayBounds final bounds for crop
     * @return the stretched/scaled/translated crop bounds that fit within displayBounds
     */
    public static RectF getScaledCropBounds(RectF cropBounds, RectF photoBounds,
                                            RectF displayBounds) {
        Matrix m = new Matrix();
        m.setRectToRect(photoBounds, displayBounds, Matrix.ScaleToFit.FILL);
        RectF trueCrop = new RectF(cropBounds);
        if (!m.mapRect(trueCrop)) {
            return null;
        }
        return trueCrop;
    }

    /**
     * Returns the size of a bitmap in bytes.
     *
     * @param bmap bitmap whose size to check
     * @return bitmap size in bytes
     */
    public static int getBitmapSize(Bitmap bmap) {
        return bmap.getRowBytes() * bmap.getHeight();
    }

    /**
     * Constrains rotation to be in [0, 90, 180, 270] rounding down.
     *
     * @param rotation any rotation value, in degrees
     * @return integer rotation in [0, 90, 180, 270]
     */
    public static int constrainedRotation(float rotation) {
        int r = (int) ((rotation % 360) / 90);
        r = (r < 0) ? (r + 4) : r;
        return r * 90;
    }

    private static float getUnrotated(float[] rotatedRect, float[] center, RectF unrotated) {
        float dy = rotatedRect[1] - rotatedRect[3];
        float dx = rotatedRect[0] - rotatedRect[2];
        float angle = (float) (Math.atan(dy / dx) * 180 / Math.PI);
        Matrix m = new Matrix();
        m.setRotate(-angle, center[0], center[1]);
        float[] unrotatedRect = new float[rotatedRect.length];
        m.mapPoints(unrotatedRect, rotatedRect);
        unrotated.set(trapToRect(unrotatedRect));
        return angle;
    }

}
