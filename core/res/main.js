/*
 * Copyright (C) 2011 Sony Ericsson Mobile Communications AB
 * Copyright (C) 2012-2013 Sony Mobile Communications AB
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

// Handle hover events in a flot chart, based on flot examples
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

/**
 * Toggle line wrapping in the log
 */
function ltbToggleLineWrap() {
    var log = $(".log");
    if ("nowrap" != log.css("white-space")) {
        log.css("white-space", "nowrap");
    } else {
        log.css("white-space", "normal");
    }
}

function main() {
	// First of all, if the webserver is not running, removing everything tagged with "ws"
	if (typeof isWebServer === 'undefined' || !isWebServer) {
		$("li > .ws").parent().remove();
		$(".ws").remove();
	}

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
	$(".auto-accordion").accordion({ heightStyle: "content" });
	$(".auto-accordion-collapse").accordion({ heightStyle: "content", collapsible: true });
	$(".auto-accordion-sort").accordion({ heightStyle: "content" }).sortable();
	$(".auto-accordion-collapse-sort").accordion({ heightStyle: "content", collapsible: true }).sortable();
	$(".auto-sortable-handle-only").sortable({ handle: ".auto-sortable-handle" });
	$(".auto-sortable").sortable();
	$(".auto-collapsible").addClass("ui-accordion ui-widget ui-helper-reset")
        .find(".auto-collapsible-header")
            .addClass("ui-accordion-header ui-helper-reset ui-state-default ui-accordion-header-active ui-state-active ui-corner-top ui-accordion-icons")
            .prepend("<span class='ui-accordion-header-icon ui-icon ui-icon-triangle-1-s'></span>")
            .end()
        .find(".auto-collapsible-content")
			.addClass("ui-helper-reset ui-widget-content ui-corner-bottom ui-accordion-content-active")
	$(".auto-collapsible-header").click(function() {
		$(this).find(".ui-icon").toggleClass("ui-icon-triangle-1-e").toggleClass("ui-icon-triangle-1-s");
		$(this).parents(".auto-collapsible:first").find(".auto-collapsible-content" ).toggle();
		$(this).parents(".auto-collapsible:first").find(".auto-collapsible-header").toggleClass("ui-accordion-header-active").toggleClass("ui-state-active");
	});
}

$(document).ready(main);
