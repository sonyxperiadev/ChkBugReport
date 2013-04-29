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
var selected_filter = "(none)";

function update_filters() {
	$.get(logid + '$listFilters', function(data) {
		$("#filter").html("");
		$("#filter").append('<option value="(none)">No filter</option>');
		for (i = 0; i < data.filters.length; i++) {
			$("#filter").append('<option value="' + data.filters[i] + '">' + data.filters[i] + '</option>');
		}
	}, "json");
}

function reload_log() {
	$.get(logid + '$logOnly', { filter : selected_filter }, function(data) {
		$("#log-placeholder").html(data);
	});
}

function log_new_filter() {
	$("#new-filter-dlg .tip")
		.html("Please give a non-empty name to this filter. Allowed characters are: a-z, A-Z, 0-9!")
		.removeClass("ui-state-error");
	$("#new-filter-dlg .name").val("");
	var dlg = $("#new-filter-dlg");
	dlg.dialog({
		modal: true,
		buttons: {
			"Create new filter" : function() {
				var name = $("#new-filter-dlg .name");
				$.get(logid + '$newFilter', { name : name.val() }, function(data) {
					if (data.err == 200) {
						dlg.dialog("close");
						update_filters();
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

function main() {
	update_filters();
	$("#filter").change(function() {
		selected_filter = $("#filter").val();
		reload_log();
	});
	reload_log();
}

$(document).ready(main);
