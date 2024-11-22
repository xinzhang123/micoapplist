package com.xiaomi.micolauncher.feature.appmainscreen.uninstall;

import static com.xiaomi.micolauncher.feature.appmainscreen.LauncherAnimUtils.SPRING_LOADED_TRANSITION_MS;
import static com.xiaomi.micolauncher.feature.appmainscreen.LauncherState.NORMAL;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.xiaomi.micolauncher.feature.appmainscreen.AbstractFloatingView;
import com.xiaomi.micolauncher.feature.appmainscreen.DropTarget;
import com.xiaomi.micolauncher.feature.appmainscreen.MainAppListFragment;
import com.xiaomi.micolauncher.feature.appmainscreen.R;
import com.xiaomi.micolauncher.feature.appmainscreen.dragndrop.DragLayer;

public class UninstallConfirmDialog extends AbstractFloatingView implements View.OnClickListener {
    private static final String TAG = "UninstallConfirmDialog";
    public TextView tvTitle;
    public TextView tvCancel;
    public TextView tvUninstall;
    private DragLayer mDragLayer;
    private MainAppListFragment mLauncher;
    private DropTarget.DragObject mDragObject;

    public UninstallConfirmDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public UninstallConfirmDialog(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public static void showUninstallConfirmDialog(MainAppListFragment launcher, DropTarget.DragObject dragObject) {
        DragLayer dl = launcher.getDragLayer();
        UninstallConfirmDialog uninstallDialog = (UninstallConfirmDialog) launcher.getLayoutInflater()
                .inflate(R.layout.layout_uninstall_confirm, dl, false);
        uninstallDialog.setupForWidget(dl, launcher, dragObject);
        uninstallDialog.tvTitle.setText(launcher.getString(R.string.dialog_uninstall_title, dragObject.dragInfo.title));
        uninstallDialog.tvCancel.setOnClickListener(uninstallDialog);
        uninstallDialog.tvUninstall.setOnClickListener(uninstallDialog);
        dl.addView(uninstallDialog);
    }

    private void setupForWidget(DragLayer dl, MainAppListFragment launcher, DropTarget.DragObject dragObject) {
        mIsOpen = true;
        mDragLayer = dl;
        mLauncher = launcher;
        mDragObject = dragObject;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        tvTitle = findViewById(R.id.tv_title);
        tvCancel = findViewById(R.id.tv_cancel);
        tvUninstall = findViewById(R.id.tv_uninstall);
    }

    @Override
    protected void handleClose(boolean animate) {
        mDragLayer.removeView(this);
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
            close(false);
            if (null != mLauncher.getWorkspace().getDragInfo()) {
                final View cell = mLauncher.getWorkspace().getDragInfo().cell;
                mLauncher.getDragController().animateDragViewToOriginalPosition(
                        null, cell, SPRING_LOADED_TRANSITION_MS);
            } else {
                mLauncher.getDragController().removeDragView();
            }
            mLauncher.getStateManager().goToState(NORMAL);
        } else if (v.getId() == R.id.tv_uninstall) {
            uninstall(mDragObject.dragInfo.getTargetComponent().getPackageName());
            close(false);
            //oh21 fixme 需要修改
            mLauncher.getDragController().removeDragView();
            mLauncher.getStateManager().goToState(NORMAL);
        }
    }

    public void uninstall(String packageName) {
        Intent broadcastIntent = new Intent(mLauncher.getActivity(), InstallResultReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mLauncher.getActivity(), 1,
                broadcastIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        PackageInstaller packageInstaller = mLauncher.getActivity().getPackageManager().getPackageInstaller();
        packageInstaller.uninstall(packageName, pendingIntent.getIntentSender());
    }
}
