package hideftvads.proto;

import java.io.*;
import java.nio.channels.*;
import java.nio.*;

/**
 * provides a http Server daemon interface/rest agent.  
 * 
 * 
 * 
 * User: jim
 * Date: May 6, 2009
 * Time: 11:50:22 PM
 */
public class httpProtocol extends ProtocolImpl{
    {{
        
        port=8181;
    }}

    /**
     * this is where we take the input channel bytes, and write them to an output channel
     * 
     * @param key
     * @param socketChannel
     */
    @Override
    public void onWrite(SelectionKey key, SocketChannel socketChannel) {
        //ToDo: verify for a purpose
    }

    /**
     * this is where we implement http 1.1. request handling 
     * @param key
     * @param socketChannel
     * @throws IOException
     */
    @Override
    public void onRead(SelectionKey key, SocketChannel socketChannel) throws IOException {


        for (HttpMethod httpMethod : HttpMethod.values()) {
           
            
            if(httpMethod.recognize(buffer)){
                final ByteBuffer buffer1 = httpMethod.tokenize(buffer);
                

            }
            
        }     
        
    }
}
