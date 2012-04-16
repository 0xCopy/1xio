package one.xio;

import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

/**
 * User: jim
 * Date: May 11, 2009
 * Time: 11:37:55 PM
 */
public enum HttpHeaders {
  Content$2dLength,
  Content$2dEncoding,
  Host,
  Accept,
  User$2dAgent,;


  final CharBuffer header = CharBuffer.wrap(URLDecoder.decode(name().replace('$', '%')));

  final ByteBuffer token = HttpMethod.UTF8.encode(header);

  int tokenLen = token.limit();

  boolean recognize(ByteBuffer buffer) {

    final int i = buffer.position();
    boolean ret = false;
    if ((buffer.get(tokenLen + i) & 0xff) == ':') {

      int j = 0;
      while (j < tokenLen && token.get(j) == buffer.get(i + j)) {
        j++;
      }
      ret = tokenLen == j;
    }

    return ret;
  }

}
