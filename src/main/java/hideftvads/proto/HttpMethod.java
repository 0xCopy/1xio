package hideftvads.proto;

import static java.lang.Character.*;
import java.nio.*;

/**
 * User: jim
 * Date: May 6, 2009
 * Time: 10:12:22 PM
 * <p/>
 * see http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html
 */
public enum HttpMethod {
    GET {


    }, POST, PUT, HEAD, DELETE, TRACE, CONNECT, OPTIONS, HELP, VERSION;


    final ByteBuffer token = (ByteBuffer) ByteBuffer.wrap(name().getBytes()).rewind().mark();


    final int margin = name().length() + 1;

    /**
     * deduce a few parse optimizations
     *
     * @param request
     * @return
     */

    boolean recognize(ByteBuffer request) {

        if (isWhitespace(request.get(margin)))
            for (int i = 0; i < margin - 1; i++)
                if (request.get(i) != token.get(i))
                    return false;
        return true;
    }


    /**
     * returns a byte-array of token offsets in the first delim +1 bytes of the input buffer     *
     * <p/>
     * in does not get rewound.
     *
     * stateless and heapless
     * 
     * 
     * @param in
     * @return
     */

    public ByteBuffer tokenize(ByteBuffer in) {

        ByteBuffer out = (ByteBuffer) in.duplicate().position(0);


        boolean isBlank = true, wasBlank = true;
        int prevIdx = 0;
        in.position(margin);
        char b = 0;
        while (b != '\n' && out.position() < margin) {
            wasBlank = isBlank;
            b = (char) (in.get() & 0xff);
            isBlank = isWhitespace(b & 0xff);

            if ((!isBlank) && wasBlank) {
                out.put((byte) ((byte) (in.position() & 0xff)-1));

                System.err.println("token found: " + in.duplicate().position(prevIdx));
            }
        }

        while (out.put((byte) 0).position() < margin) ;


        return (ByteBuffer) out.position(0);
    }


    /**
     * this is a util method which takes a byte buffer and prints the indexed pointers from 0
     * 
     * please note this is designed to be as stateless and NIO-neutral as possible.
     * 
     * @param indexEntries
     * @param httpMethod
     * @param index
     */
    public static void decodeTokens(ByteBuffer indexEntries, HttpMethod httpMethod, int index) {
        int d;
        while ((indexEntries.position() <= httpMethod.margin) && (0 != (index = 0xff & indexEntries.get()))) {
            final ByteBuffer in = indexEntries.duplicate();


            in.position(index);
            final StringBuilder report = new StringBuilder(httpMethod.name() + ": index:" + index + " char:");

            System.out.print(report);

            while (!Character.isWhitespace(d = in.get() & 0xff))
                System.out.write(d);

            System.out.write('\n');

        }
    }
}