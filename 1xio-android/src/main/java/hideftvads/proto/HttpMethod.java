package hideftvads.proto;

import alg.*;
import ds.tree.*;
import static hideftvads.proto.HttpStatus.*;
import javolution.text.*;

import java.io.*;
import static java.lang.Character.*;
import java.lang.ref.*;
import java.net.*;
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
        /**
         * we are a proxy
         *
         * @param key
         */
        @Override
        public void onConnect(final SelectionKey key) {
            final SocketChannel channel = (SocketChannel) key.channel();
            try {
                if (channel.finishConnect()) {

                    final Object[] o = (Object[]) key.attachment();
                    final HttpMethod o1 = (HttpMethod) o[0];
                    final RadixTree tree = (RadixTree) o[1];
                    final ByteBuffer src = (ByteBuffer) o[2];
                    LinkedList<Pair<Integer, LinkedList<Integer>>> lines = (LinkedList<Pair<Integer, LinkedList<Integer>>>) o[3];

                    final CharBuffer buffer = extractUri(src, lines);
                    final String str = buffer.toString();
                    final URI uri = new URI(str);

                    final String path = uri.normalize().getPath();
                    final String query = uri.getQuery();
                    final String fragment = uri.getFragment();

                    final MethodExtract methodExtract = new MethodExtract(o, path, query, fragment).invoke();
                    Text r = methodExtract.getR();

                    System.err.println(":::"+r);
                }
            } catch (Exception e) {
                key.cancel();
                e.printStackTrace();
            }

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


                final LinkedList<Pair<Integer, LinkedList<Integer>>> lines = preIndex(src);

                final int fnend = lines.get(0).$2().get(1) - 1;
                final Text path = Text.intern(UTF8.decode((ByteBuffer) src.limit(fnend).position(margin)));
                final RadixTree<Pair<Integer, Integer>> requestMap = new RadixTreeImpl<Pair<Integer, Integer>>();
                RadixTree<CharBuffer> displayOnlyTree = new RadixTreeImpl<CharBuffer>();

                for (int i = 1; i < lines.size(); i++) {
                    Pair<Integer, LinkedList<Integer>> line = lines.get(i);
                    LinkedList<Integer> tokens = line.$2();
                    if (null != tokens && !tokens.isEmpty()) {
                        final Integer newLimit = tokens.getFirst();
                        final Integer position = line.$1();
                        Text rkey = Text.valueOf(UTF8.decode((ByteBuffer) src.limit(newLimit - 2).position(position)));
                        requestMap.insert(rkey, new Pair<Integer, Integer>(newLimit + 1, tokens.getLast() - 1));
                        displayOnlyTree.insert(rkey, UTF8.decode((ByteBuffer) src.limit(tokens.getLast() - 1).position(newLimit)));
//
//                        requestMap.display();
//                        displayOnlyTree.display();
//                        final String s = Tuple.XSTREAM.toXML(requestMap);
//                        System.err.println(s);
//
//                        System.out.println("================");
                    }
                }

                if (path.startsWith(HTTP_PREFIX)) {
                    final URI uri = URI.create(path.toString()).normalize();

                    final String host = uri.getHost();
                    final int port = uri.getPort();
                    SocketChannel sc = SocketChannel.open();
                    sc.configureBlocking(false);
                    sc.socket().setTcpNoDelay(true);
                    sc.connect(new InetSocketAddress(host, port));
                    final SelectionKey proxy = sc.register(key.selector(), SelectionKey.OP_CONNECT);
                    key.attach(new Object[]{this, requestMap, src, proxy});


                } else {
                    try {

                        String name1 = path.toString();
                        if(name1.charAt(0)=='/')name1=name1.substring(1);
                        name1.replaceAll("/..","/.");
                        
                        final RandomAccessFile fnode = new RandomAccessFile(name1, "r");

                        if (fnode.getFD().valid()) {
                            final FileChannel fc = fnode.getChannel();
                            final SocketChannel channel = (SocketChannel) key.channel();
                            final Xfer xfer = new Xfer(fc, path);
                            response(key, $200);
                            final Reference<ByteBuffer> byteBufferReference = HttpMethod.borrowBuffer(DEFAULT_EXP);
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
                                recycle(byteBufferReference, DEFAULT_EXP);
                            }
                            return;
                        }
                    } catch (Exception  e) {
//                        e.printStackTrace();
                    }
                }
                try {
                    response(key, $404);
                    key.cancel();
                } catch (IOException e) {
//                    e.printStackTrace();
                }
            } catch (DuplicateKeyException ignored) {
            } catch (IOException e) {
//                e.printStackTrace();  //TODO: verify for a purpose
            }

        }

        class Xfer {
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
                                        key.interestOps(SelectionKey.OP_READ);
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


            public Xfer(FileChannel fc, CharSequence name) {
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


            RadixTree tree = (RadixTree) o[1];
            ByteBuffer request = (ByteBuffer) o[2];
            LinkedList<Pair<Integer, LinkedList<Integer>>> lines = (LinkedList<Pair<Integer, LinkedList<Integer>>>) o[3];
            SocketChannel c = (SocketChannel) k.channel();


        }}, OPTIONS, HELP, VERSION,
    $ {

        public void onAccept(SelectionKey selectionKey) {
            if (selectionKey.isAcceptable()) {
                SocketChannel client = null;

                try {
                    final ServerSocketChannel socketChannel = (ServerSocketChannel) selectionKey.channel();

                    client = socketChannel.accept();
                    client.configureBlocking(false).register(selectionKey.selector(), SelectionKey.OP_READ);
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

                    byteBufferReference = HttpMethod.borrowBuffer(DEFAULT_EXP);
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
                    } catch (Exception ignored) {
                    } finally {
                        recycle(byteBufferReference, DEFAULT_EXP);
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

    private static CharBuffer extractUri(ByteBuffer src, LinkedList<Pair<Integer, LinkedList<Integer>>> lines) {
        final Integer seek = lines.getFirst().$2().get(1) + 1;
        int limit = lines.getFirst().$2().get(2) - 1;
        src.limit(limit).position(seek);
        final CharBuffer buffer = UTF8.decode(src);
        return buffer;
    }

    private static final String HTTP_PREFIX = "http://".intern();

    private static LinkedList<Pair<Integer, LinkedList<Integer>>> preIndex(ByteBuffer src) {
        LinkedList<Pair<Integer, LinkedList<Integer>>> lines = new LinkedList<Pair<Integer, LinkedList<Integer>>>();
        final int pos = src.position();
        lines.add(new Pair<Integer, LinkedList<Integer>>(pos, new LinkedList<Integer>()));

        byte prev = 0;

        L1:


        while (src.hasRemaining()) {
            byte b = src.get();

            switch (b) {
                case EOL:
                    if (prev == EOL) {
                        break L1;
                    }
                    lines.add(new Pair<Integer, LinkedList<Integer>>(src.position(), new LinkedList<Integer>()));


                    break;
                case '\r':
                default:
                    if (!isWhitespace(b) && '\r' != b || isWhitespace(prev)) {
                    } else {
                        lines.getLast().$2().add(src.position());
                    }
                    break;
            }
            prev = b;
        }
        return lines;
    }

    private static final int EOL = '\n' & 0xff;


    private static final int DEFAULT_EXP = 0;
    final ByteBuffer token = (ByteBuffer) ByteBuffer.wrap(name().getBytes()).rewind().mark();


    final int margin = name().length() + 1;
    static final Charset UTF8 = Charset.forName("UTF8");
    private static final byte[] FIREFOX_ENDLINE = new byte[]{'\r', '\n',
            0};


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


        final Reference<ByteBuffer> byteBufferReference = HttpMethod.borrowBuffer(DEFAULT_EXP);
        try {
            final ByteBuffer buffer = byteBufferReference.get();
            final CharBuffer charBuffer = (CharBuffer) buffer.asCharBuffer().append("HTTP/1.1 ").append(httpStatus.name().substring(1)).append(' ').append(httpStatus.caption).append('\n').flip();

            final ByteBuffer out = UTF8.encode(charBuffer);


            ((SocketChannel) key.channel()).write(out);
        } catch (Exception ignored) {
        } finally {
            recycle(byteBufferReference, DEFAULT_EXP);
        }

    }

    public void onWrite(SelectionKey key) {
        throw new UnsupportedOperationException();
    }

    final static Charset charset = UTF8;
    final static CharsetEncoder charsetEncoder = charset.newEncoder();
    final static CharsetDecoder decoder = charset.newDecoder();


    public static ExecutorService threadPool = Executors.newCachedThreadPool();
    public static final int CHUNKDEFAULT = 4;
    public static final int CHUNK_NUM = 128;
    public static final int KBYTE = 1024;
    private static final int MAX_EXP = 16;


    public void onAccept(SelectionKey key) {
        throw new UnsupportedOperationException();
    }


    static Reference<ByteBuffer> borrowBuffer(int... exp) {

        final int slot = exp.length == 0
                ? DEFAULT_EXP
                : exp[0];

        if (exp.length > 1)
            System.out.println("heap " + slot + " count " + counter[slot]);


        final Queue<Reference<ByteBuffer>> buffer = buffers[slot];
        Reference<ByteBuffer> o;


        if (buffer.isEmpty()) {
            refill(slot);
            o = borrowBuffer(slot, counter[slot]++);
        } else {
            o = buffer.remove();
        }
        minus();
        return o.get() == null ? borrowBuffer(exp) : o;
    }

    private static void minus() {
        //System.out.write('-');
    }


    static synchronized private void refill(final int slot) {
        Queue<Reference<ByteBuffer>> queue = buffers[slot];
        if (queue == null) {
            queue = buffers[slot] = new ConcurrentLinkedQueue<Reference<ByteBuffer>>();
        }

        if (queue.isEmpty()) {

            final int czize = KBYTE << slot;
            final ByteBuffer buffer = ByteBuffer.allocateDirect(czize * CHUNK_NUM);

            for (int i = 0; i < CHUNK_NUM; i++) {
                final int i2 = buffer.position();
                final int newPosition = i2 + czize;

                buffer.limit(newPosition);

                queue.add(new SoftReference<ByteBuffer>(buffer.slice()));
                plus();
                buffer.position(newPosition);
            }
            //  System.out.flush();
        }

    }

    final
    private static Queue<Reference<ByteBuffer>>[] buffers = (Queue<Reference<ByteBuffer>>[]) new Queue<?>[MAX_EXP];

    static {
        for (int i = 0; i < buffers.length; i++)
            buffers[i] = new ConcurrentLinkedQueue<Reference<ByteBuffer>>();


    }

    //    static int counter[]=new int[MAX_EXP];

    static void recycle(Reference<ByteBuffer> byteBufferReference, int shift) {
        final ByteBuffer buffer = byteBufferReference.get();
        if (buffer != null) {
            buffer.clear();
            buffers[shift].add(byteBufferReference);
            plus();
        }
    }

    private static void plus() {
//        System.out.write('+');
    }


    private static int[] counter = new int[MAX_EXP];

    private static class Rfc822Key implements Callable<Text> {
        private final ByteBuffer src;
        private int pos;

        /**
         * this assumes buffer beginning of line is marked!
         */
        public Rfc822Key(Pair<Integer, ByteBuffer> p) {
            this.pos = (int) p.$1();
            this.src = (ByteBuffer) ((ByteBuffer) p.$2()).duplicate().position(pos);
        }

        public Text call() throws Exception {
            return Text.intern(UTF8.decode(src).toString());

        }
    }

    public static class MethodExtract {
        private Object[] o;
        private String path;
        private String query;
        private String fragment;
        private Text r;

        public MethodExtract(Object[] o, String path, String query, String fragment) {
            this.o = o;
            this.path = path;
            this.query = query;
            this.fragment = fragment;
        }

        public Text getR() {
            return r;
        }

        public MethodExtract invoke() {
            r = Text.valueOf(path);
            if (query != null && !(query.length()==0)) {
                r.plus("?").plus(query);
            }
            if (fragment != null && !(fragment.length()==0)) {
                r.plus("#").plus(fragment);
            }

            if (r.isBlank()) {
                return this;
            }
            r = Text.valueOf("GET ").plus(r).plus(" HTTP/1.1\r\n");

            final ArrayList arrayList = new ArrayList(Arrays.asList(o));
            arrayList.add(r);


            return this;
        }
    }
};