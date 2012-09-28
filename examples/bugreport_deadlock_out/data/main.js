/*
 Main Javascript code 
 */

function isdefined(variable) {
    return (typeof(window[variable]) == "undefined")?  false: true;
}

function onTraceViewTreeLineClicked() {
	var id = $(this).attr("id");
	$("#" + id + "_c").toggle();
}

function tvtrShow(id) {
	$("#" + id + " .tv_tr_c").show();
}

function tvtrHide(id) {
	$("#" + id + " .tv_tr_c").hide();
}

function hideStackTrace() {
	var items = $(this).parent().next();
	var child = $(this).parent().children().first();
	child.html("+");
	items.hide();
}

function showStackTrace() {
	var items = $(this).parent().next();
	var child = $(this).parent().children().first();
	child.html("-");
	items.show();
}

function main() {
	// Check if there is a newer version
	if (isdefined("chkbugreport_latest_ver")) {
		var rel = $("#chkbugreport-rel").text();
		if (rel < chkbugreport_latest_ver) {
			var sa = "<a href=\"http://seldlx0381/space/chkbugreport\">";
			var ea = "</a>";
			$("#new-version").html(
					"A newer release of " + sa + "ChkBugReport" + ea + " has been released, " +
					"download the latest version from " + sa + "here" + ea + "!");
		}
	}
	$(".toc-tree").jstree({
        "themes" : {
            "theme" : "classic",
            "dots" : false,
            "icons" : false
        },
        "plugins" : [ "themes", "html_data" ]
    });
	$(".tree").jstree({
		"themes" : {
			"theme" : "classic",
			"dots" : false,
			"icons" : false
		},
		"plugins" : [ "themes", "html_data" ]
	});
	$(".treeTable").treeTable();
	$(".tablesorter").tablesorter();
	$(".colResizable").colResizable();
	$(".tablednd").tableDnD();
	$(".tv_tr").click(onTraceViewTreeLineClicked);
	$(".stacktrace-name-name").toggle(hideStackTrace, showStackTrace);
}

$(document).ready(main);
