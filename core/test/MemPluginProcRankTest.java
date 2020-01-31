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

public class MemPluginProcRankTest {
    // Data is same except for swap data is added.
    private static final String PROCRANK_DATA = "  PID       Vss      Rss      Pss      Uss     Swap    PSwap    USwap    ZSwap  cmdline\n" +
            "  929  7805100K  269040K  136427K   92812K       0K       0K       0K       0K  system_server\n" +
            " 1497  5900796K  200144K  119126K  108452K       0K       0K       0K       0K  com.android.systemui\n" +
            " 1878  5361844K  101368K   37679K   30584K       0K       0K       0K       0K  com.android.launcher3\n" +
            "  636  1696916K   80264K   29355K   12260K       0K       0K       0K       0K  zygote\n" +
            " 3330  5546912K   84384K   28278K   24636K       0K       0K       0K       0K  com.accuweather.android\n" +
            "  654   110636K   30848K   26385K   24656K       0K       0K       0K       0K  /vendor/bin/hw/android.hardware.camera.provider@2.4-service\n" +
            " 1474  5741968K   74676K   21231K   18404K       0K       0K       0K       0K  com.android.bluetooth\n" +
            "  635  5380428K   90876K   19347K    8216K       0K       0K       0K       0K  zygote64\n" +
            " 1715  5494044K   75256K   19257K   15636K       0K       0K       0K       0K  com.android.phone\n" +
            "  501   296708K   25436K   17458K   15600K       0K       0K       0K       0K  /system/bin/surfaceflinger\n" +
            " 1699  1700120K   32208K   16827K    5032K       0K       0K       0K       0K  webview_zygote\n" +
            " 2351  5262804K   66256K   14428K   11348K       0K       0K       0K       0K  com.android.nfc\n" +
            "  825   802284K   18840K   13917K   13424K       0K       0K       0K       0K  /vendor/bin/hw/rild\n" +
            "  828  270770132K   14584K   12772K   12544K       0K       0K       0K       0K  media.swcodec";

    MemPlugin spySut;
    BugReportModule mockBugReport;
    TestSection fakeProcRankSection;

    @Before
    public void setup() {
        MemPlugin sut = new MemPlugin();
        Context mockContext = mock(Context.class);
        spySut = spy(sut);

        mockBugReport = mock(BugReportModule.class);
        when(mockBugReport.getContext()).thenReturn(mockContext);
        fakeProcRankSection = new TestSection(mockBugReport, Section.PROCRANK);
        when(mockBugReport.findSection(Section.PROCRANK)).thenReturn(fakeProcRankSection);
    }

    @Test
    public void instantiates() {
        assertNotEquals(null, spySut);
    }

    @Test
    public void getsErrorForChangedHeader() {
        final String UNKNOWN_HEADER = "  PID       Unknown Vss      Rss      Pss      Uss     Swap    PSwap    USwap    ZSwap  cmdline";
        fakeProcRankSection.setTestLines(UNKNOWN_HEADER);
        spySut.load(mockBugReport);

        spySut.generate(mockBugReport);

        verify(mockBugReport, times(1)).printErr(anyInt(), contains("unknown format"));
    }

    @Test
    public void parsesProcRankData() {
        fakeProcRankSection.setTestLines(PROCRANK_DATA);
        spySut.load(mockBugReport);

        spySut.generate(mockBugReport);

        //No Errors
        verify(mockBugReport, never()).printErr(anyInt(), contains("procrank"));
        verify(mockBugReport, never()).printErr(anyInt(), contains("Failed to remove K"));
    }
}
