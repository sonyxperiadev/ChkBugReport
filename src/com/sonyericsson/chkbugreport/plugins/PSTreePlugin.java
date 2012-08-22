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

import com.sonyericsson.chkbugreport.BugReport;
import com.sonyericsson.chkbugreport.Chapter;
import com.sonyericsson.chkbugreport.PSRecord;
import com.sonyericsson.chkbugreport.Plugin;
import com.sonyericsson.chkbugreport.ProcessRecord;
import com.sonyericsson.chkbugreport.Report;

public class PSTreePlugin extends Plugin {

    private static final String TAG = "[PSTreePlugin]";

    @Override
    public int getPrio() {
        return 98;
    }

    @Override
    public void load(Report br) {
        // NOP
    }


    @Override
    public void generate(Report rep) {
        BugReport br = (BugReport) rep;

        PSRecord ps = br.getPSTree();
        if (ps == null) {
            br.printErr(3, TAG + "Process list information not found! (aborting plugin)");
            return;
        }

        Chapter ch = new Chapter(br, "Process tree");
        br.addChapter(ch);

        ch.addLine("<table class=\"treeTable pstree\">");
        ch.addLine("<thead>");
        ch.addLine("<tr>");
        ch.addLine("<td>PID</td>");
        ch.addLine("<td>PPID</td>");
        ch.addLine("<td>Name</td>");
        ch.addLine("<td>Nice</td>");
        ch.addLine("<td>Policy</td>");
        ch.addLine("</tr>");
        ch.addLine("</thead>");
        ch.addLine("<tbody>");
        genPSTree(br, ps, ch);
        ch.addLine("</tbody>");
        ch.addLine("</table>");
    }

    private void genPSTree(BugReport br, PSRecord ps, Chapter ch) {
        int pid = ps.getPid();
        if (pid != 0) {
            String childOf = "";
            int ppid = ps.getParentPid();
            if (ppid != 0) {
                childOf = "class=\"child-of-pstree-" + ppid + "\"";
            }
            ProcessRecord pr = br.getProcessRecord(pid, false, false);
            ProcessRecord prp = br.getProcessRecord(ppid, false, false);
            if (pr != null && !pr.isExported()) { pr = null; }
            if (prp != null && !prp.isExported()) { prp = null; }
            String linkToPidB = (pr == null) ? "" : "<a href=\"" + br.createLinkToProcessRecord(pid) + "\">";
            String linkToPidE = (pr == null) ? "" : "</a>";
            String linkToPPidB = (prp == null) ? "" : "<a href=\"" + br.createLinkToProcessRecord(ppid) + "\">";
            String linkToPPidE = (prp == null) ? "" : "</a>";
            ch.addLine("<tr id=\"pstree-" + pid + "\" " + childOf + ">");
            ch.addLine("<td>" + linkToPidB + pid + linkToPidE + "</td>");
            ch.addLine("<td>" + linkToPPidB + ppid + linkToPPidE + "</td>");
            ch.addLine("<td>" + linkToPidB + ps.getName() + linkToPidE + "</td>");
            ch.addLine("<td>" + ps.getNice() + "</td>");
            ch.addLine("<td>" + ps.getPolicyStr() + "</td>");
            ch.addLine("</tr>");
        }

        for (PSRecord child : ps) {
            genPSTree(br, child, ch);
        }
    }

}
