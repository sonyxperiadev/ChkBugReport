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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class LogViewer extends View {

    private static final int SIZE = 100;

    private String mLogs[] = new String[SIZE];
    private int mIdx = 0;
    private Paint mPaint = new Paint();
    private int mTextHeight;

    public LogViewer(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint.setColor(0xff00ff00);
        mTextHeight = (int)mPaint.getTextSize();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(0xff000000);
        int count = Math.min(SIZE, getHeight() / mTextHeight);
        int y = mTextHeight;
        for (int i = 0; i < count; i++) {
            int idx = (mIdx + SIZE + i - count) % SIZE;
            String string = mLogs[idx];
            if (string != null) {
                canvas.drawText(string, 0, y, mPaint);
            }
            y += mTextHeight;
        }
    }

    public void log(String string) {
        mLogs[mIdx] = string;
        mIdx = (mIdx + 1) % SIZE;
        postInvalidate();
    }

}
