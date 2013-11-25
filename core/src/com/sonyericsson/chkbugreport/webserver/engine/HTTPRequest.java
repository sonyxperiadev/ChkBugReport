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
import java.util.HashMap;
import java.util.Set;

/**
 * Encapsulates an HTTP request, extracting the header lines and request arguments.
 */
public class HTTPRequest {

    private static final boolean DEBUG = false;

    private String mMethod;
    private String mUri;
    private String mVersion;
    private String mUriBase;
    private HashMap<String,String> mHeaders = new HashMap<String,String>();
    private HashMap<String,String> mArgs = new HashMap<String,String>();
    private InputStream mInput;
    private BufferedReader mBR;
    private int mStatus;

    public HTTPRequest(InputStream is) {
        mInput = is;
        mStatus = parse();
    }

    /**
     * Returns true if the HTTP request was parsed successfully
     */
    public boolean isValid() {
        return mStatus == 0;
    }

    /**
     * Return the HTTP request method, i.e. GET, POST, etc.
     */
    public String getMethod() {
        return mMethod;
    }

    /**
     * Return the request URI and the request arguments/parameters, e.g. index.html?foo=bar
     */
    public String getUri() {
        return mUri;
    }

    /**
     * Return the request URI, e.g. index.html?foo=bar
     */
    public String getUriBase() {
        return mUriBase;
    }

    /**
     * Return the HTTP protocol version used, e.g. 1.1
     */
    public String getVersion() {
        return mVersion;
    }

    /**
     * Return the value of a header key
     * If the header is not found, null is returned
     */
    public String getHeader(String key) {
        return mHeaders.get(key);
    }

    /**
     * Return the set of header keys (e.g. "Content-Type")
     */
    public Set<String> getHeaders() {
        return mHeaders.keySet();
    }

    /**
     * Return the value of a request parameter
     * If the parameter/argument is not present, null is returned
     */
    public String getArg(String key) {
        return mArgs.get(key);
    }

    /**
     * Return the value of a request parameter
     * If the parameter/argument is not present, the defValue is returned
     */
    public String getArg(String key, String defValue) {
        if (!mArgs.containsKey(key)) {
            return defValue;
        }
        return mArgs.get(key);
    }

    /**
     * Return the set of request parameter names
     */
    public Set<String> getArgs() {
        return mArgs.keySet();
    }

    private int parse() {
        try {
            mBR = new BufferedReader(mInput);

            // parse method uri and version
            String line = mBR.readLine();
            if (line == null) {
                System.err.println("First line empty");
                return -1;
            }

            int idx0 = line.indexOf(' ');
            if (idx0 < 0) {
                System.err.println("HTTP method not well formatted");
                return -1;
            }
            int idx1 = line.indexOf(' ', idx0+1);
            if (idx1 < 0) {
                System.err.println("HTTP method not well formatted");
                return -1;
            }

            mMethod = line.substring(0, idx0);
            mUri = line.substring(idx0 + 1, idx1);
            mVersion = line.substring(idx1+1);
            if (DEBUG) System.out.println(line);

            // Remove the starting /
            if (mUri.startsWith("/")) {
                mUri = mUri.substring(1);
            }

            if (!mMethod.equals("GET") && !mMethod.equals("HEAD") && !mMethod.equals("POST")) {
                System.err.println("Only GET, HEAD and POST are (partially) supported!");
                return -1;
            }

            // parse uri further
            int idx = mUri.indexOf('?');
            if (idx < 0) {
                mUriBase = mUri;
            } else {
                mUriBase = mUri.substring(0, idx);
                addArgs(mUri.substring(idx+1));
            }

            // process other lines
            while (true) {
                line = mBR.readLine();
                if (line == null || line.length() == 0) {
                    // stop processing the headers
                    break;
                }
                addHeader(line);
            }

            // Check if we expect a body
            if (mMethod.equals("POST")) {
                if ("application/x-www-form-urlencoded".equals(mHeaders.get("Content-Type"))) {
                    if (mHeaders.containsKey("Content-Length")) {
                        int size = Integer.parseInt(mHeaders.get("Content-Length"));
                        byte data[] = new byte[size];
                        Util.readFully(mInput, data);
                        addArgs(new String(data));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private void addHeader(String line) {
        int idx = line.indexOf(':');
        if (idx < 0) {
            System.err.println("Invalid header line: `" + line + "', ignoring");
            return;
        }
        String key = line.substring(0, idx);
        idx++;
        while (idx < line.length() && line.charAt(idx) <= ' ') {
            idx++; // skip white space
        }
        String value = line.substring(idx);
        addHeader(key, value);
    }

    private void addHeader(String key, String value) {
        if (DEBUG) System.out.println("Add header '" + key + "'='" + value + "'");
        mHeaders.put(key, value);
    }

    private void addArgs(String args) {
        while (args != null) {
            // find next key=value pair
            String arg = null;
            int idx = args.indexOf('&');
            if (idx < 0) {
                arg = args;
                args = null;
            } else {
                arg = args.substring(0, idx);
                args = args.substring(idx+1);
            }

            // split key value
            String key = null;
            String value = null;
            idx = arg.indexOf('=');
            if (idx < 0) {
                key = arg;
                value = "true";
            } else {
                key = arg.substring(0, idx);
                value = arg.substring(idx+1);
            }

            // add arg
            key = unescape(key);
            value = unescape(value);
            addArg(key, value);
        }
    }

    private String unescape(String value) {
        StringBuffer sb = new StringBuffer();
        int len = value.length();
        for (int i = 0; i < len; i++) {
            char c = value.charAt(i);
            if (c == '%') {
                if (i + 1 < len) {
                    char c2 = value.charAt(i+1);
                    if (c2 == 'u' || c2 == 'U') {
                        // Unicode character
                        if (i + 5 < len) {
                            c = (char) Integer.parseInt(value.substring(i+2, i+6), 16);
                            i += 5;
                        }
                    } else {
                        // ASCII character
                        if (i + 2 < len) {
                            c = (char) Integer.parseInt(value.substring(i+1, i+3), 16);
                            i += 2;
                        }
                    }
                }
            } else if (c == '+') {
                c = ' ';
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private void addArg(String key, String value) {
        mArgs.put(key, value);
        if (DEBUG) System.out.println("Arg: '" + key + "'='" + value + "'");
    }

}
