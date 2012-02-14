package com.vsiwest;

import com.google.gson.Gson;
import one.xio.HttpMethod;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static one.xio.HttpMethod.*;

/**
 * Created by IntelliJ IDEA.
 * User: jim
 * Date: 2/12/12
 * Time: 10:24 PM
 */
public class CouchChangesClient {

    private static String feedname = "clproxy";
    private static Serializable port = 5984;
    private static String hostname = "127.0.0.1";
    static boolean active = false;
    public static final int POLL_HEARTBEAT_MS = 45000;
    public static final byte[] ENDUPDATE = new byte[]{/*'\n',*/ '\r', '\n'};

    public static void main(String... args) throws IOException {

        if (0 < args.length) {
            hostname = args[0];
            if (1 < args.length) {
                port = args[1];
                if (2 < args.length)
                    feedname = args[2];
            }
        }


        InetSocketAddress remote = new InetSocketAddress(hostname, (Integer) port);
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);
        channel.connect(remote);

        String feedString = getFeedString();
        System.err.println("feedstring: " + feedString);
        HttpMethod.enqueue(channel, SelectionKey.OP_CONNECT, HttpMethod.$COUCHCONTROL, feedString);
        HttpMethod.main(args);
    }

    private static String getFeedString() {
        return "/" + feedname + "/_changes?feed=continuous&heartbeat=" +
                POLL_HEARTBEAT_MS;
    }

    public static void CouchControllerWrite(SelectionKey key) {
        Object[] attachment = (Object[]) key.attachment();
        SocketChannel channel = (SocketChannel) key.channel();

        try {
            channel.write((ByteBuffer) attachment[1]);
            key.interestOps(OP_READ);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public static void CouchControllerConnect(SelectionKey key) {
        Object[] attachment = (Object[]) key.attachment();
        SocketChannel channel = (SocketChannel) key.channel();
        try {
            if (channel.finishConnect()) {
                String str = "GET " + getFeedString() + " HTTP/1.1\r\n\r\n";
                attachment[1] = UTF8.encode(str);
                key.interestOps(OP_WRITE);//a bit academic for now
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * handles a control socket read
     *
     * @param key{$CLCONTROL,feedstr,pending}
     *
     */

    public static void CouchControllerRead(SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        Object[] attachment = (Object[]) key.attachment();

        try {
            ByteBuffer b = ByteBuffer.allocateDirect(channel.socket().getReceiveBufferSize());
//            ByteBuffer b = ByteBuffer.allocateDirect(333);
            int sofar = channel.read(b);
            if (active) {
                b.flip();

                Object prev = attachment.length > 2 ? attachment[2] : null;
                boolean stuff = false;
                ByteBuffer wrap = ByteBuffer.wrap(ENDUPDATE);
                b.mark();
                b.position(b.limit() - ENDUPDATE.length);


                if (0 != wrap.compareTo(b)) {
                    stuff = true;

                }
                b.reset();
                Object[] objects = {b, prev};
                if (stuff) {
                    Object[] ob = {attachment[0], attachment[1], objects};
                    key.attach(ob);
                } else {
                    key.attach(new Object[]{attachment[0], attachment[1]});
                    ClControlOnBufferCompletion(objects);
                }
                return;
            }
            String s = UTF8.decode((ByteBuffer) b.rewind()).toString();
            if (s.startsWith("HTTP/1.1 200")) {
                active = true;
            } else if (s.startsWith("HTTP/1.1 201")) {
                killswitch = true;
            } else {
                ByteBuffer encode = UTF8.encode("PUT /" + feedname + "/ HTTP/1.1\r\n\r\n");
                attachment[1] = encode;
                key.attach(toArray($COUCHCONTROL, encode));
                key.interestOps(OP_WRITE);
            }


        } catch (SocketException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();

    static {
    }

    static void ClControlOnBufferCompletion(final Object[] blist) {


        //offload the heavy stuff to some other core if possible
        EXECUTOR_SERVICE.submit(
                new Runnable() {
                    Object[] slist = blist;
                    ByteBuffer buffer;
                    Deque<ByteBuffer> linkedList;       //doesn't get used if only for a single read buffer
                    int bufsize = 0;

                    public void run() {

                        //grab the total size of the buffers and reorder them into a forward list.

                        do {

                            ByteBuffer byteBuffer = (ByteBuffer) slist[0];
                            slist = (Object[]) slist[1];
                            if (0 == bufsize) {
                                if (null == slist) {
                                    buffer = byteBuffer;
                                    break;//optimization
                                }
                                linkedList = new LinkedList<ByteBuffer>();
                            }
                            bufsize += byteBuffer.limit();
                            linkedList.addFirst(byteBuffer);

                        } while (null != slist);

                        if (null == buffer) {
                            buffer = ByteBuffer.allocateDirect(bufsize);

                            for (ByteBuffer netBuffer : linkedList) {
                                buffer.put(netBuffer);
                            }
                        }

                        buffer.rewind();
                        System.err.println("<<<<" + buffer.limit());
                        do {
                            ByteBuffer b = buffer.slice();
                            while (b.hasRemaining() && b.get() != ENDUPDATE[ENDUPDATE.length - 1]) ;
                            b.flip();
                            Integer integer = Integer.valueOf(UTF8.decode(b).toString().trim(), 0x10);
                            System.err.println("<<<" + integer);
                            buffer = ((ByteBuffer) buffer.position(b.limit())).slice();
                            ByteBuffer handoff = (ByteBuffer) buffer.slice().limit(integer);
                            String trim = UTF8.decode(handoff).toString().trim();
                            System.err.println("===" + trim);

                            CouchChange couchChange = new Gson().fromJson(trim, CouchChange.class);
                            System.err.println("+++"+couchChange.id);
                            buffer.position(handoff.limit() + ENDUPDATE.length);
                            buffer = buffer.slice();
                        } while (buffer.hasRemaining());
                    }
                });
    }
}

