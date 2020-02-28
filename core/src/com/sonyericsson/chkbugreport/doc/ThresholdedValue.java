/*
 * Copyright (C) 2020 Sony Mobile Communications AB
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
package com.sonyericsson.chkbugreport.doc;

import java.io.IOException;

public class ThresholdedValue extends DocNode {

    private long mValue;
    private long mThreshold;

    public ThresholdedValue(long value, long threshold) {
        mValue = value;
        mThreshold = threshold;
    }

    @Override
    public String getText() {
        return Long.toString(mValue);
    }

    @Override
    public void render(Renderer r) throws IOException {
        String className = mValue > mThreshold ? "highlight-by-threshold" : "";
        r.print("<span class=\"" + className + "\">");
        r.print(mValue);
        r.print("</span>");
    }


}
