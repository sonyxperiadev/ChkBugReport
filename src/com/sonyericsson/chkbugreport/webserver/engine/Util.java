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

public class Util {

    /**
     * Encodes a string to be able to send as xml
     * @param string the input string
     * @return the encoded string
     */
    public static String xmlEncode(String string) {
        if (string == null) return null;

        string = string.replace("&", "&amp;");
        string = string.replace("<", "&lt;");
        string = string.replace(">", "&gt;");
        string = string.replace("\"", "&quot;");
        return string;
    }

    /**
     * Reads a byte array completely, if possible, without needing to keep re-calling read.
     */
    public static void readFully(InputStream is, byte[] data) throws IOException {
        readFully(is, data, 0, data.length);
    }

    /**
     * Reads a byte array completely, if possible, without needing to keep re-calling read.
     */
    public static void readFully(InputStream is, byte[] data, int ofs, int length) throws IOException {
        while (length > 0) {
            int read = is.read(data, ofs, length);
            if (read <= 0) break;
            ofs += read;
            length -= read;
        }
    }

}
