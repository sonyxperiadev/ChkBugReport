package com.sonyericsson.chkbugreport.plugins.logs.event;

import com.sonyericsson.chkbugreport.BugReport;
import com.sonyericsson.chkbugreport.ProcessRecord;
import com.sonyericsson.chkbugreport.plugins.logs.LogLine;

import java.util.Vector;

/**
 * This class is used to collect tracing data (logs) related to ActivityManager.
 */
public class ActivityManagerTrace {

    /* Activity manager events */
    private Vector<AMData> mAMDatas = new Vector<AMData>();
    /* The next fake pid which can be allocated */
    private int mNextFakePid = 100000;

    public ActivityManagerTrace(EventLogPlugin eventLogPlugin, BugReport br) {
        // NOP
    }

    public void addAMData(String eventType, BugReport br, LogLine sl, int i) {
        try {
            addAMDataUnsafe(eventType, br, sl, i);
        } catch (Exception e) {
            br.printErr(4, "Error parsing AM data from event log at line " + (i + 1));
        }
    }

    private void addAMDataUnsafe(String eventType, BugReport br, LogLine sl, int i) {
        if ("am_create_activity".equals(eventType)) {
            addAMData(new AMData(AMData.ON_CREATE, -1, sl.getFields(2), sl.ts));
        } else if ("am_restart_activity".equals(eventType)) {
            addAMData(new AMData(AMData.ON_RESTART, -1, sl.getFields(2), sl.ts));
        } else if ("am_destroy_activity".equals(eventType)) {
            addAMData(new AMData(AMData.ON_DESTROY, -1, sl.getFields(2), sl.ts));
        } else if ("am_pause_activity".equals(eventType)) {
            addAMData(new AMData(AMData.ON_PAUSE, -1, sl.getFields(1), sl.ts));
        } else if ("am_resume_activity".equals(eventType)) {
            addAMData(new AMData(AMData.ON_RESUME, -1, sl.getFields(2), sl.ts));
        } else if ("am_proc_start".equals(eventType)) {
            int pid = Integer.parseInt(sl.fields[0]);
            addAMData(new AMData(AMData.BORN, pid, sl.getFields(4), sl.ts));
            suggestName(br, sl, 0, 2, 20);
        } else if ("am_proc_bound".equals(eventType)) {
            suggestName(br, sl, 0, 1, 20);
        } else if ("am_proc_died".equals(eventType)) {
            int pid = Integer.parseInt(sl.fields[0]);
            addAMData(new AMData(AMData.DIE, pid, null, sl.ts));
            suggestName(br, sl, 0, 1, 20);
        } else if ("am_create_service".equals(eventType)) {
            int pid = Integer.parseInt(sl.fields[3]);
            addAMData(new AMData(AMData.ON_CREATE, pid, sl.getFields(1), sl.ts));
            suggestName(br, sl, 3, 1, 18);
        } else if ("am_destroy_service".equals(eventType)) {
            int pid = Integer.parseInt(sl.fields[2]);
            addAMData(new AMData(AMData.ON_DESTROY, pid, sl.getFields(1), sl.ts));
            suggestName(br, sl, 2, 1, 18);
        } else {
            // ignore
        }
    }

    private void suggestName(BugReport br, LogLine sl, int idxPid, int idxPkg, int prio) {
        if (Math.max(idxPid, idxPkg) >= sl.fields.length) return; // not enough fields
        int pid = -1;
        try {
            pid = Integer.parseInt(sl.fields[idxPid]);
        } catch (Exception e) {
            return; // strange pid
        }
        suggestNameImpl(br, sl, pid, idxPkg, prio);
    }

    private void suggestNameImpl(BugReport br, LogLine sl, int pid, int idxPkg, int prio) {
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
    public String findComponent(int i, int pid) {
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
    public int findPid(int i, String component) {
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

}
