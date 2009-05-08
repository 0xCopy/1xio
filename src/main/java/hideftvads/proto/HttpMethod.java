package hideftvads.proto;

import javolution.context.*;
import javolution.io.*;
import javolution.text.*;

import java.io.*;
import static java.lang.Character.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * User: jim
 * Date: May 6, 2009
 * Time: 10:12:22 PM
 * <p/>
 * see http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html
 */
public enum HttpMethod implements Protocol {
    GET {

        @Override
        public void onWrite(SelectionKey key) {

            final Object o = key.attachment();

        }

        /**
         * enrolls a new SelectionKey to the methods
         *
         * @param key
         * @throws IOException
         */
        @Override
        public void onConnect(final SelectionKey key) throws IOException, IOError {
            PoolContext.enter();
            try {
                final Object o = key.attachment();
                assert o instanceof ByteBuffer;
                ByteBuffer buffer = (ByteBuffer) o;
                final CharSequence charSequence = methodParameters(buffer);
                final String[] strings = charSequence.toString().split(" ");
                final String fname = strings[0];
                final File fnode = new File("./" + fname);
                System.err.println("attempting to open file://" + fnode.getAbsolutePath()
                );

                if (!fnode.canRead() || !fnode.isFile()) {
                    throw new Error("failure to read on :" + fnode.getAbsolutePath());
                } else {

                    RandomAccessFile f = new RandomAccessFile(fnode, "r");


                    final long fileLength = f.length();
//                    final long progres = 0;
                    final FileChannel fc = f.getChannel();

//                    fc.transferTo(0,fileLength, (WritableByteChannel) key.channel());

                    final SelectableChannel channel = key.channel();


                    final Callable<Long> callable = new Callable<Long>() {
                        public long progres;

                        private long getRemaining() {
                            return fileLength - progres;
                        }

                        public boolean done;

                        @Override
                        public Long call() throws Exception {

                            try {

                                final long remaining = fileLength - progres;
                                if (remaining == 0) done = true;
                                progres += fc.transferTo(progres, remaining, (WritableByteChannel) channel);

                                return progres;
                            } catch (IOException e) {
                                e.printStackTrace();  //TODO: Verify for a purpose
                                return -1L;
                            }
                        }
                    };


                    final Future<Long> future;
                    future = new Future<Long>() {
                        public boolean canceled;

                        @Override
                        public boolean cancel(boolean b) {

                            key.cancel();  //ToDo: verify for a purpose
                            try {
                                channel.close();
                            } catch (IOException e) {
                                e.printStackTrace();  //TODO: Verify for a purpose
                            }
                            try {
                                fc.close();
                            } catch (IOException e) {
                                e.printStackTrace();  //TODO: Verify for a purpose
                            }
                            canceled = true;
                            return true;
                        }

                        @Override
                        public boolean isCancelled() {
                            return canceled;  //ToDo: verify for a purpose
                        }

                        /**
                         * same as calling get
                         *
                         * @return
                         */
                        @Override
                        public boolean isDone() {
                            try {
                                return callable.call() == fileLength;
                            } catch (Exception e) {
                                e.printStackTrace();
                                cancel(true);
                            }
                            return false;
                        }


                        @Override
                        public Long get() throws InterruptedException, ExecutionException {
                            try {
                                return callable.call();
                            } catch (Exception e) {
                                e.printStackTrace();  //TODO: Verify for a purpose
                                cancel(true);
                            }
                            return -1L;
                        }

                        @Override
                        public Long get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {

                            throw new UnsupportedOperationException(fname);
                        }
                    };
                    channel.register(selector, SelectionKey.OP_WRITE, future);

                }
            } finally {
                PoolContext.exit();
            }

        }
    }, POST, PUT, HEAD, DELETE, TRACE, CONNECT, OPTIONS, HELP, VERSION;


    final ByteBuffer token = (ByteBuffer) ByteBuffer.wrap(name().getBytes()).rewind().mark();


    final int margin = name().length() + 1;
    private static final UTF8ByteBufferReader UTF_8_BYTE_BUFFER_READER = new UTF8ByteBufferReader();

    HttpMethod() {
        try {
            selector = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();  //TODO: Verify for a purpose
        }
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

        final ByteBuffer charBuffer = ((ByteBuffer) indexEntries.duplicate().flip().position(margin)).slice();

        TextBuilder builder = TextBuilder.newInstance();
        final UTF8ByteBufferReader utf8ByteBufferReader = new UTF8ByteBufferReader().setInput(charBuffer);
        utf8ByteBufferReader.read(builder);

        return builder;
    }


    public Selector
            selector;
    ;

    {
        final Runnable initThread = new Runnable() {
            @Override
            public void run() {
                PoolContext.enter();
                try

                {

                    System.err.println("initializing selector for " + name());

                    while (!ProtocolImpl.killswitch) {
                        final int num = selector.select();
                        if (num > 0) {
                            final Set<SelectionKey> selectionKeySet = selector.selectedKeys();


                            final Iterator<SelectionKey> keyIterator = selectionKeySet.iterator();
                            while (keyIterator.hasNext()) {
                                SelectionKey selectionKey = keyIterator.next();
                                if (
                                        selectionKey.isValid() && selectionKey.isReadable()) {
                                    onRead(selectionKey);
                                }

                                if (
                                        selectionKey.isValid() && selectionKey.isWritable()) {
                                    onWrite(selectionKey);
                                }
                            }
                        }
                    }
                }
                catch (IOException e) {
                    e.printStackTrace();  //TODO: Verify for a purpose
                }
                finally {
                    PoolContext.exit();
                }
            }
        };
        final Thread thread = new Thread(initThread);
        thread.setDaemon(true);
        thread.setName("HidefTVAds! - " + name());
        thread.start();
    }

    @Override
    public void onWrite(SelectionKey key) {


    }

    @Override
    public void onRead(SelectionKey key) throws IOException {
        final Object o = key.attachment();
        if (o instanceof ByteBuffer) {
            this.tokenize((ByteBuffer) o);
        }
    }

    @Override
    public void onEnd(SelectionKey key, SocketChannel client) throws IOException {

    }

    /**
     * enrolls a new SelectionKey to the methods
     *
     * @param key
     * @throws IOException
     */
    @Override
    public void onConnect(SelectionKey key) throws IOException, IOError {
        PoolContext.enter();
        try {
            final Object o = key.attachment();
            assert o instanceof ByteBuffer;
            ByteBuffer buffer = (ByteBuffer) o;

            final CharSequence charSequence = methodParameters(buffer);
            throw new UnsupportedOperationException(name() + charSequence.toString());

        } finally {
            PoolContext.exit();
        }

    }
}



