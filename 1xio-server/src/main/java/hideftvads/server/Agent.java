package hideftvads.server;


import alg.Pair;
import ds.tree.RadixTree;
import ds.tree.RadixTreeImpl;
import hideftvads.proto.HttpMethod;
import javolution.text.Text;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;

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
    private RadixTree<Text> cfg=new RadixTreeImpl<Text>();

    final Selector selector = Selector.open();

    public Agent(CharSequence... args) throws IOException {

        for (CharSequence arg : args) {
            if (arg.charAt(0) == '-') {
                final String[] v = Pattern.compile("=").split(arg, 2);
                final Pair<Text, Text> pair = new Pair<Text, Text>(Text.intern(v[0].substring(1)), Text.valueOf(v[1]));
                cfg = new RadixTreeImpl<Text>()  ;
                cfg.insert(pair);
            }
        }
        Runnable runnable = new Runnable() {
            public void run() {

                try {
                    serverSocketChannel = ServerSocketChannel.open();
                    serverSocketChannel.socket().bind(new InetSocketAddress(port));
                    serverSocketChannel.configureBlocking(false);
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


                                } catch (Exception e) {
//                                    e.printStackTrace();
                                    key.attach(null);
                                    key.channel().close();
                                }
                            }
                        }
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        };

        HttpMethod.threadPool.submit(runnable);
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
