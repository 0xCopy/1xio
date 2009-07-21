
package org.jtorrent.msgdata;

import java.io.ByteArrayOutputStream;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.BitSet;
import java.security.AccessController;
import java.security.PrivilegedAction;
import sun.security.action.GetBooleanAction;
import sun.security.action.GetPropertyAction;

/**
 * This class was adapted from the java.net.URLEncoder class in the 1.4.1 JDK
 * It is modified to handle encoding of ASCII characters so the Java code
 * can interact with Python code over raw sockets.  (I guess Python doesn't
 * use Unicode)
 *
 * @author  Hunter Payne
 */
public class URLEncoder {

    static BitSet dontNeedEncoding;
    static final int caseDiff = ('a' - 'A');
    static String dfltEncName = null;

    static {

	/* The list of characters that are not encoded has been
	 * determined as follows:
	 *
	 * RFC 2396 states:
	 * -----
	 * Data characters that are allowed in a URI but do not have a
	 * reserved purpose are called unreserved.  These include upper
	 * and lower case letters, decimal digits, and a limited set of
	 * punctuation marks and symbols. 
	 *
	 * unreserved  = alphanum | mark
	 *
	 * mark        = "-" | "_" | "." | "!" | "~" | "*" | "'" | "(" | ")"
	 *
	 * Unreserved characters can be escaped without changing the
	 * semantics of the URI, but this should not be done unless the
	 * URI is being used in a context that does not allow the
	 * unescaped character to appear.
	 * -----
	 *
	 * It appears that both Netscape and Internet Explorer escape
	 * all special characters from this list with the exception
	 * of "-", "_", ".", "*". While it is not clear why they are
	 * escaping the other characters, perhaps it is safest to
	 * assume that there might be contexts in which the others
	 * are unsafe if not escaped. Therefore, we will use the same
	 * list. It is also noteworthy that this is consistent with
	 * O'Reilly's "HTML: The Definitive Guide" (page 164).
	 *
	 * As a last note, Intenet Explorer does not encode the "@"
	 * character which is clearly not unreserved according to the
	 * RFC. We are being consistent with the RFC in this matter,
	 * as is Netscape.
	 *
	 */

	dontNeedEncoding = new BitSet(256);
	int i;
	for (i = 'a'; i <= 'z'; i++) {
	    dontNeedEncoding.set(i);
	}
	for (i = 'A'; i <= 'Z'; i++) {
	    dontNeedEncoding.set(i);
	}
	for (i = '0'; i <= '9'; i++) {
	    dontNeedEncoding.set(i);
	}
	dontNeedEncoding.set(' '); /* encoding a space to a + is done
				    * in the encode() method */
	dontNeedEncoding.set('-');
	dontNeedEncoding.set('_');
	dontNeedEncoding.set('.');
	dontNeedEncoding.set('*');

    	dfltEncName = (String)AccessController.doPrivileged (
	    new GetPropertyAction("file.encoding")
    	);
    }

    /**
     * You can't call the constructor.
     */
    private URLEncoder() { }

    private static byte convertToDigit(byte b) {

	if (b == 0) return 48;
	else if (b == 1) return 49;
	else if (b == 2) return 50;
	else if (b == 3) return 51;
	else if (b == 4) return 52;
	else if (b == 5) return 53;
	else if (b == 6) return 54;
	else if (b == 7) return 55;
	else if (b == 8) return 56;
	else if (b == 9) return 57;
	else if (b == 10) return 65;
	else if (b == 11) return 66;
	else if (b == 12) return 67;
	else if (b == 13) return 68;
	else if (b == 14) return 69;
	return 70;
    }

    /**
     * converts bytes which represent ASCII characters into a byte array
     * representing the specified encoding
     */
    public static byte[] encode(byte[] arr, String enc) 
	throws UnsupportedEncodingException {

	boolean needToChange = false;
	ByteArrayOutputStream out = new ByteArrayOutputStream();

	for (int i = 0; i < arr.length; i++) {

	    char c = (char)arr[i];

	    if (dontNeedEncoding.get(c)) {

		if (c == ' ') {

		    c = '+';
		    needToChange = true;
		}
		out.write(c);

	    } else {

		out.write(37);
		int b = (int)arr[i];
		needToChange = true;

		if (b < 0) b += 256;

		byte high = (byte)(b / 16);
		byte low = (byte)(b % 16);
		byte hch = (byte)convertToDigit(high);
		byte lch = (byte)convertToDigit(low);
		out.write(hch);
		out.write(lch);
	    }
	}

	return (needToChange? out.toByteArray() : arr);
    }
}
