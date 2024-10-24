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

import com.xiaomi.micolauncher.feature.appmainscreen.DeviceProfile;
import com.xiaomi.micolauncher.feature.appmainscreen.InstallShortcutReceiver;
import com.xiaomi.micolauncher.feature.appmainscreen.Launcher;
import com.xiaomi.micolauncher.feature.appmainscreen.LauncherState;
import com.xiaomi.micolauncher.feature.appmainscreen.Workspace;

/**
 * Definition for spring loaded state used during drag and drop.
 */
public class FolderOpenState extends LauncherState {

    private static final int STATE_FLAGS = FLAG_MULTI_PAGE |
            FLAG_DISABLE_ACCESSIBILITY | FLAG_DISABLE_RESTORE | FLAG_WORKSPACE_ICONS_CAN_BE_DRAGGED |
            FLAG_DISABLE_PAGE_CLIPPING | FLAG_PAGE_BACKGROUNDS | FLAG_HIDE_BACK_BUTTON;

    public FolderOpenState(int id) {
        super(id, 0, SPRING_LOADED_TRANSITION_MS, STATE_FLAGS);
    }

    @Override
    public float[] getWorkspaceScaleAndTranslation(Launcher launcher) {
        DeviceProfile grid = launcher.getDeviceProfile();
        Workspace ws = launcher.getWorkspace();
        if (ws.getChildCount() == 0) {
            return super.getWorkspaceScaleAndTranslation(launcher);
        }

        float scale = grid.workspaceSpringLoadShrinkFactor;
        return new float[] {scale, 0, 0};
    }

    @Override
    public void onStateEnabled(Launcher launcher) {
        Workspace ws = launcher.getWorkspace();
        ws.showPageIndicatorAtCurrentScroll();
        ws.getPageIndicator().setShouldAutoHide(false);

        // Prevent any Un/InstallShortcutReceivers from updating the db while we are
        // in spring loaded mode
        InstallShortcutReceiver.enableInstallQueue(InstallShortcutReceiver.FLAG_DRAG_AND_DROP);
        launcher.getRotationHelper().setCurrentStateRequest(REQUEST_LOCK);
    }

    @Override
    public float getWorkspaceScrimAlpha(Launcher launcher) {
        return 0.3f;
    }

    @Override
    public void onStateDisabled(final Launcher launcher) {
        launcher.getWorkspace().getPageIndicator().setShouldAutoHide(true);

        // Re-enable any Un/InstallShortcutReceiver and now process any queued items
        InstallShortcutReceiver.disableAndFlushInstallQueue(
                InstallShortcutReceiver.FLAG_DRAG_AND_DROP, launcher);
    }
}
