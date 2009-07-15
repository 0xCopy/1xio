package hideftvads.proto;

import alg.*;
import sun.misc.*;

import java.io.*;
import java.lang.ref.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;

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
                            if (charBuffer.toString().startsWith("222 ")) {
                                final int desiredOps = SelectionKey.OP_READ;
                                NntpClientLifeCycle desiredState = NntpClientLifeCycle.BODY;
                                setLifecycle(selectionKey, desiredOps, desiredState);
                            }

                            if (charBuffer.toString().startsWith("215 ")) {
                                final int desiredOps = SelectionKey.OP_READ;
                                NntpClientLifeCycle desiredState = NntpClientLifeCycle.LIST;
                                setLifecycle(selectionKey, desiredOps, desiredState);
                            }
                            if (charBuffer.toString().startsWith("281 ")) {
                                final int desiredOps = SelectionKey.OP_WRITE;
                                NntpClientLifeCycle desiredState = NntpClientLifeCycle.BODY;

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

        public void onRead(SelectionKey k) {
            try {

                final NntpSession x = (NntpSession) k.attachment();

                ByteBuffer buffer;
//                int ci;
                FileChannel channel;
                RandomAccessFile randomAccessFile;
                if (x.LIST != null) {
                    buffer = x.LIST;
                    channel = x.gchannel;
                    randomAccessFile = x.gfile;

                } else {
                    final File tempFile = File.createTempFile("1xio", ".groups");
                    tempFile.deleteOnExit();
                    tempFile.delete();

                    final boolean b = tempFile.createNewFile();
                    randomAccessFile = new RandomAccessFile(tempFile.getAbsoluteFile(), "rw");
                    randomAccessFile.setLength(GRPLEN);

                    channel = randomAccessFile.getChannel();
                    buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, GRPLEN);
                    x.gfile = randomAccessFile;
                    x.gchannel = channel;
                    x.LIST = buffer;
                }
                long l = 0;
                boolean once = false;
//                do {
                x.gcursor += l = channel.transferFrom((ReadableByteChannel) k.channel(), x.gcursor, GRPLEN - x.gcursor);
//                x.gcursor = (int) ci;

                if (l > 0) {
                    final int ci = x.gcursor;
                    if (ci % 50 == 0)
                        System.err.println("read " + l + "@" + ci + ":" + buffer.get(ci - 4)
                                + ":" + buffer.get(ci - 3)
                                + ":" + buffer.get(ci - 2)
                                + ":" + buffer.get(ci - 1)
                                + ":" + buffer.get(ci)
                        );

                }
                if (
                        buffer.get(x.gcursor - 4) == '\n' &&
                                buffer.get(x.gcursor - 3) == '.' &&
                                buffer.get(x.gcursor - 2) == '\r' &&
                                buffer.get(x.gcursor - 1) == '\n'
                        ) {
                    try {
                        buffer = null;
                        x.gfile.setLength(x.gcursor);
                        x.LIST = x.gchannel.map(FileChannel.MapMode.READ_ONLY, 0, x.gcursor);
                        setLifecycle(k, SelectionKey.OP_READ, exec);
                        System.err.println("total " + x.gcursor);
                        //
                        final LinkedList<Pair<Integer, LinkedList<Integer>>> list = ProtoUtil.preIndex(x.LIST);
                        x.groupList = new CopyOnWriteArrayList<Pair<Integer, LinkedList<Integer>>>(list.toArray(new Pair[0]));
                    } catch (IOException e) {
                        e.printStackTrace();  //TODO: Verify for a purpose
                    }


                    System.err.println(Arrays.toString(x.groupList.toArray()));
                    x.LIST.clear();
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

        }}, READY, VERSION, CAPABILITIES, BODY {


        public void onRead(SelectionKey k) {
            try {

                final NntpSession x = (NntpSession) k.attachment();

                ByteBuffer buffer;
//                int ci;
                FileChannel channel;
                RandomAccessFile randomAccessFile;
                if (x.LIST != null) {
                    buffer = x.LIST;
                    channel = x.bodyChannel;
                    randomAccessFile = x.bodyFile;

                } else {
                    final File tempFile = File.createTempFile("1xio", ".body");
                    tempFile.deleteOnExit();
                    tempFile.delete();

                    final boolean b = tempFile.createNewFile();
                    randomAccessFile = new RandomAccessFile(tempFile.getAbsoluteFile(), "rw");
                    randomAccessFile.setLength(BLEN);

                    channel = randomAccessFile.getChannel();
                    buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, BLEN);
                    x.bodyFile = randomAccessFile;
                    x.bodyChannel = channel;
                    x.LIST = buffer;
                }
                long l = 0;
                x.bodyCursor += l = channel.transferFrom((ReadableByteChannel) k.channel(), x.bodyCursor, BLEN - x.bodyCursor);

                if (l > 0) {
                    final int ci = (int) x.bodyCursor;
                    if (ci % 50 == 0)
                        System.err.println("read " + l + "@" + ci + ":" + buffer.get(ci - 4)
                                + ":" + buffer.get(ci - 3)
                                + ":" + buffer.get(ci - 2)
                                + ":" + buffer.get(ci - 1)
                                + ":" + buffer.get(ci)
                        );
                }
                if (
                        buffer.get((int) (x.bodyCursor - 4)) == '\n' &&
                                buffer.get((int) (x.bodyCursor - 3)) == '.' &&
                                buffer.get((int) (x.bodyCursor - 2)) == '\r' &&
                                buffer.get((int) (x.bodyCursor - 1)) == '\n'
                        ) {
                    try {
                       
                        x.bodyFile.setLength(x.bodyCursor);
                      buffer=  x.BODY = x.bodyChannel.map(FileChannel.MapMode.PRIVATE, 0, x.bodyCursor);
                        setLifecycle(k, SelectionKey.OP_READ, exec);
                        System.err.println("total " + x.bodyCursor);
                        //
                        final LinkedList<Pair<Integer, LinkedList<Integer>>> list = ProtoUtil.preIndex(x.BODY);

                        x.bodyLines = new CopyOnWriteArrayList<Pair<Integer, LinkedList<Integer>>>(list.toArray(new Pair[0]));
                    } catch (IOException e) {
                        e.printStackTrace();  //TODO: Verify for a purpose
                    }


//                System.err.println(Arrays.toString(x.bodyLines.toArray()));
                    x.BODY.clear();
                    boolean trigger = false;
                    for (Pair<Integer, LinkedList<Integer>> bLine : x.bodyLines) {

                        if (trigger) {
                            x.BODY.clear();
/*
                            x.BODY.position(bLine.$1());
                            x.BODY.compact();
*/
                            break;

                        } else {
                            if (bLine.$2().isEmpty()) {
                                continue;
                            }
                            final Integer integer = bLine.$1();
                            final Integer integer1 = bLine.$2().getFirst();
                            if (BEGIN.limit() == integer1 - integer) {
                                final ByteBuffer buffer1 = (ByteBuffer) x.BODY.limit(integer1).position(integer);
                                final int test = buffer1.compareTo((ByteBuffer) BEGIN.position(0));

                                if (test == 0) {
                                    trigger = true;
                                    try {
                                        x.BODY.limit(bLine.$2().get(2)-1).position(bLine.$2().get(1));

                                        x.outputName = ProtoUtil.UTF8.decode(x.BODY).toString();
                                        System.err.println("outputName " + x.outputName);
                                    } catch (Exception e) {
                                        e.printStackTrace();  //TODO: Verify for a purpose
                                    }
                                }
                            }
                        }
                    }
                    if (trigger) {

                        final Integer integer = x.bodyLines.get(x.bodyLines.size() - 2).$1();
                        final CharBuffer charBuffer = ProtoUtil.UTF8.decode(((ByteBuffer) x.BODY.position(integer)));
                        System.err.println("last  " + integer + "  " + charBuffer);
                        final byte[] bytes;
                        try {
                            x.BODY.rewind();
                            final boolean b = x.BODY.hasArray();
                            final byte[] bytes1 = b ? x.BODY.array() : new byte[x.BODY.limit()];
                            if (!b) x.BODY.get(bytes1);
                            bytes = Base64.decode (bytes1);
                        } finally {
                        }
                        final int length = bytes.length;
                        System.err.println("decoded " + length);
                        if (length > 0) {
                            final FileOutputStream file = new FileOutputStream(x.outputName);
                            file.write(bytes);
                            file.close();

                        }                     
                    }
                    return;
                }


            } catch (IOException e) {
                e.printStackTrace();  //TODO: Verify for a purpose
            }


        }
        public void onWrite(SelectionKey k) {

            final SocketChannel c = (SocketChannel) k.channel();
            try {

                int i = 0;
                i += c.write((ByteBuffer) token.position(0));
                i += c.write(ProtoUtil.UTF8.encode(" " + ((NntpSession) k.attachment()).ID));
                i += c.write((ByteBuffer) ProtoUtil.EOL.position(0));
                System.err.println("sent " + i + " " + header.position(0) + " " + "****");
                setLifecycle(k, SelectionKey.OP_READ, exec);
            } catch (IOException e) {
                e.printStackTrace();
            }

        };
    };
    private static final ByteBuffer BEGIN = ProtoUtil.UTF8.encode("begin ");

    private static final int GRPLEN = 1 << 24L;

    private static final long BLEN = GRPLEN << 2;


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

