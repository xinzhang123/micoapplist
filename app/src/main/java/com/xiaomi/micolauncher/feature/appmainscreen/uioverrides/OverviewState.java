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

import static com.xiaomi.micolauncher.feature.appmainscreen.LauncherAnimUtils.OVERVIEW_TRANSITION_MS;

import com.xiaomi.micolauncher.feature.appmainscreen.LauncherState;

/**
 * Definition for overview state
 */
public class OverviewState extends LauncherState {

    public OverviewState(int id) {
        super(id, 0, OVERVIEW_TRANSITION_MS, FLAG_DISABLE_RESTORE);
    }
}
