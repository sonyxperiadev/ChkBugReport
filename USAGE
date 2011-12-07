How to use it
=============

Just run the tool passing the bugreport as parameter. I created a wrapper script which is places
in $HOME/bin so I can start it anywhere:

 $ chkbugreport thebugreport.txt

But of course you can use the jar file as well:

 $ java -jar path/to/chkbugreport.jar thebugreport.txt

The bugreport can be compressed (as .gz or in a zip file) or plain text file. The tool will create
a folder (with the input file's name and the "_out" suffix appended) and create all files under that
folder (and some subfolders). You should open the "index.html" file from this folder (Chrome or
Firefox highly suggested).

Note that chkbugreport has an extra functionality: it can process profiling data as well created for
traceview. The result will be similar: a folder with a bunch of html and image files. To use it with
tracefiles add the "-t" option on the command line:

 $ chkbugreport -t something.prof

You can generate this profiling data like this:

* Use "adb shell ps" to list all the process and find the PID of the process you want to trace
* Execute "adb shell am profile PID start /data/profile.dat" to start profiling (replace PID with the
  process ID number)
* Do the testing
* Execute "adb shell am profile PID stop" to stop profiling (replace PID with the process ID number)
* Pull out the file using "adb pull /data/profile.dat" and remove it from the phone (to save precious
  disk space): "adb shell rm /data/profile.dat"
* Create the report "chkbugreport -t profile.dat"

Also if you have only pieces of a bugreport (for example logs or stack traces), you can still use this
tool. Suppose you have a system log and a file containing the process stacktraces (taken from
/data/anr/traces.txt for example), you can use the tool like this:

 $ chkbugreport -sl:the_system_log.txt -sa:traces.txt dummy

This will generate the output in the folder called "dummy_out" (it assumes "dummy" to be the name of
the non-existing bugreport).

For a complete list of parameters just run the tool without any arguments. As of now the following
parameters are handled:

  -ds:file    - Use file as dumsys output (almost same as -pb)
  -el:file    - Use file as event log
  -ft:file    - Use file as ftrace dump
  -ps:file    - Use file as "processes" section
  -pt:file    - Use file as "processes and threads" section
  -ml:file    - Use file as main log
  -mo:file    - Parse monkey output and extract stacktraces from it
  -pb:file    - Load partial bugreport (eg. output of dumpsys)
  -sl:file    - Use file as system log
  -sa:file    - Use file as "vm traces at last anr" section
  -sn:file    - Use file as "vm traces just now" section

Extra options (less frequently needed):

  --frames    - Use HTML frames when processing bugreport (default)
  --no-frames - Don't use HTML frames when processing bugreport
  --silent    - Supress all output except fatal errors
