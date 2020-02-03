import com.sonyericsson.chkbugreport.util.Util;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

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
}
