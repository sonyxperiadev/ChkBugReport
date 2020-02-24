import com.sonyericsson.chkbugreport.BugReportModule;
import com.sonyericsson.chkbugreport.Context;
import com.sonyericsson.chkbugreport.Section;
import com.sonyericsson.chkbugreport.plugins.stacktrace.*;
import com.sonyericsson.chkbugreport.plugins.stacktrace.Process;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertEquals;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StackTraceScannerTest {

        StackTraceScanner spySut;
        BugReportModule mockBugReport;
        TestSection fakeVMTraceJustNow;

        @Before
        public void setup() {
            StackTracePlugin fakeStackTracePlugin = spy(StackTracePlugin.class);
            StackTraceScanner sut = new StackTraceScanner(fakeStackTracePlugin);
            Context mockContext = mock(Context.class);
            spySut = spy(sut);
            mockBugReport = mock(BugReportModule.class);
            when(mockBugReport.getContext()).thenReturn(mockContext);
            fakeVMTraceJustNow = new TestSection(mockBugReport, Section.VM_TRACES_JUST_NOW);
            fakeVMTraceJustNow.clear();
            when(mockBugReport.findSection(Section.VM_TRACES_JUST_NOW)).thenReturn(fakeVMTraceJustNow);
        }



    @Test
        public void instantiates() {
            assertNotEquals(null, spySut);
        }

        @Test
        public void parsesStacktraceOfPC() {
            final String VM_TRACES_NOW_DATA = "----- pid 123 at 2020-01-16 14:18:55 -----\n" +
                    "Cmd line: /system/bin/vold\n" +
                    "ABI: 'arm64'\n" +
                    "\n" +
                    "\"Binder:595_2\" sysTid=595\n" +
                    "    #00 pc 00000000000d1404  /apex/com.android.runtime/lib64/bionic/libc.so (__ioctl+4) (BuildId: 4ac85d8f66f3a910f00f4ebf4d6bcd1a)\n" +
                    "    #01 pc 000000000008ba28  /apex/com.android.runtime/lib64/bionic/libc.so (ioctl+132) (BuildId: 4ac85d8f66f3a910f00f4ebf4d6bcd1a)\n" +
                    "    #02 pc 0000000000058d14  /system/lib64/libbinder.so (android::IPCThreadState::talkWithDriver(bool)+244) (BuildId: aa80b3412b3940b77644711e1b0057cd)\n" +
                    "    #03 pc 0000000000058ef0  /system/lib64/libbinder.so (android::IPCThreadState::getAndExecuteCommand()+24) (BuildId: aa80b3412b3940b77644711e1b0057cd)\n" +
                    "    #04 pc 00000000000596c8  /system/lib64/libbinder.so (android::IPCThreadState::joinThreadPool(bool)+64) (BuildId: aa80b3412b3940b77644711e1b0057cd)\n" +
                    "    #05 pc 0000000000028a7c  /system/bin/vold (main+2604) (BuildId: 51c60340a0443417335be50c68cef511)\n" +
                    "    #06 pc 000000000007e898  /apex/com.android.runtime/lib64/bionic/libc.so (__libc_init+108) (BuildId: 4ac85d8f66f3a910f00f4ebf4d6bcd1a)";

            fakeVMTraceJustNow.setTestLines(VM_TRACES_NOW_DATA);
            Processes result = spySut.scan(mockBugReport, 0, fakeVMTraceJustNow, "test");

            Process process = result.findPid(123);
            assertNotNull(process);

            StackTrace trace = process.findTid(595);
            assertNotNull(trace);

            assertEquals(7, trace.getCount());

            assertEquals(Long.parseLong("00000000000d1404", 16), trace.get(0).getPC());
            assertEquals("/apex/com.android.runtime/lib64/bionic/libc.so", trace.get(0).getFileName());
            assertEquals("__ioctl", trace.get(0).getMethod());
            assertEquals(4, trace.get(0).getMethodOffset());

            assertEquals(Long.parseLong("00000000000596c8", 16), trace.get(4).getPC());
            assertEquals("/system/lib64/libbinder.so", trace.get(4).getFileName());
            assertEquals("android::IPCThreadState::joinThreadPool(bool)", trace.get(4).getMethod());
            assertEquals(64, trace.get(4).getMethodOffset());
        }


    @Test
    public void parsesStacktraceOfPCWithoutMethod() {
        final String VM_TRACES_NOW_DATA = "----- pid 123 at 2020-01-16 14:18:55 -----\n" +
                "Cmd line: /system/bin/vold\n" +
                "ABI: 'arm64'\n" +
                "\n" +
                "\"provider@1.0-se\" sysTid=1315\n" +
                "    #00 pc 0005ba34  /apex/com.android.runtime/lib/bionic/libc.so (syscall+28) (BuildId: d1fecf2d89af3c283776c99c1e5a0df2)\n" +
                "    #01 pc 0003853c  /vendor/lib/libsymphony-cpu.so (BuildId: ea7604c242e0ab77e8e999724e087573029569bd)\n" +
                "    #02 pc 000380c0  /vendor/lib/libsymphony-cpu.so (BuildId: ea7604c242e0ab77e8e999724e087573029569bd)";

        fakeVMTraceJustNow.setTestLines(VM_TRACES_NOW_DATA);
        Processes result = spySut.scan(mockBugReport, 0, fakeVMTraceJustNow, "test");

        Process process = result.findPid(123);
        assertNotNull(process);

        StackTrace trace = process.findTid(1315);
        assertNotNull(trace);

        assertEquals(3, trace.getCount());

        assertEquals(Long.parseLong("0005ba34", 16), trace.get(0).getPC());
        assertEquals("/apex/com.android.runtime/lib/bionic/libc.so", trace.get(0).getFileName());
        assertEquals("syscall", trace.get(0).getMethod());
        assertEquals(28, trace.get(0).getMethodOffset());

        assertEquals(Long.parseLong("0003853c", 16), trace.get(1).getPC());
        assertEquals("/vendor/lib/libsymphony-cpu.so", trace.get(1).getFileName());
        assertEquals(null, trace.get(1).getMethod());
        assertEquals(-1, trace.get(1).getMethodOffset()); // -1 is unknown
    }

    @Test
    public void parsesStacktraceOfPCWithAnonymous() {
        final String VM_TRACES_NOW_DATA = "----- pid 123 at 2020-01-16 14:18:55 -----\n" +
                "Cmd line: /system/bin/vold\n" +
                "ABI: 'arm64'\n" +
                "\n" +
                "\"drmserver\" sysTid=1332\n" +
                "    #00 pc 0009a564  /apex/com.android.runtime/lib/bionic/libc.so (__ioctl+8) (BuildId: d1fecf2d89af3c283776c99c1e5a0df2)\n" +
                "    #01 pc 0006602d  /apex/com.android.runtime/lib/bionic/libc.so (ioctl+28) (BuildId: d1fecf2d89af3c283776c99c1e5a0df2)\n" +
                "    #02 pc 0003b037  /system/lib/libbinder.so (android::IPCThreadState::talkWithDriver(bool)+206) (BuildId: 758775d771244cf7fd11c20197770c6b)\n" +
                "    #03 pc 0003b18d  /system/lib/libbinder.so (android::IPCThreadState::getAndExecuteCommand()+8) (BuildId: 758775d771244cf7fd11c20197770c6b)\n" +
                "    #04 pc 0003b77f  /system/lib/libbinder.so (android::IPCThreadState::joinThreadPool(bool)+38) (BuildId: 758775d771244cf7fd11c20197770c6b)\n" +
                "    #05 pc 0000408f  /system/bin/drmserver (main+74) (BuildId: 664e0fa664fb9d4d3395121e7a146f00)\n" +
                "    #06 pc 0005ab61  /apex/com.android.runtime/lib/bionic/libc.so (__libc_init+68) (BuildId: d1fecf2d89af3c283776c99c1e5a0df2)\n" +
                "    #07 pc 0000402f  /system/bin/drmserver (_start_main+38) (BuildId: 664e0fa664fb9d4d3395121e7a146f00)\n" +
                "    #08 pc 00004456  <anonymous:f5f08000>";

        fakeVMTraceJustNow.setTestLines(VM_TRACES_NOW_DATA);
        Processes result = spySut.scan(mockBugReport, 0, fakeVMTraceJustNow, "test");

        Process process = result.findPid(123);
        assertNotNull(process);

        StackTrace trace = process.findTid(1332);
        assertNotNull(trace);

        assertEquals(9, trace.getCount());

        assertEquals(Long.parseLong("0009a564", 16), trace.get(0).getPC());
        assertEquals("/apex/com.android.runtime/lib/bionic/libc.so", trace.get(0).getFileName());
        assertEquals("__ioctl", trace.get(0).getMethod());
        assertEquals(8, trace.get(0).getMethodOffset());

        assertEquals(Long.parseLong("00004456", 16), trace.get(8).getPC());
        assertEquals("anonymous:f5f08000", trace.get(8).getFileName());
        assertEquals(null, trace.get(8).getMethod());
        assertEquals(-1, trace.get(8).getMethodOffset()); // -1 is unknown
    }

    @Test
    public void parsesStacktraceOfPCWithNative() {
        final String VM_TRACES_NOW_DATA = "----- pid 123 at 2020-01-16 14:18:55 -----\n" +
                "Cmd line: /system/bin/vold\n" +
                "ABI: 'arm64'\n" +
                "\n" +
                "DALVIK THREADS (23):\n" +
                "\"Signal Catcher\" daemon prio=5 tid=7 Runnable\n" +
                "  | group=\"system\" sCount=0 dsCount=0 flags=0 obj=0x13580228 self=0x741052c400\n" +
                "  | sysTid=7094 nice=0 cgrp=default sched=0/0 handle=0x7416244d50\n" +
                "  | state=R schedstat=( 34420882 4547760 7 ) utm=2 stm=1 core=2 HZ=100\n" +
                "  | stack=0x741614e000-0x7416150000 stackSize=991KB\n" +
                "  | held mutexes= \"mutator lock\"(shared held)\n" +
                "  native: #00 pc 00000000004118ec  /apex/com.android.runtime/lib64/libart.so (art::DumpNativeStack(std::__1::basic_ostream<char, std::__1::char_traits<char>>&, int, BacktraceMap*, char const*, art::ArtMethod*, void*, bool)+140)\n" +
                "  (no managed stack frames)";

        fakeVMTraceJustNow.setTestLines(VM_TRACES_NOW_DATA);
        Processes result = spySut.scan(mockBugReport, 0, fakeVMTraceJustNow, "test");

        Process process = result.findPid(123);
        assertNotNull(process);

        StackTrace trace = process.findTid(7);
        assertNotNull(trace);

        assertEquals(1, trace.getCount());

        assertEquals(Long.parseLong("00000000004118ec", 16), trace.get(0).getPC());
        assertEquals("/apex/com.android.runtime/lib64/libart.so", trace.get(0).getFileName());
        assertEquals("art::DumpNativeStack(std::__1::basic_ostream<char, std::__1::char_traits<char>>&, int, BacktraceMap*, char const*, art::ArtMethod*, void*, bool)", trace.get(0).getMethod());
        assertEquals(140, trace.get(0).getMethodOffset());
    }

    @Test
    public void parsesStacktraceOfPCWithOffset() {
        final String VM_TRACES_NOW_DATA = "----- pid 123 at 2020-01-16 14:18:55 -----\n" +
                "Cmd line: /system/bin/vold\n" +
                "ABI: 'arm64'\n" +
                "\n" +
                "\"ChromiumNet\" prio=5 tid=52 Native\n" +
                "  | group=\"main\" sCount=1 dsCount=0 flags=1 obj=0x154c3790 self=0x73b38d7000\n" +
                "  | sysTid=16601 nice=-2 cgrp=default sched=0/0 handle=0x738bff6d50\n" +
                "  | state=S schedstat=( 5138957 4233386 18 ) utm=0 stm=0 core=0 HZ=100\n" +
                "  | stack=0x738bf00000-0x738bf02000 stackSize=991KB\n" +
                "  | held mutexes=\n" +
                "  kernel: (couldn't read /proc/self/task/16601/stack)\n" +
                "  native: #00 pc 00000000000d12c8  /apex/com.android.runtime/lib64/bionic/libc.so (__epoll_pwait+8)\n" +
                "  native: #01 pc 000000000035a18c  /system/product/priv-app/Velvet/Velvet.apk!libcronet.80.0.3955.6.so (offset 64e4000) (???)";

        fakeVMTraceJustNow.setTestLines(VM_TRACES_NOW_DATA);
        Processes result = spySut.scan(mockBugReport, 0, fakeVMTraceJustNow, "test");

        Process process = result.findPid(123);
        assertNotNull(process);

        StackTrace trace = process.findTid(52);
        assertNotNull(trace);

        assertEquals(2, trace.getCount());

        assertEquals(Long.parseLong("000000000035a18c", 16), trace.get(1).getPC());
        assertEquals("/system/product/priv-app/Velvet/Velvet.apk!libcronet.80.0.3955.6.so", trace.get(1).getFileName());
        assertEquals(null, trace.get(1).getMethod());
        assertEquals(Long.parseLong("64e4000", 16), trace.get(1).getOffset());
    }


    @Test
    public void getsRawLineForUnparsable() {
        final String VM_TRACES_NOW_DATA = "----- pid 123 at 2020-01-16 14:18:55 -----\n" +
                "Cmd line: /system/bin/vold\n" +
                "ABI: 'arm64'\n" +
                "\n" +
                "\"GlobalScheduler\" prio=5 tid=21 TimedWaiting\n" +
                "  | group=\"main\" sCount=1 dsCount=0 flags=1 obj=0x150c0990 self=0x741059ac00\n" +
                "  | sysTid=8241 nice=0 cgrp=default sched=0/0 handle=0x73af941d50\n" +
                "  | state=S schedstat=( 52551098 82222229 510 ) utm=4 stm=1 core=3 HZ=100\n" +
                "  | stack=0x73af83f000-0x73af841000 stackSize=1039KB\n" +
                "  | held mutexes=\n" +
                "  at sun.misc.Unsafe.park(Native method)\n" +
                "  - waiting on an unknown object\n" +
                "  at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:230)\n" +
                "  at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.awaitNanos(AbstractQueuedSynchronizer.java:2109)\n" +
                "  at java.util.concurrent.ScheduledThreadPoolExecutor$DelayedWorkQueue.take(ScheduledThreadPoolExecutor.java:1132)\n" +
                "  at java.util.concurrent.ScheduledThreadPoolExecutor$DelayedWorkQueue.take(ScheduledThreadPoolExecutor.java:849)\n" +
                "  at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1092)\n" +
                "  at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1152)\n" +
                "  at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)\n" +
                "  at tcn.run(:com.google.android.gms@19629039@19.6.29 (120408-278422107):-1)\n" +
                "  at java.lang.Thread.run(Thread.java:919)";

        fakeVMTraceJustNow.setTestLines(VM_TRACES_NOW_DATA);
        Processes result = spySut.scan(mockBugReport, 0, fakeVMTraceJustNow, "test");

        Process process = result.findPid(123);
        assertNotNull(process);

        StackTrace trace = process.findTid(21);
        assertNotNull(trace);

        assertEquals(11, trace.getCount());

        assertEquals(-1, trace.get(9).getPC());
        assertEquals("tcn.run(:com.google.android.gms@19629039@19.6.29 (120408-278422107):-1)", trace.get(9).getRaw());
        assertEquals(null, trace.get(9).getMethod());
        assertEquals(-1, trace.get(9).getMethodOffset());
        assertEquals(-1, trace.get(9).getOffset());
        assertEquals(null, trace.get(9).getFileName());
    }
}
