/*
 * Base64Charset.java
 *
 * Created on 20 December 2005, 14:29
 *
 */

package soupdragon.codec;

import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * <P>Charset for base 64 conversion.</p>
 * @author Malcolm McMahon
 * @version $Revision: 1.1 $
 */
public class Base64Charset  extends Charset {
    static final String charCodes = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
            "abcdefghijklmnopqrstuvwxyz0123456789+/";
    static int[] inverseCodes = new int[128];
    private Integer measure;
    
    static{
        // this builds the inverse mapping table using the charCodes string
        assert charCodes.length() == 64;
        Arrays.fill(inverseCodes, Base64Encoder.CHARCODE_INVALID);
        for(int i = 0; i < charCodes.length(); i++) {
            inverseCodes[(int)charCodes.charAt(i)] = i;
        }
        String ws = " \t\n\r";
        for(int i = 0; i < ws.length(); i++)
            inverseCodes[(int)ws.charAt(i)] = Base64Encoder.CHARCODE_WHITESPACE;
        inverseCodes[(int)'='] = Base64Encoder.CHARCODE_PADDER;
    }
    
    /**
     * Creates a new instance of Base64Charset
     */
    public Base64Charset() {
        super("Base64", new String[]{"Base-64"});
        
    }
    /**
     * Builds a new instance with the decoding line width specified
     * @param measure Max characters per line
     */
    public Base64Charset(int measure) {
        super("Base64:" + measure, new String[]{"Base-64:" + measure});
        this.measure = measure;
        
    }
    
    /**
     * Constructs a new encoder for this charset. </p>
     *
     * @return  A new encoder for this charset
     */
    public java.nio.charset.CharsetEncoder newEncoder() {
        return new Base64Encoder(this, inverseCodes);
    }
    
    /**
     * Constructs a new decoder for this charset. </p>
     *
     * @return  A new decoder for this charset
     */
    public java.nio.charset.CharsetDecoder newDecoder() {
        if(measure == null)
            return new Base64Decoder(this, charCodes, '=');
        else
            
            return new Base64Decoder(this, charCodes, '=', measure);
    }
    
    /**
     * Tells whether or not this charset contains the given charset.
     *
     * <p> A charset <i>C</i> is said to <i>contain</i> a charset <i>D</i> if,
     * and only if, every character representable in <i>D</i> is also
     * representable in <i>C</i>.  If this relationship holds then it is
     * guaranteed that every string that can be encoded in <i>D</i> can also be
     * encoded in <i>C</i> without performing any replacements.
     *
     * <p> That <i>C</i> contains <i>D</i> does not imply that each character
     * representable in <i>C</i> by a particular byte sequence is represented
     * in <i>D</i> by the same byte sequence, although sometimes this is the
     * case.
     *
     * <p> Every charset contains itself.
     *
     * <p> This method computes an approximation of the containment relation:
     * If it returns <tt>true</tt> then the given charset is known to be
     * contained by this charset; if it returns <tt>false</tt>, however, then
     * it is not necessarily the case that the given charset is not contained
     * in this charset.
     *
     * @return  <tt>true</tt> if, and only if, the given charset
     *          is contained in this charset
     */
    public boolean contains(Charset cs) {
        return cs instanceof Base64Charset;
    }
    
    
}
