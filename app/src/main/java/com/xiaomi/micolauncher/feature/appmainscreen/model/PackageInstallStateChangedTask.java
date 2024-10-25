/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.xiaomi.micolauncher.feature.appmainscreen.model;

import android.content.ComponentName;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Process;

import com.xiaomi.micolauncher.feature.appmainscreen.AllAppsList;
import com.xiaomi.micolauncher.feature.appmainscreen.AppInfo;
import com.xiaomi.micolauncher.feature.appmainscreen.ItemInfo;
import com.xiaomi.micolauncher.feature.appmainscreen.LauncherAppState;
import com.xiaomi.micolauncher.feature.appmainscreen.LauncherAppWidgetInfo;
import com.xiaomi.micolauncher.feature.appmainscreen.LauncherModel.CallbackTask;
import com.xiaomi.micolauncher.feature.appmainscreen.LauncherModel.Callbacks;
import com.xiaomi.micolauncher.feature.appmainscreen.PromiseAppInfo;
import com.xiaomi.micolauncher.feature.appmainscreen.ShortcutInfo;
import com.xiaomi.micolauncher.feature.appmainscreen.compat.PackageInstallerCompat;
import com.xiaomi.micolauncher.feature.appmainscreen.compat.PackageInstallerCompat.PackageInstallInfo;
import com.xiaomi.micolauncher.feature.appmainscreen.util.InstantAppResolver;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Handles changes due to a sessions updates for a currently installing app.
 */
public class PackageInstallStateChangedTask extends BaseModelUpdateTask {

    private final PackageInstallInfo mInstallInfo;

    public PackageInstallStateChangedTask(PackageInstallInfo installInfo) {
        mInstallInfo = installInfo;
    }

    @Override
    public void execute(LauncherAppState app, BgDataModel dataModel, AllAppsList apps) {
        if (mInstallInfo.state == PackageInstallerCompat.STATUS_INSTALLED) {
            try {
                // For instant apps we do not get package-add. Use setting events to update
                // any pinned icons.
                ApplicationInfo ai = app.getContext()
                        .getPackageManager().getApplicationInfo(mInstallInfo.packageName, 0);
                if (InstantAppResolver.newInstance(app.getContext()).isInstantApp(ai)) {
                    app.getModel().onPackageAdded(ai.packageName, Process.myUserHandle());
                }
            } catch (PackageManager.NameNotFoundException e) {
                // Ignore
            }
            // Ignore install success events as they are handled by Package add events.
            return;
        }

        synchronized (apps) {
            PromiseAppInfo updated = null;
            final ArrayList<AppInfo> removed = new ArrayList<>();
            for (int i=0; i < apps.size(); i++) {
                final AppInfo appInfo = apps.get(i);
                final ComponentName tgtComp = appInfo.getTargetComponent();
                if (tgtComp != null && tgtComp.getPackageName().equals(mInstallInfo.packageName)) {
                    if (appInfo instanceof PromiseAppInfo) {
                        final PromiseAppInfo promiseAppInfo = (PromiseAppInfo) appInfo;
                        if (mInstallInfo.state == PackageInstallerCompat.STATUS_INSTALLING) {
                            promiseAppInfo.level = mInstallInfo.progress;
                            updated = promiseAppInfo;
                        } else if (mInstallInfo.state == PackageInstallerCompat.STATUS_FAILED) {
                            apps.removePromiseApp(appInfo);
                            removed.add(appInfo);
                        }
                    }
                }
            }
            if (updated != null) {
                final PromiseAppInfo updatedPromiseApp = updated;
                scheduleCallbackTask(new CallbackTask() {
                    @Override
                    public void execute(Callbacks callbacks) {
                        callbacks.bindPromiseAppProgressUpdated(updatedPromiseApp);
                    }
                });
            }
            if (!removed.isEmpty()) {
                scheduleCallbackTask(new CallbackTask() {
                    @Override
                    public void execute(Callbacks callbacks) {
                        callbacks.bindAppInfosRemoved(removed);
                    }
                });
            }
        }

        synchronized (dataModel) {
            final HashSet<ItemInfo> updates = new HashSet<>();
            for (ItemInfo info : dataModel.itemsIdMap) {
                if (info instanceof ShortcutInfo) {
                    ShortcutInfo si = (ShortcutInfo) info;
                    ComponentName cn = si.getTargetComponent();
                    if (si.hasPromiseIconUi() && (cn != null)
                            && mInstallInfo.packageName.equals(cn.getPackageName())) {
                        si.setInstallProgress(mInstallInfo.progress);
                        if (mInstallInfo.state == PackageInstallerCompat.STATUS_FAILED) {
                            // Mark this info as broken.
                            si.status &= ~ShortcutInfo.FLAG_INSTALL_SESSION_ACTIVE;
                        }
                        updates.add(si);
                    }
                }
            }

            for (LauncherAppWidgetInfo widget : dataModel.appWidgets) {
                if (widget.providerName.getPackageName().equals(mInstallInfo.packageName)) {
                    widget.installProgress = mInstallInfo.progress;
                    updates.add(widget);
                }
            }

            if (!updates.isEmpty()) {
                scheduleCallbackTask(new CallbackTask() {
                    @Override
                    public void execute(Callbacks callbacks) {
                        callbacks.bindRestoreItemsChange(updates);
                    }
                });
            }
        }
    }
}
