package hideftvads.server;


import alg.Pair;
import ds.tree.RadixTree;
import ds.tree.RadixTreeImpl;
import hideftvads.proto.HttpMethod;
import static hideftvads.proto.HttpMethod.$;
import javolution.util.FastMap;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Hello world!
 */
public class Agent {
    public static boolean killswitch = false;
    public static Selector selector;
    public static ServerSocketChannel serverSocketChannel;
    //    public ByteBuffer token;
    public static int port = 8080;
    private static final Pattern eq = Pattern.compile("=");
    String config;
    Properties
            properties = new Properties();
    private static Map configs = new FastMap();


    public static void main(String[] args) throws IOException {

        for (String arg : args) {

            if (arg.startsWith("-")) {
                final Pair<String, String> pair = new Pair<String, String>(arg.split("=", 2));

                configs.put(pair.$1(pair.$1().substring(1)), pair.$2());

            }


            final RadixTree tree = new RadixTreeImpl();


        }


        final HttpMethod httpMethod = $;

        while (!killswitch) try {
            Thread.sleep(10000);
        } catch (InterruptedException ignored) {
        }
    }

    public static void init() {
        try {
            selector = Selector.open();
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.socket().bind(new java.net.InetSocketAddress(port));
            serverSocketChannel.configureBlocking(false);
            final SelectionKey listenerKey = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            while (!killswitch) {
                selector.select();
                Set<SelectionKey> keys = selector.selectedKeys();

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
}
