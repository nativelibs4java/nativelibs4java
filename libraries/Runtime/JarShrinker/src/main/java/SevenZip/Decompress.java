package SevenZip;
import java.io.*;
public class Decompress
{	
	public static void main(String[] args) throws Exception
	{
		java.io.BufferedInputStream inStream  = new java.io.BufferedInputStream(args.length == 0 ? System.in : new FileInputStream(args[0]));
		java.io.BufferedOutputStream outStream = new java.io.BufferedOutputStream(args.length == 0 ? System.out : new FileOutputStream(args[1]));
		
		decompress(inStream, outStream);
		
		outStream.flush();
		outStream.close();
		inStream.close();
	}
	public static void decompress(InputStream inStream, OutputStream outStream) throws IOException
	{
		int propertiesSize = 5;
		byte[] properties = new byte[propertiesSize];
		if (inStream.read(properties, 0, propertiesSize) != propertiesSize)
			throw new IOException("input .lzma file is too short");
		SevenZip.Compression.LZMA.Decoder decoder = new SevenZip.Compression.LZMA.Decoder();
		if (!decoder.SetDecoderProperties(properties))
			throw new IOException("Incorrect stream properties");
		long outSize = 0;
		for (int i = 0; i < 8; i++)
		{
			int v = inStream.read();
			if (v < 0)
				throw new IOException("Can't read stream size");
			outSize |= ((long)v) << (8 * i);
		}
		if (!decoder.Code(inStream, outStream, outSize))
			throw new IOException("Error in data stream");
	}
}
