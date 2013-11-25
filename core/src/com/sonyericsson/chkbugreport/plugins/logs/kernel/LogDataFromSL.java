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
import com.sonyericsson.chkbugreport.Section;
import com.sonyericsson.chkbugreport.plugins.logs.LogLine;
import com.sonyericsson.chkbugreport.plugins.logs.LogLines;
import com.sonyericsson.chkbugreport.plugins.logs.SystemLogPlugin;

/* package */ class LogDataFromSL extends KernelLogData {

    public LogDataFromSL(BugReportModule br, String chapterName, String id, String infoId) {
        super(br, null, chapterName, id, infoId);

        LogLines sl = (LogLines) br.getInfo(SystemLogPlugin.INFO_ID_SYSTEMLOG);
        Section mKernelLog = null;
        if (sl != null) {
            int cnt = sl.size();
            for (int i = 0; i < cnt; i++) {
                LogLine l = sl.get(i);
                if (l.tag.equals("kernel")) {
                    if (mKernelLog == null) {
                        mKernelLog = new Section(br, Section.KERNEL_LOG_FROM_SYSTEM);
                        br.addSection(mKernelLog);
                    }
                    String line = convertToKrnLogLevel(l.level) + l.msg;
                    mKernelLog.addLine(line);
                    addLine(br, line, l.ts);
                }
            }
        }
    }

    private String convertToKrnLogLevel(char level) {
        switch (level) {
            case 'V': return "<7>";
            case 'D': return "<6>";
            case 'I': return "<5>";
            case 'W': return "<4>";
            case 'E': return "<3>";
            case 'F': return "<0>";
        }
        return "<?>";
    }

}
