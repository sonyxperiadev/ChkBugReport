import com.sonyericsson.chkbugreport.BugReportModule;
import com.sonyericsson.chkbugreport.Context;
import com.sonyericsson.chkbugreport.ProcessRecord;
import com.sonyericsson.chkbugreport.Section;
import com.sonyericsson.chkbugreport.plugins.MemPlugin;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Vector;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class MemPluginMemInfoTest {
    // Data is same except for swap data is added.
    private static final String MEMINFO_DATA = "Applications Memory Usage (in Kilobytes):\n" +
            "Uptime: 6696824 Realtime: 6696824\n" +
            "\n" +
            "** MEMINFO in pid 9823 [com.sonymobile.home] **\n" +
            "                   Pss      Pss   Shared  Private   Shared  Private  SwapPss     Heap     Heap     Heap\n" +
            "                 Total    Clean    Dirty    Dirty    Clean    Clean    Dirty     Size    Alloc     Free\n" +
            "                ------   ------   ------   ------   ------   ------   ------   ------   ------   ------\n" +
            "  Native Heap    29797        0     2204    29748        0        0     9858    62596    33550    29045\n" +
            "  Dalvik Heap     6364        0     2064     6316        0        0      512    10260     5130     5130\n" +
            " Dalvik Other     1177        0      168     1176        0        0       12                           \n" +
            "        Stack       48        0        4       48        0        0       12                           \n" +
            "       Ashmem        2        0        4        0        8        0        0                           \n" +
            "      Gfx dev     6824        0        0     6824        0        0        0                           \n" +
            "    Other dev      144        0      284        0        0      144        0                           \n" +
            "     .so mmap     1486        0     1956       60    30792        0       10                           \n" +
            "    .jar mmap      584        0        0        0    17548        0        0                           \n" +
            "    .apk mmap     8362     5708        0        0    23804     5708        0                           \n" +
            "    .ttf mmap      198       84        0        0      464       84        0                           \n" +
            "    .dex mmap       70       68        0        0       88       68       12                           \n" +
            "    .oat mmap      431        0        0        0    15188        0        0                           \n" +
            "    .art mmap     1407        0    11900     1224      104        0       35                           \n" +
            "   Other mmap      467        0      552      148     1000        0        0                           \n" +
            "    GL mtrack    10684        0        0    10684        0        0        0                           \n" +
            "      Unknown     1235        0      564     1232        0        0      232                           \n" +
            "        TOTAL    79963     5860    19700    57460    88996     6004    10683    72856    38680    34175\n" +
            " \n" +
            " Dalvik Details\n" +
            "        .Heap     5708        0        0     5708        0        0        0                           \n" +
            "         .LOS       45        0      920       24        0        0      492                           \n" +
            "      .Zygote      355        0     1144      328        0        0       20                           \n" +
            "   .NonMoving      256        0        0      256        0        0        0                           \n" +
            " .LinearAlloc      917        0      108      916        0        0       12                           \n" +
            "          .GC      148        0       52      148        0        0        0                           \n" +
            " .IndirectRef      112        0        8      112        0        0        0                           \n" +
            "   .Boot vdex        0        0        0        0       44        0        0                           \n" +
            "     .App dex       66       64        0        0       40       64       12                           \n" +
            "    .App vdex        4        4        0        0        4        4        0                           \n" +
            "    .Boot art     1407        0    11900     1224      104        0       35                           \n" +
            "        TOTAL    79963     5860    19700    57460    88996     6004    10683    72856    38680    34175\n" +
            " \n" +
            " App Summary\n" +
            "                       Pss(KB)\n" +
            "                        ------\n" +
            "           Java Heap:     7540\n" +
            "         Native Heap:    29748\n" +
            "                Code:     5920\n" +
            "               Stack:       48\n" +
            "            Graphics:    17508\n" +
            "       Private Other:     2700\n" +
            "              System:    16499\n" +
            " \n" +
            "               TOTAL:    79963       TOTAL SWAP PSS:    10683\n" +
            " \n" +
            " Objects\n" +
            "               Views:      126         ViewRootImpl:        1\n" +
            "         AppContexts:       13           Activities:        1\n" +
            "              Assets:       40        AssetManagers:        0\n" +
            "       Local Binders:       47        Proxy Binders:       58\n" +
            "       Parcel memory:     1667         Parcel count:      777\n" +
            "    Death Recipients:        4      OpenSSL Sockets:        0\n" +
            "            WebViews:        0\n" +
            " \n" +
            " SQL\n" +
            "         MEMORY_USED:      496\n" +
            "  PAGECACHE_OVERFLOW:      151          MALLOC_SIZE:      117\n" +
            " \n" +
            " DATABASES\n" +
            "      pgsz     dbsz   Lookaside(b)          cache  Dbname\n" +
            "         4       28             36         8/30/5  /data/user/0/com.sonymobile.home/databases/google_analytics_v4.db\n" +
            "         4       80            109     290/102/25  /data/user/0/com.sonymobile.home/databases/home_database.db";

    private static final String MEMINFO_DATA_WITH_EGL_MTRACK_AND_APP_ART = "Applications Memory Usage (in Kilobytes):\n" +
            "Uptime: 6696824 Realtime: 6696824\n" +
            "\n" +
            "** MEMINFO in pid 9823 [com.sonymobile.home] **\n" +
            "                   Pss      Pss   Shared  Private   Shared  Private  SwapPss     Heap     Heap     Heap\n" +
            "                 Total    Clean    Dirty    Dirty    Clean    Clean    Dirty     Size    Alloc     Free\n" +
            "                ------   ------   ------   ------   ------   ------   ------   ------   ------   ------\n" +
            "  Native Heap    29797        0     2204    29748        0        0     9858    62596    33550    29045\n" +
            "  Dalvik Heap     6364        0     2064     6316        0        0      512    10260     5130     5130\n" +
            " Dalvik Other     1177        0      168     1176        0        0       12                           \n" +
            "        Stack       48        0        4       48        0        0       12                           \n" +
            "       Ashmem        2        0        4        0        8        0        0                           \n" +
            "      Gfx dev     6824        0        0     6824        0        0        0                           \n" +
            "    Other dev      144        0      284        0        0      144        0                           \n" +
            "     .so mmap     1486        0     1956       60    30792        0       10                           \n" +
            "    .jar mmap      584        0        0        0    17548        0        0                           \n" +
            "    .apk mmap     8362     5708        0        0    23804     5708        0                           \n" +
            "    .ttf mmap      198       84        0        0      464       84        0                           \n" +
            "    .dex mmap       70       68        0        0       88       68       12                           \n" +
            "    .oat mmap      431        0        0        0    15188        0        0                           \n" +
            "    .art mmap     1407        0    11900     1224      104        0       35                           \n" +
            "   Other mmap      467        0      552      148     1000        0        0                           \n" +
            "   EGL mtrack     3768        0        0     3768        0        0        0                           \n" +
            "        TOTAL    79963     5860    19700    57460    88996     6004    10683    72856    38680    34175\n" +
            " \n" +
            " Dalvik Details\n" +
            "     .App art     6688        0        0     6664       48        0     3272                           \n" +
            "    .Boot art     2999        0    10360     2820      116        0      161         ";

    private static final String MEMINFO_DATA_MANY_CONTEXT = "Applications Memory Usage (in Kilobytes):\n" +
            "Uptime: 6696824 Realtime: 6696824\n" +
            "\n" +
            "** MEMINFO in pid 9823 [com.sonymobile.home] **\n" +
            "                   Pss      Pss   Shared  Private   Shared  Private  SwapPss     Heap     Heap     Heap\n" +
            "                 Total    Clean    Dirty    Dirty    Clean    Clean    Dirty     Size    Alloc     Free\n" +
            "                ------   ------   ------   ------   ------   ------   ------   ------   ------   ------\n" +
            "  Native Heap    29797        0     2204    29748        0        0     9858    62596    33550    29045\n" +
            "     .so mmap     1486        0     1956       60    30792        0       10                           \n" +
            "      Unknown     1235        0      564     1232        0        0      232                           \n" +
            "        TOTAL    79963     5860    19700    57460    88996     6004    10683    72856    38680    34175\n" +
            " \n" +
            " Dalvik Details\n" +
            "        .Heap     5708        0        0     5708        0        0        0                           \n" +
            "    .App vdex        4        4        0        0        4        4        0                           \n" +
            "    .Boot art     1407        0    11900     1224      104        0       35                           \n" +
            " \n" +
            " App Summary\n" +
            "                       Pss(KB)\n" +
            "                        ------\n" +
            "           Java Heap:     7540\n" +
            "         Native Heap:    29748\n" +
            "                Code:     5920\n" +
            "               Stack:       48\n" +
            "            Graphics:    17508\n" +
            "       Private Other:     2700\n" +
            "              System:    16499\n" +
            " \n" +
            "               TOTAL:    79963       TOTAL SWAP PSS:    10683\n" +
            " \n" +
            " Objects\n" +
            "               Views:      126         ViewRootImpl:        1\n" +
            "         AppContexts:       25           Activities:        1\n" +
            "              Assets:       40        AssetManagers:        0\n" +
            "       Local Binders:       47        Proxy Binders:       58\n" +
            "       Parcel memory:     1667         Parcel count:      777\n" +
            "    Death Recipients:        4      OpenSSL Sockets:        0\n" +
            "            WebViews:        0\n";

    MemPlugin spySut;
    BugReportModule mockBugReport;
    TestSection fakeMemInfoSection;

    HashMap<Integer, ProcessRecord> processRecordMap = new HashMap<Integer, ProcessRecord>();

    @Before
    public void setup() {
        MemPlugin sut = new MemPlugin();
        Context mockContext = mock(Context.class);
        spySut = spy(sut);

        processRecordMap.clear();

        mockBugReport = mock(BugReportModule.class);
        when(mockBugReport.getContext()).thenReturn(mockContext);
        fakeMemInfoSection = new TestSection(mockBugReport, Section.DUMP_OF_SERVICE_MEMINFO);
        when(mockBugReport.findSection(Section.DUMP_OF_SERVICE_MEMINFO)).thenReturn(fakeMemInfoSection);
        when(mockBugReport.getProcessRecord(anyInt(), anyBoolean(), anyBoolean())).thenAnswer(invocation -> {
            int pid = invocation.getArgument(0);

            //Assume we can just make a new PR:
            ProcessRecord pr = new ProcessRecord(mockContext, "", pid);
            processRecordMap.put(pid, pr);

            return pr;
        });
    }

    @Test
    public void instantiates() {
        assertNotEquals(null, spySut);
    }

    @Test
    public void parsesMemInfoMemorySection() {
        fakeMemInfoSection.setTestLines(MEMINFO_DATA);
        spySut.load(mockBugReport);

        spySut.generate(mockBugReport);
        assertEquals(1, processRecordMap.size());

        ProcessRecord record = processRecordMap.get(9823);
        assertNotNull(record);

        assertEquals("com.sonymobile.home (9823)", record.getName());

        Vector<MemPlugin.MemInfo> memInfos = spySut.getMemInfos();
        assertEquals(1, memInfos.size());
        MemPlugin.MemInfo result = memInfos.get(0);

        assertEquals(29797, result.nativeHeap.pssTotal);
        assertEquals(0, result.nativeHeap.pssClean);
        assertEquals(2204, result.nativeHeap.sharedDirty);
        assertEquals(29748, result.nativeHeap.privateDirty);
        assertEquals(0, result.nativeHeap.sharedClean);
        assertEquals(0, result.nativeHeap.privateClean);
        assertEquals(9858, result.nativeHeap.swapPssDirty);
        assertEquals(62596, result.nativeHeap.heapSize);
        assertEquals(33550, result.nativeHeap.heapAlloc);
        assertEquals(29045, result.nativeHeap.heapFree);

        assertEquals(6364, result.dalvikHeap.pssTotal);
        assertEquals(0, result.dalvikHeap.pssClean);
        assertEquals(2064, result.dalvikHeap.sharedDirty);
        assertEquals(6316, result.dalvikHeap.privateDirty);
        assertEquals(0, result.dalvikHeap.sharedClean);
        assertEquals(0, result.dalvikHeap.privateClean);
        assertEquals(512, result.dalvikHeap.swapPssDirty);
        assertEquals(10260, result.dalvikHeap.heapSize);
        assertEquals(5130, result.dalvikHeap.heapAlloc);
        assertEquals(5130, result.dalvikHeap.heapFree);

        assertEquals(1177, result.dalvikOther.pssTotal);
        assertEquals(0, result.dalvikOther.pssClean);
        assertEquals(168, result.dalvikOther.sharedDirty);
        assertEquals(1176, result.dalvikOther.privateDirty);
        assertEquals(0, result.dalvikOther.sharedClean);
        assertEquals(0, result.dalvikOther.privateClean);
        assertEquals(12, result.dalvikOther.swapPssDirty);
        assertEquals(-1, result.dalvikOther.heapSize);
        assertEquals(-1, result.dalvikOther.heapAlloc);
        assertEquals(-1, result.dalvikOther.heapFree);

        assertEquals(48, result.stack.pssTotal);
        assertEquals(0, result.stack.pssClean);
        assertEquals(4, result.stack.sharedDirty);
        assertEquals(48, result.stack.privateDirty);
        assertEquals(0, result.stack.sharedClean);
        assertEquals(0, result.stack.privateClean);
        assertEquals(12, result.stack.swapPssDirty);
        assertEquals(-1, result.stack.heapSize);
        assertEquals(-1, result.stack.heapAlloc);
        assertEquals(-1, result.stack.heapFree);

        assertEquals(2, result.ashMem.pssTotal);
        assertEquals(0, result.ashMem.pssClean);
        assertEquals(4, result.ashMem.sharedDirty);
        assertEquals(0, result.ashMem.privateDirty);
        assertEquals(8, result.ashMem.sharedClean);
        assertEquals(0, result.ashMem.privateClean);
        assertEquals(0, result.ashMem.swapPssDirty);
        assertEquals(-1, result.ashMem.heapSize);
        assertEquals(-1, result.ashMem.heapAlloc);
        assertEquals(-1, result.ashMem.heapFree);

        assertEquals(6824, result.gfxDev.pssTotal);
        assertEquals(0, result.gfxDev.pssClean);
        assertEquals(0, result.gfxDev.sharedDirty);
        assertEquals(6824, result.gfxDev.privateDirty);
        assertEquals(0, result.gfxDev.sharedClean);
        assertEquals(0, result.gfxDev.privateClean);
        assertEquals(0, result.gfxDev.swapPssDirty);
        assertEquals(-1, result.gfxDev.heapSize);
        assertEquals(-1, result.gfxDev.heapAlloc);
        assertEquals(-1, result.gfxDev.heapFree);

        assertEquals(144, result.otherDev.pssTotal);
        assertEquals(0, result.otherDev.pssClean);
        assertEquals(284, result.otherDev.sharedDirty);
        assertEquals(0, result.otherDev.privateDirty);
        assertEquals(0, result.otherDev.sharedClean);
        assertEquals(144, result.otherDev.privateClean);
        assertEquals(0, result.otherDev.swapPssDirty);
        assertEquals(-1, result.otherDev.heapSize);
        assertEquals(-1, result.otherDev.heapAlloc);
        assertEquals(-1, result.otherDev.heapFree);

        assertEquals(1486, result.soMmap.pssTotal);
        assertEquals(0, result.soMmap.pssClean);
        assertEquals(1956, result.soMmap.sharedDirty);
        assertEquals(60, result.soMmap.privateDirty);
        assertEquals(30792, result.soMmap.sharedClean);
        assertEquals(0, result.soMmap.privateClean);
        assertEquals(10, result.soMmap.swapPssDirty);
        assertEquals(-1, result.soMmap.heapSize);
        assertEquals(-1, result.soMmap.heapAlloc);
        assertEquals(-1, result.soMmap.heapFree);

        assertEquals(584, result.jarMmap.pssTotal);
        assertEquals(0, result.jarMmap.pssClean);
        assertEquals(0, result.jarMmap.sharedDirty);
        assertEquals(0, result.jarMmap.privateDirty);
        assertEquals(17548, result.jarMmap.sharedClean);
        assertEquals(0, result.jarMmap.privateClean);
        assertEquals(0, result.jarMmap.swapPssDirty);
        assertEquals(-1, result.jarMmap.heapSize);
        assertEquals(-1, result.jarMmap.heapAlloc);
        assertEquals(-1, result.jarMmap.heapFree);

        assertEquals(8362, result.apkMmap.pssTotal);
        assertEquals(5708, result.apkMmap.pssClean);
        assertEquals(0, result.apkMmap.sharedDirty);
        assertEquals(0, result.apkMmap.privateDirty);
        assertEquals(23804, result.apkMmap.sharedClean);
        assertEquals(5708, result.apkMmap.privateClean);
        assertEquals(0, result.apkMmap.swapPssDirty);
        assertEquals(-1, result.apkMmap.heapSize);
        assertEquals(-1, result.apkMmap.heapAlloc);
        assertEquals(-1, result.apkMmap.heapFree);

        assertEquals(198, result.ttfMmap.pssTotal);
        assertEquals(84, result.ttfMmap.pssClean);
        assertEquals(0, result.ttfMmap.sharedDirty);
        assertEquals(0, result.ttfMmap.privateDirty);
        assertEquals(464, result.ttfMmap.sharedClean);
        assertEquals(84, result.ttfMmap.privateClean);
        assertEquals(0, result.ttfMmap.swapPssDirty);
        assertEquals(-1, result.ttfMmap.heapSize);
        assertEquals(-1, result.ttfMmap.heapAlloc);
        assertEquals(-1, result.ttfMmap.heapFree);

        assertEquals(70, result.dexMmap.pssTotal);
        assertEquals(68, result.dexMmap.pssClean);
        assertEquals(0, result.dexMmap.sharedDirty);
        assertEquals(0, result.dexMmap.privateDirty);
        assertEquals(88, result.dexMmap.sharedClean);
        assertEquals(68, result.dexMmap.privateClean);
        assertEquals(12, result.dexMmap.swapPssDirty);
        assertEquals(-1, result.dexMmap.heapSize);
        assertEquals(-1, result.dexMmap.heapAlloc);
        assertEquals(-1, result.dexMmap.heapFree);

        assertEquals(431, result.oatMmap.pssTotal);
        assertEquals(0, result.oatMmap.pssClean);
        assertEquals(0, result.oatMmap.sharedDirty);
        assertEquals(0, result.oatMmap.privateDirty);
        assertEquals(15188, result.oatMmap.sharedClean);
        assertEquals(0, result.oatMmap.privateClean);
        assertEquals(0, result.oatMmap.swapPssDirty);
        assertEquals(-1, result.oatMmap.heapSize);
        assertEquals(-1, result.oatMmap.heapAlloc);
        assertEquals(-1, result.oatMmap.heapFree);

        assertEquals(1407, result.artMmap.pssTotal);
        assertEquals(0, result.artMmap.pssClean);
        assertEquals(11900, result.artMmap.sharedDirty);
        assertEquals(1224, result.artMmap.privateDirty);
        assertEquals(104, result.artMmap.sharedClean);
        assertEquals(0, result.artMmap.privateClean);
        assertEquals(35, result.artMmap.swapPssDirty);
        assertEquals(-1, result.artMmap.heapSize);
        assertEquals(-1, result.artMmap.heapAlloc);
        assertEquals(-1, result.artMmap.heapFree);

        assertEquals(467, result.otherMmap.pssTotal);
        assertEquals(0, result.otherMmap.pssClean);
        assertEquals(552, result.otherMmap.sharedDirty);
        assertEquals(148, result.otherMmap.privateDirty);
        assertEquals(1000, result.otherMmap.sharedClean);
        assertEquals(0, result.otherMmap.privateClean);
        assertEquals(0, result.otherMmap.swapPssDirty);
        assertEquals(-1, result.otherMmap.heapSize);
        assertEquals(-1, result.otherMmap.heapAlloc);
        assertEquals(-1, result.otherMmap.heapFree);

        assertEquals(10684, result.glMtrack.pssTotal);
        assertEquals(0, result.glMtrack.pssClean);
        assertEquals(0, result.glMtrack.sharedDirty);
        assertEquals(10684, result.glMtrack.privateDirty);
        assertEquals(0, result.glMtrack.sharedClean);
        assertEquals(0, result.glMtrack.privateClean);
        assertEquals(0, result.glMtrack.swapPssDirty);
        assertEquals(-1, result.glMtrack.heapSize);
        assertEquals(-1, result.glMtrack.heapAlloc);
        assertEquals(-1, result.glMtrack.heapFree);

        assertEquals(1235, result.unknown.pssTotal);
        assertEquals(0, result.unknown.pssClean);
        assertEquals(564, result.unknown.sharedDirty);
        assertEquals(1232, result.unknown.privateDirty);
        assertEquals(0, result.unknown.sharedClean);
        assertEquals(0, result.unknown.privateClean);
        assertEquals(232, result.unknown.swapPssDirty);
        assertEquals(-1, result.unknown.heapSize);
        assertEquals(-1, result.unknown.heapAlloc);
        assertEquals(-1, result.unknown.heapFree);

        assertEquals(79963, result.total.pssTotal);
        assertEquals(5860, result.total.pssClean);
        assertEquals(19700, result.total.sharedDirty);
        assertEquals(57460, result.total.privateDirty);
        assertEquals(88996, result.total.sharedClean);
        assertEquals(6004, result.total.privateClean);
        assertEquals(10683, result.total.swapPssDirty);
        assertEquals(72856, result.total.heapSize);
        assertEquals(38680, result.total.heapAlloc);
        assertEquals(34175, result.total.heapFree);
    }

    @Test
    public void parsesMemInfoDalvikDetailsSection() {
        fakeMemInfoSection.setTestLines(MEMINFO_DATA);
        spySut.load(mockBugReport);

        spySut.generate(mockBugReport);
        assertEquals(1, processRecordMap.size());

        ProcessRecord record = processRecordMap.get(9823);
        assertNotNull(record);

        assertEquals("com.sonymobile.home (9823)", record.getName());

        Vector<MemPlugin.MemInfo> memInfos = spySut.getMemInfos();
        assertEquals(1, memInfos.size());
        MemPlugin.MemInfo result = memInfos.get(0);

        assertEquals(5708, result.dalvikDetailsHeap.pssTotal);
        assertEquals(0, result.dalvikDetailsHeap.pssClean);
        assertEquals(0, result.dalvikDetailsHeap.sharedDirty);
        assertEquals(5708, result.dalvikDetailsHeap.privateDirty);
        assertEquals(0, result.dalvikDetailsHeap.sharedClean);
        assertEquals(0, result.dalvikDetailsHeap.privateClean);
        assertEquals(0, result.dalvikDetailsHeap.swapPssDirty);
        assertEquals(-1, result.dalvikDetailsHeap.heapSize);
        assertEquals(-1, result.dalvikDetailsHeap.heapAlloc);
        assertEquals(-1, result.dalvikDetailsHeap.heapFree);

        assertEquals(45, result.dalvikDetailsLos.pssTotal);
        assertEquals(0, result.dalvikDetailsLos.pssClean);
        assertEquals(920, result.dalvikDetailsLos.sharedDirty);
        assertEquals(24, result.dalvikDetailsLos.privateDirty);
        assertEquals(0, result.dalvikDetailsLos.sharedClean);
        assertEquals(0, result.dalvikDetailsLos.privateClean);
        assertEquals(492, result.dalvikDetailsLos.swapPssDirty);
        assertEquals(-1, result.dalvikDetailsLos.heapSize);
        assertEquals(-1, result.dalvikDetailsLos.heapAlloc);
        assertEquals(-1, result.dalvikDetailsLos.heapFree);

        assertEquals(355, result.dalvikDetailsZygote.pssTotal);
        assertEquals(0, result.dalvikDetailsZygote.pssClean);
        assertEquals(1144, result.dalvikDetailsZygote.sharedDirty);
        assertEquals(328, result.dalvikDetailsZygote.privateDirty);
        assertEquals(0, result.dalvikDetailsZygote.sharedClean);
        assertEquals(0, result.dalvikDetailsZygote.privateClean);
        assertEquals(20, result.dalvikDetailsZygote.swapPssDirty);
        assertEquals(-1, result.dalvikDetailsZygote.heapSize);
        assertEquals(-1, result.dalvikDetailsZygote.heapAlloc);
        assertEquals(-1, result.dalvikDetailsZygote.heapFree);

        assertEquals(256, result.dalvikDetailsNonMoving.pssTotal);
        assertEquals(0, result.dalvikDetailsNonMoving.pssClean);
        assertEquals(0, result.dalvikDetailsNonMoving.sharedDirty);
        assertEquals(256, result.dalvikDetailsNonMoving.privateDirty);
        assertEquals(0, result.dalvikDetailsNonMoving.sharedClean);
        assertEquals(0, result.dalvikDetailsNonMoving.privateClean);
        assertEquals(0, result.dalvikDetailsNonMoving.swapPssDirty);
        assertEquals(-1, result.dalvikDetailsNonMoving.heapSize);
        assertEquals(-1, result.dalvikDetailsNonMoving.heapAlloc);
        assertEquals(-1, result.dalvikDetailsNonMoving.heapFree);

        assertEquals(148, result.dalvikDetailsGc.pssTotal);
        assertEquals(0, result.dalvikDetailsGc.pssClean);
        assertEquals(52, result.dalvikDetailsGc.sharedDirty);
        assertEquals(148, result.dalvikDetailsGc.privateDirty);
        assertEquals(0, result.dalvikDetailsGc.sharedClean);
        assertEquals(0, result.dalvikDetailsGc.privateClean);
        assertEquals(0, result.dalvikDetailsGc.swapPssDirty);
        assertEquals(-1, result.dalvikDetailsGc.heapSize);
        assertEquals(-1, result.dalvikDetailsGc.heapAlloc);
        assertEquals(-1, result.dalvikDetailsGc.heapFree);

        assertEquals(112, result.dalvikDetailsIndirectRef.pssTotal);
        assertEquals(0, result.dalvikDetailsIndirectRef.pssClean);
        assertEquals(8, result.dalvikDetailsIndirectRef.sharedDirty);
        assertEquals(112, result.dalvikDetailsIndirectRef.privateDirty);
        assertEquals(0, result.dalvikDetailsIndirectRef.sharedClean);
        assertEquals(0, result.dalvikDetailsIndirectRef.privateClean);
        assertEquals(0, result.dalvikDetailsIndirectRef.swapPssDirty);
        assertEquals(-1, result.dalvikDetailsIndirectRef.heapSize);
        assertEquals(-1, result.dalvikDetailsIndirectRef.heapAlloc);
        assertEquals(-1, result.dalvikDetailsIndirectRef.heapFree);

        assertEquals(0, result.dalvikDetailsBootVdex.pssTotal);
        assertEquals(0, result.dalvikDetailsBootVdex.pssClean);
        assertEquals(0, result.dalvikDetailsBootVdex.sharedDirty);
        assertEquals(0, result.dalvikDetailsBootVdex.privateDirty);
        assertEquals(44, result.dalvikDetailsBootVdex.sharedClean);
        assertEquals(0, result.dalvikDetailsBootVdex.privateClean);
        assertEquals(0, result.dalvikDetailsBootVdex.swapPssDirty);
        assertEquals(-1, result.dalvikDetailsBootVdex.heapSize);
        assertEquals(-1, result.dalvikDetailsBootVdex.heapAlloc);
        assertEquals(-1, result.dalvikDetailsBootVdex.heapFree);

        assertEquals(66, result.dalvikDetailsAppDex.pssTotal);
        assertEquals(64, result.dalvikDetailsAppDex.pssClean);
        assertEquals(0, result.dalvikDetailsAppDex.sharedDirty);
        assertEquals(0, result.dalvikDetailsAppDex.privateDirty);
        assertEquals(40, result.dalvikDetailsAppDex.sharedClean);
        assertEquals(64, result.dalvikDetailsAppDex.privateClean);
        assertEquals(12, result.dalvikDetailsAppDex.swapPssDirty);
        assertEquals(-1, result.dalvikDetailsAppDex.heapSize);
        assertEquals(-1, result.dalvikDetailsAppDex.heapAlloc);
        assertEquals(-1, result.dalvikDetailsAppDex.heapFree);

        assertEquals(4, result.dalvikDetailsAppVDex.pssTotal);
        assertEquals(4, result.dalvikDetailsAppVDex.pssClean);
        assertEquals(0, result.dalvikDetailsAppVDex.sharedDirty);
        assertEquals(0, result.dalvikDetailsAppVDex.privateDirty);
        assertEquals(4, result.dalvikDetailsAppVDex.sharedClean);
        assertEquals(4, result.dalvikDetailsAppVDex.privateClean);
        assertEquals(0, result.dalvikDetailsAppVDex.swapPssDirty);
        assertEquals(-1, result.dalvikDetailsAppVDex.heapSize);
        assertEquals(-1, result.dalvikDetailsAppVDex.heapAlloc);
        assertEquals(-1, result.dalvikDetailsAppVDex.heapFree);

        assertEquals(1407, result.dalvikDetailsBootArt.pssTotal);
        assertEquals(0, result.dalvikDetailsBootArt.pssClean);
        assertEquals(11900, result.dalvikDetailsBootArt.sharedDirty);
        assertEquals(1224, result.dalvikDetailsBootArt.privateDirty);
        assertEquals(104, result.dalvikDetailsBootArt.sharedClean);
        assertEquals(0, result.dalvikDetailsBootArt.privateClean);
        assertEquals(35, result.dalvikDetailsBootArt.swapPssDirty);
        assertEquals(-1, result.dalvikDetailsBootArt.heapSize);
        assertEquals(-1, result.dalvikDetailsBootArt.heapAlloc);
        assertEquals(-1, result.dalvikDetailsBootArt.heapFree);
    }

    @Test
    public void parsesMemInfoAppSummarySection() {
        fakeMemInfoSection.setTestLines(MEMINFO_DATA);
        spySut.load(mockBugReport);

        spySut.generate(mockBugReport);
        assertEquals(1, processRecordMap.size());

        ProcessRecord record = processRecordMap.get(9823);
        assertNotNull(record);

        assertEquals("com.sonymobile.home (9823)", record.getName());

        Vector<MemPlugin.MemInfo> memInfos = spySut.getMemInfos();
        assertEquals(1, memInfos.size());
        MemPlugin.MemInfo result = memInfos.get(0);

        assertEquals(7540, result.summaryJavaHeap);
        assertEquals(29748, result.summaryNativeHeap);
        assertEquals(5920, result.summaryCode);
        assertEquals(48, result.summaryStack);
        assertEquals(17508, result.summaryGraphics);
        assertEquals(2700, result.summaryPrivateOther);
        assertEquals(16499, result.summarySystem);
        assertEquals(79963, result.summaryTotal);
        assertEquals(10683, result.summaryTotalSwapPSS);
    }

    @Test
    public void parsesMemInfoObjectsSection() {
        fakeMemInfoSection.setTestLines(MEMINFO_DATA);
        spySut.load(mockBugReport);

        spySut.generate(mockBugReport);
        assertEquals(1, processRecordMap.size());

        ProcessRecord record = processRecordMap.get(9823);
        assertNotNull(record);

        assertEquals("com.sonymobile.home (9823)", record.getName());

        Vector<MemPlugin.MemInfo> memInfos = spySut.getMemInfos();
        assertEquals(1, memInfos.size());
        MemPlugin.MemInfo result = memInfos.get(0);

        assertEquals(126, result.views);
        assertEquals(1, result.viewRoots);
        assertEquals(13, result.appContexts);
        assertEquals(1, result.activities);
        assertEquals(40, result.assets);
        assertEquals(0, result.assetManagers);
        assertEquals(47, result.localBinders);
        assertEquals(58, result.proxyBinders);
        assertEquals(1667, result.parcelMemory);
        assertEquals(777, result.parcelCount);
        assertEquals(4, result.deathRec);
        assertEquals(0, result.openSSLSockets);
        assertEquals(0, result.webViews);
    }

    @Test
    public void parsesMemInfoSqlSection() {
        fakeMemInfoSection.setTestLines(MEMINFO_DATA);
        spySut.load(mockBugReport);

        spySut.generate(mockBugReport);
        assertEquals(1, processRecordMap.size());

        ProcessRecord record = processRecordMap.get(9823);
        assertNotNull(record);

        assertEquals("com.sonymobile.home (9823)", record.getName());

        Vector<MemPlugin.MemInfo> memInfos = spySut.getMemInfos();
        assertEquals(1, memInfos.size());
        MemPlugin.MemInfo result = memInfos.get(0);

        assertEquals(496, result.sqlMemUsed);
        assertEquals(151, result.sqlPageCacheOverflow);
        assertEquals(117, result.sqlMallocSize);
    }

    @Test
    public void parsesMemInfoDatabasesSection() {
        fakeMemInfoSection.setTestLines(MEMINFO_DATA);
        spySut.load(mockBugReport);

        spySut.generate(mockBugReport);
        assertEquals(1, processRecordMap.size());

        ProcessRecord record = processRecordMap.get(9823);
        assertNotNull(record);

        assertEquals("com.sonymobile.home (9823)", record.getName());

        Vector<MemPlugin.MemInfo> memInfos = spySut.getMemInfos();
        assertEquals(1, memInfos.size());

        MemPlugin.MemInfo result = memInfos.get(0);
        Vector<MemPlugin.DatabaseInfo> databaseInfos = result.dbs;

        assertEquals(2, databaseInfos.size());

        assertEquals(4, databaseInfos.get(0).pgsz);
        assertEquals(28, databaseInfos.get(0).dbsz);
        assertEquals(36, databaseInfos.get(0).lookaside);
        assertEquals("/data/user/0/com.sonymobile.home/databases/google_analytics_v4.db", databaseInfos.get(0).name);
        assertEquals("8/30/5", databaseInfos.get(0).cache);

        assertEquals(4, databaseInfos.get(1).pgsz);
        assertEquals(80, databaseInfos.get(1).dbsz);
        assertEquals(109, databaseInfos.get(1).lookaside);
        assertEquals("/data/user/0/com.sonymobile.home/databases/home_database.db", databaseInfos.get(1).name);
        assertEquals("290/102/25", databaseInfos.get(1).cache);
    }

    @Test
    public void parsesMemInfoMemorySectionWithEGLMtrack() {
        fakeMemInfoSection.setTestLines(MEMINFO_DATA_WITH_EGL_MTRACK_AND_APP_ART);
        spySut.load(mockBugReport);

        spySut.generate(mockBugReport);
        assertEquals(1, processRecordMap.size());

        ProcessRecord record = processRecordMap.get(9823);
        assertNotNull(record);

        assertEquals("com.sonymobile.home (9823)", record.getName());

        Vector<MemPlugin.MemInfo> memInfos = spySut.getMemInfos();
        assertEquals(1, memInfos.size());
        MemPlugin.MemInfo result = memInfos.get(0);

        assertEquals(3768, result.eglMTrack.pssTotal);
        assertEquals(0, result.eglMTrack.pssClean);
        assertEquals(0, result.eglMTrack.sharedDirty);
        assertEquals(3768, result.eglMTrack.privateDirty);
        assertEquals(0, result.eglMTrack.sharedClean);
        assertEquals(0, result.eglMTrack.privateClean);
        assertEquals(0, result.eglMTrack.swapPssDirty);
        assertEquals(-1, result.eglMTrack.heapSize);
        assertEquals(-1, result.eglMTrack.heapAlloc);
        assertEquals(-1, result.eglMTrack.heapFree);
    }

    @Test
    public void parsesMemInfoMemorySectionWithAppArt() {
        fakeMemInfoSection.setTestLines(MEMINFO_DATA_WITH_EGL_MTRACK_AND_APP_ART);
        spySut.load(mockBugReport);

        spySut.generate(mockBugReport);
        assertEquals(1, processRecordMap.size());

        ProcessRecord record = processRecordMap.get(9823);
        assertNotNull(record);

        assertEquals("com.sonymobile.home (9823)", record.getName());

        Vector<MemPlugin.MemInfo> memInfos = spySut.getMemInfos();
        assertEquals(1, memInfos.size());
        MemPlugin.MemInfo result = memInfos.get(0);

        assertEquals(6688, result.dalvikDetailsAppArt.pssTotal);
        assertEquals(0, result.dalvikDetailsAppArt.pssClean);
        assertEquals(0, result.dalvikDetailsAppArt.sharedDirty);
        assertEquals(6664, result.dalvikDetailsAppArt.privateDirty);
        assertEquals(48, result.dalvikDetailsAppArt.sharedClean);
        assertEquals(0, result.dalvikDetailsAppArt.privateClean);
        assertEquals(3272, result.dalvikDetailsAppArt.swapPssDirty);
        assertEquals(-1, result.dalvikDetailsAppArt.heapSize);
        assertEquals(-1, result.dalvikDetailsAppArt.heapAlloc);
        assertEquals(-1, result.dalvikDetailsAppArt.heapFree);
    }
}
