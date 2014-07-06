package one.xio;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.WeakHashMap;

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
	WeakHashMap<Impl, String> $origins = $DBG
			? new WeakHashMap<Impl, String>()
			: null;

	void onRead(SelectionKey key) throws Exception;

	void onConnect(SelectionKey key) throws Exception;

	void onWrite(SelectionKey key) throws Exception;

	void onAccept(SelectionKey key) throws Exception;

	class Helper {
		public static void toAccept(SelectionKey key, final F f) {
			toAccept(key, toAccept(f));
		}

		public static void toAccept(SelectionKey key, Impl impl) {
			toRead(key, impl);
		}

		public static void toRead(SelectionKey key, final F f) {
			key.interestOps(OP_READ).attach(toRead(f));
		}

		public static void toRead(SelectionKey key, Impl impl) {
			key.interestOps(OP_ACCEPT).attach(impl);
		}

		public static void toConnect(SelectionKey key, final F f) {
			toConnect(key, toConnect(f));
		}

		public static void toConnect(SelectionKey key, Impl impl) {
			key.interestOps(OP_CONNECT).attach(impl);
		}

		public static void toWrite(SelectionKey key, final F f) {
			toWrite(key, toWrite(f));
		}

		public static void toWrite(SelectionKey key, Impl impl) {
			key.interestOps(OP_WRITE).attach(impl);
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
