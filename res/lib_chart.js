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
var chartSelectedName = "(none)";

function chartUpdate(cb) {
	$.get(chartModuleName + '$list', function(data) {
		$("#filter").html("");
		for (i = 0; i < data.charts.length; i++) {
			$("#filter").append('<option value="' + data.charts[i] + '">' + data.charts[i] + '</option>');
		}
		if (cb) { cb(); }
	}, "json");
}

function chartSelected() {
	chartSelectedName = $("#filter").val();
	// TODO: reload chart itself
}

function chartSelect(name) {
	$("#filter").val(name);
	chartSelected();
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
						chartUpdate(function() {
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


function chartMain() {
	chartUpdate();
	$("#filter").change(chartSelected);
}

$(document).ready(chartMain);
