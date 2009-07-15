/*
 * Base64Decoder.java
 *
 * Created on 20 December 2005, 12:14
 *
 */

package soupdragon.codec;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;


/**
 * <p>Base 64 decoder class. Converts byte array to characters.</p>
 * <p>If measure is supplied this is a line width, and \n characters
 * are inserted when it's exceeeded.</p>
 *
 * @author Malcolm McMahon
 * @version $Revision: 1.1 $
 */
public class Base64Decoder extends CharsetDecoder {
    Integer measure;
    int charCount;
    char padding;
    String codeChars;
    char[] excessChars;
    int codeState;
    int bits;
    
    /** 
     * Creates a new instance of Base64Decoder 
     * @param cs The Charset that created this object
     * @param padding The padding character, usually =
     * @param measure The line width in characters
     */
    public Base64Decoder(Charset cs,String codeChars, char padding, int measure)  {
        super(cs, 1.33f, 4f);
        this.measure = measure;
        this.codeChars  = codeChars;
        this.padding = padding;
    }
    
    /**
     * @param cs The Charset that created this object
     * @param padding The padding character, usually =
     */
    
    public Base64Decoder(Charset cs,String codeChars, char padding)  {
        super(cs, 1.33f, 4f);
        this.codeChars  = codeChars;
        this.padding = padding;
    }
    
    private boolean flushExcess(CharBuffer out) {
        if(excessChars == null)
            return true;
        for(int i = 0; i < excessChars.length; i++)
            if(out.remaining() > 0)
                out.put(excessChars[i]);
            else {
            char[] nc = new char[excessChars.length - i];
            System.arraycopy(excessChars, i, nc, 0, nc.length);
            excessChars = nc;
            return false;
            }
        excessChars = null;
        return true;
    }
    /**
     * Output one character. These character output methods
     * park characters in excessChars if the buffer overflows.
     * The also add newlines if the measure calls for it.
     * @return true if space found
     */
    private boolean out(CharBuffer out, char ch) {
        if(measure != null && ++charCount > measure) {
            charCount = -1;
            return out(out, '\n', ch);
        }
        if(out.remaining() > 0) {
            out.put(ch);
            return true;
        } else {
            excessChars = new char[]{ch};
            return false;
        }
    }
    /**
     * Output two characters
     * @return true if space found
     */
    private boolean out(CharBuffer out, char ch1, char ch2) {
        if(measure != null && (charCount += 2) > measure) {
            if(charCount - 1 != measure) {
                charCount = 2;
                return out(out, '\n', ch1, ch2);
            } else {
                charCount = 1;
                return out(out, ch1, '\n', ch2);
            }
        }
        switch(out.remaining()) {
            case 0:
                excessChars = new char[]{ch1, ch2};
                return false;
            case 1:
                out.put(ch1);
                excessChars = new char[]{ch2};
                return false;
            default:
                out.put(ch1);
                out.put(ch2);
                return true;
        }
    }
    
    private boolean out(CharBuffer out, char ch1, char ch2, char ch3) {
        switch(out.remaining()) {
            case 0:
                excessChars = new char[]{ch1, ch2, ch3};
                return false;
            case 1:
                out.put(ch1);
                excessChars = new char[]{ch2, ch3};
                return false;
            case 2:
                out.put(ch1);
                out.put(ch2);
                excessChars = new char[]{ch3};
                return false;
            default:
                out.put(ch1);
                out.put(ch2);
                out.put(ch3);
                return true;
        }
    }
    
    /**
     * Flushes this decoder.
     *
     * <p> The default implementation of this method does nothing, and always
     * returns {@link CoderResult#UNDERFLOW}.  This method should be overridden
     * by decoders that may need to write final characters to the output buffer
     * once the entire input sequence has been read. </p>
     *
     * @param  out
     *         The output character buffer
     *
     * @return  A coder-result object, either {@link CoderResult#UNDERFLOW} or
     *          {@link CoderResult#OVERFLOW}
     */
    protected java.nio.charset.CoderResult implFlush(CharBuffer out) {
        if(!flushExcess(out))
            return CoderResult.OVERFLOW;
        switch(codeState) {
            case 0:  // full quanta count
                implReset();
                return CoderResult.UNDERFLOW;
            case 1:
                if(!out(out, codeChars.charAt(bits), padding))
                    return CoderResult.OVERFLOW;
                break;
            case 2:
                if(!out(out, codeChars.charAt(bits)))
                    return CoderResult.OVERFLOW;
                break;
                
        }
        if(out(out, padding)) {
            implReset();
            return CoderResult.UNDERFLOW;
        } else
            return CoderResult.OVERFLOW;
        
    }
    
    /**
     * Decodes one or more bytes into one or more characters.
     *
     * <p> This method encapsulates the basic decoding loop, decoding as many
     * bytes as possible until it either runs out of input, runs out of room
     * in the output buffer, or encounters a decoding error.  This method is
     * invoked by the {@link #decode decode} method, which handles result
     * interpretation and error recovery.
     *
     * <p> The buffers are read from, and written to, starting at their current
     * positions.  At most {@link Buffer#remaining in.remaining()} bytes
     * will be read, and at most {@link Buffer#remaining out.remaining()}
     * characters will be written.  The buffers' positions will be advanced to
     * reflect the bytes read and the characters written, but their marks and
     * limits will not be modified.
     *
     * <p> This method returns a {@link CoderResult} object to describe its
     * reason for termination, in the same manner as the {@link #decode decode}
     * method.  Most implementations of this method will handle decoding errors
     * by returning an appropriate result object for interpretation by the
     * {@link #decode decode} method.  An optimized implementation may instead
     * examine the relevant error action and implement that action itself.
     *
     * <p> An implementation of this method may perform arbitrary lookahead by
     * returning {@link CoderResult#UNDERFLOW} until it receives sufficient
     * input.  </p>
     *
     * @param  in
     *         The input byte buffer
     *
     * @param  out
     *         The output character buffer
     *
     * @return  A coder-result object describing the reason for termination
     */
    public java.nio.charset.CoderResult decodeLoop(java.nio.ByteBuffer in, java.nio.CharBuffer out) {
        if(!flushExcess(out))
            return CoderResult.OVERFLOW;
        while(in.remaining() > 0) {
            int b = in.get() & 0xff;
            switch(codeState) {
                case 0:
                    bits = (b << 4) & 0x30;
                    codeState = 1;
                    if(!out(out, codeChars.charAt((b >> 2) & 0x3f)))
                        return CoderResult.OVERFLOW;
                    break;
                case 1:
                    int cc = bits | (b >> 4) & 0xf;
                    bits = (b << 2) & 0x3c;
                    codeState = 2;
                    if(!out(out, codeChars.charAt(cc)))
                        return CoderResult.OVERFLOW;
                    break;
                case 2:
                    cc = bits | (b >> 6) & 3;
                    codeState = 0;
                    if(!out(out, codeChars.charAt(cc), codeChars.charAt(b & 0x3f)))
                        return CoderResult.OVERFLOW;
            }
        }
        return CoderResult.UNDERFLOW;
    }
    
    /**
     * Resets this decoder, clearing any charset-specific internal state.
     *
     * <p> The default implementation of this method does nothing.  This method
     * should be overridden by decoders that maintain internal state.  </p>
     */
    protected void implReset() {
        bits = 0;
        excessChars = null;
        codeState = 0;
        charCount = 0;
        
    }
    
}
