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

import com.xiaomi.micolauncher.feature.appmainscreen.InstallShortcutReceiver;
import com.xiaomi.micolauncher.feature.appmainscreen.LauncherState;
import com.xiaomi.micolauncher.feature.appmainscreen.MainAppListFragment;
import com.xiaomi.micolauncher.feature.appmainscreen.Workspace;

/**
 * Definition for spring loaded state used during drag and drop.
 */
public class FinishSelectState extends LauncherState {

    private static final int STATE_FLAGS = FLAG_MULTI_PAGE |
            FLAG_DISABLE_ACCESSIBILITY | FLAG_DISABLE_RESTORE | FLAG_WORKSPACE_ICONS_CAN_BE_DRAGGED |
            FLAG_DISABLE_PAGE_CLIPPING | FLAG_PAGE_BACKGROUNDS | FLAG_HIDE_BACK_BUTTON;

    public FinishSelectState(int id) {
        super(id, 0, SPRING_LOADED_TRANSITION_MS, STATE_FLAGS);
    }

    @Override
    public float[] getWorkspaceScaleAndTranslation(MainAppListFragment launcher) {
        return new float[] {1, 1, 0};
    }

    @Override
    public void onStateEnabled(MainAppListFragment launcher) {
        Workspace ws = launcher.getWorkspace();
        ws.showPageIndicatorAtCurrentScroll();
        if (null != ws.getPageIndicator()) {
            ws.getPageIndicator().setShouldAutoHide(false);
        }

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
        if (null != launcher.getWorkspace().getPageIndicator()) {
            launcher.getWorkspace().getPageIndicator().setShouldAutoHide(true);
        }

        // Re-enable any Un/InstallShortcutReceiver and now process any queued items
        InstallShortcutReceiver.disableAndFlushInstallQueue(
                InstallShortcutReceiver.FLAG_DRAG_AND_DROP, launcher.getActivity());
    }
}
