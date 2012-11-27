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
package com.sonyericsson.chkbugreport.chart;

import com.sonyericsson.chkbugreport.chart.DataSet.Type;
import com.sonyericsson.chkbugreport.doc.DocNode;
import com.sonyericsson.chkbugreport.doc.Renderer;

import java.awt.Color;
import java.io.IOException;
import java.util.Vector;

/* package */ class FlotGenerator extends DocNode {

    private Vector<DataSet> mDataSets;
    private long mFirstTs;
    private long mLastTs;

    public FlotGenerator(Vector<DataSet> datasets, long firstTs, long lastTs) {
        mDataSets = datasets;
        mFirstTs = firstTs;
        mLastTs = lastTs;
    }

    @Override
    public void render(Renderer r) throws IOException {
        r.println("<h1>!!! UNDER CONSTRUCTION !!!</h1>");
        r.println("<button id=\"zoomOutBtn\">Zoom out</button>");

        // Check how many TYPE_PLOT we have
        int typePlotCount = 0;
        for (DataSet ds : mDataSets) {
            if (ds.getType() == Type.PLOT) {
                typePlotCount++;
            }
        }

        // Insert the placeholders
        boolean typePlotCreated = false;
        int labelWidth = 200;
        Vector<String> ids = new Vector<String>();
        for (int i = 0; i < mDataSets.size(); i++) {
            DataSet ds = mDataSets.get(i);
            if (ds.getType() == Type.PLOT) {
                if (!typePlotCreated) {
                    typePlotCreated = true;
                    r.println("<div id=\"chart\" style=\"width: 800px; height: 400px;\"></div>");
                    ids.add("");
                }
            } else {
                r.println("<div id=\"chart" + i + "\" style=\"width: 800px; height: 25px;\"></div>");
                ids.add(Integer.toString(i));
            }
        }

        r.println("<script type=\"text/javascript\">");

        r.println("$(function(){");

        // Add zooming support
        r.println("function onZoomSelection(event, ranges) {");
        for (String id : ids) {
            r.println("  plot" + id + " = $.plot(chart" + id + ", data" + id + ",");
            r.println("                $.extend(true, {}, options" + id + ", {");
            r.println("                    xaxis: { min: ranges.xaxis.from, max: ranges.xaxis.to }");
            r.println("                 }));");
        }
        r.println("}");
        r.println("function onZoomOut(e) {");
        r.println("  e.preventDefault();");
        for (String id : ids) {
            r.println("  plot" + id + " = $.plot(chart" + id + ", data" + id + ", options" + id + ");");
        }
        r.println("}");
        r.println("$(\"#zoomOutBtn\").click(onZoomOut);");

        if (typePlotCount > 0) {
            // First step: plot the non-strip values
            r.println("var data = [");
            int yaxisCounter = 0;
            for (int i = 0; i < mDataSets.size(); i++) {
                DataSet ds = mDataSets.get(i);
                if (ds.getType() == Type.PLOT) {
                    yaxisCounter++;
                    r.println("  {");
                    r.println("  label: \"" + ds.getName() + "\",");
                    r.println("  yaxis: " + yaxisCounter + ",");
                    r.println("  data: [");
                    int cnt = ds.getDataCount();
                    for (int j = 0; j < cnt; j++) {
                        Data d = ds.getData(j);
                        if (j != 0) {
                            r.print(", ");
                        }
                        if (0 == (j & 7)) {
                            r.println("");
                            r.print("    ");
                        }
                        r.print("[" + d.time + "," + d.value + "]");
                    }
                    r.println("]");
                    r.println("  },");
                }
            }
            r.println("];");
            r.println("var chart = $(\"#chart\");");

            // Build options
            r.println("var options = {");
            r.println("  selection: { mode: 'x' },");
            r.println("  legend: { position: 'nw', margin: [ -" + labelWidth + ", 0 ], }, ");
            r.println("  yaxes: [");
            for (int i = 0; i < typePlotCount; i++) {
                r.println("  {");
                if (i == 0) {
                    r.println("    labelWidth: " + labelWidth + ",");
                } else {
                    r.println("    show: false,");
                }
                r.println("  },");

            }
            r.println("  ],");
            r.println("  xaxis: {");
            r.println("    mode: \"time\",");
            r.println("    min: " + mFirstTs + ",");
            r.println("    max: " + mLastTs + ",");
            r.println("  },");
            r.println("  grid: { hoverable: true, clickable: true },");
            r.println("};");

            // Add zooming support
            r.println("chart.bind(\"plotselected\", onZoomSelection);");

            // Add hover support
            r.println("chart.bind(\"plothover\", function(event,pos,item) { flotHover(plot,event,pos,item); });");

            // Generate the chart
            r.println("var plot = $.plot(chart, data, options);");
        }

        // Next step: plot the rest
        for (int i = 0; i < mDataSets.size(); i++) {
            DataSet ds = mDataSets.get(i);
            if (ds.getType() != Type.PLOT) {
                r.println("var data" + i + " = [");
                r.println("  {");
                r.println("  label: \"" + ds.getName() + "\",");
                r.println("  lines: { show: true, fill: true },");
                r.println("  data: [");
                int cnt = ds.getDataCount();
                if (ds.getType() != Type.STATE) {
                    // Type state is rendered with markings
                    for (int j = 0; j < cnt; j++) {
                        Data d = ds.getData(j);
                        if (j != 0) {
                            r.print(", ");
                        }
                        if (0 == (j & 7)) {
                            r.println("");
                            r.print("    ");
                        }
                        r.print("[" + d.time + "," + d.value + "]");
                    }
                }
                r.println("]");
                r.println("  },");
                r.println("];");
                r.println("var chart" + i + " = $(\"#chart" + i + "\");");

                // Build options
                r.println("var options" + i + " = {");
                r.println("  selection: { mode: 'x' },");
                r.println("  legend: { position: 'nw', margin: [ -" + labelWidth + ", 0 ], }, ");
                r.println("  yaxis: {");
                r.println("    labelWidth: " + labelWidth + ",");
                r.println("    ticks: [ ],");
                r.println("  },");
                r.println("  xaxis: {");
                r.println("    show: false,");
                r.println("    min: " + mFirstTs + ",");
                r.println("    max: " + mLastTs + ",");
                r.println("  },");
                if (ds.getType() == Type.STATE) {
                    // Type state is rendered with markings
                    r.println("  grid: {");
                    r.println("    markings: [");
                    long lastX = -1, lastV = -1;
                    for (int j = 0; j < cnt; j++) {
                        Data d = ds.getData(j);
                        if (j != 0) {
                            r.println("      { xaxis: { from: " + lastX + ", to: " + d.time + "}, color: \"#" + printColor(ds.getColor(lastV)) + "\"}, ");
                        }
                        lastX = d.time;
                        lastV = d.value;
                    }
                    r.println("    ],");
                    r.println("  }");
                }
                r.println("};");

                // Add zooming support
                r.println("chart" + i + ".bind(\"plotselected\", onZoomSelection);");

                // Generate the chart
                r.println("var plot" + i + " = $.plot(chart" + i + ", data" + i + ", options" + i + ");");
            }
        }

        // End of script
        r.println("});");
        r.println("</script>");
    }

    private String printColor(Color color) {
        return String.format("%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

}