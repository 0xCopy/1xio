package hideftvads.proto;

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

    public ProxyReadWorker(ProxyConnectWorker proxyConnectWorker, Queue<ByteBuffer> q) {
        this.proxyConnectWorker = proxyConnectWorker;
        this.q = q;
    }

    @Override
    public void run() {
        {
            final ByteBuffer br = (ByteBuffer.allocateDirect(1448*5));
            final SocketChannel is = (SocketChannel) proxyConnectWorker.srv.channel();
            final int remaining = 128 - q.size();
            if (remaining >= 1) {
                int i = 0;
                try {
                    i = is.read(br);
                    System.err.println("read " + i);
                    if (-1 == i) {
                        q.add(null);
                    } else if (0 < i) {
                        br.flip();
                        q.add(br);                                       
                        proxyConnectWorker.client.interestOps(SelectionKey.OP_WRITE);
                    }
                } catch (IOException e) {
                    e.printStackTrace();  //TODO: Verify for a purpose
                }
            } else {
                System.err.println("squelching");
                proxyConnectWorker.srv.interestOps(0);

                ProtoUtil.timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        System.err.println("unsquelching");
                        proxyConnectWorker.srv.interestOps(java.nio.channels.SelectionKey.OP_READ);
                    }
                }, 250);
            }
        }
    }
}
