package com.nativelibs4java.ffmpeg.avfilter;
import org.bridj.BridJ;
import org.bridj.Pointer;
import org.bridj.ValuedEnum;
import org.bridj.ann.Library;
import org.bridj.ann.Ptr;
import org.bridj.ann.Runtime;
import org.bridj.cpp.CPPRuntime;
import static com.nativelibs4java.ffmpeg.avcodec.AvcodecLibrary.*;
import static com.nativelibs4java.ffmpeg.avformat.AvformatLibrary.*;
import static com.nativelibs4java.ffmpeg.avutil.AvutilLibrary.*;
import static com.nativelibs4java.ffmpeg.swscale.SwscaleLibrary.*;
/**
 * Wrapper for library <b>avfilter</b><br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.free.fr/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> or <a href="http://bridj.googlecode.com/">BridJ</a> .
 */
@Library("avfilter") 
@Runtime(CPPRuntime.class) 
public class AvfilterLibrary {
	static {
		BridJ.register();
	}
	/// <i>native declaration : libavfilter/avfilter.h</i>
	public static final int AV_PERM_WRITE = 2;
	/// <i>native declaration : libavfilter/avfilter.h</i>
	public static final int AV_PERM_REUSE = 8;
	/// <i>native declaration : libavfilter/avfilter.h</i>
	public static final int LIBAVFILTER_VERSION_MICRO = 0;
	/// <i>native declaration : libavfilter/avfilter.h</i>
	public static final int AV_PERM_PRESERVE = 4;
	/// <i>native declaration : libavfilter/avfilter.h</i>
	public static final int AV_PERM_REUSE2 = 16;
	/// <i>native declaration : libavfilter/avfilter.h</i>
	public static final int LIBAVFILTER_VERSION_MAJOR = 1;
	/// <i>native declaration : libavfilter/avfilter.h</i>
	public static final int LIBAVFILTER_VERSION_MINOR = 19;
	/// <i>native declaration : libavfilter/avfilter.h</i>
	public static final int AV_PERM_READ = 1;
	public native static void avfilter_version();
	public native static Pointer<java.lang.Byte > avfilter_configuration();
	public native static Pointer<java.lang.Byte > avfilter_license();
	public native static Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterPicRef > avfilter_ref_pic(Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterPicRef > ref, int pmask);
	public native static void avfilter_unref_pic(Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterPicRef > ref);
	public native static Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterFormats > avfilter_make_format_list(Pointer<ValuedEnum<PixelFormat > > pix_fmts);
	public native static int avfilter_add_colorspace(Pointer<Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterFormats > > avff, ValuedEnum<PixelFormat > pix_fmt);
	public native static Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterFormats > avfilter_all_colorspaces();
	public native static Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterFormats > avfilter_merge_formats(Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterFormats > a, Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterFormats > b);
	public native static void avfilter_formats_ref(Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterFormats > formats, Pointer<Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterFormats > > ref);
	public native static void avfilter_formats_unref(Pointer<Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterFormats > > ref);
	public native static void avfilter_formats_changeref(Pointer<Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterFormats > > oldref, Pointer<Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterFormats > > newref);
	public native static void avfilter_default_start_frame(Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterLink > link, Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterPicRef > picref);
	public native static void avfilter_default_draw_slice(Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterLink > link, int y, int h, int slice_dir);
	public native static void avfilter_default_end_frame(Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterLink > link);
	public native static int avfilter_default_config_output_link(Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterLink > link);
	public native static int avfilter_default_config_input_link(Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterLink > link);
	public native static Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterPicRef > avfilter_default_get_video_buffer(Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterLink > link, int perms, int w, int h);
	public native static void avfilter_set_common_formats(Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterContext > ctx, Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterFormats > formats);
	public native static int avfilter_default_query_formats(Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterContext > ctx);
	public native static void avfilter_null_start_frame(Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterLink > link, Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterPicRef > picref);
	public native static void avfilter_null_draw_slice(Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterLink > link, int y, int h, int slice_dir);
	public native static void avfilter_null_end_frame(Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterLink > link);
	public native static Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterPicRef > avfilter_null_get_video_buffer(Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterLink > link, int perms, int w, int h);
	public native static int avfilter_link(Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterContext > src, int srcpad, Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterContext > dst, int dstpad);
	public native static int avfilter_config_links(Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterContext > filter);
	public native static Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterPicRef > avfilter_get_video_buffer(Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterLink > link, int perms, int w, int h);
	public native static int avfilter_request_frame(Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterLink > link);
	public native static int avfilter_poll_frame(Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterLink > link);
	public native static void avfilter_start_frame(Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterLink > link, Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterPicRef > picref);
	public native static void avfilter_end_frame(Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterLink > link);
	public native static void avfilter_draw_slice(Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterLink > link, int y, int h, int slice_dir);
	public native static void avfilter_register_all();
	public native static void avfilter_uninit();
	public native static int avfilter_register(Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilter > filter);
	public native static Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilter > avfilter_get_by_name(Pointer<java.lang.Byte > name);
	public native static Pointer<Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilter > > av_filter_next(Pointer<Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilter > > filter);
	public native static Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterContext > avfilter_open(Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilter > filter, Pointer<java.lang.Byte > inst_name);
	public native static int avfilter_init_filter(Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterContext > filter, Pointer<java.lang.Byte > args, Pointer<? > opaque);
	public native static void avfilter_destroy(Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterContext > filter);
	public native static int avfilter_insert_filter(Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterLink > link, Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterContext > filt, int in, int out);
	public native static void avfilter_insert_pad(int idx, Pointer<java.lang.Integer > count, @Ptr long padidx_off, Pointer<Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterPad > > pads, Pointer<Pointer<Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterLink > > > links, Pointer<com.nativelibs4java.ffmpeg.avfilter.AVFilterPad > newpad);
}
