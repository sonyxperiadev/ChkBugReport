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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;

/**
 * Encapsulates an HTTP response, with return code, headers and body.
 * The body is either a text, constructed with calls to print(), or provided as a byte array.
 * NOTE: the data is sent when flush() is called.
 */
public class HTTPResponse {

    private static final boolean DEBUG = false;

    private int mReturnCode = 200;
    private StringBuilder mBody = new StringBuilder();
    private byte mBodyData[];
    private PrintStream mPrintStream;
    private HashMap<String, String> mHeaders = new HashMap<String, String>();

    public HTTPResponse(OutputStream os) {
        mPrintStream = new PrintStream(os);
        mHeaders.put("Connection", "close"); // We don't support anything else
        mHeaders.put("Content-Type", "text/html; charset=UTF-8"); // Default type
    }

    /**
     * Set the HTTP response code, i.e. 200 if everything is ok, 404 if the resource is missing
     */
    public void setResponseCode(int code) {
        mReturnCode = code;
    }

    /**
     * Prints a line in the body.
     * NOTE: an extra carriege return character will be added.
     */
    public void print(String string) {
        mBody.append(string);
        mBody.append('\n');
    }

    /**
     * Resets the body to an empty text
     */
    public void clearBody() {
        mBody.setLength(0);
        mBodyData = null;
    }

    /**
     * Sets the body to the specified byte array.
     * Clears any previous content
     */
    public void setBody(byte data[]) {
        mBody = null;
        mBodyData = data;
    }

    /**
     * Sets the body to the specified text.
     * Clears any previous content
     */
    public void setBody(String body) {
        mBody = new StringBuilder(body);
        mBodyData = null;
    }

    /**
     * Sets the body to the content of the specified file.
     * Also sets the Content-Type header.
     * Clears any previous content
     */
    public void setBody(File f, String mime) throws IOException {
        FileInputStream is = new FileInputStream(f);
        setBody(is, mime);
        is.close();
    }

    /**
     * Sets the body to the content of the specified input stream.
     * Also sets the Content-Type header.
     * Clears any previous content
     */
    public void setBody(InputStream is, String mime) throws IOException {
        byte buffer[] = new byte[0x1000];
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        while (true) {
            int read = is.read(buffer, 0, buffer.length);
            if (read <= 0) break;
            bos.write(buffer, 0, read);
        }
        bos.close();
        setBody(bos.toByteArray());
        addHeader("Content-Type", mime);
    }

    /**
     * Adds a new header.
     * The header will be constructed as "key: value"
     */
    public void addHeader(String key, String value) {
        mHeaders.put(key, value);
    }

    /**
     * Sends the response to the specified output stream.
     */
    public void flush() {
        // write respone line
        String status = null;
        if (mReturnCode == 200) {
            status = "HTTP/1.1 200 OK";
        } else {
            status = "HTTP/1.1 " + mReturnCode + " Error";
        }
        mPrintStream.println(status);
        if (DEBUG) System.out.println("> " + status);

        // update content length
        addHeader("Content-Length", Integer.toString(getContentLength()));

        // write header
        for (String key : mHeaders.keySet()) {
            String value = mHeaders.get(key);
            mPrintStream.println(key + ": " + value);
            if (DEBUG) System.out.println("> " + key + ": " + value);
        }

        // body separator
        mPrintStream.println();

        // body
        if (mBody !=  null) {
            mPrintStream.print(mBody);
        } else {
            try {
                if (DEBUG) System.out.println(">> [binary data of size " + mBodyData.length + "]");
                mPrintStream.write(mBodyData);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        mPrintStream.close();
    }

    private int getContentLength() {
        if (mBody != null) {
            // NOTE: we must return the length in bytes, not the length in characters
            return mBody.toString().getBytes().length;
        } else {
            return mBodyData.length;
        }
    }

}
