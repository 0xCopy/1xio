
package org.jtorrent.installer;

import java.io.*;
import java.util.StringTokenizer;

/**
 * Removes the Bittorrent application using the uninstall ANT target.
 *
 * @author Hunter Payne
 */
public class Uninstall {

    /** starts the uninstaller */
    public Uninstall() {

	// get the path to the Bittorrent Application from the classpath
	File installPath = null;
	String classpath = System.getProperty("java.class.path");
	StringTokenizer st = 
	    new StringTokenizer(classpath, File.pathSeparator);
	
	while (st.hasMoreElements() && installPath == null) {

	    String path = (String)st.nextElement();
	    if (path.endsWith("classes"))
{
System.err.println("here");
File myFile = (new File(path)).getAbsoluteFile();
System.err.println(myFile);
		installPath = new File(new File(path).getAbsolutePath()).getParentFile();
myFile.getParentFile();
System.err.println(installPath);
}
	}

	// and start ANT
	String args[] = new String[4];
	args[0] = "-buildfile";
	args[1] = (new File(installPath, "build.xml")).getAbsolutePath();
	args[2] = "-Dinstalldir=" + installPath.getAbsolutePath();
	args[3] = "uninstall";

	try {

	    org.apache.tools.ant.Main.main(args);

	} catch (Throwable e) {e.printStackTrace();}
    }

    /** entry point */
    public static void main(String args[]) {

	new Uninstall();
    }
}
