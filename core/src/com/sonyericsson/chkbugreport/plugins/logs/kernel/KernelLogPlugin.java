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
import com.sonyericsson.chkbugreport.plugins.logs.webapp.LogWebApp;
import com.sonyericsson.chkbugreport.webserver.ChkBugReportWebServer;

import java.util.Vector;

public class KernelLogPlugin extends Plugin {

    public static final String TAG = "[KernelLogPlugin]";

    public static final String INFO_ID_KERNEL_LOG = "kernellog";
    public static final String INFO_ID_KERNEL_LOG_FROM_SYSTEM = "kernellog_fs";
    public static final String INFO_ID_LAST_KMSG = "kernellog_lk";

    private Vector<KernelLogData> mLogs = new Vector<KernelLogData>();

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

        loadLog(br, Section.KERNEL_LOG, "Kernel log", "kernellog", INFO_ID_KERNEL_LOG);
        loadLog(br, Section.LAST_KMSG, "Last kmsg", "lastkmsg", INFO_ID_LAST_KMSG);

        // Extract the log also from the system log
        // This needs some special attention, since we want to keep the system log timestamps as well
        KernelLogData data = new LogDataFromSL(br, "Kernel log from system", "kernellog_fs", INFO_ID_KERNEL_LOG_FROM_SYSTEM);
        if (data.finishLoad()) {
            mLogs.add(data);
        }
    }

    private void loadLog(BugReportModule br, String sectionName, String chapterName, String id, String infoId) {
        Section section = br.findSection(sectionName);
        if (section == null) {
            br.printErr(3, TAG + "Cannot find section " + sectionName + " (ignoring)");
            return;
        }
        KernelLogData data = new KernelLogData(br, section, chapterName, id, infoId);
        if (data.finishLoad()) {
            mLogs.add(data);
        }
    }

    @Override
    public void setWebServer(ChkBugReportWebServer ws) {
        for (KernelLogData ld : mLogs) {
            ws.addModule(ld.getInfoId(), new LogWebApp(ld, ws));
        }
    }

    /**
     * Generate the HTML document for the kernel log section.
     */
    @Override
    public void generate(Module rep) {
        BugReportModule br = (BugReportModule)rep;
        for (KernelLogData log : mLogs) {
            log.generate(br);
        }
    }

}
