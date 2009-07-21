
package org.jtorrent.plugin;

import java.util.Properties;
import java.util.Iterator;
import java.util.Set;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.net.ServerSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * This server accepts socket connections from the loopback device to allow multiple browser 
 * requests to be handled by one JTorrent client.  Currently, the only configuration parameter
 * is the port to use
 *
 * @author Hunter Payne
 */
public class KickoffServer implements Runnable {

    private Selector selector;
    private int port;
    private ServerSocketChannel ssc;
    
    /**
     * start a the server on port 7020 using the default configuration parameters
     */
    public static KickoffServer makeDefaultKickoffServer() throws IOException {
	
	Properties conf = new Properties();
	conf.put("port", "7020");
	return new KickoffServer(conf);
    }

    /**
     * This creates a server and starts it.  No other additional calls are necessary.  When the
     * JVM exits, the server will release the server port on its own.
     *
     * @param config -- configuration of this server
     */
    public KickoffServer(Properties config) throws IOException {

	selector = Selector.open();

	// Create a new server socket and set to non blocking mode
	ssc = ServerSocketChannel.open();
	ssc.configureBlocking(false);
	ServerSocket ss = ssc.socket();

	port = Integer.parseInt((String)config.get("port"));
	InetSocketAddress address = new InetSocketAddress("127.0.0.1", port);

	try {
	    
	    ss.bind(address);
	    System.out.println("bound to " + address);
	    
	} catch (IOException ioe) {
	    
	    ioe.printStackTrace();
	    System.err.println("unable to bind to port...");
	    System.exit(1);
	}

	// create the accept key
	try {

	    SelectionKey acceptKey = 
		ssc.register(selector, SelectionKey.OP_ACCEPT);

	} catch (Exception e) { e.printStackTrace(); }

	// start the management thread
	Thread thread = new Thread(this);
	thread.start();
    }

    /**
     * runs the thread responsible for network management
     */
    public void run() {

	while(true) {

	    try {

		if (selector.select() > 0) {

		    Set keys = selector.selectedKeys();
		    Iterator it = keys.iterator();

		    // loop over the connections which are ready
		    while (it.hasNext()) {

			SelectionKey key = (SelectionKey)it.next();
			it.remove();

			// if an accept request is ready
			if ((key.readyOps() & SelectionKey.OP_ACCEPT) > 0) {
			    
			    ServerSocketChannel channel = (ServerSocketChannel)key.channel();
			    SocketChannel sc = channel.accept();

			    if (sc != null) {

				sc.configureBlocking(false);
				SelectionKey connectKey = 
				    sc.register(selector, SelectionKey.OP_READ);
				//			System.out.println("accepted socked");
			    }
			}

			// if this socket is ready to read
			if ((key.readyOps() & SelectionKey.OP_READ) > 0) {
			    
			    SocketChannel channel = 
				(SocketChannel)key.channel();

			    try {

				KickoffFixedLengthMessage sb = 
				    (KickoffFixedLengthMessage)key.attachment();

				// if we are starting this read
				if (sb == null) {

				    ByteBuffer sizeBuf = ByteBuffer.allocate(1);
				    int read = channel.read(sizeBuf);
				    
				    if (read != -1) {
					
					sizeBuf.flip();
					byte size = sizeBuf.get();
					ByteBuffer buf = ByteBuffer.allocate((int)size);
					int bytesread = channel.read(buf);

					// we got the entire message
					if (bytesread == size) {
					    
					    String msg = new String(buf.array());
					    System.out.println("got full message to start " + 
							       msg.substring(4));
					    channel.close();
					    
					} else {
					    
					    // we got only part of the message so we attach
					    // it to the socket to finish later
					    String msg = new String(buf.array(), 0, bytesread);
					    sb = new KickoffFixedLengthMessage(msg, size);
					    key.attach(sb);
					}
				    } else channel.close();
				} else {

				    // number of bytes left to read
				    int size = sb.getMylen() - sb.length();
				    ByteBuffer buf = ByteBuffer.allocate(size);
				    int bytesread = channel.read(buf);
					
				    // we finally have all the bytes of this message
				    if (bytesread == size) {
					    
					String msg = new String(buf.array());
					sb.append(msg);
					System.out.println("got message to start " + 
							   sb.substring(4));
					channel.close();
					    
					// we still don't have the full message
				    } else {
					    
					String msg = new String(buf.array(), 0, bytesread);
					sb.append(msg);
				    }
				}
			    } catch (NumberFormatException nfe) {

				channel.close();

			    } catch (IOException ioe2) {

				channel.close();
			    }
			} 
		    }
		} else {
		    
		    Thread.sleep(10);
		}
	    } catch (IOException ioe) {

		System.err.println(ioe.getMessage());

	    } catch (Throwable t) {

		t.printStackTrace();
	    }
	}
    }

    // inner class to represent a partially read message
    private class KickoffFixedLengthMessage {

	private int mylen; // size of the message
	private StringBuffer mybuf; // the buffer containing the bytes already read

	KickoffFixedLengthMessage(String msg, int mylen) {

	    mybuf = new StringBuffer(msg);
	    this.mylen = mylen;
	}

	// number of bytes in the message
	private int getMylen() { return mylen; }

	// number of bytes already read
	private int length() { return mybuf.length(); }

	// add to the message
	private void append(String s) { mybuf.append(s); }

	// get part of the message
	private String substring(int index) { return mybuf.substring(index); }

	// get entire message
	public String toString() { return mybuf.toString(); }
    }

    /**
     * for testing
     */
    public static void main(String args[]) {

	if (args.length != 1) {

	    System.out.println("usage: java org.jtorrent.plugin.KickoffServer config.cfg");
	    System.exit(1);
	}

	// load configuration from a file
	Properties props = new Properties();
	File propsFile = new File(args[0]);

	try {

	    props.load(new FileInputStream(propsFile));

	} catch (java.io.FileNotFoundException fnfe) {

	    System.out.println("no file named " + args[0] + " found");
	    System.exit(1);

	} catch (IOException ioe) {

	    System.out.println("unable to read " + propsFile.getAbsolutePath());
	    System.exit(1);
	}

	try {

	    // start the server
	    new KickoffServer(props);

	} catch (IOException ioe) {

	    ioe.printStackTrace();
	}
    }
}
