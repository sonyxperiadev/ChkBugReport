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
        return 90;
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
        mod.addChapter(new WebOnlyChapter(mod, "Chart editor", MAIN_URL));
    }

    @Override
    public void setWebServer(ChkBugReportWebServer ws) {
        mCharts = new Charts(ws.getModule().getSaveFile());
        ws.addModule(MODULE, this);
    }

    @Web
    public void main(Module mod, HTTPRequest req, HTTPResponse resp) {
        Chapter ch = new Chapter(mod, "Chart Editor");

        // Add extra views to the header
        Span filterSelect = new Span();
        filterSelect.add("Chart:");
        new HtmlNode("select", filterSelect).setName("filter").setId("filter");
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

        // Add custom javascript code
        new Script(ch, "lib_chart.js");

        try {
            Renderer r = new HTTPRenderer(resp, MAIN_URL, mod, ch);
            ch.prepare(r);
            ch.render(r);
        } catch (IOException e) {
            e.printStackTrace();
            resp.setResponseCode(500);
        }
    }

    @Web
    public void list(Module mod, HTTPRequest req, HTTPResponse resp) {
        mCharts.list(mod, req, resp);
    }

    @Web
    public void newChart(Module mod, HTTPRequest req, HTTPResponse resp) {
        mCharts.newChart(mod, req, resp);
    }

}
