package hideftvads.proto;

import java.nio.*;
import java.net.*;

/**
 * User: jim
 * Date: Jul 12, 2009
 * Time: 1:19:10 AM
 */
public enum NNTPCapabilities {
    /**
     * This capability MUST be advertised by all servers and MUST be the
     * first capability in the capability list; it indicates the
     * version(s) of NNTP that the server supports.  There must be at
     * least one argument; each argument is a decimal number and MUST NOT
     * have a leading zero.  Version numbers are assigned only in RFCs
     * that update or replace this specification; servers MUST NOT create
     * their own version numbers.
     */
    VERSION,
    /**
     * This capability indicates that the server implements the various
     * commands useful for reading clients.
     */
    READER,
    /**
     * This capability indicates that the server implements the IHAVE
     * command.
     */
    IHAVE, 
    /**
     * This capability indicates that the server implements the POST
     * command.
     */
    POST,
    /**
     * This capability indicates that the server implements the NEWNEWS
     * command.
     */
    NEWNEWS,
    /**
     * This capability indicates that the server implements the header
     * access commands (HDR and LIST HEADERS).
     */
    HDR, 
    /**
     * This capability indicates that the server implements the overview
     * access commands (OVER and LIST OVERVIEW.FMT).  If and only if the
     * server supports the message-id form of the OVER command, there
     * must be a single argument MSGID.
     */
    OVER,
    /**
     * This capability indicates that the server implements at least one
     * variant of the LIST command.  There MUST be one argument for each
     * variant of the LIST command supported by the server, giving the
     * keyword for that variant.
     */
    LIST,
    /**
     * This capability MAY be provided by a server.  If so, the arguments
     * SHOULD be used to provide information such as the server software
     * name and version number.  The client MUST NOT use this line to
     * determine capabilities of the server.  (While servers often
     * provide this information in the initial greeting, clients need to
     * guess whether this is the case; this capability makes it clear
     * what the information is.)
     */
    IMPLEMENTATION,
    /**
     * This capability indicates that the server is mode-switching
     * (SwingUtilities2.Section3.4.2) and that the MODE READER command needs to be used
     * to enable the READER capability.
     */
    MODE$2dREADER,
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
