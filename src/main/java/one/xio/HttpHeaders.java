package one.xio;

import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * User: jim
 * Date: May 11, 2009
 * Time: 11:37:55 PM
 */
public enum HttpHeaders {
  Host,
  Accept,
  Expect,
  ETag,
  Cookie,
  Content$2dType,
  Content$2dLength,
  Accept$2dEncoding,
  Content$2dEncoding,
  Transfer$2dEncoding,
  User$2dAgent,
  Set$2dCookie,;

  private final String header = URLDecoder.decode(name().replace('$', '%'));

  private final ByteBuffer token = HttpMethod.UTF8.encode(header);

  private int tokenLen = token.limit();

  /**
   * utility method for ContentLength
   *
   * @param headers OnRead seeks to EOL+EOL, sending the headers half to this method.
   * @return the value for the Content-Length header
   */
  public static Map<String, int[]> getHeaders(ByteBuffer headers) {
    headers.rewind();
    int l = headers.limit();
    Map<String, int[]> linkedHashMap = new LinkedHashMap<String, int[]>();
    while (headers.hasRemaining() && '\n' != headers.get()) ;
    while (headers.hasRemaining()) {
      int p1 = headers.position();
      while (headers.hasRemaining() && ':' != headers.get()) ;
      int p2 = headers.position();
      while (headers.hasRemaining() && '\n' != headers.get()) ;
      int p3 = headers.position();

      String key = HttpMethod.UTF8.decode((ByteBuffer) headers.position(p1).limit(p2 - 1)).toString().trim();
      if (key.length() > 0) {
        linkedHashMap.put(key, new int[]{p2, p3});
      }
      headers.limit(l).position(p3);

    }

    return linkedHashMap;
  }

  public String getHeader() {
    return header;
  }

  public ByteBuffer getToken() {
    return token;
  }

  public int getTokenLen() {
    return tokenLen;
  }

  public void setTokenLen(int tokenLen) {
    this.tokenLen = tokenLen;
  }

  /**
   * @param slice
   * @return a slice suitable for UTF8.decode
   */
  public ByteBuffer parse(ByteBuffer slice) {
    slice.position(tokenLen + 2 + slice.position());
    while (Character.isWhitespace(slice.get(slice.limit() - 1)))
      slice.limit(slice.limit() - 1);
    return slice;
  }

  public boolean recognize(ByteBuffer buffer) {

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
