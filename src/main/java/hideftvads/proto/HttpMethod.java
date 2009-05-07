package hideftvads.proto;

import javolution.context.*;
import javolution.io.*;

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


        @Override
        public void onWrite(SelectionKey key, SocketChannel client) {
                throw new Error("it works")     ;
        }
        /** requires a buffer sent over for context)
         * 
         * @param key
         * @throws IOException
         */
        @Override
        public void onRead(SelectionKey key) throws IOException {
            
            final Object o = key.attachment();
            if(! (o instanceof ByteBuffer)) throw new Error("not a ByteBuffer in GET key");
            
            final SelectableChannel channel = key.channel();
            final int i = key.interestOps();
            key.cancel();
            try {
                final SelectionKey selectionKey = channel.register(  selector, SelectionKey.OP_WRITE);
            } catch (ClosedChannelException e) {
                e.printStackTrace();  //TODO: Verify for a purpose
            } finally {
            }

            if(o instanceof ByteBuffer)
            {
                final ByteBuffer buffer = this.tokenize((ByteBuffer) o);
            }            
        }
        @Override
        public void onEnd(SelectionKey key, SocketChannel client) throws IOException {

        }
        @Override
        public void onConnect(SelectionKey key) throws IOException {

        }

        public void start(SelectionKey key, SocketChannel socketChannel) {
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


    /**
     * this is a util method which takes a byte buffer and prints the indexed pointers from 0
     * <p/>
     * please note this is designed to be as stateless and NIO-neutral as possible.
     *
     * @param indexEntries
     * @param httpMethod
     * @param index
     */
    public Iterator<String> decodeTokens(ByteBuffer indexEntries, HttpMethod httpMethod, int index) throws IOException {
        int d;
        ByteBuffer in = null;
        while ((indexEntries.position() <= httpMethod.margin) && (0 != (index = 0xff & indexEntries.get()))) {
            in = indexEntries.duplicate();

            in.position(index);

            final StringBuilder report = new StringBuilder(httpMethod.name() + ": index:" + index + " char:");
            System.out.print(report);

            while (!Character.isWhitespace(d = in.get() & 0xff))
                System.out.write(d);

            System.out.write('\n');
        }

        final Buffer buffer = indexEntries.position(margin + 1).mark();
        assert in != null;
        final ByteBuffer byteBuffer = (ByteBuffer) buffer.limit(in.position());

        final UTF8ByteBufferReader utf8ByteBufferReader = new UTF8ByteBufferReader();
        utf8ByteBufferReader.setInput(byteBuffer.slice());

        final StringWriter writer = new StringWriter();
        utf8ByteBufferReader.read(writer);

        System.err.println("UTF8Output is " + writer.toString());
        return null;
    }


    public Selector
            selector;;

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
                        if(num>0)
                        {
                            final Set<SelectionKey> selectionKeySet = selector.selectedKeys();


                            final Iterator<SelectionKey> keyIterator = selectionKeySet.iterator();
                            while (keyIterator.hasNext()) {
                                SelectionKey selectionKey = keyIterator.next();
                                
                                
                                if(
                                selectionKey.isValid()&&selectionKey.isReadable()){
                                    onRead(selectionKey);
                                };
                                
                                if(
                                selectionKey.isValid()&&selectionKey.isWritable()){
                                    onWrite(selectionKey);
                                };
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
    public void onWrite(SelectionKey key, SocketChannel client) {

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

    @Override
    public void onConnect(SelectionKey key) throws IOException {

    }
}



