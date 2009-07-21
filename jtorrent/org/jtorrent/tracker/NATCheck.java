
package org.jtorrent.tracker;

import java.net.InetAddress;
import java.net.Socket;
import java.io.InputStream;

import org.jtorrent.msgdata.*;
import org.jtorrent.ptop.*;

/**
 * This class checks a peer to see if it is behind a firewall.  Basically if
 * the peer can send out bytes on port 6881.
 *
 * @author Hunter Payne
 */
public class NATCheck {

    private boolean okay;

    /**
     * Checks if the peer can upload bytes, or if it is behind a firewall.
     *
     * @param ip -- ip address of the peer to check
     * @param port -- port of the peer to check
     * @param infoHash -- info hash to send to the peer
     */
    public NATCheck(InetAddress ip, int port, byte[] infoHash) {

	okay = false;

	try {

	    Socket socket = new Socket(ip, port);
	    ProtocolMessageFormatter formatter = 
		new ProtocolMessageFormatter(socket);
	    byte peerId[] = new byte[20];
	    for (int i = 0; i < 20; i++) peerId[i] = (byte)i;
	    formatter.sendHandshake(infoHash, peerId);
	    InputStream stream = socket.getInputStream();

	    byte output[] = new byte[68];
	    int read = stream.read(output, 0, 68);

	    while (read != -1) {

		int curr = stream.read(output, read, 68 - read);
		if (curr != -1) read += curr;
		else read = -1;
	    }

	    int length = (int)output[0];

	    // skip protocol name and eight reserved bytes and length byte
	    int curr = length + 9;
	    byte peerInfoHash[] = new byte[20];
	    
	    // copy the info hash
	    for (int i = 0; i < 20; i++)
		peerInfoHash[i] = output[i + curr];

	    curr += 20;
	    
	    for (int i = 0; i < 20; i++)
		infoHash[i] = output[i + curr];

	    for (int i = 0; i < 20; i++) {

		if (infoHash[i] == peerInfoHash[i]) {

		    if (i == 19) okay = true;
		    
		} else i = 20;
	    }

	} catch (Exception e) {

	    e.printStackTrace();
	}
    }

    /** returns true if this peer isn't behind a firewall */
    public boolean isOkay() {

	return okay;
    }
}
