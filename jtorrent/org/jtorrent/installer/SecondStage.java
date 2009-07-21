
package org.jtorrent.installer;

import java.io.*;
import java.util.jar.*;
import java.util.StringTokenizer;

/**
 * This is the Second Stage installer.  It extracts the build.xml file from
 * the JAR and calls ANT to handle the rest of the installation process.
 */
public class SecondStage {

    /** starts the second stage of the installer */
    public SecondStage(File installDir, File jvmLoc) throws IOException {

	// extract the build.xml file
	ClassLoader cl = getClass().getClassLoader();
	InputStream stream = cl.getResourceAsStream("build.xml");
	File buildFile = new File(installDir, "build.xml");
	FileOutputStream fos = new FileOutputStream(buildFile);

	byte buf[] = new byte[1024];
	int read = -1;

	while ((read = stream.read(buf)) > 0) 
	    fos.write(buf, 0, read);

	fos.flush();
	fos.close();
	stream.close();

	// get the installer.jar file from classpath so it can be passed
	// to ant's installjar property
	File installJar = null;
	String classpath = System.getProperty("java.class.path");
	StringTokenizer st = 
	    new StringTokenizer(classpath, File.pathSeparator);

	while (st.hasMoreElements() && installJar == null) {

	    String path = (String)st.nextElement();
	    if (path.endsWith("installer-0.1.jar"))
		installJar = new File(path);
	}

/*
	// extract the ANT jar file
FileInputStream fis = new FileInputStream(installJar);
JarInputStream jis = new JarInputStream(fis);

JarEntry entry = jis.getNextJarEntry();
boolean found = false;

while (!found && (entry != null)) {
System.err.println(":"+entry.getCompressedSize()+":");
	if (entry.getName().equals("tools/tools.jar")) {
		found = true;
	} else entry = jis.getNextJarEntry();
}

if (entry != null) {
	long size = entry.getSize();
System.err.println(":"+entry.toString()+":");
System.err.println(size);
	byte arr[] = new byte[(int)size];

	if (jis.read(arr, 0, (int)size) == size) {
		fos = new FileOutputStream("tools/tools.jar");
		fos.write(arr); fos.flush(); fos.close();
	}
}
fis.close();
*/

	// and start ANT
	String args[] = new String[6];
	args[0] = "-buildfile";
	args[1] = buildFile.getAbsolutePath();
	args[2] = "-Dinstalldir=" + installDir.getAbsolutePath();
	args[3] = "-Djvmdir=" + jvmLoc.getAbsolutePath();
	args[4] = "-Dinstalljar=" + installJar.getAbsolutePath();
	args[5] = "installall";

	try {

	    org.apache.tools.ant.Main.main(args);

	} catch (Throwable t) { t.printStackTrace(); }

	System.exit(0);
    }

    /** entry point */
    public static void main(String args[]) {

	try {

	    if (args.length == 2) {

		File installDir = new File(args[0]);
		File jvmDir = new File(args[1]);

		if ((installDir.isDirectory()) && (jvmDir.isDirectory()))
		    new SecondStage(installDir, jvmDir);
		else System.err.println("bad args to Second Stage installer");
	    }

	} catch (Exception ioe) { ioe.printStackTrace(); }
    }
}
