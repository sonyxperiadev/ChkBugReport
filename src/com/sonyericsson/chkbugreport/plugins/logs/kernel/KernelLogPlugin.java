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

package com.sonyericsson.chkbugreport.plugins.logs.kernel;

import com.sonyericsson.chkbugreport.BugReportModule;
import com.sonyericsson.chkbugreport.Plugin;
import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.Section;

import java.util.Vector;

public class KernelLogPlugin extends Plugin {
    public static final String TAG = "[KernelLogPlugin]";

    private Vector<LogData> mLogs = new Vector<LogData>();

    @Override
    public int getPrio() {
        return 31;
    }

    @Override
    public void reset() {
        mLogs.clear();
    }

    @Override
    public void load(Module rep) {
        BugReportModule br = (BugReportModule)rep;

        loadLog(br, Section.KERNEL_LOG, "Kernel log", "kernellog");
        loadLog(br, Section.KERNEL_LOG_FROM_SYSTEM, "Kernel log from system", "kernellog_fs");
        loadLog(br, Section.LAST_KMSG, "Last kmsg", "lastkmsg");

    }

    private void loadLog(BugReportModule br, String sectionName, String chapterName, String id) {
        Section section = br.findSection(sectionName);
        if (section == null) {
            br.printErr(3, TAG + "Cannot find section " + sectionName + " (ignoring)");
            return;
        }
        LogData data = new LogData(br, section, chapterName, id);
        if (data.isLoaded()) {
            mLogs.add(data);
        }
    }

    /**
     * Generate the HTML document for the kernel log section.
     */
    @Override
    public void generate(Module rep) {
        BugReportModule br = (BugReportModule)rep;
        for (LogData log : mLogs) {
            log.generate(br);
        }
    }

}
