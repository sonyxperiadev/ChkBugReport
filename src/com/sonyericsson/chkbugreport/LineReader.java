/*
 * Copyright (C) 2011 Sony Ericsson Mobile Communications AB
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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class LineReader {

    private InputStream mIs;

    public LineReader(InputStream is) {
        mIs = new BufferedInputStream(is);
    }

    public String readLine() {
        StringBuffer sb = new StringBuffer();
        try {
            while (true) {
                int b = mIs.read();
                if (b < 0) {
                    if (sb.length() == 0) return null;
                    break; // EOF
                }
                if (b == 0xd) continue; // Skip ungly windows line ending
                if (b == 0xa) break; // EOL
                sb.append((char)b);
            }
        } catch (IOException e) {
            // Ignore exception
            e.printStackTrace();
            return null;
        }
        return sb.toString();
    }

    public void close() {
        try {
            mIs.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
