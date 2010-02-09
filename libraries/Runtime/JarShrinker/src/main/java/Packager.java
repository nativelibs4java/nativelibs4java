import SevenZip.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

public class Packager
{
	public static void main(String[] args) throws Exception
	{
		ClassLoader cl = Packager.class.getClassLoader();
		BufferedReader rin = new BufferedReader(new InputStreamReader(cl.getResourceAsStream("runtime.list")));
		String line;
		
		Set<String> runtimeRes = new HashSet<String>();
		while ((line = rin.readLine()) != null)
			runtimeRes.add(line);
		
		rin.close();
		
		InputStream inStream;
		byte[] b = new byte[1024];
		int len;
				
		if (args.length > 0) {
			String inFile = args[0], inFileLo = inFile.toLowerCase();
			if (inFileLo.endsWith(".jar") || inFileLo.endsWith(".zip")) {
				File file = new File(inFile);
				ByteArrayOutputStream bout = new ByteArrayOutputStream((int)(file.length() * 4));
				ZipInputStream zin = new ZipInputStream(new FileInputStream(file));
				ZipOutputStream zout = new ZipOutputStream(bout);
				ZipEntry e;
				while ((e = zin.getNextEntry()) != null) {
					ZipEntry ee = new ZipEntry(e.getName());
					ee.setMethod(ZipEntry.STORED);
					ByteArrayOutputStream eout = new ByteArrayOutputStream();
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
			
		ZipOutputStream zout = new ZipOutputStream(args.length == 0 ? System.out : new FileOutputStream(args[1]));
		zout.putNextEntry(new ZipEntry("META-INF/Manifest.mf"));
		PrintStream pout = new PrintStream(zout);
		pout.println("Main-Class: " + JarShrinkerLoader.class.getName());
		pout.flush();
		zout.closeEntry();
		
		for (String res : runtimeRes) {
			zout.putNextEntry(new ZipEntry(res));
			InputStream resIn = cl.getResourceAsStream(res);
			while ((len = resIn.read(b)) > 0)
				zout.write(b, 0, len);
			resIn.close();
			zout.closeEntry();
		}
		
		zout.putNextEntry(new ZipEntry("classes.7z"));
			
		//BufferedOutputStream outStream = new BufferedOutputStream(args.length == 0 ? System.out : new FileOutputStream(args[1]));
		
		compress(inStream, zout);//outStream);
		
		zout.closeEntry();
		
		zout.close();
		
		//outStream.flush();
		//outStream.close();
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
