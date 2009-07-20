package hideftvads.proto;

import alg.*;
import javolution.text.*;

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
    public final static String[] stifle = new String[]{
//            "Accept-Encoding",
//            "Accept-Charset",
            "Keep-Alive",
            "Connection",
            "Proxy-Connection",
            "Range",
            "If-Modified-Since",
            "Cache-Control"
    };

    static {
        Arrays.sort(stifle);
    }

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
            final ByteBuffer b = (ByteBuffer) byteBufferReference.get().clear();
            if (HttpMethod.USE_HTTP_PASSTHROUGH) {
                b.put(ProtoUtil.UTF8.encode("GET " + s + " "));

                b.put((ByteBuffer) src.limit(lines.getLast().$1()).position(lines.getFirst().$2().get(1)));
            } else {


                b.put(ProtoUtil.UTF8.encode("GET " + s + " HTTP/1.0\r\nConnection: close\r\n"));

//                b.put((ByteBuffer) src.limit(lines.get(1).$1()).position(lines.getFirst().$2().get(1)));
                lines.removeFirst();


                for (Pair<Integer, LinkedList<Integer>> line : lines) {
                    if (line.$2().isEmpty()) break;
                    src.limit(line.$2().getFirst() - 2).position(line.$1());
                    final String s1 = ProtoUtil.UTF8.decode(src).toString();
                    final int i = Arrays.binarySearch(stifle, s1);
                    if (i >= 0) continue;
                    final Integer newLimit = line.$2().getLast();

                    try {
                        final Buffer buffer = src.limit(newLimit + 1);
                        final ByteBuffer byteBuffer = (ByteBuffer) buffer.position(line.$1());
                        b.put(byteBuffer);
                    } catch (Throwable e) {
                        b.put((ByteBuffer) src.clear().position(line.$1()));
                    }

                }
                b.put((byte) '\r');
                b.put((byte) '\n');
            }


            /*
                        System.err.println("to send: " + hideftvads.proto.ProtoUtil.UTF8.decode((ByteBuffer) b.rewind()));
                        System.err.println("to send: " + ci + ":" + b.get(ci - 4)
                                + ":" + b.get(ci - 3)
                                + ":" + b.get(ci - 2)
                                + ":" + b.get(ci - 1)
                                + ":" + b.get(ci)
                        );
            */

            b.rewind();
            while (b.hasRemaining()) {

                final int amt = ((SocketChannel) srv.channel()).write(b);

            }


            final SelectionKey in = srv;
            final SelectionKey out = client;
            chain(in, out);
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
                 ProtoUtil.recycle(byteBufferReference);
        }
        return null;
    }

    private void chain(SelectionKey in, SelectionKey out) {
        in.interestOps(OP_READ);
        out.interestOps(0);

        final BlockingQueue<Reference<ByteBuffer>> q = new LinkedBlockingQueue<Reference<ByteBuffer>>(3);
        ProtoUtil.threadPool.submit(new ProxyWriteWorker(this, q));
        in.attach(new ProxyReadWorker(this, q));
    }

}
