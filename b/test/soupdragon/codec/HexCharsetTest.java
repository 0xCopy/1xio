/*
 * HexCharsetTest.java
 * JUnit based test
 *
 * Created on 06 January 2006, 08:01
 */

package soupdragon.codec;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Arrays;
import junit.framework.*;




/**
 *
 * @author malcolmm
 */
public class HexCharsetTest extends TestCase {
    
    public HexCharsetTest(String testName) {
        super(testName);
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite(HexCharsetTest.class);
        
        return suite;
    }
        private void assertCombinedEquals(String message, byte[] expected, ByteBuffer buf1, ByteBuffer buf2) {
        buf1.flip();buf2.flip();
        buf1.rewind(); buf2.rewind();
        byte[] combo = new byte[buf1.remaining() + buf2.remaining()];
        int size1 = buf1.remaining();
        buf1.get(combo, 0, size1);
        buf2.get(combo, size1, buf2.remaining());
        assertTrue(message, Arrays.equals(expected, combo));
        
    }
    public void testConstruction() throws UnsupportedEncodingException {
        Charset cs = Charset.forName("hex");
        assertEquals("hex", cs.displayName());
        cs = new HexCharset(false);
        assertEquals("hex", cs.displayName());
        cs = new HexCharset(true);
        assertEquals("HEX", cs.displayName());
        cs = Charset.forName("Hex");
        assertEquals("HEX", cs.displayName());
        
        cs = Charset.forName("hex:64");
        assertEquals("hex:64", cs.displayName());
        try {
            cs = Charset.forName("hex:xx");
            fail("No exception on non-numeric measure");
        } catch(UnsupportedCharsetException e) {
            //
        }
        try {
            cs = Charset.forName("hex:xx");
            fail("No exception on non-numeric measure");
        } catch(UnsupportedCharsetException e) {
            //
        }
        try {
            cs = Charset.forName("hex:-1");
            fail("No exception on negative measure");
        } catch(UnsupportedCharsetException e) {
            //
        }
        try {
            cs = Charset.forName("hex:99999999");
            fail("No exception on excessive  measure");
        } catch(UnsupportedCharsetException e) {
            //
        }
    }

    /**
     * Test of newEncoder method, of class com.cc.codec.HexCharset.
     */
    public void testNewEncoder() throws UnsupportedEncodingException {
        System.out.println("testNewEncoder");
        for(int i = 0; i < codedSamples.length; i++) {
            assertTrue("encode " + i, Arrays.equals(binarySamples[i], codedSamples[i].getBytes("hex")));
        }
        byte[] bb;
        try {
            bb = "X".getBytes("hex");
            fail("No exception for invalid hex char");
        } catch(IllegalArgumentException e) {
            // expected
        }
        try {
            bb = "1AA".getBytes("hex");
            fail("No exception for odd length");
        } catch(IllegalArgumentException e) {
            // expected
        }
        Charset cs = Charset.forName("hex");
        CharsetEncoder enc = cs.newEncoder();
        ByteBuffer buf1, buf2;
        buf1 = ByteBuffer.allocate(10);
        assertEquals(enc.encode(CharBuffer.wrap("A2F"), buf1, false), CoderResult.UNDERFLOW);
        enc.reset(); // check that reset works
        buf1 = ByteBuffer.allocate(binarySamples[0].length);
        assertEquals(enc.encode(CharBuffer.wrap(codedSamples[0].toCharArray()), buf1, true), CoderResult.UNDERFLOW);
        assertTrue(Arrays.equals(binarySamples[0], buf1.array()));
        // now test buffer split works correctly for each phase of byte output
        byte[] testb = binarySamples[6];
        String txt = codedSamples[6];
        for(int i = 9; i < 12; i++) {
            buf1 = ByteBuffer.allocate(i);
            buf2 = ByteBuffer.allocate(testb.length - i);
            enc.reset();
            CharBuffer cb = CharBuffer.wrap(txt);
            assertEquals(enc.encode(cb, buf1, true), CoderResult.OVERFLOW);
            assertEquals(enc.encode(cb, buf2, true), CoderResult.UNDERFLOW);
            assertCombinedEquals("l= " + i, testb, buf1, buf2);
        }
        
        // test whitespace disregard
        
        assertTrue(Arrays.equals(binarySamples[0],
                "f 303758a1d39b61\tb4f1\t26d97fce4\naa60c807a1286a490db83eb3e5c36c4c".getBytes("hex")));
    }
    
    /**
     * Test of newDecoder method, of class com.cc.codec.HexCharset.
     */
    public void testNewDecoder() throws UnsupportedEncodingException {
        System.out.println("testNewDecoder");
        // basic tests with binary of assorted lengths
        for(int i = 0; i < binarySamples.length; i++) {
            assertEquals("decode " + i, codedSamples[i], new String(binarySamples[i], "hex"));
            assertEquals("Decode " + i, codedSamples[i].toUpperCase(), new String(binarySamples[i], "HEX"));
        }
        // test if folding provides all lines the same length. 13 bytes per line insures
        // all states tested
        String folded = new String(binarySamples[6], "hex:26");
        String[] lines = folded.split("\n");
        assertTrue(lines.length > 1);
        for(int i = 0; i < lines.length - 1; i++)
            assertEquals(26, lines[i].length());
        
         Charset cs = Charset.forName("hex");
        CharsetDecoder dc = cs.newDecoder();
        CharBuffer buf1 = CharBuffer.allocate(10);
        byte[] testb = binarySamples[6];
        String txt = codedSamples[6];
        // first test reset works
        assertEquals(CoderResult.UNDERFLOW, dc.decode(ByteBuffer.wrap(new byte[]{122, -10}), buf1, false));
        dc.reset();
        buf1 = CharBuffer.allocate(txt.length());
        assertEquals(CoderResult.UNDERFLOW, dc.decode(ByteBuffer.wrap(testb), buf1, true));
        for(int i = 12; i < 16; i++) {
            ByteBuffer bb = ByteBuffer.wrap(testb);
            buf1 = CharBuffer.allocate(i);
            CharBuffer buf2 = CharBuffer.allocate(txt.length() + 5 - i);
            dc.reset();
            assertEquals(CoderResult.OVERFLOW, dc.decode(bb, buf1, true));
            assertEquals(CoderResult.UNDERFLOW, dc.decode(bb, buf2, true));
            assertEquals(CoderResult.UNDERFLOW, dc.flush(buf2));
            buf2.flip();
            buf1.flip();
            buf1.rewind();
            buf2.rewind();
            assertEquals("offset " + i, txt, buf1.toString() + buf2.toString());
        }
   }
    
    /**
     * Test of contains method, of class com.cc.codec.HexCharset.
     */
    public void testContains() throws UnsupportedEncodingException {
        System.out.println("testContains");
        assertTrue(Charset.forName("hex").contains(Charset.forName("hex")));
        assertFalse(Charset.forName("hex").contains(Charset.forName("UTF-8")));
    }
    private final static byte[][] binarySamples = {
        {-13, 3, 117, -118, 29, 57, -74, 27, 79, 18, 109, -105, -4, -28, -86, 96, -56, 7, -95, 40, 106, 73, 13, -72, 62, -77, -27, -61, 108, 76},
        {-96, 25, -3, -85, 5, 22, 0, 51, -81, -120, 62, -86, -40, 104, 30, -102, 19, -29, -67, 54, 66, 40, 73, 88, -90, -104, 62, -51, -69, 104, -119, -21, 33, 124, -54, 63, -126, -24, -114, 4, -106, 47, -124, -80, -35, 81, -42, -18, 82, 67, 46, 86, 127, -3, 60, 30, 38, -17, 39, -46, -60},
        {31, -108, 13, 61, -24, -52, -61, 52, 55, -123, -69, 102, 106, -105, 94, 106, -112, -1, -60, 107, 48, -15, -10, -119, 38, 79, -50, -85, 47, 69, -58, 52, 38, -21, 48, 30, -94, -72, 124, -117, 78, 109, 2, 71},
        {},
        {16},
        {-6, -71},
        {-72, 1, -50, 102, -48, 10, 97, 31, 25, 75, 56, 99, 37, -28, 126, -65, -83, 8, 103, -70, -107, -35, 68, -124, 24, -98, 108, -124, 98, -96, 109, 40, 81, -6, -66, -105, -108, 34, 30, -91, -109, -6, -80, -94, -14, 3, -106, 42, 27, 96, -91, 76, 80, -102, -61, 55, -5, -3, -6, -14, -65, -68, 115, 81, -124, -71, 1, 77, -92, 73, 56, 84, 32, 15, -88, -119, -60, -55, -19, 3, -110, -42, -30, 56, -62, 2, 6, -83, -24, 76, -45, 119, -85, -43, 82, 92, -78, -57, -112, 83, 120, -6, -56, 78, 59, 52, -113, 84, -73, 23, -18, -11, -54, 118, 45, -40, -75, -19, -120, 104, 65, 49, 63, -80, -28, -34, -114, 118, -113, 61, -99, -52, 76, -101, 64, 125, -49, -113, 92, 27, 75, 43, 91, -93, 19, 13, 50, 53, -9, -107, -68, -3, 30, 127, -75, -18, -53, -7, -25, -62, -95, -6, 40, -96, -99, -78, 6, 37, -67, 28, -20, 72, -26, 37, 24, 97, -52, 69, -77, -64, -41, -106, 35, 9, -56, -70, -26, 86, -24, 5, -19, -43, -59, 43, 65, 107, -80, 43, 60, -22, 90, -43, 65, 10, 77, 37, -126, 53, -19, 27, 86, 121, -84, -8, -92, -85, 63, 107, -25, -100, 100, -63, 31, 47, -85, 33, -109, 127, 11, -85, -47, 24, -18, -83, -10, 98, 47, -20, -27, 97, 126, -105, 29, 126, 78, 77, 99, -76, -75, 60, 98, 95, -115, 121, 76, 121, 85, 119, 85, -57, -6, -14, 99, -91, 54, 95, 90, 51, -102, 26, -46, 8, 30, -36, 47, -44, -101, -36, -86, 62, 17, -13, -5, 103, 86, 75, -9, 78, -100, -120, -65, 5, 11, 88, -112, 50, -30, 0, 20, -60, -124, -10, 71, 90, 125, -121, -66, -31, -12, -32, -56, -23, 109, 44, 83, -104, -44, -13, 0, 58, -85, 56, -119, -7, -110, 69, -77, -117, 24, 116, 75, 90, 67, -60, 12, 13, -30, 95, 44, -63, -67, -45, -35, -8, -103, -13, 69, -19, -62, 48, -27, -89, -126, 8, 45, 92, 38, -46, 66, 114, -17, -99, -91, -18, 21, -81, -99, -46, -91, 82, -54, 24, -32, -55, -4, -104, -107, 34, 103, -51, -2, -80, 52, -106, 116, 125, -26, -107, 34, 100, 55, 79, -11, 113, -99, 96, -102, 65, -120, -31}
    };
    private final static String[] codedSamples = {
        "f303758a1d39b61b4f126d97fce4aa60c807a1286a490db83eb3e5c36c4c",
                "a019fdab05160033af883eaad8681e9a13e3bd3642284958a6983ecdbb6889eb217cca3f82e88e04962f84b0dd51d6ee52432e567ffd3c1e26ef27d2c4",
                "1f940d3de8ccc3343785bb666a975e6a90ffc46b30f1f689264fceab2f45c63426eb301ea2b87c8b4e6d0247",
                "",
                "10",
                "fab9",
                "b801ce66d00a611f194b386325e47ebfad0867ba95dd4484189e6c8462a06d2851fabe9794221ea593fab0a2f203962a1b60a54c509ac337fbfdfaf2bfbc735184b9014da4493854200fa889c4c9ed0392d6e238c20206ade84cd377abd5525cb2c7905378fac84e3b348f54b717eef5ca762dd8b5ed886841313fb0e4de8e768f3d9dcc4c9b407dcf8f5c1b4b2b5ba3130d3235f795bcfd1e7fb5eecbf9e7c2a1fa28a09db20625bd1cec48e6251861cc45b3c0d7962309c8bae656e805edd5c52b416bb02b3cea5ad5410a4d258235ed1b5679acf8a4ab3f6be79c64c11f2fab21937f0babd118eeadf6622fece5617e971d7e4e4d63b4b53c625f8d794c79557755c7faf263a5365f5a339a1ad2081edc2fd49bdcaa3e11f3fb67564bf74e9c88bf050b589032e20014c484f6475a7d87bee1f4e0c8e96d2c5398d4f3003aab3889f99245b38b18744b5a43c40c0de25f2cc1bdd3ddf899f345edc230e5a782082d5c26d24272ef9da5ee15af9dd2a552ca18e0c9fc98952267cdfeb03496747de6952264374ff5719d609a4188e1"};
                
}
