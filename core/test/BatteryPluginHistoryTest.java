import com.sonyericsson.chkbugreport.BugReportModule;
import com.sonyericsson.chkbugreport.Context;
import com.sonyericsson.chkbugreport.Section;
import com.sonyericsson.chkbugreport.chart.ChartPluginRepo;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.plugins.battery.BatteryInfoPlugin;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public class BatteryPluginHistoryTest {
    private static final String BATTERY_HISTORY_DATA = "Battery History (0% used, 9124 used of 4096KB, 47 strings using 4816):\n" +
            "                    0 (14) RESET:TIME: 2020-01-08-17-18-28\n" +
            "                    0 (2) 100 c0b00041 status=discharging health=good plug=none temp=355 volt=4401 charge=2793 modemRailChargemAh=0 wifiRailChargemAh=0 +running +wake_lock +sensor +phone_scanning +screen phone_state=out brightness=dim +usb_data top=u11a82:\"com.android.launcher3\"\n" +
            "                    0 (2) 100 c0b00041 user=0:\"0\"\n" +
            "                    0 (2) 100 c0b00041 user=11:\"11\"\n" +
            "                    0 (2) 100 c0b00041 userfg=11:\"11\"\n" +
            "                    0 (2) 100 c0b00041 tmpwhitelist=u11a39:\"broadcast:u11a39:com.android.providers.calendar.intent.CalendarProvider2\"\n" +
            "                 +6ms (2) 100 c0b00041 -usb_data stats=0:\"wakelock-change\"\n" +
            "            +16s203ms (2) 100 c0b00040 brightness=dark\n" +
            "            +18s964ms (3) 100 80200040 charge=2791 -wake_lock -sensor -screen stats=0:\"screen-state\"\n" +
            "            +19s057ms (2) 100 c0200040 +wake_lock=1001:\"*telephony-radio*\"\n" +
            "            +19s059ms (1) 100 80200040 -wake_lock\n" +
            "            +19s059ms (2) 100 c0200040 +wake_lock=1001:\"*telephony-radio*\"\n" +
            "            +19s067ms (1) 100 80200040 -wake_lock\n" +
            "            +19s067ms (2) 100 c0200040 +wake_lock=1001:\"*telephony-radio*\"\n" +
            "            +19s068ms (1) 100 80200040 -wake_lock\n" +
            "          +8m10s300ms (14) TIME: 2020-01-08-17-26-38\n" +
            "            +19s501ms (3) 100 00200040 -running\n" +
            "          +8m10s322ms (2) 100 c0200040 charge=2790 +running +wake_lock=1000:\"startDream\" wake_reason=0:\"168:qcom,smd-rpm-summary:280:681b8.qcom,mpm:174:400f000.qcom,spmi:223:full-soc\"\n" +
            "          +8m11s167ms (1) 100 80200040 -wake_lock\n" +
            "          +8m12s905ms (2) 100 00200040 -running wake_reason=0:\"168:qcom,smd-rpm-summary\"\n" +
            "          +8m17s703ms (2) 100 80200040 +running wake_reason=0:\"168:qcom,smd-rpm-summary:280:681b8.qcom,mpm:174:400f000.qcom,spmi:223:full-soc\"\n" +
            "          +8m18s067ms (2) 100 00200040 -running wake_reason=0:\"168:qcom,smd-rpm-summary\"\n" +
            "          +9m58s788ms (2) 100 80200040 charge=2789 +running wake_reason=0:\"168:qcom,smd-rpm-summary:280:681b8.qcom,mpm:174:400f000.qcom,spmi:223:full-soc\"\n" +
            "          +9m59s024ms (2) 100 80200040 wake_reason=0:\"Abort:Callback failed on alarmtimer in platform_pm_suspend+0x0/0x50 returned -16\"\n" +
            "         +10m00s441ms (2) 100 00200040 -running stats=0:\"wakelock\n" +
            "       +3h06m30s789ms (2) 099 c0100021 -audio\n" +
            "       +3h06m35s139ms (2) 099 c0500021 +audio\n" +
            "       +3h06m35s344ms (3) 099 c0100021 -audio stats=0:\"shutdown\"\n" +
            "       +3h06m35s464ms (12) SHUTDOWN\n" +
            "       +3h06m35s465ms (12) START\n" +
            "       +3h06m35s465ms (14) TIME: 1970-04-04-02-08-51\n" +
            "       +3h06m35s506ms (20) TIME: 1970-04-04-02-08-51";

    BatteryInfoPlugin spySut;
    BugReportModule mockBugReport;
    TestSection fakeBatteryInfoSection;
    Chapter mockBatteryChapter;
    private final PrintStream originalErr = System.err;
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();

    @Before
    public void setup() {
        BatteryInfoPlugin sut = new BatteryInfoPlugin();
        Context mockContext = mock(Context.class);
        ChartPluginRepo mockPluginRepo = mock(ChartPluginRepo.class);
        spySut = spy(sut);

        System.setErr(new PrintStream(errContent));

        mockBugReport = mock(BugReportModule.class);
        mockBatteryChapter = mock(Chapter.class);

        when(mockBugReport.getContext()).thenReturn(mockContext);
        when(mockBugReport.findOrCreateChapter("Battery info")).thenReturn(mockBatteryChapter);
        when(mockBugReport.getChartPluginRepo()).thenReturn(mockPluginRepo);
        fakeBatteryInfoSection = new TestSection(mockBugReport, Section.DUMP_OF_SERVICE_BATTERYINFO);
        when(mockBugReport.findSection(Section.DUMP_OF_SERVICE_BATTERYINFO)).thenReturn(fakeBatteryInfoSection);
    }

    @After
    public void restore() {
        System.setErr(originalErr);
    }

    @Test
    public void instantiates() {
        assertNotEquals(null, spySut);
    }

    @Test
    public void parsesBatteryHistory() {
        fakeBatteryInfoSection.setTestLines(BATTERY_HISTORY_DATA);
        spySut.load(mockBugReport);

        spySut.generate(mockBugReport);

        //No Errors
        assertEquals("", errContent.toString());
        verify(mockBugReport, never()).printErr(anyInt(), contains("Battery history"));
        verify(mockBatteryChapter, times(1)).addChapter(any());
    }
}
