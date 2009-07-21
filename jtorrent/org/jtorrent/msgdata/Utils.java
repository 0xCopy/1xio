
package org.jtorrent.msgdata;

import java.nio.charset.Charset;

/**
 * Utility class for byte encoding
 *
 * @author Hunter Payne
 */
public class Utils {

    /** default encoding, current value is US-ASCII */
    public static final String encoding = "US-ASCII";
    private static final Charset asciiSet = Charset.forName(encoding);

    /**
     * Converts a string to a byte array using a default character encoding
     */
    public static byte[] getBytes(String str) {

	try {
	
	    return str.getBytes("US-ASCII");

	} catch (Exception e) { e.printStackTrace(); }

	return null;
    }
}
