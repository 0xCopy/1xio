
package org.jtorrent.installer;

import java.io.File;
import java.io.InputStream;

/**
 * This filter implementation can find and identify the version of Sun JDKs
 * and JRE's(I hope).
 *
 * @author Hunter Payne
 */
public class SunJVMFilter implements IJVMFilter {

    protected String minVersion = "1.4";
    protected String command = null;
    protected String name = null;
    private static boolean isWindows = false;

    static {

	String os = System.getProperty("os.name");

	if ((os != null) && (os.indexOf("Windows") != -1)) {

	    isWindows = true;
	}
    }

    // finds the version of this JVM by parsing the output from the JVM
    // when we run the it with the -version flag
    private static String parseVersion(String output) {

	System.out.println("getting version from " + output);
	int dotCheckIndex = output.indexOf(".");

	while (dotCheckIndex != -1) {

	    if ((dotCheckIndex > 0) && 
		(Character.isDigit(output.charAt(dotCheckIndex - 1))) &&
		(dotCheckIndex < output.length() - 1) &&
		(Character.isDigit(output.charAt(dotCheckIndex + 1)))) {

		// we found a place where we have two digits around a .
		// ie 1.4
		int endOffset = 2;

		// unnecessary while loop to extend where the version number 
		// ends the loop will usually run only once, at most
		while ((dotCheckIndex + endOffset < output.length() - 1) &&
		       (output.charAt(dotCheckIndex + endOffset) == '.') &&
		       (Character.isDigit(output.charAt(dotCheckIndex + 
							endOffset + 1))))
		    endOffset += 2;

		return output.substring(dotCheckIndex - 1, 
					dotCheckIndex + endOffset);
	    }

	    dotCheckIndex = output.indexOf(".", dotCheckIndex + 1);
	}

	return null;
    }

    // run the JVM with the -version flag and capture its output
    private static String getVersionOutput(String executable) {

	try {

	    Runtime runtime = Runtime.getRuntime();
	    Process proc = runtime.exec(executable + " -version");
	    InputStream is = proc.getErrorStream();
	    proc.waitFor();
	    StringBuffer buffer = new StringBuffer();
	    
	    byte buf[] = new byte[1024];
	    int read = -1;
	    
	    while ((read = is.read(buf)) > 0) 
		buffer.append(new String(buf, 0, read));

	    return buffer.toString();
	
	} catch (Throwable e) {

	    e.printStackTrace();
	}

	return null;
    }

    /** accessor for the command to run this JVM */
    public String getCommand() {

	return command;
    }

    /** accessor for the user suitable name of this JVM */
    public String getName() {

	return name;
    }

    /** 
     * returns true when called with the installation directory of a Sun JVM
     * or JRE 
     */
    public boolean accept(File dir, String name) {

	if ((name.startsWith("j")) || (name.startsWith("J"))) {
	    
	    File javaHome = new File(dir, name);

	    // TODO handle MS case
	    File executable = new File(javaHome, "bin/java"); 
	    if (isWindows) executable = new File(javaHome, "bin/java.exe"); 

	    if (executable.exists()) {

		String versionOutput = 
		    getVersionOutput(executable.getAbsolutePath());

		// find the version number
		String version = parseVersion(versionOutput);
		System.out.println("parsed version " + version);

		if ((minVersion == null) ||
		    ((version != null) && 
		     (version.compareTo(minVersion) >= 0))) {

		    // TODO change this so something which doesn't launch
		    // that silly terminal window
		    if (isWindows) executable = new File(javaHome, "bin/java");
		    command = executable.getAbsolutePath();
		    this.name = "Sun JRE " + version + " @ " + 
			javaHome.getAbsolutePath();

		    return true;
		}
	    }
	}

	return false;
    }

/*
    public static void main(String args[]) {

	File testFile = new File(args[0]);
	SunJVMFilter filter = new SunJVMFilter();

	if (filter.accept(testFile.getParentFile(), testFile.getName())) {

	    System.out.println("matched..." + filter.getName());
	    System.out.println("matched..." + filter.getCommand());

	} else System.out.println("not matched...");
	}
*/
}
