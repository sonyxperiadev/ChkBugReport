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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class WebServer {

    private static final String ROOT = "";

    private WebApp mApp;
    private String mName = "TinyWebServer";

    /**
     * Creates a new instance of the web server specifying the callback to use to
     * fetch the content.
     */
    public WebServer(WebApp app) {
        mApp = app;
    }

    public void setName(String name) {
        mName = name;
    }

    public void process(InputStream is, OutputStream os) {
        HTTPRequest req = new HTTPRequest(is);
        HTTPResponse resp = new HTTPResponse(os);
        if (req.isValid()) {
            process(req, resp);
        } else {
            resp.setResponseCode(500);
        }
        resp.flush();
    }

    public void process(HTTPRequest req, HTTPResponse resp) {
        try {
            String uriBase = req.getUriBase();
            resp.addHeader("Server", mName);
            // Routing
            if (uriBase.equals(ROOT)) {
                // Default root page requested
                mApp.process(ROOT, ROOT, req, resp);
            } else if (uriBase.contains("$")) {
                for (String f : uriBase.split("/")) {
                    if (f.contains("$")) {
                        String cm[] = f.split("\\$", 2);
                        mApp.process(cm[0], cm[1], req, resp);
                        break;
                    }
                }
            } else {
                // Data from root module requested
                serveFile(uriBase, req, resp);
            }
        } catch (Throwable e) {
            resp.setResponseCode(500);
            resp.clearBody();
            e.printStackTrace();
            resp.println("Internal server error: " + e);
        }
    }

    public void serveFile(String uriBase, HTTPRequest req, HTTPResponse resp) {
        InputStream is = mApp.getResourceAsStream(uriBase);
        byte data[] = readFile(is);

        if (data == null) {
            handle404(req, resp);
            return;
        }

        resp.setBody(data);
        resp.addHeader("Content-Type", getContentTypeOf(uriBase));
    }

    private void handle404(HTTPRequest req, HTTPResponse resp) {
        resp.setResponseCode(404);
        resp.println("Cannot open file '" + req.getUri() + "'");
        System.err.println("Cannot open file '" + req.getUri() + "'");
    }

    private String getContentTypeOf(String file) {
        String lc = file.toLowerCase();
        if (lc.endsWith(".jpg")) return "image/jpeg";
        if (lc.endsWith(".png")) return "image/png";
        if (lc.endsWith(".gif")) return "image/gif";
        if (lc.endsWith(".html")) return "text/html";
        if (lc.endsWith(".htm")) return "text/html";
        if (lc.endsWith(".txt")) return "text/plain";
        if (lc.endsWith(".xml")) return "text/xml";
        if (lc.endsWith(".js")) return "application/x-javascript";
        if (lc.endsWith(".css")) return "text/css";
        return "application/octet-stream";
    }

    private byte[] readFile(InputStream is) {
        try {
            if (is == null) {
                return null;
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte buff[] = new byte[1024];
            while (true) {
                int read = is.read(buff);
                if (read < 0) break;
                bos.write(buff, 0, read);
            }
            is.close();
            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
