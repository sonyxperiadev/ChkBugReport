package com.sonyericsson.chkbugreport.plugins.battery;

import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.chart.LogFilterChartPlugin;
import com.sonyericsson.chkbugreport.util.XMLNode;

public class Hooks {

    private BatteryInfoPlugin mPlugin;

    public Hooks(BatteryInfoPlugin plugin) {
        mPlugin = plugin;
    }

    public void add(Module mod, XMLNode hook) {
        for (XMLNode item : hook) {
            String tag = item.getName();
            if (tag == null) continue;
            if (tag.equals("logchart")) {
                LogFilterChartPlugin logChart = LogFilterChartPlugin.parse(mod, item);
                mPlugin.addBatteryLevelChartPlugin(logChart);
            } else {
                mod.printErr(4, "Unknown tag in battery info hook: " + tag);
            }
        }
    }

}
