package hideftvads.proto;

import java.io.*;
import java.nio.channels.*;

/**
 * provides a http Server daemon interface/rest agent.
 * <p/>
 * User: jim
 * Date: May 6, 2009
 * Time: 11:50:22 PM
 */
public class HttpConnection extends ProtocolImpl {

    private int


            port = 8080;

    /**
     * this is where we take the input channel bytes, and write them to an output channel
     *
     * @param key
     */
    @Override
    public void onWrite(SelectionKey key) {
        //ToDo: verify for a purpose
    }

    /**
     * this is where we implement http 1.1. request handling
     * <p/>
     * gatekeeper
     *
     * @param key
     * @throws IOException
     */
    @Override
    public void onRead(SelectionKey key) throws IOException {

        
        try {
            final SocketChannel socketChannel = (SocketChannel) key.channel();

            final int i = socketChannel.read(buffer);
            buffer.flip();
            key.attach(buffer);

            /**
             * select has signaled a IO event
             *
             */
            for (HttpMethod method : HttpMethod.values()) {


                for (HttpMethod httpMethod : HttpMethod.values()) {
                    if (httpMethod.recognize((buffer))) {
                        
                        key.cancel();
                        httpMethod.onConnect(key);

                        return;
                    }
                }
            }
        }
        catch (Exception e2) {
        } finally {
            
        }
    }
  
    public int getPort() {
        return port;  //ToDo: verify for a purpose
    }


    public static void main
            (String... a) {

        final HttpConnection connection = new HttpConnection();

    }
}
