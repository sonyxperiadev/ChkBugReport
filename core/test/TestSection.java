import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.Section;

public class TestSection extends Section {
    TestSection(Module module, String sectionName) {
        super(module, sectionName);
    }

    void setTestLines(String lines) {
        clear();
        for(String line: lines.split("\n")) {
            addLine(line);
        }
    }
}
