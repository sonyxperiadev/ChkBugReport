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

import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.util.Util;

public class ComponentStat {

    public String component;
    public String pkg;
    public String cls;

    public int createCount;
    public long totalCreatedTime;
    public long maxCreatedTime;
    public int resumeCount;
    public long totalResumedTime;
    public long maxResumedTime;
    public int errors;

    private Module _br;
    private long _createTime;
    private long _resumeTime;
    private long _firstTs;
    private long _lastTs;
    private boolean _createGuessed;
    private boolean _resumeGuessed;
    private boolean _restart;

    private boolean _debug;

    public ComponentStat(Module br, String component, long firstTs, long lastTs) {
        this.component = component;
        this.pkg = Util.extractPkgFromComp(component);
        this.cls = Util.extractClsFromComp(component);
        _br = br;
        _firstTs = firstTs;
        _lastTs = lastTs;
    }

    public void addData(AMData am) {
        switch (am.getAction()) {
            case AMData.ON_CREATE:
                _restart = false;
                onCreate(am.getTS());
                break;
            case AMData.SCHEDULE_SERVICE_RESTART:
                _restart = true;
            case AMData.ON_DESTROY:
                onDestroy(am.getTS());
                break;
            case AMData.ON_RESUME:
                onResume(am.getTS());
                break;
            case AMData.ON_PAUSE:
                onPause(am.getTS());
                break;
        }
    }

    private void checkOnCreate(long ts) {
        // Guess missing create time if needed
        if (_createTime == 0) {
            if (_createGuessed) {
                _br.printErr(5, "ComponentStat: onCreate was already guessed, but it's missing again (" + component + ")");
                onCreate(ts);
                errors++;
            } else {
                _br.printErr(5, "ComponentStat: onCreate was missing (" + component + ")");
                onCreate(_firstTs);
            }
            _createGuessed = true;
        }
    }

    private void onResume(long ts) {
        // make sure onCreate is registered
        checkOnCreate(ts);

        if (_resumeTime == 0) {
            _resumeTime = ts;
        } else {
            _br.printErr(5, "ComponentStat: onResume was called again without onPause (" + component + ")");
            errors++;
        }
    }

    private void onPause(long ts) {
        // Guess missing resume time if needed
        if (_resumeTime == 0) {
            if (_resumeGuessed) {
                _br.printErr(5, "ComponentStat: onResume was already guessed, but it's missing again (" + component + ")");
                onResume(ts);
                errors++;
            } else {
                _br.printErr(5, "ComponentStat: onResume was missing (" + component + ")");
                onResume(_firstTs);
            }
            _resumeGuessed = true;
        }

        if (_resumeTime != 0) {
            resumeCount++;
            long time = (ts - _resumeTime);
            if (time < 0) {
                _br.printErr(5, "ComponentStat: negative resumed time found, there is some problem with the timestamps!");
                errors++;
            }
            totalResumedTime += time;
            if (_debug) {
                System.out.println("+[R] " + component + " " + _resumeTime + "->" + ts + "=" + time + " sum=" + totalResumedTime);
            }
            maxResumedTime = Math.max(maxResumedTime, time);
            _resumeTime = 0;
        }
    }

    private void onCreate(long ts) {
        if (_createTime == 0) {
            _createTime = ts;
        } else {
            _br.printErr(5, "ComponentStat: onCreate was called again without onDestroy (" + component + ")");
            errors++;
        }
    }

    private void onDestroy(long ts) {
        if (!_restart) {
            checkOnCreate(ts);
        }

        if (_createTime != 0) {
            createCount++;
            long time = (ts - _createTime);
            if (time < 0) {
                _br.printErr(5, "ComponentStat: negative created time found, there is some problem with the timestamps!");
                errors++;
            }
            totalCreatedTime += time;
            if (_debug) {
                System.out.println("+[C] " + component + " " + _createTime + "->" + ts + "=" + time + " sum=" + totalCreatedTime);
            }
            maxCreatedTime = Math.max(maxCreatedTime, time);
            _createTime = 0;
        }
    }

    public void finish() {
        if (_resumeTime != 0) {
            onPause(_lastTs);
        }
        if (_createTime != 0) {
            onDestroy(_lastTs);
        }
    }

}
