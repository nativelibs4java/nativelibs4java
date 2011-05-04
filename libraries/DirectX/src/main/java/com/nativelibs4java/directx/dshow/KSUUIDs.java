package com.nativelibs4java.directx.dshow;
import org.bridj.*;

/**
 * Common GUID definitions from Microsoft SDK v6.0A (ksuuids.h)<br>
 * DirectShow's include file based on ksmedia.h from WDM DDK<br>
 * Contains the GUIDs for the MediaType type, subtype fields and format types for DVD/MPEG2 media types.
 */
public class KSUUIDs {
	/*
	 * Obtained with a regexp replace (jEdit) on C:\Program Files\Microsoft SDKs\Windows\v6.0A\Include\\ksuuids.h :
	 *   (?m)OUR_GUID_ENTRY\(\s*(\w+)\s*,\s*0x(\w+)\s*,\s*0x(\w+)\s*,\s*0x(\w+)\s*,\s*0x(\w+)\s*,\s*0x(\w+)\s*,\s*0x(\w+)\s*,\s*0x(\w+)\s*,\s*0x(\w+)\s*,\s*0x(\w+)\s*,\s*0x(\w+)\s*,\s*0x(\w+)\s*\)
	 *   "public static final String " + _1 + " = \"" + _2 + "-" + _3 + "-" + _4 + "-" + _5 + _6 + "-" + _7 + _8 + _9 + _10 + _11 + _12 + "\";"
 	 */

	//
	// --- MPEG 2 definitions ---
	//
	
	// 36523B13-8EE5-11d1-8CA3-0060B057664A
	public static final String MEDIATYPE_MPEG2_PACK = "36523B13-8EE5-11d1-8CA3-0060B057664A";
	
	// e06d8020-db46-11cf-b4d1-00805f6cbbea
	public static final String MEDIATYPE_MPEG2_PES = "e06d8020-db46-11cf-b4d1-00805f6cbbea";
	
	
	public static final String MEDIATYPE_CONTROL = "e06d8021-db46-11cf-b4d1008005f6cbbea";
	
	public static final String MEDIATYPE_MPEG2_SECTIONS = "455f176c-4b06-47ce-9aef-8caef73df7b5";
	
	// {1ED988B0-3FFC-4523-8725-347BEEC1A8A0}
	public static final String MEDIASUBTYPE_MPEG2_VERSIONED_TABLES = "1ed988b0-3ffc-4523-8725-347beec1a8a0";
	
	public static final String MEDIASUBTYPE_ATSC_SI = "b3c7397c-d303-414d-b33c-4ed2c9d29733";
	
	public static final String MEDIASUBTYPE_DVB_SI = "e9dd31a3-221d-4adb-8532-9af39c1a48";
	
	// {EC232EB2-CB96-4191-B226-0EA129F38250}
	public static final String MEDIASUBTYPE_TIF_SI = "ec232eb2-cb96-4191-b226-ea129f38250";
	
	// {C892E55B-252D-42b5-A316-D997E7A5D995}
	public static final String MEDIASUBTYPE_MPEG2DATA = "c892e55b-252d-42b5-a316-d997e7a5d995";
	
	public static final String MEDIASUBTYPE_MPEG2_WMDRM_TRANSPORT = "18BEC4EA-4676-450e-B478-0CD84C54B327";
	
	// e06d8026-db46-11cf-b4d1-00805f6cbbea
	public static final String MEDIASUBTYPE_MPEG2_VIDEO = "e06d8026-db46-11cf-b4d1-00805f6cbbea";
	
	// use MPEG2VIDEOINFO (defined below) with FORMAT_MPEG2_VIDEO
	// e06d80e3-db46-11cf-b4d1-00805f6cbbea
	public static final String FORMAT_MPEG2_VIDEO = "e06d80e3-db46-11cf-b4d1-00805f6cbbea";
	
	// F72A76A0-EB0A-11d0-ACE4-0000C0CC16BA       (FORMAT_VideoInfo2)
	public static final String FORMAT_VIDEOINFO2 = "f72a76A0-eb0a-11d0-ace4-00c0cc16ba";
	
	// MPEG2 Other subtypes
	// e06d8022-db46-11cf-b4d1-00805f6cbbea
	public static final String MEDIASUBTYPE_MPEG2_PROGRAM = "e06d8022-db46-11cf-b4d1-008005f6cbbea";
	
	// e06d8023-db46-11cf-b4d1-00805f6cbbea
	public static final String MEDIASUBTYPE_MPEG2_TRANSPORT = "e06d8023-db46-11cf-b4d1-008005f6cbbea";
	
	// 138AA9A4-1EE2-4c5b-988E-19ABFDBC8A11
	public static final String MEDIASUBTYPE_MPEG2_TRANSPORT_STRIDE = "138aa9a4-1ee2-4c5b-988e-19abfdbc8a11";
	
	// {18BEC4EA-4676-450e-B478-0CD84C54B327}
	public static final String MEDIASUBTYPE_MPEG2_UDCR_TRANSPORT = "18BEC4EA-4676-450e-B478-0CD84C54B327";
	
	// e06d802b-db46-11cf-b4d1-00805f6cbbea
	public static final String MEDIASUBTYPE_MPEG2_AUDIO = "e06d802b-db46-11cf-b4d1-008005f6cbbea";
	
	// e06d802c-db46-11cf-b4d1-00805f6cbbea
	public static final String MEDIASUBTYPE_DOLBY_AC3 = "e06d802c-db46-11cf-b4d1-008005f6cbbea";
	
	// e06d802d-db46-11cf-b4d1-00805f6cbbea
	public static final String MEDIASUBTYPE_DVD_SUBPICTURE = "e06d802d-db46-11cf-b4d1-008005f6cbbea";
	
	// e06d8032-db46-11cf-b4d1-00805f6cbbea
	public static final String MEDIASUBTYPE_DVD_LPCM_AUDIO = "e06d8032-db46-11cf-b4d1-008005f6cbbea";
	
	// e06d8033-db46-11cf-b4d1-00805f6cbbea
	public static final String MEDIASUBTYPE_DTS = "e06d8033-db46-11cf-b4d1-008005f6cbbea";
	
	// e06d8034-db46-11cf-b4d1-00805f6cbbea
	public static final String MEDIASUBTYPE_SDDS = "e06d8034-db46-11cf-b4d1-008005f6cbbea";
	
	// DVD-related mediatypes
	// ED0B916A-044D-11d1-AA78-00C04FC31D60
	public static final String MEDIATYPE_DVD_ENCRYPTED_PACK = "ed0b916a-044d-11d1-aa78-00c004fc31d60";
	
	// e06d802e-db46-11cf-b4d1-00805f6cbbea
	public static final String MEDIATYPE_DVD_NAVIGATION = "e06d802e-db46-11cf-b4d1-008005f6cbbea";
	
	// e06d802f-db46-11cf-b4d1-00805f6cbbea
	public static final String MEDIASUBTYPE_DVD_NAVIGATION_PCI = "e06d802f-db46-11cf-b4d1-008005f6cbbea";
	
	// e06d8030-db46-11cf-b4d1-00805f6cbbea
	public static final String MEDIASUBTYPE_DVD_NAVIGATION_DSI = "e06d8030-db46-11cf-b4d1-008005f6cbbea";
	
	// e06d8031-db46-11cf-b4d1-00805f6cbbea
	public static final String MEDIASUBTYPE_DVD_NAVIGATION_PROVIDER = "e06d8031-db46-11cf-b4d1-008005f6cbbea";
	
	//
	// DVD - MPEG2/AC3-related Formats
	//
	// e06d80e3-db46-11cf-b4d1-00805f6cbbea
	public static final String FORMAT_MPEG2Video = "e06d80e3-db46-11cf-b4d1-008005f6cbbea";
	
	// e06d80e4-db46-11cf-b4d1-00805f6cbbea
	public static final String FORMAT_DolbyAC3 = "e06d80e4-db46-11cf-b4d1-008005f6cbbea";
	
	// e06d80e5-db46-11cf-b4d1-00805f6cbbea
	public static final String FORMAT_MPEG2Audio = "e06d80e5-db46-11cf-b4d1-008005f6cbbea";
	
	// e06d80e6-db46-11cf-b4d1-00805f6cbbea
	public static final String FORMAT_DVD_LPCMAudio = "e06d80e6-db46-11cf-b4d1-008005f6cbbea";
	
	
	//
	// KS Property Set Id (to communicate with the WDM Proxy filter) -- from
	// ksmedia.h of WDM DDK.
	//
	
	// BFABE720-6E1F-11D0-BCF2-444553540000
	public static final String AM_KSPROPSETID_AC3 = "BFABE720-6E1F-11D0-BCF2-444553540000";
	
	// ac390460-43af-11d0-bd6a-003505c103a9
	public static final String AM_KSPROPSETID_DvdSubPic = "ac390460-43af-11d0-bd6a-003505c103a9";
	
	// 0E8A0A40-6AEF-11D0-9ED0-00A024CA19B3
	public static final String AM_KSPROPSETID_CopyProt = "0E8A0A40-6AEF-11D0-9ED0-00A024CA19B3";
	
	// A503C5C0-1D1D-11d1-AD80-444553540000
	public static final String AM_KSPROPSETID_TSRateChange = "a503c5c0-1d1d-11d1-ad80-4445535400";
	
	// 3577EB09-9582-477f-B29C-B0C452A4FF9A
	public static final String AM_KSPROPSETID_DVD_RateChange = "3577eb09-9582-477f-b29c-b0c452a4ff9a";
	
	// ae4720ae-aa71-42d8-b82a-fffdf58b76fd
	public static final String AM_KSPROPSETID_DvdKaraoke = "ae4720ae-aa71-42d8-b82a-fffdf58b76fd";
	
	// c830acbd-ab07-492f-8852-45b6987c2979
	public static final String AM_KSPROPSETID_FrameStep = "c830acbd-ab07-492f-8852-45b6987c2979";
	
	//
	// KS categories from ks.h and ksmedia.h
	//
	//
	
	// 65E8773D-8F56-11D0-A3B9-00A0C9223196
	public static final String AM_KSCATEGORY_CAPTURE = "65E8773D-8F56-11D0-A3B9-00A0C9223196";
	
	// 65E8773E-8F56-11D0-A3B9-00A0C9223196
	public static final String AM_KSCATEGORY_RENDER = "65E8773E-8F56-11D0-A3B9-00A0C9223196";
	
	// 1E84C900-7E70-11D0-A5D6-28DB04C10000
	public static final String AM_KSCATEGORY_DATACOMPRESSOR = "1E84C900-7E70-11D0-A5D6-28DB04C10000";
	
	// 6994AD04-93EF-11D0-A3CC-00A0C9223196
	public static final String AM_KSCATEGORY_AUDIO = "6994AD04-93EF-11D0-A3CC-00A0C9223196";
	
	// 6994AD05-93EF-11D0-A3CC-00A0C9223196
	public static final String AM_KSCATEGORY_VIDEO = "6994AD05-93EF-11D0-A3CC-00A0C9223196";
	
	// a799a800-a46d-11d0-a18c-00a02401dcd4
	public static final String AM_KSCATEGORY_TVTUNER = "a799a800-a46d-11d0-a18c-00a02401dcd4";
	
	// a799a801-a46d-11d0-a18c-00a02401dcd4
	public static final String AM_KSCATEGORY_CROSSBAR = "a799a801-a46d-11d0-a18c-00a02401dcd4";
	
	// a799a802-a46d-11d0-a18c-00a02401dcd4
	public static final String AM_KSCATEGORY_TVAUDIO = "a799a802-a46d-11d0-a18c-00a02401dcd4";
	
	
	// 07dad660-22f1-11d1-a9f4-00c04fbbde8f
	public static final String AM_KSCATEGORY_VBICODEC = "07dad660-22f1-11d1-a9f4-00c04fbbde8f";
	
	// multi-instance safe codec categories(kernel or user mode)
	// {9C24A977-0951-451a-8006-0E49BD28CD5F}
	public static final String AM_KSCATEGORY_VBICODEC_MI = "9c24a977-951-451a-806-e49bd28cd5f";
	
	// 0A4252A0-7E70-11D0-A5D6-28DB04C10000
	public static final String AM_KSCATEGORY_SPLITTER = "0A4252A0-7E70-11D0-A5D6-28DB04C10000";
	
	
	//
	// GUIDs needed to support IKsPin interface
	//
	
	// d3abc7e0-9a61-11d0-a40d00a0c9223196
	public static final String IID_IKsInterfaceHandler = "D3ABC7E0-9A61-11D0-A40D-00A0C9223196";
	
	// 5ffbaa02-49a3-11d0-9f3600aa00a216a1
	public static final String IID_IKsDataTypeHandler = "5FFBAA02-49A3-11D0-9F36-00AA00A216A1";
	
	// b61178d1-a2d9-11cf-9e53-00aa00a216a1
	public static final String IID_IKsPin = "b61178d1-a2d9-11cf-9e53-00aa00a216a1";
	
	// 28F54685-06FD-11D2-B27A-00A0C9223196
	public static final String IID_IKsControl = "28F54685-06FD-11D2-B27A-00A0C9223196";
	
	// CD5EBE6B-8B6E-11D1-8AE0-00A0C9223196
	public static final String IID_IKsPinFactory = "CD5EBE6B-8B6E-11D1-8AE0-00A0C9223196";
	
	// 1A8766A0-62CE-11CF-A5D6-28DB04C10000
	public static final String AM_INTERFACESETID_Standard = "1A8766A0-62CE-11CF-A5D6-28DB04C10000";

}
