
package org.jtorrent.installer;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.InputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Vector;
import java.util.Properties;
import java.util.Enumeration;

/**
 * This is the first stage installer.  It is designed to be as backwards
 * compatable as possible.  It should work under all JVMs from 1.1.* to 1.4.1
 * and beyond.  It uses the lowest possible common denominator for all 
 * functionality.  This object is the main entry point.  It loads a list of
 * JVM filters from a configuration file and searches the filesystem for
 * any JVMs which meet the needs of the application this class is installing.
 * If necessary, it installs its own JVM.
 *
 * @author Hunter Payne
 */
public class Installer extends Object implements FilenameFilter {

    /**
     * Starts the installation process.  Installs to the specified directory.
     *
     * @param installDir -- where to install this application
     */
    public Installer(File installDir) {

	try {

	    // ask the user if the install dir isn't specified on the command
	    // line
	    if (installDir == null) installDir = getInstallDir();

	    if (installDir == null) {

		System.err.println("no install directory selected");
		System.exit(0);
	    }

	    if ((!installDir.exists()) && (!installDir.mkdirs()))
		throw new RuntimeException("unable to create " + 
					   installDir.getAbsolutePath());

	    // load the install properties from the JAR
	    ClassLoader cl = getClass().getClassLoader();
	    InputStream stream = cl.getResourceAsStream("install.props");
	    Properties config = new Properties();
	    config.load(stream);

	    // load the JVM filters
	    Vector vmTypeFilters = loadFilters(config);
	    File currJVMHome = new File(System.getProperty("java.home"));

	    // find the JVMs
	    JVMLocator locator = 
		new JVMLocator(vmTypeFilters, 
			       new File(currJVMHome.getParent()));
	    Vector prospects = locator.getProspects();

	    if (prospects.size() == 0) {

		File curr = installDir;

		while (curr.getParent() != null) 
		    curr = new File(curr.getParent());

		locator = new JVMLocator(vmTypeFilters, curr);
		prospects = locator.getProspects();
	    }

	    JVM realJVM = null;

	    // we don't have a JVM, so install one
	    if (prospects.size() == 0) {

		realJVM = installJVM(installDir, config);
		
		// we let the user choose the JVM
	    } else if (prospects.size() > 1) {

		VMChooser chooser = new VMChooser(prospects);

		// hacky code for AWT frame
		while (!chooser.isFinished())
		    try { Thread.sleep(1000); } catch (Throwable t) {}

		realJVM = chooser.getSelected();

		// just use the one we found
	    } else {

		realJVM = (JVM)prospects.get(0);
	    }

	    // start the second stage of the install
	    if (realJVM != null) install(installDir, realJVM);
	    else System.err.println("unable to install JVM...");

	} catch (Exception e) {

	    e.printStackTrace();
	    System.exit(1);
	}
    }

    /** get the command to install the JVM */
    protected String getJVMInstallCommand(Properties config) {

	// TODO get the install command on a per-OS basis
	return (String)config.get("install.command");
    }

    /** install the JVM in the specified directory, currently doesn't work */
    protected JVM installJVM(File installDir, Properties config) 
	throws Exception {

	// We could use the silent install mode of the Sun JRE installer to
	// install a JRE for the user which is the intent of this method.
	// However, it would make the installer very, very bloated and 
	// I am running out of disk space.  When I get enough money to buy
	// a new machine, I'll fix this method.  For more information about
	// the silent installer, see
	// http://java.sun.com/j2se/1.4.1/docs/guide/plugin/developer_guide/silent.html
	// for now just tell the user to install a newer JVM and exit
	javax.swing.JOptionPane.showMessageDialog(null, "A JRE with the correct version wasn't located.\nPlease install a JRE with version 1.4 or later.");
	System.exit(1);

	// 

	/*	String jvmInstallCommand = getJVMInstallCommand(config);

	if (!installDir.exists() && !installDir.createNewFile())
	return null;*/

	// TODO unzip the install command's file from the top level of the
	// zip archive, or run the silent installer

	/*	System.setProperty("user.dir", installDir.getAbsolutePath());
	Runtime runtime = Runtime.getRuntime();
	Process proc = runtime.exec(jvmInstallCommand);
	
	if (proc.waitFor() == 0) {

	    File jvmDir = new File(installDir, "jre");
	    File commandFile = new File(jvmDir, "bin/java");
	    return new JVM(jvmDir.getAbsolutePath(), jvmDir, 
			   commandFile.getAbsolutePath());
	}
	*/
	return null; 
    }

    /** builds and returns a list of JVM filters */
    protected Vector loadFilters(Properties config) {

	Enumeration e = config.keys();
	Vector ret = new Vector();
	
	while (e.hasMoreElements()) {

	    String key = (String)e.nextElement();
	    
	    if (key.startsWith("filter")) {

		try {

		    Class clas = Class.forName((String)config.get(key));
		    IJVMFilter filter = (IJVMFilter)clas.newInstance();
		    ret.add(filter);

		} catch (Exception ex) {

		    ex.printStackTrace();
		}
	    }
	}

	return ret;
    }

    /**
     * starts the second stage of the install
     */
    protected void install(File installDir, JVM jvm)
	throws java.io.IOException {

	// pass the install dir and JVM location to the second stage
	Runtime runtime = Runtime.getRuntime();
	String command = jvm.getCommand() + " -classpath \"" +
	    System.getProperty("java.class.path") +
	    "\" org.jtorrent.installer.SecondStage \"" +
	    installDir.getAbsolutePath() + "\" \"" + 
	    jvm.getLocation().getAbsolutePath() + "\"";
	System.out.println("running second stage " + command);
	Process proc = runtime.exec(command);
	boolean finished = false;

	try {

	    // while the process is still running
	    while (!finished) {

	    try {

		// the proc.waitFor() method doesn't seem to work, but this
		// does
		proc.exitValue();
		finished = true;

	    } catch (IllegalThreadStateException itse) {}

	    // send the second stage's output to our console
	    InputStream os = proc.getInputStream();

	    byte buf[] = new byte[1024];
	    int read = -1;

	    while ((read = os.read(buf)) > 0)
		System.out.print(new String(buf, 0, read));

	    os = proc.getErrorStream();

	    while ((read = os.read(buf)) > 0)
		System.err.print(new String(buf, 0, read));
	    }
	    System.exit(0);

	} catch (Exception e) { e.printStackTrace(); }
	
	System.exit(1);
    }

    /** only allows new files and existing directories */
    public boolean accept(File dir, String name) {
	
	File test = new File(dir, name);
	return (!test.exists() || test.isDirectory());
    }

    /** 
     * lets the user choose which directory to install this application
     * <br />
     * Note: this relies upon the the swing JFileChooser, but if the
     * swing classes can't be found(ie in pre Java 2 JREs) it catched the
     * exception and tries to use the FileDialog component from AWT.
     * This means that this code won't compile under pre Java2 JREs, but
     * should run on it, with a very very ugle file choosing dialog.
     */
    public File getInstallDir() {
	
	try {

	    // I think not importing the swing packages above should delay
	    // the loading the the javax.* classes until this point in the code
	    // I'm not 100% sure about that though.  I might have to 
	    // use reflection to call this code.  It needs to be tested
	    // under a 1.1.* JRE
	    javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
	    chooser.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);
	    int selected = chooser.showSaveDialog(null);

	    if (selected == javax.swing.JFileChooser.APPROVE_OPTION) 
		return chooser.getSelectedFile();

	} catch (Throwable cnfe) {

	    // we don't have the javax.swing.* classes so use the crappy old
	    // FileDialog from AWT, yuk.  This stuff doesn't really work very
	    // well
	    Frame temp = new Frame();
	    FileDialog dialog = 
		new FileDialog(temp, "Choose where to install Bit Torrent", 
			       FileDialog.SAVE);
	    dialog.setFilenameFilter(this);
	    dialog.setVisible(true);
	    String selected = dialog.getFile();
	    if (selected != null) return new File(selected);
	    selected = dialog.getDirectory();
	    System.out.println("selected " + selected);
	    if (selected != null) return new File(selected);
	}

	return null;
    }

    /** starts the installer */
    public static void main(String args[]) {

	if (args.length > 0)
	    new Installer(new File(args[0]));
	else new Installer(null);
    }
}
