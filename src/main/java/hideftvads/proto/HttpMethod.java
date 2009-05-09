package hideftvads.proto;

import javolution.io.*;
import javolution.text.*;

import java.io.*;
import static java.lang.Character.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

/**
 * User: jim
 * Date: May 6, 2009
 * Time: 10:12:22 PM
 * <p/>
 * see http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html
 */
public enum HttpMethod implements Protocol {
    GET {

        public void onWrite(final SelectionKey key) {
            Object[] a = (Object[]) key.attachment();
            Xfer xfer = (Xfer) a[1];
            xfer.sendChunk(key, xfer);
        }

        /**
         * enrolls a new SelectionKey to the methods
         *
         * @param key
         * @throws IOException
         */
        @Override
        public void onConnect(final SelectionKey key) {

            try {
                assert key.attachment() instanceof ByteBuffer;
                final ByteBuffer buffer = (ByteBuffer) key.attachment();
                final CharSequence charSequence = methodParameters(buffer);
                final String[] strings = charSequence.toString().split(" ");
                final String fname = strings[0];
                final File fnode = new File("./" + fname);
                //      System.err.println("attempting to open file://" + fnode.getAbsolutePath());

                if (!fnode.canRead() || !fnode.isFile()) {

                    key.channel().close();
                } else {

                    RandomAccessFile f = new RandomAccessFile(fnode, "r");

                    final FileChannel fc = f.getChannel();

                    final SocketChannel channel = (SocketChannel) key.channel();

                    final Xfer xfer = new Xfer(fc, fname);
                    String s = "HTTP/1.1 200 OK\n" +
//                            "Content-Type: application/binary\n" +
                            "Connection: close\n" +
                            "Content-Length: " + fc.size() + "\n\n";

                    channel.write(ByteBuffer.wrap(s.getBytes()));

                    key.interestOps(SelectionKey.OP_WRITE);
                    key.attach(new Object[]{this, xfer});
                    return;
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();  //TODO: Verify for a purpose
            } catch (IOException e) {
                e.printStackTrace();  //TODO: Verify for a purpose
            }
            try {
                key.channel().close();
            } catch (IOException e) {
                e.printStackTrace();  //TODO: Verify for a purpose
            }
        }

        class Xfer {
            private void sendChunk(SelectionKey key, Xfer xfer) {
                long rem = 0;
                SocketChannel channel = null;
                try {

                    channel = (SocketChannel) key.channel();

                    final long progress = xfer.progress;
                    rem = xfer.getRemaining();

                    final long written = xfer.fc.transferTo(
                            progress, Math.min(rem, (++xfer.chunk) << 8)
                            , channel);


                    xfer.progress += written;

//                final CharSequence charSequence = xfer.logEntry();
//                System.err.println(charSequence);
//                System.err.flush();

                } catch (IOException e) {
                    e.printStackTrace();  //TODO: Verify for a purpose
                    key.cancel();
                    try {
                        key.channel().close();
                    } catch (IOException e1) {
                        e1.printStackTrace();   //TODO: Verify for a purpose
                    }
                }
                if (rem < 1) {
                    try {
                        fc.close();
                    } catch (IOException e) {
        //     e.printStackTrace();  //TODO: Verify for a purpose
                    }
                    try {
                        channel.close();
                    } catch (IOException e) {
        //     e.printStackTrace();  //TODO: Verify for a purpose
                    }
                }
            }

            long progress;
            FileChannel fc;
            long creation = System.currentTimeMillis();
            long completion = -1L;
            public CharSequence name;
            public long chunk;

            public Xfer(FileChannel fc, CharSequence name) {
                this.fc = fc;
                this.name = name;
            }

            long getRemaining() throws IOException {
                return fc.size() - progress;
            }


            public CharSequence logEntry() throws IOException {
                return new TextBuilder().append("Xfer: ").append(name).append(' ').append(progress).append('/').append(getRemaining());

            }
        }}, POST, PUT, HEAD, DELETE, TRACE, CONNECT, OPTIONS, HELP, VERSION;
    private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);


    final ByteBuffer token = (ByteBuffer) ByteBuffer.wrap(name().getBytes()).rewind().mark();


    final int margin = name().length() + 1;
    private static final UTF8ByteBufferReader UTF_8_BYTE_BUFFER_READER = new UTF8ByteBufferReader();


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
    @Override
    public void onConnect(SelectionKey key) {

        try {
            assert key.attachment() instanceof ByteBuffer;
            ByteBuffer buffer = (ByteBuffer) key.attachment();

            final CharSequence charSequence = methodParameters(buffer);
            throw new UnsupportedOperationException(name() + charSequence.toString());

        } catch (IOException e) {
            e.printStackTrace();  //TODO: Verify for a purpose
        } finally {

        }

    }

    @Override
    public void onWrite(SelectionKey key) {


    }

}



