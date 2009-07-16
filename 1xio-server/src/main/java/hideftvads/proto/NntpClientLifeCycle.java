package hideftvads.proto;

import alg.*;

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
                    System.err.println(name() + " writing " + tempFile.toURI().toASCIIString());
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
                x.gcursor += l = channel.transferFrom((ReadableByteChannel) k.channel(), x.gcursor, GRPLEN - x.gcursor);

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
                        x.LIST = x.gchannel.map(FileChannel.MapMode.READ_WRITE, 0, x.gcursor);
                        setLifecycle(k, SelectionKey.OP_READ, exec);
                        System.err.println("total " + x.gcursor);
                        //
                        final LinkedList<Pair<Integer, LinkedList<Integer>>> list = ProtoUtil.preIndex(x.LIST);
                        x.groupList = new CopyOnWriteArrayList<Pair<Integer, LinkedList<Integer>>>(list.toArray(new Pair[0]));


                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.err.println(Arrays.toString(x.groupList.toArray()));
                    x.LIST.clear();
                    return;
                }


            } catch (IOException e) {
                e.printStackTrace();
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
                System.err.println("sent " + i + " " + header.position(0));
                setLifecycle(k, SelectionKey.OP_READ, exec);
            } catch (IOException e) {
                e.printStackTrace();
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
                e.printStackTrace();
            }

        }},
    READY,
    VERSION,
    CAPABILITIES,
    BODY {


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
                    System.err.println(name() + " writing " + tempFile.toURI().toASCIIString());

                    final boolean b = tempFile.createNewFile();
                    randomAccessFile = new RandomAccessFile(tempFile.getAbsoluteFile(), "rw");
                    randomAccessFile.setLength(BLEN);

                    channel = randomAccessFile.getChannel();
                    buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, BLEN);
                    x.bodyFile = randomAccessFile;
                    x.bodyChannel = channel;
                    x.LIST = buffer;
                    x.bodyTmpFile = tempFile;
                }
                int l = 0;
                x.bodyCursor += l = (int) channel.transferFrom((ReadableByteChannel) k.channel(), x.bodyCursor, BLEN - x.bodyCursor);

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
                        buffer = x.BODY = x.bodyChannel.map(FileChannel.MapMode.READ_WRITE, 0, x.bodyCursor);
                        setLifecycle(k, SelectionKey.OP_READ, exec);
                        System.err.println("total " + x.bodyCursor);
                        //

                        x.bodyLines = ProtoUtil.preIndex(x.BODY);
                        final ListIterator<Pair<Integer, LinkedList<Integer>>> listIterator = x.bodyLines.listIterator();
                        boolean uudecode = false;
                        boolean useYdec = false;
                        while (listIterator.hasNext()) {
                            Pair<Integer, LinkedList<Integer>> line = listIterator.next();
                            listIterator.remove();
                            final LinkedList<Integer> tokenIndexes = line.$2();

                            final Integer left = line.$1();
                            final int len = tokenIndexes.getFirst() - left;
                            if (len == BEGIN.limit()
                                    && 0 == ((ByteBuffer) BEGIN.reset()).compareTo((ByteBuffer) x.BODY.limit(left + BEGIN.limit()).position(left).mark())) {
                                x.outputName = ProtoUtil.UTF8.decode((ByteBuffer) x.BODY.limit(tokenIndexes.get(2) - 1).position(tokenIndexes.get(1))).toString();
                                System.err.println("outputName " + x.outputName);
                                System.err.println("starting from " + left + ": found outputname " + ProtoUtil.UTF8.decode((ByteBuffer) x.BODY.reset()));
                                uudecode = true;
                                break;
                            } else if (len == YBEGIN.limit()
                                    && 0 == ((ByteBuffer) YBEGIN.reset()).compareTo((ByteBuffer) x.BODY.limit(left + YBEGIN.limit()).position(left).mark())) {
                                x.outputName = ProtoUtil.UTF8.decode((ByteBuffer) x.BODY.limit(tokenIndexes.get(2) - 1).position(tokenIndexes.get(1))).toString();
                                System.err.println("yenc decode ");
//                                System.err.println("starting from " + left + ": found outputname " + ProtoUtil.UTF8.decode((ByteBuffer) x.BODY.reset()));
                                useYdec = true;
                                break;
                            }
                        }
                        final Iterator<Pair<Integer, LinkedList<Integer>>> pairIterator = x.bodyLines.descendingIterator();
                        if (uudecode)
                            while (pairIterator.hasNext()) {
                                Pair<Integer, LinkedList<Integer>> line = pairIterator.next();
                                pairIterator.remove();
                                Integer left = null;
                                left = line.$1();
                                //                            if (!line.$2().isEmpty())
                                if (line.$2().size() == 1)
                                    if (2 == line.$2().getLast() - left) {
                                        System.err.println(line.toString()
                                                + ' '
                                                + ProtoUtil.UTF8.decode((ByteBuffer) x.BODY.limit(line.$2().getLast()).position(left)));

                                        if ('`' == ((ByteBuffer) x.BODY.limit(line.$2().getLast())).get(left)) {
                                            uud(x);
                                            break;
                                        }
                                    }
                            }
                        else if (useYdec) {

//                            try {
                            try {
                                x.bodyFile.seek(0);
                                YDecoder.decode(x.bodyFile, "./");
                            } catch (Throwable e) {
                                e.printStackTrace();
                            }
//                            } catch (YEncException e) {
//                                e.printStackTrace(); 
//                            }
//                            System.err.println("ydec wrote " + tempFile.getAbsolutePath() + " b: " + tempFile.length());

                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } /*catch (Throwable throwable) {
                throwable.printStackTrace();
            }*/
        }
        public void onWrite(SelectionKey k) {

            final SocketChannel c = (SocketChannel) k.channel();
            try {

                int i = 0;
                i += c.write((ByteBuffer) token.position(0));
                final String s = ((NntpSession) k.attachment()).FETCH_ID;
                i += c.write(ProtoUtil.UTF8.encode(" " + s));
                i += c.write((ByteBuffer) ProtoUtil.EOL.position(0));
                System.err.println("sent " + i + " " + header.position(0) + " " + s + '.');
                setLifecycle(k, SelectionKey.OP_READ, exec);
            } catch (IOException e) {
                e.printStackTrace();
            }

        };
    };

    private static void uud(NntpSession x) throws IOException {
        x.BODY.position(x.bodyLines.getFirst().$1()).mark();

        ByteBuffer input = x.BODY.duplicate();
        int ooffset = 0;
        final RandomAccessFile of = new RandomAccessFile(x.outputName, "rw");
        of.setLength(input.remaining());
        ByteBuffer os = (((RandomAccessFile) of)).getChannel().map(FileChannel.MapMode.READ_WRITE, 0, of.length());
        for (final Pair<Integer, LinkedList<Integer>> pair : x.bodyLines) {
            Integer offset = pair.$1();
            int encodedoctets = decode_char(input.get(offset));
            for (++offset; encodedoctets > 0; offset += 4, encodedoctets -= 3) {
                int ch;
                if (encodedoctets >= 3) {
                    ch = decode_char(input.get(offset)) << 2 |
                            decode_char(input.get(offset + 1)) >> 4;
                    os.put(ooffset++, (byte) ch);
                    ch = decode_char(input.get(offset + 1)) << 4 |
                            decode_char(input.get(offset + 2)) >> 2;
                    os.put(ooffset++, (byte) ch);
                    ch = decode_char(input.get(offset + 2)) << 6 |
                            decode_char(input.get(offset + 3));
                    os.put(ooffset++, (byte) ch);
                } else {
                    if (encodedoctets >= 1) {
                        ch = decode_char(input.get(offset)) << 2 |
                                decode_char(input.get(offset + 1)) >> 4;
                        os.put(ooffset++, (byte) ch);
                    }
                    if (encodedoctets >= 2) {
                        ch = decode_char(input.get(offset + 1)) << 4 |
                                decode_char(input.get(offset + 2)) >> 2;
                        os.put(ooffset++, (byte) ch);
                    }
                }
            }
        }
        of.setLength(ooffset - 1);

        System.err.println("output wrote " + new File(x.outputName).getAbsolutePath() + " " + of.length() + " bytes");
        of.close();
    }

    private static int decode_char(byte b) {
        return b - ' ' & 63;
    }

    private static final ByteBuffer YBEGIN = (ByteBuffer) ProtoUtil.UTF8.encode("=ybegin ").mark();
    private static final ByteBuffer BEGIN = (ByteBuffer) ProtoUtil.UTF8.encode("begin ").mark();

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
            e.printStackTrace();
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
            e.printStackTrace();
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

/**
 * Implementation of the <b>decoder</b> for YEncoding project.
 * <p/>
 * This class is to be used to decode files encoded with yenc<br>
 * <FONT Size=+2>See <a href="http://www.yenc.org">www.yenc.org</a> for details.</FONT>
 * <p/>
 * To <b>run</b> the project, use:<br>
 * <code>java org.yenc.YDecoder FileToDecode DestinationFolder</code>
 * <p/>
 * Known limitations:
 * <UL>
 * * The CRC is not checked.<br>
 * * I tried using Streams, but those do not support binary very well.<br>
 * </UL>
 * <p/>
 * <p/>
 * If you have imporovements to this code, please send them to me or to yenc@infostar.de
 * <p/>
 *
 * @author &lt; Alex Rass &gt; sashasemail@yahoo.com
 * @version 2<br>
 *          Copywrite by Alex Rass 2002.
 *          This software is to be distributed in accordance with the GNU piblic license.
 */
final class YDecoder {
    private static final String EMPTY_STRING = "";

    static {
        System.err.println("Decoder for YEnc.org project.  Version " + getVersionNumber());
    }

    /**
     * Making this private, ensures that noone tries to instantiate this class.
     */
    private YDecoder() {
    }

    /**
     * This method does all of the decoding work.
     *
     * @param file   takes a file to read from
     * @param folder destination folder.
     *               File will be created based on the name provided by the header.
     *               <p/>
     *               if there is an error in the header and the name
     *               can not be obtained, "unknown" is used.
     * @throws IOException
     */
    public static void decode(RandomAccessFile file, String folder) throws IOException {
        /* Get initial parameters */
        String line = file.readLine();
        L1:
        while (line != null && !line.startsWith("=ybegin")) {
            line = file.readLine();
        }
        if (line != null) {

            String fileName = parseForName(line, "name");
            if (fileName == null)
                fileName = "Unknown.blob";
            fileName = folder + fileName;
            RandomAccessFile fileOut = new RandomAccessFile(fileName, "rw");

            String partNo = parseForName(line, "part");

            /* Handle Multi-part */
            if (partNo == null) {
                try {
                    fileOut.setLength(0); // reset file
    
                    /* Decode the file */
                    int character;
                    boolean special = false;
    
                    line = file.readLine();
                    while (line != null && !line.startsWith("=yend")) {
                        for (int lcv = 0; lcv < line.length(); lcv++) {
                            character = (int) line.charAt(lcv);
                            if (character != 61) {
                                character = decodeChar(character, special);
                                fileOut.write(character);
                                //System.out.print((char) character);
                                special = false;
                            } else
                                special = true;
                        }
                        line = file.readLine();
                    }
                    fileOut.close();
                } catch (IOException e) {
                    e.printStackTrace();  //TODO: Verify for a purpose
                }
            } else {
                while (line != null && !line.startsWith("=ypart")) {
                    line = file.readLine();
                }
                if (line != null) {
                    long begin = Long.parseLong(parseForName(line, "begin")) - 1;
                    if (fileOut.length() < begin)
                        fileOut.setLength(begin - 1); // reset file
                    fileOut.seek(begin);
                }
//            break;
            }
        }
    }

    private static int decodeChar(int character, boolean special) throws IOException {
        int result;
        if (special)
            character = character - 64;

        result = character - 42;

        if (result < 0)
            result += 256;

        return result;
    }

    private static String parseForName(String line, String param) {
        int indexStart = line.indexOf(param + "=");
        int indexEnd = line.indexOf(" ", indexStart);
        if (indexEnd == -1)
            indexEnd = line.length();
        if (indexStart > -1)
            return line.substring(indexStart + param.length() + 1, indexEnd);
        else
            return null;
    }

    /**
     * Provides a way to find out which version this decoding engine is up to.
     *
     * @return Version number
     */
    public static int getVersionNumber() {
        return 2;
    }

    /**
     * To Run:
     * java org.yenc.YDecImpl FileToDecode DestinationFolder
     *
     * @param args Command line argument(s)
     * @throws IOException
     */
    public static void main(String... args) throws IOException {
        if (args.length < 1) {
            // print usage and exit
            System.err.println("Usage arguments: fileIn folderOut");
        } else {
            RandomAccessFile file = new RandomAccessFile(args[0], "r");
            String folder = args.length > 1 ? args[1] + File.separator : "";
            decode(file, folder);
        }
    }
}
