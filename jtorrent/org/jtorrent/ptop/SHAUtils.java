
package org.jtorrent.ptop;

import java.security.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.jtorrent.msgdata.Utils;

/**
 * This class contains utilities for computing the SHA-1 hash of a string 
 * encoded in ASCII and a byte array.
 *
 * @author Hunter Payne
 * @version 0.1
 */
public class SHAUtils {

    /**
     * The method computes the byte byte long SHA-1 hash of the provided 
     * ASCII string.
     *
     * @param message -- message from which to compute the digest
     * @return the byte array 20 bytes long which contains the SHA-1 digest
     * of the message arguement
     */
    public static byte[] computeStringDigest(String message) {

	// compute the string to an ASCII byte array
	return computeDigest(Utils.getBytes(message));
    }

    /**
     * This method computes the SHA-1 hash of the provided byte array
     *
     * @param message -- message from which to compute the digest
     * @return the byte array 20 bytes long which contains the SHA-1 digest
     * of the message arguement
     */
    public static byte[] computeDigest(byte[] message) {

	try {

	    // create the digest object
	    MessageDigest digest = MessageDigest.getInstance("SHA-1");

	    // compute the digest and place it in the byte buffer
	    ByteBuffer buf = ByteBuffer.wrap(digest.digest(message));

	    // make sure the byte buffer is encoded in big endian
	    buf = buf.order(ByteOrder.BIG_ENDIAN);

	    // return the hash
	    return buf.array();

	} catch (NoSuchAlgorithmException e) {

	    e.printStackTrace();
	}

	return null;
    }
}
