package hideftvads.proto;

import java.lang.ref.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.concurrent.*;
import java.io.*;

/**
 * User: jim
 * Date: Jul 20, 2009
 * Time: 12:05:38 AM
 */
class ProxyWriteWorker implements Runnable {
    private final BlockingQueue<Reference<ByteBuffer>> q;
    private ProxyConnectWorker proxyConnectWorker;
 
    public ProxyWriteWorker(ProxyConnectWorker proxyConnectWorker, BlockingQueue<Reference<ByteBuffer>> q) {
         this.proxyConnectWorker = proxyConnectWorker;
        this.q = q;
    }


    @Override
    public void run() {
        do {
            Reference<ByteBuffer> br = null;
            try {
                br = q.take();
                ByteBuffer b = br.get();
                while (b.hasRemaining()) {
                    final int i = ((SocketChannel) proxyConnectWorker.client.channel()).write(b);
                    System.err.println("wrote " + i);
                }
            } catch ( Exception e) {
                proxyConnectWorker.client.cancel();
                proxyConnectWorker.srv.cancel();
                try {
                    proxyConnectWorker.client.channel().close();
                    proxyConnectWorker.srv.channel().close();
                } catch (IOException e1) {
                    e1.printStackTrace();  //TODO: Verify for a purpose
                }
                e.printStackTrace();  //TODO: Verify for a purpose
            } finally {
                ProtoUtil.recycle(br);
            }
        } while (proxyConnectWorker.client.isValid());
    }
}
