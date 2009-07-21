
package org.jtorrent.installer;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.ice.jni.registry.Registry;
import com.ice.jni.registry.RegistryException;
import com.ice.jni.registry.NoSuchKeyException;
import com.ice.jni.registry.RegistryKey;
import com.ice.jni.registry.RegStringValue;

/**
 * This is a custom ANT task to add a key and a string value to the windows 
 * registry using the JNIRegistry code from 
 * http://www.trustice.com/java/jnireg/
 * If this task is executed upon a non-Windows platform, it does nothing.
 *
 * @author Hunter Payne
 */
public class WinSetRegTask extends Task {

    private String topLevelKey;
    private String key;
    private String valueName;
    private String value;
    private String libloc = null;
    private static boolean isWindows = false;

    static {

	String os = System.getProperty("os.name");

	if ((os != null) && (os.indexOf("Windows") != -1)) {

	    isWindows = true;
	}
    }

    /** adds a registry key and a string value to the Windows registry */
    public void execute() throws BuildException {

	if (isWindows) {

	    if (libloc != null) 
		System.load((new File(libloc, 
				      "ICE_JNIRegistry.dll")).getAbsolutePath());

	    RegistryKey topKey = Registry.getTopLevelKey(topLevelKey);

	    if (topKey == null) 
		throw new BuildException("no top level key " + topLevelKey);
	    
	    try {

		RegistryKey localKey = 
		    topKey.createSubKey(key, value, RegistryKey.ACCESS_WRITE);
		if (valueName == null) valueName = "";
		
		if (localKey == null)
		    throw new BuildException("no key " + key + " in " + 
					     topLevelKey);

		RegStringValue val = 
		    new RegStringValue(localKey, valueName, value);
		localKey.setValue(val);
		localKey.flushKey();
		System.out.println("made key " + key);
		System.out.println("valueName " + valueName);
		System.out.println("value " + value);

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

    /** sets the key to which to add */
    public void setKey(String key) { this.key = key; }

    /** sets the name of the value to store */
    public void setValuename(String valueName) { this.valueName = valueName; }

    /** sets the actual value to store */
    public void setValue(String value) { this.value = value; }

    /** 
     * sets the location of the Bittorrent application(optional, but 
     * must be set by the first ANT tag which calls this task or any of the
     * other Win*RegTasks)
     */
    public void setLibloc(String libloc) { this.libloc = libloc; }
}
