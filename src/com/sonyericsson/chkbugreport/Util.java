/*
 * Copyright (C) 2011 Sony Ericsson Mobile Communications AB
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
package com.sonyericsson.chkbugreport;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import javax.imageio.ImageIO;

public class Util {

    public static final String COMMON_RES[] = {
        "/style.css",
        "/icons.png",
        "/ftrace-legend-dred.png",
        "/ftrace-legend-black.png",
        "/ftrace-legend-yellow.png",
        "/ftrace-legend-red.png",
        "/ftrace-legend-cyan.png",
        "/ftrace-legend-dcyan.png",
        "/pcy_p0.png",
        "/pcy_p1.png",
        "/pcy_p2.png",
        "/pcy_p3.png",
        "/pcy_p4.png",
        "/pcy_un.png",
        "/pcy_rt.png",
        "/pcy_fg.png",
        "/pcy_bg.png",
        "/main.js",
        "/jquery.js",
        "/jquery.cookie.js",
        "/jquery.jstree.js",
        "/jquery.hotkeys.js",
        "/jquery.tablesorter.js",
        "/jquery.tablednd.js",
        "/themes/classic/d.png",
        "/themes/classic/dot_for_ie.gif",
        "/themes/classic/throbber.gif",
        "/themes/classic/style.css",
        "/themes/blue/desc.gif",
        "/themes/blue/bg.gif",
        "/themes/blue/style.css",
        "/themes/blue/asc.gif",
    };

    private static final int[] COLORS = {
        0xff0000, 0x00ff00, 0x0000ff, 0x00ffff, 0xff00ff, 0xffff00,
        0xff8000, 0x80ff00, 0x8000ff, 0xff0080, 0x00ff80, 0x0080ff,
        0xff8080, 0x80ff80, 0x8080ff,
        0x800000, 0x008000, 0x000080, 0x008080, 0x800080, 0x808000,
    };

    private static Vector<String> sJS = new Vector<String>();

    static {
        sJS.add("jquery.js");
        sJS.add("jquery.cookie.js");
        sJS.add("jquery.hotkeys.js");
        sJS.add("jquery.jstree.js");
        sJS.add("jquery.tablesorter.js");
        sJS.add("jquery.tablednd.js");
        sJS.add("main.js");
    }

    /**
     * Prepare a string to be rendered in html.
     * It escapes some characters to show it properly.
     * @param line The string to escape
     * @return The escaped string
     */
    public static String escape(String line) {
        line = line.replace("&", "&amp;");
        line = line.replace(">", "&gt;");
        line = line.replace("<", "&lt;");
        return line;
    }

    /**
     * Removes the extra whitespaces from the beginning and end of a string
     * @param s The string to be stripped
     * @return the stripped string
     */
    public static String strip(String s) {
        if (s == null) {
            return null;
        }
        int b = 0, e = s.length();
        while (b < e) {
            char c = s.charAt(b);
            if (c == '\r' || c == '\n' || c == ' ' || c == '\t') {
                b++;
            } else {
                break;
            }
        }
        while (b < e) {
            char c = s.charAt(e - 1);
            if (c == '\r' || c == '\n' || c == ' ' || c == '\t') {
                e--;
            } else {
                break;
            }
        }
        return s.substring(b, e);
    }

    /**
     * Return the anchor identifier used for a given process
     * @param pid The process pid
     * @return The anchor identifier
     */
    public static String getProcessRecordAnchor(int pid) {
        return "process_" + pid;
    }

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
            char c = (char)is.read();
            if (c == -1 || c == '\n') break;
            buff[idx++] = c;
        }
        return new String(buff, 0, idx);
    }

    public static void writeHTMLHeader(PrintStream out, String title, String pathToData) {
        out.println("<html>");
        out.println("<head>");
        out.println("  <title>" + title + "</title>");
        out.println("  <link rel=\"stylesheet\" type=\"text/css\" href=\"" + pathToData + "themes/blue/style.css\"/>");
        out.println("  <link rel=\"stylesheet\" type=\"text/css\" href=\"" + pathToData + "style.css\"/>");
        for (String js : sJS) {
            if (!js.startsWith("http:")) {
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

    public static boolean createTimeBar(Report br, String fn, int w, long ts0, long ts1) {
        int h = 75;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D)img.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.setColor(Color.BLACK);
        g.drawLine(0, h - 1, w, h - 1);

        if (!renderTimeBar(img, g, 0, 0, w, h, ts0, ts1, false)) {
            return false;
        }

        // Save the image
        try {
            ImageIO.write(img, "png", new File(br.getBaseDir() + br.getRelDataDir() + fn));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    public static boolean renderTimeBar(BufferedImage img, Graphics2D g, int ox, int oy, int w, int h, long ts0, long ts1, boolean vFlip) {
        boolean useMS = false;
        int count = 10; // let's assume we will show 10 marks
        int slice = 0;

        if (ts1 <= ts0) return false; // sanity check

        if (ts1 <= 10*1000) {
            // if the total time is less then 10 seconds, prefer milliseconds formatting
            useMS = true;
        }

        if (useMS) {
            slice = (int)((ts1 - ts0) / count); // the size of a slice, we need to round this down
            int lg = (int)(Math.log(slice) / Math.log(10) - 0.5);
            int mask = (int)Math.pow(10, lg);
            if (mask != 0) {
                slice -= slice % mask;
            }
        } else {
            ts0 /= 1000; // forget about
            ts1 /= 1000; // the milliseconds

            slice = (int)((ts1 - ts0) / count); // the size of a slice, we need to round this down
            if (slice < 5) {
                // small enough, leave it as it is
            } else if (slice < 60) {
                // less then a minute, round it to multiple of 5 seconds
                slice -= slice % 5;
            } else if (slice < 5*60) {
                // less then 5 minutes, round it to a whole minute
                slice -= (slice % 60);
            } else if (slice < 60*60) {
                // less then an hour, round it to a multiple of 5 minutes
                slice -= (slice % (5*60));
            } else {
                // round it to hour
                slice -= (slice % (60*60));
            }
        }

        if (slice <= 0) {
            slice = 1;
        }

        // find the first label, it must be a multiple of slice
        int ts = (int)ts0;
        ts -= ts % slice;
        if (ts < ts0) {
            ts += slice;
        }

        // Render the labels
        g.setColor(Color.BLACK);
        while (ts < ts1) {
            int x = ox + (int)(w * (ts - ts0) / (ts1 - ts0));
            if (vFlip) {
                g.drawLine(x, oy, x, oy + 10);
            } else {
                g.drawLine(x, oy + h, x, oy + h - 10);
            }

            String s = null;
            if (useMS) {
                int sec = ts / 1000;
                int ms = ts % 1000;
                s = String.format("%d.%03ds", sec, ms);
            } else {
                int sec = ts % 60;
                int min = (ts / 60) % 60;
                int hour = (ts / 3600) % 24;
                s = String.format("%02d:%02d:%02d", hour, min, sec);
            }
            double angle = (Math.PI / 4) * (vFlip ? +1 : -1);
            int y = vFlip ? (oy + 10) : (oy + h - 10);
            g.translate(x, y);
            g.rotate(angle);
            g.drawString(s, 0, 0);
            g.rotate(-angle);
            g.translate(-x, -y);

            ts += slice;
        }

        return true;
    }

    /**
     * Return a (possibly) unique color to render data #idx
     * @param idx
     * @return An RGB color value
     */
    public static int getColor(int idx) {
        return COLORS[idx % COLORS.length];
    }

    public static String calcMD5(File f) {
        try {
            FileInputStream is = new FileInputStream(f);
            String ret = calcMD5(is);
            is.close();
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String calcMD5(InputStream is) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte buff[] = new byte[0x10000];
            while (true) {
                int read = is.read(buff);
                if (read <= 0) break;
                md.update(buff, 0, read);
            }
            StringBuffer sb = new StringBuffer();
            byte[] hash = md.digest();
            for (byte b : hash) {
                sb.append(Integer.toHexString((b >> 4) & 0xf));
                sb.append(Integer.toHexString((b >> 0) & 0xf));
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String shadeValue(long value) {
        return shadeValue(value, "kb");
    }

    public static String shadeValue(long value, String css) {
        StringBuffer sb = new StringBuffer();
        long mb = value / 1000;
        long kb = value % 1000;
        if (mb != 0) {
            sb.append(mb);
        }
        sb.append("<span class=\"" + css + "\">");
        if (kb < 100) {
            sb.append('0');
        }
        if (kb < 10) {
            sb.append('0');
        }
        sb.append(kb);
        sb.append("</span>");
        return sb.toString();
    }

    /**
     * Extracts a bit range from a long value
     * @param value The bitmask
     * @param sb The starting bit (inclusive)
     * @param eb The ending bit (inclusive), must be < sb
     * @return The extracted value
     */
    public static long bits(long value, int sb, int eb) {
        value >>>= eb;
        sb -= eb - 1;
        long mask = (1L << sb) -1;
        value &= mask;
        return value;
    }

    /**
     * Creates a timestamp string
     * @return A string containing a timestamp
     */
    public static String createTimeStamp() {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        Date date = new Date(System.currentTimeMillis());
        return fmt.format(date);
    }

    public static int indexOf(String buff, String key, int delta) {
        int pos = buff.indexOf(key);
        if (pos < 0) return -1;
        return pos + delta;
    }

    private static boolean isInvalidChar(char c) {
        if (c == '_') return false;
        if (c == '/') return false;
        if (c == '\\') return false;
        if (c >= '0' && c <= '9') return false;
        if (c >= 'a' && c <= 'z') return false;
        if (c >= 'A' && c <= 'Z') return false;
        return true;
    }

    /**
     * Fix a name so it can be used in VCD files
     * @param name Any name
     * @return A name where invalid characters are replaced with underscore
     */
    public static String fixVCDName(String name) {
        char[] ret = name.toCharArray();
        for (int i = 0; i < name.length(); i++) {
            if (isInvalidChar(ret[i])) {
                ret[i] = '_';
            }
        }
        return new String(ret);
    }

    /**
     * Converts an integer to a binary string
     * @param value The integer value
     * @param bits The number of bits to use
     * @return The generated string
     */
    public static String toBinary(int value, int bits) {
        StringBuffer sb = new StringBuffer();
        int mask = 1 << (bits - 1);
        for (int i = 0; i < bits; i++) {
            if ((value & mask) == 0) {
                sb.append('0');
            } else {
                sb.append('1');
            }
            mask >>= 1;
        }
        return sb.toString();
    }

    /**
     * Parse a substring as integer, stripping it first.
     * @param s The string to parse
     * @param from The first index of the substring
     * @param to The last+1 index of the substring
     * @return The parsed integer value
     * @throws NumberFormatException if the number is invalid
     */
    public static int parseInt(String s, int from, int to) {
        return parseInt(s.substring(from, to));
    }

    /**
     * Parse a string as integer, stripping it first.
     * @param s The string to parse
     * @return The parsed integer value
     * @throws NumberFormatException if the number is invalid
     */
    public static int parseInt(String s) {
        s = strip(s);
        return Integer.parseInt(s);
    }

    /**
     * Parse a substring as hexadecimal integer, stripping it first.
     * @param s The string to parse
     * @param from The first index of the substring
     * @param to The last+1 index of the substring
     * @return The parsed integer value
     * @throws NumberFormatException if the number is invalid
     */
    public static int parseHex(String s, int from, int to) {
        return parseHex(s.substring(from, to));
    }

    /**
     * Parse a string as hexadecimal integer, stripping it first.
     * @param s The string to parse
     * @return The parsed integer value
     * @throws NumberFormatException if the number is invalid
     */
    public static int parseHex(String s) {
        s = strip(s).toLowerCase();
        if (s.startsWith("0x")) {
            s = s.substring(2);
        }
        if (s.startsWith("#")) {
            s = s.substring(1);
        }
        return (int) Long.parseLong(s, 16);
    }

    /**
     * Parse a substring as float, stripping it first.
     * @param s The string to parse
     * @param from The first index of the substring
     * @param to The last+1 index of the substring
     * @return The parsed float value
     * @throws NumberFormatException if the number is invalid
     */
    public static float parseFloat(String s, int from, int to) {
        return parseFloat(s.substring(from, to));
    }

    /**
     * Parse a string as float, stripping it first.
     * @param s The string to parse
     * @return The parsed float value
     * @throws NumberFormatException if the number is invalid
     */
    public static float parseFloat(String s) {
        s = strip(s);
        return Float.parseFloat(s);
    }

    public static boolean parseBoolean(String line, int from, int to) {
        return parseBoolean(line.substring(from, to));
    }

    public static boolean parseBoolean(String s) {
        s = strip(s);
        return (s.equals("true"));
    }

    public static String extract(String buff, String startKey, String endKey) {
        int idx0 = buff.indexOf(startKey);
        if (idx0 < 0) return null;
        idx0 += startKey.length();
        if (endKey == null) {
            return buff.substring(idx0);
        } else {
            int idx1 = buff.indexOf(endKey, idx0);
            if (idx1 < 0) {
                return buff.substring(idx0);
            } else {
                return buff.substring(idx0, idx1);
            }
        }
    }

    public static String simplifyComponent(String cmp) {
        if (cmp == null) return null;
        int idx = cmp.indexOf('/');
        if (idx > 0) {
            String pkg = cmp.substring(0, idx);
            String cls = cmp.substring(idx + 1);
            if (cls.startsWith(pkg)) {
                cls = cls.substring(pkg.length());
                cmp = pkg + "/" + cls;
            }
        }
        return cmp;
    }

    public static boolean isEmpty(String s) {
        if (s == null) return true;
        if (s.length() == 0) return true;
        return false;
    }

    public static void skip(InputStream is, long amount) throws IOException {
        while (amount > 0) {
            long skipped = is.skip(amount);
            if (skipped < 0) return;
            amount -= skipped;
        }
    }

    public static void skipToEol(InputStream is) throws IOException {
        while (true) {
            int c = is.read();
            if (c == -1 || c == '\n') break;
        }
    }

    public static String getValueAfter(String line, char sep) {
        int idx = line.indexOf(sep);
        if (idx < 0) {
            return null;
        }
        int len = line.length();
        do {
            idx++;
        } while (idx < len && line.charAt(idx) == ' ');
        if (idx == len) {
            return null;
        }
        return line.substring(idx, len);
    }

    public static String getKeyBefore(String line, char sep) {
        int idx = line.indexOf(sep);
        if (idx < 0) {
            return null;
        }
        return line.substring(0, idx);
    }

    public static String getSchedImg(int pcy) {
        String ret = "un";
        switch (pcy) {
            case PSRecord.PCY_NORMAL: ret = "fg"; break;
            case PSRecord.PCY_BATCH: ret = "bg"; break;
            case PSRecord.PCY_FIFO: ret = "rt"; break;
        }
        return "<img src=\"pcy_" + ret + ".png\"/>";
    }

    public static String getNiceImg(int nice) {
        String ret = "un";
        if (nice == PSRecord.NICE_UNKNOWN) {
            ret = "un";
        } else if (nice <= -10) {
            ret = "p0";
        } else if (nice < 0) {
            ret = "p1";
        } else if (nice == 0) {
            ret = "p2";
        } else if (nice >= 10) {
            ret = "p4";
        } else if (nice > 0) {
            ret = "p3";
        }
        return "<img src=\"pcy_" + ret + ".png\"/>";
    }

    public static void addJS(String string) {
        sJS.add(string);
    }
}
