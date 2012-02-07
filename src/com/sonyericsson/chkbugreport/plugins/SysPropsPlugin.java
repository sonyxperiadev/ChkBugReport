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
package com.sonyericsson.chkbugreport.plugins;

import java.util.HashMap;

import com.sonyericsson.chkbugreport.BugReport;
import com.sonyericsson.chkbugreport.Plugin;
import com.sonyericsson.chkbugreport.Report;
import com.sonyericsson.chkbugreport.Section;

public class SysPropsPlugin extends Plugin {

    private static final String TAG = "[SysPropsPlugin]";

    private HashMap<String, String> mMap = new HashMap<String, String>();

    @Override
    public int getPrio() {
        return 1; // Need to execute first
    }

    @Override
    public void load(Report rep) {
        BugReport br = (BugReport)rep;

        // reset
        mMap.clear();
        Section sec = rep.findSection(Section.SYSTEM_PROPERTIES);
        if (sec == null) {
            rep.printErr(3, TAG + "Cannot find section: " + Section.SYSTEM_PROPERTIES);
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
    public void generate(Report br) {
        // NOP
    }

    public String getProp(String key) {
        return mMap.get(key);
    }

}
