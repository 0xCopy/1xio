package one.xio;

import java.io.IOError;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.Reference;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.vsiwest.CouchChangesClient;

import static java.lang.Character.isWhitespace;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static one.xio.HttpStatus.$200;
import static one.xio.HttpStatus.$404;
import static one.xio.HttpStatus.$501;

/**
 * See  http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html
 * User: jim
 * Date: May 6, 2009
 * Time: 10:12:22 PM
 */
public enum HttpMethod {
    GET {
        public void onWrite(SelectionKey key) {
            Object[] a = (Object[]) key.attachment();
            Xfer xfer = (Xfer) a[1];
            xfer.sendChunk(key);
        }

        public void onConnect(SelectionKey key) {
            onAccept(key);
        }

        /**
         * enrolls a new SelectionKey to the methods
         *
         * @param key
         * @throws IOException
         */
        @Override
        public void onAccept(SelectionKey key) {
            try {
                assert key.attachment() instanceof ByteBuffer;
                ByteBuffer buffer = (ByteBuffer) key.attachment();
                CharSequence parameters = methodParameters(buffer);


                String[] strings = parameters.toString().split(" ");
                String fname = strings[0];



                RandomAccessFile fnode = new RandomAccessFile("./" + fname.replace("../", "./"), "r");


                if (fnode.getFD().valid()) {
                    FileChannel fc = fnode.getChannel();
                    SocketChannel channel = (SocketChannel) key.channel();
                    Xfer xfer = new Xfer(fc, fname);
                    response(key, $200);

                    try {
                        ByteBuffer buffer1 = ByteBuffer.allocateDirect(channel.socket().getSendBufferSize());
//
                        MimeType mimeType = null;
                        try {
                            mimeType = MimeType.valueOf(fname.substring(fname.lastIndexOf('.') + 1));
                        } catch (Exception ignored) {
                            throw new IOError(ignored);
                        }
                        String x = (mimeType == null ? "\r\n" : ("Content-Type: " + mimeType.contentType + "\r\n"));
                        CharBuffer c = (CharBuffer) buffer1.asCharBuffer().append("Connection: close\r\n" + x + "Content-Length: " + fc.size()).append("\r\n\r\n").flip();
                        channel.write(UTF8.encode(c));
                        key.interestOps(OP_WRITE);
                        key.attach(toArray(this, xfer));
                    } catch (Exception ignored) {    ignored.printStackTrace();
                    }
                    return;
                }
            } catch (Exception ignored) { ignored.printStackTrace();
            }
            try {
                response(key, $404);
                key.cancel();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        class Xfer {
            long progress;
            FileChannel fc;
            //            long creation = System.currentTimeMillis();
//            long completion = -1L;
            public CharSequence name;
//            public long chunk;
//            private boolean pipeline = false;

            private void sendChunk(SelectionKey key) {
                SocketChannel channel = null;
                if (fc.isOpen() && key.isValid() && key.channel().isOpen()) {
                    try {
                        SocketChannel channel1 = (SocketChannel) key.channel();
                        channel = channel1;
                        int sendBufferSize = channel1.socket().getSendBufferSize();

                        progress += this.fc.transferTo(progress, Math.min(getRemaining(), Math.min(getRemaining(), sendBufferSize)/*(++chunk) << 8*/), channel);
                        if (getRemaining() < 1) {
                            try {
                                fc.close();
                            } catch (IOException ignored) {
                            }
                            key.attach(null);
                            key.interestOps(OP_READ);//pipeline requests
                        }

                    } catch (Exception e) {
                        key.cancel();
                        try {
                            fc.close();
                        } catch (IOException ignored) {
                        }
                        fc = null;
                        try {
                            if (channel != null) {
                                channel.close();
                            }
                        } catch (IOException ignored) {
                        }
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
                return new StringBuilder().append(getClass().getName()).append(':').append(name).append(' ').append(progress).append('/').append(getRemaining());
            }


        }},

    POST, PUT, HEAD, DELETE, TRACE, CONNECT, OPTIONS, HELP, VERSION, $COUCHCONTROL {
        @Override
        public void onConnect(SelectionKey key) {
            CouchChangesClient.CouchControllerConnect(key);


        }

        /**
         *
         * @param key [method,ByteBuffer]
         */
        @Override
        void onWrite(SelectionKey key) {
            CouchChangesClient.CouchControllerWrite(key);
        }

        /**
         *
         * @param key attach={this,string,killswitch}
         */
        @Override
        public void onRead(SelectionKey key) {
            CouchChangesClient.CouchControllerRead(key);
        }

    }, $COUCHACCESS {

    },
    $ {
        public void onAccept(SelectionKey selectionKey) {
            if (selectionKey.isAcceptable()) {
                SocketChannel client = null;
                SelectionKey clientkey = null;

                try {
                    client = serverSocketChannel.accept();
                    client.configureBlocking(false).register(selector, OP_READ);
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

            Object[] att = (Object[]) key.attachment();

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
                    SocketChannel channel;
                    channel = (SocketChannel) key.channel();


                    try {
                        ByteBuffer buffer =

                                ByteBuffer.allocateDirect(channel.socket().getSendBufferSize());
                        int i = channel.read(buffer);

                        buffer.flip().mark();

                        for (HttpMethod httpMethod : HttpMethod.values())
                            if (httpMethod.recognize((ByteBuffer) buffer.reset())) {
                                //System.out.println("found: " + httpMethod);
                                key.attach(buffer);
                                httpMethod.onConnect(key);
                                return;
                            }

                        response(key, HttpStatus.$400);
                        channel.write(buffer);
                    } catch (Exception ignored) {
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

    private static CompletionException COMPLETION_EXCEPTION = new CompletionException();
//    private static int DEFAULT_EXP = 0;


    ByteBuffer token = (ByteBuffer) ByteBuffer.wrap(name().getBytes()).rewind().mark();


    int margin = name().length() + 1;
    public static Charset UTF8 = Charset.forName("UTF8");
    private static Selector selector;
    private static ConcurrentLinkedQueue<Object[]> q = new ConcurrentLinkedQueue<Object[]>();

    public static Selector getSelector() {
        return selector;
    }

    public static Object[] toArray(Object... t) {
        return t;
    }

    /**
     * handles the threadlocal ugliness if any to registering user threads into the selector/reactor pattern
     *
     * @param channel the socketchanel
     * @param op      int ChannelSelector.operator
     * @param s       the payload: grammar {enum,data1,data..n}
     * @throws ClosedChannelException
     */
    public static void enqueue(SocketChannel channel, int op, Object... s) throws ClosedChannelException {
        q.add(toArray(channel, op, s));
    }


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


        boolean isBlank = true, wasBlank;
        int prevIdx = 0;
        in.position(margin);
        char b = 0;
        while (b != '\n' && out.position() < margin) {
            wasBlank = isBlank;
            b = (char) (in.get() & 0xff);
            isBlank = isWhitespace(b & 0xff);

            if ((!isBlank) && wasBlank) {
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
        while ((b = indexEntries.get()) != 0 && (indexEntries.position() <= margin)) last = b & 0xff;

        int len = indexEntries.position();

        //this should be between 40 and 300 something....
        indexEntries.position(last);


        while (!Character.isISOControl(b = indexEntries.get() & 0xff)
                && !Character.isWhitespace(b)
                && ('\n' != b)
                && ('\r' != b)
                && ('\t' != b)
            ) ;

        return decoder.decode((ByteBuffer) indexEntries.flip().position(margin));

    }


    private static Random RANDOM = new Random();


    public void onRead(SelectionKey key) {
        Object o = key.attachment();
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

            SelectableChannel channel = key.channel();
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


        try {
            SocketChannel channel = (SocketChannel) key.channel();
            ByteBuffer buffer = ByteBuffer.allocateDirect(channel.socket().getSendBufferSize());
            CharBuffer charBuffer = (CharBuffer) buffer.asCharBuffer().append("HTTP/1.1 ").append(httpStatus.name().substring(1)).append(' ').append(httpStatus.caption).append("\r\n").flip();
            ByteBuffer out = UTF8.encode(charBuffer);


            ((SocketChannel) key.channel()).write(out);
        } catch (Exception ignored) {
        }

    }

    void onWrite(SelectionKey key) {
        throw new UnsupportedOperationException();
    }

    static Charset charset = UTF8;
    static CharsetDecoder decoder = charset.newDecoder();
    public static boolean killswitch = false;


    static ServerSocketChannel serverSocketChannel;

    private static int port = 8080;


    void onAccept(SelectionKey key) {
        throw new UnsupportedOperationException();
    }

    static public void setSelector(Selector selector) {
        HttpMethod.selector = selector;
    }


    static class CompletionException extends Throwable {
    }


    public static void main(final String... a) throws IOException {
        try {
            Selector selector1 = null;
            try {
                selector1 = Selector.open();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            setSelector(selector1);

            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.socket().bind(new java.net.InetSocketAddress(port));
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.register(selector1, SelectionKey.OP_ACCEPT);


        } catch (Throwable e11) {
            e11.printStackTrace();
        }
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (a) {
            while (!killswitch) {
                while (!q.isEmpty()) {
                    Object[] s = q.remove();
                    SocketChannel x = (SocketChannel) s[0];
                    Selector sel = getSelector();
                    Integer op = (Integer) s[1];
                    Object att = s[2];
                    x.register(sel, op, att);
                }
                selector.select(1000);
                Set<SelectionKey> keys = selector.selectedKeys();

                for (Iterator<SelectionKey> i = keys.iterator(); i.hasNext(); ) {
                    SelectionKey key = i.next();
                    i.remove();

                    if (key.isValid()) {
                        try {

                            HttpMethod m;
                            m = $;
                            Object attachment = key.attachment();
                            if (attachment instanceof Object[]) {
                                Object[] objects = (Object[]) attachment;
                                if (objects[0] instanceof HttpMethod) {
                                    m = (HttpMethod) objects[0];
                                }
                            }

                            if (key.isValid()&&key.isWritable()) {
                                m.onWrite(key);
                            }
                            if (key.isValid()&&key.isReadable()) {
                                m.onRead(key);
                            }
                            if (key.isValid()&&key.isAcceptable()) {
                                m.onAccept(key);
                            }
                            if (key.isValid()&&key.isConnectable()) {
                                m.onConnect(key);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            key.attach(null);
                            key.channel().close();
                        }
                    }
                }
            }
        }
    }

}