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
package com.sonyericsson.chkbugreport.webserver;

import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.webserver.engine.HTTPRequest;
import com.sonyericsson.chkbugreport.webserver.engine.HTTPResponse;
import com.sonyericsson.chkbugreport.webserver.engine.WebApp;
import com.sonyericsson.chkbugreport.webserver.engine.WebServer;
import com.sonyericsson.chkbugreport.webserver.engine.WebServerSocket;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * This class integrates the simple and generic web server core into chkbugreport.
 * It specifies how to load static data and how to execute java code to produce dynamic pages
 * (or to load/store data)
 */
public class ChkBugReportWebServer implements WebApp {

    private Module mMod;
    private WebServer mServer;
    private WebServerSocket mSocket;
    private HashMap<String, Object> mModules = new HashMap<String, Object>();

    public ChkBugReportWebServer(Module mod) {
        mMod = mod;
        mModules.put("self", this);
    }

    public void start() {
        mServer = new WebServer(this);
        mServer.setName("ChkBugReportServer");
        mSocket = new WebServerSocket(mServer);
        mSocket.start();
        System.out.println("Webserver start, access it at http://localhost:" + mSocket.getPort());
        System.out.println("Press Ctrl+C to close it ;-)");
    }

    @Override
    public InputStream getResourceAsStream(String uri) {
        System.out.println("[FIL] " + uri);
        if (uri.equals("favicon.ico")) {
            return getClass().getResourceAsStream("/" + uri);
        }
        try {
            String root = mMod.getOutDir();
            return new FileInputStream(root + uri);
        } catch (IOException e) {
            System.out.println("Error loading file: " + e);
            return null;
        }
    }

    @Override
    public void process(String uri, HTTPRequest req, HTTPResponse resp) {
        System.out.println("[APP] " + uri);
        if (uri.equals("")) {
            mServer.serveFile("index.html", req, resp);
            return;
        }
        String clsAndMet[] = uri.split("/");
        if (clsAndMet.length != 2) {
            setError(resp, 404, "Invalid URI!");
            return;
        }
        Object obj = mModules.get(clsAndMet[0]);
        if (obj == null) {
            setError(resp, 404, "Module not found!");
            return;
        }
        if (!exec(obj, clsAndMet[1], req, resp)) {
            setError(resp, 500, "Internal server error!");
        }
    }

    private boolean exec(Object obj, String method, HTTPRequest req, HTTPResponse resp) {
        try {
            Method m = obj.getClass().getMethod(method, HTTPRequest.class, HTTPResponse.class);
            if (null == m.getAnnotation(Web.class)) {
                return false;
            }
            m.invoke(obj, req, resp);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void setError(HTTPResponse resp, int err, String msg) {
        resp.setResponseCode(err);
        resp.setBody(msg);
    }

    @Web
    public void hello(HTTPRequest req, HTTPResponse resp) {
        resp.setBody("Hello World!");
    }

    @Web
    public void version(HTTPRequest req, HTTPResponse resp) {
        resp.print("{");
        resp.print("  \"version\": \"" + Module.VERSION + "\",");
        resp.print("  \"version_code\": " + Module.VERSION_CODE);
        resp.print("}");
    }
}
