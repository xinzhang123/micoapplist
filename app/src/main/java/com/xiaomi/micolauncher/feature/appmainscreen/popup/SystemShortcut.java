package com.xiaomi.micolauncher.feature.appmainscreen.popup;


import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import com.xiaomi.micolauncher.feature.appmainscreen.AbstractFloatingView;
import com.xiaomi.micolauncher.feature.appmainscreen.BaseDraggingActivity;
import com.xiaomi.micolauncher.feature.appmainscreen.ItemInfo;
import com.xiaomi.micolauncher.feature.appmainscreen.Launcher;
import com.xiaomi.micolauncher.feature.appmainscreen.R;
import com.xiaomi.micolauncher.feature.appmainscreen.ShortcutInfo;
import com.xiaomi.micolauncher.feature.appmainscreen.Utilities;
import com.xiaomi.micolauncher.feature.appmainscreen.model.WidgetItem;
import com.xiaomi.micolauncher.feature.appmainscreen.util.InstantAppResolver;
import com.xiaomi.micolauncher.feature.appmainscreen.util.PackageManagerHelper;
import com.xiaomi.micolauncher.feature.appmainscreen.util.PackageUserKey;
import com.xiaomi.micolauncher.feature.appmainscreen.widget.WidgetsBottomSheet;

import java.util.List;

/**
 * Represents a system shortcut for a given app. The shortcut should have a static label and
 * icon, and an onClickListener that depends on the item that the shortcut services.
 *
 * Example system shortcuts, defined as inner classes, include Widgets and AppInfo.
 */
public abstract class SystemShortcut<T extends BaseDraggingActivity> extends ItemInfo {
    public final int iconResId;
    public final int labelResId;

    public SystemShortcut(int iconResId, int labelResId) {
        this.iconResId = iconResId;
        this.labelResId = labelResId;
    }

    public abstract View.OnClickListener getOnClickListener(T activity, ItemInfo itemInfo);

    public static class Widgets extends SystemShortcut<Launcher> {

        public Widgets() {
            super(R.drawable.ic_widget, R.string.widget_button_text);
        }

        @Override
        public View.OnClickListener getOnClickListener(final Launcher launcher,
                final ItemInfo itemInfo) {
            final List<WidgetItem> widgets =
                    launcher.getPopupDataProvider().getWidgetsForPackageUser(new PackageUserKey(
                            itemInfo.getTargetComponent().getPackageName(), itemInfo.user));
            if (widgets == null) {
                return null;
            }
            return (view) -> {
                AbstractFloatingView.closeAllOpenViews(launcher);
                WidgetsBottomSheet widgetsBottomSheet =
                        (WidgetsBottomSheet) launcher.getLayoutInflater().inflate(
                                R.layout.widgets_bottom_sheet, launcher.getDragLayer(), false);
                widgetsBottomSheet.populateAndShow(itemInfo);
            };
        }
    }

    public static class AppInfo extends SystemShortcut {
        public AppInfo() {
            super(R.drawable.ic_info_no_shadow, R.string.app_info_drop_target_label);
        }

        @Override
        public View.OnClickListener getOnClickListener(
                BaseDraggingActivity activity, ItemInfo itemInfo) {
            return (view) -> {
                Rect sourceBounds = activity.getViewBounds(view);
                Bundle opts = activity.getActivityLaunchOptionsAsBundle(view);
                new PackageManagerHelper(activity).startDetailsActivityForInfo(
                        itemInfo, sourceBounds, opts);
            };
        }
    }

    public static class Install extends SystemShortcut {
        public Install() {
            super(R.drawable.ic_install_no_shadow, R.string.install_drop_target_label);
        }

        @Override
        public View.OnClickListener getOnClickListener(
                BaseDraggingActivity activity, ItemInfo itemInfo) {
            boolean supportsWebUI = (itemInfo instanceof ShortcutInfo) &&
                    ((ShortcutInfo) itemInfo).hasStatusFlag(ShortcutInfo.FLAG_SUPPORTS_WEB_UI);
            boolean isInstantApp = false;
            if (itemInfo instanceof com.xiaomi.micolauncher.feature.appmainscreen.AppInfo) {
                com.xiaomi.micolauncher.feature.appmainscreen.AppInfo appInfo = (com.xiaomi.micolauncher.feature.appmainscreen.AppInfo) itemInfo;
                isInstantApp = InstantAppResolver.newInstance(activity).isInstantApp(appInfo);
            }
            boolean enabled = supportsWebUI || isInstantApp;
            if (!enabled) {
                return null;
            }
            return createOnClickListener(activity, itemInfo);
        }

        public View.OnClickListener createOnClickListener(
                BaseDraggingActivity activity, ItemInfo itemInfo) {
            return view -> {
                Intent intent = new PackageManagerHelper(view.getContext()).getMarketIntent(
                        itemInfo.getTargetComponent().getPackageName());
                activity.startActivitySafely(view, intent, itemInfo);
                AbstractFloatingView.closeAllOpenViews(activity);
            };
        }
    }

    public static class UnInstall extends SystemShortcut {
        public UnInstall() {
            super(R.drawable.ic_uninstall_no_shadow, R.string.uninstall_drop_target_label);
        }

        @Override
        public View.OnClickListener getOnClickListener(
                BaseDraggingActivity activity, ItemInfo itemInfo) {
            ComponentName targetComponent = itemInfo.getTargetComponent();
            if( null == itemInfo || null == targetComponent || TextUtils.isEmpty(targetComponent.getPackageName()))
                return  null;
            String packageName = targetComponent.getPackageName();
            if(!Utilities.isAppInstalled(activity, packageName) || Utilities.isSystemApp(activity,itemInfo.getIntent()))
                return null;
            return createOnClickListener(activity, itemInfo);
        }

        public View.OnClickListener createOnClickListener(
                BaseDraggingActivity activity, ItemInfo itemInfo) {
            return view -> {
                ComponentName cn = itemInfo.getTargetComponent();
                Intent intent = null;
                try {
                    intent = Intent.parseUri(activity.getString(R.string.delete_package_intent), 0)
                            .setData(Uri.fromParts("package", cn.getPackageName(), cn.getClassName()))
                            .putExtra(Intent.EXTRA_USER, itemInfo.user);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                activity.startActivitySafely(view, intent, itemInfo);
                AbstractFloatingView.closeAllOpenViews(activity);
            };
        }
    }
}
