package com.xiaomi.micolauncher.feature.appmainscreen;


import android.app.ActivityOptions;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Process;
import android.os.StrictMode;
import android.os.UserHandle;
import android.util.Log;
import android.view.ActionMode;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.xiaomi.micolauncher.feature.appmainscreen.badge.BadgeInfo;
import com.xiaomi.micolauncher.feature.appmainscreen.compat.LauncherAppsCompat;
import com.xiaomi.micolauncher.feature.appmainscreen.shortcuts.DeepShortcutManager;
import com.xiaomi.micolauncher.feature.appmainscreen.uioverrides.DisplayRotationListener;
import com.xiaomi.micolauncher.feature.appmainscreen.uioverrides.WallpaperColorInfo;
import com.xiaomi.micolauncher.feature.appmainscreen.views.BaseDragLayer;

public abstract class BaseDraggingFragment2 extends BaseFragment implements WallpaperColorInfo.OnChangeListener {

    private static final String TAG = "BaseDraggingFragment2";

    public static final String INTENT_EXTRA_IGNORE_LAUNCH_ANIMATION =
            "com.xiaomi.micolauncher.feature.appmainscreen.intent.extra.shortcut.INGORE_LAUNCH_ANIMATION";
    public static final Object AUTO_CANCEL_ACTION_MODE = new Object();
    private ActionMode mCurrentActionMode;
    protected boolean mIsSafeModeEnabled;
    private BaseDraggingFragment.OnStartCallback mOnStartCallback;

    private int mThemeRes = R.style.LauncherTheme;
    private DisplayRotationListener mRotationListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mIsSafeModeEnabled = getActivity().getPackageManager().isSafeMode();
        mRotationListener = new DisplayRotationListener(getActivity(), this::onDeviceRotationChanged);

        WallpaperColorInfo wallpaperColorInfo = WallpaperColorInfo.getInstance(getActivity());
        wallpaperColorInfo.addOnChangeListener(this);
//        int themeRes = getThemeRes(wallpaperColorInfo);
//        if (themeRes != mThemeRes) {
//            mThemeRes = themeRes;
//            setTheme(themeRes);
//        }
    }

    protected int getThemeRes(WallpaperColorInfo wallpaperColorInfo) {
        if (wallpaperColorInfo.isDark()) {
            return wallpaperColorInfo.supportsDarkText() ?
                    R.style.LauncherThemeDark_DarKText : R.style.LauncherThemeDark;
        } else {
            return wallpaperColorInfo.supportsDarkText() ?
                    R.style.LauncherTheme_DarkText : R.style.LauncherTheme;
        }
    }

    private void onDeviceRotationChanged() {
//        if (mDeviceProfile.updateIsSeascape(getActivity().getWindowManager())) {
//            reapplyUi();
//        }
    }

    @Override
    public void onExtractedColorsChanged(WallpaperColorInfo wallpaperColorInfo) {

    }

    protected void onDeviceProfileInitiated() {
        if (mDeviceProfile.isVerticalBarLayout()) {
            mRotationListener.enable();
            mDeviceProfile.updateIsSeascape(getActivity().getWindowManager());
        } else {
            mRotationListener.disable();
        }
    }

    public abstract BaseDragLayer getDragLayer();

    public abstract <T extends View> T getOverviewPanel();

    public abstract View getRootView();

    public abstract BadgeInfo getBadgeInfoForItem(ItemInfo info);

    public abstract void invalidateParent(ItemInfo info);

    public static BaseDraggingFragment2 fromContext(Context context) {
        FragmentManager fragmentManager;
        if (context instanceof Launcher2) {
            fragmentManager = ((Launcher2) context).getSupportFragmentManager();
        } else {
            fragmentManager = ((Launcher2) ((ContextWrapper) context).getBaseContext()).getSupportFragmentManager();
        }
        // 遍历所有的Fragment
        for (Fragment fragment : fragmentManager.getFragments()) {
            if (fragment != null && MainAppListFragment.class.isAssignableFrom(fragment.getClass())) {
                // 类型匹配，直接返回
                return (BaseDraggingFragment2) fragment;
            }
        }
        // 没有找到，创建新的实例
        return null;
    }

    public boolean startActivitySafely(View v, Intent intent, ItemInfo item) {
        if (mIsSafeModeEnabled && !Utilities.isSystemApp(getActivity(), intent)) {
            Toast.makeText(getActivity(), R.string.safemode_shortcut_error, Toast.LENGTH_SHORT).show();
            return false;
        }

        // Only launch using the new animation if the shortcut has not opted out (this is a
        // private contract between launcher and may be ignored in the future).
        boolean useLaunchAnimation = (v != null) &&
                !intent.hasExtra(INTENT_EXTRA_IGNORE_LAUNCH_ANIMATION);
        Bundle optsBundle = useLaunchAnimation
                ? getActivityLaunchOptionsAsBundle(v)
                : null;

        UserHandle user = item == null ? null : item.user;

        // Prepare intent
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (v != null) {
            intent.setSourceBounds(getViewBounds(v));
        }
        try {
            boolean isShortcut = Utilities.ATLEAST_MARSHMALLOW
                    && (item instanceof ShortcutInfo)
                    && (item.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT
                    || item.itemType == LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT)
                    && !((ShortcutInfo) item).isPromise();
            if (isShortcut) {
                // Shortcuts need some special checks due to legacy reasons.
                startShortcutIntentSafely(intent, optsBundle, item);
            } else if (user == null || user.equals(Process.myUserHandle())) {
                // Could be launching some bookkeeping activity
                startActivity(intent, optsBundle);
            } else {
                LauncherAppsCompat.getInstance(getActivity()).startActivityForProfile(
                        intent.getComponent(), user, intent.getSourceBounds(), optsBundle);
            }
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(getActivity(), R.string.activity_not_found, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Unable to launch. tag=" + item + " intent=" + intent, e);
        }
        return false;
    }

    public Rect getViewBounds(View v) {
        int[] pos = new int[2];
        v.getLocationOnScreen(pos);
        return new Rect(pos[0], pos[1], pos[0] + v.getWidth(), pos[1] + v.getHeight());
    }

    private void startShortcutIntentSafely(Intent intent, Bundle optsBundle, ItemInfo info) {
        try {
            StrictMode.VmPolicy oldPolicy = StrictMode.getVmPolicy();
            try {
                // Temporarily disable deathPenalty on all default checks. For eg, shortcuts
                // containing file Uri's would cause a crash as penaltyDeathOnFileUriExposure
                // is enabled by default on NYC.
                StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll()
                        .penaltyLog().build());

                if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT) {
                    String id = ((ShortcutInfo) info).getDeepShortcutId();
                    String packageName = intent.getPackage();
                    DeepShortcutManager.getInstance(getActivity()).startShortcut(
                            packageName, id, intent.getSourceBounds(), optsBundle, info.user);
                } else {
                    // Could be launching some bookkeeping activity
                    startActivity(intent, optsBundle);
                }
            } finally {
                StrictMode.setVmPolicy(oldPolicy);
            }
        } catch (SecurityException e) {
            if (!onErrorStartingShortcut(intent, info)) {
                throw e;
            }
        }
    }

    public boolean finishAutoCancelActionMode() {
        if (mCurrentActionMode != null && AUTO_CANCEL_ACTION_MODE == mCurrentActionMode.getTag()) {
            mCurrentActionMode.finish();
            return true;
        }
        return false;
    }

    protected boolean onErrorStartingShortcut(Intent intent, ItemInfo info) {
        return false;
    }

    public final Bundle getActivityLaunchOptionsAsBundle(View v) {
        ActivityOptions activityOptions = getActivityLaunchOptions(v);
        return activityOptions == null ? null : activityOptions.toBundle();
    }

    public abstract ActivityOptions getActivityLaunchOptions(View v);

    protected abstract void reapplyUi();
}
