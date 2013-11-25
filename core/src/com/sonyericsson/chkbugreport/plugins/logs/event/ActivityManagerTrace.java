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
package com.sonyericsson.chkbugreport.plugins.logs.event;

import com.sonyericsson.chkbugreport.BugReportModule;
import com.sonyericsson.chkbugreport.ProcessRecord;
import com.sonyericsson.chkbugreport.plugins.logs.LogLine;

import java.util.Vector;

/**
 * This class is used to collect tracing data (logs) related to ActivityManager.
 */
public class ActivityManagerTrace {

    public static final String INFO_ID = "eventlog_am_trace";

    /* Activity manager events */
    private Vector<AMData> mAMDatas = new Vector<AMData>();
    /* The next fake pid which can be allocated */
    private int mNextFakePid = 100000;

    public ActivityManagerTrace(EventLogPlugin eventLogPlugin) {
        // NOP
    }

    public void addAMData(String eventType, BugReportModule br, LogLine sl, int i) {
        try {
            addAMDataUnsafe(eventType, br, sl, i);
        } catch (Exception e) {
            br.printErr(4, "Error parsing AM data from event log at line " + (i + 1) + " (" + e + ")");
        }
    }

    private void addAMDataUnsafe(String eventType, BugReportModule br, LogLine sl, int i) {
        int d = (br.getAndroidVersionSdk() >= 17) ? 1 : 0; // User id field inserted as first
        if ("am_create_activity".equals(eventType)) {
            addAMData(new AMData(AMData.ACTIVITY, AMData.ON_CREATE, -1, sl.getFields(d+2), sl.ts));
        } else if ("am_restart_activity".equals(eventType)) {
            addAMData(new AMData(AMData.ACTIVITY, AMData.ON_RESTART, -1, sl.getFields(d+2), sl.ts));
        } else if ("am_destroy_activity".equals(eventType)) {
            addAMData(new AMData(AMData.ACTIVITY, AMData.ON_DESTROY, -1, sl.getFields(d+2), sl.ts));
        } else if ("am_pause_activity".equals(eventType)) {
            addAMData(new AMData(AMData.ACTIVITY, AMData.ON_PAUSE, -1, sl.getFields(d+1), sl.ts));
        } else if ("am_resume_activity".equals(eventType)) {
            addAMData(new AMData(AMData.ACTIVITY, AMData.ON_RESUME, -1, sl.getFields(d+2), sl.ts));
        } else if ("am_proc_bound".equals(eventType)) {
            suggestName(br, sl, d+0, d+1, 20);
        } else if ("am_create_service".equals(eventType)) {
            int pid = Integer.parseInt(sl.fields[3]); // NOTE: due to other changes, the delta is not needed here
            addAMData(new AMData(AMData.SERVICE, AMData.ON_CREATE, pid, sl.getFields(d+1), sl.ts));
            suggestName(br, sl, 3, d+1, 18); // NOTE: due to other changes, the delta is not needed here
        } else if ("am_destroy_service".equals(eventType)) {
            int pid = Integer.parseInt(sl.fields[d+2]);
            addAMData(new AMData(AMData.SERVICE, AMData.ON_DESTROY, pid, sl.getFields(d+1), sl.ts));
            suggestName(br, sl, d+2, d+1, 18);
        } else if ("am_schedule_service_restart".equals(eventType)) {
            addAMData(new AMData(AMData.SERVICE, AMData.SCHEDULE_SERVICE_RESTART, 0, sl.getFields(d+0), sl.ts));
        } else if ("am_kill".equals(eventType)) {
            int pid = Integer.parseInt(sl.fields[d+0]);
            AMData data = new AMData(AMData.PROC, AMData.PROC_KILL, pid, sl.getFields(d+1), sl.ts);
            data.setExtra(sl.fields[d+3]); // reason for kill
            addAMData(data);
        } else if ("am_proc_died".equals(eventType)) {
            int pid = Integer.parseInt(sl.fields[d+0]);
            addAMData(new AMData(AMData.PROC, AMData.PROC_DIED, pid, sl.getFields(d+1), sl.ts));
            suggestName(br, sl, d+0, d+1, 20);
        } else if ("am_proc_start".equals(eventType)) {
            int pid = Integer.parseInt(sl.fields[d+0]);
            addAMData(new AMData(AMData.PROC, AMData.PROC_START, pid, sl.getFields(d+2), sl.ts));
            suggestName(br, sl, d+0, d+2, 20);
        } else {
            // ignore
        }
    }

    private void suggestName(BugReportModule br, LogLine sl, int idxPid, int idxPkg, int prio) {
        if (Math.max(idxPid, idxPkg) >= sl.fields.length) return; // not enough fields
        int pid = -1;
        try {
            pid = Integer.parseInt(sl.fields[idxPid]);
        } catch (Exception e) {
            return; // strange pid
        }
        suggestNameImpl(br, sl, pid, idxPkg, prio);
    }

    private void suggestNameImpl(BugReportModule br, LogLine sl, int pid, int idxPkg, int prio) {
        if (idxPkg >= sl.fields.length) return; // not enough fields
        String procName = sl.fields[idxPkg];
        if (procName.length() == 0) {
            return; // missing package name
        }
        int idx = procName.indexOf('/');
        if (idx > 0) {
            procName = procName.substring(0, idx);
        }
        ProcessRecord pr = br.getProcessRecord(pid, true, false);
        if (pr != null) {
            pr.suggestName(procName, prio);
        }
    }

    private void addAMData(AMData amData) {
        mAMDatas.add(amData);
    }

    public int size() {
        return mAMDatas.size();
    }

    public AMData get(int idx) {
        return mAMDatas.get(idx);
    }

    /**
     * Try to guess the component of a given pid
     * @param i The index where to start searching
     * @param pid The component
     * @return The found component, or null if not found
     */
    private String findComponent(int i, int pid) {
        String component = null;

        // Search backwards only (since this method is used only for proc_died)
        if (component == null) {
            for (int j = i - 1; j >= 0; j--) {
                AMData am = mAMDatas.get(j);
                if (pid == am.getPid()) {
                    component = am.getComponent();
                    if (component != null) {
                        break;
                    }
                }
            }
        }

        return component;
    }

    /**
     * Try to guess the pid of a given component
     * @param i The index where to start searching
     * @param component The component
     * @return The found pid, or -1 if not found
     */
    private int findPid(int i, String component) {
        int pid = -1;
        int cnt = mAMDatas.size();

        // Search backwards
        if (pid == -1) {
            for (int j = i - 1; j >= 0; j--) {
                AMData am = mAMDatas.get(j);
                if (component.equals(am.getComponent())) {
                    pid = am.getPid();
                    if (pid >= 0) {
                        break;
                    }
                }
            }
        }

        // Search forward
        if (pid == -1) {
            for (int j = i + 1; j < cnt; j++) {
                AMData am = mAMDatas.get(j);
                if (component.equals(am.getComponent())) {
                    pid = am.getPid();
                    if (pid >= 0) {
                        break;
                    }
                }
            }
        }

        // TODO: maybe we should verify it: there shouldn't be any am_proc_died or similar logs
        // between the found index and the current index

        // If not found, allocate a dummy/fake pid
        if (pid == -1) {
            pid = allocFakePid();
        }

        return pid;
    }

    private int allocFakePid() {
        return mNextFakePid++;
    }

    public void finishLoad() {
        int cnt = mAMDatas.size();

        // First, we must make sure that all data has a pid associated to it
        for (int i = 0; i < cnt; i++) {
            AMData am = mAMDatas.get(i);
            String component = am.getComponent();
            if (am.getPid() >= 0) continue;
            if (component == null) continue;
            int pid = findPid(i, component);
            if (pid >= 0) {
                am.setPid(pid);
            }
        }

        // Also, we must make sure that all data has a component associated to it
        for (int i = 0; i < cnt; i++) {
            AMData am = mAMDatas.get(i);
            String component = am.getComponent();
            int pid = am.getPid();
            if (pid < 0) continue;
            if (component != null) continue;
            component = findComponent(i, pid);
            if (component != null) {
                am.setComponent(component);
            }
        }

    }

}
