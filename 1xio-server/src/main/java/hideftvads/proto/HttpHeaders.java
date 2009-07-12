package hideftvads.proto;

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
    User$2dAgent,Cookie,
    ;

    final CharBuffer header = CharBuffer.wrap(URLDecoder.decode(name().replace('$', '%')));
    final ByteBuffer token = ProtoUtil.UTF8.encode(header);
    final int tokenLen = token.limit();

    boolean recognize(ByteBuffer buffer) {
        final int i = buffer.position();
        if ((buffer.get(tokenLen + i) & 0xff) == ':') {
            int j;
            
            for (j = 0; j < tokenLen && token.get(j) == buffer.get(i + j); j++) ;
            return tokenLen == j;
        }
        return false;
    }
}
