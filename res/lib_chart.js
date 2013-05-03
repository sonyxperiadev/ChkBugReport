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
/* Javascript library for processing logs */

/** The name of the module */
var chartModuleName = "charteditor";
/** The name of the selected chart */
var chartSelectedName = null;

function chartUpdateList(cb) {
	$.get(chartModuleName + '$listCharts', function(data) {
		$("#filter").html("");
		for (var i = 0; i < data.charts.length; i++) {
			$("#filter").append('<option value="' + data.charts[i] + '">' + data.charts[i] + '</option>');
		}
		if (data.charts.length == 0) {
			$("#filter").hide();
			$("#filter-delete").hide();
		} else {
			$("#filter").show();
			$("#filter-delete").show();
		}
		if (cb) { cb(); }
	}, "json");
}

function chartUpdate() {
	var chart = $("#chart");
	var chartPlugins = $("#chart-plugins");
	var chartPluginsList = $("#chart-plugins-list");
	if (chartSelectedName == null) {
		chart.hide();
		chartPlugins.hide();
		chartPluginsList.hide();
	} else {
		chart.show();
		chartPlugins.show();
		chartPluginsList.show();
		$.get(chartModuleName + '$getChart', { name : chartSelectedName }, function(data) {
			var html = "<ul>\n";
			for (var i = 0; i < data.plugins.length; i++) {
				var p = data.plugins[i];
				html += "  <li><a href=\"javascript:chartDeletePlugin('" + p + "');\">[DEL]</a> " + p + "</li>\n";
			}
			html += "</ul>\n";
			chartPlugins.find(".body").html(html);
		}, "json");
		chart.html('<div class="hint"><a href="' + chartModuleName + '$chartAsFlot?name=' + encodeURIComponent(chartSelectedName) + '">Click here for interactive version</a></div>');
		chart.append('<img src="' + chartModuleName + '$chartImage?name=' + encodeURIComponent(chartSelectedName) + '" />');
	}
}

function chartSelected() {
	chartSelectedName = $("#filter").val();
	chartUpdate();
}

function chartSelect(name) {
	$("#filter").val(name);
	chartSelected();
}

function chartAddPlugin(plugin) {
	var opts = {
			name : chartSelectedName,
			plugin : plugin
	};
	$.get(chartModuleName + '$addChartPlugin', opts, function(data) {
		chartUpdate();
	}, "json");
}

function chartDeletePlugin(plugin) {
	var opts = {
			name : chartSelectedName,
			plugin : plugin
	};
	$.get(chartModuleName + '$deleteChartPlugin', opts, function(data) {
		chartUpdate();
	}, "json");
}

function chartNew() {
	$("#new-chart-dlg .tip")
		.html("Please give a non-empty name to this chart. Allowed characters are: a-z, A-Z, 0-9!")
		.removeClass("ui-state-error");
	$("#new-chart-dlg .name").val("");
	var dlg = $("#new-chart-dlg");
	dlg.dialog({
		modal: true,
		position: "top",
		buttons: {
			"Create new chart" : function() {
				var name = $("#new-chart-dlg .name").val();
				$.get(chartModuleName + '$newChart', { name : name }, function(data) {
					if (data.err == 200) {
						dlg.dialog("close");
						chartUpdateList(function() {
							chartSelect(name);
						});
					} else {
						$("#new-chart-dlg .tip").html(data.msg).addClass("ui-state-error");
					}
				}, "json");
			},
			Cancel: function() {
				dlg.dialog("close");
			}
		}
	});
}

function chartDelete() {
	var dlg = $("#generic-dlg");
	dlg.html("Are you sure you want to delete this chart?");
	dlg.dialog({
		modal: true,
		position: "top",
		buttons: {
			Yes : function() {
				$.get(chartModuleName + '$deleteChart', { name : chartSelectedName }, function(data) {
					chartUpdateList(chartSelected);
				});
				dlg.dialog("close");
			},
			No: function() {
				dlg.dialog("close");
			}
		}
	});
}

function renderPluginList(root, data) {
	if (data.type == "node") {
		root.append("<span>" + data.name + "</span>\n");
		root.append("<ul></ul>");
		root = root.find("ul");
		for (var i = 0; i < data.children.length; i++) {
			var child = $("<li></li>\n");
			root.append(child);
			renderPluginList(child, data.children[i]);
		}
	} else {
		root.append("<a href=\"javascript:chartAddPlugin('" + data.fullName + "');\">" + data.name + "</a>");
	}
}

function initChartPluginsList() {
	$.get(chartModuleName + '$listPlugins', function(data) {
		var root = $("#chart-plugins-list").find(".body");
		root.html("");
		renderPluginList(root, data);
		root.jstree({
			"themes" : {
				"theme" : "classic",
				"dots" : false,
				"icons" : false
			},
			"plugins" : [ "themes", "html_data" ]
		});
	}, "json");
}

function chartMain() {
	chartUpdateList(chartSelected);
	$("#filter").change(chartSelected);
	initChartPluginsList();
}

$(document).ready(chartMain);
