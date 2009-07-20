package hideftvads.proto;

import alg.*;

import java.lang.ref.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;

public class ProtoUtil {
    public static final int CHUNKDEFAULT = 4;

    public static final int LF = '\n' & 0xff;

    public static final int CHUNK_NUM = 128;
    public static final int KBYTE = 1024;
    static final int MAX_EXP = 16;
    public static final int DEFAULT_EXP = 4;

    public static ExecutorService threadPool = Executors.newCachedThreadPool();
    public static final Charset UTF8 = Charset.forName("UTF8");
    public static final ByteBuffer WS = ByteBuffer.wrap(new byte[]{' '});
    public static final ByteBuffer EOL = ByteBuffer.wrap(new byte[]{'\r', '\n'});
    //    static final Charset UTF8 = Charset.forName("UTF8");
    static final byte[] FIREFOX_ENDLINE = new byte[]{'\r', '\n',
            0};
    public static boolean killswitch = false;
    public static Timer timer=new Timer("1xio timer",true);

    public
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


    static synchronized public void refill(final int slot) {
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

    public static void recycle(Reference<ByteBuffer> byteBufferReference, int shift) {
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

    static CharBuffer extractUri(ByteBuffer src, LinkedList<Pair<Integer, LinkedList<Integer>>> lines) {
        final Integer seek = lines.getFirst().$2().get(1) + 1;
        int limit = lines.getFirst().$2().get(2) - 1;
        src.limit(limit).position(seek);
        final CharBuffer buffer = UTF8.decode(src);
        return buffer;
    }

    static LinkedList<Pair<Integer, LinkedList<Integer>>> preIndex(ByteBuffer src) {
        LinkedList<Pair<Integer, LinkedList<Integer>>> lines = new LinkedList<Pair<Integer, LinkedList<Integer>>>();
        final int pos = src.position();
        lines.add(new Pair<Integer, LinkedList<Integer>>(pos, new LinkedList<Integer>()));

        byte prev = 0;

        L1:


        while (src.hasRemaining()) {
            byte b = src.get();

            switch (b) {
                case LF:
                    if (prev == LF) {
                        break L1;
                    }
                    lines.add(new Pair<Integer, LinkedList<Integer>>(src.position(), new LinkedList<Integer>()));


                    break;
                case '\r':
                default:
                    if (!java.lang.Character.isWhitespace(b) && '\r' != b || java.lang.Character.isWhitespace(prev)) {
                    } else {
                        lines.getLast().$2().add(src.position());
                    }
                    break;
            }
            prev = b;
        }
        return lines;
    }
                        static 
    public void recycle(Reference<ByteBuffer> br) {
        ProtoUtil.recycle(br, DEFAULT_EXP);
    }
}
