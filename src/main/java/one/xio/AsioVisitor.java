package one.xio;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.WeakHashMap;

import static java.lang.StrictMath.min;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
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

	class Impl implements AsioVisitor {
		{
			if ($DBG)
				$origins.put(this, wheresWaldo(4));
		}

		@Override
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
		@Override
		public void onConnect(SelectionKey key) throws Exception {
			if (((SocketChannel) key.channel()).finishConnect())
				key.interestOps(OP_WRITE);
		}

		@Override
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
		@Override
		public void onAccept(SelectionKey key) throws Exception {

			ServerSocketChannel c = (ServerSocketChannel) key.channel();
			SocketChannel accept = c.accept();
			accept.configureBlocking(false);
			accept.register((Selector) key.selector(), OP_READ | OP_WRITE, key
					.attachment());

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
	}
}
