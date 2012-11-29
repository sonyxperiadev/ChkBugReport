package com.sonyericsson.chkbugreport.plugins.stacktrace;

import com.sonyericsson.chkbugreport.BugReportModule;
import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.ProcessRecord;
import com.sonyericsson.chkbugreport.doc.Anchor;
import com.sonyericsson.chkbugreport.doc.Block;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.DocNode;
import com.sonyericsson.chkbugreport.doc.Hint;
import com.sonyericsson.chkbugreport.doc.Img;
import com.sonyericsson.chkbugreport.doc.Link;
import com.sonyericsson.chkbugreport.doc.List;
import com.sonyericsson.chkbugreport.doc.Para;
import com.sonyericsson.chkbugreport.doc.ProcessLink;
import com.sonyericsson.chkbugreport.doc.Span;
import com.sonyericsson.chkbugreport.ps.PSRecord;
import com.sonyericsson.chkbugreport.util.Util;

import java.util.Calendar;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* package */ class Generator {

    public Generator(StackTracePlugin plugin) {
    }

    public void generate(Module rep, Processes processes) {
        BugReportModule br = (BugReportModule)rep;

        int id = processes.getId();
        String chapterName = processes.getName();

        // Generate chapter
        genChapter(br, id, processes, chapterName);
    }

    private void genChapter(BugReportModule br, int id, Processes processes, String chapterName) {
        Chapter main = processes.getChapter();
        Calendar tsBr = br.getTimestamp();
        Calendar tsSec = Util.parseTimestamp(br, processes.getSectionName());
        String diff = Util.formatTimeDiff(tsBr, tsSec, true);
        diff = (diff == null) ? "" : "; " + diff + "";
        new Hint(main).add("Generated from : \"" + processes.getSectionName() + "\" " + diff);

        // Dump the actuall stack traces
        for (Process p : processes) {
            Chapter ch = p.getChapter();
            main.addChapter(ch);

            // Add timestamp
            String dateTime = p.getDate() + " " + p.getTime();
            Calendar tsProc = Util.parseTimestamp(br, dateTime);
            diff = Util.formatTimeDiff(tsBr, tsProc, true);
            diff = (diff == null) ? "" : "; " + diff + "";
            new Hint(ch).add(dateTime + diff);

            // Add link from global process record
            ProcessRecord pr = br.getProcessRecord(p.getPid(), true, true);
            pr.suggestName(p.getName(), 50);
            String linkText = "Related stack traces &gt;&gt;&gt;";
            if (id == StackTracePlugin.ID_NOW) {
                linkText = "Current stack trace &gt;&gt;&gt;";
            } else if (id == StackTracePlugin.ID_ANR) {
                linkText = "Stack trace at last ANR &gt;&gt;&gt;";
            } else if (id == StackTracePlugin.ID_OLD) {
                linkText = "Old stack traces &gt;&gt;&gt;";
            }
            new Para(pr).add(new Link(ch.getAnchor(), linkText));

            int cnt = p.getCount();
            for (int i = 0; i < cnt; i++) {
                StackTrace stack = p.get(i);
                Anchor anchorTrace = stack.getAnchor();
                DocNode waiting = new DocNode();
                int waitOn = stack.getWaitOn();
                StackTrace aidlDep = stack.getAidlDependency();
                if (waitOn >= 0) {
                    StackTrace stackWaitOn = p.findTid(waitOn);
                    waiting.add(" waiting on ");
                    waiting.add(new Link(stackWaitOn.getAnchor(), "thread-" + waitOn));
                } else if (aidlDep != null) {
                    Process aidlDepProc = aidlDep.getProcess();
                    waiting.add(" waiting on ");
                    waiting.add(new Link(aidlDep.getAnchor(), aidlDepProc.getName() + "/" + aidlDep.getName()));
                }
                String sched = parseSched(stack.getProperty("sched"));
                String nice = parseNice(stack.getProperty("nice"));
                ch.add(anchorTrace);
                DocNode st = new Block(ch).addStyle("stacktrace");
                DocNode stName = new Block(st).addStyle("stacktrace-name");
                new Span(stName).add("-");
                new Span(stName).addStyle("stacktrace-name-name").add(stack.getName());
                new Span(stName).addStyle("stacktrace-name-info")
                    .add(
                        " (tid=" + stack.getTid() +
                        " pid=" + stack.getProperty("sysTid") +
                        " prio=" + stack.getPrio() + " ")
                    .add(new Img(nice))
                    .add(new Img(sched))
                    .add(" state=" + stack.getState())
                    .add(waiting)
                    .add(")");
                DocNode stItems = new Block(st).addStyle("stacktrace-items");
                int itemCnt = stack.getCount();
                for (int j = 0; j < itemCnt; j++) {
                    StackTraceItem item = stack.get(j);
                    DocNode stItem = new Block(stItems).addStyle("stacktrace-item");
                    new Span(stItem).addStyle("stacktrace-item-method").addStyle(item.getStyle()).add(item.getMethod());
                    if (item.getFileName() != null) {
                        new Span(stItem).addStyle("stacktrace-item-file").add(" (" + item.getFileName() + ")");
                    }
                }
            }

            cnt = p.getUnknownThreadCount();
            if (cnt > 0) {
                DocNode stu = new Block(ch).addStyle("stacktrace-unknown");
                new Para(stu).add("Other/unknown threads:");
                new Hint(stu).add("These are child processes/threads of this application, but there is no information about the stacktrace of them. They are either native threads, or dalvik threads which haven't existed yet when the stack traces were saved");
                List list = new List(List.TYPE_UNORDERED, stu);
                for (int i = 0; i < cnt; i++) {
                    PSRecord psr = p.getUnknownThread(i);
                    new DocNode(list).add(psr.getName() + "(" + psr.getPid() + ")");
                }
            }
        }

        // Detect threads which could be busy
        Vector<StackTrace> busy = processes.getBusyStackTraces();
        if (busy.size() > 0) {
            // Build list
            List list = new List(List.TYPE_UNORDERED);
            for (StackTrace stack : busy) {
                Process proc = stack.getProcess();
                new DocNode(list)
                    .add(new ProcessLink(br, proc.getPid()))
                    .add("/")
                    .add(new Link(stack.getAnchor(), stack.getName()));
            }

            // Build comment
            new Para(main)
                .add("The following threads seems to be busy:")
                .add(new Hint().add("NOTE: there might be some more busy threads than these, " +
                    "since this tool has only a few patterns to recognise busy threads. " +
                    "Currently only binder threads and looper based threads are recognised."))
                .add(list);

        }

        // List all ongoig AIDL calls
        Vector<StackTrace> aidl = processes.getAIDLCalls();
        if (aidl.size() > 0) {
            // Build list
            List list = new List(List.TYPE_UNORDERED);
            for (StackTrace stack : aidl) {
                StackTrace dep = stack.getAidlDependency();
                new DocNode()
                    .add(new ProcessLink(br, stack.getPid()))
                    .add("/" + stack.getName())
                    .add(" -&gt; ")
                    .add(new ProcessLink(br, dep.getPid()))
                    .add("/" + dep.getName())
                    .add(" (" + detectAidlCall(stack) + ")");
            }

            // Build comment
            new Para(main)
                .add("The following threads are executing AIDL calls:")
                .add(list);
        }

    }

    private String detectAidlCall(StackTrace stack) {
        Pattern p = Pattern.compile("([^.]+)\\$Stub\\$Proxy\\.(.+)");
        for (StackTraceItem item : stack) {
            String method = item.getMethod();
            Matcher m = p.matcher(method);
            if (m.find()) {
                String interf = m.group(1);
                String msg = m.group(2);
                return interf + "." + msg;
            }
        }
        return "could find interface call";
    }

    private String parseSched(String sched) {
        int ret = PSRecord.PCY_UNKNOWN;
        if (sched != null) {
            String fields[] = sched.split("/");
            try {
                int v = Integer.parseInt(fields[0]);
                switch (v) {
                    case 0: ret = PSRecord.PCY_NORMAL; break;
                    case 1: ret = PSRecord.PCY_FIFO; break;
                    case 3: ret = PSRecord.PCY_BATCH; break;
                }
            } catch (Exception e) { /* NOP */ }
        }
        return Util.getSchedImg(ret);
    }

    private String parseNice(String sNice) {
        int nice = PSRecord.NICE_UNKNOWN;
        try {
            nice = Integer.parseInt(sNice);
        } catch (Exception e) { /* NOP */ }
        return Util.getNiceImg(nice);
    }


}
