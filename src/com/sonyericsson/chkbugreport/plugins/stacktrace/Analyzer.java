package com.sonyericsson.chkbugreport.plugins.stacktrace;

import com.sonyericsson.chkbugreport.Bug;
import com.sonyericsson.chkbugreport.BugReport;
import com.sonyericsson.chkbugreport.ProcessRecord;

public class Analyzer {

    public Analyzer(StackTracePlugin stackTracePlugin) {
    }

    public void analyze(BugReport br, Processes processes) {
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
                // Check for deadlocks
                checkDeadLock(p, stack, br);
            }
        }
    }

    private void colorize(Process p, StackTrace stack, BugReport br) {
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

    private void checkMainThreadViolation(StackTrace stack, Process p, BugReport br, boolean isMainThread, boolean isDirect) {
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

    private void checkDeadLock(Process p, StackTrace stack, BugReport br) {
        StackTrace orig = stack;
        int cnt = p.getCount();
        boolean used[] = new boolean[cnt];
        int idx = p.indexOf(stack.getTid());
        while (true) {
            used[idx] = true;
            int tid = stack.getWaitOn();
            if (tid < 0) return;
            idx = p.indexOf(tid);
            if (idx < 0) return;
            stack = p.get(idx);
            if (used[idx]) {
                // DEADLOCK DETECTED
                break;
            }
        }

        // If we got here, then a deadlock was detected, so create the bug

        // We found the dead lock (a loop in the dependency graph), but we need to clean it
        // (remove nodes which are not in the loop)
        int masterIdx = idx; // this is definitely part of the deadlock loop
        for (int i = 0; i < cnt; i++) {
            used[i] = false;
        }
        used[masterIdx] = true;
        while (true) {
            used[idx] = true;
            stack = p.get(idx);
            int tid = stack.getWaitOn();
            idx = p.indexOf(tid);
            if (idx < masterIdx) {
                masterIdx = idx; // we need a unique id per loop, so keep the smallest node index
            }
            if (used[idx]) {
                // Loop closed
                break;
            }
        }

        // Now make sure we print the deadlock only once
        if (p.get(masterIdx) != orig) {
            // This deadlock will be detected by someone else
            return;
        }

        // But: a deadlock will be detected many times, we must make sure we show it only once
        String a1 = "", a2 = "";
        int pid = p.getPid();
        ProcessRecord pr = br.getProcessRecord(pid, false, false);
        if (pr != null) {
            a1 = "<a href=\"" + br.createLinkToProcessRecord(pid) + "\">";
            a2 = "</a>";
        }
        Bug bug = new Bug(Bug.PRIO_DEADLOCK, 0, "Deadlock in process " + p.getName());
        bug.addLine("<div class=\"bug\">");
        bug.addLine("<p>The process " + a1 + p.getName() + "(pid " + pid + ")" + a2 + " has a deadlock involving the following threads (from \"" + p.getGroup().getName() + "\"):</p>");
        bug.addLine("<ul>");
        for (int i = 0; i < cnt; i++) {
            if (!used[i]) continue;
            stack = p.get(i);
            String anchorTrace = p.getAnchor(stack);
            String linkTrace = br.createLinkTo(p.getGroup().getChapter(), anchorTrace);
            bug.addLine("<li><a href=\"" + linkTrace + "\">" + stack.getName() + "</a></li>");
        }
        bug.addLine("</ul>");
        bug.addLine("</div>");
        br.addBug(bug);
    }


}
