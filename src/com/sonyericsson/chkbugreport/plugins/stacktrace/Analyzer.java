package com.sonyericsson.chkbugreport.plugins.stacktrace;

import com.sonyericsson.chkbugreport.BugReportModule;
import com.sonyericsson.chkbugreport.ProcessRecord;
import com.sonyericsson.chkbugreport.doc.Bug;

import java.util.HashMap;
import java.util.Vector;

public class Analyzer {

    public Analyzer(StackTracePlugin stackTracePlugin) {
    }

    public void analyze(BugReportModule br, Processes processes) {
        for (Process p : processes) {
            int cnt = p.getCount();
            for (int i = 0; i < cnt; i++) {
                StackTrace stack = p.get(i);
                // Apply a default colorization
                colorize(p, stack, br);
                // Check for main thread violations
                boolean isMainThread = stack.getName().equals("main");
                // I misunderstood the IntentService usage. I still keep parts of the code
                // for similar cases
                boolean isIntentServiceThread = false; // stack.getName().startsWith("IntentService[");
                if (isMainThread || isIntentServiceThread) {
                    checkMainThreadViolation(stack, p, br, isMainThread, true);
                    // Also check indirect violations: if the main thread is waiting on another thread
                    int waitOn = stack.getWaitOn();
                    if (waitOn >= 0) {
                        StackTrace other = p.findTid(waitOn);
                        checkMainThreadViolation(other, p, br, isMainThread, false);
                    }
                }
            }
        }
        // Check for inter process deadlocks
        checkDeadLock(processes, br);
    }

    private void colorize(Process p, StackTrace stack, BugReportModule br) {
        if (stack == null) return;

        // Check android looper based threads
        int loopIdx = stack.findMethod("android.os.Looper.loop");
        if (loopIdx >= 0) {
            int waitIdx1 = stack.findMethod("android.os.MessageQueue.nativePollOnce");
            int waitIdx2 = stack.findMethod("android.os.MessageQueue.next");
            if (waitIdx1 < 0 && waitIdx2 < 0) {
                // This looper based thread seems to be doing something
                stack.setStyle(0, loopIdx, StackTraceItem.STYLE_BUSY);
                p.addBusyThreadStack(stack);
            }
        }

        // Check binder transactions
        int binderIdx = stack.findMethod("android.os.Binder.execTransact");
        if (binderIdx >= 0) {
            stack.setStyle(0, binderIdx, StackTraceItem.STYLE_BUSY);
            p.addBusyThreadStack(stack);
        }
        // Check NativeStart.run based threads
        int nativeStartRunIdx = stack.findMethod("dalvik.system.NativeStart.run");
        if (nativeStartRunIdx > 0) {
            // Thread is not currently in NativeStart.run, it seems to be doing something
            stack.setStyle(0, nativeStartRunIdx, StackTraceItem.STYLE_BUSY);
            p.addBusyThreadStack(stack);
        }
    }

    private void checkMainThreadViolation(StackTrace stack, Process p, BugReportModule br, boolean isMainThread, boolean isDirect) {
        if (stack == null) return;
        int itemCnt = stack.getCount();
        for (int j = itemCnt-1; j >= 0; j--) {
            StackTraceItem item = stack.get(j);
            if (isMainViolation(item.getMethod())) {
                // Report a bug
                StackTraceItem caller = stack.get(j+1);
                String anchorTrace = p.getAnchor(stack);
                String linkTrace = br.createLinkTo(p.getGroup().getChapter(), anchorTrace);
                String title = (isMainThread ? "Main" : "IntentService") + " thread violation: " + item.getMethod();
                if (!isDirect) {
                    title = "(Indirect) " + title;
                }
                ProcessRecord pr = br.getProcessRecord(p.getPid(), true, true);
                String startPrA = "";
                String endPrA = "";
                if (pr != null) {
                    startPrA = "<a href=\"" + br.createLinkToProcessRecord(p.getPid()) + "\">";
                    endPrA = "</a>";
                }
                Bug bug = new Bug(Bug.PRIO_MAIN_VIOLATION, 0, title);
                bug.addLine("<div class=\"bug\">");
                bug.addLine("<p>The process " + startPrA + p.getName() + "(pid " + p.getPid() + ")" + endPrA +
                        " is violating the " + (isMainThread ? "main" : "IntentService") + " thread");
                bug.addLine("by calling the method <tt>" + item.getMethod() + "</tt>");
                bug.addLine("from method <tt>" + caller.getMethod() + "(" + caller.getFileName() + ":" + caller.getLine() + ")</tt>!</p>");
                bug.addLine("<div><a href=\"" + linkTrace + "\">(full stack trace in chapter \"" +
                        stack.getProcess().getGroup().getName() + "\")</a></div>");
                if (!isDirect) {
                    String anchorWait = p.getAnchor(p.findTid(1));
                    String linkWait = br.createLinkTo(p.getGroup().getChapter(), anchorWait);
                    bug.addLine("<p>NOTE: This is an indirect violation: the thread is waiting on another thread which executes a blocking method!</p>");
                    bug.addLine("<div><a href=\"" + linkWait + "\">(full stack trace on waiting thread)</a></div>");
                }
                bug.addLine("</div>");
                br.addBug(bug);
                // Also colorize the stack trace
                stack.setStyle(j, j + 2, StackTraceItem.STYLE_ERR);
                break;
            }
        }
    }

    private boolean isMainViolation(String method) {
        if (method.startsWith("android.content.ContentResolver.")) return true;
        if (method.startsWith("org.apache.harmony.luni.internal.net.www.protocol.http.HttpURLConnectionImpl.")) return true;
        if (method.startsWith("org.apache.harmony.luni.internal.net.www.protocol.https.HttpURLConnectionImpl.")) return true;
        if (method.startsWith("org.apache.harmony.luni.internal.net.www.protocol.http.HttpsURLConnectionImpl.")) return true;
        if (method.startsWith("org.apache.harmony.luni.internal.net.www.protocol.https.HttpsURLConnectionImpl.")) return true;
        if (method.startsWith("android.database.sqlite.SQLiteDatabase.")) return true;
        return false;
    }

    private void checkDeadLock(Processes processes, BugReportModule br) {
        // This hash table contains all the stack traces which are involved in deadlocks
        // The key is the thread, the value is the list of threads which represent the actual deadlock
        // Thus the key might not be in the actual deadlock, but have a direct/indirect dependency
        // towards it
        HashMap<StackTrace, Vector<StackTrace>> used = new HashMap<StackTrace, Vector<StackTrace>>();

        // Loop all process
        for (Process proc : processes) {
            // Loop all threads
            for (StackTrace stack : proc) {

                if (used.containsKey(stack)) {
                    // Already found this to be part of a deadlock, or depend on it
                    // Just skip it
                    continue;
                }

                // Now we simply follow the dependencies and check if we arrive back
                Vector<StackTrace> deps = new Vector<StackTrace>();
                deps.add(stack);
                while (true) {
                    stack = stack.getDependency();

                    if (stack == null) {
                        // Dead end => no deadlock
                        // Or item already detected in a deadlock, avoid reporting twice
                        deps.clear();
                        break;
                    }

                    Vector<StackTrace> deadlock = used.get(stack);
                    if (deadlock != null) {
                        // This means the the previous thread(s) all depend on an already
                        // detected deadlock, so we need to update the information
                        for (StackTrace item : deps) {
                            used.put(item, deadlock);
                        }
                        // also abort current search
                        deps.clear();
                        break;
                    }

                    int idx = deps.indexOf(stack);
                    if (idx >= 0) {
                        // dDadlock found, keep track of found items
                        for (StackTrace item : deps) {
                            used.put(item, deps);
                        }
                        // cycle starts at idx, so we need to get rid of items before idx
                        for (int i = idx - 1; i >= 0; i--) {
                            deps.remove(i);
                        }
                        break;
                    }

                    deps.add(stack);
                }
            }
        }

        // Now we have all the detected deadlocks in the "used" hashmap, but we still need to
        // extract it in a nice way
        Object[] keys = used.keySet().toArray();
        int len = keys.length;
        for (int i = 0; i < len; i++) {
            StackTrace key = (StackTrace) keys[i];
            if (key == null) continue; // already processed
            Vector<StackTrace> deadlock = used.get(key);
            Vector<StackTrace> blocked = new Vector<StackTrace>();
            Vector<Process> procList = new Vector<Process>();
            for (int j = i; j < len; j++) {
                StackTrace item = (StackTrace) keys[j];
                if (item != null && deadlock == used.get(item)) {
                    // This thread is involved in this deadlock
                    if (!deadlock.contains(item)) {
                        // If it's not part of the deadlock, then it just depends on it
                        blocked.add(item);
                    }
                    // collect process names
                    if (!procList.contains(item.getProcess())) {
                        procList.add(item.getProcess());
                    }
                    // avoid further processing of this thread
                    keys[j] = null;
                }
            }

            // Collect process names
            StringBuffer procNames = new StringBuffer();
            for (int j = 0; j < procList.size(); j++) {
                if (j > 0) {
                    procNames.append(", ");
                }
                procNames.append(procList.get(j).getName());
            }

            Bug bug = new Bug(Bug.PRIO_DEADLOCK, 0, "Deadlock in process(es) " + procNames);
            bug.addLine("<div class=\"bug\">");
            bug.addLine("<p>The process(es) <b>" + procNames + "</b> has/have a deadlock involving the " +
                    "following threads (from \"" + procList.get(0).getGroup().getName() + "\"):</p>");
            listThreads(br, bug, deadlock);
            if (blocked.size() > 0) {
                bug.addLine("<p>Additionally the following threads are blocked due to this deadlock:</p>");
                listThreads(br, bug, blocked);
            }
            bug.addLine("</div>");
            br.addBug(bug);
        }

    }

    private void listThreads(BugReportModule br, Bug bug, Vector<StackTrace> list) {
        bug.addLine("<ul>");
        for (StackTrace stack : list) {
            Process p = stack.getProcess();
            String anchorTrace = p.getAnchor(stack);
            String linkTrace = br.createLinkTo(p.getGroup().getChapter(), anchorTrace);
            bug.addLine("<li>");
            int pid = p.getPid();
            ProcessRecord pr = br.getProcessRecord(pid, false, false);
            if (pr != null) {
                bug.addLine("<a href=\"" + br.createLinkToProcessRecord(pid) + "\">" + p.getName() + "</a> / ");
            } else {
                bug.addLine(p.getName() + " / ");
            }
            bug.addLine("<a href=\"" + linkTrace + "\">" + stack.getName() + "</a></li>");
            bug.addLine("</li>");
        }
        bug.addLine("</ul>");
    }


}
