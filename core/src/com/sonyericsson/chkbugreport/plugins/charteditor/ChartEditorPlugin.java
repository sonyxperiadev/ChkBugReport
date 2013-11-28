/*
 * Copyright (C) 2013 Sony Mobile Communications AB
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
package com.sonyericsson.chkbugreport.plugins.charteditor;

import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.Plugin;
import com.sonyericsson.chkbugreport.doc.Block;
import com.sonyericsson.chkbugreport.doc.Button;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.HtmlNode;
import com.sonyericsson.chkbugreport.doc.Renderer;
import com.sonyericsson.chkbugreport.doc.Script;
import com.sonyericsson.chkbugreport.doc.Span;
import com.sonyericsson.chkbugreport.doc.WebOnlyChapter;
import com.sonyericsson.chkbugreport.webserver.ChkBugReportWebServer;
import com.sonyericsson.chkbugreport.webserver.Web;
import com.sonyericsson.chkbugreport.webserver.engine.HTTPRenderer;
import com.sonyericsson.chkbugreport.webserver.engine.HTTPRequest;
import com.sonyericsson.chkbugreport.webserver.engine.HTTPResponse;

import java.io.IOException;

public class ChartEditorPlugin extends Plugin {

    private static final String MODULE = "charteditor";
    private static final String MAIN_URL = MODULE + "$main";
    private Charts mCharts;

    @Override
    public int getPrio() {
        return 101;
    }

    @Override
    public void reset() {
        // NOP
    }

    @Override
    public void load(Module mod) {
        // NOP
    }

    @Override
    public void generate(Module mod) {
        mod.addChapter(new WebOnlyChapter(mod.getContext(), "Chart editor", MAIN_URL));
    }

    @Override
    public void setWebServer(ChkBugReportWebServer ws) {
        mCharts = new Charts(ws.getModule().getSaveFile());
        ws.addModule(MODULE, this);
    }

    @Web
    public void main(Module mod, HTTPRequest req, HTTPResponse resp) {
        Chapter ch = new Chapter(mod.getContext(), "Chart Editor");

        // Add extra views to the header
        Span filterSelect = new Span();
        filterSelect.add("Chart:");
        new HtmlNode("select", filterSelect).setName("filter").setId("filter");
        filterSelect.add(new Button("Delete chart", "javascript:chartDelete()").setId("filter-delete"));
        filterSelect.add(new Button("New chart", "javascript:chartNew()"));
        ch.addCustomHeaderView(filterSelect);

        // Add placeholder for confirmation dialog boxes
        new Block(ch).addStyle("dialog").setId("generic-dlg");

        // Add "New chart" dialog box
        new Block(ch).addStyle("dialog").setId("new-chart-dlg")
            .add("Chart name:")
            .add(new HtmlNode("input")
                .setAttr("type", "text")
                .setAttr("name", "name")
                .addStyle("name")
                .addStyle("ui-widget-content ui-corner-all"))
            .add(new Block().addStyle("tip"));

        // Add placeholder for chart plugin list
        new Block(ch).setId("chart-plugins").addStyle("auto-collapsible ui-accordion ui-widget ui-helper-reset")
            .add(new Block()
                .addStyle("header auto-collapsible-header auto-sortable-handle ui-accordion-header ui-helper-reset ui-corner-top ui-accordion-icons")
                .add("Chart plugins..."))
            .add(new Block()
                .addStyle("body auto-collapsible-content ui-helper-reset ui-widget-content ui-corner-bottom"));

        // Add placeholder for the list to add chart plugins from
        new Block(ch).setId("chart-plugins-list").addStyle("auto-collapsible ui-accordion ui-widget ui-helper-reset")
            .add(new Block()
                .addStyle("header auto-collapsible-header auto-sortable-handle ui-accordion-header ui-helper-reset ui-corner-top ui-accordion-icons")
                .add("Add to chart..."))
            .add(new Block()
                .addStyle("body auto-collapsible-content ui-helper-reset ui-widget-content ui-corner-bottom"));

        // Add placeholder for the chart itself
        new Block(ch).setId("chart");

        // Add custom javascript code
        new Script(ch, "lib_chart.js");

        try {
            Renderer r = new HTTPRenderer(resp, MAIN_URL, ch);
            ch.prepare(r);
            ch.render(r);
        } catch (IOException e) {
            e.printStackTrace();
            resp.setResponseCode(500);
        }
    }

    @Web
    public void listPlugins(Module mod, HTTPRequest req, HTTPResponse resp) {
        mCharts.listPlugins(mod, req, resp);
    }

    @Web
    public void listCharts(Module mod, HTTPRequest req, HTTPResponse resp) {
        mCharts.listCharts(mod, req, resp);
    }

    @Web
    public void newChart(Module mod, HTTPRequest req, HTTPResponse resp) {
        mCharts.newChart(mod, req, resp);
    }

    @Web
    public void getChart(Module mod, HTTPRequest req, HTTPResponse resp) {
        mCharts.getChart(mod, req, resp);
    }

    @Web
    public void deleteChart(Module mod, HTTPRequest req, HTTPResponse resp) {
        mCharts.deleteChart(mod, req, resp);
    }

    @Web
    public void deleteChartPlugin(Module mod, HTTPRequest req, HTTPResponse resp) {
        mCharts.deleteChartPlugin(mod, req, resp);
    }

    @Web
    public void addChartPlugin(Module mod, HTTPRequest req, HTTPResponse resp) {
        mCharts.addChartPlugin(mod, req, resp);
    }

    @Web
    public void chartImage(Module mod, HTTPRequest req, HTTPResponse resp) {
        mCharts.chartImage(mod, req, resp);
    }

    @Web
    public void chartAsFlot(Module mod, HTTPRequest req, HTTPResponse resp) {
        Chapter ch = mCharts.chartAsFlot(mod, req, resp);
        if (ch == null) return;
        try {
            Renderer r = new HTTPRenderer(resp, MODULE + "$chartAsFlot", ch);
            ch.prepare(r);
            ch.render(r);
        } catch (IOException e) {
            e.printStackTrace();
            resp.setResponseCode(500);
        }
    }

}
