/*
 * Base64CharsetTest.java
 * JUnit based test
 *
 * Created on 04 January 2006, 20:46
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
import java.util.Iterator;
import junit.framework.*;




/**
 *
 * @author malcolmm
 */
public class Base64CharsetTest extends TestCase {
    
    public Base64CharsetTest(String testName) {
        super(testName);
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite(Base64CharsetTest.class);
        
        return suite;
    }
    
    public void testConstruction() throws UnsupportedEncodingException {
        Charset cs = Charset.forName("base-64");
        assertEquals("Base64", cs.displayName());
        cs = new Base64Charset();
        assertEquals("Base64", cs.displayName());
        cs = Charset.forName("base-64:64");
        assertEquals("Base64:64", cs.displayName());
        try {
            cs = Charset.forName("base-64:xx");
            fail("No exception on non-numeric measure");
        } catch(UnsupportedCharsetException e) {
            //
        }
        try {
            cs = Charset.forName("base-64:xx");
            fail("No exception on non-numeric measure");
        } catch(UnsupportedCharsetException e) {
            //
        }
        try {
            cs = Charset.forName("base-64:-1");
            fail("No exception on negative measure");
        } catch(UnsupportedCharsetException e) {
            //
        }
        try {
            cs = Charset.forName("base-64:99999999");
            fail("No exception on excessive  measure");
        } catch(UnsupportedCharsetException e) {
            //
        }
    }
    
    public void testIterator() {
        Iterator<Charset> it = new Base64Provider().charsets();
        assertTrue(it.hasNext());
        assertEquals("Base64", it.next().displayName());
        try {
            it.remove();
            fail("Remove should give Unsupported");
        } catch(UnsupportedOperationException e) {
            //
            
        }
        assertTrue(it.hasNext());
        assertEquals("hex", it.next().displayName());
        assertFalse(it.hasNext());
    }
    private final static byte[][] binarySamples = {
        {-110, -56, -84, -7, -61, 6, 101, -115, 120, -98, 11, -48, -17, -19,
                 83, 74, -7, -113, -7, 21, -81, 6, -106, 12, -113, 93, 16, -57, 117, 70},
                 {104, 64, 39, -88, 7, 118, -109, 104, 84, 83, 96, 10, 73, -10, 46, 79,
                          55, 66, -66, 22, 22, 9, -105, 10, -65, 71, -65, 74, 4, 108, -117,
                          14, -82, -57, -108, 58, 26, 124, -120, -19, 0, 36, 20, 99, -81, -60,
                          -31, -75, -89, -43, 77, -106, 21, -103, 7, -112, -61, -27, 89, -97, -69},
                          {114, -78, 45, -30, -28, 70, -80, 75, -76, 2, -106, 72, 51, 78, -78, -89, -8, -34,
                                   -117, 53, -112, -11, -89, 69, 68, 0, -45, -43, 52, 25, 5, 96, 34, -2, 8, -95,
                                   -29, -5, -92, -115, -83, 67, -58, 92},
                                   {-100},
                                   {},
                                   {123, 78},
                                   {62, -70, -107, -54, 52, 94, -76, 49, 0, 91, -14, 8, 19, -102, -102, 72, -86, -106, 22,
                                            64, -83, 89, -105, -11, -28, 75, -81, -62, -90, -64, 22, -116, 44, 38, 67, -109,
                                            68, -55, -66, 63, 37, -115, -75, -75, -2, 63, -126, -79, 92, 23, 96, -95, 121,
                                            -94, 89, 69, -70, 38, -22, -34, -107, 94, 72, 90, 56, -78, -96, -20, 108, 34, -42,
                                            73, -101, -1, 4, -22, 125, -36, 44, 48, 53, 1, 36, -92, 90, 67, 4, -81, 0, 127, -127,
                                            -45, -124, 78, -123, -67, -105, -9, 114, 121, 103, -122, -85, 95, -50, -116, 91, -42,
                                            -109, -121, 10, 120, 1, 41, -118, 64, -99, 21, 13, 67, 78, -91, -73, 88, 86, -76, 12,
                                            -56, 79, -26, 14, -123, 52, -126, -41, -25, 31, -12, 75, 17, 33, 13, 91, -43, -93, -45,
                                            -8, -88, 19, -93, -95, -95, 109, -86, 75, -110, 17, 66, -38, -84, -67, 75, -10, -13, -106,
                                            87, 86, -79, 99, 4, -11, -44, 114, 5, -45, -29, -112, 36, 99, -8, -110, 50, 83,
                                            -121, -34, -79, -11, 13, 96, 88, -76, -38, 17, -87, 14, 112, -107, 81, -104,
                                            -55, 98, -106, -32, -75, -80, 125, -101, -82, 81, -36, -122, -39, 113, -28,
                                            -121, 25, 81, 20, 43, 53, 16, -73, -83, 113, -50, 89, 108, -85, -5, 40, -104,
                                            -17, 89, 87, -41, 103, 108, 48, 110, 116, 5, 103, -77, 119, 16, -33, 66, -96,
                                            46, 82, 84, 69, -109, -67, -4, -9, 75, 10, -20, 57, -40, 120, -87, 88, 121, 99,
                                            115, 108, 26, 78, -48, 32, -36, 58, -12, -62, 78, -117, 21, -69, 15, -42, -34,
                                            92, -128, 59, 74, 45, 52, 126, 20, -113, -1, 66, -15, -97, 102, -80, 123, 118,
                                            -110, -43, -83, -23, -99, -122, -72, -78, 17, -31, 75, 71, 68, -121, -44, -50,
                                            16, -47, -39, -35, 9, -89, -6, 34, -121, -124, -70, 37, -72, 71, 82, -28, -111,
                                            124, -41, 102, 33, 48, 82, -69, -62, -61, -91, 29, 77, 44, -29, -71, -93, -33, 45,
                                            -95, -99, 10, 34, -98, 74, -38, 24, -40, 125, 81, -1, -14, 54, 40, 37, -25, -43, 34,
                                            -26, 27, -50, 110, 47, -122, 100, -99, -3, -46, 57, 10, 9, -126, -104, 121, -109, 49,
                                            105, 53, -117, 90, -75, -18, -94, 50, -123, -11, 62, -67}
    };
    
    
    private final static String[] codedSamples = {
        "ksis+cMGZY14ngvQ7+1TSvmP+RWvBpYMj10Qx3VG",
                "aEAnqAd2k2hUU2AKSfYuTzdCvhYWCZcKv0e/SgRsiw6ux5Q6GnyI7QAkFGOvxOG1p9VNlhWZB5DD5Vmfuw==",
                "crIt4uRGsEu0ApZIM06yp/jeizWQ9adFRADT1TQZBWAi/gih4/ukja1Dxlw=",
                "nA==",
                "",
                "e04=",
                "PrqVyjRetDEAW/IIE5qaSKqWFkCtWZf15EuvwqbAFowsJkOTRMm+PyWNtbX+P4KxXBdgoXmiWUW6" +
                "JurelV5IWjiyoOxsItZJm/8E6n3cLDA1ASSkWkMErwB/gdOEToW9l/dyeWeGq1/OjFvWk4cKeAEp" +
                "ikCdFQ1DTqW3WFa0DMhP5g6FNILX5x/0SxEhDVvVo9P4qBOjoaFtqkuSEULarL1L9vOWV1axYwT1" +
                "1HIF0+OQJGP4kjJTh96x9Q1gWLTaEakOcJVRmMliluC1sH2brlHchtlx5IcZURQrNRC3rXHOWWyr" +
                "+yiY71lX12dsMG50BWezdxDfQqAuUlRFk73890sK7DnYeKlYeWNzbBpO0CDcOvTCTosVuw/W3lyAO" +
                "0otNH4Uj/9C8Z9msHt2ktWt6Z2GuLIR4UtHRIfUzhDR2d0Jp/oih4S6JbhHUuSRfNdmITBSu8LDpR" +
                "1NLOO5o98toZ0KIp5K2hjYfVH/8jYoJefVIuYbzm4vhmSd/dI5CgmCmHmTMWk1i1q17qIyhfU+vQ=="
    };
    
    /**
     * Test of newEncoder method, of class com.cc.codec.Base64Charset.
     */
    public void testNewEncoder() throws UnsupportedEncodingException {
        System.out.println("testNewEncoder");
        // straightforward encoding
        for(int i = 0; i < codedSamples.length; i++)
            assertTrue("Simple encoding " + i, Arrays.equals(codedSamples[i].getBytes("base64"),binarySamples[i]));
        byte[] b;
        try {
            b = "AcS".getBytes("base64");
            fail("No exception for truncated base64");
        } catch(IllegalArgumentException e) {
            // expected
        }
        try {
            b = "a|\\A".getBytes("base64");
            fail("No exception for invalid base64");
        } catch(IllegalArgumentException e) {
            // expected
        }
        Charset cs = Charset.forName("Base-64");
        CharsetEncoder enc = cs.newEncoder();
        ByteBuffer buf1, buf2;
        buf1 = ByteBuffer.allocate(10);
        assertEquals(enc.encode(CharBuffer.wrap("AB"), buf1, false), CoderResult.UNDERFLOW);
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
        // test white space and terminal garbage ignored
        
        assertTrue(Arrays.equals(binarySamples[1], 
                "a EAn\tqAd2k2hUU2A\nKSfYuTzdCvhYWCZcKv0e/SgRsiw6ux5Q6GnyI7QAkFGOvxOG1p9VNlhWZB5DD5Vmfuw==!!".
                getBytes("base64")));
        
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
    
    final static int[] critFolds = {25, 26, 27, 51, 52, 53, 77, 78, 79};
    final static int testLineWidth = 25;
    
    /**
     * Test of newDecoder method, of class com.cc.codec.Base64Charset.
     */
    public void testNewDecoder() throws UnsupportedEncodingException {
        System.out.println("testNewDecoder");
        for(int i = 0; i < binarySamples.length; i++){
//                                                                System.out.println(i);
            assertEquals("Simple decoding " + i, codedSamples[i], new String(binarySamples[i], "base64"));
        }
        
        Charset cs = Charset.forName("base-64");
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
        StringBuffer flds = new StringBuffer(txt.length() + 10);
        String f = txt;
        while(f.length() > 25) {
            flds.append(f.substring(0, 25)).append('\n');
            f = f.substring(25);
        }
        flds.append(f);
        f = flds.toString();
        String folded = new String(testb, "base64:25");
        assertEquals(f, folded);
        // 25 means \n will accur at every phase
        cs = Charset.forName("base64:" + testLineWidth);
        dc = cs.newDecoder();
        
        // these tests are focused on special handling when
        // buffer overflows arround inserted \n
        for(int i :critFolds) {
            ByteBuffer bb = ByteBuffer.wrap(testb);
            buf1 = CharBuffer.allocate(i);
            CharBuffer buf2 = CharBuffer.allocate(f.length() + 5 - i);
            dc.reset();
            assertEquals("Crit " + i,CoderResult.OVERFLOW, dc.decode(bb, buf1, true));
            assertEquals("Crit " + i,CoderResult.UNDERFLOW, dc.decode(bb, buf2, true));
            assertEquals("Crit " + i,CoderResult.UNDERFLOW, dc.flush(buf2));
            buf2.flip();
            buf1.flip();
            buf1.rewind();
            buf2.rewind();
     //       System.out.println(buf1.toString() + buf2.toString());
            assertEquals("crit " + i, f, buf1.toString() + buf2.toString());
        }
        
        

    }
    
    /**
     * Test of contains method, of class com.cc.codec.Base64Charset.
     */
    public void testContains() {
        System.out.println("testContains");
        Charset cs1 = new Base64Charset();
        Charset cs2 = Charset.forName("UTF-8");
        assertTrue(cs1.contains(cs1));
        assertFalse(cs1.contains(cs2));
    }
    
}
