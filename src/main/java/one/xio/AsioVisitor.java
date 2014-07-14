package one.xio;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;

import static java.lang.StrictMath.min;
import static java.nio.channels.SelectionKey.*;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * User: jim
 * Date: 4/15/12
 * Time: 11:50 PM
 */
public interface AsioVisitor {
  boolean $DBG = null != System.getenv("DEBUG_VISITOR_ORIGINS");
  Map<Impl, String> $origins = $DBG
      ? new WeakHashMap<Impl, String>()
      : null;

  void onRead(SelectionKey key) throws Exception;

  void onConnect(SelectionKey key) throws Exception;

  void onWrite(SelectionKey key) throws Exception;

  void onAccept(SelectionKey key) throws Exception;

  class Helper {     public static Thread selectorThread;
    public static Selector selector;
    private static Map<SelectionKey, SSLEngine> sslState = new WeakHashMap<>();
    /**
     * stores {InterestOps,{attachment}}
     */
    public static Map <SelectionKey, Pair<Integer,Object>>sslGoal=new WeakHashMap<>();

    /**
   * handles SslEngine state NEED_TASK.creates a phaser and launches all threads with invokeAll  
   *
   * @param key
   * @param sslEngine
   * @param executorService
   * @param newOps
 *@return
   */
  public static int delegateTasks(final SelectionKey key, SSLEngine sslEngine, ExecutorService executorService, final Runnable newOps) {
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
          phaser.arrive();
          return null;
        }
      });
    }
    phaser.register();
    runnables.add(new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        phaser.awaitAdvanceInterruptibly(phaser.arrive(),10, TimeUnit.DAYS);
        if(newOps!=null)newOps.run();else key.interestOps(origOps ).selector().wakeup();
        return null;
      }
    });
    try {
      key.interestOps(0);
      executorService.invokeAll(runnables);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return 0;
  }

    public static void toAccept(SelectionKey key, final F f) {
      toAccept(key, toAccept(f));
    }

    public static void toAccept(SelectionKey key, Impl impl) {
      toRead(key, impl);
    }

    public static void toRead(SelectionKey key, final F f) {
      key.interestOps(OP_READ).attach(toRead(f));
      key.selector().wakeup();
    }

    public static void toRead(SelectionKey key, Impl impl) {
      key.interestOps(OP_ACCEPT).attach(impl);
      key.selector().wakeup();
    }

    public static void toConnect(SelectionKey key, final F f) {
      toConnect(key, toConnect(f));
    }

    public static void toConnect(SelectionKey key, Impl impl) {
      key.interestOps(OP_CONNECT).attach(impl);
      key.selector().wakeup();
    }

    public static void toWrite(SelectionKey key, final F f) {
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

    public static Selector getSelector() {
      return selector;
    }

    public static void setSelector(Selector selector) {
      Helper.selector = selector;
    }

    public static Impl finishRead(final ByteBuffer payload,
                                  final Runnable success) {
      return Helper.toRead(new F() {
        public void apply(SelectionKey key) throws Exception {
          if (payload.hasRemaining()) {
            int read = Helper.read(key, payload);
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
      } else {
        cursor = payload[0];
      }
      return cursor;
    }

    public static Impl finishWrite(final Runnable success,
                                   final ByteBuffer... payload) {
      final ByteBuffer cursor = coalesceBuffers(payload);
      return toWrite(new F() {
        public void apply(SelectionKey key) throws Exception {
          int write = write(key,cursor);
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
      return Helper.toRead(new F() {
        public void apply(SelectionKey key) throws Exception {
          if (payload.hasRemaining()) {
            int read = Helper.read(key, payload);
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

    public static Impl finishWrite(final F success,
                                   final ByteBuffer... payload) {
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

    public static int read(Channel channel, ByteBuffer fromNet) throws IOException {

        return read(((SocketChannel) channel).keyFor(getSelector()), fromNet);

    }

    public static int read(SelectionKey key, ByteBuffer fromNet) throws IOException {
      SocketChannel channel = (SocketChannel) key.channel();
      int origin = fromNet.position();
      int read = channel.read( fromNet);
      if (-1 == read) channel.close();

      SSLEngine sslEngine = sslState.get(key);
      if (null != sslEngine) {

        int packetBufferSize = sslEngine.getSession().getPacketBufferSize();
        if (fromNet.remaining() < packetBufferSize) {
          throw new IOException("Underrun potential in SSL read.  please resize inbound buffer to AsioVisitor.Helper.sslState.get(key).getSession().getPacketBufferSize()");
        }
        int applicationBufferSize = sslEngine.getSession().getApplicationBufferSize();
        ByteBuffer byteBuffer1 = ByteBuffer.allocateDirect(applicationBufferSize);//todo: recycle
        fromNet.clear();
        fromNet.put((ByteBuffer) byteBuffer1.flip());
        read = fromNet.limit() - origin;

        SSLEngineResult sslEngineResult = sslEngine.unwrap((ByteBuffer) fromNet.flip(), byteBuffer1);
        HandshakeStatus handshakeStatus = sslEngineResult.getHandshakeStatus();
        Status status = sslEngineResult.getStatus();
        switch (status) {
          case BUFFER_UNDERFLOW:
            break;
          case BUFFER_OVERFLOW:
            break;
          case OK:
            break;
          case CLOSED:
            channel.socket().close();
            break;
        }
        if (handshakeStatus != null)
          switch (handshakeStatus) {
            case NOT_HANDSHAKING:
            case FINISHED:
              break;
            case NEED_TASK:
             case NEED_WRAP:
            case NEED_UNWRAP:
              throw new IOException("renegotiation requested from peer.  not implemented here for security reasons.");
          }
      }
      return read;
    }

    public  static int write(Channel channel, ByteBuffer fromApp) throws IOException {

        return write(((SocketChannel) channel).keyFor(getSelector()), fromApp);

     }

    public  static int write(SelectionKey key, ByteBuffer fromApp) throws IOException {

      SocketChannel channel = (SocketChannel) key.channel();
      int origin = fromApp.position();
      SSLEngine sslEngine = sslState.get(key);
      int write = 0;
      if (null != sslEngine) {
        try {
          ByteBuffer toNet = ByteBuffer.allocateDirect(sslEngine.getSession().getApplicationBufferSize());
          SSLEngineResult sslEngineResult = sslEngine.wrap(fromApp, toNet);

          HandshakeStatus handshakeStatus = sslEngineResult.getHandshakeStatus();
          switch (handshakeStatus) {
            case NOT_HANDSHAKING:
            case FINISHED:
              break;
            case NEED_TASK:
             case NEED_WRAP:
            case NEED_UNWRAP:
              throw new IOException("SSL Renegotiation attempt not supported. (insecure and unnecessary practice)");
          }
          Status status = sslEngineResult.getStatus();
          switch (status) {
            case BUFFER_UNDERFLOW:
              break;
            case BUFFER_OVERFLOW:
              break;
            case OK:
              write = ((SocketChannel) key.channel()).write(toNet);
            case CLOSED:
              break;
          }
        } catch (SSLException e) {
          e.printStackTrace();
        }
      }
      else
        write=((SocketChannel)key.channel()).write(fromApp);

      return write;

    }

    enum sslBacklog {
      fromNet, toNet, fromApp, toApp;
      public Map<SelectionKey, Pair<ByteBuffer, Pair<ByteBuffer, Pair>>> per = new WeakHashMap<>();

      public Pair<ByteBuffer, Pair<ByteBuffer, Pair>> on(SelectionKey key) {
        Pair<ByteBuffer, Pair<ByteBuffer, Pair>> byteBufferPairPair = per.get(key);
        return byteBufferPairPair;
      }

      public SelectionKey on(SelectionKey key, Pair<ByteBuffer, Pair<ByteBuffer, Pair>> slist) {
        per.put(key, slist);
        return key;
      }
    }
    public interface F {
      void apply(SelectionKey key) throws Exception;
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
