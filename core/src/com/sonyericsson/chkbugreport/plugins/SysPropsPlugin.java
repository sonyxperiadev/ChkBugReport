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
package com.sonyericsson.chkbugreport.plugins;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sonyericsson.chkbugreport.BugReportModule;
import com.sonyericsson.chkbugreport.Plugin;
import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.Section;

public class SysPropsPlugin extends Plugin {

    private static final String TAG = "[SysPropsPlugin]";

    private static final Pattern UPTIME_PATTERN = Pattern.compile(".*up\\s+(.*),\\s+(.*) users,\\s+load average: (.*), (.*), (.*)");
    private static final Pattern TIME_PATTERN_1 = Pattern.compile("(.*) days, (.*):(.*):(.*)");
    private static final Pattern TIME_PATTERN_2 = Pattern.compile("(.*):(.*):(.*)");
    private static final Pattern TIME_PATTERN_3 = Pattern.compile("(.*):(.*)");
    private static final Pattern TIME_TEXT = Pattern.compile("(?:(?:\\d+) (?:days|hours|min),?\\s?)+");
    private static final Pattern TIME_PATTERN_MIN = Pattern.compile(".*?(\\d+) min.*");
    private static final Pattern TIME_PATTERN_HOUR = Pattern.compile(".*?(\\d+) hour.*");
    private static final Pattern TIME_PATTERN_DAY = Pattern.compile(".*?(\\d+) days.*");
    private static final Pattern TIME_DAY_AND_TIME = Pattern.compile("(?:(\\d+) (?:day(?:s)?),?\\s?) (\\d+):(\\d+)");

    private HashMap<String, String> mMap = new HashMap<String, String>();

    private long mUpTime;
    private long mUsers;
    private float mOneMinLoad;
    private float mFiveMinLoad;
    private float mFifteenMinLoad;

    @Override
    public int getPrio() {
        return 1; // Need to execute first
    }

    public long getUpTime() {
        return mUpTime;
    }

    public long getUsers() {
        return mUsers;
    }

    public float getOneMinLoad() {
        return mOneMinLoad;
    }

    public float getFiveMinLoad() {
        return mFiveMinLoad;
    }


    public float getFifteenMinLoad() {
        return mFifteenMinLoad;
    }


    @Override
    public void reset() {
        // NOP
    }

    @Override
    public void load(Module rep) {
        BugReportModule br = (BugReportModule)rep;

        loadSystemProperties(br);
        loadUptime(br);
    }

    private void loadUptime(BugReportModule br) {
        mUpTime = 0;
        mUsers = 0;
        mOneMinLoad = 0;
        mFiveMinLoad = 0;
        mFifteenMinLoad = 0;
        Section sec = br.findSection(Section.UPTIME);
        if (sec == null) {
            br.printErr(3, TAG + "Cannot find section: " + Section.UPTIME);
            return;
        }

        String line = sec.getLine(0);
        Matcher m = UPTIME_PATTERN.matcher(line);
        if (!m.matches()) {
            br.printErr(4, TAG + "Cannot parse uptime: " + line);
            return;
        }
        mUpTime = parseTime(br, m.group(1));
        mUsers = Long.parseLong(m.group(2));
        mOneMinLoad = Float.parseFloat(m.group(3));
        mFiveMinLoad =  Float.parseFloat(m.group(4));
        mFifteenMinLoad =  Float.parseFloat(m.group(5));

        br.setUptime(mUpTime, 100);
    }

    private long parseTime(BugReportModule br, String str) {
        Matcher m = TIME_TEXT.matcher(str);
        if(m.matches()) {
            Matcher daysMatcher = TIME_PATTERN_DAY.matcher(str);
            Matcher hoursMatcher = TIME_PATTERN_HOUR.matcher(str);
            Matcher minutesMatcher = TIME_PATTERN_MIN.matcher(str);

            long days = daysMatcher.matches() ? Long.parseLong(daysMatcher.group(1)):  0;
            long hours = hoursMatcher.matches() ? Long.parseLong(hoursMatcher.group(1)):  0;
            long minutes = minutesMatcher.matches() ? Long.parseLong(minutesMatcher.group(1)):  0;
            return ((days * 24 + hours) * 60 + minutes) * 60;

        }
        m = TIME_DAY_AND_TIME.matcher(str);
        if(m.matches()) {
            long days = Long.parseLong(m.group(1));
            long hours = Long.parseLong(m.group(2));
            long minutes = Long.parseLong(m.group(3));
            return ((days * 24 + hours) * 60 + minutes) * 60;

        }
        m = TIME_PATTERN_1.matcher(str);
        if (m.matches()) {
            long days = Long.parseLong(m.group(1));
            long hours = Long.parseLong(m.group(2));
            long minutes = Long.parseLong(m.group(3));
            long seconds = Long.parseLong(m.group(4));
            return ((days * 24 + hours) * 60 + minutes) * 60 + seconds;
        }
        m = TIME_PATTERN_2.matcher(str);
        if (m.matches()) {
            long hours = Long.parseLong(m.group(1));
            long minutes = Long.parseLong(m.group(2));
            long seconds = Long.parseLong(m.group(3));
            return (hours * 60 + minutes) * 60 + seconds;
        }
        m = TIME_PATTERN_3.matcher(str);
        if (m.matches()) {
            long minutes = Long.parseLong(m.group(1));
            long seconds = Long.parseLong(m.group(2));
            return (minutes) * 60 + seconds;
        }
        br.printErr(4, TAG + "Cannot parse time string: " + str);
        return 0;
    }

    private void loadSystemProperties(BugReportModule br) {
        // reset
        mMap.clear();
        Section sec = br.findSection(Section.SYSTEM_PROPERTIES);
        if (sec == null) {
            br.printErr(3, TAG + "Cannot find section: " + Section.SYSTEM_PROPERTIES);
            return;
        }
        int cnt = sec.getLineCount();
        for (int i = 0; i < cnt; i++) {
            String line = sec.getLine(i);
            int len = line.length();
            int idx = line.indexOf(':');
            if (idx < 0) continue;
            if (line.charAt(0) != '[') continue;
            if (line.charAt(idx-1) != ']') continue;
            if (line.charAt(idx+2) != '[') continue;
            if (line.charAt(len-1) != ']') continue;
            String key = line.substring(1, idx - 1);
            String value = line.substring(idx + 3, len - 1);
            mMap.put(key, value);
        }

        // Now let the others now what is the current android version
        br.setAndroidVersion(mMap.get("ro.build.version.release"));
        br.setAndroidSdkVersion(mMap.get("ro.build.version.sdk"));
    }

    @Override
    public void generate(Module br) {
        // NOP
    }

    public String getProp(String key) {
        return mMap.get(key);
    }

}
