import com.sonyericsson.chkbugreport.Context;
import com.sonyericsson.chkbugreport.ImageCanvas;
import com.sonyericsson.chkbugreport.util.Util;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mock;

public class UtilTest {
    Util sut;

    @Before
    public void setup() {
        sut = new Util();
    }

    @Test
    public void instantiates() {
        assertNotEquals(null, sut);
    }

    @Test
    public void parsesIntegerUid() {
        assertEquals(1000, sut.parseUid("1000"));
        assertEquals(1234, sut.parseUid("1234"));
    }

    @Test
    public void parsesStringUid() {
        assertEquals(10123, sut.parseUid("u0a123"));
        assertEquals(1110106, sut.parseUid("u11a106"));
    }

    @Test(expected = NumberFormatException.class)
    public void throwsForInvalidUID() {
        sut.parseUid("notvalid");
    }

    @Test
    public void parsesSTypeUid() {
        assertEquals(1101002, sut.parseUid("u11s1002"));
    }

    @Test
    public void parsesITypeUid() {
        assertEquals(1200002, sut.parseUid("u11i1002"));
    }

    @Test
    public void parsesAITypeUid() {
        assertEquals(1191002, sut.parseUid("u11ai1002"));
    }

    @Test(expected = NumberFormatException.class)
    public void throwsForInvalidAppTypeUID() {
        sut.parseUid("u10d12345");
    }

    @Test(expected = NumberFormatException.class)
    public void throwsForInvalidFormatUID() {
        sut.parseUid("invalid");
    }

    @Test
    public void rendersMsTimeBar() {
        ImageCanvas mockImageCanvas = mock(ImageCanvas.class);
        sut.renderTimeBar(mockImageCanvas, 0, 0, 800, 75, 0, 1000, false);
        verify(mockImageCanvas, times(10)).drawString(any(), eq(0.f), eq(0.f));
        verify(mockImageCanvas, times(1)).drawString("0.000s", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("0.100s", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("0.200s", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("0.300s", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("0.400s", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("0.500s", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("0.600s", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("0.700s", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("0.800s", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("0.900s", 0.f, 0.f);
    }

    @Test
    public void rendersMinTimeBar() {
        ImageCanvas mockImageCanvas = mock(ImageCanvas.class);
        sut.renderTimeBar(mockImageCanvas, 0, 0, 800, 75, 0, 60*1000, false);
        verify(mockImageCanvas, times(12)).drawString(any(), eq(0.f), eq(0.f));
        verify(mockImageCanvas, times(1)).drawString("00:00:00", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("00:00:05", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("00:00:10", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("00:00:15", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("00:00:20", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("00:00:25", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("00:00:30", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("00:00:35", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("00:00:40", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("00:00:45", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("00:00:50", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("00:00:55", 0.f, 0.f);
    }

    @Test
    public void rendersHrTimeBar() {
        ImageCanvas mockImageCanvas = mock(ImageCanvas.class);
        sut.renderTimeBar(mockImageCanvas, 0, 0, 800, 75, 0, 60*60*1000, false);
        verify(mockImageCanvas, times(12)).drawString(any(), eq(0.f), eq(0.f));
        verify(mockImageCanvas, times(1)).drawString("00:00:00", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("00:05:00", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("00:10:00", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("00:15:00", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("00:20:00", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("00:25:00", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("00:30:00", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("00:35:00", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("00:40:00", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("00:45:00", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("00:50:00", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("00:55:00", 0.f, 0.f);
    }

    @Test
    public void renders1DayTimeBar() {
        ImageCanvas mockImageCanvas = mock(ImageCanvas.class);
        sut.renderTimeBar(mockImageCanvas, 0, 0, 800, 75, 0, 24*60*60*1000, false);
        verify(mockImageCanvas, times(12)).drawString(any(), eq(0.f), eq(0.f));
        verify(mockImageCanvas, times(1)).drawString("00:00:00", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("02:00:00", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("04:00:00", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("06:00:00", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("08:00:00", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("10:00:00", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("12:00:00", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("14:00:00", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("16:00:00", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("18:00:00", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("20:00:00", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("22:00:00", 0.f, 0.f);
    }

    @Test
    public void renders10DayTimeBar() {
        ImageCanvas mockImageCanvas = mock(ImageCanvas.class);
        sut.renderTimeBar(mockImageCanvas, 0, 0, 800, 75, 0, 10*24*60*60*1000, false);
        verify(mockImageCanvas, times(10)).drawString(any(), eq(0.f), eq(0.f));
        verify(mockImageCanvas, times(1)).drawString("00:00:00:00", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("01:00:00:00", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("02:00:00:00", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("03:00:00:00", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("04:00:00:00", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("05:00:00:00", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("06:00:00:00", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("07:00:00:00", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("08:00:00:00", 0.f, 0.f);
        verify(mockImageCanvas, times(1)).drawString("09:00:00:00", 0.f, 0.f);
    }
}