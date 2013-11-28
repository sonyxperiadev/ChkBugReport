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
import com.sonyericsson.chkbugreport.chart.ChartGenerator;
import com.sonyericsson.chkbugreport.chart.ChartPluginInfo;
import com.sonyericsson.chkbugreport.chart.ChartPluginRepo;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.util.SaveFile;
import com.sonyericsson.chkbugreport.util.SavedData;
import com.sonyericsson.chkbugreport.webserver.JSON;
import com.sonyericsson.chkbugreport.webserver.engine.HTTPRequest;
import com.sonyericsson.chkbugreport.webserver.engine.HTTPResponse;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;

public class Charts extends SavedData<ChartData> {

    public Charts(SaveFile saveFile) {
        super(saveFile, "charts");
        load();
    }

    public ChartData find(String chartName) {
        if (chartName != null) {
            for (ChartData chart : getData()) {
                if (chartName.equals(chart.getName())) {
                    return chart;
                }
            }
        }
        return null;
    }

    @Override
    protected ChartData createItem() {
        return new ChartData("");
    }

    public void listPlugins(Module mod, HTTPRequest req, HTTPResponse resp) {
        JSON json = new JSON();
        json.add("type", "node");
        json.add("name", "Available plugins:");
        JSON plugins = json.addArray("children");
        ChartPluginRepo repo = mod.getChartPluginRepo();
        HashMap<String, JSON> cache = new HashMap<String, JSON>();
        for (ChartPluginInfo info : repo) {
            JSON node = plugins;
            String path[] = info.getName().split("/");
            String prefix = null;
            for (int i = 0; i < path.length - 1; i++) {
                prefix = (prefix == null) ? path[i] : prefix + "/" + path[i];
                JSON tmp = cache.get(prefix);
                if (tmp != null) {
                    node = tmp;
                } else {
                    node = node.add();
                    node.add("type", "node");
                    node.add("name", path[i]);
                    node = node.addArray("children");
                    cache.put(prefix, node);
                }
            }
            node = node.add();
            node.add("type", "leaf");
            node.add("name", path[path.length - 1]);
            node.add("fullName", info.getName());
        }
        json.writeTo(resp);
    }

    public void listCharts(Module mod, HTTPRequest req, HTTPResponse resp) {
        JSON json = new JSON();
        JSON charts = json.addArray("charts");
        for (ChartData chart : getData()) {
            charts.add(chart.getName());
        }
        json.writeTo(resp);
    }

    public void newChart(Module mod, HTTPRequest req, HTTPResponse resp) {
        JSON json = new JSON();
        String name = req.getArg("name");
        if (name == null || name.length() == 0) {
            json.add("err", 400);
            json.add("msg", "Name is not specified or empty!");
        } else if (!name.matches("[a-zA-Z0-9_]+")) {
            json.add("err", 400);
            json.add("msg", "Invalid characters in name!");
        } else if (null != find(name)) {
            json.add("err", 400);
            json.add("msg", "A chart with that name already exists!");
        } else {
            ChartData chart = new ChartData(name);
            add(chart);
            json.add("err", 200);
            json.add("msg", "Chart created!");
        }
        json.writeTo(resp);
    }

    public void getChart(Module mod, HTTPRequest req, HTTPResponse resp) {
        JSON json = new JSON();
        String name = req.getArg("name");
        ChartData chart = find(name);
        if (name == null || name.length() == 0) {
            json.add("err", 400);
            json.add("msg", "Name is not specified or empty!");
        } else if (chart == null) {
            json.add("err", 400);
            json.add("msg", "A chart with that name does not exists!");
        } else {
            json.add("err", 200);
            json.add("id", chart.getId());
            json.add("name", chart.getName());
            JSON plugins = json.addArray("plugins");
            for (String plugin : chart.getPluginsAsArray()) {
                plugins.add(plugin);
            }
        }
        json.writeTo(resp);
    }

    public void deleteChart(Module mod, HTTPRequest req, HTTPResponse resp) {
        JSON json = new JSON();
        String name = req.getArg("name");
        ChartData chart = find(name);
        if (chart == null) {
            json.add("err", 400);
            json.add("msg", "Cannot find chart!");
        } else {
            delete(chart);
            json.add("err", 200);
            json.add("msg", "Chart deleted!");
        }
        json.writeTo(resp);
    }

    public void deleteChartPlugin(Module mod, HTTPRequest req, HTTPResponse resp) {
        JSON json = new JSON();
        String name = req.getArg("name");
        String plugin = req.getArg("plugin");
        ChartData chart = find(name);
        if (chart == null) {
            json.add("err", 400);
            json.add("msg", "Cannot find chart!");
        } else {
            chart.deletePlugin(plugin);
            update(chart);
            json.add("err", 200);
            json.add("msg", "Chart updated!");
        }
        json.writeTo(resp);
    }

    public void addChartPlugin(Module mod, HTTPRequest req, HTTPResponse resp) {
        JSON json = new JSON();
        String name = req.getArg("name");
        String plugin = req.getArg("plugin");
        ChartData chart = find(name);
        if (plugin == null || plugin.length() == 0) {
            json.add("err", 400);
            json.add("msg", "Plugin is not specified or empty!");
        } else if (chart == null) {
            json.add("err", 400);
            json.add("msg", "Cannot find chart!");
        } else {
            chart.addPlugin(plugin);
            update(chart);
            json.add("err", 200);
            json.add("msg", "Chart updated!");
        }
        json.writeTo(resp);
    }

    public ChartGenerator prepareChart(Module mod, HTTPRequest req, HTTPResponse resp) {
        String name = req.getArg("name");
        ChartData chart = find(name);
        if (chart == null) {
            resp.setResponseCode(404);
            return null;
        }
        ChartGenerator gen = new ChartGenerator(name);
        ChartPluginRepo repo = mod.getChartPluginRepo();
        for (String p : chart.getPluginsAsArray()) {
            ChartPluginInfo info = repo.get(p);
            if (info != null) {
                gen.addPlugin(info.createInstance());
            }
        }
        return gen;
    }

    public void chartImage(Module mod, HTTPRequest req, HTTPResponse resp) {
        ChartGenerator gen = prepareChart(mod, req, resp);
        if (gen != null) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            gen.setOutput(out);
            gen.generate(mod);
            resp.setBody(out.toByteArray());
            resp.addHeader("Content-Type", "image/png");
        }
    }

    public Chapter chartAsFlot(Module mod, HTTPRequest req, HTTPResponse resp) {
        ChartGenerator gen = prepareChart(mod, req, resp);
        if (gen != null) {
            Chapter ch = new Chapter(mod.getContext(), "Chart Editor");
            gen.generate(mod);
            ch.add(gen.createFlotVersion());
            return ch;
        }
        return null;
    }

}
