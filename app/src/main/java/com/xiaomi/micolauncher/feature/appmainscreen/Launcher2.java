package com.xiaomi.micolauncher.feature.appmainscreen;

import android.content.Context;
import android.content.ContextWrapper;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

public class Launcher2 extends FragmentActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher2);
    }

    public static MainAppListFragment getLauncher(Context context) {
        if (context instanceof Launcher2) {
            return getFragment(context);
        }
        return getFragment((Launcher2) ((ContextWrapper) context).getBaseContext());
    }

    public static MainAppListFragment getFragment(Context context) {
        FragmentManager fragmentManager = ((FragmentActivity) context).getSupportFragmentManager();
        // 遍历所有的Fragment
        for (Fragment fragment : fragmentManager.getFragments()) {
            if (fragment != null && MainAppListFragment.class.isAssignableFrom(fragment.getClass())) {
                // 类型匹配，直接返回
                return (MainAppListFragment) fragment;
            }
        }
        // 没有找到，创建新的实例
        return null;
    }
}
