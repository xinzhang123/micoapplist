/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.xiaomi.micolauncher.feature.appmainscreen.states;

import static com.xiaomi.micolauncher.feature.appmainscreen.LauncherAnimUtils.SPRING_LOADED_TRANSITION_MS;
import static com.xiaomi.micolauncher.feature.appmainscreen.states.RotationHelper.REQUEST_LOCK;

import android.graphics.Rect;
import android.util.Log;

import com.xiaomi.micolauncher.feature.appmainscreen.DeviceProfile;
import com.xiaomi.micolauncher.feature.appmainscreen.InstallShortcutReceiver;
import com.xiaomi.micolauncher.feature.appmainscreen.Launcher;
import com.xiaomi.micolauncher.feature.appmainscreen.LauncherState;
import com.xiaomi.micolauncher.feature.appmainscreen.MainAppListFragment;
import com.xiaomi.micolauncher.feature.appmainscreen.Workspace;

/**
 * Definition for spring loaded state used during drag and drop.
 */
public class SpringLoadedState extends LauncherState {

    private static final int STATE_FLAGS = FLAG_MULTI_PAGE |
            FLAG_DISABLE_ACCESSIBILITY | FLAG_DISABLE_RESTORE | FLAG_WORKSPACE_ICONS_CAN_BE_DRAGGED |
            FLAG_DISABLE_PAGE_CLIPPING | FLAG_PAGE_BACKGROUNDS | FLAG_HIDE_BACK_BUTTON;

    public SpringLoadedState(int id) {
        super(id, 0, SPRING_LOADED_TRANSITION_MS, STATE_FLAGS);
    }

    @Override
    public float[] getWorkspaceScaleAndTranslation(MainAppListFragment launcher) {
        DeviceProfile grid = launcher.getDeviceProfile();
        Workspace ws = launcher.getWorkspace();
        if (ws.getChildCount() == 0) {
            return super.getWorkspaceScaleAndTranslation(launcher);
        }

        if (grid.isVerticalBarLayout()) {
            float scale = grid.workspaceSpringLoadShrinkFactor;
            return new float[] {scale, 1, 0};
        }

        Log.d("123", "getWorkspaceScaleAndTranslation: grid.workspaceSpringLoadShrinkFactor === " + grid.workspaceSpringLoadShrinkFactor);
        float scale = grid.workspaceSpringLoadShrinkFactor;
        Rect insets = launcher.getDragLayer().getInsets();

        float scaledHeight = scale * ws.getNormalChildHeight();
        float shrunkTop = insets.top + grid.dropTargetBarSizePx;
        float shrunkBottom = ws.getMeasuredHeight() - insets.bottom
                - grid.workspacePadding.bottom
                - grid.workspaceSpringLoadedBottomSpace;
        float totalShrunkSpace = shrunkBottom - shrunkTop;

        float desiredCellTop = shrunkTop + (totalShrunkSpace - scaledHeight) / 2;

        float halfHeight = ws.getHeight() / 2;
        float myCenter = ws.getTop() + halfHeight;
        float cellTopFromCenter = halfHeight - ws.getChildAt(0).getTop();
        float actualCellTop = myCenter - cellTopFromCenter * scale;
        return new float[] { scale, 1, (desiredCellTop - actualCellTop) / scale};
    }

    @Override
    public void onStateEnabled(MainAppListFragment launcher) {
        Workspace ws = launcher.getWorkspace();
        ws.showPageIndicatorAtCurrentScroll();
        ws.getPageIndicator().setShouldAutoHide(false);

        // Prevent any Un/InstallShortcutReceivers from updating the db while we are
        // in spring loaded mode
        InstallShortcutReceiver.enableInstallQueue(InstallShortcutReceiver.FLAG_DRAG_AND_DROP);
        launcher.getRotationHelper().setCurrentStateRequest(REQUEST_LOCK);
    }

    @Override
    public float getWorkspaceScrimAlpha(MainAppListFragment launcher) {
        return 0.3f;
    }

    @Override
    public void onStateDisabled(final MainAppListFragment launcher) {
        launcher.getWorkspace().getPageIndicator().setShouldAutoHide(true);

        // Re-enable any Un/InstallShortcutReceiver and now process any queued items
        InstallShortcutReceiver.disableAndFlushInstallQueue(
                InstallShortcutReceiver.FLAG_DRAG_AND_DROP, launcher.getActivity());
    }
}
