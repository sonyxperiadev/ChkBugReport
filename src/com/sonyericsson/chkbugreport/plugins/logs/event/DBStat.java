package com.sonyericsson.chkbugreport.plugins.logs.event;

import java.util.Vector;

/**
 * Direct database access statistics
 */
public class DBStat {
    public String db;
    public int totalTime;
    public int maxTime;
    public int count;
    public Vector<Integer> pids = new Vector<Integer>();
}
