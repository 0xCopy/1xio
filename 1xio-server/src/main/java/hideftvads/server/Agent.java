package hideftvads.server;


import alg.*;
import hideftvads.proto.*;
import static hideftvads.proto.ProtoUtil.*;
import javolution.text.*;
import javolution.util.*;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

/**
 * Hello world!
 */
public class Agent {
    public boolean killswitch = false;
    public ServerSocketChannel serverSocketChannel;
    //    public ByteBuffer token;
    public int port = 8080;

    // '$' as universal polymorphic default predicate,
    // when in doubt '$ $($...  $){} is the dominant generic predicate expression

    private static final HttpMethod $ = HttpMethod.$;
    private FastList<Pair<String, String>> cfg = new FastList<Pair<String, String>>();

    final Selector selector = Selector.open();

    public Agent(CharSequence... args) throws IOException {

        for (CharSequence arg : args) {
            if (arg.charAt(0) == '-') {
                final String[] v = Pattern.compile("=").split(arg, 2);
                final Pair<String, String> pair = new Pair<String, String>(Text.intern(v[0].substring(1)).toString(), Text.valueOf(v[1]).toString());
                cfg.add(pair);
            }
        }
        Runnable runnable = new Runnable() {
            public void run() {

                try {
                    serverSocketChannel = ServerSocketChannel.open();
                    final ServerSocket socket = serverSocketChannel.socket();
                    serverSocketChannel.configureBlocking(false);
                    socket.setPerformancePreferences(1, 9999, 0);
                    socket.bind(new InetSocketAddress(port));
                    socket.setReceiveBufferSize(512);
                    
                    final SelectionKey listenerKey = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

                    while (!killswitch) {
                        final int count = selector.select();
                        final Set<SelectionKey> keys = selector.selectedKeys();

                        for (Iterator<SelectionKey> i = keys.iterator(); i.hasNext();) {
                            final SelectionKey key = i.next();
                            i.remove();

                            if (key.isValid()) {
                                try {

                                    final Object at = key.attachment();
                                    if (at instanceof Callable) {
//                                        client.interestOps(0);
//                                        threadPool.submit((Callable) at);
                                        ((Callable) at).call();
                                        continue;
                                    } else if (at instanceof Runnable) {
//                                         client.interestOps(0);
//                                        threadPool.submit((Runnable) at);

                                        ((Runnable) at).run();
                                        continue;
                                    } else {

                                        final HttpMethod m;
                                        m = at == null
                                                ? $
                                                : (HttpMethod) (at instanceof Object[]
                                                && ((Object[]) at)[0] instanceof HttpMethod
                                                ? ((Object[]) at)[0]
                                                : at instanceof HttpMethod
                                                ? at
                                                : $
                                        );

                                        if (key.isWritable()) {
                                            m.onWrite(key);
                                        }

                                        if (key.isReadable()) {
                                            m.onRead(key);
                                        }

                                        if (key.isConnectable()) {
                                            m.onConnect(key);
                                        }

                                        if (key.isAcceptable()) {
                                            m.onAccept(key);
                                        }
                                    }


                                } catch (Exception e) {
//                                    e.printStackTrace();
                                    key.attach(null);
                                    key.channel().close();
                                    key.cancel();
                                }
                            }
                        }
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        };

        threadPool.submit(runnable);
    }

    static public void main(String... a) throws IOException {
        final Agent agent = new Agent(a);

        while (!agent.killswitch) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException ignored) {
            }
        }
    }
}
