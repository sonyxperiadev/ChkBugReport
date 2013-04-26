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

function main() {
	update_filters();
	$("#filter").change(function() {
		selected_filter = $("#filter").val();
		reload_log();
	});
	reload_log();
}

$(document).ready(main);
