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
/** The index of the filter being edited, or -1 if it's not in edit mode */
var logEditFilterId = -1;
/** Cached value of the filters */
var logCachedFilters = null;

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
		logUpdateFilterButtons();
		$.get(logid + '$listFilters', { filter : logSelectedFilter }, function(data) {
			logCachedFilters = data;
			var b = f.find(".body").find(".content");
			b.html("");
			for (i = 0; i < data.filters.length; i++) {
				var filter = data.filters[i];
				var html = '<div class="filter-row">';
				html += '<a class="action-delete" href="javascript:logDeleteFilter(' + filter.id + ')">Delete</a>';
				html += '<a class="action-edit" href="javascript:logEditFilter(' + filter.id + ')">Edit</a>';
				html += 'If';
				var filterFound = false;
				if (filter.tag) {
					html += ' <span class="cond">tag matches <code>' + filter.tag + '<code></span>';
					filterFound = true;
				}
				if (filter.msg) {
					if (filterFound) html += ' and';
					html += ' <span class="cond">message matches <code>' + filter.msg + '<code></span>'
					filterFound = true;
				}
				if (filter.line) {
					if (filterFound) html += ' and';
					html += ' <span class="cond">line matches <code>' + filter.line + '<code></span>';
					filterFound = true;
				}
				html += ' then <span class="action">';
				if (filter.action == 'HIDE') {
					html += 'hide log line';
				} else if (filter.action == 'SHOW') {
					html += 'show log line';
				} else {
					html += '???';
				}
				html += '</span>'
				b.append(html);
			}
		}, "json");
	}
}

function logReload() {
	$("#log-placeholder").html("... loading ...");
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

function logUpdateFilterButtons() {
	var f = $("#log-filter");
	if (logEditFilterId < 0) {
		f.find("#btn-add-new").show();
		f.find("#btn-del-grp").show();
		f.find("#btn-edit-row").hide();
		f.find("#btn-cancel").hide();
	} else {
		f.find("#btn-add-new").hide();
		f.find("#btn-del-grp").hide();
		f.find("#btn-edit-row").show();
		f.find("#btn-cancel").show();
	}
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
			logReload();
		} else {
			f.find(".tip").html(data.msg).addClass("ui-state-error");
		}
	}, "json");
}

function logDeleteFilter(id) {
	var dlg = $("#generic-dlg");
	dlg.html("Are you sure you want to delete this filter?");
	dlg.dialog({
		modal: true,
		position: "top",
		buttons: {
			Yes : function() {
				$.get(logid + '$deleteFilter', { filter : logSelectedFilter, id : id }, function(data) {
					logUpdateFilters();
					logReload();
				});
				dlg.dialog("close");
			},
			No: function() {
				dlg.dialog("close");
			}
		}
	});
}

function logEditFilter(id) {
	logEditFilterId = id;
	logUpdateFilterButtons();
	logResetNewFilterForm();
	for (i = 0; i < logCachedFilters.filters.length; i++) {
		var filter = logCachedFilters.filters[i];
		if (filter.id == id) {
			var f = $("#log-filter");
			f.find("input[name=tag]").val(filter.tag);
			f.find("input[name=msg]").val(filter.msg);
			f.find("input[name=line]").val(filter.line);
			f.find("select[name=action]").val(filter.action);
			f.find(".tip").html("").removeClass("ui-state-error");
			break;
		}
	}
}

function logUpdateFilter() {
	var f = $("#log-filter");
	var opts = {
		id: logEditFilterId,
		tag: f.find("input[name=tag]").val(),
		msg: f.find("input[name=msg]").val(),
		line: f.find("input[name=line]").val(),
		action: f.find("select[name=action]").val(),
		filter : logSelectedFilter
	};
	$.get(logid + '$updateFilter', opts, function(data) {
		if (data.err == 200) {
			logEditFilterId = -1;
			logResetNewFilterForm();
			logUpdateFilters();
			logReload();
		} else {
			f.find(".tip").html(data.msg).addClass("ui-state-error");
		}
	}, "json");
}

function logCancelEditFilter() {
	logEditFilterId = id;
	logUpdateFilterButtons();
	logResetNewFilterForm();
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
	d.append('<div>' +
			'<button id="btn-add-new" onClick="javascript:logNewFilter()">Add new filter</button>' +
			'<button id="btn-del-grp" onClick="javascript:logDeleteFilterGroup()">Delete filter group</button>' +
			'<button id="btn-edit-row" onClick="javascript:logUpdateFilter()">Update filter</button>' +
			'<button id="btn-cancel" onClick="javascript:logCancelEditFilter()">Cancel</button>' +
		'</div>')
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
