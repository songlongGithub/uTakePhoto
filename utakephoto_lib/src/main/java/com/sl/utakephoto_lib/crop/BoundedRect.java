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
package com.sl.utakephoto_lib.crop;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;

import java.util.Arrays;

/**
 * Maintains invariant that inner rectangle is constrained to be within the
 * outer, rotated rectangle.
 */
public class BoundedRect {
    private float rot;
    private RectF outer;
    private RectF inner;
    private float[] innerRotated;

    public BoundedRect(float rotation, Rect outerRect, Rect innerRect) {
        rot = rotation;
        outer = new RectF(outerRect);
        inner = new RectF(innerRect);
        innerRotated = CropMath.getCornersFromRect(inner);
        rotateInner();
        if (!isConstrained())
            reconstrain();
    }

    public BoundedRect(float rotation, RectF outerRect, RectF innerRect) {
        rot = rotation;
        outer = new RectF(outerRect);
        inner = new RectF(innerRect);
        innerRotated = CropMath.getCornersFromRect(inner);
        rotateInner();
        if (!isConstrained())
            reconstrain();
    }

    public void resetTo(float rotation, RectF outerRect, RectF innerRect) {
        rot = rotation;
        outer.set(outerRect);
        inner.set(innerRect);
        innerRotated = CropMath.getCornersFromRect(inner);
        rotateInner();
        if (!isConstrained())
            reconstrain();
    }

    /**
     * Sets inner, and re-constrains it to fit within the rotated bounding rect.
     */
    public void setInner(RectF newInner) {
        if (inner.equals(newInner))
            return;
        inner = newInner;
        innerRotated = CropMath.getCornersFromRect(inner);
        rotateInner();
        if (!isConstrained())
            reconstrain();
    }

    /**
     * Sets rotation, and re-constrains inner to fit within the rotated bounding rect.
     */
    public void setRotation(float rotation) {
        if (rotation == rot)
            return;
        rot = rotation;
        innerRotated = CropMath.getCornersFromRect(inner);
        rotateInner();
        if (!isConstrained())
            reconstrain();
    }

    public void setToInner(RectF r) {
        r.set(inner);
    }

    public void setToOuter(RectF r) {
        r.set(outer);
    }

    public RectF getInner() {
        return new RectF(inner);
    }

    public RectF getOuter() {
        return new RectF(outer);
    }

    /**
     * Tries to move the inner rectangle by (dx, dy).  If this would cause it to leave
     * the bounding rectangle, snaps the inner rectangle to the edge of the bounding
     * rectangle.
     */
    public void moveInner(float dx, float dy) {
        Matrix m0 = getInverseRotMatrix();

        RectF translatedInner = new RectF(inner);
        translatedInner.offset(dx, dy);

        float[] translatedInnerCorners = CropMath.getCornersFromRect(translatedInner);
        float[] outerCorners = CropMath.getCornersFromRect(outer);

        m0.mapPoints(translatedInnerCorners);
        float[] correction = {
                0, 0
        };

        // find correction vectors for corners that have moved out of bounds
        for (int i = 0; i < translatedInnerCorners.length; i += 2) {
            float correctedInnerX = translatedInnerCorners[i] + correction[0];
            float correctedInnerY = translatedInnerCorners[i + 1] + correction[1];
            if (!CropMath.inclusiveContains(outer, correctedInnerX, correctedInnerY)) {
                float[] badCorner = {
                        correctedInnerX, correctedInnerY
                };
                float[] nearestSide = CropMath.closestSide(badCorner, outerCorners);
                float[] correctionVec =
                        GeometryMathUtils.shortestVectorFromPointToLine(badCorner, nearestSide);
                correction[0] += correctionVec[0];
                correction[1] += correctionVec[1];
            }
        }

        for (int i = 0; i < translatedInnerCorners.length; i += 2) {
            float correctedInnerX = translatedInnerCorners[i] + correction[0];
            float correctedInnerY = translatedInnerCorners[i + 1] + correction[1];
            if (!CropMath.inclusiveContains(outer, correctedInnerX, correctedInnerY)) {
                float[] correctionVec = {
                        correctedInnerX, correctedInnerY
                };
                CropMath.getEdgePoints(outer, correctionVec);
                correctionVec[0] -= correctedInnerX;
                correctionVec[1] -= correctedInnerY;
                correction[0] += correctionVec[0];
                correction[1] += correctionVec[1];
            }
        }

        // Set correction
        for (int i = 0; i < translatedInnerCorners.length; i += 2) {
            float correctedInnerX = translatedInnerCorners[i] + correction[0];
            float correctedInnerY = translatedInnerCorners[i + 1] + correction[1];
            // update translated corners with correction vectors
            translatedInnerCorners[i] = correctedInnerX;
            translatedInnerCorners[i + 1] = correctedInnerY;
        }

        innerRotated = translatedInnerCorners;
        // reconstrain to update inner
        reconstrain();
    }

    /**
     * Attempts to resize the inner rectangle.  If this would cause it to leave
     * the bounding rect, clips the inner rectangle to fit.
     */
    public void resizeInner(RectF newInner) {
        Matrix m = getRotMatrix();
        Matrix m0 = getInverseRotMatrix();

        float[] outerCorners = CropMath.getCornersFromRect(outer);
        m.mapPoints(outerCorners);
        float[] oldInnerCorners = CropMath.getCornersFromRect(inner);
        float[] newInnerCorners = CropMath.getCornersFromRect(newInner);
        RectF ret = new RectF(newInner);

        for (int i = 0; i < newInnerCorners.length; i += 2) {
            float[] c = {
                    newInnerCorners[i], newInnerCorners[i + 1]
            };
            float[] c0 = Arrays.copyOf(c, 2);
            m0.mapPoints(c0);
            if (!CropMath.inclusiveContains(outer, c0[0], c0[1])) {
                float[] outerSide = CropMath.closestSide(c, outerCorners);
                float[] pathOfCorner = {
                        newInnerCorners[i], newInnerCorners[i + 1],
                        oldInnerCorners[i], oldInnerCorners[i + 1]
                };
                float[] p = GeometryMathUtils.lineIntersect(pathOfCorner, outerSide);
                if (p == null) {
                    // lines are parallel or not well defined, so don't resize
                    p = new float[2];
                    p[0] = oldInnerCorners[i];
                    p[1] = oldInnerCorners[i + 1];
                }
                // relies on corners being in same order as method
                // getCornersFromRect
                switch (i) {
                    case 0:
                    case 1:
                        ret.left = (p[0] > ret.left) ? p[0] : ret.left;
                        ret.top = (p[1] > ret.top) ? p[1] : ret.top;
                        break;
                    case 2:
                    case 3:
                        ret.right = (p[0] < ret.right) ? p[0] : ret.right;
                        ret.top = (p[1] > ret.top) ? p[1] : ret.top;
                        break;
                    case 4:
                    case 5:
                        ret.right = (p[0] < ret.right) ? p[0] : ret.right;
                        ret.bottom = (p[1] < ret.bottom) ? p[1] : ret.bottom;
                        break;
                    case 6:
                    case 7:
                        ret.left = (p[0] > ret.left) ? p[0] : ret.left;
                        ret.bottom = (p[1] < ret.bottom) ? p[1] : ret.bottom;
                        break;
                    default:
                        break;
                }
            }
        }
        float[] retCorners = CropMath.getCornersFromRect(ret);
        m0.mapPoints(retCorners);
        innerRotated = retCorners;
        // reconstrain to update inner
        reconstrain();
    }

    /**
     * Attempts to resize the inner rectangle.  If this would cause it to leave
     * the bounding rect, clips the inner rectangle to fit while maintaining
     * aspect ratio.
     */
    public void fixedAspectResizeInner(RectF newInner) {
        Matrix m = getRotMatrix();
        Matrix m0 = getInverseRotMatrix();

        float aspectW = inner.width();
        float aspectH = inner.height();
        float aspRatio = aspectW / aspectH;
        float[] corners = CropMath.getCornersFromRect(outer);

        m.mapPoints(corners);
        float[] oldInnerCorners = CropMath.getCornersFromRect(inner);
        float[] newInnerCorners = CropMath.getCornersFromRect(newInner);

        // find fixed corner
        int fixed = -1;
        if (inner.top == newInner.top) {
            if (inner.left == newInner.left)
                fixed = 0; // top left
            else if (inner.right == newInner.right)
                fixed = 2; // top right
        } else if (inner.bottom == newInner.bottom) {
            if (inner.right == newInner.right)
                fixed = 4; // bottom right
            else if (inner.left == newInner.left)
                fixed = 6; // bottom left
        }
        // no fixed corner, return without update
        if (fixed == -1)
            return;
        float widthSoFar = newInner.width();
        int moved = -1;
        for (int i = 0; i < newInnerCorners.length; i += 2) {
            float[] c = {
                    newInnerCorners[i], newInnerCorners[i + 1]
            };
            float[] c0 = Arrays.copyOf(c, 2);
            m0.mapPoints(c0);
            if (!CropMath.inclusiveContains(outer, c0[0], c0[1])) {
                moved = i;
                if (moved == fixed)
                    continue;
                float[] l2 = CropMath.closestSide(c, corners);
                float[] l1 = {
                        newInnerCorners[i], newInnerCorners[i + 1],
                        oldInnerCorners[i], oldInnerCorners[i + 1]
                };
                float[] p = GeometryMathUtils.lineIntersect(l1, l2);
                if (p == null) {
                    // lines are parallel or not well defined, so set to old
                    // corner
                    p = new float[2];
                    p[0] = oldInnerCorners[i];
                    p[1] = oldInnerCorners[i + 1];
                }
                // relies on corners being in same order as method
                // getCornersFromRect
                float fixed_x = oldInnerCorners[fixed];
                float fixed_y = oldInnerCorners[fixed + 1];
                float newWidth = Math.abs(fixed_x - p[0]);
                float newHeight = Math.abs(fixed_y - p[1]);
                newWidth = Math.max(newWidth, aspRatio * newHeight);
                if (newWidth < widthSoFar)
                    widthSoFar = newWidth;
            }
        }

        float heightSoFar = widthSoFar / aspRatio;
        RectF ret = new RectF(inner);
        if (fixed == 0) {
            ret.right = ret.left + widthSoFar;
            ret.bottom = ret.top + heightSoFar;
        } else if (fixed == 2) {
            ret.left = ret.right - widthSoFar;
            ret.bottom = ret.top + heightSoFar;
        } else if (fixed == 4) {
            ret.left = ret.right - widthSoFar;
            ret.top = ret.bottom - heightSoFar;
        } else if (fixed == 6) {
            ret.right = ret.left + widthSoFar;
            ret.top = ret.bottom - heightSoFar;
        }
        float[] retCorners = CropMath.getCornersFromRect(ret);
        m0.mapPoints(retCorners);
        innerRotated = retCorners;
        // reconstrain to update inner
        reconstrain();
    }

    // internal methods

    private boolean isConstrained() {
        for (int i = 0; i < 8; i += 2) {
            if (!CropMath.inclusiveContains(outer, innerRotated[i], innerRotated[i + 1]))
                return false;
        }
        return true;
    }

    private void reconstrain() {
        // innerRotated has been changed to have incorrect values
        CropMath.getEdgePoints(outer, innerRotated);
        Matrix m = getRotMatrix();
        float[] unrotated = Arrays.copyOf(innerRotated, 8);
        m.mapPoints(unrotated);
        inner = CropMath.trapToRect(unrotated);
    }

    private void rotateInner() {
        Matrix m = getInverseRotMatrix();
        m.mapPoints(innerRotated);
    }

    private Matrix getRotMatrix() {
        Matrix m = new Matrix();
        m.setRotate(rot, outer.centerX(), outer.centerY());
        return m;
    }

    private Matrix getInverseRotMatrix() {
        Matrix m = new Matrix();
        m.setRotate(-rot, outer.centerX(), outer.centerY());
        return m;
    }
}
