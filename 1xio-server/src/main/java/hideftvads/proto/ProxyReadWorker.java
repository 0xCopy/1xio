package hideftvads.proto;

import java.io.*;
import java.lang.ref.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * User: jim
 * Date: Jul 20, 2009
 * Time: 2:03:47 AM
 */
class ProxyReadWorker implements Runnable {
    private final BlockingQueue<Reference<ByteBuffer>> q;
    private ProxyConnectWorker proxyConnectWorker;

    public ProxyReadWorker(ProxyConnectWorker proxyConnectWorker, BlockingQueue<Reference<ByteBuffer>> q) {
        this.proxyConnectWorker = proxyConnectWorker;
        this.q = q;
    }

    @Override
    public void run() {
        synchronized (proxyConnectWorker) {
            final Reference<ByteBuffer> br = new SoftReference<ByteBuffer>(ByteBuffer.allocateDirect(1024));
            final ByteBuffer b = br.get();
            final SocketChannel is = (SocketChannel) proxyConnectWorker.srv.channel();
            final int remaining = q.remainingCapacity();
            if (remaining >= 1) {
                int i = 0;
                try {
                    i = is.read(b);
//                    System.err.println("read " + i);
                    if (-1 == i) {
                        q.add(null);
                    } else
                    if(0<i){
                        b.flip();
                        q.add(br);

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
