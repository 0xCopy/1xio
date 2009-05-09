package hideftvads.proto;


import java.io.*;
import java.nio.*;
import java.nio.channels.*;

/**
 * provides a http Server daemon interface/rest agent.
 * <p/>
 * User: jim
 * Date: May 6, 2009
 * Time: 11:50:22 PM
 */
public   class HttpConnection extends ProtocolImpl {

    private int port = 8080;

    /**
     * this is where we take the input channel bytes, and write them to an output channel
     *
     * @param key
     */
    @Override
    public void onWrite(SelectionKey key) {

        final Object[] att = (Object[]) key.attachment();

        if (att != null) {
            HttpMethod method = (HttpMethod) att[0];
            method.onWrite(key);
            return;
        }
        key.cancel();
    }

    /**
     * this is where we implement http 1.1. request handling
     * <p/>
     * Lifecycle of the attachemnts is
     * <ol>
     * <li> null means new socket
     * <li>we attach(buffer) during the onConnect
     * <li> we <i>expect</i> Object[HttpMethod,*,...] to be present for ongoing connections to delegate
     * </ol>
     *
     * @param key
     * @throws IOException
     */
    @Override
    public void onRead(SelectionKey key) {


        try {
            Object[] p = (Object[]) key.attachment();
            if (p == null) {
                final SocketChannel channel;
                channel = (SocketChannel) key.channel();

                final ByteBuffer buffer = ByteBuffer.allocateDirect(512);
                final int i = channel.read(buffer);

              buffer.flip().mark();


                for (HttpMethod httpMethod : HttpMethod.values())
                    if (httpMethod.recognize((ByteBuffer) buffer.reset())) {
                        //System.err.println("found: " + httpMethod);
                        key.attach(buffer);
                        httpMethod.onConnect(key);
                        return;
                    }

                channel.close();
                return;
            }

            HttpMethod fst = (HttpMethod) p[0];
            fst.onRead(key);

        } catch (IOException e) {
            e.printStackTrace();  //TODO: Verify for a purpose
        }
    }

    public int getPort() {
        return port;  //ToDo: verify for a purpose
    }


    public static void main
            (String... a) {

        final HttpConnection connection = new HttpConnection();
        while (!killswitch) try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();  //TODO: Verify for a purpose
        }           
    }
}
