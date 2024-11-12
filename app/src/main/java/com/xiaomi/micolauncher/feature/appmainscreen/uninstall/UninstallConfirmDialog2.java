package com.xiaomi.micolauncher.feature.appmainscreen.uninstall;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.xiaomi.micolauncher.feature.appmainscreen.AbstractFloatingView;
import com.xiaomi.micolauncher.feature.appmainscreen.ItemInfo;
import com.xiaomi.micolauncher.feature.appmainscreen.MainAppListFragment;
import com.xiaomi.micolauncher.feature.appmainscreen.R;
import com.xiaomi.micolauncher.feature.appmainscreen.ShortcutInfo;
import com.xiaomi.micolauncher.feature.appmainscreen.dragndrop.DragLayer;

public class UninstallConfirmDialog2 extends AbstractFloatingView implements View.OnClickListener {
    private static final String TAG = "UninstallConfirmDialog";
    public TextView tvTitle;
    public TextView tvCancel;
    public TextView tvUninstall;
    public ImageView ivApp;
    private DragLayer mDragLayer;
    private MainAppListFragment mLauncher;
    private ItemInfo mItemInfo;

    public UninstallConfirmDialog2(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public UninstallConfirmDialog2(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public static void showUninstallConfirmDialog(MainAppListFragment launcher, ItemInfo itemInfo) {
        DragLayer dl = launcher.getDragLayer();
        UninstallConfirmDialog2 uninstallDialog = (UninstallConfirmDialog2) launcher.getLayoutInflater()
                .inflate(R.layout.layout_uninstall_confirm2, dl, false);
        uninstallDialog.setupForWidget(dl, launcher, itemInfo);
        uninstallDialog.tvTitle.setText(launcher.getString(R.string.dialog_uninstall_title, itemInfo.title));
        if (itemInfo instanceof ShortcutInfo) {
            uninstallDialog.ivApp.setImageBitmap(((ShortcutInfo) itemInfo).iconBitmap);
        }
        uninstallDialog.tvCancel.setOnClickListener(uninstallDialog);
        uninstallDialog.tvUninstall.setOnClickListener(uninstallDialog);
        dl.addView(uninstallDialog);
    }

    private void setupForWidget(DragLayer dl, MainAppListFragment launcher, ItemInfo itemInfo) {
        mIsOpen = true;
        mDragLayer = dl;
        mLauncher = launcher;
        mItemInfo = itemInfo;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        tvTitle = findViewById(R.id.tv_title);
        tvCancel = findViewById(R.id.tv_cancel);
        tvUninstall = findViewById(R.id.tv_uninstall);
        ivApp = findViewById(R.id.iv_app);
    }

    @Override
    protected void handleClose(boolean animate) {
//        mDragLayer.removeView(this);
    }

    @Override
    public void logActionCommand(int command) {

    }

    @Override
    protected boolean isOfType(int type) {
        return (type & TYPE_UNINSTALL_POPUP) != 0;
    }

    @Override
    public boolean onControllerInterceptTouchEvent(MotionEvent ev) {
        Log.d(TAG, "onControllerInterceptTouchEvent: ");
        return false;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.tv_cancel) {
            Log.d(TAG, "onClick: ");
            mDragLayer.removeView(this);
//            final View cell = mLauncher.getWorkspace().getDragInfo().cell;
//            mLauncher.getDragController().animateDragViewToOriginalPosition(
//                    null, cell, SPRING_LOADED_TRANSITION_MS);
//            mLauncher.getStateManager().goToState(NORMAL);
        } else if (v.getId() == R.id.tv_uninstall) {
            mDragLayer.removeView(this);
//                            ComponentName cn = itemInfo.getTargetComponent();
//                Intent intent = null;
//                try {
//                    intent = Intent.parseUri(activity.getString(R.string.delete_package_intent), 0)
//                            .setData(Uri.fromParts("package", cn.getPackageName(), cn.getClassName()))
//                            .putExtra(Intent.EXTRA_USER, itemInfo.user);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//                activity.startActivitySafely(view, intent, itemInfo);
        }
    }
}
