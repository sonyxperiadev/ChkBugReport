/*
 Main Javascript code 
 */

// Show a simple tooltip, based on flot examples
function showTooltip(x, y, contents) {
    $('<div id="tooltip">' + contents + '</div>').css( {
        position: 'absolute',
        display: 'none',
        top: y + 5,
        left: x + 5,
        border: '1px solid #fdd',
        padding: '2px',
        'background-color': '#fee',
        opacity: 0.80
    }).appendTo("body").fadeIn(200);
}

// Hide the tooltip, based on flot examples
function hideTooltip() {
    $("#tooltip").remove();
}

// Hande hover events in a flot chart, based on flot examples
var flotHoverPrev = null;
function flotHover(plot,event, pos, item) {
    if (item) {
        if (flotHoverPrev != item.dataIndex) {
            flotHoverPrev = item.dataIndex;
            hideTooltip();
            var x = parseInt(item.datapoint[0].toFixed(2));
            var y = item.datapoint[1].toFixed(2);
            var xaxis = plot.getXAxes()[0];
            var t = xaxis.tickFormatter(x, xaxis);
            showTooltip(item.pageX, item.pageY, item.series.label + " @" + t + " = " + y);
        }
    } else {
        hideTooltip();
        flotHoverPrev = null;
    }
}

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

/**
 * Process log lines executing a callback functions on each line which matches
 * a regular expression. The pattern is read from the log toolbar.
 * @param func The callback function, whose first argument will be the matched
 * line wrapped into a jQuery object.
 */
function ltbProcessLines(func) {
    var regexp = new RegExp($("#regexp").val());
    $(".log").children().each(function (i) {
        if ($(this).text().match(regexp)) {
            func($(this));
        }
    });
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
