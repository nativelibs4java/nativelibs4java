import java.net.*;
import static SevenZip.Decompress.*;
import java.io.*;
import java.util.zip.*;
import java.util.*;
public class JarShrinkerLoader {
	static final String propsResName = "META-INF/jarshrinker.properties";
	public static void main(String[] args) {
		try {
			File jarFile = File.createTempFile("classes", ".jar");
			jarFile.deleteOnExit();
			
			System.out.println("Temp classes jar : " + jarFile.getCanonicalFile());
			
			InputStream in = new BufferedInputStream(JarShrinkerLoader.class.getClassLoader().getResourceAsStream("classes.7z"));
			OutputStream out = new BufferedOutputStream(new FileOutputStream(jarFile));
			decompress(in, out);
			out.close();
			in.close();
			
			String mainClassName = null;
			
			ZipFile zf = new ZipFile(jarFile, ZipFile.OPEN_READ);
			ZipEntry ze = zf.getEntry("META-INF/Manifest.mf");
			if (ze != null) {
				BufferedReader rin = new BufferedReader(new InputStreamReader(zf.getInputStream(ze)));
				String line;
				while ((line = rin.readLine()) != null) {
					line = line.trim();
					if (line.startsWith("Main-Class: ")) {
						mainClassName = line.substring("Main-Class: ".length()).trim();
						break;
					}
				}
				rin.close();
			}
			zf.close();
			
			if (mainClassName == null)
				mainClassName = args[0];
			
			URLClassLoader loader = new URLClassLoader(new URL[] { jarFile.toURI().toURL() });
			loader.loadClass(mainClassName).getMethod("main", String[].class).invoke(args);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

}
