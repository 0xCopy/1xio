package hideftvads.proto;

import java.io.*;
import java.nio.channels.*;
import java.util.concurrent.*;

/**
 * User: jim
 * Date: May 6, 2009
 * Time: 5:46:51 PM
 */
public interface Protocol {
    int BUFFERSIZE = 512;

    ExecutorService REACTOR = Executors.newFixedThreadPool(32);

    void onWrite(SelectionKey key);

    void onRead(SelectionKey key) throws IOException;

    void onEnd(SelectionKey key, SocketChannel client) throws IOException;

    void onConnect(SelectionKey key) throws IOException, IOError;
}
