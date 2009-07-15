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
                        while (listIterator.hasNext()) {
                            Pair<Integer, LinkedList<Integer>> line = listIterator.next();
                            listIterator.remove();
                            final LinkedList<Integer> tokenIndexes = line.$2();
                            if (tokenIndexes.size() != 3) continue;
                            final Integer left = line.$1();
                            final int len = tokenIndexes.getFirst() - left;
                            if (len == BEGIN.limit() && 0 == ((ByteBuffer) BEGIN.reset()).compareTo((ByteBuffer) x.BODY.limit(left + BEGIN.limit()).position(left).mark())) {
                                x.outputName = ProtoUtil.UTF8.decode((ByteBuffer) x.BODY.limit(tokenIndexes.get(2) - 1).position(tokenIndexes.get(1))).toString();
                                System.err.println("outputName " + x.outputName);
                                System.err.println("starting from " + left + ": found outputname " + ProtoUtil.UTF8.decode((ByteBuffer) x.BODY.reset()));
                                break;
                            }
                        }
                        final Iterator<Pair<Integer, LinkedList<Integer>>> pairIterator = x.bodyLines.descendingIterator();
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
                                        x.BODY.position(x.bodyLines.getFirst().$1()).mark();


  

                                        ByteBuffer input=x.BODY.duplicate();
                                        int ooffset = 0;
                                        final RandomAccessFile of = new RandomAccessFile(x.outputName, "rw");
                                        of .setLength(input.remaining());
                                        ByteBuffer os= (((RandomAccessFile) of)).getChannel(). map(FileChannel.MapMode.READ_WRITE, 0,of.length());
                                        for (Pair<Integer, LinkedList<Integer>> pair : x.bodyLines) {
                                            Integer offset = pair.$1();
                                            int encodedoctets =  decode_char(input.get(offset))   ;
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
                                        of.setLength(ooffset-1);
                                        
                                        System.err.println("output wrote "+new File(x.outputName).getAbsolutePath()+ " "+of.length()+" bytes");
                                        of.close();
                                        break;
                                    }
                                }
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();  //TODO: Verify for a purpose
            } catch (IOException e) {
                e.printStackTrace();  //TODO: Verify for a purpose
            } catch (Throwable throwable) {
                throwable.printStackTrace();  //TODO: Verify for a purpose
            }
        }private int decode_char(byte b) {
            return b - ' ' & 63;  //To change body of created methods use File | Settings | File Templates.
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

class uudecode {
    /*==========================================================================*/

    private byte[] input;
    private byte[] output;
    private int ooffset;
    private int offset;
    public String name;

    uudecode(byte[] is) throws IOException {

        input = is;
        output = new byte[is.length];
    }

    uudecode(String filename) throws IOException {
        FileInputStream fis = new FileInputStream(filename);
        int bytesize;
        input = new byte[4000000];
        bytesize = fis.read(input, 0, 4000000);
        output = new byte[bytesize];
    }


    public void run() throws Throwable {
        offset += 10;
        get_name();
        decode();
        return;
    }

    private int decode() throws Throwable {                            
        while (input[offset] != 32) {                        
            int encodedoctets;                            
            encodedoctets = decode_char(input[offset]);                
            for (++offset; encodedoctets > 0; offset += 4, encodedoctets -= 3) { 
                int ch;                                
                if (encodedoctets >= 3) {                    
                    ch = decode_char(input[offset]) << 2 |             
                            decode_char(input[offset + 1]) >> 4;            
                    output[ooffset++] = (byte) ch;                
                    ch = decode_char(input[offset + 1]) << 4 |             
                            decode_char(input[offset + 2]) >> 2;            
                    output[ooffset++] = (byte) ch;                
                    ch = decode_char(input[offset + 2]) << 6 |             
                            decode_char(input[offset + 3]);                
                    output[ooffset++] = (byte) ch;                
                } else {
                    if (encodedoctets >= 1) {                    
                        ch = decode_char(input[offset]) << 2 |         
                                decode_char(input[offset + 1]) >> 4;            
                        output[ooffset++] = (byte) ch;                
                    }
                    if (encodedoctets >= 2) {                    
                        ch = decode_char(input[offset + 1]) << 4 |         
                                decode_char(input[offset + 2]) >> 2;            
                        output[ooffset++] = (byte) ch;                
                    }
                }
            }
            skip_to_newline();                            
        }
        skip_to_newline();                            
        if (input[offset] == 'e' && input[offset + 1] == 'n'                
                && input[offset + 2] == 'd') {                        
            return 0;                                
        }
        return -1;                                
    }

    private void skip_to_newline() {                        
        while (offset < input.length && input[offset] != 10)                         {
            offset++;
        }                                
        offset++;                                 
        return;
    }

    private int decode_char(int in) {                        
        return ((in) - ' ') & 63;                        
    }

    private void get_name() {                            
        int start = offset;                            
        while (input[offset] != 32) {                        
            offset++;                                
        }
        name = new String(input, start, offset - start);                
        offset += 2;                                
        return;
    }

    /*==========================================================================*/
    public void write_file(String filename) throws IOException {
        FileOutputStream fos = new FileOutputStream(filename);
        fos.write(output, 0, ooffset);
        System.err.println(new File(filename).getAbsolutePath());
    }

    public void reset() {
        ooffset = 0;
        offset = 0;
        return;
    }

    public static void main(String[] args) {
        uudecode bin;
        long time;
        try {
            bin = new uudecode(args[0]);
        }
        catch (IOException e) {
            return;
        }
        long start = System.currentTimeMillis();
        for (int i = 0; i < 100; i += 1) {
            bin.reset();
            try {
                bin.run();
            } catch (Throwable throwable) {
                throwable.printStackTrace();  //TODO: Verify for a purpose
            }
        }
        long end = System.currentTimeMillis();
        time = end - start;
        System.out.print("" + (float) time / 1000.0);
        try {
            bin.write_file(bin.name);
        }
        catch (IOException e) {
            return;
        }
    }
}
