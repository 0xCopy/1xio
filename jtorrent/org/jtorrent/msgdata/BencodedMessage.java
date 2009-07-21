
package org.jtorrent.msgdata;

import java.util.*;
import java.io.*;

/**
 * This class represents a bencoded message used by BitTorrent to configure
 * downloads and communicate with the BitTorrent tracker.
 * 
 * @author Hunter Payne
 */
public class BencodedMessage extends Properties {

    private ArrayList keyOrder;

    /**
     * Creates a new empty bencoded message
     */
    public BencodedMessage() {

	super();
	keyOrder = new ArrayList();
    }

    /**
     * retrieves a integer value from the message
     *
     * @param key -- the name of the integer value to retrieve
     */
    public int getIntValue(String key) {

	return ((Integer)get(key)).intValue();
    }

    /**
     * stores a integer value in the message
     *
     * @param key -- the name of the integer value to store
     * @param value -- the integer value to store
     */
    public void setIntValue(String key, int value) {

	put(key, new Integer(value));
	keyOrder.add(key);
    }

    /**
     * retrieves a string value from the message
     *
     * @param key -- the name of the string value to retrieve
     */
    public String getStringValue(String key) {

	return (String)get(key);
    }

    /**
     * stores a string value in the message
     *
     * @param key -- the name of the string value to store
     * @param value -- the string value to store
     */
    public void setStringValue(String key, String value) {
	
	put(key, value);
	keyOrder.add(key);
    }

    /**
     * stores a byte array as a string value in the message
     *
     * @param key -- the name of the string value to store
     * @param value -- the byte array to store
     */
    public void setStringValue(String key, byte[] value) {
	
	put(key, value);
	keyOrder.add(key);
    }

    /**
     * gets a byte array from the message
     *
     * @param key -- the name of the byte array to retrieve
     */
    public byte[] getStringBytes(String key) {

	return (byte[])get(key);
    }
    
    /**
     * gets a list from the message
     *
     * @param key -- the name of the list to retrieve
     */
    public List getListValue(String key) {

	return (List)get(key);
    }

    /**
     * stores a list in the message
     *
     * @param key -- the name of the list to store
     * @param value -- the list to store
     */
    public void setListValue(String key, List value) {
	
	put(key, value);
	keyOrder.add(key);
    }

    /**
     * gets a dictionary from the message
     *
     * @param key -- the name of the dictionary to retrieve
     */
    public Properties getDictValue(String key) {

	return (BencodedMessage)get(key);
    }

    /**
     * stores a dictionary in the message
     *
     * @param key -- the name of the dictionary to store
     * @param value -- the dictionary to store
     */
    public void setDictValue(String key, Properties value) {

	put(key, value);
	keyOrder.add(key);
    }

    // internal class to store the intermediate state of a message while it
    // it being parsed from an array of bytes
    private static class DecodingStatus {

	private String message;
	private byte[] msgBytes;
	private int curr;

	DecodingStatus(String m, byte[] bytes) {

	    message = m;
	    msgBytes = bytes;
	    curr = 0;
	}

	// peek ahead
	char getNextChar() {

	    return message.charAt(curr);
	}

	// get next character
	char readNextChar() {

	    return message.charAt(curr++);
	}

	String substring(int start, int end) {

	    return message.substring(start, end);
	}

	byte[] subarr(int start, int end) {

	    byte arr[] = new byte[end - start];
	    System.arraycopy(msgBytes, start, arr, 0, (end - start));
	    return arr;
	}

	byte[] nextarr(int length) {

	    byte ret[] = subarr(curr, curr + length);
	    curr += length;
	    return ret;
	}

	int getCurr() {
	    
	    return curr;
	}
	
	void incCurr(int c) {
	    
	    curr += c;
	}
    }

    /**
     * reads a bencoded message object from the specified stream
     *
     * @param stream -- stream which contains a bencoded message
     * @return null if there is an error, the message otherwise
     */
    public static BencodedMessage readFromStream(InputStream stream) {

	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	byte buf[] = new byte[1024];
	int read = 0;

	try {

	    // read the bytes from the stream into a buffer
	    while ((read = stream.read(buf)) > -1) {
		
		baos.write(buf, 0, read);
	    }

	    byte message[] = baos.toByteArray();
	    String msgString = new String(message, Utils.encoding);

	    // read the message from the stream's bytes and a string decoded
	    // representation of the bytes
	    return readFromMessage(msgString, message);

	} catch (IOException ioe) {

	    ioe.printStackTrace();
	}

	return null;
    }

    /**
     * reads a bencoded message object from a byte array
     *
     * @param msgBytes -- byte array containing the message
     * @return null if there is an error, the message otherwise
     */
    public static BencodedMessage readFromMessage(byte[] msgBytes) {
						   
	try {

	    if (msgBytes.length > 0) {

		String msgString = new String(msgBytes, Utils.encoding);
		return readFromMessage(msgString, msgBytes);
	    }
	} catch (Exception e) { e.printStackTrace(); }

	return null;
    }

    // starts the parsing process
    private static BencodedMessage readFromMessage(String message, 
						   byte[] msgBytes) {

	try {

	    DecodingStatus status = new DecodingStatus(message, msgBytes);

	    // make sure we have a message
	    char first = status.getNextChar();
	    if ('d' == first) return readDict(status);

	} catch (Exception e) {

	    e.printStackTrace();
	}

	return null;
    }
    
    // parses a message from the decoded status
    private static BencodedMessage readDict(DecodingStatus status) {

	// read the d
	status.readNextChar();

	BencodedMessage ret = new BencodedMessage();
	char first = status.getNextChar();

	// while we aren't finished
	while (first != 'e') {

	    // read the next key
	    String key = readString(status);

	    // total hack!!!!
	    // because these keys store byte arrays handle them separately
	    if ((!key.equals("pieces")) && (!key.equals("peer id")) &&
		(!key.equals("ip"))) {
		
		first = status.getNextChar();

		try {

		    // read the correct value for this key and store it
		    if ('d' == first) {

			ret.setDictValue(key, readDict(status));
			
		    } else if ('l' == first) {
			
			ret.setListValue(key, readList(status));
			
		    } else if ('i' == first) {
			
			ret.setIntValue(key, readInt(status));
			
		    } else ret.setStringValue(key, readString(status));
		    
		    first = status.getNextChar();
		    
		} catch (NumberFormatException nfe) { 
		    
		    nfe.printStackTrace(); 
		    return null;
		}
	    } else {

		StringBuffer buf = new StringBuffer();
		char next = status.readNextChar();

		// look for the start of this byte array
		while (next != ':') {
		    
		    buf.append(next);
		    next = status.readNextChar();
		}

		try {

		    // get its length
		    int length = Integer.parseInt(buf.toString());
		    byte hashs[] = status.nextarr(length);

		    // and store the byte array
		    ret.setStringValue(key, hashs);
		    first = status.getNextChar();

		} catch (NumberFormatException nfe) {

		    nfe.printStackTrace();
		    return null;
		}
	    }
	}

	// read the e
	status.readNextChar();
	return ret;
    }

    private static int readInt(DecodingStatus status) {

	// read the i
	status.readNextChar();

	StringBuffer buf = new StringBuffer();
	char next = status.readNextChar();

	// loop until we find the end of this integer
	while (next != 'e') {

	    buf.append(next);
	    next = status.readNextChar();
	}

	// the e has already been read
	try {

	    return Integer.parseInt(buf.toString());

	} catch (NumberFormatException nfe) {

	    nfe.printStackTrace();
	    return -1;
	}
    }

    private static List readList(DecodingStatus status) {

	// read the l
	status.readNextChar();

	ArrayList ret = new ArrayList();
	char next = status.getNextChar();

	// while this list has more elements
	while ('e' != next) {

	    if ('d' == next) {

		ret.add(readDict(status));

	    } else if ('i' == next) {

		ret.add(new Integer(readInt(status)));

	    } else if ('l' == next) {

		ret.add(readList(status));

	    } else {

		ret.add(readString(status));
	    }

	    next = status.getNextChar();
	} 

	// read the e
	status.readNextChar();

	return ret;
    }

    // read a string from the message's bytes
    private static String readString(DecodingStatus status) {

	StringBuffer buf = new StringBuffer();
	char next = status.readNextChar();

	// get end of the length of this string
	while (next != ':') {

	    buf.append(next);
	    next = status.readNextChar();
	}

	try {

	    int length = Integer.parseInt(buf.toString());

	    // get the string itself
	    String ret = status.substring(status.getCurr(), status.getCurr() + 
					  length);
	    status.incCurr(length);
	    return ret;

	} catch (NumberFormatException nfe) {

	    nfe.printStackTrace();
	    return null;
	}
    }

    /**
     * encodes this message into a byte array
     *
     * @param a byte array containing the bencoded form of this object's data
     */
    public byte[] getMessageBytes() {

	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	String encoding = Utils.encoding;
	writeMessageBytes(baos, encoding);
	return baos.toByteArray();
    }

    /**
     * writes the bytes of the bencoded form of this object's data into the
     * provided output stream using the specified character encoding
     *
     * @param baos -- stream to write the bytes of this message
     * @param encoding -- how to encode the strings into a byte array
     */
    public void writeMessageBytes(ByteArrayOutputStream baos, 
				  String encoding) {

	try {

	    // write the d
	    baos.write(100);

	    // loop over the keys
	    for (int i = 0; i < keyOrder.size(); i++) {

		String name = (String)keyOrder.get(i);
		baos.write(("" + name.length()).getBytes(encoding));
		baos.write(58);
		baos.write(name.getBytes(encoding));
		Object value = get(name);

		// make sure this isn't a byte array
		if (!(value instanceof byte[])) {
		    
		    writeObject(baos, encoding, value);

		} else {

		    // write this byte array raw to the output stream
		    byte arr[] = (byte[])value;
		    baos.write(("" + arr.length).getBytes(encoding));
		    baos.write(58);
		    baos.write((byte[])value);
		}
	    }

	    // write the e
	    baos.write(101);

	} catch (IOException ioe) { ioe.printStackTrace(); }
    }

    // writes an object to the specified output stream using the specified
    // encoding
    private void writeObject(ByteArrayOutputStream baos, String encoding, 
			     Object value) throws IOException {

	if (value instanceof Integer) {
	    
	    baos.write(105);
	    baos.write(("" + ((Integer)value).intValue()).getBytes(encoding));
	    baos.write(101);
	    
	} else if (value instanceof String) {
	    
	    baos.write(("" + ((String)value).length()).getBytes(encoding));
	    baos.write(58);
	    baos.write(((String)value).getBytes(encoding));
	    
	} else if (value instanceof List) {
	    
	    baos.write(108);
	    List list = (List)value;
	    
	    for (int i = 0; i < list.size(); i++) {
		
		writeObject(baos, encoding, list.get(i));
	    }
	    
	    baos.write(101);
	    
	} else if (value instanceof Hashtable) {
	    
	    ((BencodedMessage)value).writeMessageBytes(baos, encoding);
	    
	} else {
	    
	    System.err.println("illegal value type " + value);
	}
    }
    /*
    public static void main(String args[]) {

	try {

	    File file = new File("test/Testfile.torrent");
	    FileInputStream fis = new FileInputStream(file);
	    byte arr[] = new byte[20];
	    fis.skip(107);
	    fis.read(arr, 0, 20);
	    for (int i = 0; i < arr.length; i++) {

		System.out.print(arr[i]);
		System.out.print(" ");
	    }

	    System.out.println("");
	    
	    /*	    BencodedMessage message = new BencodedMessage();
	    ArrayList list = new ArrayList();
	    list.add("str2");
	    list.add(new Integer(3));
	    message.setIntValue("int", 9);
	    message.setStringValue("str", "string");
	    message.setListValue("list", list);
	    System.out.println("message " + message);

	    String mess = message.getMessageString();
	    System.out.println("encoding " + mess);
	    message = readFromMessage(mess);
	    System.out.println("message " + message);

	} catch (Exception e) { e.printStackTrace(); }
	}*/
}
