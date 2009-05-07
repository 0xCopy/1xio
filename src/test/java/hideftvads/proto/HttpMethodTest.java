package hideftvads.proto;

import junit.framework.*;

import java.nio.*;

/**
 * User: jim
 * Date: May 7, 2009
 * Time: 1:09:25 AM
 */
public class HttpMethodTest extends TestCase {
    public void testTokenize() {


        String request;

        ByteBuffer indexEntries;

        request = "POST /welcome/test1.mp4 HTTP/1.1 UTF-8 en-US*\n" +
                "foo: sadfa f\n" +
                "aaoao: asdasdasd awd sdsa dsf a\n\n";

        indexEntries = ByteBuffer.wrap(request.getBytes());

        for (HttpMethod httpMethod : HttpMethod.values()) {

            if (httpMethod.recognize(indexEntries)) {
                assertEquals(HttpMethod.POST, httpMethod);
                httpMethod.tokenize(indexEntries);

                final int margin = httpMethod.margin;

                indexEntries.mark().position(0);

                int index = 0; 
                
                HttpMethod.decodeTokens(indexEntries, httpMethod, index);
                return;
            }
        }
    }

    public void testName() {        // Add your code here
    }

    public void testOrdinal() {
        // Add your code here
    }

    public void testToString() {
        // Add your code here
    }

    public void testEquals() {
        // Add your code here
    }

    public void testHashCode() {
        // Add your code here
    }

    public void testCompareTo() {
        // Add your code here
    }

    public void testGetDeclaringClass() {
        // Add your code here
    }

    public void testValueOf() {
        // Add your code here
    }
}
