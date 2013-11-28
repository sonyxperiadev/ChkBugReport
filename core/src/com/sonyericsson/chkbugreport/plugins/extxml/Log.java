/*
 * Copyright (C) 2011 Sony Ericsson Mobile Communications AB
 * Copyright (C) 2012-2013 Sony Mobile Communications AB
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
package com.sonyericsson.chkbugreport.plugins.extxml;

import com.sonyericsson.chkbugreport.BugReportModule;
import com.sonyericsson.chkbugreport.doc.Block;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.HtmlNode;
import com.sonyericsson.chkbugreport.plugins.logs.LogLine;
import com.sonyericsson.chkbugreport.plugins.logs.LogLines;
import com.sonyericsson.chkbugreport.plugins.logs.LogMatcher;
import com.sonyericsson.chkbugreport.plugins.logs.LogToolbar;
import com.sonyericsson.chkbugreport.plugins.logs.MainLogPlugin;
import com.sonyericsson.chkbugreport.plugins.logs.event.EventLogPlugin;
import com.sonyericsson.chkbugreport.util.Util;
import com.sonyericsson.chkbugreport.util.XMLNode;

import java.util.HashMap;
import java.util.Vector;

/* package */ class Log {

    private BugReportModule mMod;
    private Chapter mCh;
    private XMLNode mCode;

    public Log(BugReportModule mod, Chapter ch, XMLNode code) {
        mMod = mod;
        mCh = ch;
        mCode = code;
    }

    public void exec() {
        // Create the matchers and group them by log
        HashMap<String, LogState> logsMap = new HashMap<String, LogState>();
        Vector<LogState> logs = new Vector<LogState>();
        for (XMLNode node : mCode) {
            String tag = node.getName();
            if (tag == null) {
                // NOP
            } else if ("filter".equals(tag)) {
                String log = node.getAttr("log");
                if (log == null) throw new RuntimeException("filter needs log attribute");
                LogState logState = logsMap.get(log);
                if (logState == null) {
                    logState = new LogState(log);
                    logsMap.put(log, logState);
                    logs.add(logState);
                }
                logState.add(new LogMatcher(mMod, node));
            } else {
                mMod.printErr(4, "Unknown tag in log: " + tag);
            }
        }

        // Process all logs in parallel, always using the next match with the earliest timestamp
        LogCollector result = new LogCollector(mCh);
        while (true) {
            LogLine nextLine = null;
            LogState foundIn = null;
            for (LogState ls : logs) {
                LogLine ll = ls.findNext();
                if (ll == null) continue;
                if (nextLine == null || nextLine.ts > ll.ts) {
                    nextLine = ll;
                    foundIn = ls;
                }
            }
            if (nextLine == null) break; // no more matches

            // Check if the matcher has some extra info (e.g. start new session)
            XMLNode xml = foundIn.getFinder().getXML();
            if (Util.parseBoolean(xml.getAttr("newSession"), false)) {
                Chapter ch = new Chapter(mMod.getContext(), nextLine.msg);
                mCh.addChapter(ch);
                result = new LogCollector(ch);
            }

            result.add(nextLine);
            foundIn.moveToNext();
        }

    }

    /**
     * Collects a log block in a chapter
     */
    static class LogCollector {

        private HtmlNode mLogBlock;

        public LogCollector(Chapter ch) {
            new LogToolbar(ch);
            mLogBlock = new Block(ch).addStyle("log");
        }

        public void add(LogLine line) {
            mLogBlock.add(line.symlink());
        }

    }

    /**
     * Keeps track on which line should be the next match from a given log.
     */
    class LogState {

        public LogState(String logName) {
            this.logName = logName;

            // Find the log
            if (logName.equals("event")) {
                logs = (LogLines) mMod.getInfo(EventLogPlugin.INFO_ID_LOG);
            } else if (logName.equals("system")) {
                logs = (LogLines) mMod.getInfo(MainLogPlugin.INFO_ID_SYSTEMLOG);
            } else if (logName.equals("main")) {
                logs = (LogLines) mMod.getInfo(MainLogPlugin.INFO_ID_MAINLOG);
            }
            if (logs == null || logs.size() == 0) {
                mMod.printErr(4, "Log '" + logName + "' not found or empty!");
            } else {
                count = logs.size();
            }
        }

        /**
         * Consume the last found line, so next time findNext() will look for another one
         */
        public void moveToNext() {
            next = null;
        }

        /**
         * Find the next line which matches the patterns. If a line was already found, it
         * keeps returning the same line until moveToNext() is called.
         * Returns null when there are no more lines
         */
        public LogLine findNext() {
            if (next != null) return next; // already found one, and it's not cleared yet
            while (index < count) {
                next = logs.get(index++);
                for (LogMatcher lm : matchers) {
                    if (lm.matches(next)) {
                        finder = lm;
                        return next;
                    }
                }
                next = null;
            }
            return null;
        }

        public LogMatcher getFinder() {
            return finder;
        }

        public void add(LogMatcher lm) {
            matchers.add(lm);
        }

        public String logName;
        public Vector<LogMatcher> matchers = new Vector<LogMatcher>();
        public LogLines logs;
        public LogLine next;
        public LogMatcher finder;
        public int count;
        public int index;
    }

}
