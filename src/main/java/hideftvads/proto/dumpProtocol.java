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
    private int port = 8182;
    private ByteBuffer buffer = ByteBuffer.allocateDirect(512);

    @Override
    public void onWrite(SelectionKey key) {
    }


    @Override
    public void onRead(SelectionKey key) throws IOException {

        final SocketChannel socketChannel = (SocketChannel) key.channel();
        int bytesread = socketChannel.read((ByteBuffer) getBuffer().clear());

        if (bytesread == -1)
            socketChannel.close();

        getBuffer().flip(); //only need to do it once for trimming 


        System.out.write(getBuffer().array());
    }

    public int getPort() {
        return port;
    }


    public static void main(String... args) throws InterruptedException {
        final dumpProtocol printer = new dumpProtocol();

        Thread.currentThread().sleep(60000);
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }
}
