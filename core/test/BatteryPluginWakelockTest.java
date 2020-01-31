import com.sonyericsson.chkbugreport.BugReportModule;
import com.sonyericsson.chkbugreport.Context;
import com.sonyericsson.chkbugreport.Section;
import com.sonyericsson.chkbugreport.chart.ChartPluginRepo;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.plugins.battery.BatteryInfoPlugin;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.*;

public class BatteryPluginWakelockTest {
        private static final String KERNEL_WAKE_LOCK_DATA = "Statistics since last charge:\n" +
                "  All kernel wake locks:\n" +
                "  Kernel Wake lock epoll_healthd_file:NETLINK: 15s 197ms (774 times) realtime\n" +
                "  Kernel Wake lock hal_bluetooth_lock: 14s 98ms (12 times) realtime\n" +
                "  Kernel Wake lock epoll_system_server_file:[timerfd22_system_server]: 12s 549ms (220 times) realtime\n" +
                "  Kernel Wake lock PowerManagerService.WakeLocks: 9s 683ms (51 times) realtime\n" +
                "  Kernel Wake lock pn551: 4s 38ms (2 times) realtime\n" +
                "  Kernel Wake lock qpnp_fg_sanity_check: 3s 7ms (2 times) realtime\n" +
                "  Kernel Wake lock qpnp_fg_memaccess: 2s 888ms (762 times) realtime\n" +
                "  Kernel Wake lock epoll_system_server_file:[timerfd20_system_server]: 2s 444ms (40 times) realtime\n" +
                "  Kernel Wake lock epoll_healthd_file:[timerfd2_healthd]: 2s 249ms (36 times) realtime\n" +
                "  Kernel Wake lock alarmtimer: 2s 3ms (1 times) realtime\n" +
                "  Kernel Wake lock battery: 1s 910ms (161 times) realtime\n" +
                "  Kernel Wake lock PowerManagerService.Display: 688ms (4 times) realtime\n" +
                "  Kernel Wake lock PowerManager.SuspendLockout: 654ms (0 times) realtime\n" +
                "  Kernel Wake lock epoll_system_server_file:[timerfd19_system_server]: 466ms (8 times) realtime\n" +
                "  Kernel Wake lock qpnp-smbcharger-16: 252ms (161 times) realtime\n" +
                "  Kernel Wake lock usb: 208ms (114 times) realtime\n" +
                "  Kernel Wake lock PowerManagerService.Broadcasts: 207ms (4 times) realtime\n" +
                "  Kernel Wake lock bms: 164ms (47 times) realtime\n" +
                "  Kernel Wake lock nanohub_wakelock_read: 137ms (2 times) realtime";

        BatteryInfoPlugin spySut;
        BugReportModule mockBugReport;
        TestSection fakeBatteryInfoSection;
        Chapter mockBatteryChapter;

        @Before
        public void setup() {
            BatteryInfoPlugin sut = new BatteryInfoPlugin();
            Context mockContext = mock(Context.class);
            ChartPluginRepo mockPluginRepo = mock(ChartPluginRepo.class);
            spySut = spy(sut);

            mockBugReport = mock(BugReportModule.class);
            mockBatteryChapter = mock(Chapter.class);

            when(mockBugReport.getContext()).thenReturn(mockContext);
            when(mockBugReport.findOrCreateChapter("Battery info")).thenReturn(mockBatteryChapter);
            when(mockBugReport.getChartPluginRepo()).thenReturn(mockPluginRepo);
            fakeBatteryInfoSection = new TestSection(mockBugReport, Section.DUMP_OF_SERVICE_BATTERYINFO);
            when(mockBugReport.findSection(Section.DUMP_OF_SERVICE_BATTERYINFO)).thenReturn(fakeBatteryInfoSection);
        }

        @Test
        public void instantiates() {
            assertNotEquals(null, spySut);
        }

        @Test
        public void parsesKernelWakeLocksData() {
            fakeBatteryInfoSection.setTestLines(KERNEL_WAKE_LOCK_DATA);
            spySut.load(mockBugReport);

            spySut.generate(mockBugReport);

            //No Errors
            verify(mockBugReport, never()).printErr(anyInt(), contains("KWL"));
            verify(mockBatteryChapter, times(1)).addChapter(any());
        }
}
