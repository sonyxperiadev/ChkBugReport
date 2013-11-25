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
package com.sonyericsson.chkbugreport.util;

import java.io.PrintStream;
import java.util.Vector;

/**
 * Helper methods to generate the skeleton of the html files.
 */
public final class HtmlUtil {

    private static Vector<String> sJS = new Vector<String>();

    static {
        sJS.add("jquery.js");
        sJS.add("jquery.cookie.js");
        sJS.add("jquery.hotkeys.js");
        sJS.add("jquery.jstree.js");
        sJS.add("jquery.tablesorter.js");
        sJS.add("jquery.tablednd.js");
        sJS.add("jquery.treeTable.js");
        sJS.add("jquery.flot.js");
        sJS.add("jquery.flot.navigate.js");
        sJS.add("jquery.flot.selection.js");
        sJS.add("jquery.ui.js");
        sJS.add("colResizable-1.3.source.js");
        sJS.add("/self$wsjs");
        sJS.add("main.js");
    }

    /**
     * Add a new javascript file to be copied to the output folder
     * @param filename The name of the javascript file
     */
    public static void addJS(String filename) {
        sJS.add(filename);
    }

    /**
     * Prepare a string to be rendered in html.
     * It escapes some characters to show it properly.
     * @param line The string to escape
     * @return The escaped string
     */
    public static String escape(String line) {
        line = line.replace("&", "&amp;");
        line = line.replace("\"", "&quot;");
        line = line.replace(">", "&gt;");
        line = line.replace("<", "&lt;");
        return line;
    }

    /**
     * Parse html/xml escape sequences
     * @param line The string which is escaped
     * @return The normal string
     */
    public static String unescape(String line) {
        line = line.replace("&amp;", "&");
        line = line.replace("&quot;", "\"");
        line = line.replace("&gt;", ">");
        line = line.replace("&lt;", "<");
        return line;
    }

    public static void writeHTMLHeader(PrintStream out, String title, String pathToData) {
        out.println("<html>");
        out.println("<head>");
        out.println("  <title>" + title + "</title>");
        out.println("  <link rel=\"stylesheet\" type=\"text/css\" href=\"" + pathToData + "themes/blue/style.css\"/>");
        out.println("  <link rel=\"stylesheet\" type=\"text/css\" href=\"" + pathToData + "jquery.treeTable.css\"/>");
        out.println("  <link rel=\"stylesheet\" type=\"text/css\" href=\"" + pathToData + "jquery.ui.css\"/>");
        out.println("  <link rel=\"stylesheet\" type=\"text/css\" href=\"" + pathToData + "style.css\"/>");
        for (String js : sJS) {
            if (!js.startsWith("http:") && !js.startsWith("/")) {
                js = pathToData + js;
            }
            out.println("  <script type=\"text/javascript\" src=\"" + js + "\"></script>");
        }
        out.println("</head>");
        out.println("<body>");
    }

    public static void writeHTMLHeaderLite(PrintStream out, String title) {
        out.println("<html>");
        out.println("<head>");
        out.println("  <title>" + title + "</title>");
        out.println("</head>");
    }

    public static void writeHTMLFooter(PrintStream out) {
        out.println("</body>");
        out.println("</html>");
    }

    public static void writeHTMLFooterLite(PrintStream out) {
        out.println("</html>");
    }

}
