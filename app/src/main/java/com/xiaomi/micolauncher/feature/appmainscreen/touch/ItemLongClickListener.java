/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.xiaomi.micolauncher.feature.appmainscreen.touch;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.xiaomi.micolauncher.feature.appmainscreen.LauncherState.ALL_APPS;
import static com.xiaomi.micolauncher.feature.appmainscreen.LauncherState.NORMAL;
import static com.xiaomi.micolauncher.feature.appmainscreen.LauncherState.OVERVIEW;

import android.util.Log;
import android.view.View;
import android.view.View.OnLongClickListener;

import com.xiaomi.micolauncher.feature.appmainscreen.CellLayout;
import com.xiaomi.micolauncher.feature.appmainscreen.DeviceProfile;
import com.xiaomi.micolauncher.feature.appmainscreen.DropTarget;
import com.xiaomi.micolauncher.feature.appmainscreen.ItemInfo;
import com.xiaomi.micolauncher.feature.appmainscreen.Launcher;
import com.xiaomi.micolauncher.feature.appmainscreen.MainAppListFragment;
import com.xiaomi.micolauncher.feature.appmainscreen.dragndrop.DragController;
import com.xiaomi.micolauncher.feature.appmainscreen.dragndrop.DragOptions;
import com.xiaomi.micolauncher.feature.appmainscreen.folder.Folder;

/**
 * Class to handle long-clicks on workspace items and start drag as a result.
 */
public class ItemLongClickListener {

    public static OnLongClickListener INSTANCE_WORKSPACE =
            ItemLongClickListener::onWorkspaceItemLongClick;

    public static OnLongClickListener INSTANCE_ALL_APPS =
            ItemLongClickListener::onAllAppsItemLongClick;

    private static boolean onWorkspaceItemLongClick(View v) {
        Log.d("123", "onWorkspaceItemLongClick: ");
        MainAppListFragment launcher = MainAppListFragment.getLauncher(v.getContext());
        if (!canStartDrag(launcher)) return false;
        if (!launcher.isInState(NORMAL) && !launcher.isInState(OVERVIEW)) return false;
        if (!(v.getTag() instanceof ItemInfo)) return false;

        launcher.setWaitingForResult(null);
        beginDrag(v, launcher, (ItemInfo) v.getTag(), new DragOptions());
        return true;
    }

    public static void beginDrag(View v, MainAppListFragment launcher, ItemInfo info,
                                 DragOptions dragOptions) {
        if (info.container >= 0) {
            Folder folder = Folder.getOpen(launcher);
            if (folder != null) {
                if (!folder.getItemsInReadingOrder().contains(v)) {
                    folder.close(true);
                } else {
                    folder.startDrag(v, dragOptions);
                    return;
                }
            }
        }

        CellLayout.CellInfo longClickCellInfo = new CellLayout.CellInfo(v, info);
        launcher.getWorkspace().startDrag(longClickCellInfo, dragOptions);
    }

    private static boolean onAllAppsItemLongClick(View v) {
        MainAppListFragment launcher = MainAppListFragment.getLauncher(v.getContext());
        if (!canStartDrag(launcher)) return false;
        // When we have exited all apps or are in transition, disregard long clicks
        if (!launcher.isInState(ALL_APPS) && !launcher.isInState(OVERVIEW)) return false;
        if (launcher.getWorkspace().isSwitchingState()) return false;

        // Start the drag
        final DragController dragController = launcher.getDragController();
        dragController.addDragListener(new DragController.DragListener() {
            @Override
            public void onDragStart(DropTarget.DragObject dragObject, DragOptions options) {
                v.setVisibility(INVISIBLE);
            }

            @Override
            public void onDragEnd() {
                v.setVisibility(VISIBLE);
                dragController.removeDragListener(this);
            }
        });

        DeviceProfile grid = launcher.getDeviceProfile();
        DragOptions options = new DragOptions();
        options.intrinsicIconScaleFactor = (float) grid.allAppsIconSizePx / grid.iconSizePx;
        launcher.getWorkspace().beginDragShared(v, launcher.getAppsView(), options);
        return false;
    }

    public static boolean canStartDrag(MainAppListFragment launcher) {
        if (launcher == null) {
            return false;
        }
        // We prevent dragging when we are loading the workspace as it is possible to pick up a view
        // that is subsequently removed from the workspace in startBinding().
        if (launcher.isWorkspaceLocked()) return false;
        // Return early if an item is already being dragged (e.g. when long-pressing two shortcuts)
        if (launcher.getDragController().isDragging()) return false;

        return true;
    }
}
