package one.xio;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import static java.nio.channels.SelectionKey.OP_WRITE;

/**
 * User: jim
 * Date: 4/15/12
 * Time: 11:50 PM
 */
public interface AsioVisitor {
  void onRead(SelectionKey key) throws Exception;

  void onConnect(SelectionKey key) throws Exception;

  void onWrite(SelectionKey key) throws Exception;

  void onAccept(SelectionKey key) throws Exception;

  class Impl implements AsioVisitor {
    @Override
    public void onRead(SelectionKey key) throws Exception {
      System.err.println("fail: " + key.toString());
      throw new UnsupportedOperationException("found in " + getClass().getCanonicalName());
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
      System.err.println("fail: " + key.toString());
      throw new UnsupportedOperationException("found in " + getClass().getCanonicalName());
    }

    @Override
    public void onAccept(SelectionKey key) throws Exception {
      System.err.println("fail: " + key.toString());
      throw new UnsupportedOperationException("found in " + getClass().getCanonicalName());

    }
  }
}
