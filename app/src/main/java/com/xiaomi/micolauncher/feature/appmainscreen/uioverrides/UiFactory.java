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

package com.xiaomi.micolauncher.feature.appmainscreen.uioverrides;

import android.app.Activity;
import android.content.Context;
import android.os.CancellationSignal;

import androidx.fragment.app.Fragment;

import com.xiaomi.micolauncher.feature.appmainscreen.Launcher;
import com.xiaomi.micolauncher.feature.appmainscreen.LauncherStateManager.StateHandler;
import com.xiaomi.micolauncher.feature.appmainscreen.MainAppListFragment;
import com.xiaomi.micolauncher.feature.appmainscreen.util.TouchController;

import java.io.PrintWriter;

public class UiFactory {

    public static TouchController[] createTouchControllers(MainAppListFragment launcher) {
        return new TouchController[] {
                launcher.getDragController(), new AllAppsSwipeController(launcher)};
    }

    public static void setOnTouchControllersChangedListener(Context context, Runnable listener) { }

    public static StateHandler[] getStateHandler(MainAppListFragment launcher) {
        return new StateHandler[] { launcher.getWorkspace() };
    }

    public static void resetOverview(MainAppListFragment launcher) { }

    public static void onLauncherStateOrFocusChanged(MainAppListFragment launcher) { }

    public static void onCreate(MainAppListFragment launcher) { }

    public static void onStart(MainAppListFragment launcher) { }

    public static void onLauncherStateOrResumeChanged(MainAppListFragment launcher) { }

    public static void onTrimMemory(MainAppListFragment launcher, int level) { }

    public static void useFadeOutAnimationForLauncherStart(MainAppListFragment launcher,
            CancellationSignal cancellationSignal) { }

    public static boolean dumpActivity(Fragment activity, PrintWriter writer) {
        return false;
    }

    public static void prepareToShowOverview(MainAppListFragment launcher) { }

    public static void setBackButtonAlpha(MainAppListFragment launcher, float alpha, boolean animate) { }
}
