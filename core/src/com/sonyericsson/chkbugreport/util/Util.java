/*
 * Copyright (C) 2011 Sony Ericsson Mobile Communications AB
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
package com.sonyericsson.chkbugreport.util;

import com.sonyericsson.chkbugreport.ImageCanvas;
import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.PlatformUtil;
import com.sonyericsson.chkbugreport.doc.Renderer;
import com.sonyericsson.chkbugreport.ps.PSRecord;
import com.sonyericsson.chkbugreport.webserver.engine.BufferedReader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A collection of various helper methods
 */
public final class Util {

    /**
     * The name of the folder where configurations and plugins are stored.
     * This is relative to the user's home directory.
     */
    public static final String PRIVATE_DIR_NAME = ".chkbugreport";
    public static final String EXTERNAL_PLUGINS_ALT_DIR_NAME = "extplugins";

    /** Length of 1 second in milliseconds */
    public static final long SEC_MS = 1000;
    /** Length of 1 minute in milliseconds */
    public static final long MIN_MS = 60 * SEC_MS;
    /** Length of 1 hour in milliseconds */
    public static final long HOUR_MS = 60 * MIN_MS;
    /** Length of 1 day in milliseconds */
    public static final long DAY_MS = 24 * HOUR_MS;

    /** Size of byte in bytes */
    public static final int B = 1;
    /** Size of kilobyte in bytes */
    public static final int KB = 1024*B;
    /** Size of megabyte in bytes */
    public static final int MB = 1024*KB;

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

    public static boolean createTimeBar(Module br, String fn, int w, long ts0, long ts1) {
        int h = 75;
        ImageCanvas img = new ImageCanvas(w, h);
        img.setColor(ImageCanvas.WHITE);
        img.fillRect(0, 0, w, h);
        img.setColor(ImageCanvas.BLACK);
        img.drawLine(0, h - 1, w, h - 1);

        if (!renderTimeBar(img, 0, 0, w, h, ts0, ts1, false)) {
            return false;
        }

        // Save the image
        try {
            img.writeTo(new File(br.getBaseDir() + fn));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    public static boolean renderTimeBar(ImageCanvas img, int ox, int oy, int w, int h, long ts0, long ts1, boolean vFlip) {
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
        img.setColor(ImageCanvas.BLACK);
        while (ts < ts1) {
            int x = ox + (int)(w * (ts - ts0) / (ts1 - ts0));
            if (vFlip) {
                img.drawLine(x, oy, x, oy + 10);
            } else {
                img.drawLine(x, oy + h, x, oy + h - 10);
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
            float angle = (float) ((Math.PI / 4) * (vFlip ? +1 : -1));
            int y = vFlip ? (oy + 10) : (oy + h - 10);
            img.translate(x, y);
            img.rotate(angle);
            img.drawString(s, 0, 0);
            img.rotate(-angle);
            img.translate(-x, -y);

            ts += slice;
        }

        return true;
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

    public static int indexOf(String[] list, String key) {
        for (int i = 0; i < list.length; i++) {
            if (key.equals(list[i])) {
                return i;
            }
        }
        return -1;
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
     * @param def The default value if the provided string is null or empty
     * @return The parsed integer value
     * @throws NumberFormatException if the number is invalid
     */
    public static int parseInt(String s, int from, int to, int def) {
        return parseInt(s.substring(from, to), def);
    }

    /**
     * Parse a string as integer, stripping it first.
     * @param s The string to parse
     * @param def The default value if the provided string is null or empty
     * @return The parsed integer value
     * @throws NumberFormatException if the number is invalid
     */
    public static int parseInt(String s, int def) {
        if (s == null) return def;
        s = strip(s);
        if (s.length() == 0) return def;
        return Integer.parseInt(s);
    }

    /**
     * Parse a substring as hexadecimal integer, stripping it first.
     * @param s The string to parse
     * @param from The first index of the substring
     * @param to The last+1 index of the substring
     * @param def The default value if the provided string is null or empty
     * @return The parsed integer value
     * @throws NumberFormatException if the number is invalid
     */
    public static int parseHex(String s, int from, int to, int def) {
        return parseHex(s.substring(from, to), def);
    }

    /**
     * Parse a string as hexadecimal integer, stripping it first.
     * @param s The string to parse
     * @param def The default value if the provided string is null or empty
     * @return The parsed integer value
     * @throws NumberFormatException if the number is invalid
     */
    public static int parseHex(String s, int def) {
        if (s == null) return def;
        s = strip(s).toLowerCase();
        if (s.length() == 0) return def;
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

    public static boolean parseBoolean(String line, int from, int to, boolean def) {
        return parseBoolean(line.substring(from, to), def);
    }

    public static boolean parseBoolean(String s, boolean def) {
        if (s == null) return def;
        s = strip(s);
        if (s.length() == 0) return def;
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
        return "pcy_" + ret + ".png";
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
        return "pcy_" + ret + ".png";
    }

    /**
     * Parses a timestamp in the format YYYY-MM-DD HH:MM:SS
     * @param rep The report instance which is currently used (needed for logging)
     * @param buff The string buffer to parse
     * @return a Calendar instance when parsing was successful, null otherwise
     */
    public static Calendar parseTimestamp(Module rep, String buff) {
        Calendar cal = null;
        Pattern p = Pattern.compile("([0-9]{4})-([0-9]{2})-([0-9]{2}) ([0-9]{2}):([0-9]{2}):([0-9]{2})");
        Matcher m = p.matcher(buff);
        if (m.find()) {
            cal = Calendar.getInstance();
            String sYear = m.group(1);
            String sMonth = m.group(2);
            String sDay = m.group(3);
            String sHour = m.group(4);
            String sMin = m.group(5);
            String sSec = m.group(6);
            try {
                cal.set(Calendar.YEAR, Integer.parseInt(sYear));
                cal.set(Calendar.MONTH, Integer.parseInt(sMonth) - 1);
                cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(sDay));
                cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(sHour));
                cal.set(Calendar.MINUTE, Integer.parseInt(sMin));
                cal.set(Calendar.SECOND, Integer.parseInt(sSec));
            } catch (NumberFormatException nfe) {
                rep.printOut(5, "Invalid number: '" + sYear + "' in text '" + buff + "'!");
                cal = null;
            }
        }
        return cal;
    }

    /**
     * Parses a timestamp in the format [+-]NNdNNhNNmNNsNNms
     * @param s The string buffer to parse
     * @return the parsed value in milliseconds
     */
    public static long parseRelativeTimestamp(String s) {
        s = Util.strip(s);
        long ret = 0;
        int idx;

        // In newever android version, there is an extra field, which is wrongly included here
        // For now let's just remove that field here
        idx = s.indexOf(' ');
        if (idx > 0) s = s.substring(idx + 1);

        // skip over the negative and positive signs
        if (s.charAt(0) == '-') {
            s = s.substring(1);
        }
        if (s.charAt(0) == '+') {
            s = s.substring(1);
        }

        // Remove the "ms" from the end... it screws up our parsing
        if (s.endsWith("ms")) {
            s = s.substring(0, s.length() - 2);
        }

        // parse day
        idx = s.indexOf("d");
        if (idx >= 0) {
            int day = Integer.parseInt(s.substring(0, idx));
            s = s.substring(idx + 1);
            ret += day * (24 * 3600000L);
        }
        // parse hours
        idx = s.indexOf("h");
        if (idx >= 0) {
            int hour = Integer.parseInt(s.substring(0, idx));
            s = s.substring(idx + 1);
            ret += hour * 3600000L;
        }

        // parse minutes
        idx = s.indexOf("m");
        if (idx >= 0) {
            int min = Integer.parseInt(s.substring(0, idx));
            s = s.substring(idx + 1);
            ret += min * 60000L;
        }

        // parse seconds
        idx = s.indexOf("s");
        if (idx >= 0) {
            int sec = Integer.parseInt(s.substring(0, idx));
            s = s.substring(idx + 1);
            ret += sec * 1000L;
        }

        // parse millis
        int ms = Integer.parseInt(s);
        ret += ms;

        return ret;
    }

    public static String formatTimeDiff(Calendar ref, Calendar now, boolean ignoreMS) {
        if (ref != null && now != null) {
            long diff = now.getTimeInMillis() - ref.getTimeInMillis();
            StringBuffer sb = new StringBuffer();
            if (diff < 0) {
                sb.append('-');
                diff = -diff;
            } else {
                sb.append('+');
            }
            int msec = (int) (diff % 1000);
            diff /= 1000;
            if (!ignoreMS) {
                sb.insert(1, "ms");
                sb.insert(1, msec);
            }
            if (diff > 0 || ignoreMS) {
                int sec = (int) (diff % 60);
                diff /= 60;
                sb.insert(1, "s");
                sb.insert(1, sec);
            }
            if (diff > 0) {
                int min = (int) (diff % 60);
                diff /= 60;
                sb.insert(1, "m");
                sb.insert(1, min);
            }
            if (diff > 0) {
                int hour = (int) (diff % 24);
                diff /= 24;
                sb.insert(1, "h");
                sb.insert(1, hour);
            }
            if (diff > 0) {
                sb.insert(1, "d");
                sb.insert(1, diff);
            }
            return sb.toString();
        }
        return null;
    }

    public static String extractPkgFromComp(String component) {
        int idx = component.indexOf('/');
        if (idx < 0) {
            return component;
        }
        return component.substring(0, idx);
    }

    public static String extractClsFromComp(String component) {
        int idx = component.indexOf('/');
        if (idx < 0) {
            return "";
        }
        return component.substring(idx + 1);
    }

    public static String formatTS(long duration) {
        StringBuffer sb = new StringBuffer();
        sb.insert(0, "ms");
        sb.insert(0, duration % 1000);
        duration /= 1000;

        if (duration > 0) {
            sb.insert(0, "s");
            sb.insert(0, duration % 60);
            duration /= 60;
        }

        if (duration > 0) {
            sb.insert(0, "m");
            sb.insert(0, duration % 60);
            duration /= 60;
        }

        if (duration > 0) {
            sb.insert(0, "h");
            sb.insert(0, duration % 24);
            duration /= 24;
        }

        if (duration > 0) {
            sb.insert(0, "d");
            sb.insert(0, duration);
        }

        return sb.toString();
    }

    public static String stripHtml(String text) {
        StringBuffer sb = new StringBuffer();
        int len = text.length();
        boolean html = false;
        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            if (html) {
                if (c == '>') {
                    html = false;
                }
            } else if (c == '<') {
                html = true;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String formatLogTS(long ts) {
        int ms = (int) (ts % 1000);
        ts /= 1000;
        int sec = (int) (ts % 60);
        ts /= 60;
        int min = (int) (ts % 60);
        ts /= 60;
        int hour = (int) (ts % 24);
        ts /= 24;
        int day = (int)(ts % 31);
        ts /= 31;
        int month = (int)(ts % 12);
        return String.format("%02d-%02d %02d:%02d:%02d.%03d", month, day, hour, min, sec, ms);
    }

    public static void assertNotNull(Object o) {
        if (o == null) {
            throw new AssertionError();
        }
    }

    public static void assertTrue(boolean b) {
        if (!b) {
            throw new AssertionError();
        }
    }

    public static void printResource(Renderer r, String resName) {
        try {
            InputStream is = Util.class.getResourceAsStream(PlatformUtil.ASSETS_ROOT + resName);
            BufferedReader br = new BufferedReader(is);
            String line;
            while (null != (line = br.readLine())) {
                r.println(line);
            }
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
