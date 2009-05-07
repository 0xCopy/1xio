package hideftvads.proto;

import java.io.*;
import java.nio.channels.*;

/**
 * User: jim
 * Date: May 6, 2009
 * Time: 5:46:51 PM
 */
public interface Protocol {
    int BUFFERSIZE = 512;

    void onWrite(SelectionKey key, SocketChannel client);

    void onRead(SelectionKey key) throws IOException;

    void onEnd(SelectionKey key, SocketChannel client) throws IOException;

    void onConnect(SelectionKey key) throws IOException;
}
