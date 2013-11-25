/* This JavaScript file is included only when using the built-in web server */
var isWebServer = true;

/* Hook into the table of contents */
function hook_toc() {
	$(".toc ul").append("<li><a href=\"/self$hello\" target=\"content\">Hello World</a></li>");
}

$(document).ready(function() {
	$(".ws").show();
	// hook_toc();
});
