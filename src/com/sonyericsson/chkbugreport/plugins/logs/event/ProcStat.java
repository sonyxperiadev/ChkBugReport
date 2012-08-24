package com.sonyericsson.chkbugreport.plugins.logs.event;

import com.sonyericsson.chkbugreport.Report;

public class ProcStat {

    public String proc;
    public int count;
    public long totalTime;
    public long maxTime;
    public int restartCount;
    public long minRestartTime;
    public long totalRestartTime;
    public int bgKillRestartCount;
    public long minBgKillRestartTime;
    public long totalBgKillRestartTime;
    public int errors;

    private Report _br;
    private long _firstTs;
    private long _lastTs;
    private long _startTs;
    private long _killTs;
    private long _diedTs;

    public ProcStat(Report br, String component, long firstTs, long lastTs) {
        _br = br;
        proc = component;
        _firstTs = firstTs;
        _lastTs = lastTs;
    }

    public void addData(AMData am) {
        switch (am.getAction()) {
        case AMData.PROC_KILL:
            checkProcStart(_firstTs);
            if ("too many background".equals(am.getExtra())) {
                procKill(am.getTS());
            }
            break;
        case AMData.PROC_DIED:
            checkProcStart(_firstTs);
            procDied(am.getTS());
            break;
        case AMData.PROC_START:
            procStart(am.getTS());
            break;
        }
    }

    private void procStart(long ts) {
        if (_killTs != 0) {
            long time = ts - _killTs;
            bgKillRestartCount++;
            totalBgKillRestartTime += time;
            minBgKillRestartTime = bgKillRestartCount == 1 ? time : Math.min(minBgKillRestartTime, time);
            _killTs = 0;
        }
        if (_diedTs != 0) {
            long time = ts - _diedTs;
            restartCount++;
            totalRestartTime += time;
            minRestartTime = restartCount == 1 ? time : Math.min(minRestartTime, time);
            _diedTs = 0;
        }
        _startTs = ts;
    }

    private void procDied(long ts) {
        count++;
        long time = ts - _startTs;
        totalTime += time;
        maxTime = Math.max(maxTime, time);
        _diedTs = ts;
    }

    private void procKill(long ts) {
        _killTs = ts;
    }

    private void checkProcStart(long ts) {
        if (_startTs == 0) {
            _startTs = ts;
        }
    }

    public void finish() {
        if (_startTs != 0) {
            count++;
            long time = _lastTs - _startTs;
            totalTime += time;
            maxTime = Math.max(maxTime, time);
            _diedTs = _lastTs;
        }

    }

}
