/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.xiaomi.micolauncher.feature.appmainscreen;

import static com.xiaomi.micolauncher.feature.appmainscreen.anim.AlphaUpdateListener.updateVisibility;
import static com.xiaomi.micolauncher.feature.appmainscreen.anim.Interpolators.OVERSHOOT_1_2;

import android.animation.TimeInterpolator;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewPropertyAnimator;
import android.widget.FrameLayout;

import com.xiaomi.micolauncher.feature.appmainscreen.anim.Interpolators;
import com.xiaomi.micolauncher.feature.appmainscreen.dragndrop.DragController;
import com.xiaomi.micolauncher.feature.appmainscreen.dragndrop.DragController.DragListener;
import com.xiaomi.micolauncher.feature.appmainscreen.dragndrop.DragOptions;

import java.util.ArrayList;
import java.util.List;

/*
 * The top bar containing various drop targets: Delete/App Info/Uninstall.
 */
public class DropTargetBar extends FrameLayout
        implements DragListener, Insettable, DropTargetMultiListener {

    protected static final int DEFAULT_DRAG_FADE_DURATION = 200;
    protected static final TimeInterpolator DEFAULT_INTERPOLATOR = Interpolators.ACCEL;

    private final Runnable mFadeAnimationEndRunnable =
            () -> updateVisibility(DropTargetBar.this);

    @ViewDebug.ExportedProperty(category = "launcher")
    protected boolean mDeferOnDragEnd;

    @ViewDebug.ExportedProperty(category = "launcher")
    protected boolean mVisible = false;

    private ButtonDropTarget[] mDropTargets;
    private ViewPropertyAnimator mCurrentAnimation;

    private boolean mIsVertical = true;
    private int mMarginPx;
    private List<ShortcutInfo> mList = new ArrayList<>();
    private boolean mIsMultiSelect;

    public DropTargetBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DropTargetBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mDropTargets = new ButtonDropTarget[getChildCount()];
        for (int i = 0; i < mDropTargets.length; i++) {
            mDropTargets[i] = (ButtonDropTarget) getChildAt(i);
            mDropTargets[i].setDropTargetBar(this);
        }
//        setBackgroundColor(Color.parseColor("#000000"));
    }

    @Override
    public void setInsets(Rect insets) {
        LayoutParams lp = (LayoutParams) getLayoutParams();
        DeviceProfile grid = MainAppListFragment.getLauncher(getContext()).getDeviceProfile();
        mIsVertical = grid.isVerticalBarLayout();
        Log.d("DropTargetBar", "setInsets: " + mIsVertical);
        lp.leftMargin = insets.left;
        lp.topMargin = getResources().getDimensionPixelSize(R.dimen.dp_30);
        lp.bottomMargin = insets.bottom;
        lp.rightMargin = insets.right;

        lp.width = grid.dropTargetBarWidthPx;
        lp.height = grid.dropTargetBarSizePx;
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        mMarginPx = grid.dropTargetBarMarginPx;
    }

    public void setup(DragController dragController) {
        dragController.addDragListener(this);
        for (int i = 0; i < mDropTargets.length; i++) {
            dragController.addDragListener(mDropTargets[i]);
            dragController.addDropTarget(mDropTargets[i]);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        int widthSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        int heightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);

        for (ButtonDropTarget button : mDropTargets) {
            if (button.getVisibility() != GONE) {
                button.setTextVisible(false);
                button.measure(widthSpec, heightSpec);
            }
        }
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int start = 0;
        int end;

        for (ButtonDropTarget button : mDropTargets) {
            if (button.getVisibility() != GONE) {
                end = start + button.getMeasuredWidth();
                button.layout(start, 0, end, button.getMeasuredHeight());
                start = end + mMarginPx;
            }
        }
    }

    private int getVisibleButtonsCount() {
        int visibleCount = 0;
        for (ButtonDropTarget buttons : mDropTargets) {
            if (buttons.getVisibility() != GONE) {
                visibleCount++;
            }
        }
        return visibleCount;
    }

    private void animateToVisibility(boolean isVisible) {
        if (mVisible != isVisible) {
            mVisible = isVisible;

            // Cancel any existing animation
            if (mCurrentAnimation != null) {
                mCurrentAnimation.cancel();
                mCurrentAnimation = null;
            }

            float finalAlpha = mVisible ? 1 : 0;
            if (Float.compare(getAlpha(), finalAlpha) != 0) {
                setVisibility(View.VISIBLE);
                mCurrentAnimation = animate().alpha(finalAlpha)
                        .setInterpolator(OVERSHOOT_1_2)
                        .setDuration(DEFAULT_DRAG_FADE_DURATION)
                        .withEndAction(mFadeAnimationEndRunnable);
            }

        }
    }

    /*
     * DragController.DragListener implementation
     */
    @Override
    public void onDragStart(DropTarget.DragObject dragObject, DragOptions options) {
        animateToVisibility(true);
    }

    /**
     * This is called to defer hiding the delete drop target until the drop animation has completed,
     * instead of hiding immediately when the drag has ended.
     */
    protected void deferOnDragEnd() {
        mDeferOnDragEnd = true;
    }

    @Override
    public void onDragEnd() {
        if (!mDeferOnDragEnd) {
            animateToVisibility(false);
        } else {
            mDeferOnDragEnd = false;
        }
    }

    public ButtonDropTarget[] getDropTargets() {
        return mDropTargets;
    }

    @Override
    public void gotoMultiSelect() {
        mIsMultiSelect = true;
        animateToVisibility(true);
        for (ButtonDropTarget dropTarget : getDropTargets()) {
            dropTarget.gotoMultiSelect();
        }
    }

    @Override
    public void cancelMultiSelect() {
        mIsMultiSelect = false;
        mList.clear();
        onDragEnd();
    }

    public boolean isIsMultiSelect() {
        return mIsMultiSelect;
    }

    public List<ShortcutInfo> getList() {
        return mList;
    }

    public void addOrRemoveSelectInfo(ShortcutInfo shortcutInfo, boolean isAdd) {
        if (isAdd) {
            mList.add(shortcutInfo);
        } else {
            mList.remove(shortcutInfo);
        }
        if (mList.size() > 0) {
            mDropTargets[2].setVisibility(View.VISIBLE);
            if (mList.size() > 1) {
                mDropTargets[1].setVisibility(View.VISIBLE);
            } else {
                mDropTargets[1].setVisibility(View.INVISIBLE);
            }
        } else {
            mDropTargets[1].setVisibility(View.INVISIBLE);
            mDropTargets[2].setVisibility(View.INVISIBLE);
        }
    }
}
