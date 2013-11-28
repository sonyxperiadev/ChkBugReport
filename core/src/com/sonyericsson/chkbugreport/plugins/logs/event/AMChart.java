/*
 * Copyright (C) 2011 Sony Ericsson Mobile Communications AB
 * Copyright (C) 2012-2013 Sony Mobile Communications AB
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
package com.sonyericsson.chkbugreport.plugins.logs.event;

import com.sonyericsson.chkbugreport.ImageCanvas;
import com.sonyericsson.chkbugreport.Module;

import java.io.File;
import java.io.IOException;

/**
 * Graph/chart generated from the activity managers life cycle logs
 */
/* package */ class AMChart {
    public static final int W = 800;
    public static final int H = 25;

    public static final int STATE_UNKNOWN = -1;
    public static final int STATE_NONE = 0;
    public static final int STATE_ALIVE = 1;
    public static final int STATE_CREATED = 2;
    public static final int STATE_RESUMED = 3;

    private static final int STATE_COLORS[] = {
        0xffffffff,
        0xffffc0c0,
        0xffff8080,
        0xff00ff00,
    };

    private String mComponent;
    private ImageCanvas mImg;
    private long mTSStart;
    private long mTSEnd;
    private int mLastX = 0;
    private int mLastState = STATE_UNKNOWN;
    private int mInitState = STATE_UNKNOWN;
    private int mUsed = 0;

    public AMChart(int pid, String component, long tsStart, long tsEnd) {
        mComponent = component;
        mTSStart = tsStart;
        mTSEnd = tsEnd;
        mImg = new ImageCanvas(W, H);
        mImg.setColor(ImageCanvas.WHITE);
        mImg.fillRect(0, 0, W, H);
    }

    public static int actionToState(int action) {
        int state = STATE_UNKNOWN;
        switch (action) {
            case AMData.ON_CREATE: state = STATE_CREATED; break;
            case AMData.SCHEDULE_SERVICE_RESTART: /* fall through */
            case AMData.ON_DESTROY: state = STATE_ALIVE; break;
            case AMData.ON_PAUSE: state = STATE_CREATED; break;
            case AMData.ON_RESUME: state = STATE_RESUMED; break;
            case AMData.ON_RESTART: state = STATE_RESUMED; break;
            case AMData.PROC_START: state = STATE_ALIVE; break;
            case AMData.PROC_DIED: state = STATE_NONE; break;
        }
        return state;
    }

    public String getComponent() {
        return mComponent;
    }

    public void addData(AMData am) {
        // Calculate the new state
        int state = actionToState(am.getAction());
        if (state == STATE_UNKNOWN) {
            // cannot handle this event
            return;
        }

        // First, try to guess the previous state
        if (mLastState == STATE_UNKNOWN) {
            switch (am.getAction()) {
                case AMData.ON_CREATE: mLastState = STATE_ALIVE; break;
                case AMData.SCHEDULE_SERVICE_RESTART: /* fall through */
                case AMData.ON_DESTROY: mLastState = STATE_CREATED; break;
                case AMData.ON_PAUSE: mLastState = STATE_RESUMED; break;
                case AMData.ON_RESUME: mLastState = STATE_CREATED; break;
                case AMData.ON_RESTART: mLastState = STATE_ALIVE; break;
                case AMData.PROC_START: mLastState = STATE_NONE; break;
                case AMData.PROC_DIED: mLastState = STATE_ALIVE; break;
            }
            mInitState = mLastState;
        }

        // Calculate the new state
        int x = (int)(W * (am.getTS() - mTSStart) / (mTSEnd - mTSStart));

        if (mLastState != STATE_UNKNOWN) {
            // Render the state
            drawState(x);
        }

        mLastX = x;
        mLastState = state;
    }

    private void drawState(int x) {
        if (mLastX >= x) {
            mImg.setColor(ImageCanvas.YELLOW);
            mImg.fillRect(mLastX, 0, 1, H);
        } else {
            mImg.setColor(STATE_COLORS[mLastState]);
            mImg.fillRect(mLastX + 1, 0, x - mLastX + 1, H);
        }
        mUsed++;
    }

    public String finish(Module br) {
        // Finish the rendering (render the last state)
        if (mLastState != STATE_UNKNOWN) {
            drawState(W);
        }

        if (mUsed == 0) {
            // Noting was rendered, so don't save the empty image
            return null;
        }

        // Save the image
        String fn = "amchart_" + hashCode() + ".png";
        try {
            mImg.writeTo(new File(br.getBaseDir() + fn));
            mImg = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fn;
    }

    public int getInitState() {
        return mInitState;
    }

}
