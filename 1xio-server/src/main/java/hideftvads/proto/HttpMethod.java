package hideftvads.proto;

import alg.*;
import static hideftvads.proto.HttpStatus.*;
import static hideftvads.proto.ProtoUtil.*;
import javolution.text.*;
import javolution.util.*;

import java.io.*;
import static java.lang.Character.*;
import java.lang.ref.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import static java.nio.channels.SelectionKey.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * See  http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html
 * User: jim
 * Date: May 6, 2009
 * Time: 10:12:22 PM
 */
public enum HttpMethod {
    GET {
        public void onWrite(final SelectionKey key) {
            Object[] a = (Object[]) key.attachment();
            fileXfer xfer = (fileXfer) a[1];
            xfer.sendChunk(key);
        }

        /**
         * enrolls a new SelectionKey to the methods
         *
         * @param key
         * @throws IOException
         */
        @Override
        public void onAccept(final SelectionKey key) {
            try {
                assert key.attachment() instanceof ByteBuffer;
                final ByteBuffer src = (ByteBuffer) key.attachment();

                final LinkedList<alg.Pair<Integer, LinkedList<Integer>>> lines = preIndex(src);

                final int fnend = lines.get(0).$2().get(1) - 1;
                final Text path = new Text(UTF8.decode((ByteBuffer) src.limit(fnend).position(margin)).toString());
                src.limit(lines.getLast().$1());

                if (path.startsWith(HTTP_PREFIX)) {
                    URL uri = new URL(path.toString());

                    int port = uri.getPort();
                    if (port == -1) port = 80;
                    final InetSocketAddress remote = new InetSocketAddress(uri.getHost(), port);
                    final SocketChannel sc = SocketChannel.open();
                    sc.configureBlocking(false);
                    sc.socket().setTcpNoDelay(true);
                    sc.connect(remote);

                    final SelectionKey proxy = sc.register(key.selector(), SelectionKey.OP_CONNECT);

                    proxy.attach(new ProxyConnectWorker(sc, lines, path, src, proxy, key));
                    key.interestOps(0);
                    return;
                } else {
                    try {

                        String name1 = path.toString();
                        if (name1.charAt(0) == '/') name1 = name1.substring(1);
                        name1.replaceAll("/..", "/.");

                        final RandomAccessFile fnode = new RandomAccessFile(name1, "r");

                        if (fnode.getFD().valid()) {
                            final FileChannel fc = fnode.getChannel();
                            final SocketChannel channel = (SocketChannel) key.channel();
                            final fileXfer xfer = new fileXfer(fc, path);
                            response(key, $200);
                            final Reference<ByteBuffer> byteBufferReference = new SoftReference<ByteBuffer>(ByteBuffer.allocateDirect(1024));
                            try {
                                final ByteBuffer buffer1 = byteBufferReference.get();
                                MimeType mimeType = null;
                                try {
                                    mimeType = MimeType.valueOf(path.subtext(path.lastIndexOf(".") + 1).toString());
                                } catch (Exception ignored) {
                                }
                                String mimeHeader = mimeType == null ? "\n" : "Content-Type: " + mimeType.contentType + "\n";
                                final CharBuffer c = (CharBuffer) buffer1.asCharBuffer().append("Connection: close\n").append(mimeHeader).append("Content-Length: ").append(String.valueOf(fc.size())).append("\n\n").flip();
                                channel.write(UTF8.encode(c));


                                key.attach(new Object[]{this, xfer});
                                key.interestOps(SelectionKey.OP_WRITE);
                            } catch (Exception ignored) {
                            } finally {
                            }
                            return;
                        }
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
                try {
                    response(key, $404);
                    key.cancel();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();  //TODO: verify for a purpose
            }

        }

        class fileXfer {
            long progress;
            FileChannel fc;
            long creation = System.currentTimeMillis(),
                    completion = -1;
            public CharSequence name;
            public long chunk;
            private boolean pipeline = false;

            void sendChunk(final SelectionKey key) {
                if (!fc.isOpen() || !key.isValid() || !key.channel().isOpen()) {
                    return;
                }
                final SocketChannel[] channel = new SocketChannel[]{null};
                final Callable<Object>
                        callable = new Callable<Object>() {
                    public Object call() throws Exception {
//                        Context.enter(LogContext.class);
                        try {
                            try {

                                progress += fc.transferTo(progress, Math.min(getRemaining(), ++chunk << 8), (WritableByteChannel) key.channel());
                                if (getRemaining() < 1) {

                                    completion = System.currentTimeMillis();
                                    final double span = (double) completion - creation / 1000.0;
                                    final String s = name() + ':' + ((SocketChannel) key.channel()).socket().getInetAddress().getCanonicalHostName() + '/' + name.toString() + ' ' + creation + ' ' + ":complete:" + ' ' + fc.size() / span + ' ' + "chunkavg " + fc.size() / chunk;
                                    System.err.println(s);
                                    try {
                                        fc.close();
                                    } catch (IOException ignored) {
                                    }
                                    if (pipeline) {
                                        key.attach($);
                                        key.interestOps(OP_READ);
                                    } else {
                                        key.cancel();
                                    }
                                }

                            } catch (Exception e) {
                                System.err.println(name() + ":fail:" + e.getCause() + ' ' + creation + ' ' + name + " progress:" + progress);

                                key.cancel();
                                try {
                                    fc.close();
                                } catch (IOException ignored) {
                                }
                                fc = null;
                                try {
                                    if (channel[0] != null) {
                                        channel[0].close();
                                    }
                                } catch (IOException ignored) {
                                }
                            }
                        } finally {
//                            Context.exit(LogContext.class);
                        }
                        return null;
                    }
                };
                try {
                    callable.call();
                } catch (Exception ignored) {

                }
            }


            public fileXfer(FileChannel fc, CharSequence name) {
                this.fc = fc;
                this.name = name;
                completion = -1L;
            }

            long getRemaining() {
                try {
                    return fc.size() - progress;
                } catch (Exception e) {
                    return 0;
                }
            }


            public CharSequence logEntry() throws IOException {
                return new StringBuilder().append(getClass().getName()).append(':').append(name).append(' ').append(progress).append('/').append(getRemaining());
            }


        }},

    POST, PUT, HEAD, DELETE, TRACE, CONNECT {
        @Override
        public void onWrite(SelectionKey k) {
            final SelectionKey selectionKey = k;
            final Object o[] = (Object[]) selectionKey.attachment();


            FastMap tree = (FastMap) o[1];
            ByteBuffer request = (ByteBuffer) o[2];
            LinkedList<Pair<Integer, LinkedList<Integer>>> lines = (LinkedList<Pair<Integer, LinkedList<Integer>>>) o[3];
            SocketChannel c = (SocketChannel) k.channel();


        }}, OPTIONS, HELP, VERSION,
    $ {

        public void onAccept(SelectionKey key) {
            if (key.isAcceptable()) {
                SocketChannel client = null;

                try {
                    final ServerSocketChannel socketChannel = (ServerSocketChannel) key.channel();

                    client = socketChannel.accept();
                    client.configureBlocking(false).register(key.selector(), OP_READ);
                } catch (IOException e) {

                    e.printStackTrace();
                    try {
                        if (client != null) {
                            client.close();
                        }
                    } catch (IOException ignored) {
                    }
                }
            }
        }

        /**
         * this is where we take the input channel bytes, and write them to an output channel
         *
         * @param key
         */
        @Override
        public void onWrite(SelectionKey key) {
            final Object[] att = (Object[]) key.attachment();
            if (att != null) {
                HttpMethod method = (HttpMethod) att[0];
                method.onWrite(key);
                return;
            }
            key.cancel();
        }

        /**
         * this is where we implement http 1.1. request handling
         * <p/>
         * Lifecycle of the attachemnts is
         * <ol>
         * <li> null means new socket
         * <li>we attach(buffer) during the onConnect
         * <li> we <i>expect</i> Object[HttpMethod,*,...] to be present for ongoing connections to delegate
         * </ol>
         *
         * @param key
         * @throws IOException
         */
        @Override
        public void onRead(SelectionKey key) {


            Reference<ByteBuffer> byteBufferReference = null;
            try {
                Object[] p = (Object[]) key.attachment();

                if (p == null) {
                    final SocketChannel channel;
                    channel = (SocketChannel) key.channel();

                    byteBufferReference = new SoftReference<ByteBuffer>(ByteBuffer.allocateDirect(1024));
                    try {
                        final ByteBuffer buffer = byteBufferReference.get();
                        final int i = channel.read(buffer);

                        buffer.flip().mark();

                        for (final HttpMethod httpMethod : HttpMethod.values())
                            if (httpMethod.recognize((ByteBuffer) buffer.reset())) {
                                //System.out.println("found: " + httpMethod);
                                key.attach(buffer);

                                httpMethod.onAccept(key);
                                return;
                            }

                        response(key, HttpStatus.$400);
                        channel.write(buffer);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    } finally {
                    }
                    channel.close();
                    return;
                }

                HttpMethod fst = (HttpMethod) p[0];
                fst.onRead(key);

            } catch (IOException e) {

                e.printStackTrace();
            }
        }

    },;
//    private static int DEFAULT_EXP= DEFAULT_EXP;

    private static final String HTTP_PREFIX = "http://".intern();

    final ByteBuffer token = (ByteBuffer) ByteBuffer.wrap(name().getBytes()).rewind().mark();


    final int margin = name().length() + 1;
    public static final boolean USE_HTTP_PASSTHROUGH = false;


    /**
     * deduce a few parse optimizations
     *
     * @param request
     * @return
     */

    boolean recognize(ByteBuffer request) {

        if (isWhitespace(request.get(margin)))
            for (int i = 0; i < margin - 1; i++)
                if (request.get(i) != token.get(i))
                    return false;
        return true;
    }


    /**
     * returns a byte-array of token offsets in the first delim +1 bytes of the input buffer     *
     * <p/>
     * stateless and heapless
     *
     * @param in
     * @return
     */

    public ByteBuffer tokenize(ByteBuffer in) {

        ByteBuffer out = (ByteBuffer) in.duplicate().position(0);


        boolean isBlank = true, wasBlank = true;
        int prevIdx = 0;
        in.position(margin);
        char b = 0;
        while (b != '\n' && out.position() < margin) {
            wasBlank = isBlank;
            b = (char) (in.get() & 0xff);
            isBlank = isWhitespace(b & 0xff);

            if (!isBlank && wasBlank) {
                out.put((byte) ((byte) (in.position() & 0xff) - 1));

                System.out.println("token found: " + in.duplicate().position(prevIdx));
            }
        }

        while (out.put((byte) 0).position() < margin) ;


        return (ByteBuffer) out.position(0);
    }


    public CharSequence methodParameters(ByteBuffer indexEntries) throws IOException {


        /***
         * seemingly a lot of work to do as little as possible
         *
         */

        indexEntries.position(0);
        int last = 0;
        int b;

        // start from 0 and traverese to null terminator inserted during the tokenization...
        while ((b = indexEntries.get()) != 0 && indexEntries.position() <= margin) last = b & 0xff;

        final int len = indexEntries.position();

        //this should be between 40 and 300 something....
        indexEntries.position(last);


        while (!Character.isISOControl(b = indexEntries.get() & 0xff)
                && !Character.isWhitespace(b)
                && '\n' != b
                && '\r' != b
                && '\t' != b) ;

        return UTF8.decode((ByteBuffer) indexEntries.flip().position(margin));

    }


    private static final Random RANDOM = new Random();


    public void onRead(SelectionKey key) {
        final Object o = key.attachment();
        if (o instanceof ByteBuffer) {
            this.tokenize((ByteBuffer) o);
        }
    }


    /**
     * enrolls a new SelectionKey to the methods
     *
     * @param key
     * @throws IOException
     */
    public void onConnect(SelectionKey key) {

        try {
            response(key, $501);
            ByteBuffer b = (ByteBuffer) key.attachment();

            final SelectableChannel channel = key.channel();
            SocketChannel c = (SocketChannel) channel;

            c.write((ByteBuffer) b.rewind());


        } catch (IOException ignored) {
        } finally {
            try {
                key.channel().close();
            } catch (IOException ignored) {
            }
            key.cancel();

        }


    }

    private static void response(SelectionKey key, HttpStatus httpStatus) throws IOException {


        final Reference<ByteBuffer> byteBufferReference = new SoftReference<ByteBuffer>(ByteBuffer.allocateDirect(1024));
        try {
            final ByteBuffer buffer = byteBufferReference.get();
            final CharBuffer charBuffer = (CharBuffer) buffer.asCharBuffer().append("HTTP/1.1 ").append(httpStatus.name().substring(1)).append(' ').append(httpStatus.caption).append("\r\n").flip();

            final ByteBuffer out = UTF8.encode(charBuffer);


            ((SocketChannel) key.channel()).write(out);
        } catch (Exception ignored) {
        } finally {
        }

    }

    public void onWrite(SelectionKey key) {
        throw new UnsupportedOperationException();
    }

  

    public void onAccept(SelectionKey key) {
        throw new UnsupportedOperationException();
    }

 
 
}