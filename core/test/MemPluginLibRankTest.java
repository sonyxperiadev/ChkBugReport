import com.sonyericsson.chkbugreport.BugReportModule;
import com.sonyericsson.chkbugreport.Context;
import com.sonyericsson.chkbugreport.ProcessRecord;
import com.sonyericsson.chkbugreport.Section;
import com.sonyericsson.chkbugreport.plugins.MemPlugin;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.*;

public class MemPluginLibRankTest {
        // Data is same except for swap data is added.
        private static final String LIBRANK_DATA_NO_SWAP = " RSStot       VSS      RSS      PSS      USS  Name/PID\n" +
                "309423K                                       [anon:libc_malloc]\n" +
                "          188416K   86440K   84507K   84440K    com.android.systemui [1497]\n" +
                "          166912K   23644K   21700K   21632K    system_server [929]\n" +
                "           76800K   19692K   17706K   17636K    com.android.launcher3 [1878]\n" +
                "           29184K   10120K   10120K   10120K    /vendor/bin/hw/android.hardware.camera.provider@2.4-service [654]\n" +
                "           57344K   11936K    9757K    9676K    com.android.bluetooth [1474]\n" +
                "           52736K    9984K    7887K    7808K    com.accuweather.android [3330]\n" +
                "           62464K    8544K    6406K    6328K    android.process.acore [2105]\n" +
                "           52736K    7856K    5827K    5756K    com.android.phone [1715]";

        private static final String LIBRANK_DATA_WITH_SWAP = " RSStot       VSS      RSS      PSS      USS     Swap  Name/PID\n" +
                "309423K                                                [anon:libc_malloc]\n" +
                "          188416K   86440K   84507K   84440K   16180K    com.android.systemui [1497]\n" +
                "          166912K   23644K   21700K   21632K   20804K    system_server [929]\n" +
                "           76800K   19692K   17706K   17636K   38284K    com.android.launcher3 [1878]\n" +
                "           29184K   10120K   10120K   10120K       0K    /vendor/bin/hw/android.hardware.camera.provider@2.4-service [654]\n" +
                "           57344K   11936K    9757K    9676K    6088K    com.android.bluetooth [1474]\n" +
                "           52736K    9984K    7887K    7808K   17552K    com.accuweather.android [3330]\n" +
                "           62464K    8544K    6406K    6328K   35236K    android.process.acore [2105]\n" +
                "           52736K    7856K    5827K    5756K    8660K    com.android.phone [1715]";

        MemPlugin spySut;
        BugReportModule mockBugReport;
        TestSection fakeLibRankSection;

        HashMap<Integer, ProcessRecord> processRecordMap = new HashMap<Integer, ProcessRecord>();

        @Before
        public void setup() {
            MemPlugin sut = new MemPlugin();
            Context mockContext = mock(Context.class);
            spySut = spy(sut);

            processRecordMap.clear();

            mockBugReport = mock(BugReportModule.class);
            when(mockBugReport.getContext()).thenReturn(mockContext);
            fakeLibRankSection = new TestSection(mockBugReport, Section.LIBRANK);
            when(mockBugReport.findSection(Section.LIBRANK)).thenReturn(fakeLibRankSection);
            when(mockBugReport.getProcessRecord(anyInt(), anyBoolean(), anyBoolean())).thenAnswer(invocation -> {
                int pid = invocation.getArgument(0);

                //Assume we can just make a new PR:
                ProcessRecord pr = new ProcessRecord(mockContext, "", pid);
                processRecordMap.put(pid, pr);

                return pr;
            });
        }

        @Test
        public void instantiates() {
            assertNotEquals(null, spySut);
        }

        @Test
        public void parsesLibRankWithNoSwapData() {
            fakeLibRankSection.setTestLines(LIBRANK_DATA_NO_SWAP);
            spySut.load(mockBugReport);

            //No Errors
            verify(mockBugReport, never()).printErr(anyInt(), contains("librank"));

            spySut.generate(mockBugReport);
            assertEquals(8, processRecordMap.size());

            //Check names match pids:
            assertEquals("com.android.systemui", processRecordMap.get(1497).getProcName());
            assertEquals("system_server", processRecordMap.get(929).getProcName());
            assertEquals("com.android.launcher3", processRecordMap.get(1878).getProcName());
            assertEquals("/vendor/bin/hw/android.hardware.camera.provider@2.4-service", processRecordMap.get(654).getProcName());
            assertEquals("com.android.bluetooth", processRecordMap.get(1474).getProcName());
            assertEquals("com.accuweather.android", processRecordMap.get(3330).getProcName());
            assertEquals("android.process.acore", processRecordMap.get(2105).getProcName());
            assertEquals("com.android.phone", processRecordMap.get(1715).getProcName());
        }

    @Test
    public void parsesLibRankWithSwapData() {
        fakeLibRankSection.setTestLines(LIBRANK_DATA_WITH_SWAP);
        spySut.load(mockBugReport);

        //No Errors
        verify(mockBugReport, never()).printErr(anyInt(), contains("librank"));

        spySut.generate(mockBugReport);
        assertEquals(8, processRecordMap.size());

        //Check names match pids:
        assertEquals("com.android.systemui", processRecordMap.get(1497).getProcName());
        assertEquals("system_server", processRecordMap.get(929).getProcName());
        assertEquals("com.android.launcher3", processRecordMap.get(1878).getProcName());
        assertEquals("/vendor/bin/hw/android.hardware.camera.provider@2.4-service", processRecordMap.get(654).getProcName());
        assertEquals("com.android.bluetooth", processRecordMap.get(1474).getProcName());
        assertEquals("com.accuweather.android", processRecordMap.get(3330).getProcName());
        assertEquals("android.process.acore", processRecordMap.get(2105).getProcName());
        assertEquals("com.android.phone", processRecordMap.get(1715).getProcName());
    }
}
