package one.xio;

import java.io.IOException;
import java.nio.channels.SelectionKey;

/**
 * User: jim
 * Date: 4/15/12
 * Time: 11:50 PM
 */
public interface AsioVisitor {
  void onRead(SelectionKey key)throws IOException;

  void onConnect(SelectionKey key)throws IOException;

  void onWrite(SelectionKey key)throws IOException;

  void onAccept(SelectionKey key)throws IOException;
}
