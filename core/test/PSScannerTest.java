import com.sonyericsson.chkbugreport.BugReportModule;
import com.sonyericsson.chkbugreport.Context;
import com.sonyericsson.chkbugreport.Section;
import com.sonyericsson.chkbugreport.ps.PSRecord;
import com.sonyericsson.chkbugreport.ps.PSRecords;
import com.sonyericsson.chkbugreport.ps.PSScanner;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.*;

public class PSScannerTest {
    private static final String PS_DATA = "LABEL                          USER           PID   TID  PPID     VSZ    RSS WCHAN            ADDR S PRI  NI RTPRIO SCH PCY     TIME CMD            \n" +
            "u:r:init:s0                    root             1     1     0   73884   4768 0                   0 S  19   0      -   0  fg 00:00:02 init\n" +
            "u:r:kernel:s0                  root             2     2     0       0      0 0                   0 S  19   0      -   0  fg 00:00:00 kthreadd\n" +
            "u:r:kernel:s0                  root             5     5     2       0      0 0                   0 S  19   0      -   0  fg 00:00:02 kworker/u16:0\n" +
            "u:r:kernel:s0                  root             6     6     2       0      0 0                   0 S  19   0      -   0  fg 00:00:01 ksoftirqd/0\n" +
            "u:r:kernel:s0                  root             7     7     2       0      0 0                   0 S  41   0      1   1  fg 00:00:41 rcu_preempt\n" +
            "u:r:priv_app:s0:c512,c768      u0_a217       4064  4064   814 7272312 159480 0                   0 S  29 -10      -   0  ta 00:04:24 earchbox:search\n" +
            "u:r:priv_app:s0:c512,c768      u0_a217       4064  4074   814 7272312 159480 0                   0 S  19   0      -   0  ta 00:00:04 Jit thread pool";

    private static final String PST_DATA = "1       /system/bin/init                 0\n" +
            "2       [kthreadd]                       0\n" +
            "5       [kworker/u16:0]                  0\n" +
            "6       [ksoftirqd/0]                    0\n" +
            "7       [rcu_preempt]                    0\n" +
            "4064    com.google.android.googlequicksearchbox:search 0";

    PSScanner spySut;

    @Before
    public void setup() {
        BugReportModule mockBugReport = mock(BugReportModule.class);

        PSScanner sut = new PSScanner(mockBugReport);
        Context mockContext = mock(Context.class);
        spySut = spy(sut);

        when(mockBugReport.getContext()).thenReturn(mockContext);

        TestSection fakePSSection = new TestSection(mockBugReport, Section.PROCESSES_AND_THREADS);
        fakePSSection.setTestLines(PS_DATA);

        TestSection fakePSTSection = new TestSection(mockBugReport, Section.PROCESSES_TIMES);
        fakePSTSection.setTestLines(PST_DATA);

        when(mockBugReport.findSection(Section.PROCESSES_AND_THREADS)).thenReturn(fakePSSection);
        when(mockBugReport.findSection(Section.PROCESSES_TIMES)).thenReturn(fakePSTSection);
    }

    @Test
    public void instantiates() {
        assertNotEquals(null, spySut);
    }

    @Test
    public void parsesPSData() {
        PSRecords result = spySut.run();
        assertNotEquals(null, result);
        assertEquals(false, result.isEmpty());
        assertNotEquals(null, result.getPSRecord(1));
        assertNotEquals(null, result.getPSRecord(4064));

        PSRecord pid1 = result.getPSRecord(1);
        assertEquals(1, pid1.getPid());
        assertEquals(0, pid1.getParentPid());
        assertEquals("fg", pid1.getPolicyStr());
        assertEquals(0, pid1.getNice());
        assertEquals("/system/bin/init", pid1.getName());

        PSRecord pid4064 = result.getPSRecord(4064);
        assertEquals(4064, pid4064.getPid());
        assertEquals(814, pid4064.getParentPid());
        assertEquals("??", pid4064.getPolicyStr());
        assertEquals(0, pid4064.getNice());
        assertEquals("com.google.android.googlequicksearchbox:search", pid4064.getName());
    }
}
