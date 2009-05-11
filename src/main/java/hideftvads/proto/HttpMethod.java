package hideftvads.proto;

import static hideftvads.proto.HttpStatus.*;

import java.io.*;
import static java.lang.Character.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
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
            Xfer xfer = (Xfer) a[1];
            xfer.sendChunk(key);
        }
        public void onConnect(final SelectionKey key) {
            onAccept(key);
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
                final ByteBuffer buffer = (ByteBuffer) key.attachment();
                final CharSequence parameters = methodParameters(buffer);


                final String[] strings = parameters.toString().split(" ");
                final String fname = strings[0];


                final RandomAccessFile fnode = new RandomAccessFile("./" + fname, "r");


                if (fnode.getFD().valid()) {

                    final FileChannel fc = fnode.getChannel();

                    final SocketChannel channel = (SocketChannel) key.channel();

                    final Xfer xfer = new Xfer(fc, fname);

                    response(key, $200);

                    final CharBuffer c = (CharBuffer) ByteBuffer.allocateDirect(512).asCharBuffer().append("Connection: close\nContent-Length: " + fc.size()).append("\n\n").flip();

                    channel.write(UTF8.encode(c));
                    key.interestOps(SelectionKey.OP_WRITE);
                    key.attach(new Object[]{this, xfer});
                    return;
                }
            } catch (Exception e) {
            } finally {
            }
            
            try {
                    response(key, $404);
//                ((SocketChannel) key.channel()).write();
                    key.cancel();
                    key.channel().close();
                    return;
                } catch (IOException e) {
                    e.printStackTrace();  //TODO: Verify for a purpose
                }

        }

        class Xfer {
            long progress;
            FileChannel fc;
            long creation = System.currentTimeMillis();
            long completion = -1L;
            public CharSequence name;
            public long chunk;

            private void sendChunk(SelectionKey key) {
                SocketChannel channel = null;
                if (fc.isOpen() && key.isValid() && key.channel().isOpen())
                    try {
                        channel = (SocketChannel) key.channel();

                        progress += this.fc.transferTo(progress, Math.min(getRemaining(), (++chunk) << 8), channel);
                        if (getRemaining() < 1) throw new XferCompletionException();

                    } catch (Exception e) {
                        key.cancel();
                        try {
                            fc.close();
                        } catch (IOException e1) {
                            e1.printStackTrace();  //TODO: Verify for a purpose
                        }
                        fc = null;
                        try {
                            channel.close();

                        } catch (IOException e1) {
                            e1.printStackTrace();  //TODO: Verify for a purpose
                        }
                    }
            }


            public Xfer(FileChannel fc, CharSequence name) {
                this.fc = fc;
                this.name = name;
            }

            long getRemaining() {
                try {
                    return fc.size() - progress;
                } catch (Exception e) {
                    return 0;
                }
            }


            public CharSequence logEntry() throws IOException {
                return new StringBuilder().append("Xfer: ").append(name).append(' ').append(progress).append('/').append(getRemaining());

            }


            class XferCompletionException extends Exception {
            }
        }},

    POST, PUT, HEAD, DELETE, TRACE, CONNECT, OPTIONS, HELP, VERSION,
    $ {

        public void onAccept(SelectionKey selectionKey) {
            if (selectionKey.isAcceptable()) {
                SocketChannel client = null;
                SelectionKey clientkey = null;

                try {
                    client = serverSocketChannel.accept();
                    client.configureBlocking(false).register(selector, SelectionKey.OP_READ);
                } catch (IOException e) {

                    e.printStackTrace();  //TODO: Verify for a purpose

                    try {
                        if (client != null) {
                            client.close();
//                            InterruptibleChannel clientkey;
                        }
                    } catch (IOException e1) {
                        e1.printStackTrace();  //TODO: Verify for a purpose
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


            try {
                Object[] p = (Object[]) key.attachment();
                if (p == null) {
                    final SocketChannel channel;
                    channel = (SocketChannel) key.channel();

                    final ByteBuffer buffer = ByteBuffer.allocateDirect(512);
                    final int i = channel.read(buffer);

                    buffer.flip().mark();


                    for (HttpMethod httpMethod : HttpMethod.values())
                        if (httpMethod.recognize((ByteBuffer) buffer.reset())) {
                            //System.err.println("found: " + httpMethod);
                            key.attach(buffer);
                            httpMethod.onConnect(key);
                            return;
                        }

                    response(key, HttpStatus.$400);
                    channel.write(buffer);

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
    private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);
//    public void init(){}

    final ByteBuffer token = (ByteBuffer) ByteBuffer.wrap(name().getBytes()).rewind().mark();


    final int margin = name().length() + 1;
    static final ExecutorService REACTOR = Executors.newCachedThreadPool();
    private static final Charset UTF8 = Charset.forName("UTF8");


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

            if ((!isBlank) && wasBlank) {
                out.put((byte) ((byte) (in.position() & 0xff) - 1));

                System.err.println("token found: " + in.duplicate().position(prevIdx));
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
        while ((b = indexEntries.get()) != 0 && (indexEntries.position() <= margin)) last = b & 0xff;

        final int len = indexEntries.position();

        //this should be between 40 and 300 something....
        indexEntries.position(last);


        while (!Character.isISOControl(b = indexEntries.get() & 0xff)
                && !Character.isWhitespace(b)
                && ('\n' != b)
                && ('\r' != b)
                && ('\t' != b)) ;

        return decoder.decode((ByteBuffer) indexEntries.flip().position(margin));

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


        } catch (IOException e) {
            e.printStackTrace();  //TODO: Verify for a purpose
        } finally {
            try {
                key.channel().close();
            } catch (IOException e) {
                e.printStackTrace();  //TODO: Verify for a purpose
            }
            key.cancel();

        }


    }

    private static void response(SelectionKey key, HttpStatus httpStatus) throws IOException {


        final CharBuffer charBuffer = (CharBuffer) ByteBuffer.allocateDirect(512).asCharBuffer().append("HTTP/1.1 ").append(httpStatus.name().substring(1)).append(' ').append(httpStatus.caption).append('\n').flip();

        final ByteBuffer out = UTF8.encode(charBuffer);


        ((SocketChannel) key.channel()).write(out);


    }

    void onWrite(SelectionKey key) {
        throw new UnsupportedOperationException();
    }

    ;

    public static void main
            (String... a) throws IOException {

//        final HttpConnection connection = new HttpConnection();


        while (!killswitch) try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();  //TODO: Verify for a purpose
        }
    }

    final static Charset charset = UTF8;
    final static CharsetEncoder charsetEncoder = charset.newEncoder();
    final static CharsetDecoder decoder = charset.newDecoder();
    static boolean killswitch = false;


    static Selector selector;

    static ServerSocketChannel serverSocketChannel;
    //    public ByteBuffer buffer;
    private static int port = 8080;

    static {

        REACTOR.submit((new Runnable() {
            @Override
            public void run() {
                try {
                    init();
                } catch (Exception e) {
                    e.printStackTrace();  //TODO: Verify for a purpose
                }
            }
        }));
    }

    void onAccept(SelectionKey key) {
        throw new UnsupportedOperationException();
    }

    static private void init() {
        try {
            selector = Selector.open();
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.socket().bind(new java.net.InetSocketAddress(port));
            serverSocketChannel.configureBlocking(false);
            final SelectionKey listenerKey = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            while (!killswitch) {
                selector.select();
                Set keys = selector.selectedKeys();

                for (Iterator i = keys.iterator(); i.hasNext();) {
                    final SelectionKey key = (SelectionKey) i.next();
                    i.remove();

                    if (key.isValid()) {
                        try {

                            final Object at = key.attachment();
                            final HttpMethod m;
                            m = at == null
                                    ? $
                                    : (HttpMethod) (((
                                    at instanceof Object[])
                                    && ((Object[]) at)[0] instanceof HttpMethod)
                                    ? ((Object[]) at)[0]
                                    : at instanceof HttpMethod
                                    ? at
                                    : $
                            );

                            if (key.isWritable()) {
                                m.onWrite(key);
                            }

                            if (key.isReadable()) {
                                m.onRead(key);
                            }

                            if (key.isConnectable()) {
                                m.onConnect(key);
                            }

                            if (key.isAcceptable()) {
                                m.onAccept(key);
                            }


                        } catch (Exception e) {
                            key.attach(null);
                            key.channel().close();
                        }
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
};