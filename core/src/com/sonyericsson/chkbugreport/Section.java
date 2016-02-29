/*
 * Copyright (C) 2011 Sony Ericsson Mobile Communications AB
 * Copyright (C) 2012 Sony Mobile Communications AB
 * Copyright (C) 2016 Google Inc.
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

import java.io.InputStream;

public class Section extends Lines {

    public static final String APP_ACTIVITIES = "APP ACTIVITIES";
    public static final String APP_SERVICES = "APP SERVICES";
    public static final String BINDER_STATE = "BINDER STATE";
    public static final String CPU_INFO = "CPU INFO";
    public static final String DUMP_OF_SERVICE_ALARM = "DUMP OF SERVICE alarm";
    public static final String DUMP_OF_SERVICE_BATTERYINFO = "DUMP OF SERVICE batteryinfo";
    public static final String DUMP_OF_SERVICE_BATTERYSTATS = "DUMP OF SERVICE batterystats";
    public static final String DUMP_OF_SERVICE_MEMINFO = "DUMP OF SERVICE meminfo";
    public static final String DUMP_OF_SERVICE_PACKAGE = "DUMP OF SERVICE package";
    public static final String DUMP_OF_SERVICE_SURFACEFLINGER = "DUMP OF SERVICE SurfaceFlinger";
    public static final String DUMP_OF_SERVICE_WINDOW = "DUMP OF SERVICE window";
    public static final String DUMPSYS = "DUMPSYS";
    public static final String EVENT_LOG = "EVENT LOG";
    public static final String FILESYSTEMS_AND_FREE_SPACE = "FILESYSTEMS & FREE SPACE";
    public static final String FTRACE = "FTRACE";
    public static final String KERNEL_CPUFREQ = "KERNEL CPUFREQ";
    public static final String KERNEL_LOG = "KERNEL LOG";
    public static final String KERNEL_LOG_FROM_SYSTEM = "KERNEL LOG FROM SYSTEM";
    public static final String KERNEL_WAKELOCKS = "KERNEL WAKELOCKS";
    public static final String KERNEL_WAKE_SOURCES = "KERNEL WAKE SOURCES";
    public static final String LAST_KMSG = "LAST KMSG";
    public static final String LIBRANK = "LIBRANK";
    // Note, this does not actually exists... but sometimes we get this buffer separately
    public static final String MAIN_LOG = "MAIN LOG";
    public static final String MEMORY_INFO = "MEMORY INFO";
    public static final String PACKAGE_SETTINGS = "PACKAGE SETTINGS";
    // This doesn't exists either, it's used when partial bugreport is parsed
    public static final String PARTIAL_FILE_HEADER = "PARTIAL FILE HEADER";
    public static final String PROCESSES = "PROCESSES";
    public static final String PROCESSES_AND_THREADS = "PROCESSES AND THREADS";
    public static final String PROCESSES_IN_CAMS = "Processes in Current Activity Manager State";
    public static final String PROCRANK = "PROCRANK";
    public static final String SYSTEM_LOG = "SYSTEM LOG";
    public static final String SYSTEM_PROPERTIES = "SYSTEM PROPERTIES";
    public static final String UPTIME = "UPTIME";
    // Note, this does not actually exists... but it can be pulled from /data/system/usagestats/usage-history.xml
    public static final String USAGE_HISTORY = "USAGE HISTORY";
    public static final String VM_TRACES_AT_LAST_ANR = "VM TRACES AT LAST ANR";
    public static final String VM_TRACES_JUST_NOW = "VM TRACES JUST NOW";
    public static final String WINDOW_MANAGER_POLICY_STATE = "WINDOW MANAGER POLICY STATE";
    public static final String WINDOW_MANAGER_SESSIONS = "WINDOW MANAGER SESSIONS";
    public static final String WINDOW_MANAGER_TOKENS = "WINDOW MANAGER TOKENS";
    public static final String WINDOW_MANAGER_WINDOWS = "WINDOW MANAGER WINDOWS";
    // These are metadata sections, they probably don't contain text but binary blobs
    public static final String SCREEN_SHOT = "META: SCREEN SHOT";
    // These are special sections, they don't store data, but trigger other processing/scanning
    public static final String META_SCAN_DIR = "META: SCAN DIR";
    public static final String META_PARSE_MONKEY = "META: PARSE MONKEY";

    private int mId;
    private String mFileName;
    private String mShortName;

    public Section(Module module, String sectionName) {
        super(sectionName);

        // Clean up the name to be able to use as file name
        int p = sectionName.indexOf('(');
        if (p >= 0) {
            sectionName = sectionName.substring(0, p - 1);
        }
        p = sectionName.indexOf(':');
        if (p >= 0) {
            sectionName = sectionName.substring(0, p);
        }
        mShortName = sectionName;
        // Some filesystems don't allow space in filename
        sectionName = sectionName.replace(' ', '_');
        // Unix filesystems don't allow '/' in filename
        sectionName = sectionName.replace('/', '-');
        mId = module.allocSectionId();
        mFileName = String.format("%03d-%s", mId, sectionName);
    }

    public String getFileName() {
        return mFileName;
    }

    public String getShortName() {
        return mShortName;
    }

    public InputStream createInputStream() {
        return new SectionInputStream(this);
    }

    public static boolean isSection(String type) {
        if (type.startsWith("!")) return false;
        if (type.startsWith("META:")) return false;
        return true;
    }

}
