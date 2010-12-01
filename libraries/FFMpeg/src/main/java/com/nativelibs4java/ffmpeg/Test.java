package com.nativelibs4java.ffmpeg;

import java.io.FileNotFoundException;
import org.bridj.BridJ;
import java.io.File;
import static com.nativelibs4java.ffmpeg.avcodec.AvcodecLibrary.*;
import static com.nativelibs4java.ffmpeg.avformat.AvformatLibrary.*;
import static com.nativelibs4java.ffmpeg.avutil.AvutilLibrary.*;
import static com.nativelibs4java.ffmpeg.swscale.SwscaleLibrary.*;

import com.nativelibs4java.ffmpeg.avcodec.*;
import com.nativelibs4java.ffmpeg.avformat.*;
import com.nativelibs4java.ffmpeg.avutil.*;
import com.nativelibs4java.ffmpeg.swscale.*;

import org.bridj.Pointer;
import static org.bridj.Pointer.*;
import org.bridj.ValuedEnum;

/**
 * Converted to Java+BridJ from this post: http://dranger.com/ffmpeg/tutorial01.html
 * @author ochafik
 */
public class Test {

    public static void main(String[] args) throws FileNotFoundException {
        String file = "/Users/ochafik/Movies/Existenz.avi";

        BridJ.getNativeLibrary("avcodec", new File("/usr/local/lib/libavcodec.dylib"));
        BridJ.getNativeLibrary("avdevice", new File("/usr/local/lib/libavdevice.dylib"));
        BridJ.getNativeLibrary("avformat", new File("/usr/local/lib/libavformat.dylib"));
        BridJ.getNativeLibrary("avutil", new File("/usr/local/lib/libavutil.dylib"));
        //BridJ.getNativeLibrary("postproc", new File("/usr/local/lib/libpostproc.dylib"));
        BridJ.getNativeLibrary("swscale", new File("/usr/local/lib/libswscale.dylib"));


        av_register_all();

        Pointer<Pointer<AVFormatContext>> ppFormatCtx = allocatePointer(AVFormatContext.class);
        Pointer<Byte> filePtr = pointerToCString(file);
        if (av_open_input_file(ppFormatCtx, filePtr, NULL, 0, NULL) != 0) {
            error("Couldn't open file");
        }

        Pointer<AVFormatContext> pFormatCtx = ppFormatCtx.get();
        if (av_find_stream_info(pFormatCtx) < 0) {
            error("Couldn't find stream information");
        }

        dump_format(pFormatCtx, 0, filePtr, 0);


        // Find the first video stream
        int videoStream = -1;
        AVFormatContext formatCtx = pFormatCtx.get();
        for (int i = 0; i < formatCtx.nb_streams(); i++) {
            Pointer<AVStream> pStream = formatCtx.streams().get(i);
            if (pStream == null)
                continue;
            pStream = pStream.as(AVStream.class); // TODO FIX BRIDJ AND REMOVE THIS LINE
            AVStream stream = pStream.get();
            Pointer<AVCodecContext> pCodec = stream.codec();
            if (pCodec == null)
                continue;

            //String codecName = pCodec.get().codec_name().getCString();
            //System.out.println("Codec Name = " + codecName);
            AVCodecContext codec = pCodec.get();
            ValuedEnum<AVMediaType> codec_type = codec.codec_type();
            if (codec_type == AVMediaType.AVMEDIA_TYPE_VIDEO) {
                videoStream = i;
                break;
            }
        }
        if (videoStream == -1) {
            error("Didn't find a video stream");
        }

        // Get a pointer to the codec context for the video stream
        Pointer<AVCodecContext> pCodecCtx = formatCtx.streams().get(videoStream).get().codec();
        AVCodecContext codecCtx = pCodecCtx.get();

        // Find the decoder for the video stream
        Pointer<AVCodec> pCodec = avcodec_find_decoder(codecCtx.codec_id());
        if (pCodec == NULL) {
            error("Unsupported codec !");
        }

        // Open codec
        if (avcodec_open(pCodecCtx, pCodec) < 0) {
            error("Could not open codec");
        }

        // Allocate video frame
        Pointer<AVFrame> pFrame = avcodec_alloc_frame();
        Pointer<AVFrame> pFrameRGB = avcodec_alloc_frame();
        if (pFrame == NULL || pFrameRGB == NULL) {
            error("Failed to allocate frame");
        }

        // Determine required buffer size and allocate buffer
        int numBytes = avpicture_get_size(PixelFormat.PIX_FMT_RGB24, codecCtx.width(), codecCtx.height());

        Pointer<Byte> buffer = allocateBytes(numBytes); // TODO use av_malloc instead ?

        // Assign appropriate parts of buffer to image planes in pFrameRGB
        // Note that pFrameRGB is an AVFrame, but AVFrame is a superset
        // of AVPicture
        avpicture_fill(pFrameRGB.as(AVPicture.class), buffer, PixelFormat.PIX_FMT_RGB24, codecCtx.width(), codecCtx.height());

        {
            Pointer<Integer> pFrameFinished = allocateInt();
            Pointer<AVPacket> pPacket = allocate(AVPacket.class);
            AVPacket packet = pPacket.get();

            int i = 0;
            while (av_read_frame(pFormatCtx, pPacket) >= 0) {
                // Is this a packet from the video stream?
                if (packet.stream_index() == videoStream) {
                    // Decode video frame
                    avcodec_decode_video(pCodecCtx, pFrame, pFrameFinished, packet.data(), packet.size());

                    // Did we get a video frame?
                    if (pFrameFinished.get() != 0) {
                        // Convert the image from its native format to RGB
                        //img_convert(pFrameRGB.as(AVPicture.class), PixelFormat.PIX_FMT_RGB24, pFrame.as(AVPicture.class), codecCtx.pix_fmt(),
                        //    codecCtx.width(), codecCtx.height());
                        // Save the frame to disk
                        //if(++i<=5)
                        //  SaveFrame(pFrameRGB, pCodecCtx->width,
                        //            pCodecCtx->height, i);
                    }
                }

                // Free the packet that was allocated by av_read_frame
                av_free_packet(pPacket);
            }
        }

        // Free the RGB image
        //av_free(buffer);
        //av_free(pFrameRGB);

        // Free the YUV frame
        //av_free(pFrame);

        // Close the codec
        avcodec_close(pCodecCtx);

        // Close the video file
        av_close_input_file(pFormatCtx);

    }

    static void error(String msg) {
        throw new Error(msg);
    }
}
