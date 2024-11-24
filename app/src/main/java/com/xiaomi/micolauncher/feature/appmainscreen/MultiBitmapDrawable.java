/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//oh21 fixme 这个文件需要修改图片长按效果和bitmap显示效果
public class MultiBitmapDrawable extends Drawable {

    protected final Paint mPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    private List<Bitmap> mList = new ArrayList<>();
    public static final float DRAWABLE_CORNER = 24;
    private float moveSpace;
    private float mImageViewWidth;
    private float mBitmapWidth;

    public MultiBitmapDrawable(int measuredWidth) {
        mPaint.setFilterBitmap(true);
        mPaint.setAntiAlias(true);
        mImageViewWidth = measuredWidth;
        invalidateSelf();
    }

    public void setData(List<Bitmap> list) {
        mList.addAll(list);
        Collections.reverse(mList);
        mBitmapWidth = mList.get(0).getWidth();
//        moveSpace = (mWidth - mBitmapWidth) / list.size();
    }


    @Override
    public final void draw(Canvas canvas) {
        for (Bitmap bitmap : mList) {
            canvas.translate(canvas.getWidth() / 2 - mBitmapWidth / 2 - moveSpace, 0);
            BitmapShader bitmapShader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            mPaint.setShader(bitmapShader);
            RectF rectF = new RectF(0, 0, bitmap.getWidth(), bitmap.getWidth());
            canvas.drawRoundRect(rectF, DRAWABLE_CORNER, DRAWABLE_CORNER, mPaint);
            moveSpace += 10;
        }
    }

    @Override
    public void setAlpha(int i) {

    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {

    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
