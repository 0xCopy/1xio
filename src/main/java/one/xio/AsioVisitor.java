package one.xio;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLSessionContext;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Map;
import java.util.WeakHashMap;

import static java.lang.StrictMath.min;
import static java.nio.channels.SelectionKey.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.net.ssl.SSLEngineResult.HandshakeStatus;
import static one.xio.AsioVisitor.Helper.*;

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
		ByteBuffer getFromSSL();

		ByteBuffer getToSSL();

		SSLEngine getSslEngine();

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
			public static ClientSslTask server(
					final String... nonAuthoritativeHostname) {
				return new ClientSslTask() {
					private ByteBuffer fromSSL = ByteBuffer
							.allocateDirect(4 << 10);
					private ByteBuffer ToSSL = ByteBuffer.allocate(4 << 10);
					private SSLEngine sslEngine;
					private SSLSessionContext sessionContext;

					@Override
					public void apply(SelectionKey key) throws Exception {
						sslEngine = (nonAuthoritativeHostname.length > 0
								? nonAuthoritativeHostname
								: null) == null
								? SSLContext.getDefault().createSSLEngine()
								: SSLContext
										.getDefault()
										.createSSLEngine(
												(nonAuthoritativeHostname.length > 0
														? nonAuthoritativeHostname
														: null)[0],
												((InetSocketAddress) ((SocketChannel) key
														.channel())
														.getRemoteAddress())
														.getPort());
						sessionContext = SSLContext.getDefault()
								.getClientSessionContext();
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
				};
			}

			public static ClientSslTask client(
					final String... nonAuthoritativeHostname) {
				return new ClientSslTask() {
					private ByteBuffer fromSSL = ByteBuffer
							.allocateDirect(4 << 10);
					private ByteBuffer ToSSL = ByteBuffer.allocate(4 << 10);
					private SSLEngine sslEngine;
					private SSLSessionContext sessionContext;

					@Override
					public void apply(SelectionKey key) throws Exception {
						sslEngine = (nonAuthoritativeHostname.length > 0
								? nonAuthoritativeHostname
								: null) == null
								? SSLContext.getDefault().createSSLEngine()
								: SSLContext
										.getDefault()
										.createSSLEngine(
												(nonAuthoritativeHostname.length > 0
														? nonAuthoritativeHostname
														: null)[0],
												((InetSocketAddress) ((SocketChannel) key
														.channel())
														.getRemoteAddress())
														.getPort());
						sessionContext = SSLContext.getDefault()
								.getClientSessionContext();

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
				};
			}
		}
	}

	class FSM {
		/**
		 * for scale, the gc needs to be other than CGC or the thrashing of keys will produce floating keys based on gc scans.
		 */
		static Map<SelectionKey, SslVisitor> sslSessions = new WeakHashMap<SelectionKey, SslVisitor>();
		static Map<SelectionKey, HandshakeStatus> sslStatus = new WeakHashMap<SelectionKey, HandshakeStatus>();
		static Map<SelectionKey, ByteBuffer> oob = new WeakHashMap<SelectionKey, ByteBuffer>();

		public static final ByteBuffer NIL = ByteBuffer.allocate(0);

		/**
		 * possible unplanned write occurs during a read request.
		 *
		 * @param key
		 * @param payload
		 * @return
		 * @throws IOException
		 */
		public static int read(final SelectionKey key, ByteBuffer payload)
				throws IOException {
			HandshakeStatus handshakeStatus1 = sslStatus.get(sslStatus);

			SslVisitor sslVisitor = sslSessions.get(key);
			if (null != sslVisitor) {
				SSLEngine sslEngine = sslVisitor.getSslEngine();
				if (HandshakeStatus.NEED_WRAP == sslStatus.get(key)) {
					ByteBuffer toSSL = sslVisitor.getToSSL();
					SSLEngineResult wrap = sslEngine.wrap((ByteBuffer) NIL
							.rewind(), (ByteBuffer) toSSL);
					((SocketChannel) key.channel()).write((ByteBuffer) toSSL
							.flip());
					HandshakeStatus handshakeStatus = wrap.getHandshakeStatus();
					sslStatus.put(key, handshakeStatus);

					toSSL.compact();
					return 0;
				} else {
					ByteBuffer byteBuffer = oob.get(key);
					if (null != byteBuffer) {
						int remaining = payload.remaining();
						if (byteBuffer.remaining() <= remaining) {
							payload.put(byteBuffer);
							oob.remove(key);
						} else {
							payload.put((ByteBuffer) byteBuffer.slice().limit(
									remaining));
							byteBuffer.position(byteBuffer.position()
									+ remaining);
							byteBuffer.compact();
						}
					}
					ByteBuffer fromSSL = sslVisitor.getFromSSL();
					SSLEngineResult unwrap = sslEngine.unwrap(
							(ByteBuffer) fromSSL.flip(), payload);//todo: if oob triggers,and payload has 0 remaining, will this pop?
					int read = ((SocketChannel) key.channel()).read(fromSSL);
					HandshakeStatus handshakeStatus = unwrap
							.getHandshakeStatus();
					sslStatus.put(key, handshakeStatus);
					fromSSL.compact();
					return unwrap.bytesProduced();
				}
			}
			return read((ReadableByteChannel) key.channel(), payload);
		}

		/**
		 * possible unplanned write occurs during a write request.
		 *
		 * @param key
		 * @param payload
		 * @return
		 * @throws IOException
		 */
		public static int write(final SelectionKey key, ByteBuffer payload)
				throws IOException {
			HandshakeStatus handshakeStatus1 = sslStatus.get(sslStatus);

			SslVisitor sslVisitor = sslSessions.get(key);
			if (null != sslVisitor) {
				SSLEngine sslEngine = sslVisitor.getSslEngine();
				if (HandshakeStatus.NEED_UNWRAP == sslStatus.get(key)) {
					ByteBuffer tmpOob = ByteBuffer.allocateDirect(4 << 10);
					ByteBuffer fromSSL = sslVisitor.getFromSSL();
					SSLEngineResult unwrap = sslEngine.unwrap(fromSSL, tmpOob);
					ByteBuffer byteBuffer = oob.get(key);
					oob.put(key, (ByteBuffer) (null != byteBuffer ? ByteBuffer
							.allocate(
									byteBuffer.remaining()
											+ tmpOob.flip().remaining()).put(
									byteBuffer).put(tmpOob).flip() : tmpOob
							.flip()));
					((SocketChannel) key.channel()).write((ByteBuffer) fromSSL
							.flip());
					HandshakeStatus handshakeStatus = unwrap
							.getHandshakeStatus();
					sslStatus.put(key, handshakeStatus);
					fromSSL.compact();
					return 0;
				} else {
					ByteBuffer toSSL = sslVisitor.getToSSL();
					SSLEngineResult wrap = sslEngine.wrap(payload, toSSL);

					int write = ((SocketChannel) key.channel())
							.write((ByteBuffer) toSSL.flip());
					HandshakeStatus handshakeStatus = wrap.getHandshakeStatus();
					sslStatus.put(key, handshakeStatus);
					toSSL.compact();
					return wrap.bytesProduced();
				}
			}
			return write((SocketChannel) key.channel(), payload);
		}

		public static int read(ReadableByteChannel channel, ByteBuffer cursor)
				throws IOException {

			return channel.read(cursor);
		}

		public static int write(WritableByteChannel channel1, ByteBuffer payload)
				throws IOException {
			return channel1.write(payload);
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
						int read = FSM.read(key, payload);
						if (read == -1)
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
					int write = FSM.write(key, cursor);
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
						int read = FSM.read(key, payload);
						if (read == -1)
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
					int write = FSM.write(key, cursor);
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
			int d = depth.length > 0 ? depth[0] : 2;
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
			accept.register((Selector) key.selector(), OP_READ | OP_WRITE, key
					.attachment());

		}
	}
}
