import com.sonyericsson.chkbugreport.Section;
import com.sonyericsson.chkbugreport.plugins.SysPropsPlugin;
import com.sonyericsson.chkbugreport.BugReportModule;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import static org.mockito.Mockito.*;

public class SysPropsPluginTest {
    private static final float FLOAT_DELTA = 0.001f;

    SysPropsPlugin sut;
    BugReportModule mockBugReport;
    Section mockSection;

    @Before
    public void setup() {
        sut = new SysPropsPlugin();
        mockBugReport = mock(BugReportModule.class);
        mockSection = mock(Section.class);
        when(mockBugReport.findSection(Section.UPTIME)).thenReturn(mockSection);
    }
    @Test
    public void instantiates() {
        assertNotEquals(null, sut);
    }


    @Test
    public void parsesUpTimeWithHoursAndMin() {
        final String UPTIME = " 10:12:36 up 17:57,  0 users,  load average: 0.00, 0.00, 0.00";
        SysPropsPlugin sut = new SysPropsPlugin();
        when(mockSection.getLine(0)).thenReturn(UPTIME);
        sut.load(mockBugReport);
        assertEquals(1077, sut.getUpTime()); //17 * 60 + 57 sec
        assertEquals(0, sut.getUsers());
        assertEquals(0.0, sut.getOneMinLoad(), FLOAT_DELTA);
        assertEquals(0.0, sut.getFiveMinLoad(), FLOAT_DELTA);
        assertEquals(0.0, sut.getFifteenMinLoad(), FLOAT_DELTA);
    }

    @Test
    public void parsesUpTimeWithZeroMin() {
        final String UPTIME = " 07:39:27 up 0 min,  0 users,  load average: 8.11, 2.18, 0.74";
        SysPropsPlugin sut = new SysPropsPlugin();
        when(mockSection.getLine(0)).thenReturn(UPTIME);
        sut.load(mockBugReport);
        assertEquals(0, sut.getUpTime());
        assertEquals(0, sut.getUsers());
        assertEquals(8.11, sut.getOneMinLoad(), FLOAT_DELTA);
        assertEquals(2.18, sut.getFiveMinLoad(), FLOAT_DELTA);
        assertEquals(0.74, sut.getFifteenMinLoad(), FLOAT_DELTA);
    }

    @Test
    public void parsesUpTimeWithFiveMin() {
        final String UPTIME = " 10:23:00 up 5 min,  0 users,  load average: 1.52, 3.32, 1.81";
        SysPropsPlugin sut = new SysPropsPlugin();
        when(mockSection.getLine(0)).thenReturn(UPTIME);
        sut.load(mockBugReport);
        assertEquals(5 * 60, sut.getUpTime());
        assertEquals(0, sut.getUsers());
        assertEquals(1.52, sut.getOneMinLoad(), FLOAT_DELTA);
        assertEquals(3.32, sut.getFiveMinLoad(), FLOAT_DELTA);
        assertEquals(1.81, sut.getFifteenMinLoad(), FLOAT_DELTA);
    }

    @Test
    public void parsesUpTimeWithDaysAndMin() {
        final String UPTIME = " 13:13:10 up 7 days, 55 min,  0 users,  load average: 1.81, 2.04, 2.25";
        SysPropsPlugin sut = new SysPropsPlugin();
        when(mockSection.getLine(0)).thenReturn(UPTIME);
        sut.load(mockBugReport);
        assertEquals(608100, sut.getUpTime());
        assertEquals(0, sut.getUsers());
        assertEquals(1.81, sut.getOneMinLoad(), FLOAT_DELTA);
        assertEquals(2.04, sut.getFiveMinLoad(), FLOAT_DELTA);
        assertEquals(2.25, sut.getFifteenMinLoad(), FLOAT_DELTA);
    }

    @Test
    public void parsesUpTimeWithDaysAndTime() {
        final String UPTIME = " 10:06:03 up 1 day, 18:38,  0 users,  load average: 1.27, 0.65, 0.39";
        SysPropsPlugin sut = new SysPropsPlugin();
        when(mockSection.getLine(0)).thenReturn(UPTIME);
        sut.load(mockBugReport);
        assertEquals(153480, sut.getUpTime());
        assertEquals(0, sut.getUsers());
        assertEquals(1.27, sut.getOneMinLoad(), FLOAT_DELTA);
        assertEquals(0.65, sut.getFiveMinLoad(), FLOAT_DELTA);
        assertEquals(0.39, sut.getFifteenMinLoad(), FLOAT_DELTA);
    }
    @Test
    public void parsesUsers() {
        final String UPTIME = " 10:06:03 up 1 day, 18:38,  123 users,  load average: 1.27, 0.65, 0.39";
        SysPropsPlugin sut = new SysPropsPlugin();
        when(mockSection.getLine(0)).thenReturn(UPTIME);
        sut.load(mockBugReport);
        assertEquals(123, sut.getUsers());
    }
}
