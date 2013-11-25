/*
 * Copyright (C) 2013 Sony Mobile Communications AB
 *
 * This file is part of ChkBugReport.
 *
 * ChkBugReport is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * ChkBugReport is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ChkBugReport.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.sonyericsson.chkbugreport;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/** Image canvas for Android */
public class ImageCanvas {

    public static final int BLACK       = 0xff000000;
    public static final int WHITE       = 0xffffffff;
    public static final int YELLOW      = 0xffffff00;
    public static final int DARK_GRAY   = 0xff666666;
    public static final int LIGHT_GRAY  = 0xffbbbbbb;
    public static final int RED         = 0xffff0000;
    public static final int CYAN        = 0xff00ffff;

    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Paint mPaint = new Paint();

    public ImageCanvas(int width, int height) {
        mBitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
    }

    public int getWidth() {
        return mBitmap.getWidth();
    }

    public int getHeight() {
        return mBitmap.getHeight();
    }

    public void writeTo(File f) throws IOException {
        mBitmap.compress(Bitmap.CompressFormat.PNG, 90, new FileOutputStream(f));
    }

    public void writeTo(OutputStream os) throws IOException {
        mBitmap.compress(Bitmap.CompressFormat.PNG, 90, os);
    }

    public float getStrokeWidth() {
        return mPaint.getStrokeWidth();
    }

    public void setStrokeWidth(float width) {
        mPaint.setStrokeWidth(width);
    }

    public void setColor(int argb) {
        mPaint.setColor(argb);
    }

    public void drawLine(int x0, int y0, int x1, int y1) {
        mCanvas.drawLine(x0, y0, x1, y1, mPaint);
    }

    public void fillRect(int x, int y, int w, int h) {
        mPaint.setStyle(Paint.Style.FILL);
        mCanvas.drawRect(x, y, x + w, y + h, mPaint);
    }

    public void drawRect(int x, int y, int w, int h) {
        mPaint.setStyle(Paint.Style.STROKE);
        mCanvas.drawRect(x, y, x + w - 1, y + h - 1, mPaint);
    }

    public float getFontHeight() {
        return mPaint.getTextSize();
    }

    public float getStringWidth(String string) {
        return mPaint.measureText(string);
    }

    public float getAscent() {
        return -mPaint.ascent();
    }

    public float getDescent() {
        return mPaint.descent();
    }

    public void drawString(String string, float x, float y) {
        mCanvas.drawText(string, x, y, mPaint);
    }

    public void translate(float dx, float dy) {
        mCanvas.translate(dx, dy);
    }

    public void rotate(float degrees) {
        mCanvas.rotate(degrees);
    }

}
