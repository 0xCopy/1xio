package hideftvads.proto;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;

/**
 * User: jim
 * Date: May 6, 2009
 * Time: 6:47:05 PM
 */
public class dumpProtocol extends ProtocolImpl { 

    @Override
    public void onWrite(SelectionKey key, SocketChannel socketChannel) { 
    }
    
    
    @Override
    public void onRead(SelectionKey key, SocketChannel socketChannel) throws IOException {

        int bytesread = socketChannel.read((ByteBuffer) buffer.clear());

        if (bytesread == -1)
            onEnd(key, socketChannel);

        buffer.flip(); //only need to do it once for trimming 

 
             System.out.write( buffer.array());
    }

    public int getPort() {
        return serverSocketChannel.socket().getLocalPort();
    }


    public static void main(String... args) throws InterruptedException {
        final dumpProtocol printer = new dumpProtocol();

        Thread.currentThread().sleep(60000);
    } 
}
