/*
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
package com.sonyericsson.chkbugreport.webserver.engine;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**
 * A very simple implementation of BufferedReader which has both Reader and InputStream backends.
 */
public class BufferedReader {

    private Reader mReader;
    private InputStream mStream;

    public BufferedReader(Reader reader) {
        mReader = reader;
    }

    public BufferedReader(InputStream stream) {
        mStream = stream;
    }

    public String readLine() throws IOException {
        // TODO - optimize this
        StringBuilder sb = new StringBuilder();
        while (true) {
            int ch = (mReader != null) ? mReader.read() : mStream.read();
            if (ch == -1) {
                if (sb.length() == 0) {
                    return null;
                }
                break;
            }
            if (ch == '\n') {
                break;
            }
            if (ch == '\r') {
                continue;
            }
            sb.append((char) ch);
        }
        return sb.toString();
    }

}
