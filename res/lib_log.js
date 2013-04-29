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

/** The name of the selected filter group (or '(none)' if no filter should be used) */
var logSelectedFilter = "(none)";

function logUpdateFilterGroups() {
	$.get(logid + '$listFilterGroups', function(data) {
		$("#filter").html("");
		$("#filter").append('<option value="(none)">No filter</option>');
		for (i = 0; i < data.filters.length; i++) {
			$("#filter").append('<option value="' + data.filters[i] + '">' + data.filters[i] + '</option>');
		}
	}, "json");
}

function logUpdateFilters() {
	var f = $("#log-filter");
	if (logSelectedFilter == "(none)") {
		f.hide();
	} else {
		f.show();
		f.find(".header span").html("Edit filter: " + logSelectedFilter);
		logResetNewFilterForm();
		$.get(logid + '$listFilters', { filter : logSelectedFilter }, function(data) {
			var b = f.find(".body .content");
			b.html("");
			// TODO
		}, "json");
	}
}

function logReload() {
	$.get(logid + '$logOnly', { filter : logSelectedFilter }, function(data) {
		$("#log-placeholder").html(data);
	});
}

function logNewFilterGroup() {
	$("#new-filter-dlg .tip")
		.html("Please give a non-empty name to this filter. Allowed characters are: a-z, A-Z, 0-9!")
		.removeClass("ui-state-error");
	$("#new-filter-dlg .name").val("");
	var dlg = $("#new-filter-dlg");
	dlg.dialog({
		modal: true,
		position: "top",
		buttons: {
			"Create new filter" : function() {
				var name = $("#new-filter-dlg .name");
				$.get(logid + '$newFilterGroup', { name : name.val() }, function(data) {
					if (data.err == 200) {
						dlg.dialog("close");
						logUpdateFilterGroups();
					} else {
						$("#new-filter-dlg .tip").html(data.msg).addClass("ui-state-error");
					}
				}, "json");
			},
			Cancel: function() {
				//$(this).dialog("close");
				dlg.dialog("close");
			}
		}
	});
}

function logResetNewFilterForm() {
	var f = $("#log-filter");
	f.find("input[name=tag]").val("");
	f.find("input[name=msg]").val("");
	f.find("input[name=line]").val("");
	f.find(".tip").html("").removeClass("ui-state-error");
}

function logNewFilter() {
	var f = $("#log-filter");
	var opts = {
		tag: f.find("input[name=tag]").val(),
		msg: f.find("input[name=msg]").val(),
		line: f.find("input[name=line]").val(),
		action: f.find("select[name=action]").val(),
		filter : logSelectedFilter
	};
	$.get(logid + '$newFilter', opts, function(data) {
		if (data.err == 200) {
			logResetNewFilterForm();
			logUpdateFilters();
		} else {
			f.find(".tip").html(data.msg).addClass("ui-state-error");
		}
	}, "json");
}

function logFilterGroupSelected() {
	logSelectedFilter = $("#filter").val();
	logUpdateFilters();
	logReload();
}

function logInitAddNewFilter() {
	var d = $("#log-filter .add-new");
	d.append('<div>Match log tag: <input name="tag" /></div>');
	d.append('<div>Match log message: <input name="msg" /></div>');
	d.append('<div>Match whole line: <input name="line" /></div>');
	d.append('<div>Action: <select name="action"><option value="HIDE">Hide matched line</option><option value="SHOW">Show matched line</option></select></div>');
	d.append('<div><button onClick="javascript:logNewFilter()">Add new filter</button></div>')
	d.append('<div class="tip"></div>')
}

function logMain() {
	logUpdateFilterGroups();
	logUpdateFilters();
	logInitAddNewFilter();
	$("#filter").change(logFilterGroupSelected);
	logReload();
}

$(document).ready(logMain);
