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
package com.sonyericsson.chkbugreport.plugins.stacktrace;

import com.sonyericsson.chkbugreport.BugReportModule;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

/* package */ final class DBImporter {

    private Connection mConn;

    public DBImporter(StackTracePlugin stackTracePlugin) {
    }

    public void importIntoDB(BugReportModule br, HashMap<Integer, Processes> allProcesses) {
        mConn = br.getSQLConnection();
        if (mConn != null) {
            try {
                importIntoDBUnsafe(allProcesses);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void importIntoDBUnsafe(HashMap<Integer,Processes> allProcesses) throws SQLException {
        int nextProcessId = 0;
        int nextThreadId = 0;
        int nextItemId = 0;

        // Create the table structure
        Statement stat = mConn.createStatement();
        stat.execute("CREATE TABLE stacktrace_groups (id int, name varchar)");
        stat.execute("INSERT INTO stacktrace_groups VALUES (" + StackTracePlugin.ID_NOW + ", \"VM traces just now\")");
        stat.execute("INSERT INTO stacktrace_groups VALUES (" + StackTracePlugin.ID_ANR + ", \"VM traces at last ANR\")");
        stat.execute("INSERT INTO stacktrace_groups VALUES (" + StackTracePlugin.ID_OLD + ", \"VM traces\")");
        stat.execute("CREATE TABLE stacktrace_processes (id int, pid int, name varchar, group_id int)");
        stat.execute("CREATE TABLE stacktrace_threads (id int, tid int, name varchar, process_id int)");
        stat.execute("CREATE TABLE stacktrace_items (id int, idx id, method varchar, file varchar, line int, thread_id int)");
        stat.close();
        PreparedStatement insProc = mConn.prepareStatement("INSERT INTO stacktrace_processes(id,pid,name,group_id) VALUES (?,?,?,?)");
        PreparedStatement insThread = mConn.prepareStatement("INSERT INTO stacktrace_threads(id,tid,name,process_id) VALUES (?,?,?,?)");
        PreparedStatement insItem = mConn.prepareStatement("INSERT INTO stacktrace_items(id,idx,method,file,line,thread_id) VALUES (?,?,?,?,?,?)");

        // Handle each process group
        for (Processes processes : allProcesses.values()) {
            for (Process process : processes) {
                int processId = ++nextProcessId;
                insProc.setInt(1, processId);
                insProc.setInt(2, process.getPid());
                insProc.setString(3, process.getName());
                insProc.setInt(4, processes.getId());
                insProc.addBatch();

                int threadCnt = process.getCount();
                for (int i = 0; i < threadCnt; i++) {
                    int threadId = ++nextThreadId;
                    StackTrace stack = process.get(i);
                    insThread.setInt(1, threadId);
                    insThread.setInt(2, stack.getTid());
                    insThread.setString(3, stack.getName());
                    insThread.setInt(4, processId);
                    insThread.addBatch();

                    int stackSize = stack.getCount();
                    for (int j = 0; j < stackSize; j++) {
                        int itemId = ++nextItemId;
                        StackTraceItem item = stack.get(j);
                        insItem.setInt(1, itemId);
                        insItem.setInt(2, j);
                        insItem.setString(3, item.getMethod());
                        insItem.setString(4, item.getFileName());
                        insItem.setInt(5, item.getLine());
                        insItem.setInt(6, threadId);
                        insItem.addBatch();
                    }
                }
            }
        }

        // Cleanup
        insItem.executeBatch();
        insItem.close();
        insThread.executeBatch();
        insThread.close();
        insProc.executeBatch();
        insProc.close();
        mConn.commit();
    }


}
