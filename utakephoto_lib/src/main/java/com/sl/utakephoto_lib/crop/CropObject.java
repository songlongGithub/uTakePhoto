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

package com.sl.utakephoto_lib.crop;

import android.graphics.Rect;
import android.graphics.RectF;


public class CropObject {
    private BoundedRect mBoundedRect;
    private float mAspectWidth = 1;
    private float mAspectHeight = 1;
    private boolean mFixAspectRatio = false;
    private float mRotation = 0;
    private float mTouchTolerance = 45;
    private float mMinSideSize = 20;

    public static final int MOVE_NONE = 0;
    // Sides
    public static final int MOVE_LEFT = 1;
    public static final int MOVE_TOP = 2;
    public static final int MOVE_RIGHT = 4;
    public static final int MOVE_BOTTOM = 8;
    public static final int MOVE_BLOCK = 16;

    // Corners
    public static final int TOP_LEFT = MOVE_TOP | MOVE_LEFT;
    public static final int TOP_RIGHT = MOVE_TOP | MOVE_RIGHT;
    public static final int BOTTOM_RIGHT = MOVE_BOTTOM | MOVE_RIGHT;
    public static final int BOTTOM_LEFT = MOVE_BOTTOM | MOVE_LEFT;

    private int mMovingEdges = MOVE_NONE;

    public CropObject(Rect outerBound, Rect innerBound, int outerAngle) {
        mBoundedRect = new BoundedRect(outerAngle % 360, outerBound, innerBound);
    }

    public CropObject(RectF outerBound, RectF innerBound, int outerAngle) {
        mBoundedRect = new BoundedRect(outerAngle % 360, outerBound, innerBound);
    }

    public void resetBoundsTo(RectF inner, RectF outer) {
        mBoundedRect.resetTo(0, outer, inner);
    }

    public void getInnerBounds(RectF r) {
        mBoundedRect.setToInner(r);
    }

    public void getOuterBounds(RectF r) {
        mBoundedRect.setToOuter(r);
    }

    public RectF getInnerBounds() {
        return mBoundedRect.getInner();
    }

    public RectF getOuterBounds() {
        return mBoundedRect.getOuter();
    }

    public int getSelectState() {
        return mMovingEdges;
    }

    public boolean isFixedAspect() {
        return mFixAspectRatio;
    }

    public void rotateOuter(int angle) {
        mRotation = angle % 360;
        mBoundedRect.setRotation(mRotation);
        clearSelectState();
    }

    public boolean setInnerAspectRatio(float width, float height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Width and Height must be greater than zero");
        }
        RectF inner = mBoundedRect.getInner();
        CropMath.fixAspectRatioContained(inner, width, height);
        if (inner.width() < mMinSideSize || inner.height() < mMinSideSize) {
            return false;
        }
        mAspectWidth = width;
        mAspectHeight = height;
        mFixAspectRatio = true;
        mBoundedRect.setInner(inner);
        clearSelectState();
        return true;
    }

    public void setTouchTolerance(float tolerance) {
        if (tolerance <= 0) {
            throw new IllegalArgumentException("Tolerance must be greater than zero");
        }
        mTouchTolerance = tolerance;
    }

    public void setMinInnerSideSize(float minSide) {
        if (minSide <= 0) {
            throw new IllegalArgumentException("Min dide must be greater than zero");
        }
        mMinSideSize = minSide;
    }

    public void unsetAspectRatio() {
        mFixAspectRatio = false;
        clearSelectState();
    }

    public boolean hasSelectedEdge() {
        return mMovingEdges != MOVE_NONE;
    }

    public static boolean checkCorner(int selected) {
        return selected == TOP_LEFT || selected == TOP_RIGHT || selected == BOTTOM_RIGHT
                || selected == BOTTOM_LEFT;
    }

    public static boolean checkEdge(int selected) {
        return selected == MOVE_LEFT || selected == MOVE_TOP || selected == MOVE_RIGHT
                || selected == MOVE_BOTTOM;
    }

    public static boolean checkBlock(int selected) {
        return selected == MOVE_BLOCK;
    }

    public static boolean checkValid(int selected) {
        return selected == MOVE_NONE || checkBlock(selected) || checkEdge(selected)
                || checkCorner(selected);
    }

    public void clearSelectState() {
        mMovingEdges = MOVE_NONE;
    }

    public int wouldSelectEdge(float x, float y) {
        int edgeSelected = calculateSelectedEdge(x, y);
        if (edgeSelected != MOVE_NONE && edgeSelected != MOVE_BLOCK) {
            return edgeSelected;
        }
        return MOVE_NONE;
    }

    public boolean selectEdge(int edge) {
        if (!checkValid(edge)) {
            // temporary
            throw new IllegalArgumentException("bad edge selected");
            // return false;
        }
        if ((mFixAspectRatio && !checkCorner(edge)) && !checkBlock(edge) && edge != MOVE_NONE) {
            // temporary
            throw new IllegalArgumentException("bad corner selected");
            // return false;
        }
        mMovingEdges = edge;
        return true;
    }

    public boolean selectEdge(float x, float y) {
        int edgeSelected = calculateSelectedEdge(x, y);
        if (mFixAspectRatio) {
            edgeSelected = fixEdgeToCorner(edgeSelected);
        }
        if (edgeSelected == MOVE_NONE) {
            return false;
        }
        return selectEdge(edgeSelected);
    }

    public boolean moveCurrentSelection(float dX, float dY) {
        if (mMovingEdges == MOVE_NONE) {
            return false;
        }
        RectF crop = mBoundedRect.getInner();

        float minWidthHeight = mMinSideSize;

        int movingEdges = mMovingEdges;
        if (movingEdges == MOVE_BLOCK) {
            mBoundedRect.moveInner(dX, dY);
            return true;
        } else {
            float dx = 0;
            float dy = 0;

            if ((movingEdges & MOVE_LEFT) != 0) {
                dx = Math.min(crop.left + dX, crop.right - minWidthHeight) - crop.left;
            }
            if ((movingEdges & MOVE_TOP) != 0) {
                dy = Math.min(crop.top + dY, crop.bottom - minWidthHeight) - crop.top;
            }
            if ((movingEdges & MOVE_RIGHT) != 0) {
                dx = Math.max(crop.right + dX, crop.left + minWidthHeight)
                        - crop.right;
            }
            if ((movingEdges & MOVE_BOTTOM) != 0) {
                dy = Math.max(crop.bottom + dY, crop.top + minWidthHeight)
                        - crop.bottom;
            }

            if (mFixAspectRatio) {
                float[] l1 = {
                        crop.left, crop.bottom
                };
                float[] l2 = {
                        crop.right, crop.top
                };
                if (movingEdges == TOP_LEFT || movingEdges == BOTTOM_RIGHT) {
                    l1[1] = crop.top;
                    l2[1] = crop.bottom;
                }
                float[] b = {
                        l1[0] - l2[0], l1[1] - l2[1]
                };
                float[] disp = {
                        dx, dy
                };
                float[] bUnit = GeometryMathUtils.normalize(b);
                float sp = GeometryMathUtils.scalarProjection(disp, bUnit);
                dx = sp * bUnit[0];
                dy = sp * bUnit[1];
                RectF newCrop = fixedCornerResize(crop, movingEdges, dx, dy);

                mBoundedRect.fixedAspectResizeInner(newCrop);
            } else {
                if ((movingEdges & MOVE_LEFT) != 0) {
                    crop.left += dx;
                }
                if ((movingEdges & MOVE_TOP) != 0) {
                    crop.top += dy;
                }
                if ((movingEdges & MOVE_RIGHT) != 0) {
                    crop.right += dx;
                }
                if ((movingEdges & MOVE_BOTTOM) != 0) {
                    crop.bottom += dy;
                }
                mBoundedRect.resizeInner(crop);
            }
        }
        return true;
    }

    // Helper methods

    private int calculateSelectedEdge(float x, float y) {
        RectF cropped = mBoundedRect.getInner();

        float left = Math.abs(x - cropped.left);
        float right = Math.abs(x - cropped.right);
        float top = Math.abs(y - cropped.top);
        float bottom = Math.abs(y - cropped.bottom);

        int edgeSelected = MOVE_NONE;
        // Check left or right.
        if ((left <= mTouchTolerance) && ((y + mTouchTolerance) >= cropped.top)
                && ((y - mTouchTolerance) <= cropped.bottom) && (left < right)) {
            edgeSelected |= MOVE_LEFT;
        }
        else if ((right <= mTouchTolerance) && ((y + mTouchTolerance) >= cropped.top)
                && ((y - mTouchTolerance) <= cropped.bottom)) {
            edgeSelected |= MOVE_RIGHT;
        }

        // Check top or bottom.
        if ((top <= mTouchTolerance) && ((x + mTouchTolerance) >= cropped.left)
                && ((x - mTouchTolerance) <= cropped.right) && (top < bottom)) {
            edgeSelected |= MOVE_TOP;
        }
        else if ((bottom <= mTouchTolerance) && ((x + mTouchTolerance) >= cropped.left)
                && ((x - mTouchTolerance) <= cropped.right)) {
            edgeSelected |= MOVE_BOTTOM;
        }
        return edgeSelected;
    }

    private static RectF fixedCornerResize(RectF r, int moving_corner, float dx, float dy) {
        RectF newCrop = null;
        // Fix opposite corner in place and move sides
        if (moving_corner == BOTTOM_RIGHT) {
            newCrop = new RectF(r.left, r.top, r.left + r.width() + dx, r.top + r.height()
                    + dy);
        } else if (moving_corner == BOTTOM_LEFT) {
            newCrop = new RectF(r.right - r.width() + dx, r.top, r.right, r.top + r.height()
                    + dy);
        } else if (moving_corner == TOP_LEFT) {
            newCrop = new RectF(r.right - r.width() + dx, r.bottom - r.height() + dy,
                    r.right, r.bottom);
        } else if (moving_corner == TOP_RIGHT) {
            newCrop = new RectF(r.left, r.bottom - r.height() + dy, r.left
                    + r.width() + dx, r.bottom);
        }
        return newCrop;
    }

    private static int fixEdgeToCorner(int moving_edges) {
        if (moving_edges == MOVE_LEFT) {
            moving_edges |= MOVE_TOP;
        }
        if (moving_edges == MOVE_TOP) {
            moving_edges |= MOVE_LEFT;
        }
        if (moving_edges == MOVE_RIGHT) {
            moving_edges |= MOVE_BOTTOM;
        }
        if (moving_edges == MOVE_BOTTOM) {
            moving_edges |= MOVE_RIGHT;
        }
        return moving_edges;
    }

}
