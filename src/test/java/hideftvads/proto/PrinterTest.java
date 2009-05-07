package hideftvads.proto;

import junit.framework.*;

import java.io.*;
import java.net.*;

/**
 * User: jim
 * Date: May 6, 2009
 * Time: 7:18:23 PM
 */
public class PrinterTest extends TestCase {

    dumpProtocol p;
    ByteArrayOutputStream quadprinter;
    private static final String XDEADBEEF = "0xdeadbeef";
    private Socket pusher;
    private static final String QUADPRINTER = "quadprinter\n";

    public void setUp() {
        quadprinter = new ByteArrayOutputStream();
        p = new dumpProtocol(
         );   
     }

    public void tearDown() {
        p = null;
        System.gc();
    }

    public void testOnRead() throws IOException {
        setUp();

        final ServerSocket serverSocket = p.serverSocketChannel.socket();
        final SocketAddress socketAddress = serverSocket.getLocalSocketAddress();
        final int port = p.getPort();
        final URL url = new URL("http://" + socketAddress + ":" + (port == 80 ? "" : ":" + port));
        final URLConnection urlConnection = url.openConnection();
        
        urlConnection.getOutputStream().write(QUADPRINTER.getBytes());
//        urlConnection.getOutputStream().read(QUADPRINTER.getBytes());
          urlConnection.getOutputStream().close();

    }

    public void testOnEnd() {
        // Add your code here
    }

    public void testOnQuit() {
        // Add your code here
    }

    public void testOnConnect() {
        // Add your code here
    }
}
