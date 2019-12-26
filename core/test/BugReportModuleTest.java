import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import com.sonyericsson.chkbugreport.Context;
import com.sonyericsson.chkbugreport.BugReportModule;

public class BugReportModuleTest {
    @Test
    public void itInstantiates() {
        Context context = new Context();
        BugReportModule sut = new BugReportModule(context);
        assertNotEquals(null, sut);
    }
}
