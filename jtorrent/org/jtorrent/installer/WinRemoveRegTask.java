
package org.jtorrent.installer;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.ice.jni.registry.Registry;
import com.ice.jni.registry.RegistryException;
import com.ice.jni.registry.NoSuchKeyException;
import com.ice.jni.registry.RegistryKey;
import com.ice.jni.registry.RegBinaryValue;

/**
 * This is a custom ANT task to remove a key from the windows registry using
 * the JNIRegistry code from http://www.trustice.com/java/jnireg/
 * If this task is executed upon a non-Windows platform, it does nothing.
 *
 * @author Hunter Payne
 */
public class WinRemoveRegTask extends Task {

    private String topLevelKey;
    private String key;
    private String libloc = null;
    private static boolean isWindows = false;

    static {

	String os = System.getProperty("os.name");

	if ((os != null) && (os.indexOf("Windows") != -1)) {

	    isWindows = true;
	}
    }

    /** removes the registry key from the windows registry */
    public void execute() throws BuildException {

	if (isWindows) {

	    if (libloc != null) 
		System.load((new File(libloc, 
				      "ICE_JNIRegistry.dll")).getAbsolutePath());

	    RegistryKey topKey = Registry.getTopLevelKey(topLevelKey);

	    if (topKey == null) 
		throw new BuildException("no top level key " + topLevelKey);
	    
	    try {

		topKey.deleteSubKey(key);

	    } catch (NoSuchKeyException ex) {

		ex.printStackTrace();
		throw new BuildException("no key " + key + " in " + 
					 topLevelKey);

	    } catch (RegistryException re) {

		re.printStackTrace();
		throw new BuildException("no key " + key + " in " + 
					 topLevelKey);
	    }
	}
    }

    /** sets the top level key, HKLM, HKCR, etc. */
    public void setToplevelkey(String key) {

	topLevelKey = key;
    }

    /** sets the key to remove */
    public void setKey(String key) { this.key = key; }

    /** 
     * sets the location of the Bittorrent application(optional, but 
     * must be set by the first ANT tag which calls this task or any of the
     * other Win*RegTasks)
     */
    public void setLibloc(String libloc) { this.libloc = libloc; }
}
