The purpose of the ChkBugReport tool is to process the bugreport files generated on the phone, and
extract as much useful data as possible.

The generated report is in HTML format (or more exactly, a collection of HTML, text and image files).
The main reason is, it is much easier to create HTML files (and images and include them in the document).
Also HTML gives us the possibility to use javascript, and hence to have a dynamic report (for example
some tables in the generated report can be sorted by clicking on the column headers, in some other
cases the table rows can be reordered by dragging the rows, etc). Some other files are also produced
in the "raw" sub-folder. These files usually can be analyzed/opened by other tools (not necessarily the
browser). For example each section from the bugreport will be saved here. Also some VCD files
(containing some timelines/charts) could be saved here, which can be opened with GtkWave.

ChkBugReport uses some very cool, opensource javascript libraries to make the reports more dynamic:
 * jQuery (http://jquery.com/)
 * jQuery UI (http://jqueryui.com/)
 * jsTree jQuery plugin (http://www.jstree.com/)
 * tablednd jQuery plugin (http://code.google.com/p/tablednd/)
 * tablesorter jQuery plugin (http://tablesorter.com/docs/)
 * js-hotkeys (http://code.google.com/p/js-hotkeys/)
 * jquery-cookie (https://github.com/carhartl/jquery-cookie)
 * Flot charts (www.flotcharts.org)

The latest version of ChkBugReport can be found at:

  https://github.com/sonyxperiadev/ChkBugReport

If you find some information to be incorrect (broken link, wrong info, false error, etc), feel free
to contact me: Pal Szasz (pal.szasz@sonymobile.com).
