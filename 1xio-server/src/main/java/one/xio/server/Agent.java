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

/**
 * Created by IntelliJ IDEA.
 * User: kiosk
 * Date: Dec 2, 2009
 * Time: 2:32:15 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class Agent {
    private boolean killswitch = false;
    private ServerSocketChannel serverSocketChannel;
    private int port = 8080;
    private static final HttpMethod $ = HttpMethod.$;
    private List<Pair<String, String>> cfg = new FastList<Pair<String, String>>();
    final Selector selector;
    private SelectionKey listenerKey;
    private int bufferSize = 512;

    public Agent(CharSequence... args) throws IOException {
        selector = Selector.open();
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

    public void handleArgs(CharSequence[] args) {
        for (CharSequence arg : args) {
            if (arg.charAt(0) == '-') {
                final String[] v = Pattern.compile("=").split(arg, 2);
                final Pair<String, String> pair = new Pair<String, String>(Text.intern(v[0].substring(1)).toString(), Text.valueOf(v[1]).toString());
                getCfg().add(pair);
            }
        }
            }

    public void onRunnable(Object attachment) {
        ((Runnable) attachment).run();
    }

    public void onCallable(Object attachment) throws Exception {
        final Callable callable = (Callable) attachment;
        callable.call();
    }

    public abstract void onAccept(SelectionKey key);

    public abstract void onConnect(SelectionKey key);

    public abstract void onRead(SelectionKey key);

    public abstract void onWrite(SelectionKey key);

    public void onAccept(SelectionKey key, HttpMethod m) {
        m.onAccept(key);
    }

    public void onConnect(SelectionKey key, HttpMethod m) {
        m.onConnect(key);
    }

    public void onRead(SelectionKey key, HttpMethod m) {
        m.onRead(key);
    }

    public void onWrite(SelectionKey key, HttpMethod m) {
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
