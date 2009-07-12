package hideftvads.proto;

import alg.*;

import java.io.*;
import java.lang.ref.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

/**
 * User: jim
 * Date: Jul 12, 2009
 * Time: 3:56:55 AM
 */
public enum NntpClientLifeCycle {
    /**
     * the event loop -- default functor and command processing nexus
     */exec {
        /**
         * read a status code, update Session, replace self.
         */
        public void onRead(SelectionKey selectionKey) {
            final Reference<ByteBuffer> ref = ProtoUtil.borrowBuffer(ProtoUtil.DEFAULT_EXP);
            try {
                final ByteBuffer buffer = ref.get();

                final SocketChannel c = (SocketChannel) selectionKey.channel();
                int count = -1;
                do {
                    try {
                        count = c.read(buffer);
                        if (count > 0) {
                            System.err.println("read " + count);
                            final CharBuffer charBuffer = ProtoUtil.UTF8.decode((ByteBuffer) buffer.flip());
                            System.err.println(charBuffer);
                            if (charBuffer.toString().startsWith("200 ")) {
                                final int desiredOps = SelectionKey.OP_WRITE;
                                NntpClientLifeCycle desiredState = NntpClientLifeCycle.CAPABILITIES;


                                setLifecycle(selectionKey, desiredOps, desiredState);

                            }
                            if (charBuffer.toString().startsWith("480 ")) {
                                final int desiredOps = SelectionKey.OP_WRITE;
                                NntpClientLifeCycle desiredState = NntpClientLifeCycle.AUTHINFO$20USER;


                                setLifecycle(selectionKey, desiredOps, desiredState);

                            }
                            if (charBuffer.toString().startsWith("381 ")) {
                                final int desiredOps = SelectionKey.OP_WRITE;
                                NntpClientLifeCycle desiredState = NntpClientLifeCycle.AUTHINFO$20PASS;

                                setLifecycle(selectionKey, desiredOps, desiredState);

                            }
                            if (charBuffer.toString().startsWith("215 ")) {
                                final int desiredOps = SelectionKey.OP_READ;
                                NntpClientLifeCycle desiredState = NntpClientLifeCycle.LIST;
                                setLifecycle(selectionKey, desiredOps, desiredState);
                            }
                            if (charBuffer.toString().startsWith("281 ")) {
                                final int desiredOps = SelectionKey.OP_WRITE;
                                NntpClientLifeCycle desiredState = NntpClientLifeCycle.LIST;

                                setLifecycle(selectionKey, desiredOps, desiredState);
                            }

                            if (charBuffer.toString().startsWith("500 ")) {
//                                final int desiredOps = SelectionKey.OP_WRITE;
//                                NntpClientLifeCycle desiredState = NntpClientLifeCycle.VERSION;
//
//
//                                setLifecycle(selectionKey, desiredOps, desiredState);
//
//
//                            }
//                            if (charBuffer.toString().startsWith("503 ")) {
                                selectionKey.cancel();
                                Thread.currentThread().interrupt();

                            }
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } while (count > 0);
            } finally {
                ProtoUtil.recycle(ref, ProtoUtil.DEFAULT_EXP);
            }
        }
    }, LIST {
//        public void onWrite(SelectionKey k) {
//            final SocketChannel c = (SocketChannel) k.channel();
//            try {
//
//                int i = 0;
//                i += c.write((ByteBuffer) token.position(0));
        //                i += c.write((ByteBuffer) ProtoUtil.EOL.position(0));
        //                System.err.println("sent " + i + " " + header.position(0));
        //                setLifecycle(k, SelectionKey.OP_READ, LIST);
        //            } catch (IOException e) {
        //                e.printStackTrace();  //TODO: Verify for a purpose
        //            }
        //        }
        public void onRead(SelectionKey k) {
            try {

                final NntpSession x = (NntpSession) k.attachment();

                ByteBuffer buffer;
//                int ci;
                FileChannel channel;
                RandomAccessFile randomAccessFile;
                if (x.groups != null) {
                    buffer = x.groups;
                    channel = x.gchannel;
                    randomAccessFile = x.gfile;

                } else {
                    final File tempFile = File.createTempFile("1xio", ".tmp");
                    tempFile.deleteOnExit();
                    tempFile.delete();

                    final boolean b = tempFile.createNewFile();
                    randomAccessFile = new RandomAccessFile(tempFile.getAbsoluteFile(), "rw");
                    randomAccessFile.setLength(GRPLEN);

                    channel = randomAccessFile.getChannel();
                    buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, GRPLEN);
                     x.gfile = randomAccessFile;
                    x.gchannel = channel;
                    x.groups = buffer;
                }
                long l = 0;
                boolean once = false;
//                do {
                x.gcursor += l = channel.transferFrom((ReadableByteChannel) k.channel(), x.gcursor, GRPLEN - x.gcursor);
//                x.gcursor = (int) ci;

                if (l > 0) {
                    final int ci = x.gcursor;
                    System.err.println("read " + l + "@" + ci + ":" + buffer.get((int) (ci - 4))
                            + ":" + buffer.get((int) (ci - 3))
                            + ":" + buffer.get((int) (ci - 2))
                            + ":" + buffer.get((int) (ci - 1))
                            + ":" + buffer.get((int) (ci))
                    );

                }
                if (
                        buffer.get((int) ( x.gcursor  - 4)) == '\n' &&
                                buffer.get((int) ( x.gcursor -3)) == '.' &&
                                buffer.get((int) ( x.gcursor -2)) == '\r' &&
                                buffer.get((int) ( x.gcursor -1)) == '\n'  
                                                                                                                                        ){
                    x.gfile .setLength(  x.gcursor );
                    setLifecycle(k, SelectionKey.OP_READ, exec);
                    System.err.println("total " +  x.gcursor );
                    //
                    final LinkedList<Pair<Integer, LinkedList<Integer>>> list = ProtoUtil.preIndex(buffer);

                    final Iterator<Pair<Integer, LinkedList<Integer>>> pairIterator = list.descendingIterator();


                    return;
                }


            } catch (IOException e) {
                e.printStackTrace();  //TODO: Verify for a purpose
            }


        }

    }, AUTHINFO$20USER {
        public void onWrite(SelectionKey k) {
            final SocketChannel c = (SocketChannel) k.channel();
            try {

                int i = 0;
                i += c.write((ByteBuffer) token.position(0));
                i += c.write((ByteBuffer) ByteBuffer.wrap(WS).position(0));
                i += c.write(ProtoUtil.UTF8.encode(((NntpSession) k.attachment()).AUTHINFO$20USER));
                i += c.write((ByteBuffer) ProtoUtil.EOL.position(0));
                System.err.println("sent " + i + " " + header.position(0) + " " + "****");
                setLifecycle(k, SelectionKey.OP_READ, exec);
            } catch (IOException e) {
                e.printStackTrace();  //TODO: Verify for a purpose
            }

        }}, AUTHINFO$20PASS {
        public void onWrite(SelectionKey k) {
            final SocketChannel c = (SocketChannel) k.channel();
            try {

                int i = 0;
                i += c.write(token);
                i += c.write((ByteBuffer) ByteBuffer.wrap(WS).position(0));
                i += c.write(ProtoUtil.UTF8.encode(((NntpSession) k.attachment()).AUTHINFO$20PASS));
                i += c.write((ByteBuffer) ProtoUtil.EOL.position(0));
                System.err.println("sent " + i + " " + header.position(0) + " " + "****");
                setLifecycle(k, SelectionKey.OP_READ, exec);
            } catch (IOException e) {
                e.printStackTrace();  //TODO: Verify for a purpose
            }

        }}, READY, VERSION, CAPABILITIES;

    private static final int GRPLEN = 1 << 24L;
    private static final byte[] WS = new byte[]{' '};

    private static void setLifecycle(SelectionKey selectionKey, int desiredOps, NntpClientLifeCycle desiredState) {
        NntpSession nntpSession = (NntpSession) selectionKey.attachment();

        nntpSession.lifecyle = desiredState;
        selectionKey.interestOps(desiredOps);
    }

    public void onConnect(SelectionKey selectionKey) {

        boolean ready = false;
        try {
            ready = ((SocketChannel) selectionKey.
                    channel())
                    .finishConnect();
        } catch (IOException e) {
            e.printStackTrace();  //TODO: Verify for a purpose
        }
        if (ready) {
            selectionKey.interestOps(SelectionKey.OP_READ);

            System.err.println("connected");
        }

    }

    public void onRead(SelectionKey selectionKey) {
    }

    public void onWrite(SelectionKey k) {
        final SocketChannel c = (SocketChannel) k.channel();
        try {

            int i = 0;
            i += c.write((ByteBuffer) token.position(0));
            i += c.write((ByteBuffer) ProtoUtil.EOL.position(0));
            System.err.println("sent " + i + " " + header.position(0) + " " + "****");
            setLifecycle(k, SelectionKey.OP_READ, exec);
        } catch (IOException e) {
            e.printStackTrace();  //TODO: Verify for a purpose
        }

    }

    /**
     *
     */
    final CharBuffer header = CharBuffer.wrap(URLDecoder.decode(name().replace('$', '%')));
    final ByteBuffer token = ProtoUtil.UTF8.encode(header);
    final int tokenLen = token.limit();

    boolean recognize(ByteBuffer buffer) {
        final int i = buffer.position();
        if ((buffer.get(tokenLen + i) & 0xff) == ':') {
            int j;

            for (j = 0; j < tokenLen && token.get(j) == buffer.get(i + j); j++) ;
            return tokenLen == j;
        }
        return false;
    }
}
