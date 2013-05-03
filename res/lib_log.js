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

/** The ID of the log row where the user is currently adding a comment (-1 = not adding comment) */
var logAddCommentTo = -1;

function logUpdateFilterGroups(cb) {
	$.get(logid + '$listFilterGroups', function(data) {
		$("#filter").html("");
		$("#filter").append('<option value="(none)">No filter</option>');
		for (var i = 0; i < data.filters.length; i++) {
			$("#filter").append('<option value="' + data.filters[i] + '">' + data.filters[i] + '</option>');
		}
		if (cb) { cb(); }
	}, "json");
}

function logUpdateFilters(cb) {
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
			for (var i = 0; i < data.filters.length; i++) {
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
			if (cb) { cb(); }
		}, "json");
	}
}

function logReload() {
	$("#log-placeholder").html("... loading ...");
	$.get(logid + '$logOnly', { filter : logSelectedFilter }, function(data) {
		$("#log-placeholder").html(data);
		logInstallHover($(".log-dynamic > .log-line, .log-dynamic > .log-comment"));
		logInstallCommentHover($(".log-dynamic > div.log-comment"));
	});
}

function logSelectFilterGroup(name) {
	$("#filter").val(name);
	logFilterGroupSelected();
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
				var name = $("#new-filter-dlg .name").val();
				$.get(logid + '$newFilterGroup', { name : name }, function(data) {
					if (data.err == 200) {
						dlg.dialog("close");
						logUpdateFilterGroups(function() {
							logSelectFilterGroup(name);
						});
					} else {
						$("#new-filter-dlg .tip").html(data.msg).addClass("ui-state-error");
					}
				}, "json");
			},
			Cancel: function() {
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
	for (var i = 0; i < logCachedFilters.filters.length; i++) {
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

function logDeleteFilterGroup() {
	var dlg = $("#generic-dlg");
	dlg.html("Are you sure you want to delete this filter group? It will remove all filters from it!");
	dlg.dialog({
		modal: true,
		position: "top",
		buttons: {
			Yes : function() {
				$.get(logid + '$deleteFilterGroup', { filter : logSelectedFilter }, function(data) {
					logSelectedFilter = "(none)";
					logUpdateFilterGroups();
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

function logAddComment(row) {
	// Remove hover item
	row.find('.log-row-btn').remove();
	row.removeClass("log-hover");
	// enter log edit mode
	var id = row.attr('id');
	logAddCommentTo = id;
	row.after('<div class="log-edit-comment"><textarea></textarea><div class="tip"/><button class="btn-add-comment">Save</buton><button class="btn-cancel-comment">Cancel</buton></div>')
	var edit = $(".log-edit-comment");
	edit.find(".btn-add-comment").click(function(){
		var comment = edit.find("textarea").val();
		var opts = {
				id : id,
				comment : comment
		}
		$.get(logid + '$addComment', opts, function(data) {
			if (data.err == 200) {
				var commentObj = $('<div class="log-comment" id="' + id + ',' + data.id + '">' + comment + '</div>');
				edit.replaceWith(commentObj)
				logInstallHover(commentObj);
				logInstallCommentHover(commentObj);
				logAddCommentTo = -1;
			} else {
				edit.find(".tip").html(data.msg).addClass("ui-state-error");
			}
		}, "json");
	});
	edit.find(".btn-cancel-comment").click(function(){
		edit.remove();
		logAddCommentTo = -1;
	});
}

function logEditComment(row) {
	// Remove hover item
	row.find('.log-row-btn').remove();
	row.removeClass("log-hover");
	var origComment = row.html();
	// enter log edit mode
	var id = row.attr('id');
	logAddCommentTo = id;
	row.after('<div class="log-edit-comment"><textarea>' + origComment + '</textarea><div class="tip"/><button class="btn-add-comment">Save</buton><button class="btn-cancel-comment">Cancel</buton></div>')
	row.hide();
	var edit = $(".log-edit-comment");
	edit.find(".btn-add-comment").click(function(){
		var comment = edit.find("textarea").val();
		var opts = {
				id : id,
				comment : comment
		}
		$.get(logid + '$updateComment', opts, function(data) {
			if (data.err == 200) {
				var commentObj = $('<div class="log-comment" id="' + id + '">' + comment + '</div>');
				edit.replaceWith(commentObj)
				logInstallHover(commentObj);
				logInstallCommentHover(commentObj);
				logAddCommentTo = -1;
				row.remove();
			} else {
				edit.find(".tip").html(data.msg).addClass("ui-state-error");
			}
		}, "json");
	});
	edit.find(".btn-cancel-comment").click(function(){
		edit.remove();
		row.show();
		logAddCommentTo = -1;
	});
}

function logDeleteComment(row) {
	var dlg = $("#generic-dlg");
	var id = row.attr('id');
	dlg.html("Are you sure you want to delete this comment?");
	dlg.dialog({
		modal: true,
		position: "top",
		buttons: {
			Yes : function() {
				$.get(logid + '$deleteComment', { id : id }, function(data) {
					row.remove();
				});
				dlg.dialog("close");
			},
			No: function() {
				dlg.dialog("close");
			}
		}
	});
}

function logInstallHover(node) {
	node.hover(
			function() {
				if (logAddCommentTo < 0) {
					var row = $(this);
					$(this).prepend('<div class="log-row-btn log-row-btn-comment">Comment</div>');
					$(this).find(".log-row-btn-comment").click(function(){logAddComment(row);});
					$(this).addClass("log-hover");
				}
			},
			function() {
				$(this).find('.log-row-btn').remove();
				$(this).removeClass("log-hover");
			});
}

function logInstallCommentHover(node) {
	node.hover(
			function() {
				if (logAddCommentTo < 0) {
					var row = $(this);
					$(this).prepend('<div class="log-row-btn log-row-btn-edit">Edit</div>');
					$(this).prepend('<div class="log-row-btn log-row-btn-delete">Delete</div>');
					$(this).find(".log-row-btn-edit").click(function(){logEditComment(row);});
					$(this).find(".log-row-btn-delete").click(function(){logDeleteComment(row);});
					$(this).addClass("log-hover");
				}
			},
			function() {
				$(this).find('.log-row-btn').remove();
				$(this).removeClass("log-hover");
			});
}

function logMain() {
	// Setup filters
	logUpdateFilterGroups();
	logUpdateFilters();
	logInitAddNewFilter();
	$("#filter").change(logFilterGroupSelected);
	logReload();
	// Setup comments
}

$(document).ready(logMain);
