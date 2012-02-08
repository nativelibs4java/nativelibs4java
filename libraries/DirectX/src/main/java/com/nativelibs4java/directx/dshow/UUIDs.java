package com.nativelibs4java.directx.dshow;
import org.bridj.*;

/**
 * Common GUID definitions from Microsoft SDK v6.0A (uuids.h)<br>
 * Contains the GUIDs for the MediaType type, subtype fields and format types for standard media types, and also class ids for well-known components.
 */
public class UUIDs {
	/*
	 * Obtained with a regexp replace (jEdit) on C:\Program Files\Microsoft SDKs\Windows\v6.0A\Include\\uuids.h :
	 *   (?m)OUR_GUID_ENTRY\(\s*(\w+)\s*,\s*0x(\w+)\s*,\s*0x(\w+)\s*,\s*0x(\w+)\s*,\s*0x(\w+)\s*,\s*0x(\w+)\s*,\s*0x(\w+)\s*,\s*0x(\w+)\s*,\s*0x(\w+)\s*,\s*0x(\w+)\s*,\s*0x(\w+)\s*,\s*0x(\w+)\s*\)
	 *   "public static final String " + _1 + " = \"" + _2 + "-" + _3 + "-" + _4 + "-" + _5 + _6 + "-" + _7 + _8 + _9 + _10 + _11 + _12 + "\";"
 	 */
 	 
 	public static final String GUID_NULL = "00000000-0000-0000-0000-000000000000";
 	 
	public static final String MEDIATYPE_NULL       = GUID_NULL;
	public static final String MEDIASUBTYPE_NULL    = GUID_NULL;

	
	// -- Use this subtype if you don't have a use for a subtype for your type
	// e436eb8e-524f-11ce-9f53-0020af0ba770            MEDIASUBTYPE_None
	public static final String MEDIASUBTYPE_None = "e436eb8e-524f-11ce-9f53-0020af0ba770";
	
	
	// -- major types ---
	
	
	// 73646976-0000-0010-8000-00AA00389B71  'vids' == MEDIATYPE_Video
	public static final String MEDIATYPE_Video = "73646976-0000-0010-8000-00aa00389b71";
	
	// 73647561-0000-0010-8000-00AA00389B71  'auds' == MEDIATYPE_Audio
	public static final String MEDIATYPE_Audio = "73647561-0000-0010-8000-00aa00389b71";
	
	// 73747874-0000-0010-8000-00AA00389B71  'txts' == MEDIATYPE_Text
	public static final String MEDIATYPE_Text = "73747874-0000-0010-8000-00aa00389b71";
	
	// 7364696D-0000-0010-8000-00AA00389B71  'mids' == MEDIATYPE_Midi
	public static final String MEDIATYPE_Midi = "7364696D-0000-0010-8000-00aa00389b71";
	
	// e436eb83-524f-11ce-9f53-0020af0ba770            MEDIATYPE_Stream
	public static final String MEDIATYPE_Stream = "e436eb83-524f-11ce-9f53-0020af0ba770";
	
	// 73(s)76(v)61(a)69(i)-0000-0010-8000-00AA00389B71  'iavs' == MEDIATYPE_Interleaved
	public static final String MEDIATYPE_Interleaved = "73766169-0000-0010-8000-00aa00389b71";
	
	// 656c6966-0000-0010-8000-00AA00389B71  'file' == MEDIATYPE_File
	public static final String MEDIATYPE_File = "656c6966-0000-0010-8000-00aa00389b71";
	
	// 73636d64-0000-0010-8000-00AA00389B71  'scmd' == MEDIATYPE_ScriptCommand
	public static final String MEDIATYPE_ScriptCommand = "73636d64-0000-0010-8000-00aa00389b71";
	
	// 670AEA80-3A82-11d0-B79B-00AA003767A7            MEDIATYPE_AUXLine21Data
	public static final String MEDIATYPE_AUXLine21Data = "670aea80-3a82-11d0-b79b-0aa03767a7";
	
	
	
	// FB77E152-53B2-499c-B46B-509FC33EDFD7             MEDIATYPE_DTVCCData
	public static final String MEDIATYPE_DTVCCData = "fb77e152-53b2-499c-b46b-509fc33edfd7";
	
	// B88B8A89-B049-4C80-ADCF-5898985E22C1             MEDIATYPE_MSTVCaption
	public static final String MEDIATYPE_MSTVCaption = "B88B8A89-B049-4C80-ADCF-5898985E22C1";
	
	// F72A76E1-EB0A-11D0-ACE4-0000C0CC16BA            MEDIATYPE_VBI
	public static final String MEDIATYPE_VBI = "f72a76e1-eb0a-11d0-ace4-0000c0cc16ba";
	
	// 0482DEE3-7817-11cf-8a03-00aa006ecb65            MEDIATYPE_Timecode
	public static final String MEDIATYPE_Timecode = "482dee3-7817-11cf-8a3-0aa06ecb65";
	
	// 74726c6d-0000-0010-8000-00AA00389B71  'lmrt' == MEDIATYPE_LMRT
	public static final String MEDIATYPE_LMRT = "74726c6d-0000-0010-8000-00aa00389b71";
	
	// 74726c6d-0000-0010-8000-00AA00389B71  'urls' == MEDIATYPE_URL_STREAM
	public static final String MEDIATYPE_URL_STREAM = "736c7275-0000-0010-8000-00aa00389b71";
	
	// -- sub types ---
	
	// 4C504C43-0000-0010-8000-00AA00389B71  'CLPL' == MEDIASUBTYPE_CLPL
	public static final String MEDIASUBTYPE_CLPL = "4C504C43-0000-0010-8000-00aa00389b71";
	
	// 56595559-0000-0010-8000-00AA00389B71  'YUYV' == MEDIASUBTYPE_YUYV
	public static final String MEDIASUBTYPE_YUYV = "56595559-0000-0010-8000-00aa00389b71";
	
	// 56555949-0000-0010-8000-00AA00389B71  'IYUV' == MEDIASUBTYPE_IYUV
	public static final String MEDIASUBTYPE_IYUV = "56555949-0000-0010-8000-00aa00389b71";
	
	// 39555659-0000-0010-8000-00AA00389B71  'YVU9' == MEDIASUBTYPE_YVU9
	public static final String MEDIASUBTYPE_YVU9 = "39555659-0000-0010-8000-00aa00389b71";
	
	// 31313459-0000-0010-8000-00AA00389B71  'Y411' == MEDIASUBTYPE_Y411
	public static final String MEDIASUBTYPE_Y411 = "31313459-0000-0010-8000-00aa00389b71";
	
	// 50313459-0000-0010-8000-00AA00389B71  'Y41P' == MEDIASUBTYPE_Y41P
	public static final String MEDIASUBTYPE_Y41P = "50313459-0000-0010-8000-00aa00389b71";
	
	// 32595559-0000-0010-8000-00AA00389B71  'YUY2' == MEDIASUBTYPE_YUY2
	public static final String MEDIASUBTYPE_YUY2 = "32595559-0000-0010-8000-00aa00389b71";
	
	// 55595659-0000-0010-8000-00AA00389B71  'YVYU' == MEDIASUBTYPE_YVYU
	public static final String MEDIASUBTYPE_YVYU = "55595659-0000-0010-8000-00aa00389b71";
	
	// 59565955-0000-0010-8000-00AA00389B71  'UYVY' ==  MEDIASUBTYPE_UYVY
	public static final String MEDIASUBTYPE_UYVY = "59565955-0000-0010-8000-00aa00389b71";
	
	// 31313259-0000-0010-8000-00AA00389B71  'Y211' ==  MEDIASUBTYPE_Y211
	public static final String MEDIASUBTYPE_Y211 = "31313259-0000-0010-8000-00aa00389b71";
	
	// 524a4c43-0000-0010-8000-00AA00389B71  'CLJR' ==  MEDIASUBTYPE_CLJR
	public static final String MEDIASUBTYPE_CLJR = "524a4c43-0000-0010-8000-00aa00389b71";
	
	// 39304649-0000-0010-8000-00AA00389B71  'IF09' ==  MEDIASUBTYPE_IF09
	public static final String MEDIASUBTYPE_IF09 = "39304649-0000-0010-8000-00aa00389b71";
	
	// 414c5043-0000-0010-8000-00AA00389B71  'CPLA' ==  MEDIASUBTYPE_CPLA
	public static final String MEDIASUBTYPE_CPLA = "414c5043-0000-0010-8000-00aa00389b71";
	
	// 47504A4D-0000-0010-8000-00AA00389B71            MEDIASUBTYPE_MJPG
	public static final String MEDIASUBTYPE_MJPG = "47504A4D-0000-0010-8000-00aa00389b71";
	
	// 4A4D5654-0000-0010-8000-00AA00389B71            MEDIASUBTYPE_TVMJ
	public static final String MEDIASUBTYPE_TVMJ = "4A4D5654-0000-0010-8000-00aa00389b71";
	
	// 454B4157-0000-0010-8000-00AA00389B71            MEDIASUBTYPE_WAKE
	public static final String MEDIASUBTYPE_WAKE = "454B4157-0000-0010-8000-00aa00389b71";
	
	// 43434643-0000-0010-8000-00AA00389B71            MEDIASUBTYPE_CFCC
	public static final String MEDIASUBTYPE_CFCC = "43434643-0000-0010-8000-00aa00389b71";
	
	// 47504A49-0000-0010-8000-00AA00389B71            MEDIASUBTYPE_IJPG
	public static final String MEDIASUBTYPE_IJPG = "47504A49-0000-0010-8000-00aa00389b71";
	
	// 6D756C50-0000-0010-8000-00AA00389B71            MEDIASUBTYPE_Plum
	public static final String MEDIASUBTYPE_Plum = "6D756C50-0000-0010-8000-00aa00389b71";
	
	// FAST DV-Master
	// 53435644-0000-0010-8000-00AA00389B71            MEDIASUBTYPE_DVCS
	public static final String MEDIASUBTYPE_DVCS = "53435644-0000-0010-8000-00aa00389b71";
	
	// H.264 compressed video stream
	// 34363248-0000-0010-8000-00AA00389B71  'H264' == MEDIASUBTYPE_H264
	public static final String MEDIASUBTYPE_H264 = "34363248-0000-0010-8000-00aa00389b71";
	
	// FAST DV-Master
	// 44535644-0000-0010-8000-00AA00389B71            MEDIASUBTYPE_DVSD
	public static final String MEDIASUBTYPE_DVSD = "44535644-0000-0010-8000-00aa00389b71";
	
	// MIROVideo DV
	// 4656444D-0000-0010-8000-00AA00389B71            MEDIASUBTYPE_MDVF
	public static final String MEDIASUBTYPE_MDVF = "4656444D-0000-0010-8000-00aa00389b71";
	
	// e436eb78-524f-11ce-9f53-0020af0ba770            MEDIASUBTYPE_RGB1
	// e436eb78-524f-11ce-9f53-0020af0ba770            MEDIASUBTYPE_RGB1
	public static final String MEDIASUBTYPE_RGB1 = "e436eb78-524f-11ce-9f53-0020af0ba770";
	
	// e436eb79-524f-11ce-9f53-0020af0ba770            MEDIASUBTYPE_RGB4
	public static final String MEDIASUBTYPE_RGB4 = "e436eb79-524f-11ce-9f53-0020af0ba770";
	
	// e436eb7a-524f-11ce-9f53-0020af0ba770            MEDIASUBTYPE_RGB8
	public static final String MEDIASUBTYPE_RGB8 = "e436eb7a-524f-11ce-9f53-0020af0ba770";
	
	// e436eb7b-524f-11ce-9f53-0020af0ba770            MEDIASUBTYPE_RGB565
	public static final String MEDIASUBTYPE_RGB565 = "e436eb7b-524f-11ce-9f53-0020af0ba770";
	
	// e436eb7c-524f-11ce-9f53-0020af0ba770            MEDIASUBTYPE_RGB555
	public static final String MEDIASUBTYPE_RGB555 = "e436eb7c-524f-11ce-9f53-0020af0ba770";
	
	// e436eb7d-524f-11ce-9f53-0020af0ba770            MEDIASUBTYPE_RGB24
	public static final String MEDIASUBTYPE_RGB24 = "e436eb7d-524f-11ce-9f53-0020af0ba770";
	
	// e436eb7e-524f-11ce-9f53-0020af0ba770            MEDIASUBTYPE_RGB32
	public static final String MEDIASUBTYPE_RGB32 = "e436eb7e-524f-11ce-9f53-0020af0ba770";
	
	
	//
	// RGB surfaces that contain per pixel alpha values.
	//
	
	// 297C55AF-E209-4cb3-B757-C76D6B9C88A8            MEDIASUBTYPE_ARGB1555
	public static final String MEDIASUBTYPE_ARGB1555 = "297c55af-e209-4cb3-b757-c76d6b9c88a8";
	
	// 6E6415E6-5C24-425f-93CD-80102B3D1CCA            MEDIASUBTYPE_ARGB4444
	public static final String MEDIASUBTYPE_ARGB4444 = "6e6415e6-5c24-425f-93cd-80102b3d1cca";
	
	// 773c9ac0-3274-11d0-B724-00aa006c1A01            MEDIASUBTYPE_ARGB32
	public static final String MEDIASUBTYPE_ARGB32 = "773c9ac0-3274-11d0-b724-0aa06c1a1";
	
	
	// 2f8bb76d-b644-4550-acf3-d30caa65d5c5            MEDIASUBTYPE_A2R10G10B10
	public static final String MEDIASUBTYPE_A2R10G10B10 = "2f8bb76d-b644-4550-acf3-d30caa65d5c5";
	
	// 576f7893-bdf6-48c4-875f-ae7b81834567            MEDIASUBTYPE_A2B10G10R10
	public static final String MEDIASUBTYPE_A2B10G10R10 = "576f7893-bdf6-48c4-875f-ae7b81834567";
	
	
	// 56555941-0000-0010-8000-00AA00389B71  'AYUV' == MEDIASUBTYPE_AYUV
	//
	// See the DX-VA header and documentation for a description of this format.
	//
	public static final String MEDIASUBTYPE_AYUV = "56555941-0000-0010-8000-00aa00389b71";
	
	// 34344941-0000-0010-8000-00AA00389B71  'AI44' == MEDIASUBTYPE_AI44
	//
	// See the DX-VA header and documentation for a description of this format.
	//
	public static final String MEDIASUBTYPE_AI44 = "34344941-0000-0010-8000-00aa00389b71";
	
	// 34344149-0000-0010-8000-00AA00389B71  'IA44' == MEDIASUBTYPE_IA44
	//
	// See the DX-VA header and documentation for a description of this format.
	//
	public static final String MEDIASUBTYPE_IA44 = "34344149-0000-0010-8000-00aa00389b71";
	
	
	//
	// DirectX7 D3D Render Target media subtypes.
	//
	
	// 32335237-0000-0010-8000-00AA00389B71  '7R32' == MEDIASUBTYPE_RGB32_D3D_DX7_RT
	public static final String MEDIASUBTYPE_RGB32_D3D_DX7_RT = "32335237-0000-0010-8000-00aa00389b71";
	
	// 36315237-0000-0010-8000-00AA00389B71  '7R16' == MEDIASUBTYPE_RGB16_D3D_DX7_RT
	public static final String MEDIASUBTYPE_RGB16_D3D_DX7_RT = "36315237-0000-0010-8000-00aa00389b71";
	
	// 38384137-0000-0010-8000-00AA00389B71  '7A88' == MEDIASUBTYPE_ARGB32_D3D_DX7_RT
	public static final String MEDIASUBTYPE_ARGB32_D3D_DX7_RT = "38384137-0000-0010-8000-00aa00389b71";
	
	// 34344137-0000-0010-8000-00AA00389B71  '7A44' == MEDIASUBTYPE_ARGB4444_D3D_DX7_RT
	public static final String MEDIASUBTYPE_ARGB4444_D3D_DX7_RT = "34344137-0000-0010-8000-00aa00389b71";
	
	// 35314137-0000-0010-8000-00AA00389B71  '7A15' == MEDIASUBTYPE_ARGB1555_D3D_DX7_RT
	public static final String MEDIASUBTYPE_ARGB1555_D3D_DX7_RT = "35314137-0000-0010-8000-00aa00389b71";
	
	
	//
	// DirectX9 D3D Render Target media subtypes.
	//
	
	// 32335239-0000-0010-8000-00AA00389B71  '9R32' == MEDIASUBTYPE_RGB32_D3D_DX9_RT
	public static final String MEDIASUBTYPE_RGB32_D3D_DX9_RT = "32335239-0000-0010-8000-00aa00389b71";
	
	// 36315239-0000-0010-8000-00AA00389B71  '9R16' == MEDIASUBTYPE_RGB16_D3D_DX9_RT
	public static final String MEDIASUBTYPE_RGB16_D3D_DX9_RT = "36315239-0000-0010-8000-00aa00389b71";
	
	// 38384139-0000-0010-8000-00AA00389B71  '9A88' == MEDIASUBTYPE_ARGB32_D3D_DX9_RT
	public static final String MEDIASUBTYPE_ARGB32_D3D_DX9_RT = "38384139-0000-0010-8000-00aa00389b71";
	
	// 34344139-0000-0010-8000-00AA00389B71  '9A44' == MEDIASUBTYPE_ARGB4444_D3D_DX9_RT
	public static final String MEDIASUBTYPE_ARGB4444_D3D_DX9_RT = "34344139-0000-0010-8000-00aa00389b71";
	
	// 35314139-0000-0010-8000-00AA00389B71  '9A15' == MEDIASUBTYPE_ARGB1555_D3D_DX9_RT
	public static final String MEDIASUBTYPE_ARGB1555_D3D_DX9_RT = "35314139-0000-0010-8000-00aa00389b71";
	
	/*
	#define MEDIASUBTYPE_HASALPHA(mt) ( ((mt).subtype == MEDIASUBTYPE_ARGB4444)            || \
										((mt).subtype == MEDIASUBTYPE_ARGB32)              || \
										((mt).subtype == MEDIASUBTYPE_AYUV)                || \
										((mt).subtype == MEDIASUBTYPE_AI44)                || \
										((mt).subtype == MEDIASUBTYPE_IA44)                || \
										((mt).subtype == MEDIASUBTYPE_ARGB1555)            || \
										((mt).subtype == MEDIASUBTYPE_ARGB32_D3D_DX7_RT)   || \
										((mt).subtype == MEDIASUBTYPE_ARGB4444_D3D_DX7_RT) || \
										((mt).subtype == MEDIASUBTYPE_ARGB1555_D3D_DX7_RT) || \
										((mt).subtype == MEDIASUBTYPE_ARGB32_D3D_DX9_RT)   || \
										((mt).subtype == MEDIASUBTYPE_ARGB4444_D3D_DX9_RT) || \
										((mt).subtype == MEDIASUBTYPE_ARGB1555_D3D_DX9_RT) )
	
	#define MEDIASUBTYPE_HASALPHA7(mt) (((mt).subtype == MEDIASUBTYPE_ARGB32_D3D_DX7_RT)   || \
										((mt).subtype == MEDIASUBTYPE_ARGB4444_D3D_DX7_RT) || \
										((mt).subtype == MEDIASUBTYPE_ARGB1555_D3D_DX7_RT) )
	
	#define MEDIASUBTYPE_D3D_DX7_RT(mt) (((mt).subtype == MEDIASUBTYPE_ARGB32_D3D_DX7_RT)   || \
										 ((mt).subtype == MEDIASUBTYPE_ARGB4444_D3D_DX7_RT) || \
										 ((mt).subtype == MEDIASUBTYPE_ARGB1555_D3D_DX7_RT) || \
										 ((mt).subtype == MEDIASUBTYPE_RGB32_D3D_DX7_RT)    || \
										 ((mt).subtype == MEDIASUBTYPE_RGB16_D3D_DX7_RT))
	
	#define MEDIASUBTYPE_HASALPHA9(mt) (((mt).subtype == MEDIASUBTYPE_ARGB32_D3D_DX9_RT)   || \
										((mt).subtype == MEDIASUBTYPE_ARGB4444_D3D_DX9_RT) || \
										((mt).subtype == MEDIASUBTYPE_ARGB1555_D3D_DX9_RT) )
	
	
	#define MEDIASUBTYPE_D3D_DX9_RT(mt) (((mt).subtype == MEDIASUBTYPE_ARGB32_D3D_DX9_RT)   || \
										 ((mt).subtype == MEDIASUBTYPE_ARGB4444_D3D_DX9_RT) || \
										 ((mt).subtype == MEDIASUBTYPE_ARGB1555_D3D_DX9_RT) || \
										 ((mt).subtype == MEDIASUBTYPE_RGB32_D3D_DX9_RT)    || \
										 ((mt).subtype == MEDIASUBTYPE_RGB16_D3D_DX9_RT))
	*/
	
	//
	// DX-VA uncompressed surface formats
	//
	
	// 32315659-0000-0010-8000-00AA00389B71  'YV12' ==  MEDIASUBTYPE_YV12
	public static final String MEDIASUBTYPE_YV12 = "32315659-0000-0010-8000-00aa00389b71";
	
	// 3231564E-0000-0010-8000-00AA00389B71  'NV12' ==  MEDIASUBTYPE_NV12
	public static final String MEDIASUBTYPE_NV12 = "3231564E-0000-0010-8000-00aa00389b71";
	
	// 3231564E-0000-0010-8000-00AA00389B71  'NV24' ==  MEDIASUBTYPE_NV24
	public static final String MEDIASUBTYPE_NV24 = "3432564E-0000-0010-8000-00aa00389b71";
	
	// 31434D49-0000-0010-8000-00AA00389B71  'IMC1' ==  MEDIASUBTYPE_IMC1
	public static final String MEDIASUBTYPE_IMC1 = "31434D49-0000-0010-8000-00aa00389b71";
	
	// 32434d49-0000-0010-8000-00AA00389B71  'IMC2' ==  MEDIASUBTYPE_IMC2
	public static final String MEDIASUBTYPE_IMC2 = "32434D49-0000-0010-8000-00aa00389b71";
	
	// 33434d49-0000-0010-8000-00AA00389B71  'IMC3' ==  MEDIASUBTYPE_IMC3
	public static final String MEDIASUBTYPE_IMC3 = "33434D49-0000-0010-8000-00aa00389b71";
	
	// 34434d49-0000-0010-8000-00AA00389B71  'IMC4' ==  MEDIASUBTYPE_IMC4
	public static final String MEDIASUBTYPE_IMC4 = "34434D49-0000-0010-8000-00aa00389b71";
	
	// 30343353-0000-0010-8000-00AA00389B71  'S340' ==  MEDIASUBTYPE_S340
	public static final String MEDIASUBTYPE_S340 = "30343353-0000-0010-8000-00aa00389b71";
	
	// 32343353-0000-0010-8000-00AA00389B71  'S342' ==  MEDIASUBTYPE_S342
	public static final String MEDIASUBTYPE_S342 = "32343353-0000-0010-8000-00aa00389b71";
	
	
	// e436eb7f-524f-11ce-9f53-0020af0ba770            MEDIASUBTYPE_Overlay
	public static final String MEDIASUBTYPE_Overlay = "e436eb7f-524f-11ce-9f53-0020af0ba770";
	
	// e436eb80-524f-11ce-9f53-0020af0ba770            MEDIASUBTYPE_MPEGPacket
	public static final String MEDIASUBTYPE_MPEG1Packet = "e436eb80-524f-11ce-9f53-0020af0ba770";
	
	// e436eb81-524f-11ce-9f53-0020af0ba770            MEDIASUBTYPE_MPEG1Payload
	public static final String MEDIASUBTYPE_MPEG1Payload = "e436eb81-524f-11ce-9f53-0020af0ba770";
	
	// 00000050-0000-0010-8000-00AA00389B71         MEDIASUBTYPE_MPEG1AudioPayload
	public static final String MEDIASUBTYPE_MPEG1AudioPayload = "00000050-0000-0010-8000-00AA00389B71";
	
	// e436eb82-524f-11ce-9f53-0020af0ba770            MEDIASUBTYPE_MPEG1SystemStream
	public static final String MEDIATYPE_MPEG1SystemStream = "e436eb82-524f-11ce-9f53-0020af0ba770";
	
	// the next consecutive number is assigned to MEDIATYPE_Stream and appears higher up
	// e436eb84-524f-11ce-9f53-0020af0ba770            MEDIASUBTYPE_MPEG1System
	public static final String MEDIASUBTYPE_MPEG1System = "e436eb84-524f-11ce-9f53-0020af0ba770";
	
	// e436eb85-524f-11ce-9f53-0020af0ba770            MEDIASUBTYPE_MPEG1VideoCD
	public static final String MEDIASUBTYPE_MPEG1VideoCD = "e436eb85-524f-11ce-9f53-0020af0ba770";
	
	// e436eb86-524f-11ce-9f53-0020af0ba770            MEDIASUBTYPE_MPEG1Video
	public static final String MEDIASUBTYPE_MPEG1Video = "e436eb86-524f-11ce-9f53-0020af0ba770";
	
	// e436eb87-524f-11ce-9f53-0020af0ba770            MEDIASUBTYPE_MPEG1Audio
	public static final String MEDIASUBTYPE_MPEG1Audio = "e436eb87-524f-11ce-9f53-0020af0ba770";
	
	// e436eb88-524f-11ce-9f53-0020af0ba770            MEDIASUBTYPE_Avi
	public static final String MEDIASUBTYPE_Avi = "e436eb88-524f-11ce-9f53-0020af0ba770";
	
	// {3DB80F90-9412-11d1-ADED-0000F8754B99}          MEDIASUBTYPE_Asf
	public static final String MEDIASUBTYPE_Asf = "3db80f90-9412-11d1-aded-00f8754b99";
	
	// e436eb89-524f-11ce-9f53-0020af0ba770            MEDIASUBTYPE_QTMovie
	public static final String MEDIASUBTYPE_QTMovie = "e436eb89-524f-11ce-9f53-0020af0ba770";
	
	// 617a7072-0000-0010-8000-00AA00389B71         MEDIASUBTYPE_Rpza
	public static final String MEDIASUBTYPE_QTRpza = "617a7072-0000-0010-8000-00aa00389b71";
	
	// 20636d73-0000-0010-8000-00AA00389B71         MEDIASUBTYPE_Smc
	public static final String MEDIASUBTYPE_QTSmc = "20636d73-0000-0010-8000-00aa00389b71";
	
	// 20656c72-0000-0010-8000-00AA00389B71        MEDIASUBTYPE_Rle
	public static final String MEDIASUBTYPE_QTRle = "20656c72-0000-0010-8000-00aa00389b71";
	
	// 6765706a-0000-0010-8000-00AA00389B71        MEDIASUBTYPE_Jpeg
	public static final String MEDIASUBTYPE_QTJpeg = "6765706a-0000-0010-8000-00aa00389b71";
	
	// e436eb8a-524f-11ce-9f53-0020af0ba770            MEDIASUBTYPE_PCMAudio_Obsolete
	public static final String MEDIASUBTYPE_PCMAudio_Obsolete = "e436eb8a-524f-11ce-9f53-0020af0ba770";
	
	// 00000001-0000-0010-8000-00AA00389B71            MEDIASUBTYPE_PCM
	public static final String MEDIASUBTYPE_PCM = "00000001-0000-0010-8000-00AA00389B71";
	
	// e436eb8b-524f-11ce-9f53-0020af0ba770            MEDIASUBTYPE_WAVE
	public static final String MEDIASUBTYPE_WAVE = "e436eb8b-524f-11ce-9f53-0020af0ba770";
	
	// e436eb8c-524f-11ce-9f53-0020af0ba770            MEDIASUBTYPE_AU
	public static final String MEDIASUBTYPE_AU = "e436eb8c-524f-11ce-9f53-0020af0ba770";
	
	// e436eb8d-524f-11ce-9f53-0020af0ba770            MEDIASUBTYPE_AIFF
	public static final String MEDIASUBTYPE_AIFF = "e436eb8d-524f-11ce-9f53-0020af0ba770";
	
	// 64(d)73(s)76(v)64(d)-0000-0010-8000-00AA00389B71  'dvsd' == MEDIASUBTYPE_dvsd
	public static final String MEDIASUBTYPE_dvsd = "64737664-0000-0010-8000-00aa00389b71";
	
	// 64(d)68(h)76(v)64(d)-0000-0010-8000-00AA00389B71  'dvhd' == MEDIASUBTYPE_dvhd
	public static final String MEDIASUBTYPE_dvhd = "64687664-0000-0010-8000-00aa00389b71";
	
	// 6c(l)73(s)76(v)64(d)-0000-0010-8000-00AA00389B71  'dvsl' == MEDIASUBTYPE_dvsl
	public static final String MEDIASUBTYPE_dvsl = "6c737664-0000-0010-8000-00aa00389b71";
	
	// 35(5)32(2)76(v)64(d)-0000-0010-8000-00AA00389B71  'dv25' ==  MEDIASUBTYPE_dv25
	public static final String MEDIASUBTYPE_dv25 = "35327664-0000-0010-8000-00aa00389b71";
	
	// 30(0)35(5)76(v)64(d)-0000-0010-8000-00AA00389B71  'dv50' ==  MEDIASUBTYPE_dv50
	public static final String MEDIASUBTYPE_dv50 = "30357664-0000-0010-8000-00aa00389b71";
	
	// 31(1)68(h)76(v)64(d)-0000-0010-8000-00AA00389B71  'dvh1' ==  MEDIASUBTYPE_dvh1
	public static final String MEDIASUBTYPE_dvh1 = "31687664-0000-0010-8000-00aa00389b71";
	
	// 6E8D4A22-310C-11d0-B79A-00AA003767A7         MEDIASUBTYPE_Line21_BytePair
	public static final String MEDIASUBTYPE_Line21_BytePair = "6e8d4a22-310c-11d0-b79a-0aa03767a7";
	
	// 6E8D4A23-310C-11d0-B79A-00AA003767A7         MEDIASUBTYPE_Line21_GOPPacket
	public static final String MEDIASUBTYPE_Line21_GOPPacket = "6e8d4a23-310c-11d0-b79a-0aa03767a7";
	
	// 6E8D4A24-310C-11d0-B79A-00AA003767A7         MEDIASUBTYPE_Line21_VBIRawData
	public static final String MEDIASUBTYPE_Line21_VBIRawData = "6e8d4a24-310c-11d0-b79a-0aa03767a7";
	
	//0AF414BC-4ED2-445e-9839-8F095568AB3C          MEDIASUBTYPE_708_608Data
	public static final String MEDIASUBTYPE_708_608Data = "af414bc-4ed2-445e-9839-8f95568ab3c";
	
	// F52ADDAA-36F0-43F5-95EA-6D866484262A         MEDIASUBTYPE_DtvCcData
	public static final String MEDIASUBTYPE_DtvCcData = "F52ADDAA-36F0-43F5-95EA-6D866484262A";
	
	// F72A76E3-EB0A-11D0-ACE4-0000C0CC16BA         MEDIASUBTYPE_TELETEXT
	public static final String MEDIASUBTYPE_TELETEXT = "f72a76e3-eb0a-11d0-ace4-0000c0cc16ba";
	
	// 2791D576-8E7A-466F-9E90-5D3F3083738B        MEDIASUBTYPE_WSS
	public static final String MEDIASUBTYPE_WSS = "2791D576-8E7A-466F-9E90-5D3F3083738B";
	
	// A1B3F620-9792-4d8d-81A4-86AF25772090        MEDIASUBTYPE_VPS
	public static final String MEDIASUBTYPE_VPS = "a1b3f620-9792-4d8d-81a4-86af25772090";
	
	// derived from WAVE_FORMAT_DRM
	// 00000009-0000-0010-8000-00aa00389b71
	public static final String MEDIASUBTYPE_DRM_Audio = "00000009-0000-0010-8000-00aa00389b71";
	
	// derived from WAVE_FORMAT_IEEE_FLOAT
	// 00000003-0000-0010-8000-00aa00389b71
	public static final String MEDIASUBTYPE_IEEE_FLOAT = "00000003-0000-0010-8000-00aa00389b71";
	
	// derived from WAVE_FORMAT_DOLBY_AC3_SPDIF
	// 00000092-0000-0010-8000-00aa00389b71
	public static final String MEDIASUBTYPE_DOLBY_AC3_SPDIF = "00000092-0000-0010-8000-00aa00389b71";
	
	// derived from WAVE_FORMAT_RAW_SPORT
	// 00000240-0000-0010-8000-00aa00389b71
	public static final String MEDIASUBTYPE_RAW_SPORT = "00000240-0000-0010-8000-00aa00389b71";
	
	// derived from wave format tag 0x241, call it SPDIF_TAG_241h for now
	// 00000241-0000-0010-8000-00aa00389b71
	public static final String MEDIASUBTYPE_SPDIF_TAG_241h = "00000241-0000-0010-8000-00aa00389b71";
	
	
	
	// DirectShow DSS definitions
	
	// A0AF4F81-E163-11d0-BAD9-00609744111A
	public static final String MEDIASUBTYPE_DssVideo = "a0af4f81-e163-11d0-bad9-0609744111a";
	
	// A0AF4F82-E163-11d0-BAD9-00609744111A
	public static final String MEDIASUBTYPE_DssAudio = "a0af4f82-e163-11d0-bad9-0609744111a";
	
	// 5A9B6A40-1A22-11D1-BAD9-00609744111A
	public static final String MEDIASUBTYPE_VPVideo = "5a9b6a40-1a22-11d1-bad9-0609744111a";
	
	// 5A9B6A41-1A22-11D1-BAD9-00609744111A
	public static final String MEDIASUBTYPE_VPVBI = "5a9b6a41-1a22-11d1-bad9-0609744111a";
	
	// BF87B6E0-8C27-11d0-B3F0-00AA003761C5     Capture graph building
	public static final String CLSID_CaptureGraphBuilder = "BF87B6E0-8C27-11d0-B3F0-0AA003761C5";
	
	// BF87B6E1-8C27-11d0-B3F0-00AA003761C5     New Capture graph building
	public static final String CLSID_CaptureGraphBuilder2 = "BF87B6E1-8C27-11d0-B3F0-0AA003761C5";
	
	// e436ebb0-524f-11ce-9f53-0020af0ba770            Prototype filtergraph
	public static final String CLSID_ProtoFilterGraph = "e436ebb0-524f-11ce-9f53-0020af0ba770";
	
	// e436ebb1-524f-11ce-9f53-0020af0ba770            Reference clock
	public static final String CLSID_SystemClock = "e436ebb1-524f-11ce-9f53-0020af0ba770";
	
	// e436ebb2-524f-11ce-9f53-0020af0ba770           Filter Mapper
	public static final String CLSID_FilterMapper = "e436ebb2-524f-11ce-9f53-0020af0ba770";
	
	// e436ebb3-524f-11ce-9f53-0020af0ba770           Filter Graph
	public static final String CLSID_FilterGraph = "e436ebb3-524f-11ce-9f53-0020af0ba770";
	
	// e436ebb8-524f-11ce-9f53-0020af0ba770           Filter Graph no thread
	public static final String CLSID_FilterGraphNoThread = "e436ebb8-524f-11ce-9f53-0020af0ba770";
	
	// a3ecbc41-581a-4476-b693-a63340462d8b
	public static final String CLSID_FilterGraphPrivateThread = "a3ecbc41-581a-4476-b693-a63340462d8b";
	
	// e4bbd160-4269-11ce-838d-00aa0055595a           MPEG System stream
	public static final String CLSID_MPEG1Doc = "e4bbd160-4269-11ce-838d-0aa055595a";
	
	// 701722e0-8ae3-11ce-a85c-00aa002feab5           MPEG file reader
	public static final String CLSID_FileSource = "701722e0-8ae3-11ce-a85c-00aa002feab5";
	
	// 26C25940-4CA9-11ce-A828-00AA002FEAB5           Takes MPEG1 packets as input
	public static final String CLSID_MPEG1PacketPlayer = "26c25940-4ca9-11ce-a828-0aa02feab5";
	
	// 336475d0-942a-11ce-a870-00aa002feab5           MPEG splitter
	public static final String CLSID_MPEG1Splitter = "336475d0-942a-11ce-a870-00aa002feab5";
	
	// feb50740-7bef-11ce-9bd9-0000e202599c           MPEG video decoder
	public static final String CLSID_CMpegVideoCodec = "feb50740-7bef-11ce-9bd9-00e22599c";
	
	// 4a2286e0-7bef-11ce-9bd9-0000e202599c           MPEG audio decoder
	public static final String CLSID_CMpegAudioCodec = "4a2286e0-7bef-11ce-9bd9-00e22599c";
	
	// e30629d3-27e5-11ce-875d-00608cb78066           Text renderer
	public static final String CLSID_TextRender = "e30629d3-27e5-11ce-875d-0608cb78066";
	
	
	
	// {F8388A40-D5BB-11d0-BE5A-0080C706568E}
	public static final String CLSID_InfTee = "f8388a40-d5bb-11d0-be5a-080c76568e";
	
	// 1b544c20-fd0b-11ce-8c63-00aa0044b51e           Avi Stream Splitter
	public static final String CLSID_AviSplitter = "1b544c20-fd0b-11ce-8c63-0aa0044b51e";
	
	// 1b544c21-fd0b-11ce-8c63-00aa0044b51e           Avi File Reader
	public static final String CLSID_AviReader = "1b544c21-fd0b-11ce-8c63-0aa0044b51e";
	
	// 1b544c22-fd0b-11ce-8c63-00aa0044b51e           Vfw 2.0 Capture Driver
	public static final String CLSID_VfwCapture = "1b544c22-fd0b-11ce-8c63-0aa0044b51e";
	
	public static final String CLSID_CaptureProperties = "1B544c22-FD0B-11ce-8C63-00AA0044B51F";
	
	//e436ebb4-524f-11ce-9f53-0020af0ba770            Control Distributor
	public static final String CLSID_FGControl = "e436ebb4-524f-11ce-9f53-0020af0ba770";
	
	// 44584800-F8EE-11ce-B2D4-00DD01101B85           .MOV reader (old)
	public static final String CLSID_MOVReader = "44584800-f8ee-11ce-b2d4-00dd1101b85";
	
	// D51BD5A0-7548-11cf-A520-0080C77EF58A           QT Splitter
	public static final String CLSID_QuickTimeParser = "d51bd5a0-7548-11cf-a520-080c77ef58a";
	
	// FDFE9681-74A3-11d0-AFA7-00AA00B67A42           QT Decoder
	public static final String CLSID_QTDec = "fdfe9681-74a3-11d0-afa7-0aa0b67a42";
	
	// D3588AB0-0781-11ce-B03A-0020AF0BA770           AVIFile-based reader
	public static final String CLSID_AVIDoc = "d3588ab0-0781-11ce-b03a-0020afba770";
	
	// 70e102b0-5556-11ce-97c0-00aa0055595a           Video renderer
	public static final String CLSID_VideoRenderer = "70e102b0-5556-11ce-97c0-00aa0055595a";
	
	// 1643e180-90f5-11ce-97d5-00aa0055595a           Colour space convertor
	public static final String CLSID_Colour = "1643e180-90f5-11ce-97d5-00aa0055595a";
	
	// 1da08500-9edc-11cf-bc10-00aa00ac74f6           VGA 16 color ditherer
	public static final String CLSID_Dither = "1da08500-9edc-11cf-bc10-00aa00ac74f6";
	
	// 07167665-5011-11cf-BF33-00AA0055595A           Modex video renderer
	public static final String CLSID_ModexRenderer = "7167665-5011-11cf-bf33-0aa055595a";
	
	// e30629d1-27e5-11ce-875d-00608cb78066           Waveout audio renderer
	public static final String CLSID_AudioRender = "e30629d1-27e5-11ce-875d-0608cb78066";
	
	// 05589faf-c356-11ce-bf01-00aa0055595a           Audio Renderer Property Page
	public static final String CLSID_AudioProperties = "05589faf-c356-11ce-bf01-0aa055595a";
	
	// 79376820-07D0-11cf-A24D-0020AFD79767           DSound audio renderer
	public static final String CLSID_DSoundRender = "79376820-07D0-11CF-A24D-020AFD79767";
	
	// e30629d2-27e5-11ce-875d-00608cb78066           Wavein audio recorder
	public static final String CLSID_AudioRecord = "e30629d2-27e5-11ce-875d-0608cb78066";
	
	// {2CA8CA52-3C3F-11d2-B73D-00C04FB6BD3D}         IAMAudioInputMixer property page
	public static final String CLSID_AudioInputMixerProperties = "2ca8ca52-3c3f-11d2-b73d-0c04fb6bd3d";
	
	// {CF49D4E0-1115-11ce-B03A-0020AF0BA770}         AVI Decoder
	public static final String CLSID_AVIDec = "cf49d4e0-1115-11ce-b03a-020afba770";
	
	// {A888DF60-1E90-11cf-AC98-00AA004C0FA9}         AVI ICDraw* wrapper
	public static final String CLSID_AVIDraw = "a888df60-1e90-11cf-ac98-0aa04cfa9";
	
	// 6a08cf80-0e18-11cf-a24d-0020afd79767       ACM Wrapper
	public static final String CLSID_ACMWrapper = "6a08cf80-0e18-11cf-a24d-020afd79767";
	
	// {e436ebb5-524f-11ce-9f53-0020af0ba770}    Async File Reader
	public static final String CLSID_AsyncReader = "e436ebb5-524f-11ce-9f53-0020af0ba770";
	
	// {e436ebb6-524f-11ce-9f53-0020af0ba770}    Async URL Reader
	public static final String CLSID_URLReader = "e436ebb6-524f-11ce-9f53-0020af0ba770";
	
	// {e436ebb7-524f-11ce-9f53-0020af0ba770}    IPersistMoniker PID
	public static final String CLSID_PersistMonikerPID = "e436ebb7-524f-11ce-9f53-0020af0ba770";
	
	// {D76E2820-1563-11cf-AC98-00AA004C0FA9}
	public static final String CLSID_AVICo = "d76e2820-1563-11cf-ac98-0aa04cfa9";
	
	// {8596E5F0-0DA5-11d0-BD21-00A0C911CE86}
	public static final String CLSID_FileWriter = "8596e5f0-da5-11d0-bd21-0a0c911ce86";
	
	// {E2510970-F137-11CE-8B67-00AA00A3F1A6}     AVI mux filter
	public static final String CLSID_AviDest = "e2510970-f137-11ce-8b67-0aa0a3f1a6";
	
	// {C647B5C0-157C-11d0-BD23-00A0C911CE86}
	public static final String CLSID_AviMuxProptyPage = "c647b5c0-157c-11d0-bd23-0a0c911ce86";
	
	// {0A9AE910-85C0-11d0-BD42-00A0C911CE86}
	public static final String CLSID_AviMuxProptyPage1 = "a9ae910-85c0-11d0-bd42-0a0c911ce86";
	
	// {07b65360-c445-11ce-afde-00aa006c14f4}
	public static final String CLSID_AVIMIDIRender = "07b65360-c445-11ce-afde-00aa006c14f4";
	
	// {187463A0-5BB7-11d3-ACBE-0080C75E246E}    WMSDK-based ASF reader
	public static final String CLSID_WMAsfReader = "187463a0-5bb7-11d3-acbe-080c75e246e";
	
	// {7c23220e-55bb-11d3-8b16-00c04fb6bd3d}    WMSDK-based ASF writer
	public static final String CLSID_WMAsfWriter = "7c23220e-55bb-11d3-8b16-0c04fb6bd3d";
	
	//  {afb6c280-2c41-11d3-8a60-0000f81e0e4a}
	public static final String CLSID_MPEG2Demultiplexer = "afb6c280-2c41-11d3-8a60-0000f81e0e4a";
	
	// {3ae86b20-7be8-11d1-abe6-00a0c905f375}
	public static final String CLSID_MMSPLITTER = "3ae86b20-7be8-11d1-abe6-00a0c905f375";
	
	// {2DB47AE5-CF39-43c2-B4D6-0CD8D90946F4}
	public static final String CLSID_StreamBufferSink = "2db47ae5-cf39-43c2-b4d6-cd8d9946f4";
	
	// {C9F5FE02-F851-4eb5-99EE-AD602AF1E619}
	public static final String CLSID_StreamBufferSource = "c9f5fe02-f851-4eb5-99ee-ad602af1e619";
	
	// {FA8A68B2-C864-4ba2-AD53-D3876A87494B}
	public static final String CLSID_StreamBufferConfig = "fa8a68b2-c864-4ba2-ad53-d3876a87494b";
	
	// {6CFAD761-735D-4aa5-8AFC-AF91A7D61EBA}
	public static final String CLSID_Mpeg2VideoStreamAnalyzer = "6cfad761-735d-4aa5-8afc-af91a7d61eba";
	
	// {CCAA63AC-1057-4778-AE92-1206AB9ACEE6}
	public static final String CLSID_StreamBufferRecordingAttributes = "ccaa63ac-1057-4778-ae92-126ab9acee6";
	
	// {D682C4BA-A90A-42fe-B9E1-03109849C423}
	public static final String CLSID_StreamBufferComposeRecording = "d682c4ba-a90a-42fe-b9e1-3109849c423";
	
	// {B1B77C00-C3E4-11cf-AF79-00AA00B67A42}               DV video decoder
	public static final String CLSID_DVVideoCodec = "b1b77c00-c3e4-11cf-af79-0aa0b67a42";
	
	// {13AA3650-BB6F-11d0-AFB9-00AA00B67A42}               DV video encoder
	public static final String CLSID_DVVideoEnc = "13aa3650-bb6f-11d0-afb9-0aa0b67a42";
	
	// {4EB31670-9FC6-11cf-AF6E-00AA00B67A42}               DV splitter
	public static final String CLSID_DVSplitter = "4eb31670-9fc6-11cf-af6e-0aa0b67a42";
	
	// {129D7E40-C10D-11d0-AFB9-00AA00B67A42}               DV muxer
	public static final String CLSID_DVMux = "129d7e40-c10d-11d0-afb9-0aa0b67a42";
	
	// {060AF76C-68DD-11d0-8FC1-00C04FD9189D}
	public static final String CLSID_SeekingPassThru = "60af76c-68dd-11d0-8fc1-0c04fd9189d";
	
	// 6E8D4A20-310C-11d0-B79A-00AA003767A7                 Line21 (CC) Decoder
	public static final String CLSID_Line21Decoder = "6e8d4a20-310c-11d0-b79a-0aa03767a7";
	
	// E4206432-01A1-4BEE-B3E1-3702C8EDC574                 Line21 (CC) Decoder v2
	public static final String CLSID_Line21Decoder2 = "e4206432-01a1-4bee-b3e1-3702c8edc574";
	
	// {CD8743A1-3736-11d0-9E69-00C04FD7C15B}
	public static final String CLSID_OverlayMixer = "cd8743a1-3736-11d0-9e69-0c04fd7c15b";
	
	// {814B9800-1C88-11d1-BAD9-00609744111A}
	public static final String CLSID_VBISurfaces = "814b9800-1c88-11d1-bad9-0609744111a";
	
	// {70BC06E0-5666-11d3-A184-00105AEF9F33}               WST Teletext Decoder
	public static final String CLSID_WSTDecoder = "70bc06e0-5666-11d3-a184-0105aef9f33";
	
	// {301056D0-6DFF-11d2-9EEB-006008039E37}
	public static final String CLSID_MjpegDec = "301056d0-6dff-11d2-9eeb-060839e37";
	
	// {B80AB0A0-7416-11d2-9EEB-006008039E37}
	public static final String CLSID_MJPGEnc = "b80ab0a0-7416-11d2-9eeb-060839e37";
	
	
	
	// pnp objects and categories
	// 62BE5D10-60EB-11d0-BD3B-00A0C911CE86                 ICreateDevEnum
	public static final String CLSID_SystemDeviceEnum = "62BE5D10-60EB-11d0-BD3B-00A0C911CE86";
	
	// 4315D437-5B8C-11d0-BD3B-00A0C911CE86
	public static final String CLSID_CDeviceMoniker = "4315D437-5B8C-11d0-BD3B-00A0C911CE86";
	
	// 860BB310-5D01-11d0-BD3B-00A0C911CE86                 Video capture category
	public static final String CLSID_VideoInputDeviceCategory = "860BB310-5D01-11d0-BD3B-00A0C911CE86";
	public static final String CLSID_CVidCapClassManager = "860BB310-5D01-11d0-BD3B-00A0C911CE86";
	
	// 083863F1-70DE-11d0-BD40-00A0C911CE86                 Filter category
	public static final String CLSID_LegacyAmFilterCategory = "083863F1-70DE-11d0-BD40-00A0C911CE86";
	public static final String CLSID_CQzFilterClassManager = "083863F1-70DE-11d0-BD40-00A0C911CE86";
	
	// 33D9A760-90C8-11d0-BD43-00A0C911CE86
	public static final String CLSID_VideoCompressorCategory = "33d9a760-90c8-11d0-bd43-0a0c911ce86";
	public static final String CLSID_CIcmCoClassManager = "33d9a760-90c8-11d0-bd43-0a0c911ce86";
	
	// 33D9A761-90C8-11d0-BD43-00A0C911CE86
	public static final String CLSID_AudioCompressorCategory = "33d9a761-90c8-11d0-bd43-0a0c911ce86";
	public static final String CLSID_CAcmCoClassManager = "33d9a761-90c8-11d0-bd43-0a0c911ce86";
	
	// 33D9A762-90C8-11d0-BD43-00A0C911CE86                 Audio source cateogry
	public static final String CLSID_AudioInputDeviceCategory = "33d9a762-90c8-11d0-bd43-0a0c911ce86";
	public static final String CLSID_CWaveinClassManager = "33d9a762-90c8-11d0-bd43-0a0c911ce86";
	
	// E0F158E1-CB04-11d0-BD4E-00A0C911CE86                 Audio renderer category
	public static final String CLSID_AudioRendererCategory = "e0f158e1-cb04-11d0-bd4e-0a0c911ce86";
	public static final String CLSID_CWaveOutClassManager = "e0f158e1-cb04-11d0-bd4e-0a0c911ce86";
	
	// 4EFE2452-168A-11d1-BC76-00C04FB9453B                 Midi renderer category
	public static final String CLSID_MidiRendererCategory = "4EfE2452-168A-11d1-BC76-0c04FB9453B";
	public static final String CLSID_CMidiOutClassManager = "4EfE2452-168A-11d1-BC76-0c04FB9453B";
	
	// CC7BFB41-F175-11d1-A392-00E0291F3959     External Renderers Category
	public static final String CLSID_TransmitCategory = "cc7bfb41-f175-11d1-a392-0e0291f3959";
	
	// CC7BFB46-F175-11d1-A392-00E0291F3959     Device Control Filters
	public static final String CLSID_DeviceControlCategory = "cc7bfb46-f175-11d1-a392-0e0291f3959";
	
	// DA4E3DA0-D07D-11d0-BD50-00A0C911CE86
	public static final String CLSID_ActiveMovieCategories = "da4e3da0-d07d-11d0-bd50-0a0c911ce86";
	
	// 2721AE20-7E70-11D0-A5D6-28DB04C10000
	public static final String CLSID_DVDHWDecodersCategory = "2721AE20-7E70-11D0-A5D6-28DB04C10000";
	
	// 7D22E920-5CA9-4787-8C2B-A6779BD11781     Encoder API encoder category
	public static final String CLSID_MediaEncoderCategory = "7D22E920-5CA9-4787-8C2B-A6779BD11781";
	
	// 236C9559-ADCE-4736-BF72-BAB34E392196     Encoder API multiplexer category
	public static final String CLSID_MediaMultiplexerCategory = "236C9559-ADCE-4736-BF72-BAB34E392196";
	
	// CDA42200-BD88-11d0-BD4E-00A0C911CE86
	public static final String CLSID_FilterMapper2 = "cda42200-bd88-11d0-bd4e-0a0c911ce86";
	
	
	// 1e651cc0-b199-11d0-8212-00c04fc32c45
	public static final String CLSID_MemoryAllocator = "1e651cc0-b199-11d0-8212-00c04fc32c45";
	
	// CDBD8D00-C193-11d0-BD4E-00A0C911CE86
	public static final String CLSID_MediaPropertyBag = "cdbd8d00-c193-11d0-bd4e-0a0c911ce86";
	
	// FCC152B7-F372-11d0-8E00-00C04FD7C08B
	public static final String CLSID_DvdGraphBuilder = "FCC152B7-F372-11d0-8E00-00C04FD7C08B";
	
	// 9B8C4620-2C1A-11d0-8493-00A02438AD48
	public static final String CLSID_DVDNavigator = "9b8c4620-2c1a-11d0-8493-0a02438ad48";
	
	// f963c5cf-a659-4a93-9638-caf3cd277d13
	public static final String CLSID_DVDState = "f963c5cf-a659-4a93-9638-caf3cd277d13";
	
	// CC58E280-8AA1-11d1-B3F1-00AA003761C5
	public static final String CLSID_SmartTee = "cc58e280-8aa1-11d1-b3f1-0aa03761c5";
	
	// FB056BA0-2502-45B9-8E86-2B40DE84AD29
	public static final String CLSID_DtvCcFilter = "fb056ba0-2502-45b9-8e86-2b40de84ad29";
	
	// 2F7EE4B6-6FF5-4EB4-B24A-2BFC41117171
	public static final String CLSID_MSTVCaptionFilter = "2F7EE4B6-6FF5-4EB4-B24A-2BFC41117171";
	
	// -- format types ---
	
	// 0F6417D6-C318-11D0-A43F-00A0C9223196        FORMAT_None
	public static final String FORMAT_None = "0F6417D6-c318-11d0-a43f-00a0c9223196";
	
	// 05589f80-c356-11ce-bf01-00aa0055595a        FORMAT_VideoInfo
	public static final String FORMAT_VideoInfo = "05589f80-c356-11ce-bf01-00aa0055595a";
	
	// F72A76A0-EB0A-11d0-ACE4-0000C0CC16BA        FORMAT_VideoInfo2
	public static final String FORMAT_VideoInfo2 = "f72a76A0-eb0a-11d0-ace4-0000c0cc16ba";
	
	// 05589f81-c356-11ce-bf01-00aa0055595a        FORMAT_WaveFormatEx
	public static final String FORMAT_WaveFormatEx = "05589f81-c356-11ce-bf01-00aa0055595a";
	
	// 05589f82-c356-11ce-bf01-00aa0055595a        FORMAT_MPEGVideo
	public static final String FORMAT_MPEGVideo = "05589f82-c356-11ce-bf01-00aa0055595a";
	
	// 05589f83-c356-11ce-bf01-00aa0055595a        FORMAT_MPEGStreams
	public static final String FORMAT_MPEGStreams = "05589f83-c356-11ce-bf01-00aa0055595a";
	
	// 05589f84-c356-11ce-bf01-00aa0055595a        FORMAT_DvInfo, DVINFO
	public static final String FORMAT_DvInfo = "05589f84-c356-11ce-bf01-00aa0055595a";
	
	// C7ECF04D-4582-4869-9ABB-BFB523B62EDF       FORMAT_525WSS    
	public static final String FORMAT_525WSS = "c7ecf04d-4582-4869-9abb-bfb523b62edf";
	
	
	// -- Video related GUIDs ---
	
	// 944d4c00-dd52-11ce-bf0e-00aa0055595a
	public static final String CLSID_DirectDrawProperties = "944d4c00-dd52-11ce-bf0e-00aa0055595a";
	
	// 59ce6880-acf8-11cf-b56e-0080c7c4b68a
	public static final String CLSID_PerformanceProperties = "59ce6880-acf8-11cf-b56e-0080c7c4b68a";
	
	// 418afb70-f8b8-11ce-aac6-0020af0b99a3
	public static final String CLSID_QualityProperties = "418afb70-f8b8-11ce-aac6-0020af0b99a3";
	
	// 61ded640-e912-11ce-a099-00aa00479a58
	public static final String IID_IBaseVideoMixer = "61ded640-e912-11ce-a099-00aa00479a58";
	
	// 36d39eb0-dd75-11ce-bf0e-00aa0055595a
	public static final String IID_IDirectDrawVideo = "36d39eb0-dd75-11ce-bf0e-00aa0055595a";
	
	// bd0ecb0-f8e2-11ce-aac6-0020af0b99a3
	public static final String IID_IQualProp = "1bd0ecb0-f8e2-11ce-aac6-0020af0b99a3";
	
	// {CE292861-FC88-11d0-9E69-00C04FD7C15B}
	public static final String CLSID_VPObject = "ce292861-fc88-11d0-9e69-0c04fd7c15b";
	
	// {CE292862-FC88-11d0-9E69-00C04FD7C15B}
	public static final String IID_IVPObject = "ce292862-fc88-11d0-9e69-0c04fd7c15b";
	
	// {25DF12C1-3DE0-11d1-9E69-00C04FD7C15B}
	public static final String IID_IVPControl = "25df12c1-3de0-11d1-9e69-0c04fd7c15b";
	
	// {814B9801-1C88-11d1-BAD9-00609744111A}
	public static final String CLSID_VPVBIObject = "814b9801-1c88-11d1-bad9-0609744111a";
	
	// {814B9802-1C88-11d1-BAD9-00609744111A}
	public static final String IID_IVPVBIObject = "814b9802-1c88-11d1-bad9-0609744111a";
	
	// {BC29A660-30E3-11d0-9E69-00C04FD7C15B}
	public static final String IID_IVPConfig = "bc29a660-30e3-11d0-9e69-0c04fd7c15b";
	
	// {C76794A1-D6C5-11d0-9E69-00C04FD7C15B}
	public static final String IID_IVPNotify = "c76794a1-d6c5-11d0-9e69-0c04fd7c15b";
	
	// {EBF47183-8764-11d1-9E69-00C04FD7C15B}
	public static final String IID_IVPNotify2 = "ebf47183-8764-11d1-9e69-0c04fd7c15b";
	
	
	// {EC529B00-1A1F-11D1-BAD9-00609744111A}
	public static final String IID_IVPVBIConfig = "ec529b00-1a1f-11d1-bad9-0609744111a";
	
	// {EC529B01-1A1F-11D1-BAD9-00609744111A}
	public static final String IID_IVPVBINotify = "ec529b01-1a1f-11d1-bad9-0609744111a";
	
	// {593CDDE1-0759-11d1-9E69-00C04FD7C15B}
	public static final String IID_IMixerPinConfig = "593cdde1-759-11d1-9e69-0c04fd7c15b";
	
	// {EBF47182-8764-11d1-9E69-00C04FD7C15B}
	public static final String IID_IMixerPinConfig2 = "ebf47182-8764-11d1-9e69-0c04fd7c15b";
	
	
	// This is a real pain in the neck. The OLE GUIDs are separated out into a
	// different file from the main header files. The header files can then be
	// included multiple times and are protected with the following statements,
	//
	//      #ifndef __SOMETHING_DEFINED__
	//      #define __SOMETHING_DEFINED__
	//          all the header contents
	//      #endif // __SOMETHING_DEFINED__
	//
	// When the actual GUIDs are to be defined (using initguid) the GUID header
	// file can then be included to really define them just once. Unfortunately
	// DirectDraw has the GUIDs defined in the main header file. So if the base
	// classes bring in ddraw.h to get at the DirectDraw structures and so on
	// nobody would then be able to really include ddraw.h to allocate the GUID
	// memory structures because of the aforementioned header file protection
	// Therefore the DirectDraw GUIDs are defined and allocated for real here
	
	public static final String CLSID_DirectDraw = "D7B70EE0-4340-11CF-B063-0020AFC2CD35";
	public static final String CLSID_DirectDrawClipper = "593817A0-7DB3-11CF-A2DE-00AA00b93356";
	public static final String IID_IDirectDraw = "6C14DB80-A733-11CE-A521-0020AF0BE560";
	public static final String IID_IDirectDraw2 = "B3A6F3E0-2B43-11CF-A2DE-00AA00B93356";
	public static final String IID_IDirectDrawSurface = "6C14DB81-A733-11CE-A521-0020AF0BE560";
	public static final String IID_IDirectDrawSurface2 = "57805885-6eec-11cf-9441-a82303c10e27";
	public static final String IID_IDirectDrawSurface3 = "DA044E00-69B2-11D0-A1D5-00AA00B8DFBB";
	public static final String IID_IDirectDrawSurface4 = "0B2B8630-AD35-11D0-8EA6-00609797EA5B";
	public static final String IID_IDirectDrawSurface7 = "06675a80-3b9b-11d2-b92f-00609797ea5b";
	public static final String IID_IDirectDrawPalette = "6C14DB84-A733-11CE-A521-0020AF0BE560";
	public static final String IID_IDirectDrawClipper = "6C14DB85-A733-11CE-A521-0020AF0BE560";
	public static final String IID_IDirectDrawColorControl = "4B9F0EE0-0D7E-11D0-9B06-00A0C903A3B8";
	
	public static final String IID_IDDVideoPortContainer = "6C142760-A733-11CE-A521-0020AF0BE560";
	
	public static final String IID_IDirectDrawKernel = "8D56C120-6A08-11D0-9B06-00A0C903A3B8";
	public static final String IID_IDirectDrawSurfaceKernel = "60755DA0-6A40-11D0-9B06-00A0C903A3B8";
	
	// 0618aa30-6bc4-11cf-bf36-00aa0055595a
	public static final String CLSID_ModexProperties = "0618aa30-6bc4-11cf-bf36-00aa0055595a";
	
	// dd1d7110-7836-11cf-bf47-00aa0055595a
	public static final String IID_IFullScreenVideo = "dd1d7110-7836-11cf-bf47-00aa0055595a";
	
	// 53479470-f1dd-11cf-bc42-00aa00ac74f6
	public static final String IID_IFullScreenVideoEx = "53479470-f1dd-11cf-bc42-00aa00ac74f6";
	
	// {101193C0-0BFE-11d0-AF91-00AA00B67A42}           DV decoder property
	public static final String CLSID_DVDecPropertiesPage = "101193c0-bfe-11d0-af91-0aa0b67a42";
	
	// {4150F050-BB6F-11d0-AFB9-00AA00B67A42}           DV encoder property
	public static final String CLSID_DVEncPropertiesPage = "4150f050-bb6f-11d0-afb9-0aa0b67a42";
	
	// {4DB880E0-C10D-11d0-AFB9-00AA00B67A42}           DV Muxer property
	public static final String CLSID_DVMuxPropertyPage = "4db880e0-c10d-11d0-afb9-0aa0b67a42";
	
	
	// -- Direct Sound Audio related GUID ---
	
	// 546F4260-D53E-11cf-B3F0-00AA003761C5
	public static final String IID_IAMDirectSound = "546f4260-d53e-11cf-b3f0-0aa03761c5";
	
	// -- MPEG audio decoder properties
	
	// {b45dd570-3c77-11d1-abe1-00a0c905f375}
	public static final String IID_IMpegAudioDecoder = "b45dd570-3c77-11d1-abe1-00a0c905f375";
	
	// --- Line21 Decoder interface GUID ---
	
	// 6E8D4A21-310C-11d0-B79A-00AA003767A7            IID_IAMLine21Decoder
	public static final String IID_IAMLine21Decoder = "6e8d4a21-310c-11d0-b79a-0aa03767a7";
	
	// --- WST Decoder interface GUID ---
	
	// C056DE21-75C2-11d3-A184-00105AEF9F33            IID_IAMWstDecoder
	public static final String IID_IAMWstDecoder = "c056de21-75c2-11d3-a184-0105aef9f33";
	
	// --- WST Decoder Property Page ---
	
	// 04E27F80-91E4-11d3-A184-00105AEF9F33            WST Decoder Property Page
	public static final String CLSID_WstDecoderPropertyPage = "4e27f80-91e4-11d3-a184-0105aef9f33";
	
	
	// -- Analog video related GUIDs ---
	
	
	// -- format types ---
	// 0482DDE0-7817-11cf-8A03-00AA006ECB65
	public static final String FORMAT_AnalogVideo = "482dde0-7817-11cf-8a3-0aa06ecb65";
	
	
	// -- major type, Analog Video
	
	// 0482DDE1-7817-11cf-8A03-00AA006ECB65
	public static final String MEDIATYPE_AnalogVideo = "482dde1-7817-11cf-8a3-0aa06ecb65";
	
	
	// -- Analog Video subtypes, NTSC
	
	// 0482DDE2-7817-11cf-8A03-00AA006ECB65
	public static final String MEDIASUBTYPE_AnalogVideo_NTSC_M = "482dde2-7817-11cf-8a3-0aa06ecb65";
	
	// -- Analog Video subtypes, PAL
	
	// 0482DDE5-7817-11cf-8A03-00AA006ECB65
	public static final String MEDIASUBTYPE_AnalogVideo_PAL_B = "482dde5-7817-11cf-8a3-0aa06ecb65";
	
	// 0482DDE6-7817-11cf-8A03-00AA006ECB65
	public static final String MEDIASUBTYPE_AnalogVideo_PAL_D = "482dde6-7817-11cf-8a3-0aa06ecb65";
	
	// 0482DDE7-7817-11cf-8A03-00AA006ECB65
	public static final String MEDIASUBTYPE_AnalogVideo_PAL_G = "482dde7-7817-11cf-8a3-0aa06ecb65";
	
	// 0482DDE8-7817-11cf-8A03-00AA006ECB65
	public static final String MEDIASUBTYPE_AnalogVideo_PAL_H = "482dde8-7817-11cf-8a3-0aa06ecb65";
	
	// 0482DDE9-7817-11cf-8A03-00AA006ECB65
	public static final String MEDIASUBTYPE_AnalogVideo_PAL_I = "482dde9-7817-11cf-8a3-0aa06ecb65";
	
	// 0482DDEA-7817-11cf-8A03-00AA006ECB65
	public static final String MEDIASUBTYPE_AnalogVideo_PAL_M = "482ddea-7817-11cf-8a3-0aa06ecb65";
	
	// 0482DDEB-7817-11cf-8A03-00AA006ECB65
	public static final String MEDIASUBTYPE_AnalogVideo_PAL_N = "482ddeb-7817-11cf-8a3-0aa06ecb65";
	
	// 0482DDEC-7817-11cf-8A03-00AA006ECB65
	public static final String MEDIASUBTYPE_AnalogVideo_PAL_N_COMBO = "482ddec-7817-11cf-8a3-0aa06ecb65";
	
	// -- Analog Video subtypes, SECAM
	
	// 0482DDF0-7817-11cf-8A03-00AA006ECB65
	public static final String MEDIASUBTYPE_AnalogVideo_SECAM_B = "482ddf0-7817-11cf-8a3-0aa06ecb65";
	
	// 0482DDF1-7817-11cf-8A03-00AA006ECB65
	public static final String MEDIASUBTYPE_AnalogVideo_SECAM_D = "482ddf1-7817-11cf-8a3-0aa06ecb65";
	
	// 0482DDF2-7817-11cf-8A03-00AA006ECB65
	public static final String MEDIASUBTYPE_AnalogVideo_SECAM_G = "482ddf2-7817-11cf-8a3-0aa06ecb65";
	
	// 0482DDF3-7817-11cf-8A03-00AA006ECB65
	public static final String MEDIASUBTYPE_AnalogVideo_SECAM_H = "482ddf3-7817-11cf-8a3-0aa06ecb65";
	
	// 0482DDF4-7817-11cf-8A03-00AA006ECB65
	public static final String MEDIASUBTYPE_AnalogVideo_SECAM_K = "482ddf4-7817-11cf-8a3-0aa06ecb65";
	
	// 0482DDF5-7817-11cf-8A03-00AA006ECB65
	public static final String MEDIASUBTYPE_AnalogVideo_SECAM_K1 = "482ddf5-7817-11cf-8a3-0aa06ecb65";
	
	// 0482DDF6-7817-11cf-8A03-00AA006ECB65
	public static final String MEDIASUBTYPE_AnalogVideo_SECAM_L = "482ddf6-7817-11cf-8a3-0aa06ecb65";
	
	
	// --  External audio related GUIDs ---
	
	// -- major types, Analog Audio
	
	// 0482DEE1-7817-11cf-8a03-00aa006ecb65
	public static final String MEDIATYPE_AnalogAudio = "482dee1-7817-11cf-8a3-0aa06ecb65";
	
	// -- Well known time format GUIDs ---
	
	public static final String TIME_FORMAT_NONE = GUID_NULL;
	
	// 7b785570-8c82-11cf-bc0c-00aa00ac74f6
	public static final String TIME_FORMAT_FRAME = "7b785570-8c82-11cf-bcc-0aa0ac74f6";
	
	// 7b785571-8c82-11cf-bc0c-00aa00ac74f6
	public static final String TIME_FORMAT_BYTE = "7b785571-8c82-11cf-bcc-0aa0ac74f6";
	
	// 7b785572-8c82-11cf-bc0c-00aa00ac74f6
	public static final String TIME_FORMAT_SAMPLE = "7b785572-8c82-11cf-bcc-0aa0ac74f6";
	
	// 7b785573-8c82-11cf-bc0c-00aa00ac74f6
	public static final String TIME_FORMAT_FIELD = "7b785573-8c82-11cf-bcc-0aa0ac74f6";
	
	
	// 7b785574-8c82-11cf-bc0c-00aa00ac74f6
	public static final String TIME_FORMAT_MEDIA_TIME = "7b785574-8c82-11cf-bcc-0aa0ac74f6";
	
	
	// for IKsPropertySet
	
	// 9B00F101-1567-11d1-B3F1-00AA003761C5
	public static final String AMPROPSETID_Pin = "9b00f101-1567-11d1-b3f1-0aa03761c5";
	
	// fb6c4281-0353-11d1-905f-0000c0cc16ba
	public static final String PIN_CATEGORY_CAPTURE = "fb6c4281-0353-11d1-905f-0000c0cc16ba";
	
	// fb6c4282-0353-11d1-905f-0000c0cc16ba
	public static final String PIN_CATEGORY_PREVIEW = "fb6c4282-0353-11d1-905f-0000c0cc16ba";
	
	// fb6c4283-0353-11d1-905f-0000c0cc16ba
	public static final String PIN_CATEGORY_ANALOGVIDEOIN = "fb6c4283-0353-11d1-905f-0000c0cc16ba";
	
	// fb6c4284-0353-11d1-905f-0000c0cc16ba
	public static final String PIN_CATEGORY_VBI = "fb6c4284-0353-11d1-905f-0000c0cc16ba";
	
	// fb6c4285-0353-11d1-905f-0000c0cc16ba
	public static final String PIN_CATEGORY_VIDEOPORT = "fb6c4285-0353-11d1-905f-0000c0cc16ba";
	
	// fb6c4286-0353-11d1-905f-0000c0cc16ba
	public static final String PIN_CATEGORY_NABTS = "fb6c4286-0353-11d1-905f-0000c0cc16ba";
	
	// fb6c4287-0353-11d1-905f-0000c0cc16ba
	public static final String PIN_CATEGORY_EDS = "fb6c4287-0353-11d1-905f-0000c0cc16ba";
	
	// fb6c4288-0353-11d1-905f-0000c0cc16ba
	public static final String PIN_CATEGORY_TELETEXT = "fb6c4288-0353-11d1-905f-0000c0cc16ba";
	
	// fb6c4289-0353-11d1-905f-0000c0cc16ba
	public static final String PIN_CATEGORY_CC = "fb6c4289-0353-11d1-905f-0000c0cc16ba";
	
	// fb6c428a-0353-11d1-905f-0000c0cc16ba
	public static final String PIN_CATEGORY_STILL = "fb6c428a-0353-11d1-905f-0000c0cc16ba";
	
	// fb6c428b-0353-11d1-905f-0000c0cc16ba
	public static final String PIN_CATEGORY_TIMECODE = "fb6c428b-0353-11d1-905f-0000c0cc16ba";
	
	// fb6c428c-0353-11d1-905f-0000c0cc16ba
	public static final String PIN_CATEGORY_VIDEOPORT_VBI = "fb6c428c-0353-11d1-905f-0000c0cc16ba";
	
	
	// the following special GUIDS are used by ICaptureGraphBuilder::FindInterface
	
	// {AC798BE0-98E3-11d1-B3F1-00AA003761C5}
	public static final String LOOK_UPSTREAM_ONLY = "ac798be0-98e3-11d1-b3f1-0aa03761c5";
	
	// {AC798BE1-98E3-11d1-B3F1-00AA003761C5}
	public static final String LOOK_DOWNSTREAM_ONLY = "ac798be1-98e3-11d1-b3f1-0aa03761c5";
	
	// -------------------------------------------------------------------------
	// KSProxy GUIDS
	// -------------------------------------------------------------------------
	
	// {266EEE41-6C63-11cf-8A03-00AA006ECB65}
	public static final String CLSID_TVTunerFilterPropertyPage = "266eee41-6c63-11cf-8a3-0aa06ecb65";
	
	// {71F96461-78F3-11d0-A18C-00A0C9118956}
	public static final String CLSID_CrossbarFilterPropertyPage = "71f96461-78f3-11d0-a18c-0a0c9118956";
	
	// {71F96463-78F3-11d0-A18C-00A0C9118956}
	public static final String CLSID_TVAudioFilterPropertyPage = "71f96463-78f3-11d0-a18c-0a0c9118956";
	
	// {71F96464-78F3-11d0-A18C-00A0C9118956}
	public static final String CLSID_VideoProcAmpPropertyPage = "71f96464-78f3-11d0-a18c-0a0c9118956";
	
	// {71F96465-78F3-11d0-A18C-00A0C9118956}
	public static final String CLSID_CameraControlPropertyPage = "71f96465-78f3-11d0-a18c-0a0c9118956";
	
	// {71F96466-78F3-11d0-A18C-00A0C9118956}
	public static final String CLSID_AnalogVideoDecoderPropertyPage = "71f96466-78f3-11d0-a18c-0a0c9118956";
	
	// {71F96467-78F3-11d0-A18C-00A0C9118956}
	public static final String CLSID_VideoStreamConfigPropertyPage = "71f96467-78f3-11d0-a18c-0a0c9118956";
	
	// {37E92A92-D9AA-11d2-BF84-8EF2B1555AED} Audio Renderer Advanced Property Page
	public static final String CLSID_AudioRendererAdvancedProperties = "37e92a92-d9aa-11d2-bf84-8ef2b1555aed";
	
	
	// -------------------------------------------------------------------------
	// VMR GUIDS
	// -------------------------------------------------------------------------
	
	// {B87BEB7B-8D29-423f-AE4D-6582C10175AC}
	public static final String CLSID_VideoMixingRenderer = "B87BEB7B-8D29-423f-AE4D-6582C10175AC";
	
	// {6BC1CFFA-8FC1-4261-AC22-CFB4CC38DB50}
	public static final String CLSID_VideoRendererDefault = "6BC1CFFA-8FC1-4261-AC22-CFB4CC38DB50";
	
	// {99d54f63-1a69-41ae-aa4d-c976eb3f0713}
	public static final String CLSID_AllocPresenter = "99d54f63-1a69-41ae-aa4d-c976eb3f0713";
	
	// {4444ac9e-242e-471b-a3c7-45dcd46352bc}
	public static final String CLSID_AllocPresenterDDXclMode = "4444ac9e-242e-471b-a3c7-45dcd46352bc";
	
	// {6f26a6cd-967b-47fd-874a-7aed2c9d25a2}
	public static final String CLSID_VideoPortManager = "6f26a6cd-967b-47fd-874a-7aed2c9d25a2";
	
	
	// -------------------------------------------------------------------------
	// VMR GUIDS for DX9
	// -------------------------------------------------------------------------
	
	// {51b4abf3-748f-4e3b-a276-c828330e926a}
	public static final String CLSID_VideoMixingRenderer9 = "51b4abf3-748f-4e3b-a276-c828330e926a";
	
	
	// -------------------------------------------------------------------------
	// EVR GUIDS
	// -------------------------------------------------------------------------
	
	// {FA10746C-9B63-4b6c-BC49-FC300EA5F256}
	public static final String CLSID_EnhancedVideoRenderer = "fa10746c-9b63-4b6c-bc49-fc30ea5f256";
	
	// {E474E05A-AB65-4f6a-827C-218B1BAAF31F}
	public static final String CLSID_MFVideoMixer9 = "E474E05A-AB65-4f6a-827C-218B1BAAF31F";
	
	// {98455561-5136-4d28-AB08-4CEE40EA2781}
	public static final String CLSID_MFVideoPresenter9 = "98455561-5136-4d28-ab8-4cee40ea2781";
	
	// {a0a7a57b-59b2-4919-a694-add0a526c373}
	public static final String CLSID_EVRTearlessWindowPresenter9 = "a0a7a57b-59b2-4919-a694-add0a526c373";
	
	// -------------------------------------------------------------------------
	// BDA Network Provider GUIDS
	// -------------------------------------------------------------------------
	
	// This is the GUID for the generic NP which would replace ATSC, DVBT, DVBS
	// and DVBC NP. All the other GUIDs are still kept for backward compatibility
	// {B2F3A67C-29DA-4c78-8831-091ED509A475}
	public static final String CLSID_NetworkProvider = "b2f3a67c-29da-4c78-8831-91ed59a475";
	
	// {0DAD2FDD-5FD7-11D3-8F50-00C04F7971E2}
	public static final String CLSID_ATSCNetworkProvider = "0dad2fdd-5fd7-11d3-8f50-00c04f7971e2";
	
	// {E3444D16-5AC4-4386-88DF-13FD230E1DDA}
	public static final String CLSID_ATSCNetworkPropertyPage = "e3444d16-5ac4-4386-88df-13fd230e1dda";
	
	// {FA4B375A-45B4-4d45-8440-263957B11623}
	public static final String CLSID_DVBSNetworkProvider = "fa4b375a-45b4-4d45-8440-263957b11623";
	
	// {216C62DF-6D7F-4e9a-8571-05F14EDB766A}
	public static final String CLSID_DVBTNetworkProvider = "216c62df-6d7f-4e9a-8571-5f14edb766a";
	
	// {DC0C0FE7-0485-4266-B93F-68FBF80ED834}
	public static final String CLSID_DVBCNetworkProvider = "dc0c0fe7-485-4266-b93f-68fbf8ed834";
	
	// -------------------------------------------------------------------------
	// attribute GUIDs
	// -------------------------------------------------------------------------
	
	// {EB7836CA-14FF-4919-BCE7-3AF12319E50C}
	public static final String DSATTRIB_UDCRTag = "EB7836CA-14FF-4919-bce7-3af12319e50c";
	
	// {2F5BAE02-7B8F-4f60-82D6-E4EA2F1F4C99}
	public static final String DSATTRIB_PicSampleSeq = "2f5bae02-7b8f-4f60-82d6-e4ea2f1f4c99";
	
	// -------------------------------------------------------------------------
	// TVE Receiver filter guids
	// -------------------------------------------------------------------------
	
	// The CLSID used by the TVE Receiver filter
	// {05500280-FAA5-4DF9-8246-BFC23AC5CEA8}
	public static final String CLSID_DShowTVEFilter = "05500280-FAA5-4DF9-8246-BFC23AC5CEA8";
	
	// {05500281-FAA5-4DF9-8246-BFC23AC5CEA8}
	public static final String CLSID_TVEFilterTuneProperties = "05500281-FAA5-4DF9-8246-BFC23AC5CEA8";
	
	
	// {05500282-FAA5-4DF9-8246-BFC23AC5CEA8}
	public static final String CLSID_TVEFilterCCProperties = "05500282-FAA5-4DF9-8246-BFC23AC5CEA8";
	
	// {05500283-FAA5-4DF9-8246-BFC23AC5CEA8}
	public static final String CLSID_TVEFilterStatsProperties = "05500283-FAA5-4DF9-8246-BFC23AC5CEA8";
	
	// -------------------------------------------------------------------------
	// Defined ENCAPI parameter GUIDs
	// -------------------------------------------------------------------------
	
	// The CLSID for the original IVideoEncoder proxy plug-in
	// {B43C4EEC-8C32-4791-9102-508ADA5EE8E7}
	public static final String CLSID_IVideoEncoderProxy = "b43c4eec-8c32-4791-912-508ada5ee8e7";
	
	// The CLSID for the ICodecAPI proxy plug-in
	// {7ff0997a-1999-4286-a73c-622b8814e7eb}
	public static final String CLSID_ICodecAPIProxy = "7ff0997a-1999-4286-a73c-622b8814e7eb";
	
	// The CLSID for the combination ICodecAPI/IVideoEncoder proxy plug-in
	// {b05dabd9-56e5-4fdc-afa4-8a47e91f1c9c}
	public static final String CLSID_IVideoEncoderCodecAPIProxy = "b05dabd9-56e5-4fdc-afa4-8a47e91f1c9c";
	
	// {49CC4C43-CA83-4ad4-A9AF-F3696AF666DF}
	public static final String ENCAPIPARAM_BITRATE = "49cc4c43-ca83-4ad4-a9af-f3696af666df";
	
	// {703F16A9-3D48-44a1-B077-018DFF915D19}
	public static final String ENCAPIPARAM_PEAK_BITRATE = "703f16a9-3d48-44a1-b077-18dff915d19";
	
	// {EE5FB25C-C713-40d1-9D58-C0D7241E250F}
	public static final String ENCAPIPARAM_BITRATE_MODE = "ee5fb25c-c713-40d1-9d58-c0d7241e25f";
	
	// {0C0171DB-FEFC-4af7-9991-A5657C191CD1}
	public static final String ENCAPIPARAM_SAP_MODE = "c0171db-fefc-4af7-9991-a5657c191cd1";
	
	// for kernel control
	
	// {62b12acf-f6b0-47d9-9456-96f22c4e0b9d}
	public static final String CODECAPI_CHANGELISTS = "62b12acf-f6b0-47d9-9456-96f22c4e0b9d";
	
	// {7112e8e1-3d03-47ef-8e60-03f1cf537301 }
	public static final String CODECAPI_VIDEO_ENCODER = "7112e8e1-3d03-47ef-8e60-03f1cf537301";
	
	// {b9d19a3e-f897-429c-bc46-8138b7272b2d }
	public static final String CODECAPI_AUDIO_ENCODER = "b9d19a3e-f897-429c-bc46-8138b7272b2d";
	
	// {6c5e6a7c-acf8-4f55-a999-1a628109051b }
	public static final String CODECAPI_SETALLDEFAULTS = "6c5e6a7c-acf8-4f55-a999-1a628109051b";
	
	// {6a577e92-83e1-4113-adc2-4fcec32f83a1 }
	public static final String CODECAPI_ALLSETTINGS = "6a577e92-83e1-4113-adc2-4fcec32f83a1";
	
	// {0581af97-7693-4dbd-9dca-3f9ebd6585a1 }
	public static final String CODECAPI_SUPPORTSEVENTS = "0581af97-7693-4dbd-9dca-3f9ebd6585a1";
	
	// {1cb14e83-7d72-4657-83fd-47a2c5b9d13d }
	public static final String CODECAPI_CURRENTCHANGELIST = "1cb14e83-7d72-4657-83fd-47a2c5b9d13d";

	// -----------------------------------------------
	// Used for decoders that exposing ICodecAPI
	// -----------------------------------------------
	public static final String CODECAPI_AVDecMmcssClass = "e0ad4828-df66-4893-9f33-788aa4ec4082";

}
