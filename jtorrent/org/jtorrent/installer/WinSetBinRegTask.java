
package org.jtorrent.installer;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.ice.jni.registry.Registry;
import com.ice.jni.registry.RegistryException;
import com.ice.jni.registry.NoSuchKeyException;
import com.ice.jni.registry.RegistryKey;
import com.ice.jni.registry.RegBinaryValue;

/**
 * This is a custom ANT task to add a key and a binary value to the windows 
 * registry using the JNIRegistry code from 
 * http://www.trustice.com/java/jnireg/
 * If this task is executed upon a non-Windows platform, it does nothing.
 *
 * @author Hunter Payne
 */
public class WinSetBinRegTask extends Task {

    private String topLevelKey;
    private String key;
    private String valueName;
    private String value;
    private static boolean isWindows = false;

    static {

	String os = System.getProperty("os.name");

	if ((os != null) && (os.indexOf("Windows") != -1)) {

	    isWindows = true;
	}
    }

    /** adds a registry key and a binary value to the Windows registry */
    public void execute() throws BuildException {

	if (isWindows) {

	    RegistryKey topKey = Registry.getTopLevelKey(topLevelKey);

	    if (topKey == null) 
		throw new BuildException("no top level key " + topLevelKey);
	    
	    try {

		if (valueName == null) valueName = "";
		RegistryKey localKey = 
		    topKey.createSubKey(key, valueName, 
					RegistryKey.ACCESS_WRITE);
		
		if (localKey == null)
		    throw new BuildException("no key " + key + " in " + 
					     topLevelKey);

		// hack, hack, hack!!!
		byte data[] = new byte[4];
		data[0] = (byte)Integer.parseInt(value.substring(0, 2), 16);
		data[1] = (byte)Integer.parseInt(value.substring(2, 4), 16);
		data[2] = (byte)Integer.parseInt(value.substring(4, 6), 16);
		data[3] = (byte)Integer.parseInt(value.substring(6, 8), 16);

		RegBinaryValue val = 
		    new RegBinaryValue(localKey, valueName, data);
		localKey.setValue(val);
		localKey.flushKey();
		System.out.println("write binary key " + key);
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

	    } catch (NumberFormatException nfe) {

		nfe.printStackTrace();
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

    /** sets the string rep in Hex of the value to store, (ie AB000111) */
    public void setValue(String value) { this.value = value; }
}
