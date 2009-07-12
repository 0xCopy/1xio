package probe;

import hideftvads.proto.*;
import static hideftvads.proto.ProtoUtil.*;
import junit.framework.*;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import static java.nio.channels.SelectionKey.*;
import java.nio.channels.spi.*;
import java.nio.charset.*;
import java.util.*;

public class TestNNTP extends TestCase {
    private static final Charset UTF8 = Charset.forName("UTF8");
    private static final Selector SELECTOR;

    static {
        Selector SELECTOR1;
        try {
            SELECTOR1 = Selector.open();
        } catch (IOException e) {
            SELECTOR1 = null;
        }
        SELECTOR = SELECTOR1;
    }


    public void testProbe() {
        try {
            final SocketChannel c = SelectorProvider.provider().openSocketChannel();

            c.configureBlocking(false);
            c.register(SELECTOR, OP_CONNECT, new NntpSession());
            c.connect(new InetSocketAddress("news.easynews.com", 119));

            Runnable runnable = new Runnable() {
                public void run() {

                    do {
                        try {
                            final int i = SELECTOR.select();
                            if (i > 0) {
                                final Iterator<SelectionKey> keyIterator = SELECTOR.selectedKeys().iterator();
                                while (keyIterator.hasNext()) {

                                    final SelectionKey selectionKey = keyIterator.next();
                                    keyIterator.remove();

                                    if (selectionKey.isConnectable()) {
                                        ((NntpSession) selectionKey.attachment()).lifecyle.onConnect(selectionKey);
                                        break;
                                    }
                                    ;
                                    if (selectionKey.isReadable()) {
                                        ((NntpSession) selectionKey.attachment()).lifecyle.onRead(selectionKey);
                                        break;
                                    }
                                    if (selectionKey.isWritable()) {
                                        ((NntpSession) selectionKey.attachment()).lifecyle.onWrite(selectionKey);
                                        break;
                                    }

                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } while (!killswitch);
                }
            };

            ProtoUtil.threadPool.submit(runnable);
            while (!killswitch)
                Thread.sleep(1000);

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }
}