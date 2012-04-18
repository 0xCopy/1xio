package one.xio;

import java.net.URLDecoder;
import java.nio.*;

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


  private final CharBuffer header = CharBuffer.wrap(URLDecoder.decode(name().replace('$', '%')));

  private final ByteBuffer token = HttpMethod.UTF8.encode(header);

  private int tokenLen = token.limit();

  /**
   * utility method for ContentLength
   *
   * @param headers OnRead seeks to EOL+EOL, sending the headers half to this method.
   * @return the value for the Content-Length header
   */
  public static CharBuffer getContentLength(ByteBuffer headers) {
    int b;
    CharBuffer decode = null;
    while (headers.hasRemaining()) {
      int m = headers.position();
      while (headers.hasRemaining() && '\n' != (b = headers.get())) ;
      int l = headers.limit();
      int e = headers.position();
      ByteBuffer line = ((ByteBuffer) headers.position(m).limit(e)).slice();
      headers.limit(l);
      headers.position(e);
      if (Content$2dLength.recognize(line)) {
        ByteBuffer parse = Content$2dLength.parse(line);
        decode = HttpMethod.UTF8.decode(parse);
        break;
      }
    }
    return decode;
  }
  public CharBuffer getHeader() {
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
   * @return   a slice suitable for UTF8.decode
   */
  public ByteBuffer parse(ByteBuffer slice) {
    slice.position(tokenLen + 2 + slice.position());
    while (Character.isWhitespace(slice.get(slice.limit() - 1))) slice.limit(slice.limit() - 1);
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
