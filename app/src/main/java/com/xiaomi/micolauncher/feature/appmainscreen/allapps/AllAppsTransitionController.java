package com.xiaomi.micolauncher.feature.appmainscreen.allapps;

import static com.xiaomi.micolauncher.feature.appmainscreen.LauncherState.ALL_APPS_CONTENT;
import static com.xiaomi.micolauncher.feature.appmainscreen.LauncherState.ALL_APPS_HEADER;
import static com.xiaomi.micolauncher.feature.appmainscreen.LauncherState.ALL_APPS_HEADER_EXTRA;
import static com.xiaomi.micolauncher.feature.appmainscreen.LauncherState.VERTICAL_SWIPE_INDICATOR;
import static com.xiaomi.micolauncher.feature.appmainscreen.anim.Interpolators.LINEAR;
import static com.xiaomi.micolauncher.feature.appmainscreen.anim.PropertySetter.NO_ANIM_PROPERTY_SETTER;
import static com.xiaomi.micolauncher.feature.appmainscreen.util.SystemUiController.UI_STATE_ALL_APPS;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.util.Property;
import android.view.View;

import com.xiaomi.micolauncher.feature.appmainscreen.DeviceProfile;
import com.xiaomi.micolauncher.feature.appmainscreen.DeviceProfile.OnDeviceProfileChangeListener;
import com.xiaomi.micolauncher.feature.appmainscreen.Launcher;
import com.xiaomi.micolauncher.feature.appmainscreen.LauncherState;
import com.xiaomi.micolauncher.feature.appmainscreen.LauncherStateManager.AnimationConfig;
import com.xiaomi.micolauncher.feature.appmainscreen.LauncherStateManager.StateHandler;
import com.xiaomi.micolauncher.feature.appmainscreen.MainAppListFragment;
import com.xiaomi.micolauncher.feature.appmainscreen.R;
import com.xiaomi.micolauncher.feature.appmainscreen.anim.AnimationSuccessListener;
import com.xiaomi.micolauncher.feature.appmainscreen.anim.AnimatorSetBuilder;
import com.xiaomi.micolauncher.feature.appmainscreen.anim.PropertySetter;
import com.xiaomi.micolauncher.feature.appmainscreen.util.Themes;
import com.xiaomi.micolauncher.feature.appmainscreen.views.ScrimView;

/**
 * Handles AllApps view transition.
 * 1) Slides all apps view using direct manipulation
 * 2) When finger is released, animate to either top or bottom accordingly.
 * <p/>
 * Algorithm:
 * If release velocity > THRES1, snap according to the direction of movement.
 * If release velocity < THRES1, snap according to either top or bottom depending on whether it's
 * closer to top or closer to the page indicator.
 */
public class AllAppsTransitionController implements StateHandler, OnDeviceProfileChangeListener {

    public static final Property<AllAppsTransitionController, Float> ALL_APPS_PROGRESS =
            new Property<AllAppsTransitionController, Float>(Float.class, "allAppsProgress") {

        @Override
        public Float get(AllAppsTransitionController controller) {
            return controller.mProgress;
        }

        @Override
        public void set(AllAppsTransitionController controller, Float progress) {
            controller.setProgress(progress);
        }
    };

    private AllAppsContainerView mAppsView;
    private ScrimView mScrimView;

    private final MainAppListFragment mLauncher;
    private final boolean mIsDarkTheme;
    private boolean mIsVerticalLayout;

    // Animation in this class is controlled by a single variable {@link mProgress}.
    // Visually, it represents top y coordinate of the all apps container if multiplied with
    // {@link mShiftRange}.

    // When {@link mProgress} is 0, all apps container is pulled up.
    // When {@link mProgress} is 1, all apps container is pulled down.
    private float mShiftRange;      // changes depending on the orientation
    private float mProgress;        // [0, 1], mShiftRange * mProgress = shiftCurrent

    private float mScrollRangeDelta = 0;

    public AllAppsTransitionController(MainAppListFragment l) {
        mLauncher = l;
        mShiftRange = mLauncher.getDeviceProfile().heightPx;
        mProgress = 1f;

        mIsDarkTheme = Themes.getAttrBoolean(mLauncher.getActivity(), R.attr.mico_isMainColorDark);
        mIsVerticalLayout = mLauncher.getDeviceProfile().isVerticalBarLayout();
        mLauncher.addOnDeviceProfileChangeListener(this);
    }

    public float getShiftRange() {
        return mShiftRange;
    }

    private void onProgressAnimationStart() {
        // Initialize values that should not change until #onDragEnd
        mAppsView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDeviceProfileChanged(DeviceProfile dp) {
        mIsVerticalLayout = dp.isVerticalBarLayout();
        setScrollRangeDelta(mScrollRangeDelta);

        if (mIsVerticalLayout) {
            mAppsView.setAlpha(1);
            mLauncher.getHotseat().setTranslationY(0);
            mLauncher.getWorkspace().getPageIndicator().setTranslationY(0);
        }
    }

    /**
     * Note this method should not be called outside this class. This is public because it is used
     * in xml-based animations which also handle updating the appropriate UI.
     *
     * @param progress value between 0 and 1, 0 shows all apps and 1 shows workspace
     *
     * @see #setState(LauncherState)
     * @see #setStateWithAnimation(LauncherState, AnimatorSetBuilder, AnimationConfig)
     */
    public void setProgress(float progress) {
        mProgress = progress;
        mScrimView.setProgress(progress);
        float shiftCurrent = progress * mShiftRange;

        mAppsView.setTranslationY(shiftCurrent);
        float hotseatTranslation = -mShiftRange + shiftCurrent;

        if (!mIsVerticalLayout) {
            mLauncher.getHotseat().setTranslationY(hotseatTranslation);
            mLauncher.getWorkspace().getPageIndicator().setTranslationY(hotseatTranslation);
        }

        // Use a light system UI (dark icons) if all apps is behind at least half of the
        // status bar.
        boolean forceChange = shiftCurrent - mScrimView.getDragHandleSize()
                <= mLauncher.getDeviceProfile().getInsets().top / 2;
        if (forceChange) {
            mLauncher.getSystemUiController().updateUiState(UI_STATE_ALL_APPS, !mIsDarkTheme);
        } else {
            mLauncher.getSystemUiController().updateUiState(UI_STATE_ALL_APPS, 0);
        }
    }

    public float getProgress() {
        return mProgress;
    }

    /**
     * Sets the vertical transition progress to {@param state} and updates all the dependent UI
     * accordingly.
     */
    @Override
    public void setState(LauncherState state) {
        setProgress(state.getVerticalProgress(mLauncher));
        setAlphas(state, NO_ANIM_PROPERTY_SETTER);
        onProgressAnimationEnd();
    }

    /**
     * Creates an animation which updates the vertical transition progress and updates all the
     * dependent UI using various animation events
     */
    @Override
    public void setStateWithAnimation(LauncherState toState,
            AnimatorSetBuilder builder, AnimationConfig config) {
        //去除上滑抽屉菜单
        /*
        float targetProgress = toState.getVerticalProgress(mLauncher);
        if (Float.compare(mProgress, targetProgress) == 0) {
            setAlphas(toState, config.getPropertySetter(builder));
            // Fail fast
            onProgressAnimationEnd();
            return;
        }

        if (!config.playNonAtomicComponent()) {
            // There is no atomic component for the all apps transition, so just return early.
            return;
        }

        Interpolator interpolator = config.userControlled ? LINEAR : toState == OVERVIEW
                ? builder.getInterpolator(ANIM_OVERVIEW_SCALE, FAST_OUT_SLOW_IN)
                : FAST_OUT_SLOW_IN;
        ObjectAnimator anim =
                ObjectAnimator.ofFloat(this, ALL_APPS_PROGRESS, mProgress, targetProgress);
        anim.setDuration(config.duration);
        anim.setInterpolator(builder.getInterpolator(ANIM_VERTICAL_PROGRESS, interpolator));
        anim.addListener(getProgressAnimatorListener());

        builder.play(anim);

        setAlphas(toState, config.getPropertySetter(builder));*/
    }

    private void setAlphas(LauncherState toState, PropertySetter setter) {
        int visibleElements = toState.getVisibleElements(mLauncher);
        boolean hasHeader = (visibleElements & ALL_APPS_HEADER) != 0;
        boolean hasHeaderExtra = (visibleElements & ALL_APPS_HEADER_EXTRA) != 0;
        boolean hasContent = (visibleElements & ALL_APPS_CONTENT) != 0;

        setter.setViewAlpha(mAppsView.getSearchView(), hasHeader ? 1 : 0, LINEAR);
        setter.setViewAlpha(mAppsView.getContentView(), hasContent ? 1 : 0, LINEAR);
        setter.setViewAlpha(mAppsView.getScrollBar(), hasContent ? 1 : 0, LINEAR);
        mAppsView.getFloatingHeaderView().setContentVisibility(hasHeaderExtra, hasContent, setter);

        setter.setInt(mScrimView, ScrimView.DRAG_HANDLE_ALPHA,
                (visibleElements & VERTICAL_SWIPE_INDICATOR) != 0 ? 255 : 0, LINEAR);
    }

    public AnimatorListenerAdapter getProgressAnimatorListener() {
        return new AnimationSuccessListener() {
            @Override
            public void onAnimationSuccess(Animator animator) {
                onProgressAnimationEnd();
            }

            @Override
            public void onAnimationStart(Animator animation) {
                onProgressAnimationStart();
            }
        };
    }

    public void setupViews(AllAppsContainerView appsView) {
        mAppsView = appsView;
        mScrimView = mLauncher.findViewById(R.id.scrim_view);
    }

    /**
     * Updates the total scroll range but does not update the UI.
     */
    public void setScrollRangeDelta(float delta) {
        mScrollRangeDelta = delta;
        mShiftRange = mLauncher.getDeviceProfile().heightPx - mScrollRangeDelta;

        if (mScrimView != null) {
            mScrimView.reInitUi();
        }
    }

    /**
     * Set the final view states based on the progress.
     * TODO: This logic should go in {@link LauncherState}
     */
    private void onProgressAnimationEnd() {
        if (Float.compare(mProgress, 1f) == 0) {
            mAppsView.setVisibility(View.INVISIBLE);
            mAppsView.reset(false /* animate */);
        } else if (Float.compare(mProgress, 0f) == 0) {
            mAppsView.setVisibility(View.VISIBLE);
            mAppsView.onScrollUpEnd();
        } else {
            mAppsView.setVisibility(View.VISIBLE);
        }
    }
}
