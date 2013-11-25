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
package com.sonyericsson.chkbugreport.traceview;

import java.io.IOException;
import java.io.InputStream;

/**
 * Helper methods to read values from InoutStreams
 */
public final class StreamUtil {

    public static int read2LE(InputStream is) throws IOException {
        int lo = is.read();
        int hi = is.read();
        if (lo < 0 || hi < 0) {
            throw new IOException("premature EOF");
        }
        return (hi << 8) | lo;
    }

    public static int read4LE(InputStream is) throws IOException {
        int ret = 0;
        for (int i = 0; i < 4; i++) {
            int b = is.read();
            if (b < 0) {
                throw new IOException("premature EOF");
            }
            ret = (ret << 8) | b;
        }
        return ret;
    }

    public static int read4BE(InputStream is) throws IOException {
        int ret = 0;
        for (int i = 0; i < 4; i++) {
            int b = is.read();
            if (b < 0) {
                throw new IOException("premature EOF");
            }
            ret = (ret >>> 8) | (b << 24);
        }
        return ret;
    }

    public static long read8LE(InputStream is) throws IOException {
        long ret = 0;
        for (int i = 0; i < 8; i++) {
            int b = is.read();
            if (b < 0) {
                throw new IOException("premature EOF");
            }
            ret = (ret << 8) | b;
        }
        return ret;
    }

    public static String readLine(InputStream is) throws IOException {
        char buff[] = new char[1024];
        int idx = 0;
        while (true) {
            int c = is.read();
            if (c == -1 || c == '\n') break;
            buff[idx++] = (char) c;
        }
        return new String(buff, 0, idx);
    }

}
