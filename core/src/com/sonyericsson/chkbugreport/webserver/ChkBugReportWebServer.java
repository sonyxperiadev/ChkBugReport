/*
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
package com.sonyericsson.chkbugreport.webserver;

import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.PlatformUtil;
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
    private int mServerPort = 0;

    public ChkBugReportWebServer(Module mod) {
        mMod = mod;
        mModules.put("self", this);
        mod.setWebServer(this);
    }

    public Module getModule() {
        return mMod;
    }

    public void addModule(String location, Object module) {
        mModules.put(location, module);
    }

    public void setPort(int serverPort) {
        mServerPort = serverPort;
    }

    public void start(boolean startBrowser) {
        mServer = new WebServer(this);
        mServer.setName("ChkBugReportServer");
        mSocket = new WebServerSocket(mServer);
        if (mServerPort > 0) {
            mSocket.setPort(mServerPort);
        }
        mSocket.start();
        System.out.println("Webserver start, access it at http://localhost:" + mSocket.getPort());
        System.out.println("Press Ctrl+C to close it ;-)");
        if (startBrowser) {
            PlatformUtil.openUri("http://localhost:" + mSocket.getPort());
        }
    }

    @Override
    public InputStream getResourceAsStream(String uri) {
        System.out.println("[FIL] " + uri);
        if (uri.equals("favicon.ico")) {
            return getClass().getResourceAsStream(PlatformUtil.ASSETS_ROOT + uri);
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
    public void process(String clsRef, String metRef, HTTPRequest req, HTTPResponse resp) {
        System.out.println("[APP] " + clsRef + " / " + metRef);
        if (clsRef.equals("")) {
            mServer.serveFile("index.html", req, resp);
            return;
        }
        Object obj = mModules.get(clsRef);
        if (obj == null) {
            setError(resp, 404, "Module not found!");
            return;
        }
        if (!exec(obj, metRef, req, resp)) {
            setError(resp, 500, "Internal server error!");
        }
    }

    private void setError(HTTPResponse resp, int err, String msg) {
        resp.setResponseCode(err);
        resp.setBody(msg);
    }

    private boolean exec(Object obj, String method, HTTPRequest req, HTTPResponse resp) {
        try {
            Method m = obj.getClass().getMethod(method, Module.class, HTTPRequest.class, HTTPResponse.class);
            if (null == m.getAnnotation(Web.class)) {
                return false;
            }
            m.invoke(obj, mMod, req, resp);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Web
    public void hello(Module mod, HTTPRequest req, HTTPResponse resp) {
        resp.setBody("Hello World!");
    }

    @Web
    public void version(Module mod, HTTPRequest req, HTTPResponse resp) {
        resp.println("{");
        resp.println("  \"version\": \"" + Module.VERSION + "\",");
        resp.println("  \"version_code\": " + Module.VERSION_CODE);
        resp.println("}");
    }

    @Web
    public void wsjs(Module mod, HTTPRequest req, HTTPResponse resp) {
        try {
            resp.setBody(getClass().getResourceAsStream(PlatformUtil.ASSETS_ROOT + "ws.js"), "text/javascript");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
