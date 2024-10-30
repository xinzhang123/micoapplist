package com.xiaomi.micolauncher.feature.appmainscreen;

import android.view.View;


/**
 * A specialized listener for Overview buttons where both clicks and long clicks are logged
 * handled the same via {@link #handleViewClick(View)}.
 */
public abstract class OverviewButtonClickListener implements View.OnClickListener,
        View.OnLongClickListener {

    private int mControlType;

    public OverviewButtonClickListener(int controlType) {
        mControlType = controlType;
    }

    public void attachTo(View v) {
        v.setOnClickListener(this);
        v.setOnLongClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (shouldPerformClick(view)) {
            handleViewClick(view, 0);
        }
    }

    @Override
    public boolean onLongClick(View view) {
        if (shouldPerformClick(view)) {
            handleViewClick(view, 1);
        }
        return true;
    }

    private boolean shouldPerformClick(View view) {
        return !MainAppListFragment.getLauncher(view.getContext()).getWorkspace().isSwitchingState();
    }

    private void handleViewClick(View view, int action) {
        handleViewClick(view);
    }

    public abstract void handleViewClick(View view);
}