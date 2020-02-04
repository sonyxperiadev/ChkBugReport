import com.sonyericsson.chkbugreport.plugins.battery.WakeLock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class WakeLockTest {
    private final PrintStream originalErr = System.err;
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();

    @Before
    public void setup() {
        System.setErr(new PrintStream(errContent));
    }

    @After
    public void restore() {
        System.setErr(originalErr);
    }

    @Test
    public void findsIntegerUid() {
        WakeLock sut = new WakeLock("Wake lock 1000 *job*/android/com.android.server.pm.DynamicCodeLoggingService: 1s 354ms (2 times) max=807 actual=1522 realtime");
        assertEquals("1000", sut.getUIDString());
        assertEquals("", errContent.toString());
    }

    @Test
    public void parsesIntegerUid() {
        WakeLock sut = new WakeLock("Wake lock 1000 *job*/android/com.android.server.pm.DynamicCodeLoggingService: 1s 354ms (2 times) max=807 actual=1522 realtime");
        assertEquals(1000, sut.getUID());
    }

    @Test
    public void findsStringUid() {
        WakeLock sut = new WakeLock("Wake lock u0a87 Scrims: 1s 206ms (2 times) max=721 actual=1430 realtime");
        assertEquals("u0a87", sut.getUIDString());
        assertEquals("", errContent.toString());
    }

    @Test
    public void parsesStringUid() {
        WakeLock sut = new WakeLock("Wake lock u0a87 Scrims: 1s 206ms (2 times) max=721 actual=1430 realtime");
        assertEquals(10087, sut.getUID());
    }

    @Test
    public void parsesName() {
        WakeLock sut = new WakeLock("Wake lock u0a87 Scrims: 1s 206ms (2 times) max=721 actual=1430 realtime");
        assertEquals("Scrims", sut.getName());
    }

    @Test
    public void parsesTime() {
        WakeLock sut = new WakeLock("Wake lock u0a87 Scrims: 1s 206ms (2 times) max=721 actual=1430 realtime");
        assertEquals(1206, sut.getDurationMs());
    }

    @Test
    public void parsesCount() {
        WakeLock sut = new WakeLock("Wake lock u0a87 Scrims: 1s 206ms (2 times) max=721 actual=1430 realtime");
        assertEquals(2, sut.getCount());
    }

    @Test
    public void printsErrorForUnknownLine() {
        WakeLock sut = new WakeLock("Some Weird Line");
        assertNotEquals("", errContent.toString());
    }

    @Test
    public void parsesShortLineWithUIDNumber() {
        WakeLock sut = new WakeLock("123", "Wake lock *dexopt* realtime");
        assertEquals(123, sut.getUID());
        assertEquals("*dexopt*", sut.getName());
    }

    @Test
    public void parsesShortWithType() {
        WakeLock sut = new WakeLock("123", "Wake lock startDream: 199ms partial (2 times) max=173 actual=301 realtime");
        assertEquals("partial", sut.getType());
        assertEquals(199, sut.getDurationMs());
        assertEquals(2, sut.getCount());
        assertEquals(173, sut.getMax());
        assertEquals(301, sut.getActual());
    }

    @Test
    public void getsEmptyStringIfNoType() {
        WakeLock sut = new WakeLock("123", "Wake lock startDream: 199ms (2 times) max=173 actual=301 realtime");
        assertEquals("", sut.getType());
    }
}
