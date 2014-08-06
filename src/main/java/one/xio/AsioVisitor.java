package one.xio;

import one.xio.AsioVisitor.FSM.sslBacklog;
import one.xio.AsioVisitor.Helper.Do.post;
import one.xio.AsioVisitor.Helper.Do.pre;
import one.xio.AsyncSingletonServer.SingleThreadSingletonServer;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import java.net.InetSocketAddress;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.StrictMath.min;
import static java.nio.channels.SelectionKey.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static one.xio.AsioVisitor.Helper.Do.pre.flip;
import static one.xio.AsioVisitor.Helper.REALTIME_CUTOFF;
import static one.xio.AsioVisitor.Helper.REALTIME_UNIT;
import static one.xio.AsioVisitor.Helper.on;
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

  class FSM {
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
    public static void delegateTasks(final Pair<SelectionKey, SSLEngine> state) throws InterruptedException {
      SelectionKey key = state.getA();
      List<Callable<Void>> runnables = new ArrayList<>();
      Runnable t;
      final AtomicReference<CyclicBarrier> barrier = new AtomicReference<>();
      SSLEngine sslEngine = state.getB();
      while (null != (t = sslEngine.getDelegatedTask())) {
        final Runnable finalT1 = t;
        runnables.add(new Callable<Void>() {
          public Void call() throws Exception {
            finalT1.run();
            barrier.get().await(REALTIME_CUTOFF, REALTIME_UNIT);
            return null;
          }
        });
      }
      barrier.set(new CyclicBarrier(runnables.size(), new Runnable() {
        @Override
        public void run() {
          try {
            handShake(state);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }));

      assert executorService != null : "must install FSM executorService!";
      executorService.invokeAll(runnables);

    }

    public static void setExecutorService(ExecutorService svc) {
      executorService = svc;
    }

    /**
     * this is a beast.
     */
    public static void handShake(Pair<SelectionKey, SSLEngine> state) throws Exception {
      if (!state.getA().isValid()) return;
      SSLEngine sslEngine = state.getB();

      HandshakeStatus handshakeStatus = sslEngine.getHandshakeStatus();
      System.err.println("hs: " + handshakeStatus);

      switch (handshakeStatus) {
        case NEED_TASK:
          delegateTasks(state);
          break;
        case NOT_HANDSHAKING:
        case FINISHED:
          SelectionKey key = state.getA();
          Pair<Integer, Object> integerObjectPair = sslGoal.remove(key);
          sslState.put(key, sslEngine);
          Integer a = integerObjectPair.getA();
          Object b = integerObjectPair.getB();
          key.interestOps(a).attach(b);
          key.selector().wakeup();
          break;
        case NEED_WRAP:
          needWrap(state);
          break;
        case NEED_UNWRAP:
          needUnwrap(state);
          break;
      }
    }

    public static void needUnwrap(final Pair<SelectionKey, SSLEngine> state) throws Exception {
      final ByteBuffer fromNet = sslBacklog.fromNet.resume(state);
      ByteBuffer toApp = sslBacklog.toApp.resume(state);
      SSLEngine sslEngine = state.getB();
      SSLEngineResult unwrap = sslEngine.unwrap((ByteBuffer) fromNet.flip(), toApp);
      System.err.println("" + unwrap);
      fromNet.compact();

      Status status = unwrap.getStatus();
      SelectionKey key = state.getA();
      switch (status) {
        case BUFFER_UNDERFLOW:
          key.interestOps(OP_READ).attach(new Impl() {
            public void onRead(SelectionKey key) throws Exception {
              int read = ((SocketChannel) key.channel()).read(fromNet);
              if (-1 == read) {
                key.cancel();
              } else {
                handShake(state);
              }
            }
          });
          key.selector().wakeup();
          break;
        case OK:
          handShake(state);

          break;
        case BUFFER_OVERFLOW:
          handShake(state);
          break;
        case CLOSED:
          state.getA().cancel();
          break;

      }
    }


    public static void needWrap(Pair<SelectionKey, SSLEngine> state) throws Exception {

      ByteBuffer toNet = sslBacklog.toNet.resume(state);
      ByteBuffer fromApp = sslBacklog.fromApp.resume(state);

      SSLEngine sslEngine = state.getB();
      SSLEngineResult wrap = sslEngine.wrap(on(fromApp, flip), toNet);
      System.err.println("wrap: " + wrap);
      switch (wrap.getStatus()) {
        case BUFFER_UNDERFLOW:
          throw new Error("not supposed to happen here");
        case OK:
          SocketChannel channel = (SocketChannel) state.getA().channel();
          channel.write((ByteBuffer) toNet.flip());
          toNet.compact();
          fromApp.compact();
          handShake(state);
          return;
        case BUFFER_OVERFLOW:
          throw new Error("buffer size impossible");
        case CLOSED:
          state.getA().cancel();
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
    public static final TimeUnit REALTIME_UNIT = TimeUnit.valueOf(Config.get("REALTIME_UNIT", TimeUnit.MINUTES.name()));
    public static final Integer REALTIME_CUTOFF = Integer.parseInt(Config.get("REALTIME_CUTOFF", "3"));


    /**
     * called once client is connected, but before any bytes are read or written from socket.
     *
     * @param host        ssl remote host
     * @param port        ssl remote port
     * @param asioVisitor
     * @param clientOps   ussually OP_WRITE but OP_READ for non-http protocols as well
     * @throws Exception
     */
    public static void sslClient(final String host, final int port, final Impl asioVisitor, final int clientOps) throws Exception {
      SocketChannel open = SocketChannel.open();
      open.configureBlocking(false);
      open.connect(new InetSocketAddress(host, port));
      finishConnect(open, new F() {
        @Override
        public void apply(SelectionKey key) throws Exception {
          SSLEngine sslEngine = SSLContext.getDefault().createSSLEngine(host, port);
          sslEngine.setUseClientMode(true);
          sslEngine.setWantClientAuth(false);
          FSM.sslState.put(key, sslEngine);
          FSM.sslGoal.put(key, Pair.<Integer, Object>pair(clientOps, asioVisitor));
          FSM.needWrap(pair(key, sslEngine));
        }
      });

    }

    public static ByteBuffer coalesceBuffers(List<ByteBuffer> byteBuffers) {
      ByteBuffer[] byteBuffers1 = byteBuffers.toArray(new ByteBuffer[byteBuffers.size()]);
      return coalesceBuffers(byteBuffers1);
    }

    /**
     * some kind of less painful way to do byteBuffer operations and a few new ones thrown in.
     * <p/>
     * evidence that this can be shorter
     * <pre>
     *
     * res.add(on(nextChunk, rewind));
     * res.add((ByteBuffer) nextChunk.rewind());
     *
     *
     * </pre>
     */
    public interface Do {
      <T extends ByteBuffer> T perform(T target);

      enum pre implements Do {
        duplicate {
          @Override
          public <T extends ByteBuffer> T perform(T target) {
            return (T) target.duplicate();
          }
        }, flip {
          @Override
          public <T extends ByteBuffer> T perform(T target) {
            return (T) target.flip();
          }
        }, slice {
          @Override
          public <T extends ByteBuffer> T perform(T target) {
            return (T) target.slice();
          }
        }, mark {
          @Override
          public <T extends ByteBuffer> T perform(T target) {
            return (T) target.mark();
          }
        }, reset {
          @Override
          public <T extends ByteBuffer> T perform(T target) {
            return (T) target.reset();
          }
        }, rewind {
          @Override
          public <T extends ByteBuffer> T perform(T target) {
            return (T) target.rewind();
          }
        },
        /**
         * rewinds, dumps to console but returns unchanged buffer
         */
        debug {
          @Override
          public <T extends ByteBuffer> T perform(T target) {
            System.err.println("%%: " + asString(target, duplicate, rewind));
            return target;
          }
        }, ro {
          @Override
          public <T extends ByteBuffer> T perform(T target) {
            return (T) target.asReadOnlyBuffer();
          }
        },

        /**
         * perfoms get until non-ws returned.  then backtracks.by one.
         * <p/>
         * <p/>
         * resets position and throws BufferUnderFlow if runs out of space before success
         */


        forceSkipWs {
          @Override
          public <T extends ByteBuffer> T perform(T target) {
            int position = target.position();

            while (target.hasRemaining() && Character.isWhitespace(target.get()))
              ;
            if (!target.hasRemaining()) {
              target.position(position);
              throw new BufferUnderflowException();
            }
            return on(target, back1);
          }
        },
        skipWs {
          @Override
          public <T extends ByteBuffer> T perform(T target) {
            int position = target.position();

            while (target.hasRemaining() && Character.isWhitespace(target.get()))
              ;

            return on(target, back1);
          }
        },
        toWs {
          @Override
          public <T extends ByteBuffer> T perform(T target) {
            while (target.hasRemaining() && !Character.isWhitespace(target.get()))
              ;
            return target;
          }
        },
        /**
         * @throws java.nio.BufferUnderflowException if EOL was not reached
         */
        forceToEol{
          @Override
          public <T extends ByteBuffer> T perform(T target) {
            while (target.hasRemaining() && '\n' != target.get())
              ;
            if (!target.hasRemaining())
              throw new BufferUnderflowException();
            return target;
          }
        },
        /**
         * @throws java.nio.BufferUnderflowException if EOL was not reached
         */
        toEol {
          @Override
          public <T extends ByteBuffer> T perform(T target) {
            while (target.hasRemaining() && '\n' != target.get());
            return target;
          }
        },
        back1 {
          @Override
          public <T extends ByteBuffer> T perform(T target) {
            int position = target.position();
            return (T) (position > 0 ? target.position(position - 1) : target);
          }
        },
        /**
         * reverses position _up to_ 2.
         */
        back2 {
          @Override
          public <T extends ByteBuffer> T perform(T target) {
            int position = target.position();
            return (T) (position > 1 ? target.position(position - 2) : on(target, back1));
          }
        }, /**
         * reduces the position of target until the character is non-white.
         */rtrim {
          @Override
          public <T extends ByteBuffer> T perform(T target) {
            int start = target.position(), i = start;
            while (--i >= 0 && Character.isWhitespace(target.get(i)))
              ;

            return (T) target.position(++i);
          }
        }, skipDigits {
          @Override
          public <T extends ByteBuffer> T perform(T target) {
            while (target.hasRemaining() && Character.isDigit(target.get()))
              ;
            return target;
          }
        }
      }

      enum post implements Do {
        compact {
          @Override
          public <T extends ByteBuffer> T perform(T target) {
            return (T) target.compact();
          }
        }, reset {
          @Override
          public <T extends ByteBuffer> T perform(T target) {
            return (T) target.reset();
          }
        }, rewind {
          @Override
          public <T extends ByteBuffer> T perform(T target) {
            return (T) target.rewind();
          }
        }, clear {
          @Override
          public <T extends ByteBuffer> T perform(T target) {
            return (T) target.clear();
          }

        }, grow {
          @Override
          public <T extends ByteBuffer> T perform(T target) {
            return (T) growBuffer(target);
          }

        }, ro {
          @Override
          public <T extends ByteBuffer> T perform(T target) {
            return (T) target.asReadOnlyBuffer();
          }
        },
        /**
         * fills remainder of buffer to 0's
         */

        pad0 {
          @Override
          public <T extends ByteBuffer> T perform(T target) {
            while (target.hasRemaining()) {
              target.put((byte) 0);
            }
            return target;
          }
        },
        /**
         * fills prior bytes to current position with 0's
         */

        pad0Until {
          @Override
          public <T extends ByteBuffer> T perform(T target) {
            int limit = target.limit();
            target.flip();
            while (target.hasRemaining()) {
              target.put((byte) 0);
            }
            return (T) target.limit(limit);
          }
        }
      }
    }

    public static <T extends ByteBuffer> T on(T b, Do... ops) {for (int i = 0, opsLength = ops.length; i < opsLength; i++) {Do op = ops[i];b = op.<T>perform(b);}return b;}

    /**
     * convenience method
     *
     * @param bytes
     * @param operations
     * @return
     */
    public static String asString(ByteBuffer bytes, Do... operations) {
      for (Do operation : operations) {
        if (operation instanceof pre) {
          bytes = operation.perform(bytes);
        }
      }
      String s = UTF_8.decode(bytes).toString();
      for (Do operation : operations) {
        if (operation instanceof post) {
          bytes = operation.perform(bytes);
        }
      }
      return s;
    }

    /**
     * convenience method
     *
     * @param src
     * @param operations
     * @return
     */
    public static ByteBuffer asBuffer(String src, Do... operations) {

      ByteBuffer byteBuffer = UTF_8.encode(src);
      for (Do operation : operations) {
        byteBuffer = operation.perform(byteBuffer);
      }
      return byteBuffer;
    }


    public static void toRead(SelectionKey key, F f) {
      key.interestOps(OP_READ).attach(toRead(f));
      key.selector().wakeup();
    }

    public static void toRead(SelectionKey key, Impl impl) {
      //toRead
      key.interestOps(OP_READ).attach(impl);
      key.selector().wakeup();
    }

    public static void toConnect(SelectionKey key, F f) {
      toConnect(key, toConnect(f));
    }

    public static void toConnect(SelectionKey key, Impl impl) {//toConnect
      key.interestOps(OP_CONNECT).attach(impl);
      key.selector().wakeup();
    }

    public static void finishConnect(String host, int port, F onSuccess) throws Exception {      //finishConnect

      SocketChannel open = SocketChannel.open();
      open.configureBlocking(false);
      open.connect(new InetSocketAddress(host, port));
      finishConnect(open, onSuccess);
    }

    public static void finishConnect(final SocketChannel channel, final F onSuccess) throws Exception {
      //finishConnect
      SingleThreadSingletonServer.enqueue(channel, OP_CONNECT,
          toConnect(new F() {
            @Override
            public void apply(SelectionKey key) throws Exception {
              if (channel.finishConnect()) {
                onSuccess.apply(key);
              }
            }
          }));
    }

    public static void toWrite(SelectionKey key, F f) {
      //toWrite
      toWrite(key, toWrite(f));
    }

    public static void toWrite(SelectionKey key, Impl impl) {
      //toWrite
      key.interestOps(OP_WRITE).attach(impl);
      key.selector().wakeup();
    }


    public static Impl toRead(final F f) {
      return new Impl() {
        public void onRead(SelectionKey key) throws Exception {
          //toRead begin
          f.apply(key);
          //toRead end
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
      //toConnect
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
      //finishRead
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
      if (payload.hasRemaining())
        toRead(key, finishRead(payload, success));
      else
        success.run();
    }

    public static ByteBuffer coalesceBuffers(ByteBuffer... src) {
      ByteBuffer cursor;
      int total = 0;
      if (1 < src.length) {
        for (int i = 0, payloadLength = src.length; i < payloadLength; i++) {
          ByteBuffer byteBuffer = src[i];
          total += byteBuffer.remaining();
        }
        cursor = ByteBuffer.allocateDirect(total);
        for (int i = 0, payloadLength = src.length; i < payloadLength; i++) {
          ByteBuffer byteBuffer = src[i];
          cursor.put(byteBuffer);
        }
        cursor.rewind();
      } else {
        cursor = src[0];
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
      if (payload.hasRemaining())
        toRead(key, finishRead(payload, success));
      else try {
        success.apply(key);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    public static Impl finishWrite(final F success,
                                   ByteBuffer... src1) {
      final ByteBuffer src = coalesceBuffers(src1);
      return toWrite(new F() {
        public void apply(SelectionKey key) throws Exception {
          int write = write(key, src);
          if (-1 == write)
            key.cancel();
          if (!src.hasRemaining())
            success.apply(key);
        }
      });
    }

    public static void finishWrite(ByteBuffer payload, F onSuccess) {
      finishWrite(onSuccess, payload);
    }

    public static void finishWrite(SelectionKey key, F onSuccess,
                                   ByteBuffer... payload) {
      ByteBuffer cursor = coalesceBuffers(payload);
      if (cursor.hasRemaining())
        toWrite(key, finishWrite(onSuccess, cursor));
      else
        try {
          onSuccess.apply(key);
        } catch (Exception e) {
          e.printStackTrace();
        }
    }

    public static int read(Channel channel, ByteBuffer fromNet) throws Exception {
      return read(((SocketChannel) channel).keyFor(getSelector()), fromNet);
    }

    public static int write(Channel channel, ByteBuffer fromApp) throws Exception {
      return write(((SocketChannel) channel).keyFor(getSelector()), fromApp);
    }

    public static int write(SelectionKey key, ByteBuffer src) throws Exception {
      SSLEngine sslEngine = FSM.sslState.get(key);
      int write = 0;
      if (null == sslEngine) {
        write = ((SocketChannel) key.channel()).write(src);
        return write;
      }
      ByteBuffer toNet = sslBacklog.toNet.resume(pair(key, sslEngine));
      ByteBuffer fromApp = sslBacklog.fromApp.resume(pair(key, sslEngine));
      ByteBuffer origin = src.duplicate();
      cat(src, fromApp);
      SSLEngineResult wrap = sslEngine.wrap((ByteBuffer) fromApp.flip(), toNet);
      fromApp.compact();
      System.err.println("write:wrap: " + wrap);


      switch (wrap.getHandshakeStatus()) {
        case NOT_HANDSHAKING:
        case FINISHED:
          Status status = wrap.getStatus();
          switch (status) {
            case BUFFER_OVERFLOW:
            case OK:
              SocketChannel channel = (SocketChannel) key.channel();
              int ignored = channel.write((ByteBuffer) toNet.flip());
              toNet.compact();

              int i = src.position() - origin.position();
              return i;
            case CLOSED:
              key.cancel();
              return -1;
          }
          break;
        case NEED_TASK:
        case NEED_WRAP:
        case NEED_UNWRAP:
          sslPush(key, sslEngine);
          break;
      }

      return 0;
    }

    public static int read(SelectionKey key, ByteBuffer toApp) throws Exception {
      SSLEngine sslEngine = FSM.sslState.get(key);
      int read = 0;
      if (null == sslEngine) {
        read = ((SocketChannel) key.channel()).read(toApp);
        return read;
      }
      ByteBuffer fromNet = sslBacklog.fromNet.resume(pair(key, sslEngine));
      read = ((SocketChannel) key.channel()).read(fromNet);
      ByteBuffer overflow = sslBacklog.toApp.resume(pair(key, sslEngine));
      ByteBuffer origin = toApp.duplicate();
      SSLEngineResult unwrap = sslEngine.unwrap(on(fromNet, flip), overflow);
      cat(on(overflow,flip), toApp);
      if(overflow.hasRemaining())System.err.println("**!!!* sslBacklog.toApp retaining "+overflow.remaining()+" bytes");
      overflow.compact();
      fromNet.compact();
      System.err.println("read:unwrap: " + unwrap);
      Status status = unwrap.getStatus();
      switch (unwrap.getHandshakeStatus()) {
        case NOT_HANDSHAKING:
        case FINISHED:
          switch (status) {
            case BUFFER_UNDERFLOW:
              if (-1 == read) key.cancel();

            case OK:
              int i = toApp.position() - origin.position();
              return i;

            case CLOSED:
              key.cancel();
              return -1;

          }

          break;
        case NEED_TASK:
        case NEED_WRAP:
        case NEED_UNWRAP:
          sslPush(key, sslEngine);
          break;
      }

      return 0;
    }

    public static void sslPop(SelectionKey key) {
      Pair<Integer, Object> remove = FSM.sslGoal.remove(key);
      key.interestOps(remove.getA()).attach(remove.getB());
      key.selector().wakeup();
    }

    public static void sslPush(SelectionKey key, SSLEngine engine) throws Exception {
      FSM.sslGoal.put(key, pair(key.interestOps(), key.attachment()));
      FSM.handShake(pair(key, engine));
    }

    public static ByteBuffer cat(ByteBuffer src, ByteBuffer dest) {
      int need = src
          .remaining(),
          have = dest.remaining();
      if (have > need) return dest.put(src);
      dest.put((ByteBuffer) src.slice().limit(have));
      src.position(src.position() + have);
      return dest;

    }

    public static ByteBuffer growBuffer(ByteBuffer src) {
      return ByteBuffer.allocateDirect(src.capacity() << 1).put(src);
    }

    /*
    public static void debugBuf(ByteBuffer nextChunk) {
          on(nextChunk, pre.debug);
        }
        */
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
