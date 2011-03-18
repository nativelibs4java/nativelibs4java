package org.bridj.cpp.com;

/**
 * Windows CodecAPI Properties from Microsoft SDK v6.0A<br>
 * Legend for the types indicated in parenthesis :
 * <table>
 *     <tr><th>Reference      </th><th>VariantType </th><th>VariantField           </th></tr>
 *     <tr><td>UINT8          </td><td>VT_UI1      </td><td>bVal                   </td></tr>
 *     <tr><td>UINT16         </td><td>VT_UI2      </td><td>uiVal                  </td></tr>
 *     <tr><td>UINT32         </td><td>VT_UI4      </td><td>ulVal                  </td></tr>
 *     <tr><td>UINT64         </td><td>VT_UI8      </td><td>ullVal                 </td></tr>
 *     <tr><td>INT8           </td><td>VT_I1       </td><td>eVal                   </td></tr>
 *     <tr><td>INT16          </td><td>VT_I2       </td><td>iVal                   </td></tr>
 *     <tr><td>INT32          </td><td>VT_I4       </td><td>lVal                   </td></tr>
 *     <tr><td>INT64          </td><td>VT_I8       </td><td>llVal                  </td></tr>
 *     <tr><td>BOOL           </td><td>VT_BOOL     </td><td>boolVal                </td></tr>
 *     <tr><td>GUID           </td><td>VT_BSTR     </td><td>bstrVal (guid string)  </td></tr>
 *     <tr><td>UINT32/UNINT32 </td><td>VT_UI8      </td><td>ullVal  (ratio)        </td></tr>
 * </table>
 */
public class CodecAPI {
	/*
	 * Obtained with a regexp replace (jEdit) on C:\Program Files\Microsoft SDKs\Windows\v6.0A\Include\codecapi.h :
	 *   (?m)#define\s+(\w+)\s+0x(\w+)\s*,\s*0x(\w+)\s*,\s*0x(\w+)\s*,\s*0x(\w+)\s*,\s*0x(\w+)\s*,\s*0x(\w+)\s*,\s*0x(\w+)\s*,\s*0x(\w+)\s*,\s*0x(\w+)\s*,\s*0x(\w+)\s*,\s*0x(\w+)
	 *   "public static final String " + _1 + " = \"" + _2 + "-" + _3 + "-" + _4 + "-" + _5 + _6 + "-" + _7 + _8 + _9 + _10 + _11 + _12 + "\";"
	 * and :
	 *   (?m)DEFINE_CODECAPI_GUID\s*\(\s*(\w+)\s*,\s*("[^"]+")\s*,[^)]+\)
	 *   "public static final String " + _1 + " = " + _2 + ";"
	 * and :
	 *   (?m)(public interface \w+\s*\{\s*)([^}]+)\}
	 *   _1 + "public static final int \n\t" + _2 + ";\n}"
 	 */
		
	//
	// Common Parameters
	//
	
	// AVEncCommonFormatConstraint (GUID)
	
	public static final String AVEncCommonFormatConstraint = "57cbb9b8-116f-4951-b40c-c2a035ed8f17";
	
	public static final String GUID_AVEncCommonFormatUnSpecified = "af46a35a-6024-4525-a48a-094b97f5b3c2";
	public static final String GUID_AVEncCommonFormatDVD_V = "cc9598c4-e7fe-451d-b1ca-761bc840b7f3";
	public static final String GUID_AVEncCommonFormatDVD_DashVR = "e55199d6-044c-4dae-a488-531ed306235b";
	public static final String GUID_AVEncCommonFormatDVD_PlusVR = "e74c6f2e-ec37-478d-9af4-a5e135b6271c";
	public static final String GUID_AVEncCommonFormatVCD = "95035bf7-9d90-40ff-ad5c-5cf8cf71ca1d";
	public static final String GUID_AVEncCommonFormatSVCD = "51d85818-8220-448c-8066-d69bed16c9ad";
	public static final String GUID_AVEncCommonFormatATSC = "8d7b897c-a019-4670-aa76-2edcac7ac296";
	public static final String GUID_AVEncCommonFormatDVB = "71830d8f-6c33-430d-844b-c2705baae6db";
	public static final String GUID_AVEncCommonFormatMP3 = "349733cd-eb08-4dc2-8197-e49835ef828b";
	public static final String GUID_AVEncCommonFormatHighMAT = "1eabe760-fb2b-4928-90d1-78db88eee889";
	public static final String GUID_AVEncCommonFormatHighMPV = "a2d25db8-b8f9-42c2-8bc7-0b93cf604788";
	
	// AVEncCodecType (GUID)
	public static final String AVEncCodecType = "08af4ac1-f3f2-4c74-9dcf-37f2ec79f826";
	
	public static final String GUID_AVEncMPEG1Video = "c8dafefe-da1e-4774-b27d-11830c16b1fe";
	public static final String GUID_AVEncMPEG2Video = "046dc19a-6677-4aaa-a31d-c1ab716f4560";
	public static final String GUID_AVEncMPEG1Audio = "d4dd1362-cd4a-4cd6-8138-b94db4542b04";
	public static final String GUID_AVEncMPEG2Audio = "ee4cbb1f-9c3f-4770-92b5-fcb7c2a8d381";
	public static final String GUID_AVEncWMV = "4e0fef9b-1d43-41bd-b8bd-4d7bf7457a2a";
	public static final String GUID_AVEndMPEG4Video = "dd37b12a-9503-4f8b-b8d0-324a00c0a1cf";
	public static final String GUID_AVEncH264Video = "95044eab-31b3-47de-8e75-38a42bb03e28";
	public static final String GUID_AVEncDV = "09b769c7-3329-44fb-8954-fa30937d3d5a";
	public static final String GUID_AVEncWMAPro = "1955f90c-33f7-4a68-ab81-53f5657125c4";
	public static final String GUID_AVEncWMALossless = "55ca7265-23d8-4761-9031-b74fbe12f4c1";
	public static final String GUID_AVEncWMAVoice = "13ed18cb-50e8-4276-a288-a6aa228382d9";
	public static final String GUID_AVEncDolbyDigitalPro = "f5be76cc-0ff8-40eb-9cb1-bba94004d44f";
	public static final String GUID_AVEncDolbyDigitalConsumer = "c1a7bf6c-0059-4bfa-94ef-ef747a768d52";
	public static final String GUID_AVEncDolbyDigitalPlus = "698d1b80-f7dd-415c-971c-42492a2056c6";
	public static final String GUID_AVEncDTSHD = "2052e630-469d-4bfb-80ca-1d656e7e918f";
	public static final String GUID_AVEncDTS = "45fbcaa2-5e6e-4ab0-8893-5903bee93acf";
	public static final String GUID_AVEncMLP = "05f73e29-f0d1-431e-a41c-a47432ec5a66";
	public static final String GUID_AVEncPCM = "844be7f4-26cf-4779-b386-cc05d187990c";
	public static final String GUID_AVEncSDDS = "1dc1b82f-11c8-4c71-b7b6-ee3eb9bc2b94";
	
		
		
	
	// AVEncCommonRateControlMode (UINT32)
	public static final String AVEncCommonRateControlMode = "1c0608e9-370c-4710-8a58-cb6181c42423";
	
	public interface eAVEncCommonRateControlMode
	{
		public static final int 
		eAVEncCommonRateControlMode_CBR                = 0,
		eAVEncCommonRateControlMode_PeakConstrainedVBR = 1,
		eAVEncCommonRateControlMode_UnconstrainedVBR   = 2,
		eAVEncCommonRateControlMode_Quality            = 3
	;
	};
	
	// AVEncCommonLowLatency (BOOL)
	public static final String AVEncCommonLowLatency = "9d3ecd55-89e8-490a-970a-0c9548d5a56e";
	
	// AVEncCommonMultipassMode (UINT32)
	public static final String AVEncCommonMultipassMode = "22533d4c-47e1-41b5-9352-a2b7780e7ac4";
	
	// AVEncCommonPassStart (UINT32)
	public static final String AVEncCommonPassStart = "6a67739f-4eb5-4385-9928-f276a939ef95";
	
	// AVEncCommonPassEnd (UINT32)
	public static final String AVEncCommonPassEnd = "0e3d01bc-c85c-467d-8b60-c41012ee3bf6";
	
	// AVEncCommonRealTime (BOOL)
	public static final String AVEncCommonRealTime = "143a0ff6-a131-43da-b81e-98fbb8ec378e";
	
	// AVEncCommonQuality (UINT32)
	public static final String AVEncCommonQuality = "fcbf57a3-7ea5-4b0c-9644-69b40c39c391";
	
	// AVEncCommonQualityVsSpeed (UINT32)
	public static final String AVEncCommonQualityVsSpeed = "98332df8-03cd-476b-89fa-3f9e442dec9f";
	
	// AVEncCommonMeanBitRate (UINT32)
	public static final String AVEncCommonMeanBitRate = "f7222374-2144-4815-b550-a37f8e12ee52";
	
	// AVEncCommonMeanBitRateInterval (UINT64)
	public static final String AVEncCommonMeanBitRateInterval = "bfaa2f0c-cb82-4bc0-8474-f06a8a0d0258";
	
	// AVEncCommonMaxBitRate (UINT32)
	public static final String AVEncCommonMaxBitRate = "9651eae4-39b9-4ebf-85ef-d7f444ec7465";
	
	// AVEncCommonMinBitRate (UINT32)
	public static final String AVEncCommonMinBitRate = "101405b2-2083-4034-a806-efbeddd7c9ff";
	
	// AVEncCommonBufferSize (UINT32)
	public static final String AVEncCommonBufferSize = "0db96574-b6a4-4c8b-8106-3773de0310cd";
	
	// AVEncCommonBufferInLevel (UINT32)
	public static final String AVEncCommonBufferInLevel = "d9c5c8db-fc74-4064-94e9-cd19f947ed45";
	
	// AVEncCommonBufferOutLevel (UINT32)
	public static final String AVEncCommonBufferOutLevel = "ccae7f49-d0bc-4e3d-a57e-fb5740140069";
	
	// AVEncCommonStreamEndHandling (UINT32)
	public static final String AVEncCommonStreamEndHandling = "6aad30af-6ba8-4ccc-8fca-18d19beaeb1c";
	
	public interface eAVEncCommonStreamEndHandling
	{
		public static final int 
		eAVEncCommonStreamEndHandling_DiscardPartial = 0,
		eAVEncCommonStreamEndHandling_EnsureComplete = 1
	;
	};
	
	//
	// Common Post Encode Statistical Parameters
	//
	
	// AVEncStatCommonCompletedPasses (UINT32)
	public static final String AVEncStatCommonCompletedPasses = "3e5de533-9df7-438c-854f-9f7dd3683d34";
	
	//
	// Common Video Parameters
	//
	
	// AVEncVideoOutputFrameRate (UINT32)
	public static final String AVEncVideoOutputFrameRate = "ea85e7c3-9567-4d99-87c4-02c1c278ca7c";
	
	// AVEncVideoOutputFrameRateConversion (UINT32)
	public static final String AVEncVideoOutputFrameRateConversion = "8c068bf4-369a-4ba3-82fd-b2518fb3396e";
	
	public interface eAVEncVideoOutputFrameRateConversion
	{
		public static final int 
		eAVEncVideoOutputFrameRateConversion_Disable = 0,
		eAVEncVideoOutputFrameRateConversion_Enable  = 1,
		eAVEncVideoOutputFrameRateConversion_Alias   = 2
	;
	};
	
	// AVEncVideoPixelAspectRatio (UINT32 as UINT16/UNIT16) <---- You have WORD in the doc
	public static final String AVEncVideoPixelAspectRatio = "3cdc718f-b3e9-4eb6-a57f-cf1f1b321b87";
	
	// AVEncVideoForceSourceScanType (UINT32)
	public static final String AVEncVideoForceSourceScanType = "1ef2065f-058a-4765-a4fc-8a864c103012";
	public interface eAVEncVideoSourceScanType
	{
		public static final int 
		eAVEncVideoSourceScan_Automatic         = 0,
		eAVEncVideoSourceScan_Interlaced        = 1,
		eAVEncVideoSourceScan_Progressive       = 2
	;
	};
	
	// AVEncVideoNoOfFieldsToEncode (UINT64)
	public static final String AVEncVideoNoOfFieldsToEncode = "61e4bbe2-4ee0-40e7-80ab-51ddeebe6291";
	
	// AVEncVideoNoOfFieldsToSkip (UINT64)
	public static final String AVEncVideoNoOfFieldsToSkip = "a97e1240-1427-4c16-a7f7-3dcfd8ba4cc5";
	
	// AVEncVideoEncodeDimension (UINT32)
	public static final String AVEncVideoEncodeDimension = "1074df28-7e0f-47a4-a453-cdd73870f5ce";
	
	// AVEncVideoEncodeOffsetOrigin (UINT32)
	public static final String AVEncVideoEncodeOffsetOrigin = "6bc098fe-a71a-4454-852e-4d2ddeb2cd24";
	
	// AVEncVideoDisplayDimension (UINT32)
	public static final String AVEncVideoDisplayDimension = "de053668-f4ec-47a9-86d0-836770f0c1d5";
	
	// AVEncVideoOutputScanType (UINT32)
	public static final String AVEncVideoOutputScanType = "460b5576-842e-49ab-a62d-b36f7312c9db";
	public interface eAVEncVideoOutputScanType
	{
		public static final int 
		eAVEncVideoOutputScan_Progressive       = 0, 
		eAVEncVideoOutputScan_Interlaced        = 1,
		eAVEncVideoOutputScan_SameAsInput       = 2,
		eAVEncVideoOutputScan_Automatic         = 3
	;
	};
	
	// AVEncVideoInverseTelecineEnable (BOOL)
	public static final String AVEncVideoInverseTelecineEnable = "2ea9098b-e76d-4ccd-a030-d3b889c1b64c";
	
	// AVEncVideoInverseTelecineThreshold (UINT32)
	public static final String AVEncVideoInverseTelecineThreshold = "40247d84-e895-497f-b44c-b74560acfe27";
	
	// AVEncVideoSourceFilmContent (UINT32)
	public static final String AVEncVideoSourceFilmContent = "1791c64b-ccfc-4827-a0ed-2557793b2b1c";
	
	public interface eAVEncVideoFilmContent
	{
		public static final int 
		eAVEncVideoFilmContent_VideoOnly = 0,
		eAVEncVideoFilmContent_FilmOnly  = 1,
		eAVEncVideoFilmContent_Mixed     = 2
	;
	};
	
	// AVEncVideoSourceIsBW (BOOL)
	public static final String AVEncVideoSourceIsBW = "42ffc49b-1812-4fdc-8d24-7054c521e6eb";
	
	// AVEncVideoFieldSwap (BOOL)
	public static final String AVEncVideoFieldSwap = "fefd7569-4e0a-49f2-9f2b-360ea48c19a2";
	
	// AVEncVideoInputChromaResolution (UINT32)
	// AVEncVideoOutputChromaSubsamplingFormat (UINT32)
	public static final String AVEncVideoInputChromaResolution = "bb0cec33-16f1-47b0-8a88-37815bee1739";
	public static final String AVEncVideoOutputChromaResolution = "6097b4c9-7c1d-4e64-bfcc-9e9765318ae7";
	
	public interface eAVEncVideoChromaResolution
	{
		public static final int 
		eAVEncVideoChromaResolution_SameAsSource =0 ,
		eAVEncVideoChromaResolution_444 = 1,
		eAVEncVideoChromaResolution_422 = 2,
		eAVEncVideoChromaResolution_420 = 3,
		eAVEncVideoChromaResolution_411 = 4
	;
	};
	
	// AVEncVideoInputChromaSubsampling (UINT32)
	// AVEncVideoOutputChromaSubsampling (UINT32)
	public static final String AVEncVideoInputChromaSubsampling = "a8e73a39-4435-4ec3-a6ea-98300f4b36f7";
	public static final String AVEncVideoOutputChromaSubsampling = "fa561c6c-7d17-44f0-83c9-32ed12e96343";
	
	public interface eAVEncVideoChromaSubsampling
	{
		public static final int 
		eAVEncVideoChromaSubsamplingFormat_SameAsSource                   = 0,
		eAVEncVideoChromaSubsamplingFormat_ProgressiveChroma              = 0x8,
		eAVEncVideoChromaSubsamplingFormat_Horizontally_Cosited           = 0x4,
		eAVEncVideoChromaSubsamplingFormat_Vertically_Cosited             = 0x2,
		eAVEncVideoChromaSubsamplingFormat_Vertically_AlignedChromaPlanes = 0x1
	;
	};
	
	// AVEncVideoInputColorPrimaries (UINT32)
	// AVEncVideoOutputColorPrimaries (UINT32)
	public static final String AVEncVideoInputColorPrimaries = "c24d783f-7ce6-4278-90ab-28a4f1e5f86c";
	public static final String AVEncVideoOutputColorPrimaries = "be95907c-9d04-4921-8985-a6d6d87d1a6c";
	
	public interface eAVEncVideoColorPrimaries
	{
		public static final int 
		eAVEncVideoColorPrimaries_SameAsSource  = 0, 
		eAVEncVideoColorPrimaries_Reserved      = 1,
		eAVEncVideoColorPrimaries_BT709         = 2,
		eAVEncVideoColorPrimaries_BT470_2_SysM  = 3,
		eAVEncVideoColorPrimaries_BT470_2_SysBG = 4,
		eAVEncVideoColorPrimaries_SMPTE170M     = 5,
		eAVEncVideoColorPrimaries_SMPTE240M     = 6,
		eAVEncVideoColorPrimaries_EBU3231       = 7,
		eAVEncVideoColorPrimaries_SMPTE_C       = 8
	;
	};
	
	// AVEncVideoInputColorTransferFunction (UINT32)
	// AVEncVideoOutputColorTransferFunction (UINT32)
	public static final String AVEncVideoInputColorTransferFunction = "8c056111-a9c3-4b08-a0a0-ce13f8a27c75";
	public static final String AVEncVideoOutputColorTransferFunction = "4a7f884a-ea11-460d-bf57-b88bc75900de";
	
	public interface eAVEncVideoColorTransferFunction
	{
		public static final int 
		eAVEncVideoColorTransferFunction_SameAsSource = 0,
		eAVEncVideoColorTransferFunction_10           = 1,  // (Linear, scRGB)
		eAVEncVideoColorTransferFunction_18           = 2,
		eAVEncVideoColorTransferFunction_20           = 3,
		eAVEncVideoColorTransferFunction_22           = 4,  // (BT470-2 SysM) 
		eAVEncVideoColorTransferFunction_22_709       = 5,  // (BT709,  SMPTE296M, SMPTE170M, BT470, SMPTE274M, BT.1361) 
		eAVEncVideoColorTransferFunction_22_240M      = 6,  // (SMPTE240M, interim 274M)
		eAVEncVideoColorTransferFunction_22_8bit_sRGB = 7,  // (sRGB)
		eAVEncVideoColorTransferFunction_28           = 8
	;
	};
	
	// AVEncVideoInputColorTransferMatrix (UINT32)
	// AVEncVideoOutputColorTransferMatrix (UINT32)
	public static final String AVEncVideoInputColorTransferMatrix = "52ed68b9-72d5-4089-958d-f5405d55081c";
	public static final String AVEncVideoOutputColorTransferMatrix = "a9b90444-af40-4310-8fbe-ed6d933f892b";
	
	
	public interface eAVEncVideoColorTransferMatrix
	{
		public static final int 
		eAVEncVideoColorTransferMatrix_SameAsSource = 0,
		eAVEncVideoColorTransferMatrix_BT709        = 1,
		eAVEncVideoColorTransferMatrix_BT601        = 2,  // (601, BT470-2 B,B, 170M)
		eAVEncVideoColorTransferMatrix_SMPTE240M    = 3
	;
	};
	
	// AVEncVideoInputColorLighting (UINT32)
	// AVEncVideoOutputColorLighting (UINT32)
	public static final String AVEncVideoInputColorLighting = "46a99549-0015-4a45-9c30-1d5cfa258316";
	public static final String AVEncVideoOutputColorLighting = "0e5aaac6-ace6-4c5c-998e-1a8c9c6c0f89";
	
	public interface eAVEncVideoColorLighting
	{
		public static final int 
		eAVEncVideoColorLighting_SameAsSource = 0,
		eAVEncVideoColorLighting_Unknown      = 1,
		eAVEncVideoColorLighting_Bright       = 2,
		eAVEncVideoColorLighting_Office       = 3,
		eAVEncVideoColorLighting_Dim          = 4,
		eAVEncVideoColorLighting_Dark         = 5
	;
	};
	
	// AVEncVideoInputColorNominalRange (UINT32)
	// AVEncVideoOutputColorNominalRange (UINT32)
	public static final String AVEncVideoInputColorNominalRange = "16cf25c6-a2a6-48e9-ae80-21aec41d427e";
	public static final String AVEncVideoOutputColorNominalRange = "972835ed-87b5-4e95-9500-c73958566e54";
	
	public interface eAVEncVideoColorNominalRange
	{
		public static final int 
		eAVEncVideoColorNominalRange_SameAsSource = 0,
		eAVEncVideoColorNominalRange_0_255        = 1,  // (8 bit: 0..255, 10 bit: 0..1023)
		eAVEncVideoColorNominalRange_16_235       = 2,  // (16..235, 64..940 (16*4...235*4) 
		eAVEncVideoColorNominalRange_48_208       = 3   // (48..208) 
	;
	};
	
	// AVEncInputVideoSystem (UINT32)
	public static final String AVEncInputVideoSystem = "bede146d-b616-4dc7-92b2-f5d9fa9298f7";
	
	public interface eAVEncInputVideoSystem
	{
		public static final int 
		eAVEncInputVideoSystem_Unspecified = 0,
		eAVEncInputVideoSystem_PAL       = 1,
		eAVEncInputVideoSystem_NTSC      = 2,
		eAVEncInputVideoSystem_SECAM     = 3,
		eAVEncInputVideoSystem_MAC       = 4,
		eAVEncInputVideoSystem_HDV       = 5,
		eAVEncInputVideoSystem_Component = 6
	;
	};
	
	// AVEncVideoHeaderDropFrame (UINT32)
	public static final String AVEncVideoHeaderDropFrame = "6ed9e124-7925-43fe-971b-e019f62222b4";
	
	// AVEncVideoHeaderHours (UINT32)
	public static final String AVEncVideoHeaderHours = "2acc7702-e2da-4158-bf9b-88880129d740";
	
	// AVEncVideoHeaderMinutes (UINT32)
	public static final String AVEncVideoHeaderMinutes = "dc1a99ce-0307-408b-880b-b8348ee8ca7f";
	
	// AVEncVideoHeaderSeconds (UINT32)
	public static final String AVEncVideoHeaderSeconds = "4a2e1a05-a780-4f58-8120-9a449d69656b";
	
	// AVEncVideoHeaderFrames (UINT32)
	public static final String AVEncVideoHeaderFrames = "afd5f567-5c1b-4adc-bdaf-735610381436";
	
	// AVEncVideoDefaultUpperFieldDominant (BOOL)
	public static final String AVEncVideoDefaultUpperFieldDominant = "810167c4-0bc1-47ca-8fc2-57055a1474a5";
	
	// AVEncVideoCBRMotionTradeoff (UINT32)
	public static final String AVEncVideoCBRMotionTradeoff = "0d49451e-18d5-4367-a4ef-3240df1693c4";
	
	// AVEncVideoCodedVideoAccessUnitSize (UINT32)
	public static final String AVEncVideoCodedVideoAccessUnitSize = "b4b10c15-14a7-4ce8-b173-dc90a0b4fcdb";
	
	// AVEncVideoMaxKeyframeDistance (UINT32)
	public static final String AVEncVideoMaxKeyframeDistance = "2987123a-ba93-4704-b489-ec1e5f25292c";
	
	//
	// Common Post-Encode Video Statistical Parameters
	//
	
	// AVEncStatVideoOutputFrameRate (UINT32/UINT32)
	public static final String AVEncStatVideoOutputFrameRate = "be747849-9ab4-4a63-98fe-f143f04f8ee9";
	
	// AVEncStatVideoCodedFrames (UINT32)
	public static final String AVEncStatVideoCodedFrames = "d47f8d61-6f5a-4a26-bb9f-cd9518462bcd";
	
	// AVEncStatVideoTotalFrames (UINT32)
	public static final String AVEncStatVideoTotalFrames = "fdaa9916-119a-4222-9ad6-3f7cab99cc8b";
	
	//
	// Common Audio Parameters
	//
	
	// AVEncAudioIntervalToEncode (UINT64)
	public static final String AVEncAudioIntervalToEncode = "866e4b4d-725a-467c-bb01-b496b23b25f9";
	
	// AVEncAudioIntervalToSkip (UINT64)
	public static final String AVEncAudioIntervalToSkip = "88c15f94-c38c-4796-a9e8-96e967983f26";
	
	// AVEncAudioDualMono (UINT32) - Read/Write
	// Some audio encoders can encode 2 channel input as "dual mono". Use this
	// property to set the appropriate field in the bitstream header to indicate that the 
	// 2 channel bitstream is or isn't dual mono. 
	// For encoding MPEG audio, use the DualChannel option in AVEncMPACodingMode instead
	public static final String AVEncAudioDualMono = "3648126b-a3e8-4329-9b3a-5ce566a43bd3";
	
	public interface eAVEncAudioDualMono
	{
		public static final int 
		eAVEncAudioDualMono_SameAsInput = 0, // As indicated by input media type 
		eAVEncAudioDualMono_Off         = 1,  // 2-ch output bitstream should not be dual mono
		eAVEncAudioDualMono_On          = 2   // 2-ch output bitstream should be dual mono
	;
	}; 
	
	// AVEncAudioMapDestChannel0..15 (UINT32)
	public static final String AVEncAudioMapDestChannel0 = "bc5d0b60-df6a-4e16-9803-b82007a30c8d";
	public static final String AVEncAudioMapDestChannel1 = "bc5d0b61-df6a-4e16-9803-b82007a30c8d";
	public static final String AVEncAudioMapDestChannel2 = "bc5d0b62-df6a-4e16-9803-b82007a30c8d";
	public static final String AVEncAudioMapDestChannel3 = "bc5d0b63-df6a-4e16-9803-b82007a30c8d";
	public static final String AVEncAudioMapDestChannel4 = "bc5d0b64-df6a-4e16-9803-b82007a30c8d";
	public static final String AVEncAudioMapDestChannel5 = "bc5d0b65-df6a-4e16-9803-b82007a30c8d";
	public static final String AVEncAudioMapDestChannel6 = "bc5d0b66-df6a-4e16-9803-b82007a30c8d";
	public static final String AVEncAudioMapDestChannel7 = "bc5d0b67-df6a-4e16-9803-b82007a30c8d";
	public static final String AVEncAudioMapDestChannel8 = "bc5d0b68-df6a-4e16-9803-b82007a30c8d";
	public static final String AVEncAudioMapDestChannel9 = "bc5d0b69-df6a-4e16-9803-b82007a30c8d";
	public static final String AVEncAudioMapDestChannel10 = "bc5d0b6a-df6a-4e16-9803-b82007a30c8d";
	public static final String AVEncAudioMapDestChannel11 = "bc5d0b6b-df6a-4e16-9803-b82007a30c8d";
	public static final String AVEncAudioMapDestChannel12 = "bc5d0b6c-df6a-4e16-9803-b82007a30c8d";
	public static final String AVEncAudioMapDestChannel13 = "bc5d0b6d-df6a-4e16-9803-b82007a30c8d";
	public static final String AVEncAudioMapDestChannel14 = "bc5d0b6e-df6a-4e16-9803-b82007a30c8d";
	public static final String AVEncAudioMapDestChannel15 = "bc5d0b6f-df6a-4e16-9803-b82007a30c8d";
	
	// AVEncAudioInputContent (UINT32) <---- You have ENUM in the doc
	public static final String AVEncAudioInputContent = "3e226c2b-60b9-4a39-b00b-a7b40f70d566";
	
	public interface eAVEncAudioInputContent
	{
		public static final int 
		AVEncAudioInputContent_Unknown =0,
		AVEncAudioInputContent_Voice = 1,
		AVEncAudioInputContent_Music = 2
	;
	};
	
	//
	// Common Post-Encode Audio Statistical Parameters
	//
	
	// AVEncStatAudioPeakPCMValue (UINT32)
	public static final String AVEncStatAudioPeakPCMValue = "dce7fd34-dc00-4c16-821b-35d9eb00fb1a";
	
	// AVEncStatAudioAveragePCMValue (UINT32)
	public static final String AVEncStatAudioAveragePCMValue = "979272f8-d17f-4e32-bb73-4e731c68ba2d";
	
	// AVEncStatAudioAverageBPS (UINT32)
	public static final String AVEncStatAudioAverageBPS = "ca6724db-7059-4351-8b43-f82198826a14";
	
	//
	// MPEG Video Encoding Interface
	//
	
	//
	// MPV Encoder Specific Parameters
	//
	
	// AVEncMPVGOPSize (UINT32)
	public static final String AVEncMPVGOPSize = "95f31b26-95a4-41aa-9303-246a7fc6eef1";
	
	// AVEncMPVGOPOpen (BOOL)
	public static final String AVEncMPVGOPOpen = "b1d5d4a6-3300-49b1-ae61-a09937ab0e49";
	
	// AVEncMPVDefaultBPictureCount (UINT32)
	public static final String AVEncMPVDefaultBPictureCount = "8d390aac-dc5c-4200-b57f-814d04babab2";
	
	// AVEncMPVProfile (UINT32) <---- You have GUID in the doc
	public static final String AVEncMPVProfile = "dabb534a-1d99-4284-975a-d90e2239baa1";
	
	public interface eAVEncMPVProfile
	{
		public static final int 
		eAVEncMPVProfile_unknown = 0,
		eAVEncMPVProfile_Simple = 1,
		eAVEncMPVProfile_Main   = 2,
		eAVEncMPVProfile_High   = 3,
		eAVEncMPVProfile_422    = 4
	;
	};
	
	// AVEncMPVLevel (UINT32) <---- You have GUID in the doc
	public static final String AVEncMPVLevel = "6ee40c40-a60c-41ef-8f50-37c2249e2cb3";
	
	public interface eAVEncMPVLevel
	{
		public static final int 
		eAVEncMPVLevel_Low      = 1,
		eAVEncMPVLevel_Main     = 2,
		eAVEncMPVLevel_High1440 = 3,
		eAVEncMPVLevel_High     = 4
	;
	};
	
	// AVEncMPVFrameFieldMode (UINT32)
	public static final String AVEncMPVFrameFieldMode = "acb5de96-7b93-4c2f-8825-b0295fa93bf4";
	
	public interface eAVEncMPVFrameFieldMode
	{
		public static final int 
		eAVEncMPVFrameFieldMode_FieldMode = 0,
		eAVEncMPVFrameFieldMode_FrameMode = 1
	;
	};
	
	//
	// Advanced MPV Encoder Specific Parameters
	//
	
	// AVEncMPVAddSeqEndCode (BOOL)
	public static final String AVEncMPVAddSeqEndCode = "a823178f-57df-4c7a-b8fd-e5ec8887708d";
	
	// AVEncMPVGOPSInSeq (UINT32)
	public static final String AVEncMPVGOPSInSeq = "993410d4-2691-4192-9978-98dc2603669f";
	
	// AVEncMPVUseConcealmentMotionVectors (BOOL)
	public static final String AVEncMPVUseConcealmentMotionVectors = "ec770cf3-6908-4b4b-aa30-7fb986214fea";
	
	// AVEncMPVSceneDetection (UINT32)
	public static final String AVEncMPVSceneDetection = "552799f1-db4c-405b-8a3a-c93f2d0674dc";
	
	public interface eAVEncMPVSceneDetection
	{
		public static final int 
		eAVEncMPVSceneDetection_None                 = 0,
		eAVEncMPVSceneDetection_InsertIPicture       = 1,
		eAVEncMPVSceneDetection_StartNewGOP          = 2,
		eAVEncMPVSceneDetection_StartNewLocatableGOP = 3
	;
	};
	
	// AVEncMPVGenerateHeaderSeqExt (BOOL)
	public static final String AVEncMPVGenerateHeaderSeqExt = "d5e78611-082d-4e6b-98af-0f51ab139222";
	
	// AVEncMPVGenerateHeaderSeqDispExt (BOOL)
	public static final String AVEncMPVGenerateHeaderSeqDispExt = "6437aa6f-5a3c-4de9-8a16-53d9c4ad326f";
	
	// AVEncMPVGenerateHeaderPicExt (BOOL)
	public static final String AVEncMPVGenerateHeaderPicExt = "1b8464ab-944f-45f0-b74e-3a58dad11f37";
	
	// AVEncMPVGenerateHeaderPicDispExt (BOOL)
	public static final String AVEncMPVGenerateHeaderPicDispExt = "c6412f84-c03f-4f40-a00c-4293df8395bb";
	
	// AVEncMPVGenerateHeaderSeqScaleExt (BOOL)
	public static final String AVEncMPVGenerateHeaderSeqScaleExt = "0722d62f-dd59-4a86-9cd5-644f8e2653d8";
	
	// AVEncMPVScanPattern (UINT32)
	public static final String AVEncMPVScanPattern = "7f8a478e-7bbb-4ae2-b2fc-96d17fc4a2d6";
	
	public interface eAVEncMPVScanPattern
	{
		public static final int 
		eAVEncMPVScanPattern_Auto          = 0,
		eAVEncMPVScanPattern_ZigZagScan    = 1,
		eAVEncMPVScanPattern_AlternateScan = 2
	;
	};
	
	// AVEncMPVIntraDCPrecision (UINT32)
	public static final String AVEncMPVIntraDCPrecision = "a0116151-cbc8-4af3-97dc-d00cceb82d79";
	
	// AVEncMPVQScaleType (UINT32)
	public static final String AVEncMPVQScaleType = "2b79ebb7-f484-4af7-bb58-a2a188c5cbbe";
	
	public interface eAVEncMPVQScaleType
	{
		public static final int 
		eAVEncMPVQScaleType_Auto      = 0,
		eAVEncMPVQScaleType_Linear    = 1,
		eAVEncMPVQScaleType_NonLinear = 2
	;
	};
	
	// AVEncMPVIntraVLCTable (UINT32)
	public static final String AVEncMPVIntraVLCTable = "a2b83ff5-1a99-405a-af95-c5997d558d3a";
	
	public interface eAVEncMPVIntraVLCTable
	{
		public static final int 
		eAVEncMPVIntraVLCTable_Auto      = 0,
		eAVEncMPVIntraVLCTable_MPEG1     = 1,
		eAVEncMPVIntraVLCTable_Alternate = 2
	;
	};
	
	// AVEncMPVQuantMatrixIntra (BYTE[64] encoded as a string of 128 hex digits)
	public static final String AVEncMPVQuantMatrixIntra = "9bea04f3-6621-442c-8ba1-3ac378979698";
	
	// AVEncMPVQuantMatrixNonIntra (BYTE[64] encoded as a string of 128 hex digits)
	public static final String AVEncMPVQuantMatrixNonIntra = "87f441d8-0997-4beb-a08e-8573d409cf75";
	
	// AVEncMPVQuantMatrixChromaIntra (BYTE[64] encoded as a string of 128 hex digits)
	public static final String AVEncMPVQuantMatrixChromaIntra = "9eb9ecd4-018d-4ffd-8f2d-39e49f07b17a";
	
	// AVEncMPVQuantMatrixChromaNonIntra (BYTE[64] encoded as a string of 128 hex digits)
	public static final String AVEncMPVQuantMatrixChromaNonIntra = "1415b6b1-362a-4338-ba9a-1ef58703c05b";
	
	//
	// MPEG1 Audio Encoding Interface
	//
	
	//
	// MPEG1 Audio Specific Parameters
	//
	
	// AVEncMPALayer (UINT)
	public static final String AVEncMPALayer = "9d377230-f91b-453d-9ce0-78445414c22d";
	
	public interface eAVEncMPALayer
	{
		public static final int 
		eAVEncMPALayer_1 = 1,
		eAVEncMPALayer_2 = 2,
		eAVEncMPALayer_3 = 3
	;
	};
	
	// AVEncMPACodingMode (UINT)
	public static final String AVEncMPACodingMode = "b16ade03-4b93-43d7-a550-90b4fe224537";
	
	public interface eAVEncMPACodingMode
	{
		public static final int 
		eAVEncMPACodingMode_Mono        = 0,
		eAVEncMPACodingMode_Stereo      = 1,
		eAVEncMPACodingMode_DualChannel = 2,
		eAVEncMPACodingMode_JointStereo = 3,
		eAVEncMPACodingMode_Surround    = 4
	;
	};
	
	// AVEncMPACopyright (BOOL) - default state to encode into the stream (may be overridden by input)
	// 1 (true)  - copyright protected
	// 0 (false) - not copyright protected 
	public static final String AVEncMPACopyright = "a6ae762a-d0a9-4454-b8ef-f2dbeefdd3bd";
	
	// AVEncMPAOriginalBitstream (BOOL) - default value to encode into the stream (may be overridden by input)
	// 1 (true)  - for original bitstream
	// 0 (false) - for copy bitstream 
	public static final String AVEncMPAOriginalBitstream = "3cfb7855-9cc9-47ff-b829-b36786c92346";
	
	// AVEncMPAEnableRedundancyProtection (BOOL) 
	// 1 (true)  -  Redundancy should be added to facilitate error detection and concealment (CRC)
	// 0 (false) -  No redundancy should be added
	public static final String AVEncMPAEnableRedundancyProtection = "5e54b09e-b2e7-4973-a89b-0b3650a3beda";
	
	// AVEncMPAPrivateUserBit (UINT) - User data bit value to encode in the stream
	public static final String AVEncMPAPrivateUserBit = "afa505ce-c1e3-4e3d-851b-61b700e5e6cc";
	
	// AVEncMPAEmphasisType (UINT)
	// Indicates type of de-emphasis filter to be used
	public static final String AVEncMPAEmphasisType = "2d59fcda-bf4e-4ed6-b5df-5b03b36b0a1f";
	
	public interface eAVEncMPAEmphasisType
	{
		public static final int 
		eAVEncMPAEmphasisType_None        = 0,
		eAVEncMPAEmphasisType_50_15       = 1,
		eAVEncMPAEmphasisType_Reserved    = 2,
		eAVEncMPAEmphasisType_CCITT_J17   = 3;
	};
	
	//
	// Dolby Digital(TM) Audio Encoding Interface
	//
	
	//
	// Dolby Digital(TM) Audio Specific Parameters
	//
	
	// AVEncDDService (UINT)
	public static final String AVEncDDService = "d2e1bec7-5172-4d2a-a50e-2f3b82b1ddf8";
	
	public interface eAVEncDDService
	{
		public static final int 
		eAVEncDDService_CM = 0,  // (Main Service: Complete Main)
		eAVEncDDService_ME = 1,  // (Main Service: Music and Effects (ME))
		eAVEncDDService_VI = 2,  // (Associated Service: Visually-Impaired (VI)
		eAVEncDDService_HI = 3,  // (Associated Service: Hearing-Impaired (HI))
		eAVEncDDService_D  = 4,  // (Associated Service: Dialog (D))
		eAVEncDDService_C  = 5,  // (Associated Service: Commentary (C))
		eAVEncDDService_E  = 6,  // (Associated Service: Emergency (E))
		eAVEncDDService_VO = 7   // (Associated Service: Voice Over (VO) / Karaoke)
	;
	};
	
	// AVEncDDDialogNormalization (UINT32)
	public static final String AVEncDDDialogNormalization = "d7055acf-f125-437d-a704-79c79f0404a8";
	
	// AVEncDDCentreDownMixLevel (UINT32)
	public static final String AVEncDDCentreDownMixLevel = "e285072c-c958-4a81-afd2-e5e0daf1b148";
	
	// AVEncDDSurroundDownMixLevel (UINT32)
	public static final String AVEncDDSurroundDownMixLevel = "7b20d6e5-0bcf-4273-a487-506b047997e9";
	
	// AVEncDDProductionInfoExists (BOOL)
	public static final String AVEncDDProductionInfoExists = "b0b7fe5f-b6ab-4f40-964d-8d91f17c19e8";
	
	// AVEncDDProductionRoomType (UINT32)
	public static final String AVEncDDProductionRoomType = "dad7ad60-23d8-4ab7-a284-556986d8a6fe";
	
	public interface eAVEncDDProductionRoomType
	{
		public static final int 
		eAVEncDDProductionRoomType_NotIndicated = 0,
		eAVEncDDProductionRoomType_Large        = 1,
		eAVEncDDProductionRoomType_Small        = 2
	;
	};
	
	// AVEncDDProductionMixLevel (UINT32)
	public static final String AVEncDDProductionMixLevel = "301d103a-cbf9-4776-8899-7c15b461ab26";
	
	// AVEncDDCopyright (BOOL)
	public static final String AVEncDDCopyright = "8694f076-cd75-481d-a5c6-a904dcc828f0";
	
	// AVEncDDOriginalBitstream (BOOL)
	public static final String AVEncDDOriginalBitstream = "966ae800-5bd3-4ff9-95b9-d30566273856";
	
	// AVEncDDDigitalDeemphasis (BOOL)
	public static final String AVEncDDDigitalDeemphasis = "e024a2c2-947c-45ac-87d8-f1030c5c0082";
	
	// AVEncDDDCHighPassFilter (BOOL)
	public static final String AVEncDDDCHighPassFilter = "9565239f-861c-4ac8-bfda-e00cb4db8548";
	
	// AVEncDDChannelBWLowPassFilter (BOOL)
	public static final String AVEncDDChannelBWLowPassFilter = "e197821d-d2e7-43e2-ad2c-00582f518545";
	
	// AVEncDDLFELowPassFilter (BOOL)
	public static final String AVEncDDLFELowPassFilter = "d3b80f6f-9d15-45e5-91be-019c3fab1f01";
	
	// AVEncDDSurround90DegreeePhaseShift (BOOL)
	public static final String AVEncDDSurround90DegreeePhaseShift = "25ecec9d-3553-42c0-bb56-d25792104f80";
	
	// AVEncDDSurround3dBAttenuation (BOOL)
	public static final String AVEncDDSurround3dBAttenuation = "4d43b99d-31e2-48b9-bf2e-5cbf1a572784";
	
	// AVEncDDDynamicRangeCompressionControl (UINT32)
	public static final String AVEncDDDynamicRangeCompressionControl = "cfc2ff6d-79b8-4b8d-a8aa-a0c9bd1c2940";
	
	public interface eAVEncDDDynamicRangeCompressionControl
	{
		public static final int 
		eAVEncDDDynamicRangeCompressionControl_None          = 0,
		eAVEncDDDynamicRangeCompressionControl_FilmStandard  = 1,
		eAVEncDDDynamicRangeCompressionControl_FilmLight     = 2,
		eAVEncDDDynamicRangeCompressionControl_MusicStandard = 3,
		eAVEncDDDynamicRangeCompressionControl_MusicLight    = 4,
		eAVEncDDDynamicRangeCompressionControl_Speech        = 5
	;
	};
	
	// AVEncDDRFPreEmphasisFilter (BOOL)
	public static final String AVEncDDRFPreEmphasisFilter = "21af44c0-244e-4f3d-a2cc-3d3068b2e73f";
	
	// AVEncDDSurroundExMode (UINT32)
	public static final String AVEncDDSurroundExMode = "91607cee-dbdd-4eb6-bca2-aadfafa3dd68";
	
	public interface eAVEncDDSurroundExMode
	{
		public static final int 
		eAVEncDDSurroundExMode_NotIndicated = 0,
		eAVEncDDSurroundExMode_No           = 1,
		eAVEncDDSurroundExMode_Yes          = 2
	;
	};
	
	// AVEncDDPreferredStereoDownMixMode (UINT32)
	public static final String AVEncDDPreferredStereoDownMixMode = "7f4e6b31-9185-403d-b0a2-763743e6f063";
	
	public interface eAVEncDDPreferredStereoDownMixMode
	{
		public static final int 
		eAVEncDDPreferredStereoDownMixMode_LtRt = 0,
		eAVEncDDPreferredStereoDownMixMode_LoRo = 1
	;
	};
	
	// AVEncDDLtRtCenterMixLvl_x10 (INT32)
	public static final String AVEncDDLtRtCenterMixLvl_x10 = "dca128a2-491f-4600-b2da-76e3344b4197";
	
	// AVEncDDLtRtSurroundMixLvl_x10 (INT32)
	public static final String AVEncDDLtRtSurroundMixLvl_x10 = "212246c7-3d2c-4dfa-bc21-652a9098690d";
	
	// AVEncDDLoRoCenterMixLvl (INT32)
	public static final String AVEncDDLoRoCenterMixLvl_x10 = "1cfba222-25b3-4bf4-9bfd-e7111267858c";
	
	// AVEncDDLoRoSurroundMixLvl_x10 (INT32)
	public static final String AVEncDDLoRoSurroundMixLvl_x10 = "e725cff6-eb56-40c7-8450-2b9367e91555";
	
	// AVEncDDAtoDConverterType (UINT32)
	public static final String AVEncDDAtoDConverterType = "719f9612-81a1-47e0-9a05-d94ad5fca948";
	
	public interface eAVEncDDAtoDConverterType
	{
		public static final int 
		eAVEncDDAtoDConverterType_Standard = 0,
		eAVEncDDAtoDConverterType_HDCD     = 1
	;
	};
	
	// AVEncDDHeadphoneMode (UINT32)
	public static final String AVEncDDHeadphoneMode = "4052dbec-52f5-42f5-9b00-d134b1341b9d";
	
	public interface eAVEncDDHeadphoneMode
	{
		public static final int 
		eAVEncDDHeadphoneMode_NotIndicated = 0,
		eAVEncDDHeadphoneMode_NotEncoded   = 1,
		eAVEncDDHeadphoneMode_Encoded      = 2
	;
	};
	
	//
	// WMV Video Encoding Interface
	//
	
	//
	// WMV Video Specific Parameters
	//
	
	// AVEncWMVKeyFrameDistance (UINT32)
	public static final String AVEncWMVKeyFrameDistance = "5569055e-e268-4771-b83e-9555ea28aed3";
	
	// AVEncWMVInterlacedEncoding (UINT32)
	public static final String AVEncWMVInterlacedEncoding = "e3d00f8a-c6f5-4e14-a588-0ec87a726f9b";
	
	// AVEncWMVDecoderComplexity (UINT32)
	public static final String AVEncWMVDecoderComplexity = "f32c0dab-f3cb-4217-b79f-8762768b5f67";
	
	// AVEncWMVHasKeyFrameBufferLevelMarker (BOOL)
	public static final String AVEncWMVKeyFrameBufferLevelMarker = "51ff1115-33ac-426c-a1b1-09321bdf96b4";
	
	// AVEncWMVProduceDummyFrames (UINT32)
	public static final String AVEncWMVProduceDummyFrames = "d669d001-183c-42e3-a3ca-2f4586d2396c";
	
	//
	// WMV Post-Encode Statistical Parameters
	//
	
	// AVEncStatWMVCBAvg (UINT32/UINT32)
	public static final String AVEncStatWMVCBAvg = "6aa6229f-d602-4b9d-b68c-c1ad78884bef";
	
	// AVEncStatWMVCBMax (UINT32/UINT32)
	public static final String AVEncStatWMVCBMax = "e976bef8-00fe-44b4-b625-8f238bc03499";
	
	// AVEncStatWMVDecoderComplexityProfile (UINT32)
	public static final String AVEncStatWMVDecoderComplexityProfile = "89e69fc3-0f9b-436c-974a-df821227c90d";
	
	// AVEncStatMPVSkippedEmptyFrames (UINT32)
	public static final String AVEncStatMPVSkippedEmptyFrames = "32195fd3-590d-4812-a7ed-6d639a1f9711";
	
	//
	// MPEG1/2 Multiplexer Interfaces
	//
	
	//
	// MPEG1/2 Packetizer Interface
	//
	
	// Shared with Mux:
	// AVEncMP12MuxEarliestPTS (UINT32)
	// AVEncMP12MuxLargestPacketSize (UINT32)
	// AVEncMP12MuxSysSTDBufferBound (UINT32)
	
	// AVEncMP12PktzSTDBuffer (UINT32)
	public static final String AVEncMP12PktzSTDBuffer = "0b751bd0-819e-478c-9435-75208926b377";
	
	// AVEncMP12PktzStreamID (UINT32)
	public static final String AVEncMP12PktzStreamID = "c834d038-f5e8-4408-9b60-88f36493fedf";
	
	// AVEncMP12PktzInitialPTS (UINT32)
	public static final String AVEncMP12PktzInitialPTS = "2a4f2065-9a63-4d20-ae22-0a1bc896a315";
	
	// AVEncMP12PktzPacketSize (UINT32)
	public static final String AVEncMP12PktzPacketSize = "ab71347a-1332-4dde-a0e5-ccf7da8a0f22";
	
	// AVEncMP12PktzCopyright (BOOL)
	public static final String AVEncMP12PktzCopyright = "c8f4b0c1-094c-43c7-8e68-a595405a6ef8";
	
	// AVEncMP12PktzOriginal (BOOL)
	public static final String AVEncMP12PktzOriginal = "6b178416-31b9-4964-94cb-6bff866cdf83";
	
	//
	// MPEG1/2 Multiplexer Interface
	//
	
	// AVEncMP12MuxPacketOverhead (UINT32)
	public static final String AVEncMP12MuxPacketOverhead = "e40bd720-3955-4453-acf9-b79132a38fa0";
	
	// AVEncMP12MuxNumStreams (UINT32)
	public static final String AVEncMP12MuxNumStreams = "f7164a41-dced-4659-a8f2-fb693f2a4cd0";
	
	// AVEncMP12MuxEarliestPTS (UINT32)
	public static final String AVEncMP12MuxEarliestPTS = "157232b6-f809-474e-9464-a7f93014a817";
	
	// AVEncMP12MuxLargestPacketSize (UINT32)
	public static final String AVEncMP12MuxLargestPacketSize = "35ceb711-f461-4b92-a4ef-17b6841ed254";
	
	// AVEncMP12MuxInitialSCR (UINT32)
	public static final String AVEncMP12MuxInitialSCR = "3433ad21-1b91-4a0b-b190-2b77063b63a4";
	
	// AVEncMP12MuxMuxRate (UINT32)
	public static final String AVEncMP12MuxMuxRate = "ee047c72-4bdb-4a9d-8e21-41926c823da7";
	
	// AVEncMP12MuxPackSize (UINT32)
	public static final String AVEncMP12MuxPackSize = "f916053a-1ce8-4faf-aa0b-ba31c80034b8";
	
	// AVEncMP12MuxSysSTDBufferBound (UINT32)
	public static final String AVEncMP12MuxSysSTDBufferBound = "35746903-b545-43e7-bb35-c5e0a7d5093c";
	
	// AVEncMP12MuxSysRateBound (UINT32)
	public static final String AVEncMP12MuxSysRateBound = "05f0428a-ee30-489d-ae28-205c72446710";
	
	// AVEncMP12MuxTargetPacketizer (UINT32)
	public static final String AVEncMP12MuxTargetPacketizer = "d862212a-2015-45dd-9a32-1b3aa88205a0";
	
	// AVEncMP12MuxSysFixed (UINT32)
	public static final String AVEncMP12MuxSysFixed = "cefb987e-894f-452e-8f89-a4ef8cec063a";
	
	// AVEncMP12MuxSysCSPS (UINT32)
	public static final String AVEncMP12MuxSysCSPS = "7952ff45-9c0d-4822-bc82-8ad772e02993";
	
	// AVEncMP12MuxSysVideoLock (BOOL)
	public static final String AVEncMP12MuxSysVideoLock = "b8296408-2430-4d37-a2a1-95b3e435a91d";
	
	// AVEncMP12MuxSysAudioLock (BOOL)
	public static final String AVEncMP12MuxSysAudioLock = "0fbb5752-1d43-47bf-bd79-f2293d8ce337";
	
	// AVEncMP12MuxDVDNavPacks (BOOL)
	public static final String AVEncMP12MuxDVDNavPacks = "c7607ced-8cf1-4a99-83a1-ee5461be3574";
	
	//
	// Decoding Interface
	//
	
	
	// format values are GUIDs as VARIANT BSTRs 
	public static final String AVDecCommonInputFormat = "E5005239-BD89-4be3-9C0F-5DDE317988CC";
	public static final String AVDecCommonOutputFormat = "3c790028-c0ce-4256-b1a2-1b0fc8b1dcdc";
	
	// AVDecCommonMeanBitRate - Mean bitrate in mbits/sec (UINT32)
	public static final String AVDecCommonMeanBitRate = "59488217-007A-4f7a-8E41-5C48B1EAC5C6";
	// AVDecCommonMeanBitRateInterval - Mean bitrate interval (in 100ns) (UINT64)
	public static final String AVDecCommonMeanBitRateInterval = "0EE437C6-38A7-4c5c-944C-68AB42116B85";
	
	//
	// Audio Decoding Interface
	//
	
	// Value GUIDS
	// The following 6 GUIDs are values of the AVDecCommonOutputFormat property
	//
	// Stereo PCM output using matrix-encoded stereo down mix (aka Lt/Rt) 
	public static final String GUID_AVDecAudioOutputFormat_PCM_Stereo_MatrixEncoded = "696E1D30-548F-4036-825F-7026C60011BD";
	//
	// Regular PCM output (any number of channels) 
	public static final String GUID_AVDecAudioOutputFormat_PCM = "696E1D31-548F-4036-825F-7026C60011BD";
	//
	// SPDIF PCM (IEC 60958) stereo output. Type of stereo down mix should
	// be specified by the application.
	public static final String GUID_AVDecAudioOutputFormat_SPDIF_PCM = "696E1D32-548F-4036-825F-7026C60011BD";
	//
	// SPDIF bitstream (IEC 61937) output, such as AC3, MPEG or DTS.
	public static final String GUID_AVDecAudioOutputFormat_SPDIF_Bitstream = "696E1D33-548F-4036-825F-7026C60011BD";
	//
	// Stereo PCM output using regular stereo down mix (aka Lo/Ro)
	public static final String GUID_AVDecAudioOutputFormat_PCM_Headphones = "696E1D34-548F-4036-825F-7026C60011BD";
	
	// Stereo PCM output using automatic selection of stereo down mix 
	// mode (Lo/Ro or Lt/Rt). Use this when the input stream includes
	// information about the preferred downmix mode (such as Annex D of AC3).
	// Default down mix mode should be specified by the application.
	public static final String GUID_AVDecAudioOutputFormat_PCM_Stereo_Auto = "696E1D35-548F-4036-825F-7026C60011BD";
	
	//
	// Video Decoder properties
	//
	
	// AVDecVideoImageSize (UINT32) - High UINT16 width, low UINT16 height
	public static final String AVDecVideoImageSize = "5EE5747C-6801-4cab-AAF1-6248FA841BA4";
	
	// AVDecVideoPixelAspectRatio (UINT32 as UINT16/UNIT16) - High UINT16 width, low UINT16 height
	public static final String AVDecVideoPixelAspectRatio = "B0CF8245-F32D-41df-B02C-87BD304D12AB";
	
	// AVDecVideoInputScanType (UINT32)
	public static final String AVDecVideoInputScanType = "38477E1F-0EA7-42cd-8CD1-130CED57C580";
	public interface eAVDecVideoInputScanType
	{
		public static final int 
		eAVDecVideoInputScan_Unknown           = 0,
		eAVDecVideoInputScan_Progressive       = 1, 
		eAVDecVideoInputScan_Interlaced_UpperFieldFirst = 2,
		eAVDecVideoInputScan_Interlaced_LowerFieldFirst = 3
	;
	};
	
	//
	// Audio Decoder properties
	//
	
	
	public static final String GUID_AVDecAudioInputWMA = "C95E8DCF-4058-4204-8C42-CB24D91E4B9B";
	public static final String GUID_AVDecAudioInputWMAPro = "0128B7C7-DA72-4fe3-BEF8-5C52E3557704";
	public static final String GUID_AVDecAudioInputDolby = "8E4228A0-F000-4e0b-8F54-AB8D24AD61A2";
	public static final String GUID_AVDecAudioInputDTS = "600BC0CA-6A1F-4e91-B241-1BBEB1CB19E0";
	public static final String GUID_AVDecAudioInputPCM = "F2421DA5-BBB4-4cd5-A996-933C6B5D1347";
	public static final String GUID_AVDecAudioInputMPEG = "91106F36-02C5-4f75-9719-3B7ABF75E1F6";
	
	// AVDecAudioDualMono (UINT32) - Read only
	// The input bitstream header might have a field indicating whether the 2-ch bitstream
	// is dual mono or not. Use this property to read this field.
	// If it's dual mono, the application can set AVDecAudioDualMonoReproMode to determine
	// one of 4 reproduction modes
	public static final String AVDecAudioDualMono = "4a52cda8-30f8-4216-be0f-ba0b2025921d";
	
	public interface eAVDecAudioDualMono
	{
		public static final int 
		eAVDecAudioDualMono_IsNotDualMono = 0, // 2-ch bitstream input is not dual mono
		eAVDecAudioDualMono_IsDualMono    = 1, // 2-ch bitstream input is dual mono
		eAVDecAudioDualMono_UnSpecified   = 2  // There is no indication in the bitstream 
	;
	}; 
	
	// AVDecAudioDualMonoReproMode (UINT32)
	// Reproduction modes for programs containing two independent mono channels (Ch1 & Ch2).
	// In case of 2-ch input, the decoder should get AVDecAudioDualMono to check if the input
	// is regular stereo or dual mono. If dual mono, the application can ask the user to set the playback
	// mode by setting AVDecAudioDualReproMonoMode. If output is not stereo, use AVDecDDMatrixDecodingMode or
	// equivalent.
	public static final String AVDecAudioDualMonoReproMode = "a5106186-cc94-4bc9-8cd9-aa2f61f6807e";
	
	public interface eAVDecAudioDualMonoReproMode
	{
		public static final int 
		eAVDecAudioDualMonoReproMode_STEREO      = 0, // Ch1+Ch2 for mono output, (Ch1 left,     Ch2 right) for stereo output
		eAVDecAudioDualMonoReproMode_LEFT_MONO   = 1, // Ch1 for mono output,     (Ch1 left,     Ch1 right) for stereo output
		eAVDecAudioDualMonoReproMode_RIGHT_MONO  = 2, // Ch2 for mono output,     (Ch2 left,     Ch2 right) for stereo output
		eAVDecAudioDualMonoReproMode_MIX_MONO    = 3 // Ch1+Ch2 for mono output, (Ch1+Ch2 left, Ch1+Ch2 right) for stereo output
	;
	};
	
	//
	// Audio Common Properties
	//
	
	// AVAudioChannelCount (UINT32)
	// Total number of audio channels, including LFE if it exists.
	public static final String AVAudioChannelCount = "1d3583c4-1583-474e-b71a-5ee463c198e4";
	
	// AVAudioChannelConfig (UINT32)
	// A bit-wise OR of any number of enum values specified by eAVAudioChannelConfig
	public static final String AVAudioChannelConfig = "17f89cb3-c38d-4368-9ede-63b94d177f9f";
	
	// Enumerated values for  AVAudioChannelConfig are identical 
	// to the speaker positions defined in ksmedia.h and used 
	// in WAVE_FORMAT_EXTENSIBLE. Configurations for 5.1 and
	// 7.1 channels should be identical to KSAUDIO_SPEAKER_5POINT1_SURROUND
	// and KSAUDIO_SPEAKER_7POINT1_SURROUND in ksmedia.h. This means:
	// 5.1 ch -> LOW_FREQUENCY | FRONT_LEFT | FRONT_RIGHT | FRONT_CENTER | SIDE_LEFT | SIDE_RIGHT 
	// 7.1 ch -> LOW_FREQUENCY | FRONT_LEFT | FRONT_RIGHT | FRONT_CENTER | SIDE_LEFT | SIDE_RIGHT | BACK_LEFT | BACK_RIGHT
	//
	public interface eAVAudioChannelConfig
	{
		public static final int 
		eAVAudioChannelConfig_FRONT_LEFT    = 0x1, 
		eAVAudioChannelConfig_FRONT_RIGHT   = 0x2, 
		eAVAudioChannelConfig_FRONT_CENTER  = 0x4, 
		eAVAudioChannelConfig_LOW_FREQUENCY = 0x8,  // aka LFE
		eAVAudioChannelConfig_BACK_LEFT     = 0x10, 
		eAVAudioChannelConfig_BACK_RIGHT    = 0x20, 
		eAVAudioChannelConfig_FRONT_LEFT_OF_CENTER  = 0x40, 
		eAVAudioChannelConfig_FRONT_RIGHT_OF_CENTER = 0x80, 
		eAVAudioChannelConfig_BACK_CENTER = 0x100,  // aka Mono Surround 
		eAVAudioChannelConfig_SIDE_LEFT   = 0x200,  // aka Left Surround
		eAVAudioChannelConfig_SIDE_RIGHT  = 0x400,  // aka Right Surround
		eAVAudioChannelConfig_TOP_CENTER  = 0x800, 
		eAVAudioChannelConfig_TOP_FRONT_LEFT   = 0x1000, 
		eAVAudioChannelConfig_TOP_FRONT_CENTER = 0x2000, 
		eAVAudioChannelConfig_TOP_FRONT_RIGHT  = 0x4000, 
		eAVAudioChannelConfig_TOP_BACK_LEFT    = 0x8000, 
		eAVAudioChannelConfig_TOP_BACK_CENTER  = 0x10000, 
		eAVAudioChannelConfig_TOP_BACK_RIGHT   = 0x20000 
	;
	};
	
	// AVAudioSampleRate (UINT32)
	// In samples per second (Hz)
	public static final String AVAudioSampleRate = "971d2723-1acb-42e7-855c-520a4b70a5f2";
	
	//
	// Dolby Digital(TM) Audio Specific Parameters
	//
	
	// AVDDSurroundMode (UINT32) common to encoder/decoder
	public static final String AVDDSurroundMode = "99f2f386-98d1-4452-a163-abc78a6eb770";
	
	public interface eAVDDSurroundMode
	{
		public static final int 
		eAVDDSurroundMode_NotIndicated = 0,
		eAVDDSurroundMode_No           = 1,
		eAVDDSurroundMode_Yes          = 2
	;
	};
	
	// AVDecDDOperationalMode (UINT32)
	public static final String AVDecDDOperationalMode = "d6d6c6d1-064e-4fdd-a40e-3ecbfcb7ebd0";
	
	public interface eAVDecDDOperationalMode
	{
		public static final int 
		eAVDecDDOperationalMode_NONE    = 0,
		eAVDecDDOperationalMode_LINE    = 1,// Dialnorm enabled, dialogue at -31dBFS, dynrng used, high/low scaling allowed  
		eAVDecDDOperationalMode_RF      = 2,// Dialnorm enabled, dialogue at -20dBFS, dynrng & compr used, high/low scaling NOT allowed (always fully compressed)
		eAVDecDDOperationalMode_CUSTOM0 = 3,// Analog dialnorm (dialogue normalization not part of the decoder)
		eAVDecDDOperationalMode_CUSTOM1 = 4 // Digital dialnorm (dialogue normalization is part of the decoder)
	;
	};
	
	// AVDecDDMatrixDecodingMode(UINT32)
	// A ProLogic decoder has a built-in auto-detection feature. When the Dolby Digital decoder 
	// is set to the 6-channel output configuration and it is fed a 2/0 bit stream to decode, it can 
	// do one of the following: 
	// a) decode the bit stream and output it on the two front channels (eAVDecDDMatrixDecodingMode_OFF), 
	// b) decode the bit stream followed by ProLogic decoding to create 6-channels (eAVDecDDMatrixDecodingMode_ON). 
	// c) the decoder will look at the Surround bit ("dsurmod") in the bit stream to determine whether 
	//    apply ProLogic decoding or not (eAVDecDDMatrixDecodingMode_AUTO).
	public static final String AVDecDDMatrixDecodingMode = "ddc811a5-04ed-4bf3-a0ca-d00449f9355f";
	
	public interface eAVDecDDMatrixDecodingMode
	{
		public static final int 
		eAVDecDDMatrixDecodingMode_OFF  = 0,  
		eAVDecDDMatrixDecodingMode_ON   = 1,  
		eAVDecDDMatrixDecodingMode_AUTO = 2   
	;
	};
	
	// AVDecDDDynamicRangeScaleHigh (UINT32) 
	// Indicates what fraction of the dynamic range compression
	// to apply. Relevant for negative values of dynrng only.
	// Linear range 0-100, where:
	//   0 - No dynamic range compression (preserve full dynamic range)
	// 100 - Apply full dynamic range compression 
	public static final String AVDecDDDynamicRangeScaleHigh = "50196c21-1f33-4af5-b296-11426d6c8789";
	
	
	// AVDecDDDynamicRangeScaleLow (UINT32) 
	// Indicates what fraction of the dynamic range compression
	// to apply. Relevant for positive values of dynrng only.
	// Linear range 0-100, where:
	//   0 - No dynamic range compression (preserve full dynamic range)
	// 100 - Apply full dynamic range compression 
	public static final String AVDecDDDynamicRangeScaleLow = "044e62e4-11a5-42d5-a3b2-3bb2c7c2d7cf";

}
