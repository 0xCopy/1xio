package one.xio;

import one.xio.AsioVisitor.Helper.F;
import one.xio.AsioVisitor.SslVisitor.ClientSslTask;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Phaser;

import static java.lang.StrictMath.min;
import static java.nio.channels.SelectionKey.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static one.xio.AsioVisitor.FSM.*;
import static one.xio.AsioVisitor.SslVisitor.Minimal.client;

/**
 * User: jim
 * Date: 4/15/12
 * Time: 11:50 PM
 */
public interface AsioVisitor {
  boolean $DBG = null != System.getenv("DEBUG_VISITOR_ORIGINS");
  WeakHashMap<Impl, String> $origins = $DBG
      ? new WeakHashMap<Impl, String>()
      : null;

  void onRead(SelectionKey key) throws Exception;

  void onConnect(SelectionKey key) throws Exception;

  void onWrite(SelectionKey key) throws Exception;

  void onAccept(SelectionKey key) throws Exception;

  /**
   * marker
   */
  interface SslVisitor extends F {

    /**
     * buffer from remote address socket
     *
     * @return inbuff
     */
    ByteBuffer getFromSSL();

    /**
     * buffer sent to remote address socket
     *
     * @return outbuf
     */
    ByteBuffer getToSSL();

    /**
     * each sslVisitor has its own SslEngine reference
     *
     * @return
     */
    SSLEngine getSslEngine();

    ExecutorService getExecutorService();

    void setFromSsl(ByteBuffer byteBuffer);

    /**
     * performs IO on behalf of SslEngine negotiation as client-mode
     */
    interface ClientSslTask extends SslVisitor {
    }

    /**
     * performs IO on behalf of SslEngine negotiation as server-mode
     */
    interface ServerSslTask extends SslVisitor {
    }

    class Minimal {
      private static ExecutorService executorService;


      public static void setExecutorService(ExecutorService executorService) {
        Minimal.executorService = executorService;
      }

/*not done with client yet.  will be mostly the same
      public abstract  static ServerSslTask server(
          final String... nonAuthoritativeHostname) ;
*/

      public static ClientSslTask client(final String... nonAuthoritativeHostname) {
        return new ClientSslTask() {
          public void setSession(SSLSession session) {
            this.session = session;
          }

          public SSLSession getSession() {
            return session;
          }

          private SSLSession session;
          private ByteBuffer fromSSL;
          private ByteBuffer ToSSL;
          private SSLEngine sslEngine;

          @Override
          public void apply(SelectionKey key) throws Exception {
            if (0 < nonAuthoritativeHostname.length) {
              String s = nonAuthoritativeHostname[0];
              int port = ((InetSocketAddress) ((SocketChannel) key
                  .channel())
                  .getRemoteAddress())
                  .getPort();
              sslEngine = SSLContext.getDefault().createSSLEngine(s, port);
            } else
              sslEngine = SSLContext.getDefault().createSSLEngine();

//            sslEngine.setEnabledProtocols(new String[]{"TLSv1"});

            sslEngine.setUseClientMode(true);
/*            sslEngine.setEnableSessionCreation(true);
            String[] enabledCipherSuites = sslEngine.getEnabledCipherSuites();
            String[] enabledProtocols = sslEngine.getEnabledProtocols();
            SSLParameters sslParameters = sslEngine.getSSLParameters();*/
//            sslEngine.setWantClientAuth(false);
            int packetBufferSize = sslEngine.getSession().getPacketBufferSize();
            fromSSL = ByteBuffer.allocateDirect(packetBufferSize);
            ToSSL = ByteBuffer.allocate(packetBufferSize);
            sslSessions.put(key, this);
          }

          public ByteBuffer getFromSSL() {
            return fromSSL;
          }

          public void setFromSSL(ByteBuffer fromSSL) {
            this.fromSSL = fromSSL;
          }

          public ByteBuffer getToSSL() {
            return ToSSL;
          }

          public void setToSSL(ByteBuffer toSSL) {
            ToSSL = toSSL;
          }

          public SSLEngine getSslEngine() {
            return sslEngine;
          }

          public void setSslEngine(SSLEngine sslEngine) {
            this.sslEngine = sslEngine;
          }

          public ExecutorService getExecutorService() {
            return Minimal.getExecutorService();
          }

          @Override
          public void setFromSsl(ByteBuffer byteBuffer) {
            fromSSL = byteBuffer;
          }


          private WeakReference<byte[]> tasks;

          public WeakReference<byte[]> getTasks() {
            return tasks;
          }

          public void setTasks(WeakReference<byte[]> tasks) {
            this.tasks = tasks;

          }
        };
      }

      /**
       * ssl wants to delegate runnables to the consumer.
       *
       * @return an executor set by 1xio consumer
       */
      public static ExecutorService getExecutorService() {
        return executorService;
      }
    }
  }

  class FSM {
    /**
     * for scale, the gc needs to be other than CGC or the thrashing of keys will produce floating keys based on gc scans.
     */
    static Map<SelectionKey, SslVisitor> sslSessions = new WeakHashMap<>();
    static Map<SelectionKey, HandshakeStatus> sslStatus = new WeakHashMap<>();
    static Map<SelectionKey, ByteBuffer> oobSslBuffers = new WeakHashMap<>();

    public static void assignClientKey(SelectionKey key, String... nonAuthoritativeHostName) {

      ClientSslTask client1 = null;
      try {
        client1 = client(nonAuthoritativeHostName);
        client1.apply(key);
      } catch (Exception e) {
        e.printStackTrace();
      }
      sslSessions.put(key, client1);
    }

    public static final ByteBuffer NIL = ByteBuffer.allocate(0);
    public static Thread selectorThread;
    private static Selector selector;

    /**
     * possible unplanned write occurs during a read request.
     *
     * @param key
     * @param payload
     * @return
     * @throws IOException
     */
    public static int read(SelectionKey key, ByteBuffer payload)
        throws IOException {
      SslVisitor sslVisitor = sslSessions.get(key);
      SSLEngine sslEngine = sslVisitor.getSslEngine();
      SSLSession session = sslEngine.getSession();
      int packetBufferSize = session.getPacketBufferSize();
      int applicationBufferSize = session.getApplicationBufferSize();

      if (null != sslVisitor) {
        HandshakeStatus handshakeStatus = sslStatus.remove(key);
        if (null != handshakeStatus)
          switch (handshakeStatus) {
            case NOT_HANDSHAKING:
              break;
            case FINISHED:
              break;
            case NEED_TASK:
              break;
            case NEED_WRAP:
              ByteBuffer toNet = ByteBuffer.allocateDirect(packetBufferSize);

              SSLEngineResult wrap = sslEngine.wrap(NIL, toNet);

              switch (wrap.getStatus()) {
                case BUFFER_UNDERFLOW:
                  break;
                case BUFFER_OVERFLOW:
                  break;
                case OK:
                  int write = ((SocketChannel) key.channel()).write((ByteBuffer) toNet.flip());
                  System.err.println("wrote " + write);
                case CLOSED:
                  break;
              }
              sslStatus.put(key, wrap.getHandshakeStatus());
              return wrap.bytesConsumed();

          }
        ByteBuffer fromNet=ByteBuffer.allocateDirect(packetBufferSize);
        int read = ((SocketChannel) key.channel()).read(fromNet);
        if(0>read){

          ByteBuffer appBuffer = ByteBuffer.allocateDirect(applicationBufferSize);
          SSLEngineResult wrap = sslEngine.wrap(fromNet, appBuffer);
          switch (wrap.getStatus()) {

            case OK:
          payload.put((ByteBuffer) appBuffer.flip());break;
             case BUFFER_UNDERFLOW:
             case BUFFER_OVERFLOW:
             case CLOSED:
            default:
              return -1;
          }
          return wrap.bytesProduced();

        }
          return 0;//todo: known wrong

      } else
        return ((SocketChannel) key.channel()).read(payload);

    }

    /**
     * possible unplanned write occurs during a write request.
     *
     * @param key
     * @param fromApp
     * @return
     * @throws IOException
     */
    public static int write(SelectionKey key, ByteBuffer fromApp) throws IOException {
      SslVisitor sslVisitor = sslSessions.get(key);
      SSLEngine sslEngine = sslVisitor.getSslEngine();
      SSLSession session = sslEngine.getSession();
      int packetBufferSize = session.getPacketBufferSize();
      int applicationBufferSize = session.getApplicationBufferSize();

      if (null != sslVisitor) {
        HandshakeStatus handshakeStatus = sslStatus.remove(key);
        if (null != handshakeStatus)
          switch (handshakeStatus) {
            case NOT_HANDSHAKING:
              break;
            case FINISHED:
              break;
            case NEED_TASK:
              break;
            case NEED_WRAP:
              break;
            case NEED_UNWRAP:
              ByteBuffer fromNet = ByteBuffer.allocateDirect(packetBufferSize);
              int read = ((ReadableByteChannel) key.channel()).read(fromNet);
              ByteBuffer toApp = ByteBuffer.allocateDirect(applicationBufferSize);
              SSLEngineResult unwrap = sslEngine.unwrap(fromNet, toApp);
              Status status = unwrap.getStatus();
              switch (status) {
                case BUFFER_UNDERFLOW:
                  break;
                case BUFFER_OVERFLOW:
                  break;
                case OK:
                  break;
                case CLOSED:
                  key.cancel();
                  break;
              }

          }
        ByteBuffer toNet = ByteBuffer.allocateDirect(packetBufferSize);

        SSLEngineResult wrap = sslEngine.wrap(fromApp, toNet);

        switch (wrap.getStatus()) {
          case BUFFER_UNDERFLOW:
            break;
          case BUFFER_OVERFLOW:
            break;
          case OK:
            int write = ((SocketChannel) key.channel()).write((ByteBuffer) toNet.flip());
            System.err.println("wrote " + write);
          case CLOSED:
            break;
        }
        sslStatus.put(key, wrap.getHandshakeStatus());

        return wrap.bytesConsumed();
      } else
        return write((SocketChannel) key.channel(), fromApp);
    }

    /**
     * handles SslEngine state NEED_TASK.creates a phaser and launches all threads with invokeAll and
     *
     * @param key
     * @param sslVisitor
     * @param sslEngine
     * @return
     */
    public static int delegateTasks(final SelectionKey key, SslVisitor sslVisitor, SSLEngine sslEngine) {
      final int origOps = key.interestOps();
      List<Callable<Void>> runnables = new ArrayList<>();
      final Phaser phaser = new Phaser();

      Runnable t;
      while (null != (t = sslEngine.getDelegatedTask())) {

        final Runnable finalT1 = t;
        runnables.add(new Callable<Void>() {
          @Override
          public Void call() throws Exception {
            phaser.register();
            finalT1.run();
            phaser.arriveAndDeregister();
            return null;
          }
        });
      }
      runnables.add(new Callable<Void>() {
        @Override
        public Void call() throws Exception {
          phaser.register();
          phaser.arriveAndAwaitAdvance();
          key.interestOps(origOps);
          return null;
        }
      });
      try {
        key.interestOps(0);
        sslVisitor.getExecutorService().invokeAll(runnables);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }


      return 0;
    }

    public static int read(SocketChannel channel, ByteBuffer cursor)
        throws IOException {

      return read(channel.keyFor(selector), cursor);
    }

    public static int write(SocketChannel channel, ByteBuffer payload)
        throws IOException {
      return write(channel.keyFor(getSelector()), payload);

    }

    public static void setSelector(Selector selector) {
      FSM.selector = selector;
    }

    public static Selector getSelector() {
      return selector;
    }
  }

  class Helper {
    public static void toAccept(SelectionKey key, F f) {
      toAccept(key, toAccept(f));
    }

    public static void toAccept(SelectionKey key, Impl impl) {
      key.interestOps(OP_ACCEPT).attach(impl);
      key.selector().wakeup();
    }

    public static void toRead(SelectionKey key, F f) {
      key.interestOps(OP_READ).attach(toRead(f));
      key.selector().wakeup();
    }

    public static void toRead(SelectionKey key, Impl impl) {
      key.interestOps(OP_READ).attach(impl);
      key.selector().wakeup();
    }

    public static void toConnect(SelectionKey key, F f) {
      toConnect(key, toConnect(f));
    }

    public static void toConnect(SelectionKey key, Impl impl) {
      key.interestOps(OP_CONNECT).attach(impl);
      key.selector().wakeup();
    }

    public static void toWrite(SelectionKey key, F f) {
      toWrite(key, toWrite(f));
    }

    public static void toWrite(SelectionKey key, Impl impl) {
      key.interestOps(OP_WRITE).attach(impl);
      key.selector().wakeup();
    }

    public static Impl toAccept(final F f) {
      return new Impl() {
        public void onAccept(SelectionKey key) throws Exception {
          f.apply(key);
        }
      };
    }

    public static Impl toRead(final F f) {
      return new Impl() {

        public void onRead(SelectionKey key) throws Exception {
          f.apply(key);
        }
      };
    }

    public static Impl toWrite(final F f) {
      return new Impl() {

        public void onWrite(SelectionKey key) throws Exception {
          f.apply(key);
        }
      };
    }

    public static Impl toConnect(final F f) {
      return new Impl() {

        public void onConnect(SelectionKey key) throws Exception {
          f.apply(key);
        }
      };
    }

    public interface F {
      void apply(SelectionKey key) throws Exception;
    }

    public static Impl finishRead(final ByteBuffer payload,
                                  final Runnable success) {
      return toRead(new F() {
        public void apply(SelectionKey key) throws Exception {
          if (payload.hasRemaining()) {
            int read = read(key, payload);
            if (-1 == read)
              key.cancel();
          }
          if (!payload.hasRemaining())
            success.run();//warning, will not remove READ_OP from interest.  you are responsible for steering the outcome
        }
      });
    }

    public static void finishRead(SelectionKey key, ByteBuffer payload,
                                  Runnable success) {
      toRead(key, finishRead(payload, success));
    }

    public static ByteBuffer coalesceBuffers(ByteBuffer... payload) {
      ByteBuffer cursor;
      int total = 0;
      if (1 < payload.length) {
        for (int i = 0, payloadLength = payload.length; i < payloadLength; i++) {
          ByteBuffer byteBuffer = payload[i];
          total += byteBuffer.remaining();
        }
        cursor = ByteBuffer.allocateDirect(total);
        for (int i = 0, payloadLength = payload.length; i < payloadLength; i++) {
          ByteBuffer byteBuffer = payload[i];
          cursor.put(byteBuffer);
        }
        cursor.rewind();
      } else cursor = payload[0];
      return cursor;
    }

    public static Impl finishWrite(final Runnable success,
                                   ByteBuffer... payload) {
      final ByteBuffer cursor = coalesceBuffers(payload);
      return toWrite(new F() {
        public void apply(SelectionKey key) throws Exception {
          int write = write(key, cursor);
          if (-1 == write)
            key.cancel();
          if (!cursor.hasRemaining())
            success.run();
        }
      });
    }

    public static Impl finishWrite(ByteBuffer payload, Runnable onSuccess) {
      return finishWrite(onSuccess, payload);
    }

    public static void finishWrite(SelectionKey key, Runnable onSuccess,
                                   ByteBuffer... payload) {
      toWrite(key, finishWrite(onSuccess, payload));
    }

    public static Impl finishRead(final ByteBuffer payload, final F success) {
      return toRead(new F() {
        public void apply(SelectionKey key) throws Exception {
          if (payload.hasRemaining()) {
            int read = read(key, payload);
            if (-1 == read)
              key.cancel();
          }
          if (!payload.hasRemaining())
            success.apply(key);//warning, will not remove READ_OP from interest.  you are responsible for steering the outcome
        }
      });
    }

    public static void finishRead(SelectionKey key, ByteBuffer payload,
                                  F success) {
      toRead(key, finishRead(payload, success));
    }

    public static Impl finishWrite(final F success, ByteBuffer... payload) {
      final ByteBuffer cursor = coalesceBuffers(payload);
      return toWrite(new F() {
        public void apply(SelectionKey key) throws Exception {
          int write = write(key, cursor);
          if (-1 == write)
            key.cancel();
          if (!cursor.hasRemaining())
            success.apply(key);
        }
      });
    }

    public static Impl finishWrite(ByteBuffer payload, F onSuccess) {
      return finishWrite(onSuccess, payload);
    }

    public static void finishWrite(SelectionKey key, F onSuccess,
                                   ByteBuffer... payload) {
      toWrite(key, finishWrite(onSuccess, payload));
    }
  }

  class Impl implements AsioVisitor {
    {
      if ($DBG)
        $origins.put(this, wheresWaldo(4));
    }

    /**
     * tracking aid
     *
     * @param depth typically 2 is correct
     * @return a stack trace string that intellij can hyperlink
     */
    public static String wheresWaldo(int... depth) {
      int d = 0 < depth.length ? depth[0] : 2;
      Throwable throwable = new Throwable();
      Throwable throwable1 = throwable.fillInStackTrace();
      StackTraceElement[] stackTrace = throwable1.getStackTrace();
      StringBuilder ret = new StringBuilder();
      for (int i = 2, end = min(stackTrace.length - 1, d); i <= end; i++) {
        StackTraceElement stackTraceElement = stackTrace[i];
        ret.append("\tat ").append(stackTraceElement.getClassName())
            .append(".").append(stackTraceElement.getMethodName())
            .append("(").append(stackTraceElement.getFileName())
            .append(":").append(stackTraceElement.getLineNumber())
            .append(")\n");

      }
      return ret.toString();
    }

    public void onRead(SelectionKey key) throws Exception {
      System.err.println("fail: " + key.toString());
      int receiveBufferSize = 4 << 10;
      String trim = UTF_8.decode(
          ByteBuffer.allocateDirect(receiveBufferSize)).toString()
          .trim();

      throw new UnsupportedOperationException("found " + trim + " in "
          + getClass().getCanonicalName());
    }

    /**
     * this doesn't change very often for outbound web connections
     *
     * @param key
     * @throws Exception
     */
    public void onConnect(SelectionKey key) throws Exception {
      if (((SocketChannel) key.channel()).finishConnect())
        key.interestOps(OP_WRITE);
    }

    public void onWrite(SelectionKey key) throws Exception {
      SocketChannel channel = (SocketChannel) key.channel();
      System.err.println("buffer underrun?: "
          + channel.socket().getRemoteSocketAddress());
      throw new UnsupportedOperationException("found in "
          + getClass().getCanonicalName());
    }

    /**
     * HIGHLY unlikely to solve a problem with OP_READ | OP_WRITE,
     * each network socket protocol typically requires one or the other but not both.
     *
     * @param key the serversocket key
     * @throws Exception
     */
    public void onAccept(SelectionKey key) throws Exception {

      ServerSocketChannel c = (ServerSocketChannel) key.channel();
      SocketChannel accept = c.accept();
      accept.configureBlocking(false);
      accept.register(key.selector(), OP_READ | OP_WRITE, key
          .attachment());

    }
  }
}
