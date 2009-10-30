package one.xio.proto;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

/**
 * User: jim
 * Date: Jul 20, 2009
 * Time: 12:05:38 AM
 */
class ProxyWriteWorker implements Runnable {
    private final Queue<ByteBuffer> q;
    private ProxyConnectWorker proxyConnectWorker;
    public ByteBuffer br;

    public ProxyWriteWorker(ProxyConnectWorker proxyConnectWorker, Queue<ByteBuffer> q) {
        this.proxyConnectWorker = proxyConnectWorker;
        this.q = q;
    }

    @Override
    public void run() {
//        do {
        if (br == null || !br.hasRemaining()) {
            try {
                br = q.remove();
                if (br == null) {

                    proxyConnectWorker.srv.channel().close();
                    proxyConnectWorker.client.channel().close();

                    proxyConnectWorker.srv.cancel();
                    proxyConnectWorker.client.cancel();
                    return;
                }
            } catch (Throwable e) {
                proxyConnectWorker.client.interestOps(0);
                ProtoUtil.timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            proxyConnectWorker.client.interestOps(SelectionKey.OP_WRITE);
                        }catch(Throwable e){}  


                    }
                }, 250);
                return;
            }
        }
        try {
//            br = br;
//            if (br == null) throw new Exception();
            final int i = ((SocketChannel) proxyConnectWorker.client.channel()).write(br);
//            System.err.println("wrote " + i);
        } catch (Exception e) {
            proxyConnectWorker.client.cancel();
            proxyConnectWorker.srv.cancel();
            try {
                proxyConnectWorker.client.channel().close();
                proxyConnectWorker.srv.channel().close();
            } catch (IOException e1) {
//                    e1.printStackTrace();  //TODO: Verify for a purpose
            }
//                e.printStackTrace();  //TODO: Verify for a purpose
        } finally {
        }
//        } while (proxyConnectWorker.client.isValid());
    }
}