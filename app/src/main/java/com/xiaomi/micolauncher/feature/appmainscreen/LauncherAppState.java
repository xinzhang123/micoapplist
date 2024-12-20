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

package com.xiaomi.micolauncher.feature.appmainscreen;

import static com.xiaomi.micolauncher.feature.appmainscreen.SettingsActivity.NOTIFICATION_BADGING;

import android.content.ComponentName;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Looper;
import android.util.Log;

import com.xiaomi.micolauncher.feature.appmainscreen.compat.LauncherAppsCompat;
import com.xiaomi.micolauncher.feature.appmainscreen.compat.PackageInstallerCompat;
import com.xiaomi.micolauncher.feature.appmainscreen.compat.UserManagerCompat;
import com.xiaomi.micolauncher.feature.appmainscreen.config.FeatureFlags;
import com.xiaomi.micolauncher.feature.appmainscreen.notification.NotificationListener;
import com.xiaomi.micolauncher.feature.appmainscreen.util.ConfigMonitor;
import com.xiaomi.micolauncher.feature.appmainscreen.util.Preconditions;
import com.xiaomi.micolauncher.feature.appmainscreen.util.SettingsObserver;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class LauncherAppState {

    public static final String ACTION_FORCE_ROLOAD = "force-reload-launcher";

    // We do not need any synchronization for this variable as its only written on UI thread.
    private static LauncherAppState INSTANCE;

    private final Context mContext;
    private final LauncherModel mModel;
    private final IconCache mIconCache;
    private final WidgetPreviewLoader mWidgetCache;
    private final InvariantDeviceProfile mInvariantDeviceProfile;
    private final SettingsObserver mNotificationBadgingObserver;
//    private Configuration mOldConfig; //oh21 当前横竖屏状态

    public static LauncherAppState getInstance(final Context context) {
        if (INSTANCE == null) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                INSTANCE = new LauncherAppState(context.getApplicationContext());
            } else {
                try {
                    return new MainThreadExecutor().submit(new Callable<LauncherAppState>() {
                        @Override
                        public LauncherAppState call() throws Exception {
                            return LauncherAppState.getInstance(context);
                        }
                    }).get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return INSTANCE;
    }

    public static LauncherAppState getInstanceNoCreate() {
        return INSTANCE;
    }

    public Context getContext() {
        return mContext;
    }

    private LauncherAppState(Context context) {
        if (getLocalProvider(context) == null) {
            throw new RuntimeException(
                    "Initializing LauncherAppState in the absence of LauncherProvider");
        }
        Log.v(MainAppListFragment.TAG, "LauncherAppState initiated");
        Preconditions.assertUIThread();
        mContext = context;

        mInvariantDeviceProfile = new InvariantDeviceProfile(mContext);
        mIconCache = new IconCache(mContext, mInvariantDeviceProfile);
        mWidgetCache = new WidgetPreviewLoader(mContext, mIconCache);
        mModel = new LauncherModel(this, mIconCache, AppFilter.newInstance(mContext));

//        mOldConfig = new Configuration(mContext.getResources().getConfiguration());

        LauncherAppsCompat.getInstance(mContext).addOnAppsChangedCallback(mModel);

        // Register intent receivers
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
        // For handling managed profiles
        filter.addAction(Intent.ACTION_MANAGED_PROFILE_ADDED);
        filter.addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED);
        filter.addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE);
        filter.addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE);
        filter.addAction(Intent.ACTION_MANAGED_PROFILE_UNLOCKED);

        if (FeatureFlags.IS_DOGFOOD_BUILD) {
            filter.addAction(ACTION_FORCE_ROLOAD);
        }

        mContext.registerReceiver(mModel, filter);
        UserManagerCompat.getInstance(mContext).enableAndResetCache();
        new ConfigMonitor(mContext).register();

        if (!mContext.getResources().getBoolean(R.bool.notification_badging_enabled)) {
            mNotificationBadgingObserver = null;
        } else {
            // Register an observer to rebind the notification listener when badging is re-enabled.
            mNotificationBadgingObserver = new SettingsObserver.Secure(
                    mContext.getContentResolver()) {
                @Override
                public void onSettingChanged(boolean isNotificationBadgingEnabled) {
                    if (isNotificationBadgingEnabled) {
                        NotificationListener.requestRebind(new ComponentName(
                                mContext, NotificationListener.class));
                    }
                }
            };
            mNotificationBadgingObserver.register(NOTIFICATION_BADGING);
        }
    }

    /**
     * Call from Application.onTerminate(), which is not guaranteed to ever be called.
     */
    public void onTerminate() {
        mContext.unregisterReceiver(mModel);
        final LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(mContext);
        launcherApps.removeOnAppsChangedCallback(mModel);
        PackageInstallerCompat.getInstance(mContext).onStop();
        if (mNotificationBadgingObserver != null) {
            mNotificationBadgingObserver.unregister();
        }
    }

    LauncherModel setLauncher(MainAppListFragment launcher) {
        getLocalProvider(mContext).setLauncherProviderChangeListener(launcher);
        mModel.initialize(launcher);
        return mModel;
    }

    public IconCache getIconCache() {
        return mIconCache;
    }

    public LauncherModel getModel() {
        return mModel;
    }

    public WidgetPreviewLoader getWidgetCache() {
        return mWidgetCache;
    }

    public InvariantDeviceProfile getInvariantDeviceProfile() {
        return mInvariantDeviceProfile;
    }

    public void updateDeviceProfile(){
        mInvariantDeviceProfile.initProfile(mContext);
    }

    /**
     * Shorthand for {@link #getInvariantDeviceProfile()}
     */
    public static InvariantDeviceProfile getIDP(Context context) {
        return LauncherAppState.getInstance(context).getInvariantDeviceProfile();
    }

    private static LauncherProvider getLocalProvider(Context context) {
        // modify by codemx.cn --20190322--------start
        LauncherProvider provider = null;
        try {
            ContentProviderClient client = context.getContentResolver()
                    .acquireContentProviderClient(LauncherProvider.AUTHORITY);
            if (client != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {//>24
                    client.close();
                } else {
                    client.release();
                }
                provider = (LauncherProvider) client.getLocalContentProvider();
            } else {
                Log.e("TAG", " can't get ContentProviderClient--- ");
            }
        } catch (Exception e) {
            Log.e("TAG", e.getMessage());
        }
        return provider;
        // modify by codemx.cn --20190322--------end
    }

    //oh21 fixme 这里把Launcher改成不使用configChange处理横竖屏，从Oncreate中主动处理Ui。看看要不要把onConfigurationChanged其他逻辑也拿到这个里面
    public void doConfigChange() {
//        Configuration newConfig = mContext.getResources().getConfiguration();
//        int diff = mOldConfig.diff(newConfig);
//        if ((diff & (CONFIG_ORIENTATION | CONFIG_SCREEN_SIZE)) != 0) {
//            updateDeviceProfile();
//        }
//        mOldConfig.setTo(newConfig);
    }
}
