
package org.jtorrent.installer;

import java.util.Vector;
import java.util.Hashtable;
import java.io.FilenameFilter;
import java.io.File;

/**
 * This class searches the filesystem for JVMs which can run our application.
 *
 * @author Hunter Payne
 */
public class JVMLocator extends Object implements FilenameFilter {

    private Vector prospects;
    private Hashtable jvmDirs; // to prevent search a JVM's directory

    /**
     * Searches the filesystem for JVMs
     *
     * @param vmTypeFilters -- list of JVM filters 
     * @param rootFile -- from where to start searching
     */
    public JVMLocator(Vector vmTypeFilters, File rootFile) {

	jvmDirs = new Hashtable();
	prospects = new Vector();
	recurse(rootFile, vmTypeFilters);
    }

    /**
     * Searches the filesystem for JVMs
     *
     * @param vmTypeFilters -- list of JVM filters 
     * @param roots -- list of places from which to start searching
     */
    public JVMLocator(Vector vmTypeFilters, Vector roots) {

	prospects = new Vector();

	for (int i = 0; i < roots.size(); i++) {

	    File rootFile = (File)roots.get(i);
	    recurse(rootFile, vmTypeFilters);
	}
    }

    /**
     * returns a list of JVMs which were found on this filesystem
     * @return a Vector of org.jtorrent.installer.JVM objects
     */
    public Vector getProspects() {

	return prospects;
    }

    /**
     * checks if the specified file is a directory to recursively search upon
     */
    public boolean accept(File dir, String name) {

	if (!dir.getAbsolutePath().startsWith("/proc")) {

	    File check = new File(dir, name);
	    return check.isDirectory();
	}

	return false;
    }

    // recurse down the filesystem and file all the acceptable JVMs
    private void recurse(File curr, Vector vmTypeFilters) {

	// find the JVMs in this directory
	for (int i = 0; i < vmTypeFilters.size(); i++) {

	    IJVMFilter filter = (IJVMFilter)vmTypeFilters.get(i);
	    String list[] = curr.list(filter);

	    if (list != null) {

		for (int j = 0; j < list.length; j++) {

		    File checker = new File(curr, list[j]);
		    jvmDirs.put(checker, checker);
		    prospects.add(new JVM(filter.getName(), checker, 
					  filter.getCommand()));
		}
	    }
	}

	// make recursive calls upon the directories
	String list[] = curr.list(this);

	if (list != null) {

	    for (int j = 0; j < list.length; j++) {
		
		File dir = new File(curr, list[j]);
		if (!jvmDirs.containsKey(dir)) recurse(dir, vmTypeFilters);
	    }
	}
    }

    /*
    public static void main(String args[]) {

	File root = new File(args[0]);
	Vector filters = new Vector();
	filters.add(new SunJVMFilter());
	JVMLocator locator = new JVMLocator(filters, root);
	System.out.println("found " + locator.getProspects());
	}*/
}
