/*
 * Copyright (C) 2011 Sony Ericsson Mobile Communications AB
 * Copyright (C) 2012 Sony Mobile Communications AB
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

import java.io.IOException;
import java.io.InputStream;

/* package */ class SectionInputStream extends InputStream {

    private Section mSection;
    private int mLineIdx;
    private String mBuff;
    private int mCharIdx;

    public SectionInputStream(Section s) {
        mSection = s;
    }

    @Override
    public int read() throws IOException {
        if (mSection == null) return -1;

        // Check if we need to fill the buffer
        while (mBuff == null || mCharIdx == mBuff.length()) {
            mCharIdx = 0;
            if (mLineIdx < mSection.getLineCount()) {
                mBuff = mSection.getLine(mLineIdx++);
                if (mLineIdx > 1) {
                    return '\n';
                }
            } else {
                mBuff = null;
                return -1; // EOF
            }
        }

        // We now have something in the buffer
        return mBuff.charAt(mCharIdx++);
    }

}
