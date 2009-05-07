package hideftvads.proto;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;

/**
 * User: jim
 * Date: May 6, 2009
 * Time: 4:22:34 PM
 */
public abstract class ProtocolImpl implements Protocol {

    Charset charset = Charset.forName("ISO-8859-1");
    CharsetEncoder charsetEncoder = charset.newEncoder();
    CharsetDecoder decoder = charset.newDecoder();

    ByteBuffer buffer = ByteBuffer.allocate(512);

    Selector selector;

    ServerSocketChannel serverSocketChannel;
    boolean killswitch = false;
      int port = 8008;
 
 
    public ProtocolImpl() {

        try {
            selector = Selector.open();

            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.socket().bind(new java.net.InetSocketAddress(port));
            serverSocketChannel.configureBlocking(false);
            SelectionKey selectionKey = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            while (!killswitch) {
                selector.select();
                Set keys = selector.selectedKeys();

                for (Iterator i = keys.iterator(); i.hasNext();) {
                    SelectionKey key = (SelectionKey) i.next();
                    i.remove();

                    if (key.isValid() && key == selectionKey) {
                        onConnect(key);
                    } else { 
                        SocketChannel client = (SocketChannel) key.channel();

                        if (key.isValid() && key.isReadable()) { 
                            onRead(key, client);
                        }

                        if (key.isValid() && key.isWritable()) {
                            onWrite(key, client);
                        }
                    }
                }
            }
        } catch (IOException e) {
         }
    }

  abstract  public void onWrite(SelectionKey key, SocketChannel socketChannel);

   abstract public void onRead(SelectionKey key, SocketChannel socketChannel) throws IOException;    

    public void onEnd(SelectionKey key, SocketChannel socketChannel) throws IOException {
        key.cancel();
        socketChannel.close();
    }

    public void onQuit(SelectionKey key, SocketChannel socketChannel) throws IOException {
        key.cancel();
        socketChannel.close();
        killswitch = true; 
    }

    public void onConnect(SelectionKey selectionKey) throws IOException {
        if (selectionKey.isAcceptable()) {
            SocketChannel client = serverSocketChannel.accept();
            client.configureBlocking(false);
            SelectionKey clientkey = client.register(selector, SelectionKey.OP_READ);
            clientkey.attach(new Integer(0));
        }
    }

}

     