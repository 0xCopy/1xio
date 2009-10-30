package one.xio.proto;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

/**
 * User: jim
 * Date: Jul 20, 2009
 * Time: 2:03:47 AM
 */
class ProxyReadWorker implements Runnable {
    private final Queue<ByteBuffer> q;
    private ProxyConnectWorker proxyConnectWorker;
    public final SelectionKey srv;
    public final SelectionKey client;

    public ProxyReadWorker(ProxyConnectWorker proxyConnectWorker, Queue<ByteBuffer> q) {
        this.proxyConnectWorker = proxyConnectWorker;
        this.q = q;
        srv = this.proxyConnectWorker.srv;
        client = this.proxyConnectWorker.client;
    }

    @Override
    public void run() {
        {
            final ByteBuffer br = (ByteBuffer.allocateDirect(1448 * 5));
            final SocketChannel is = (SocketChannel) srv.channel();
            final int remaining = 128 - q.size();
            if (remaining >= 1) {
                int i = 0;
                try {
                    i = is.read(br);
//                    System.err.println("read " + i);
                    if (-1 == i) {
                        q.add(null);


                        srv.cancel();
                        srv.channel().close();
                    } else if (0 < i) {
                        br.flip();
                        q.add(br);
                        client.interestOps(SelectionKey.OP_WRITE);
                    }
                } catch (IOException e) {
//                    e.printStackTrace();  //TODO: Verify for a purpose
                    client.cancel();
                    srv.cancel();
                    try {
                        client.channel().close();
                        srv.channel().close();
                    } catch (Throwable e1) {
                     }
                }
            } else {
                System.err.println("squelching");
                srv.interestOps(0);

                ProtoUtil.timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            System.err.println("unsquelching");
                            srv.interestOps(SelectionKey.OP_READ);
                        } catch (Throwable e) {
//                            e.printStackTrace();  //TODO: Verify for a purpose
                        } finally {
                        }
                    }
                }, 250);
            }
        }
    }
}
