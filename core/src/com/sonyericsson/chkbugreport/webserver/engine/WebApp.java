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

import java.io.InputStream;

/**
 * This interface must be implemented by the code which uses the webserver.
 * It is used to fetch static and dynamic pages.
 */
public interface WebApp {

    /**
     * Returns an InputStream which provides the specified static page
     */
    public InputStream getResourceAsStream(String uri);

    /**
     * Placeholder to process an HTTP request to create a dynamic page
     */
    public void process(String clsRef, String metRef, HTTPRequest req, HTTPResponse resp);

}
