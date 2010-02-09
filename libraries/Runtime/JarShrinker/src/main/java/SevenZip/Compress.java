package SevenZip;
import java.io.*;
import java.util.zip.*;
public class Compress
{
	public static void main(String[] args) throws Exception
	{
		InputStream inStream;
		
		if (args.length > 0) {
			String inFile = args[0], inFileLo = inFile.toLowerCase();
			if (inFileLo.endsWith(".jar") || inFileLo.endsWith(".zip")) {
				File file = new File(inFile);
				ByteArrayOutputStream bout = new ByteArrayOutputStream((int)(file.length() * 4));
				ZipInputStream zin = new ZipInputStream(new FileInputStream(file));
				ZipOutputStream zout = new ZipOutputStream(bout);
				ZipEntry e;
				byte[] b = new byte[1024];
				while ((e = zin.getNextEntry()) != null) {
					ZipEntry ee = new ZipEntry(e.getName());
					ee.setMethod(ZipEntry.STORED);
					ByteArrayOutputStream eout = new ByteArrayOutputStream();
					int len;
					while ((len = zin.read(b)) > 0)
						eout.write(b, 0, len);
					eout.close();
					byte[] ebytes = eout.toByteArray();
					          
					//System.out.println("ZipEntry[" + e.getName() + "].size = " +  ebytes.length);
					if (ebytes.length > 0) {
						ee.setSize(ebytes.length);
						ee.setCrc(e.getCrc());
						zout.putNextEntry(ee);
						
						zout.write(ebytes, 0, ebytes.length);
					}
					zout.closeEntry();
				}
				zout.close();
				zin.close();
				
				inStream = new ByteArrayInputStream(bout.toByteArray());
			} else
				inStream = new BufferedInputStream(new FileInputStream(args[0]));
		} else
			inStream = System.in;
		BufferedOutputStream outStream = new BufferedOutputStream(args.length == 0 ? System.out : new FileOutputStream(args[1]));
		
		compress(inStream, outStream);
		
		outStream.flush();
		outStream.close();
		inStream.close();
	}
	public static void compress(InputStream inStream, OutputStream outStream) throws IOException
	{
		SevenZip.Compression.LZMA.Encoder encoder = new SevenZip.Compression.LZMA.Encoder();
		if (!encoder.SetAlgorithm(2))
			throw new IOException("Incorrect compression mode");
		if (!encoder.SetDictionarySize(1 << 23))
			throw new IOException("Incorrect dictionary size");
		if (!encoder.SetNumFastBytes(128))
			throw new IOException("Incorrect -fb value");
		if (!encoder.SetMatchFinder(1))
			throw new IOException("Incorrect -mf value");
		if (!encoder.SetLcLpPb(3, 0, 2))
			throw new IOException("Incorrect -lc or -lp or -pb value");
		encoder.SetEndMarkerMode(true);
		encoder.WriteCoderProperties(outStream);
		long fileSize = -1;
		for (int i = 0; i < 8; i++)
			outStream.write((int)(fileSize >>> (8 * i)) & 0xFF);
		encoder.Code(inStream, outStream, -1, -1, null);
	}
}
