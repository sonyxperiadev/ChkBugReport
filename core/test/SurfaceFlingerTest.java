import com.sonyericsson.chkbugreport.BugReportModule;
import com.sonyericsson.chkbugreport.Context;
import com.sonyericsson.chkbugreport.Section;
import com.sonyericsson.chkbugreport.plugins.MemPlugin;
import com.sonyericsson.chkbugreport.plugins.SurfaceFlingerPlugin;
import org.junit.Before;
import org.junit.Test;

import java.util.Vector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

public class SurfaceFlingerTest {
    private static final String SURFACE_FLINGER_DATA = "Build configuration: [sf PRESENT_TIME_OFFSET=0 FORCE_HWC_FOR_RBG_TO_YUV=1 MAX_VIRT_DISPLAY_DIM=4096 RUNNING_WITHOUT_SYNC_FRAMEWORK=0 NUM_FRAMEBUFFER_SURFACE_BUFFERS=2] [libui] [libgui]\n" +
            "\n" +
            "Display identification data:\n" +
            "Display 129 (HWC display 0): invalid EDID: 0 ff ff ff ff ff ff 0 44 6d 1 0 1 0 0 0 1b 10 1 3 80 50 2d 78 a d c9 a0 57 47 98 27 12 48 4c 0 0 0 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 a2 3d 38 14 40 70 13 81 4 8 38 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 fe 0 33 0 0 0 0 0 0 0 0 0 0 0 0 0 38 \n" +
            "\n" +
            "Wide-Color information:\n" +
            "Device has wide color built-in display: 1\n" +
            "Device uses color management: 1\n" +
            "DisplayColorSetting: Managed\n" +
            "Display 129 color modes:\n" +
            "    ColorMode::NATIVE (0)\n" +
            "    ColorMode::SRGB (7)\n" +
            "    ColorMode::DISPLAY_P3 (9)\n" +
            "    Current color mode: ColorMode::SRGB (7)\n" +
            "\n" +
            "Sync configuration: [using: EGL_ANDROID_native_fence_sync EGL_KHR_wait_sync]\n" +
            "\n" +
            "VSYNC configuration:\n" +
            "         app phase:   6500000 ns\t         SF phase:   4000000 ns\n" +
            "   early app phase:   6500000 ns\t   early SF phase:   4000000 ns\n" +
            "GL early app phase:   6500000 ns\tGL early SF phase:   4000000 ns\n" +
            "    present offset:         0 ns\t     VSYNC period:  16666666 ns\n" +
            "\n" +
            "Scheduler enabled.+  Smart 90 for video detection: off\n" +
            "\n" +
            "app: state=Idle VSyncState={displayId=129, count=305146}\n" +
            "  pending events (count=0):\n" +
            "  connections (count=47):\n" +
            "    Connection{0x7322803460, VSyncRequest::None}\n" +
            "    Connection{0x731e839500, VSyncRequest::None}\n" +
            "    Connection{0x731e8395a0, VSyncRequest::None}\n" +
            "    Connection{0x7315530540, VSyncRequest::None}\n" +
            "    Connection{0x731e839780, VSyncRequest::None}\n" +
            "    Connection{0x7315531260, VSyncRequest::None}\n" +
            "    Connection{0x7315532a20, VSyncRequest::None}\n" +
            "    Connection{0x7315532ac0, VSyncRequest::None}\n" +
            "    Connection{0x7315533420, VSyncRequest::None}\n" +
            "    Connection{0x731e839c80, VSyncRequest::None}\n" +
            "    Connection{0x731e839d20, VSyncRequest::None}\n" +
            "    Connection{0x731e839dc0, VSyncRequest::None}\n" +
            "    Connection{0x731e839e60, VSyncRequest::None}\n" +
            "    Connection{0x73155c5a00, VSyncRequest::None}\n" +
            "    Connection{0x73155c5820, VSyncRequest::None}\n" +
            "    Connection{0x73155c50a0, VSyncRequest::None}\n" +
            "    Connection{0x731e8396e0, VSyncRequest::None}\n" +
            "    Connection{0x731e839be0, VSyncRequest::None}\n" +
            "    Connection{0x731e83a5e0, VSyncRequest::None}\n" +
            "    Connection{0x731e83c0c0, VSyncRequest::None}\n" +
            "    Connection{0x731e83c3e0, VSyncRequest::None}\n" +
            "    Connection{0x73155c6c20, VSyncRequest::None}\n" +
            "    Connection{0x731e83c160, VSyncRequest::None}\n" +
            "    Connection{0x731e83c5c0, VSyncRequest::None}\n" +
            "    Connection{0x731e83c700, VSyncRequest::None}\n" +
            "    Connection{0x731e83c480, VSyncRequest::None}\n" +
            "    Connection{0x731e83cd40, VSyncRequest::None}\n" +
            "    Connection{0x731e83cf20, VSyncRequest::None}\n" +
            "    Connection{0x731e83cac0, VSyncRequest::None}\n" +
            "    Connection{0x73155c55a0, VSyncRequest::None}\n" +
            "    Connection{0x731e83cde0, VSyncRequest::None}\n" +
            "    Connection{0x731e83b940, VSyncRequest::None}\n" +
            "    Connection{0x731e83b300, VSyncRequest::None}\n" +
            "    Connection{0x731e83a400, VSyncRequest::None}\n" +
            "    Connection{0x731e83cb60, VSyncRequest::None}\n" +
            "    Connection{0x731e83b4e0, VSyncRequest::None}\n" +
            "    Connection{0x731e83b440, VSyncRequest::None}\n" +
            "    Connection{0x731e83b580, VSyncRequest::None}\n" +
            "    Connection{0x731e83a2c0, VSyncRequest::None}\n" +
            "    Connection{0x731e83b120, VSyncRequest::None}\n" +
            "    Connection{0x731e83ba80, VSyncRequest::None}\n" +
            "    Connection{0x731e83da60, VSyncRequest::None}\n" +
            "    Connection{0x731e83de20, VSyncRequest::None}\n" +
            "    Connection{0x731e83dec0, VSyncRequest::None}\n" +
            "    Connection{0x731e83df60, VSyncRequest::None}\n" +
            "    Connection{0x731e9b5320, VSyncRequest::None}\n" +
            "    Connection{0x73155c94c0, VSyncRequest::None}\n" +
            "\n" +
            "Static screen stats:\n" +
            "  < 1 frames: 1902.700 s (44.7%)\n" +
            "  < 2 frames: 1997.261 s (46.9%)\n" +
            "  < 3 frames: 59.579 s (1.4%)\n" +
            "  < 4 frames: 40.913 s (1.0%)\n" +
            "  < 5 frames: 3.094 s (0.1%)\n" +
            "  < 6 frames: 1.404 s (0.0%)\n" +
            "  < 7 frames: 0.328 s (0.0%)\n" +
            "  7+ frames: 255.355 s (6.0%)\n" +
            "\n" +
            "Total missed frame count: 204\n" +
            "HWC missed frame count: 204\n" +
            "GPU missed frame count: 0\n" +
            "\n" +
            "Buffering stats:\n" +
            "  [Layer name] <Active time> <Two buffer> <Double buffered> <Triple buffered>\n" +
            "  [StatusBar#0] 5138.27 0.003 0.260 0.740\n" +
            "  [SurfaceView - #0] 240.63 0.285 0.891 0.109\n" +
            "  [BootAnimation#0] 87.39 0.000 1.000 0.000\n" +
            "  [NavigationBar0#0] 28.51 0.302 0.578 0.422\n" +
            "  [SurfaceView - com.sonyericsson.setupwizard/com.sonymobile.setupwizard.screen.WelcomeScreen#0] 26.67 0.000 0.000 0.000\n" +
            "  [com.android.settings/com.android.settings.SubSettings#0] 8.94 0.703 0.787 0.213\n" +
            "  [com.android.settings/com.android.settings.homepage.SettingsHomepageActivity#0] 8.19 0.409 0.516 0.484\n" +
            "  [com.android.settings/com.android.settings.SubSettings#1] 6.71 0.583 1.000 0.000\n" +
            "  [com.google.android.setupwizard/com.google.android.setupwizard.network.NetworkActivity#0] 6.27 0.333 0.333 0.667\n" +
            "  [com.sonymobile.home/com.sonymobile.home.HomeActivity#0] 3.62 0.695 1.000 0.000\n" +
            "  [ColorFade#0] 3.06 0.000 0.000 1.000\n" +
            "  [com.android.systemui/com.android.systemui.recents.RecentsActivity#0] 2.85 0.132 0.517 0.483\n" +
            "  [VolumeDialogImpl#0] 2.03 0.402 0.402 0.598\n" +
            "  [com.sonymobile.home/com.sonymobile.home.HomeActivity#1] 1.98 0.541 0.583 0.417\n" +
            "  [com.google.android.setupwizard/com.google.android.setupwizard.time.DateTimeCheck#0] 1.81 0.000 0.000 1.000\n" +
            "  [com.example.headsupnotification/com.example.headsupnotification2.MainActivity#0] 1.14 0.205 0.205 0.795\n" +
            "  [com.sonyericsson.setupwizard/com.sonymobile.setupwizard.screen.ImportantInformationScreen#0] 1.00 0.802 0.802 0.198\n" +
            "  [com.android.settings.intelligence/com.android.settings.intelligence.search.SearchActivity#0] 0.77 0.000 0.000 1.000\n" +
            "  [com.google.android.googlequicksearchbox/com.google.android.apps.gsa.velour.dynamichosts.TransparentVelvetDynamicHostActivity#0] 0.70 0.398 0.398 0.602\n" +
            "  [com.sonyericsson.lockscreen.uxpnxt/com.sonymobile.lockscreen.settings.AmbientSettingsTopActivity#0] 0.66 1.000 1.000 0.000\n" +
            "  [com.google.android.setupwizard/com.google.android.setupwizard.carrier.SimMissingActivity#0] 0.55 1.000 1.000 0.000\n" +
            "  [com.sonymobile.themes.liquid.LiveWallpaperService#0] 0.53 1.000 1.000 0.000\n" +
            "  [com.sonyericsson.setupwizard/com.sonymobile.setupwizard.screen.WelcomeScreen#0] 0.38 0.000 0.000 1.000\n" +
            "  [com.sonymobile.anondata/com.sonymobile.anondata.ui.PrototypeDialog#0] 0.38 1.000 1.000 0.000\n" +
            "  [com.sonyericsson.lockscreen.uxpnxt/com.sonymobile.lockscreen.settings.AmbientActivity#0] 0.37 1.000 1.000 0.000\n" +
            "  [com.sonyericsson.usbux/com.sonyericsson.usbux.dialogs.PcCompanionInstallationActivity#0] 0.31 0.000 0.000 1.000\n" +
            "  [com.sonyericsson.usbux/com.sonyericsson.usbux.service.ConfirmMtpModeActivity#0] 0.28 0.000 0.000 1.000\n" +
            "  [com.google.android.setupwizard/com.google.android.setupwizard.time.DateTimeActivity#0] 0.11 1.000 1.000 0.000\n" +
            "  [#0] 0.11 0.000 1.000 0.000\n" +
            "  [InputMethod#0] 0.09 1.000 1.000 0.000\n" +
            "\n" +
            "Visible layers (count = 3)\n" +
            "GraphicBufferProducers: 9, max 4096\n" +
            "+ ContainerLayer (Display OneHand Overlays#0)\n" +
            "  Region TransparentRegion (this=0 count=0)\n" +
            "  Region VisibleRegion (this=0 count=0)\n" +
            "  Region SurfaceDamageRegion (this=0 count=0)\n" +
            "      layerStack=   0, z=       -1, pos=(0,0), size=(   0,   0), crop=[  0,   0,  -1,  -1], cornerRadius=0.000000, isProtected=0, isOpaque=0, invalidate=1, dataspace=Default, defaultPixelFormat=Unknown/None, color=(0.000,0.000,0.000,1.000), flags=0x00000002, tr=[0.00, 0.00][0.00, 0.00]\n" +
            "      parent=none\n" +
            "      zOrderRelativeOf=none\n" +
            "      activeBuffer=[   0x   0:   0,Unknown/None], tr=[0.00, 0.00][0.00, 0.00] queued-frames=0, mRefreshPending=0, metadata={}\n" +
            "+ ContainerLayer (Display Root#0)\n" +
            "  Region TransparentRegion (this=1 count=2)\n" +
            "  Region VisibleRegion (this=3 count=4)\n" +
            "  Region SurfaceDamageRegion (this=5 count=6)\n" +
            "      layerStack=   7, z=        8, pos=(9,10), size=(  11,  12), crop=[ 13,   14, -15, -16], cornerRadius=1.700000, isProtected=18, isOpaque=19, invalidate=20, dataspace=Something, defaultPixelFormat=RGBA8888, color=(2.100,2.200,2.300,2.400), flags=0x00000025, tr=[2.60, 2.70][2.80, 2.90]\n" +
            "      parent=CoolParent\n" +
            "      zOrderRelativeOf=OtherThing\n" +
            "      activeBuffer=[  30x  31:   32,WowAThing], tr=[3.30, 3.40][3.50, 3.60] queued-frames=37, mRefreshPending=38, metadata={SweetMetaDataBro}\n" +
            "+ ContainerLayer (mBelowAppWindowsContainers#0)\n" +
            "  Region TransparentRegion (this=0 count=0)\n" +
            "  Region VisibleRegion (this=0 count=0)\n" +
            "  Region SurfaceDamageRegion (this=0 count=0)\n" +
            "      layerStack=   0, z=        0, pos=(0,0), size=(   0,   0), crop=[  0,   0,  -1,  -1], cornerRadius=0.000000, isProtected=0, isOpaque=0, invalidate=1, dataspace=Default, defaultPixelFormat=Unknown/None, color=(0.000,0.000,0.000,1.000), flags=0x00000000, tr=[0.00, 0.00][0.00, 0.00]\n" +
            "      parent=Display Root#0\n" +
            "      zOrderRelativeOf=none\n" +
            "      activeBuffer=[   0x   0:   0,Unknown/None], tr=[0.00, 0.00][0.00, 0.00] queued-frames=0, mRefreshPending=0, metadata={}\n" +
            //Part of dump dropped...
            "SurfaceFlinger global state:\n" +
            "EGL implementation : 1.5\n" +
            "EGL_KHR_image EGL_KHR_image_base EGL_QCOM_create_image EGL_KHR_lock_surface EGL_KHR_lock_surface2 EGL_KHR_lock_surface3 EGL_KHR_gl_texture_2D_image EGL_KHR_gl_texture_cubemap_image EGL_KHR_gl_texture_3D_image EGL_KHR_gl_renderbuffer_image EGL_ANDROID_blob_cache EGL_KHR_create_context EGL_KHR_surfaceless_context EGL_KHR_create_context_no_error EGL_KHR_get_all_proc_addresses EGL_QCOM_lock_image2 EGL_EXT_protected_content EGL_KHR_no_config_context EGL_EXT_surface_SMPTE2086_metadata EGL_ANDROID_recordable EGL_ANDROID_native_fence_sync EGL_ANDROID_image_native_buffer EGL_ANDROID_framebuffer_target EGL_EXT_create_context_robustness EGL_EXT_pixel_format_float EGL_EXT_yuv_surface EGL_IMG_context_priority EGL_IMG_image_plane_attribs EGL_KHR_cl_event EGL_KHR_cl_event2 EGL_KHR_fence_sync EGL_KHR_gl_colorspace EGL_EXT_image_gl_colorspace EGL_KHR_mutable_render_buffer EGL_KHR_partial_update EGL_KHR_reusable_sync EGL_KHR_wait_sync EGL_QCOM_gpu_perf \n" +
            "GLES: Qualcomm, Adreno (TM) 630, OpenGL ES 3.2 V@415.0 (GIT@34b6654, Id4a1c1aeea, 1569595379) (Date:09/27/19)\n" +
            "GL_OES_EGL_image GL_OES_EGL_image_external GL_OES_EGL_sync GL_OES_vertex_half_float GL_OES_framebuffer_object GL_OES_rgb8_rgba8 GL_OES_compressed_ETC1_RGB8_texture GL_AMD_compressed_ATC_texture GL_KHR_texture_compression_astc_ldr GL_KHR_texture_compression_astc_hdr GL_OES_texture_compression_astc GL_OES_texture_npot GL_EXT_texture_filter_anisotropic GL_EXT_texture_format_BGRA8888 GL_EXT_read_format_bgra GL_OES_texture_3D GL_EXT_color_buffer_float GL_EXT_color_buffer_half_float GL_QCOM_alpha_test GL_OES_depth24 GL_OES_packed_depth_stencil GL_OES_depth_texture GL_OES_depth_texture_cube_map GL_EXT_sRGB GL_OES_texture_float GL_OES_texture_float_linear GL_OES_texture_half_float GL_OES_texture_half_float_linear GL_EXT_texture_type_2_10_10_10_REV GL_EXT_texture_sRGB_decode GL_EXT_texture_format_sRGB_override GL_OES_element_index_uint GL_EXT_copy_image GL_EXT_geometry_shader GL_EXT_tessellation_shader GL_OES_texture_stencil8 GL_EXT_shader_io_blocks GL_OES_shader_image_atomic GL_OES_sample_variables GL_EXT_texture_border_clamp GL_EXT_EGL_image_external_wrap_modes GL_EXT_multisampled_render_to_texture GL_EXT_multisampled_render_to_texture2 GL_OES_shader_multisample_interpolation GL_EXT_texture_cube_map_array GL_EXT_draw_buffers_indexed GL_EXT_gpu_shader5 GL_EXT_robustness GL_EXT_texture_buffer GL_EXT_shader_framebuffer_fetch GL_ARM_shader_framebuffer_fetch_depth_stencil GL_OES_texture_storage_multisample_2d_array GL_OES_sample_shading GL_OES_get_program_binary GL_EXT_debug_label GL_KHR_blend_equation_advanced GL_KHR_blend_equation_advanced_coherent GL_QCOM_tiled_rendering GL_ANDROID_extension_pack_es31a GL_EXT_primitive_bounding_box GL_OES_standard_derivatives GL_OES_vertex_array_object GL_EXT_disjoint_timer_query GL_KHR_debug GL_EXT_YUV_target GL_EXT_sRGB_write_control GL_EXT_texture_norm16 GL_EXT_discard_framebuffer GL_OES_surfaceless_context GL_OVR_multiview GL_OVR_multiview2 GL_EXT_texture_sRGB_R8 GL_KHR_no_error GL_EXT_debug_marker GL_OES_EGL_image_external_essl3 GL_OVR_multiview_multisampled_render_to_texture GL_EXT_buffer_storage GL_EXT_external_buffer GL_EXT_blit_framebuffer_params GL_EXT_clip_cull_distance GL_EXT_protected_textures GL_EXT_shader_non_constant_global_initializers GL_QCOM_texture_foveated GL_QCOM_texture_foveated_subsampled_layout GL_QCOM_shader_framebuffer_fetch_noncoherent GL_QCOM_shader_framebuffer_fetch_rate GL_EXT_memory_object GL_EXT_memory_object_fd GL_EXT_EGL_image_array GL_NV_shader_noperspective_interpolation GL_KHR_robust_buffer_access_behavior GL_EXT_EGL_image_storage GL_EXT_blend_func_extended GL_EXT_clip_control GL_OES_texture_view GL_EXT_fragment_invocation_density GL_QCOM_YUV_texture_gather \n" +
            "RenderEngine supports protected context: 0\n" +
            "RenderEngine is in protected context: 0\n" +
            "RenderEngine program cache size for unprotected context: 64\n" +
            "RenderEngine program cache size for protected context: 0\n" +
            "RenderEngine last dataspace conversion: (Default) to (BT709 sRGB Full range)\n" +
            "  Region undefinedRegion (this=0x7322808620, count=1)\n" +
            "    [  0,   0,   0,   0]\n" +
            "  orientation=0, isPoweredOn=1\n" +
            "  transaction-flags         : 00000000\n" +
            "  gpu_to_cpu_unsupported    : 0\n" +
            "  refresh-rate              : 60.000002 fps\n" +
            "  x-dpi                     : 428.625000\n" +
            "  y-dpi                     : 428.625000\n" +
            "  transaction time: 0.000000 us\n" +
            "Tracing state: disabled\n" +
            "  number of entries: 0 (0.00MB / 0.00MB)";

    SurfaceFlingerPlugin spySut;
    BugReportModule mockBugReport;
    TestSection fakeSurfaceFlingerSection;

    private static final String PLUGIN_LOG_TAG = "[SurfaceFlingerPlugin]";

    @Before
    public void setup() {
        SurfaceFlingerPlugin sut = new SurfaceFlingerPlugin();
        Context mockContext = mock(Context.class);
        spySut = spy(sut);

        mockBugReport = mock(BugReportModule.class);
        when(mockBugReport.getContext()).thenReturn(mockContext);
        fakeSurfaceFlingerSection = new TestSection(mockBugReport, Section.DUMP_OF_SERVICE_SURFACEFLINGER);
        when(mockBugReport.findSection(Section.DUMP_OF_SERVICE_SURFACEFLINGER)).thenReturn(fakeSurfaceFlingerSection);
    }

    @Test
    public void instantiates() {
        assertNotEquals(null, spySut);
    }

    @Test
    public void parsesLayers() {
        fakeSurfaceFlingerSection.setTestLines(SURFACE_FLINGER_DATA);
        spySut.load(mockBugReport);

        spySut.generate(mockBugReport);

        //No Errors
        verify(mockBugReport, never()).printErr(anyInt(), contains(PLUGIN_LOG_TAG));

        Vector<SurfaceFlingerPlugin.NewLayer> layers = spySut.getNewLayers();
        assertEquals(3, layers.size());

        assertEquals( "ContainerLayer", layers.get(0).type);
        assertEquals( "Display OneHand Overlays#0", layers.get(0).id);
        assertEquals( 0, layers.get(0).layerStack);
        assertEquals( -1, layers.get(0).z);
        assertEquals( 0, layers.get(0).rect.x);
        assertEquals( 0, layers.get(0).rect.y);
        assertEquals( 0, layers.get(0).rect.w);
        assertEquals( 0, layers.get(0).rect.h);

        assertEquals( 0, layers.get(0).crop.x);
        assertEquals( 0, layers.get(0).crop.y);
        assertEquals( -1, layers.get(0).crop.w);
        assertEquals( -1, layers.get(0).crop.h);
        assertEquals( 0.0, layers.get(0).cornerRadius, 0.00001);
        assertEquals( 0, layers.get(0).isProtected);
        assertEquals( 0, layers.get(0).isOpaque);
        assertEquals( 1, layers.get(0).invalidate);
        assertEquals( "Default", layers.get(0).dataSpace);
        assertEquals( "Unknown/None", layers.get(0).defaultPixelFormat);
        assertEquals(  0.000, layers.get(0).color[0], 0.0001);
        assertEquals(  0.000, layers.get(0).color[1], 0.0001);
        assertEquals(  0.000, layers.get(0).color[2], 0.0001);
        assertEquals(  1.000, layers.get(0).color[3], 0.0001);
        assertEquals( 0x00000002, layers.get(0).flags);
        assertEquals(0.00, layers.get(0).tr[0][0],0.001);
        assertEquals(0.00, layers.get(0).tr[0][1],0.001);
        assertEquals(0.00, layers.get(0).tr[1][0],0.001);
        assertEquals(0.00, layers.get(0).tr[1][1],0.001);
        assertEquals( "none", layers.get(0).parent);
        assertEquals( "none", layers.get(0).zOrderRelativeOf);
        assertEquals(0, layers.get(0).activeBufferWidth);
        assertEquals(0, layers.get(0).activeBufferHeight);
        assertEquals(0, layers.get(0).activeBufferStride);
        assertEquals("Unknown/None", layers.get(0).activeBufferPixelFormat);
        assertEquals(0.00, layers.get(0).bufferTransform[0][0], 0.001);
        assertEquals(0.00, layers.get(0).bufferTransform[0][1], 0.001);
        assertEquals(0.00, layers.get(0).bufferTransform[1][0], 0.001);
        assertEquals(0.00, layers.get(0).bufferTransform[1][1], 0.001);
        assertEquals(0, layers.get(0).queued);
        assertEquals(0, layers.get(0).refreshPending);
        assertEquals("", layers.get(0).metadata);

        assertEquals( "TransparentRegion", layers.get(0).regTransparent.getName());
        assertEquals( "VisibleRegion", layers.get(0).regVisable.getName());
        assertEquals( "SurfaceDamageRegion", layers.get(0).regSurfaceDamage.getName());



        assertEquals( "ContainerLayer", layers.get(1).type);
        assertEquals( "Display Root#0", layers.get(1).id);
        assertEquals( 7, layers.get(1).layerStack);
        assertEquals( 8, layers.get(1).z);
        assertEquals( 9, layers.get(1).rect.x);
        assertEquals( 10, layers.get(1).rect.y);
        assertEquals( 11, layers.get(1).rect.w);
        assertEquals( 12, layers.get(1).rect.h);
        assertEquals( 13, layers.get(1).crop.x);
        assertEquals( 14, layers.get(1).crop.y);
        assertEquals( -15, layers.get(1).crop.w);
        assertEquals( -16, layers.get(1).crop.h);
        assertEquals( 1.7, layers.get(1).cornerRadius, 0.00001);
        assertEquals( 18, layers.get(1).isProtected);
        assertEquals( 19, layers.get(1).isOpaque);
        assertEquals( 20, layers.get(1).invalidate);
        assertEquals( "Something", layers.get(1).dataSpace);
        assertEquals( "RGBA8888", layers.get(1).defaultPixelFormat);
        assertEquals(  2.100, layers.get(1).color[0], 0.0001);
        assertEquals(  2.200, layers.get(1).color[1], 0.0001);
        assertEquals(  2.300, layers.get(1).color[2], 0.0001);
        assertEquals(  2.400, layers.get(1).color[3], 0.0001);
        assertEquals( 0x00000025, layers.get(1).flags);
        assertEquals(2.60, layers.get(1).tr[0][0],0.001);
        assertEquals(2.70, layers.get(1).tr[0][1],0.001);
        assertEquals(2.80, layers.get(1).tr[1][0],0.001);
        assertEquals(2.90, layers.get(1).tr[1][1],0.001);
        assertEquals( "CoolParent", layers.get(1).parent);
        assertEquals( "OtherThing", layers.get(1).zOrderRelativeOf);
        assertEquals(30, layers.get(1).activeBufferWidth);
        assertEquals(31, layers.get(1).activeBufferHeight);
        assertEquals(32, layers.get(1).activeBufferStride);
        assertEquals("WowAThing", layers.get(1).activeBufferPixelFormat);
        assertEquals(3.30, layers.get(1).bufferTransform[0][0], 0.001);
        assertEquals(3.40, layers.get(1).bufferTransform[0][1], 0.001);
        assertEquals(3.50, layers.get(1).bufferTransform[1][0], 0.001);
        assertEquals(3.60, layers.get(1).bufferTransform[1][1], 0.001);
        assertEquals(37, layers.get(1).queued);
        assertEquals(38, layers.get(1).refreshPending);
        assertEquals("SweetMetaDataBro", layers.get(1).metadata);

        assertEquals( "TransparentRegion", layers.get(1).regTransparent.getName());
        assertEquals( "VisibleRegion", layers.get(1).regVisable.getName());
        assertEquals( "SurfaceDamageRegion", layers.get(1).regSurfaceDamage.getName());
    }
}
