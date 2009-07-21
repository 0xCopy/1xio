
package org.jtorrent.plugin;

import java.net.Socket;
import java.io.OutputStream;
import java.io.IOException;

/**
 * This client tries to send a message using the loopback device to an already running jtorrent
 * client.  If it can't it will throw an IOException.  This means that this process should start
 * the jtorrent client inside of its own process and start the download itself.  In addition, it
 * should start the KickoffServer so new clients use this process.
 *
 * @author Hunter Payne
 */
public class KickoffClient {

    private Socket sock;
    private OutputStream os;

    /**
     * Opens a sock to a client on the same machine to request it start downloading the specified
     * .torrent.  It uses port 7020 by default.  If it can't connect to port 7020, it 
     * throws an IOException.
     *
     * @param url -- location of the .torrent
     */
    public KickoffClient(String url) throws IOException {

	this(url, 7020);
    }

    /**
     * Opens a sock to a client on the same machine to request it start downloading the specified
     * .torrent.  If it can't connect to the localhost port, it throws an IOException
     *
     * @param url -- location of the .torrent
     * @param port -- port to connect to
     */
    public KickoffClient(String url, int port) throws IOException {
 
	sock = new Socket("127.0.0.1", port);
	os = sock.getOutputStream();
	String msg = "GET " + url;
	System.out.println("sending " + msg);
	os.write(msg.length());
	os.write(msg.getBytes());
	os.flush();
	sock.close();
    }

    private static int getNextArg(int curr, String args[], String chars) {

	while (curr < args.length) {

	    if (args[curr].charAt(0) == '-') {

		for (int i = 0; i < chars.length(); i++) {

		    if (chars.charAt(i) == args[curr].charAt(1)) return curr;
		}
	    }

	    curr++;
	}

	return curr;
    }
    
    /**
     * main entry point for the browser
     */
    public static void main(String args[]) {
	
	String url = null;
	int port = 7020;
	KickoffClient client = null;
	String options = "pt";
	int curr = getNextArg(0, args, options);

	// get command line arguements
	while (curr < args.length) {

	    char option = args[curr].charAt(1);
	    
	    switch(option) {
	    case 'p':

		try {

		    curr++;
		    port = Integer.parseInt(args[curr]);
		    //		    System.out.println("port " + port);

		} catch (NumberFormatException nfe) { 

		    System.err.println(args[curr] + " isn't a port number"); 
		}

		curr++;
		break;
	    case 't':

		curr++;
		url = args[curr];
		curr++;
		break;
	    default: curr++;
	    }
	    
	    curr = getNextArg(curr, args, options);
	}
	
	try {

	    if (url != null) client = new KickoffClient(url, port);
	    else System.err.println("no .torrent file specified");

	} catch (java.net.ConnectException ce) {

	    System.out.println(ce.toString());
	    // TODO start the client ourselves

	} catch (Exception e) {

	    e.printStackTrace();
	}
    }
}
