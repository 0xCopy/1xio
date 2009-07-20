package hideftvads.proto;

import alg.*;
import javolution.text.*;

import java.io.*;
import java.lang.ref.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import static java.nio.channels.SelectionKey.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * User: jim
 * Date: Jul 20, 2009
 * Time: 12:06:12 AM
 */
class ProxyConnectWorker implements Callable {
    private final SocketChannel sc;
    private final LinkedList<Pair<Integer, LinkedList<Integer>>> lines;
    private final Text path;
    private final ByteBuffer src;
    public final SelectionKey srv;
    public final SelectionKey client;

    public ProxyConnectWorker(SocketChannel sc, LinkedList<Pair<Integer, LinkedList<Integer>>> lines, Text path, ByteBuffer src, SelectionKey proxy, SelectionKey client) {
        this.sc = sc;
        this.lines = lines;
        this.path = path;
        this.src = src;
        this.srv = proxy;
        this.client = client;
    }


    @Override
    public Object call() throws Exception {

//            final Object x = new Object();
//            final Reference<ByteBuffer>[] br = new Reference[1];
        if (!sc.finishConnect()) {
            return this;
        }

        final Pair<Integer, LinkedList<Integer>> first = lines.getFirst();

        final String spec = path.toString();
        final String s = new URL(spec).getFile();
        System.err.println("requesting " + s);
        final Reference<ByteBuffer> byteBufferReference = hideftvads.proto.ProtoUtil.borrowBuffer();

        try {
            final ByteBuffer
                    b = (ByteBuffer) byteBufferReference.get().clear();
            if (HttpMethod.USE_HTTP_PASSTHROUGH) {
                b.put(ProtoUtil.UTF8.encode("GET " + s + " "));

                b.put((ByteBuffer) src.limit(lines.getLast().$1()).position(lines.getFirst().$2().get(1)));
            } else {
                b.put(ProtoUtil.UTF8.encode("GET " + s + " HTTP\r\n"));
                b.put((ByteBuffer) src.limit(lines.getLast().$1()).position(lines.get(1).$1()));
            }

//            final ByteBuffer buffer1 = byteBufferReference.get();
//            sc.write(UTF8.encode("GET " + s + " "));
//            final ByteBuffer buffer = (ByteBuffer) src.limit(lines.getLast().$1()-1).position(lines.getFirst().$2().get(1));
//            final CharBuffer charBuffer = UTF8.decode(buffer);
//            System.err.println("relaying " + charBuffer.toString());
//
//            try {
//                final SocketChannel socketChannel = (SocketChannel) srv.channel();
//                socketChannel.configureBlocking(false);
//                final int i = socketChannel.write(buffer);
//
            int ci = b.flip().limit() - 1;
            System.err.println("to send: " + hideftvads.proto.ProtoUtil.UTF8.decode((ByteBuffer) b.rewind()));
            System.err.println("to send: " + ci + ":" + b.get(ci - 4)
                    + ":" + b.get(ci - 3)
                    + ":" + b.get(ci - 2)
                    + ":" + b.get(ci - 1)
                    + ":" + b.get(ci)
            );

            b.rewind();
            while (b.hasRemaining()) {
//                    ((SocketChannel) srv.channel()).configureBlocking(true);
                final int amt = ((SocketChannel) srv.channel()).write(b);

            }


            final SelectionKey in = srv;
            final SelectionKey out = client;

//            chain(out, in);      
            chain(in, out);
//            in.interestOps((int) OP_READ);
//            out.interestOps((int) OP_READ);


        } finally {

        }
        return null;
    }

    private void chain(SelectionKey in, SelectionKey out) {
        in.interestOps(OP_READ);
        out.interestOps(0);

        final BlockingQueue<Reference<ByteBuffer>> q = new LinkedBlockingQueue<Reference<ByteBuffer>>(3);
        ProtoUtil.threadPool.submit(new ProxyWriteWorker(this, q));
        in.attach(new ProxyReadWorker(q));
    }

    private class ProxyReadWorker implements Runnable {
        private final BlockingQueue<Reference<ByteBuffer>> q;

        public ProxyReadWorker(BlockingQueue<Reference<ByteBuffer>> q) {
            this.q = q;
        }

        @Override
        public void run() {
            final Reference<ByteBuffer> br = hideftvads.proto.ProtoUtil.borrowBuffer();
            final ByteBuffer b = br.get();
            final SocketChannel is = (SocketChannel) srv.channel();
            final int remaining = q.remainingCapacity();
            if (remaining >= 1) {
                int i = 0;
                try {
                    i = is.read(b);
                    System.err.println("read " + i);
                    if (-1 == i) {
                        srv.cancel();
                    } else {
                        b.flip();
                        q.add(br);

                    }
                } catch (IOException e) {
                    e.printStackTrace();  //TODO: Verify for a purpose
                }
            } else {
                srv.interestOps(0);
                ProtoUtil.timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        srv.interestOps(OP_READ);
                    }
                }, 250);
            }
        }
    }
}
