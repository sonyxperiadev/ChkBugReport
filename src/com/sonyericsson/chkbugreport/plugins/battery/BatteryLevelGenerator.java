package com.sonyericsson.chkbugreport.plugins.battery;

import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.chart.ChartGenerator;
import com.sonyericsson.chkbugreport.chart.ChartPlugin;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.DocNode;
import com.sonyericsson.chkbugreport.plugins.logs.event.BatteryLevels;

import java.util.Vector;

public class BatteryLevelGenerator {

    private BatteryLevels mData;
    private ChartGenerator mChartGen = new ChartGenerator("Battery level");

    public BatteryLevelGenerator(BatteryLevels batteryLevels) {
        mData = batteryLevels;
        mChartGen.addPlugin(new BatteryLevelChart(batteryLevels));
    }

    public void generate(Module br, Chapter mainCh) {
        Chapter ch = new Chapter(br, "Battery level");
        mainCh.addChapter(ch);

        generateGraph(br, ch);
    }

    private boolean generateGraph(Module br, Chapter ch) {
        String fn = "eventlog_batterylevel_graph.png";
        long firstTs = mData.getFirstTs();
        long lastTs = mData.getLastTs();

        DocNode ret = mChartGen.generate(br, fn, firstTs, lastTs);
        if (ret == null) {
            return false;
        }
        ch.add(ret);
        return true;
    }

    public void addPlugins(Vector<ChartPlugin> plugins) {
        mChartGen.addPlugins(plugins);
    }

}
