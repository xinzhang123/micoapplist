package com.xiaomi.micolauncher.feature.appmainscreen;

import static com.xiaomi.micolauncher.feature.appmainscreen.util.SystemUiController.UI_STATE_OVERVIEW;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.Configuration;
import android.view.View;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.xiaomi.micolauncher.feature.appmainscreen.uioverrides.UiFactory;
import com.xiaomi.micolauncher.feature.appmainscreen.util.SystemUiController;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.util.ArrayList;

public class BaseFragment extends Fragment {
    public static final int INVISIBLE_BY_STATE_HANDLER = 1 << 0;
    public static final int INVISIBLE_BY_APP_TRANSITIONS = 1 << 1;
    public static final int INVISIBLE_ALL =
            INVISIBLE_BY_STATE_HANDLER | INVISIBLE_BY_APP_TRANSITIONS;

    @Retention(SOURCE)
    @IntDef(
            flag = true,
            value = {INVISIBLE_BY_STATE_HANDLER, INVISIBLE_BY_APP_TRANSITIONS})
    public @interface InvisibilityFlags {
    }

    private final ArrayList<DeviceProfile.OnDeviceProfileChangeListener> mDPChangeListeners = new ArrayList<>();

    private final ArrayList<MultiWindowModeChangedListener> mMultiWindowModeChangedListeners =
            new ArrayList<>();

    protected DeviceProfile mDeviceProfile;
    protected SystemUiController mSystemUiController;

    private static final int ACTIVITY_STATE_STARTED = 1 << 0;
    private static final int ACTIVITY_STATE_RESUMED = 1 << 1;
    private static final int ACTIVITY_STATE_USER_ACTIVE = 1 << 2;

    @Retention(SOURCE)
    @IntDef(
            flag = true,
            value = {ACTIVITY_STATE_STARTED, ACTIVITY_STATE_RESUMED, ACTIVITY_STATE_USER_ACTIVE})
    public @interface ActivityFlags {
    }

    @ActivityFlags
    private int mActivityFlags;

    @InvisibilityFlags
    private int mForceInvisible;

    public DeviceProfile getDeviceProfile() {
        return mDeviceProfile;
    }

    public View.AccessibilityDelegate getAccessibilityDelegate() {
        return null;
    }

    public boolean isInMultiWindowModeCompat() {
        return Utilities.ATLEAST_NOUGAT && getActivity().isInMultiWindowMode();
    }

    public static BaseFragment fromContext(Context context) {
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
                return (BaseFragment) fragment;
            }
        }
        // 没有找到，创建新的实例
        return null;
    }

    public SystemUiController getSystemUiController() {
        if (mSystemUiController == null) {
            mSystemUiController = new SystemUiController(getActivity().getWindow());
        }
        return mSystemUiController;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onStart() {
        mActivityFlags |= ACTIVITY_STATE_STARTED;
        super.onStart();
    }

    @Override
    public void onResume() {
        mActivityFlags |= ACTIVITY_STATE_RESUMED | ACTIVITY_STATE_USER_ACTIVE;
        super.onResume();
    }

//    @Override
//    protected void onUserLeaveHint() {
//        mActivityFlags &= ~ACTIVITY_STATE_USER_ACTIVE;
//        super.onUserLeaveHint();
//    }

//    @Override
//    public void onMultiWindowModeChanged(boolean isInMultiWindowMode, Configuration newConfig) {
//        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig);
//        for (int i = mMultiWindowModeChangedListeners.size() - 1; i >= 0; i--) {
//            mMultiWindowModeChangedListeners.get(i).onMultiWindowModeChanged(isInMultiWindowMode);
//        }
//    }

    @Override
    public void onStop() {
        mActivityFlags &= ~ACTIVITY_STATE_STARTED & ~ACTIVITY_STATE_USER_ACTIVE;
        mForceInvisible = 0;
        super.onStop();
    }

    @Override
    public void onPause() {
        mActivityFlags &= ~ACTIVITY_STATE_RESUMED;
        super.onPause();
        getSystemUiController().updateUiState(UI_STATE_OVERVIEW, 0);
    }

    public boolean isStarted() {
        return (mActivityFlags & ACTIVITY_STATE_STARTED) != 0;
    }

    public boolean hasBeenResumed() {
        return (mActivityFlags & ACTIVITY_STATE_RESUMED) != 0;
    }

    public boolean isUserActive() {
        return (mActivityFlags & ACTIVITY_STATE_USER_ACTIVE) != 0;
    }

    public void addOnDeviceProfileChangeListener(DeviceProfile.OnDeviceProfileChangeListener listener) {
        mDPChangeListeners.add(listener);
    }

    public void removeOnDeviceProfileChangeListener(DeviceProfile.OnDeviceProfileChangeListener listener) {
        mDPChangeListeners.remove(listener);
    }

    protected void dispatchDeviceProfileChanged() {
        for (int i = mDPChangeListeners.size() - 1; i >= 0; i--) {
            mDPChangeListeners.get(i).onDeviceProfileChanged(mDeviceProfile);
        }
    }

    public void addMultiWindowModeChangedListener(MultiWindowModeChangedListener listener) {
        mMultiWindowModeChangedListeners.add(listener);
    }

    public void removeMultiWindowModeChangedListener(MultiWindowModeChangedListener listener) {
        mMultiWindowModeChangedListeners.remove(listener);
    }

    public void addForceInvisibleFlag(@InvisibilityFlags int flag) {
        mForceInvisible |= flag;
    }

    public void clearForceInvisibleFlag(@InvisibilityFlags int flag) {
        mForceInvisible &= ~flag;
    }

    public boolean isForceInvisible() {
        return mForceInvisible != 0;
    }

    public interface MultiWindowModeChangedListener {
        void onMultiWindowModeChanged(boolean isInMultiWindowMode);
    }

//    @Override
//    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
//        if (!UiFactory.dumpActivity(this, writer)) {
//            super.dump(prefix, fd, writer, args);
//        }
//    }
//
//    protected void dumpMisc(PrintWriter writer) {
//        writer.println(" deviceProfile isTransposed=" + getDeviceProfile().isVerticalBarLayout());
//        writer.println(" orientation=" + getResources().getConfiguration().orientation);
//        writer.println(" mSystemUiController: " + mSystemUiController);
//        writer.println(" mActivityFlags: " + mActivityFlags);
//        writer.println(" mForceInvisible: " + mForceInvisible);
//    }
}
