package one.xio;

import java.nio.channels.SelectionKey;

/**
 * User: jim
 * Date: 4/15/12
 * Time: 11:50 PM
 */
public interface AsioVisitor {
  void onRead(SelectionKey key);

  void onConnect(SelectionKey key);

  void onWrite(SelectionKey key);

  void onAccept(SelectionKey key);
}
