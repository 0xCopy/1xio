package one.xio;

import one.xio.AsioVisitor.FSM.sslBacklog;
import one.xio.AsioVisitor.Helper.F;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.StrictMath.min;
import static java.nio.channels.SelectionKey.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static one.xio.Pair.pair;

/**
 * User: jim
 * Date: 4/15/12
 * Time: 11:50 PM
 */
public interface AsioVisitor {
  boolean $DBG = "true".equals(Config.get("DEBUG_VISITOR_ORIGINS", "false"));
  Map<Impl, String> $origins = $DBG
      ? new WeakHashMap<Impl, String>()
      : null;

  void onRead(SelectionKey key) throws Exception;

  void onConnect(SelectionKey key) throws Exception;

  void onWrite(SelectionKey key) throws Exception;

  void onAccept(SelectionKey key) throws Exception;
class FSM{
  public static final boolean DEBUG_SENDJSON = Config.get("DEBUG_SENDJSON", "false").equals(
      "true");
  public static Thread selectorThread;
  public static Selector selector;
  public static Map<SelectionKey, SSLEngine> sslState = new WeakHashMap<>();
  /**
   * stores {InterestOps,{attachment}}
   */
  public static Map<SelectionKey, Pair<Integer, Object>> sslGoal = new WeakHashMap<>();
  private static ExecutorService executorService;

  /**
   * handles SslEngine state NEED_TASK.creates a phaser and launches all threads with invokeAll
   */
  public static int delegateTasks(Pair<SelectionKey, SSLEngine> state, final Runnable newOps) {
    final SelectionKey key = state.getA();
    final int origOps = key.interestOps();
    List<Callable<Void>> runnables = new ArrayList<>();
    Runnable t;
    final AtomicReference<CyclicBarrier> barrier = new AtomicReference<>();
    final SSLEngine sslEngine = state.getB();
    while (null != (t = sslEngine.getDelegatedTask())) {
      final Runnable finalT1 = t;
      runnables.add(new Callable<Void>() {
        public Void call() throws Exception {
          finalT1.run();
          barrier.get().await(3, TimeUnit.MINUTES);
          return null;
        }
      });
    }
    barrier.set(new CyclicBarrier(runnables.size(), new Runnable() {
      @Override
      public void run() {
        System.err.println("!!! cyclibarrier hit.  " + sslEngine + " " + sslEngine.getSession() + " " + Arrays.deepToString(sslEngine.getHandshakeSession().getValueNames()));
        try {
          TimeUnit.MILLISECONDS.sleep(250);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
//          sslBacklog.fromNet.on(key, null);
//          sslBacklog.toNet.on(key,null);
        if (null != newOps) {
          System.err.println("!!! firing newOp");
          newOps.run();
        } else {
          System.err.println("!!! waking Up previous op");
          key.interestOps(origOps).selector().wakeup();
        }
      }
    }));

    try {
      key.interestOps(0);
      assert executorService!=null:"must install FSM executorService!";
      executorService.invokeAll(runnables);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return 0;
  }

  public static void setExecutorService(ExecutorService svc) {
    executorService = svc;
  }

  /**
   * this is a beast.
   */
  public static void handShake(final Pair<SelectionKey, SSLEngine> state) {
    if(!state.getA().isValid())return;
    SSLEngine sslEngine = state.getB();

    HandshakeStatus handshakeStatus = sslEngine.getHandshakeStatus();
    System.err.println("hs: " + handshakeStatus);

    switch (handshakeStatus) {
      case NEED_TASK:
        delegateTasks(state, new Runnable() {
          public void run() {
            handShake(state);
          }
        });
        return;
      case NOT_HANDSHAKING:
      case FINISHED:
        SelectionKey key = state.getA();
        Pair<Integer, Object> integerObjectPair = sslGoal.remove(key);
        sslState.put(key, sslEngine);
        Integer a = integerObjectPair.getA();
        Object b = integerObjectPair.getB();
        key.interestOps(a).attach(b);
        key.selector().wakeup();
        return;
      case NEED_WRAP:
        needWrap(state);
        return;
      case NEED_UNWRAP:
        needUnwrap(state);
    }
  }

  public static void needUnwrap(Pair<SelectionKey, SSLEngine> state) {
    SelectionKey key = state.getA();

    SSLEngine sslEngine = state.getB();
    ByteBuffer fromNet = sslBacklog.fromNet.resume(state);
    ByteBuffer toApp = sslBacklog.toApp.resume(state);
    System.err.println("unwrap1: " + fromNet.toString());
    try {
//if(0==fromNet.position()){needNetUnwrap(state);return;}
      SSLEngineResult unwrap = sslEngine.unwrap((ByteBuffer) fromNet.flip(), sslBacklog.toApp.resume(state));
      System.err.println("unwrap2a: " + unwrap);
      System.err.println("unwrap2b: " + fromNet.toString());

      Status status = unwrap.getStatus();
      switch (status) {
        case BUFFER_UNDERFLOW:
          fromNet.compact();
          needNetUnwrap(state);
          break;
        case BUFFER_OVERFLOW:
          sslBacklog.toApp.on(key, doubleBuffer((ByteBuffer) toApp.flip()));
          handShake(state);
          break;
        case OK://sslBacklog.fromNet.on(key, duplicate.compact());
          fromNet.compact();
          handShake(state);
          break;
        case CLOSED:
          throw new Error("client closed");
      }

    } catch (SSLException e) {
      e.printStackTrace();
    }
  }

  private static ByteBuffer doubleBuffer(ByteBuffer src) {
    return ByteBuffer.allocateDirect(src.capacity() << 1).put(src);
  }

  private static void needNetUnwrap(final Pair<SelectionKey, SSLEngine> state) {

    SelectionKey key = state.getA();
    final ByteBuffer fromNet = sslBacklog.fromNet.resume(state);
    final ByteBuffer toApp = sslBacklog.toApp.resume(state);

    Helper.toRead(key, new F() {
      public void apply(SelectionKey key) throws Exception {
        int read = ((SocketChannel) key.channel()).read(fromNet);
        System.err.println("hsread: " + read);
        SSLEngine sslEngine = state.getB();
        System.err.println("nunwrap1a: " + fromNet.toString());
        System.err.println("?<< https://"+UTF_8.decode((ByteBuffer) fromNet.duplicate().flip()));//fromNet.flip()
        SSLEngineResult unwrap = sslEngine.unwrap((ByteBuffer) fromNet.flip(), toApp);
        System.err.println("nunwrap2:" + unwrap);
        System.err.println("nunwrap2a: " + fromNet.toString());
        switch (unwrap.getStatus()) {
          case BUFFER_UNDERFLOW:
            SelectionKey on = sslBacklog.fromNet.on(key, doubleBuffer((ByteBuffer) fromNet.flip()));
            handShake(state);
            break;
          case BUFFER_OVERFLOW:
            sslBacklog.toApp.on(key, doubleBuffer((ByteBuffer) toApp.flip()));
            handShake(state);
            break;
          case OK:
            fromNet.compact();
            break;
          case CLOSED:
            break;
        }
        key.interestOps(0);
        handShake(state);
      }
    });
  }

  public static void needWrap(Pair<SelectionKey, SSLEngine> state) {
    ByteBuffer resume = sslBacklog.toNet.resume(state);
    ByteBuffer toNet = sslBacklog.toNet.resume(state);
    try {

      SSLEngine sslEngine = state.getB();
      SSLEngineResult wrap = sslEngine.wrap(Helper.NIL, toNet);
      System.err.println("wrap2:" + wrap);
      System.err.println("wrap2a: " + toNet.toString());
      if (0 < wrap.bytesProduced()) {
        SelectionKey key = state.getA();
        ((SocketChannel) key.channel()).write((ByteBuffer) toNet.flip());
      }
      toNet.compact();
      handShake(state);
    } catch (SSLException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  enum sslBacklog {
    fromNet, toNet, fromApp, toApp;
    public Map<SelectionKey, ByteBuffer> per = new WeakHashMap<>();

    public ByteBuffer resume(Pair<SelectionKey, SSLEngine> state) {
      SelectionKey key = state.getA();
      ByteBuffer buffer = on(key);
      if (null == buffer) {
        SSLEngine sslEngine = state.getB();
        on(key, buffer = ByteBuffer.allocateDirect(sslEngine.getSession().getPacketBufferSize()));
      }
      return buffer;
    }

    public ByteBuffer on(SelectionKey key) {
      ByteBuffer byteBufferPairPair = per.get(key);
      return byteBufferPairPair;
    }

    public SelectionKey on(SelectionKey key, ByteBuffer buffer) {
      per.put(key, buffer);
      return key;
    }
  }
}
  class Helper {
    public static final ByteBuffer[] NIL = new ByteBuffer[]{};

    public static void toAccept(SelectionKey key, F f) {
      toAccept(key, toAccept(f));
    }

    public static void toAccept(SelectionKey key, Impl impl) {
      toRead(key, impl);
    }

    public static void toRead(SelectionKey key, F f) {
      key.interestOps(OP_READ).attach(toRead(f));
      key.selector().wakeup();
    }

    public static void toRead(SelectionKey key, Impl impl) {
      key.interestOps(OP_ACCEPT).attach(impl);
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

    public static Selector getSelector() {
      return FSM.selector;
    }

    public static void setSelector(Selector selector) {
      FSM.selector = selector;
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
                                   ByteBuffer... payload) {
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

    public static int read(SelectionKey key, ByteBuffer dest) throws IOException {
      if(!key.isValid())return -1;
      SocketChannel channel = (SocketChannel) key.channel();
      int origin = dest.position();

      SSLEngine sslEngine = FSM.sslState.get(key);
      int read = 0;
      if (null != sslEngine) {


        ByteBuffer fromNet = sslBacklog.fromNet.resume(pair(key, sslEngine));
        ByteBuffer toApp = sslBacklog.toApp.resume(pair(key, sslEngine));
        int read1 = channel.read(fromNet);
        if(-1==read1)key.cancel();
        System.err.println("r1: "+read1);
        System.err.println("r1a: from "+fromNet);
        System.err.println("r1b: dest "+dest);
        System.err.println("r1c: ta   "+toApp);
        int destRemaining = dest.remaining();
        boolean backlogWaiting = toApp.flip().hasRemaining()||read1>dest.remaining();
        toApp.compact();
        SSLEngineResult unwrap = null;
        if (backlogWaiting) {


          unwrap = sslEngine.unwrap((ByteBuffer) fromNet.flip(), toApp);
          System.err.println("<<? "+UTF_8.decode((ByteBuffer) toApp.duplicate().flip()));
          int taRemaining = toApp.flip().remaining();
          if (destRemaining > taRemaining) {
            //dest is big enough.
            dest.put(toApp);
            toApp.compact();
          } else {
            //dest is a subset of backlog.
            int position = toApp.position();
            dest.put((ByteBuffer) toApp.slice().limit(destRemaining));
            ((ByteBuffer) toApp.position(position + destRemaining)).compact();
          }
        } else {
          unwrap = sslEngine.unwrap((ByteBuffer) fromNet.flip(), dest);
        }
        read=dest.position()-origin;
        System.err.println("unwrap");
        switch (unwrap.getStatus()) {
          case BUFFER_UNDERFLOW:
            break;
          case BUFFER_OVERFLOW:
            break;
          case OK:
            fromNet.compact();
            System.err.println("r2: "+read1);
            System.err.println("r2a: from "+fromNet);
            System.err.println("r2b: dest "+dest);
            System.err.println("r2c: ta   " + toApp);
            break;
          case CLOSED:
            break;
        }
      } else
        read = channel.read(dest);
      return read;
    }

    public static int write(Channel channel, ByteBuffer fromApp) throws IOException {

      return write(((SocketChannel) channel).keyFor(getSelector()), fromApp);

    }

    public static int write(SelectionKey key, ByteBuffer fromApp) throws IOException {
      SSLEngine sslEngine = FSM.sslState.get(key);
      int write = 0;
      if (null != sslEngine) {
        try {
          ByteBuffer toNet = sslBacklog.toNet.resume(pair(key, sslEngine));
          System.err.println("w:fa:0 " + fromApp);
          System.err.println("w:tn:0 " + toNet);
          SSLEngineResult sslEngineResult = sslEngine.wrap(fromApp, toNet);
          System.err.println("w:r:1 " + sslEngineResult);
          System.err.println("w:fa:1 " + fromApp);
          System.err.println("w:tn:1 " + toNet);
          Status status = sslEngineResult.getStatus();
          switch (status) {
            case BUFFER_UNDERFLOW:
              break;
            case BUFFER_OVERFLOW:
              break;
            case OK:
              HandshakeStatus handshakeStatus = sslEngineResult.getHandshakeStatus();
              switch (handshakeStatus) {
                case NOT_HANDSHAKING:
                case FINISHED:
                  write=sslEngineResult.bytesConsumed();
                  System.err.println("w:w:2 " +  ((SocketChannel) key.channel()).write((ByteBuffer) toNet.flip()));
                  System.err.println("w:tn:2 " + toNet);
                  toNet.compact();
                  System.err.println("w:tn:3 " + toNet);
                  break;
                case NEED_TASK:
                case NEED_WRAP:
                case NEED_UNWRAP:
                  FSM.sslGoal.put(key, new Pair<>(key.interestOps(), key.attachment()));
                  Pair<SelectionKey, SSLEngine> state = pair(key, sslEngine);
                  FSM.handShake(state);
                  break;
              }
            case CLOSED:
              break;
          }
        } catch (SSLException e) {
          e.printStackTrace();
        }
      } else
        write = ((SocketChannel) key.channel()).write(fromApp);

      return write;

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
