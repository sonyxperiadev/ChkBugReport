package com.sonyericsson.chkbugreport.plugins.logs.event;

import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.Util;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.Img;
import com.sonyericsson.chkbugreport.doc.Link;
import com.sonyericsson.chkbugreport.doc.Para;
import com.sonyericsson.chkbugreport.doc.Table;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map.Entry;

public class ActivityManagerGraphGenerator {

    private EventLogPlugin mPlugin;
    private ActivityManagerTrace mAmTrace;

    public ActivityManagerGraphGenerator(EventLogPlugin plugin, ActivityManagerTrace amTrace) {
        mPlugin = plugin;
        mAmTrace = amTrace;
    }

    /**
     * Generate report based on AM logs
     * @param br The bugreport
     * @param mainCh The main chapter
     */
    public void generate(Module br, Chapter mainCh) {
        // Sanity check
        int cnt = mAmTrace.size();
        if (cnt == 0) {
            return;
        }
        long firstTs = mPlugin.getFirstTs();
        long lastTs = mPlugin.getLastTs();
        if (firstTs >= lastTs) {
            br.printErr(3, "Event log too short!");
            return;
        }

        // Create the chapter
        Chapter ch = new Chapter(br, "AM Graphs");
        mainCh.addChapter(ch);

        // Then we process each log record and add it to the chart
        // and to the VCD file
        int vcdId = 1;
        HashMap<String, Integer> vcdIds = new HashMap<String, Integer>();
        HashMap<String, AMChart> charts = new HashMap<String, AMChart>();
        for (int i = 0; i < cnt; i++) {
            AMData am = mAmTrace.get(i);
            int pid = am.getPid();
            String component = am.getComponent();
            if (pid < 0 || component == null) continue;
            if (null == vcdIds.get(component)) {
                vcdIds.put(component, vcdId++);
            }
            AMChart chart = charts.get(component);
            if (chart == null) {
                chart = new AMChart(pid, component, firstTs, lastTs);
                charts.put(component, chart);
            }
            chart.addData(am);
        }

        // Write the VCD file
        String fn = br.getRelRawDir() + "am_logs.vcd";
        try {
            FileOutputStream fos = new FileOutputStream(br.getBaseDir() + fn);
            PrintStream fo = new PrintStream(fos);

            // write header
            fo.println("$timescale 1ms $end");
            fo.println("$scope am_logs $end");
            for (Entry<String, Integer> item : vcdIds.entrySet()) {
                fo.println("$var wire 1 a" + item.getValue() + " " + item.getKey() + " $end");
            }
            fo.println("$upscope $end");
            fo.println("$enddefinitions $end");

            // Write initial values
            fo.println("#" + firstTs);
            for (Entry<String, Integer> item : vcdIds.entrySet()) {
                int id = item.getValue();
                String component = item.getKey();
                AMChart chart = charts.get(component);
                int initState = AMChart.STATE_UNKNOWN;
                if (chart != null) {
                    initState = chart.getInitState();
                }
                char state = getVCDState(initState);
                fo.println("b" + state + " a" + id);
            }

            // Write events
            for (int i = 0; i < cnt; i++) {
                AMData am = mAmTrace.get(i);
                int pid = am.getPid();
                String component = am.getComponent();
                if (pid < 0 || component == null) continue;
                int id = vcdIds.get(component);
                int state = AMChart.actionToState(am.getAction());
                if (state != AMChart.STATE_UNKNOWN) {
                    fo.println("#" + am.getTS());
                    fo.println("b" + getVCDState(state) + " a" + id);
                }
            }

            new Para(ch)
                .add("AM logs converted to VCD file (you can use GTKWave to open it): ")
                .add(new Link(fn, fn));
        } catch (IOException e) {
            br.printErr(3, "Error saving vcd file: " + e);
        }

        // We need to finish the charts (fill in the end, save the image, etc)
        Table t = new Table(Table.FLAG_DND, ch);
        t.addColumn("Component", Table.FLAG_NONE);
        t.addColumn("Graph", Table.FLAG_NONE);
        t.begin();
        fn = "amchart_time.png";
        if (Util.createTimeBar(br, fn, AMChart.W, firstTs, lastTs)) {
            t.addData("");
            t.addData(new Img(fn));
        }
        for (AMChart chart : charts.values()) {
            fn = chart.finish(br);
            if (fn == null) continue;
            t.addData(chart.getComponent());
            t.addData(new Img(fn));
        }
        t.end();
    }

    private char getVCDState(int initState) {
        switch (initState) {
            case AMChart.STATE_ALIVE:
                return 'Z';
            case AMChart.STATE_CREATED:
                return '-';
            case AMChart.STATE_RESUMED:
                return 'X';
            default:
                return '0';
        }
    }

}
