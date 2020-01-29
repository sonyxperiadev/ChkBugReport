import com.sonyericsson.chkbugreport.BugReportModule;
import com.sonyericsson.chkbugreport.Section;
import com.sonyericsson.chkbugreport.plugins.WindowManagerPlugin;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.*;

public class WindowManagerPluginTest {
    private static final String WINDOW_DATA = "  Window #0 Window{fd184e8 u0 InputMethod}:\n" +
            "    mDisplayId=0 stackId=0 mSession=Session{ce80bcd 23447:u0a10205} mClient=android.os.BinderProxy@aca0e0b\n" +
            "    mOwnerUid=10205 mShowToOwnerOnly=true package=com.sonymobile.pobox appop=NONE\n" +
            "    mAttrs={(0,0)(fillxwrap) gr=BOTTOM CENTER_VERTICAL sim={adjust=pan} ty=INPUT_METHOD fmt=TRANSPARENT wanim=0x1030056\n" +
            "      fl=NOT_FOCUSABLE LAYOUT_IN_SCREEN SPLIT_TOUCH HARDWARE_ACCELERATED DRAWS_SYSTEM_BAR_BACKGROUNDS}\n" +
            "    Requested w=2160 h=1008 mLayoutSeq=11766\n" +
            "    mIsImWindow=true mIsWallpaper=false mIsFloatingLayer=true mWallpaperVisible=false\n" +
            "    mBaseLayer=151000 mSubLayer=0    mToken=WindowToken{fa9b9bf android.os.Binder@4f743de}\n" +
            "    mViewVisibility=0x8 mHaveFrame=true mObscured=false\n" +
            "    mSeq=0 mSystemUiVisibility=0x0\n" +
            "    mPolicyVisibility=false mLegacyPolicyVisibilityAfterAnim=false mAppOpVisibility=true parentHidden=false mPermanentlyHidden=false mHiddenWhileSuspended=false mForceHideNonSystemOverlayWindow=false\n" +
            "    mGivenContentInsets=[0,0][0,0] mGivenVisibleInsets=[0,0][0,0]\n" +
            "    mTouchableInsets=2 mGivenInsetsPending=false\n" +
            "    touchable region=SkRegion((0,72,2016,1080))\n" +
            "    mFullConfiguration={1.15 440mcc10mnc [ja_JP,en_US] ldltr sw360dp w360dp h648dp 480dpi nrml long hdr widecg port finger -keyb/v/h -nav/h winConfig={ mBounds=Rect(0, 0 - 1080, 2160) mAppBounds=Rect(0, 0 - 1080, 2016) mWindowingMode=fullscreen mDisplayWindowingMode=fullscreen mActivityType=undefined mAlwaysOnTop=undefined mRotation=ROTATION_0} s.9622}\n" +
            "    mLastReportedConfiguration={1.15 440mcc10mnc [ja_JP,en_US] ldltr sw360dp w672dp h336dp 480dpi nrml long hdr widecg land finger -keyb/v/h -nav/h winConfig={ mBounds=Rect(0, 0 - 2160, 1080) mAppBounds=Rect(0, 0 - 2016, 1080) mWindowingMode=fullscreen mDisplayWindowingMode=fullscreen mActivityType=undefined mAlwaysOnTop=undefined mRotation=ROTATION_90} s.9526}\n" +
            "    mHasSurface=false isReadyForDisplay()=false mWindowRemovalAllowed=false\n" +
            "    Frames: containing=[0,72][2016,1080] parent=[0,72][2016,1080]\n" +
            "        display=[0,72][2016,1080] overscan=[0,72][2016,1080]\n" +
            "        content=[0,72][2016,1080] visible=[0,72][2016,1080]\n" +
            "        decor=[0,0][0,0]\n" +
            "        outset=[0,0][0,0]\n" +
            "    mFrame=[0,72][2016,1080] last=[0,72][2016,1080]\n" +
            "     cutout=DisplayCutout{insets=Rect(0, 0 - 0, 0) boundingRect={Bounds=[Rect(0, 0 - 0, 0), Rect(0, 0 - 0, 0), Rect(0, 0 - 0, 0), Rect(0, 0 - 0, 0)]}} last=DisplayCutout{insets=Rect(0, 0 - 0, 0) boundingRect={Bounds=[Rect(0, 0 - 0, 0), Rect(0, 0 - 0, 0), Rect(0, 0 - 0, 0), Rect(0, 0 - 0, 0)]}}\n" +
            "    Cur insets: overscan=[0,0][0,0] content=[0,0][0,0] visible=[0,0][0,0] stable=[0,0][0,0] outsets=[0,0][0,0]    Lst insets: overscan=[0,0][0,0] content=[0,0][0,0] visible=[0,0][0,0] stable=[0,0][0,0] outset=[0,0][0,0]\n" +
            "     surface=[0,0][0,0]\n" +
            "    WindowStateAnimator{b21b80f InputMethod}:\n" +
            "      mDrawState=NO_SURFACE       mLastHidden=true\n" +
            "      mSystemDecorRect=[0,0][2016,1008] mLastClipRect=[0,0][2016,1008]\n" +
            "    mOrientationChanging=false configOrientationChanging=true mAppFreezing=false mReportOrientationChanged=true\n" +
            "    mForceSeamlesslyRotate=false seamlesslyRotate: pending=null finishedFrameNumber=0\n" +
            "    isOnScreen=false\n" +
            "    isVisible=false\n" +
            "  Window #1 Window{32ea425 u0 NavigationBar0}:\n" +
            "    mDisplayId=0 stackId=0 mSession=Session{aaeba49 29108:u0a10099} mClient=android.os.BinderProxy@61cca69\n" +
            "    mOwnerUid=10099 mShowToOwnerOnly=false package=com.android.systemui appop=NONE\n" +
            "    mAttrs={(0,0)(fillxfill) sim={adjust=pan} ty=NAVIGATION_BAR fmt=TRANSLUCENT\n" +
            "      fl=NOT_FOCUSABLE NOT_TOUCH_MODAL TOUCHABLE_WHEN_WAKING WATCH_OUTSIDE_TOUCH SPLIT_TOUCH HARDWARE_ACCELERATED FLAG_SLIPPERY\n" +
            "      pfl=COLOR_SPACE_AGNOSTIC}\n" +
            "    Requested w=1080 h=2160 mLayoutSeq=11936\n" +
            "    mBaseLayer=231000 mSubLayer=0    mToken=WindowToken{6245cee android.os.BinderProxy@782e4f0}\n" +
            "    mViewVisibility=0x0 mHaveFrame=true mObscured=false\n" +
            "    mSeq=0 mSystemUiVisibility=0x0\n" +
            "    mGivenContentInsets=[0,0][0,0] mGivenVisibleInsets=[0,0][0,0]\n" +
            "    mFullConfiguration={1.15 440mcc10mnc [ja_JP,en_US] ldltr sw360dp w360dp h648dp 480dpi nrml long hdr widecg port finger -keyb/v/h -nav/h winConfig={ mBounds=Rect(0, 0 - 1080, 2160) mAppBounds=Rect(0, 0 - 1080, 2016) mWindowingMode=fullscreen mDisplayWindowingMode=fullscreen mActivityType=undefined mAlwaysOnTop=undefined mRotation=ROTATION_0} s.9622}\n" +
            "    mLastReportedConfiguration={1.15 440mcc10mnc [ja_JP,en_US] ldltr sw360dp w360dp h648dp 480dpi nrml long hdr widecg port finger -keyb/v/h -nav/h winConfig={ mBounds=Rect(0, 0 - 1080, 2160) mAppBounds=Rect(0, 0 - 1080, 2016) mWindowingMode=fullscreen mDisplayWindowingMode=fullscreen mActivityType=undefined mAlwaysOnTop=undefined mRotation=ROTATION_0} s.9622}\n" +
            "    mHasSurface=true isReadyForDisplay()=true mWindowRemovalAllowed=false\n" +
            "    Frames: containing=[0,2016][1080,2160] parent=[0,2016][1080,2160]\n" +
            "        display=[0,2016][1080,2160] overscan=[0,2016][1080,2160]\n" +
            "        content=[0,2016][1080,2160] visible=[0,2016][1080,2160]\n" +
            "        decor=[0,0][0,0]\n" +
            "        outset=[-2147483648,-2147483648][2147483647,2147483647]\n" +
            "    mFrame=[0,2016][1080,2160] last=[0,2016][1080,2160]\n" +
            "     cutout=DisplayCutout{insets=Rect(0, 0 - 0, 0) boundingRect={Bounds=[Rect(0, 0 - 0, 0), Rect(0, 0 - 0, 0), Rect(0, 0 - 0, 0), Rect(0, 0 - 0, 0)]}} last=DisplayCutout{insets=Rect(0, 0 - 0, 0) boundingRect={Bounds=[Rect(0, 0 - 0, 0), Rect(0, 0 - 0, 0), Rect(0, 0 - 0, 0), Rect(0, 0 - 0, 0)]}}\n" +
            "    Cur insets: overscan=[0,0][0,0] content=[0,0][0,0] visible=[0,0][0,0] stable=[0,0][0,0] outsets=[0,0][0,0]    Lst insets: overscan=[0,0][0,0] content=[0,0][0,0] visible=[0,0][0,0] stable=[0,0][0,0] outset=[0,0][0,0]\n" +
            "     surface=[0,0][0,0]\n" +
            "    WindowStateAnimator{992096e NavigationBar0}:\n" +
            "      mSurface=Surface(name=NavigationBar0)/@0x55c3c21\n" +
            "      Surface: shown=true layer=0 alpha=1.0 rect=(0.0,0.0) 1080 x 144 transform=(1.0, 0.0, 1.0, 0.0)\n" +
            "      mDrawState=HAS_DRAWN       mLastHidden=false\n" +
            "      mSystemDecorRect=[0,0][1080,144] mLastClipRect=[0,0][1080,144]\n" +
            "    mLastFreezeDuration=+1h50m56s282ms\n" +
            "    mForceSeamlesslyRotate=false seamlesslyRotate: pending=null finishedFrameNumber=0\n" +
            "    isOnScreen=true\n" +
            "    isVisible=true";


    WindowManagerPlugin spySut;
    BugReportModule mockBugReport;
    TestSection fakeWindowsSection;

    @Before
    public void setup() {
        WindowManagerPlugin sut = new WindowManagerPlugin();
        spySut = spy(sut);

        Section mockWindowSection = mock(Section.class);
        mockBugReport = mock(BugReportModule.class);
        fakeWindowsSection = new TestSection(mockBugReport, Section.WINDOW_MANAGER_WINDOWS);
        fakeWindowsSection.clear();
        when(mockBugReport.findSection(Section.DUMP_OF_SERVICE_WINDOW)).thenReturn(mockWindowSection);
        when(mockBugReport.findSection(Section.WINDOW_MANAGER_WINDOWS)).thenReturn(fakeWindowsSection);
    }

    @Test
    public void instantiates() {
        assertNotEquals(null, spySut);
    }

    @Test
    public void parsesWindows() {
        fakeWindowsSection.setTestLines(WINDOW_DATA);
        spySut.load(mockBugReport);
        verify(mockBugReport, never()).addBug(any());
        verify(mockBugReport, never()).printErr(anyInt(), contains(Section.DUMP_OF_SERVICE_WINDOW));
        verify(mockBugReport, never()).printErr(anyInt(), contains(Section.WINDOW_MANAGER_WINDOWS));

        spySut.generate(mockBugReport);
        verify(spySut).generateWindowList(any(), any());
    }
}
