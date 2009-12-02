package one.xio.server;


import alg.Pair;
import javolution.text.Text;
import javolution.util.FastList;
import one.xio.proto.HttpMethod;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import static one.xio.proto.ProtoUtil.threadPool;
//import javolution.text.Text;

/**
 * Hello world!
 */
public class Agent {
    private boolean killswitch = false;
    private ServerSocketChannel serverSocketChannel;
    //    public ByteBuffer token;
    private int port = 8080;

    // '$' as universal polymorphic default predicate,
    // when in doubt '$ $($...  $){} is the dominant generic predicate expression

    private static final HttpMethod $ = HttpMethod.$;
    private List<Pair<String, String>> cfg = new FastList<Pair<String, String>>();

    private final Selector selector = Selector.open();
    private SelectionKey listenerKey;
    private int bufferSize = 512;

    public Agent(CharSequence... args) throws IOException {

        handleArgs(args);
        Runnable runnable = new Runnable() {
            public void run() {

                try {
                    initSocket();

                    setListenerKey(getServerSocketChannel().register(getSelector(), SelectionKey.OP_ACCEPT));

                    while (!isKillswitch()) {
                        final int count = getSelector().select();
                        final Set<SelectionKey> keys = getSelector().selectedKeys();

                        for (Iterator<SelectionKey> i = keys.iterator(); i.hasNext();) {
                            final SelectionKey key = i.next();
                            i.remove();

                            if (key.isValid()) {
                                try {

                                    final Object attachment = key.attachment();

                                    if (attachment == null) {
                                        if (key.isValid() && key.isWritable()) {
                                            onWrite(key);
                                        }

                                        if (key.isValid() && key.isReadable()) {
                                            onRead(key);
                                        }

                                        if (key.isValid() && key.isConnectable()) {
                                            onConnect(key);
                                        }

                                        if (key.isValid() && key.isAcceptable()) {
                                            onAccept(key);
                                        }
                                        continue;
                                    }

                                    if (attachment instanceof Callable) {
//                                        client.interestOps(0);
//                                        threadPool.submit((Callable) attachment);
                                        onCallable(attachment);
                                        continue;
                                    }

                                    if (attachment instanceof Runnable) {
//                                         client.interestOps(0);
//                                        threadPool.submit((Runnable) attachment);

                                        onRunnable(attachment);
                                        continue;
                                    }

                                    HttpMethod m;
                                    if (attachment instanceof Object[] && ((Object[]) attachment)[0] instanceof HttpMethod)
                                        m = (HttpMethod) ((Object[]) attachment)[0];
                                    else if ((HttpMethod) attachment instanceof HttpMethod)
                                        m = (HttpMethod) attachment;
                                    else
                                        m = get$();

                                    if (key.isValid() && key.isWritable()) {
                                        onWrite(key, m);
                                    }

                                    if (key.isValid() && key.isReadable()) {
                                        onRead(key, m);
                                    }

                                    if (key.isValid() && key.isConnectable()) {
                                        onConnect(key, m);
                                    }

                                    if (key.isValid() && key.isAcceptable()) {
                                        onAccept(key, m);
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

    private void handleArgs(CharSequence[] args) {
        for (CharSequence arg : args) {
            if (arg.charAt(0) == '-') {
                final String[] v = Pattern.compile("=").split(arg, 2);
                final Pair<String, String> pair = new Pair<String, String>(Text.intern(v[0].substring(1)).toString(), Text.valueOf(v[1]).toString());
                getCfg().add(pair);
            }
        }
    }

    private void onRunnable(Object attachment) {
        ((Runnable) attachment).run();
    }

    private void onCallable(Object attachment) throws Exception {
        final Callable callable = (Callable) attachment;
        callable.call();
    }

    private void onAccept(SelectionKey key) {
        onAccept(key, get$());
    }

    private void onConnect(SelectionKey key) {
        onConnect(key, get$());
    }

    private void onRead(SelectionKey key) {
        onRead(key, get$());
    }

    private void onWrite(SelectionKey key) {
        onWrite(key, get$());
    }

    private void onAccept(SelectionKey key, HttpMethod m) {
        m.onAccept(key);
    }

    private void onConnect(SelectionKey key, HttpMethod m) {
        m.onConnect(key);
    }

    private void onRead(SelectionKey key, HttpMethod m) {
        m.onRead(key);
    }

    private void onWrite(SelectionKey key, HttpMethod m) {
        m.onWrite(key);
    }


    protected void initSocket() throws IOException {
        setServerSocketChannel(ServerSocketChannel.open());
        final ServerSocket socket = getServerSocketChannel().socket();
        getServerSocketChannel().configureBlocking(false);
        socket.setPerformancePreferences(1, 9999, 0);
        socket.bind(new InetSocketAddress(getPort()));
        setBufferSize(512);
        socket.setReceiveBufferSize(getBufferSize());
    }

    static public void main(String... a) throws IOException {
        final Agent agent = new Agent(a);

        while (!agent.isKillswitch()) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException ignored) {
            }
        }
    }

    public boolean isKillswitch() {
        return killswitch;
    }

    public void setKillswitch(boolean killswitch) {
        this.killswitch = killswitch;
    }

    public ServerSocketChannel getServerSocketChannel() {
        return serverSocketChannel;
    }

    public void setServerSocketChannel(ServerSocketChannel serverSocketChannel) {
        this.serverSocketChannel = serverSocketChannel;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }


    public List<Pair<String, String>> getCfg() {
        return cfg;
    }

    public void setCfg(List<Pair<String, String>> cfg) {
        this.cfg = cfg;
    }

    public Selector getSelector() {
        return selector;
    }

    public SelectionKey getListenerKey() {
        return listenerKey;
    }

    public void setListenerKey(SelectionKey listenerKey) {
        this.listenerKey = listenerKey;
    }

    public static HttpMethod $() {
        return get$();
    }

    public static HttpMethod get$() {
        return $;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }
}
