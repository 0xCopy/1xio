/*
 * Base64Encoder.java
 *
 * Created on 20 December 2005, 11:15
 *
 */

package soupdragon.codec;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

/**
 * <p>Base-64 Encoder - translates from base-64 text into binary.</p>
 * <p>Whitespace is ignored, so is anything following a terminating = 
 * character.</p>
 * @author Malcolm McMahon
 * @version $Revision: 1.1 $
 */
public class Base64Encoder extends CharsetEncoder {
    final static int CHARCODE_INVALID = -1;
    final static int CHARCODE_PADDER = -2;
    final static int CHARCODE_WHITESPACE = -3;
    
    int[] encTable;
    int encState;
    Byte excessByte;
    int bits;
    /** Creates a new instance of Base64Encoder
     * @param cs The Charset which created this
     * @param encTable Table which maps chacacters onto 6 bit values.
     */
    public Base64Encoder(Charset cs, int[] encTable) {
        super(cs, 1.4f, 2f);
        this.encTable = encTable;
    }
    
    private boolean out(ByteBuffer bb, int outValue) {
        if(bb.remaining() > 0) {
            bb.put((byte)outValue);
            return true;
        } else {
            excessByte = Byte.valueOf((byte)outValue);
            return false;
        }
    }
    
    
    
    
    /**
     * Flushes this encoder.
     *
     * <p> The default implementation of this method does nothing, and always
     * returns {@link CoderResult#UNDERFLOW}.  This method should be overridden
     * by encoders that may need to write final bytes to the output buffer
     * once the entire input sequence has been read. </p>
     *
     * @param  out
     *         The output byte buffer
     *
     * @return  A coder-result object, either {@link CoderResult#UNDERFLOW} or
     *          {@link CoderResult#OVERFLOW}
     */
    protected java.nio.charset.CoderResult implFlush(java.nio.ByteBuffer out) {
        if(encState != 0 && encState != 4)
            throw new IllegalArgumentException("Base-64 text ends prematurely");
        if(excessByte == null) {
            implReset();
            return CoderResult.UNDERFLOW;
        }
        if(out.remaining() > 0) {
            out.put(excessByte.byteValue());
            implReset();
            return CoderResult.UNDERFLOW;
        } else
            return CoderResult.OVERFLOW;
        
    }
    
    /**
     * Encodes one or more characters into one or more bytes.
     *
     * <p> This method encapsulates the basic encoding loop, encoding as many
     * characters as possible until it either runs out of input, runs out of room
     * in the output buffer, or encounters an encoding error.  This method is
     * invoked by the {@link #encode encode} method, which handles result
     * interpretation and error recovery.
     *
     * <p> The buffers are read from, and written to, starting at their current
     * positions.  At most {@link Buffer#remaining in.remaining()} characters
     * will be read, and at most {@link Buffer#remaining out.remaining()}
     * bytes will be written.  The buffers' positions will be advanced to
     * reflect the characters read and the bytes written, but their marks and
     * limits will not be modified.
     *
     * <p> This method returns a {@link CoderResult} object to describe its
     * reason for termination, in the same manner as the {@link #encode encode}
     * method.  Most implementations of this method will handle encoding errors
     * by returning an appropriate result object for interpretation by the
     * {@link #encode encode} method.  An optimized implementation may instead
     * examine the relevant error action and implement that action itself.
     *
     * <p> An implementation of this method may perform arbitrary lookahead by
     * returning {@link CoderResult#UNDERFLOW} until it receives sufficient
     * input.  </p>
     *
     * @param  in
     *         The input character buffer
     *
     * @param  out
     *         The output byte buffer
     *
     * @return  A coder-result object describing the reason for termination
     */
    public java.nio.charset.CoderResult encodeLoop(java.nio.CharBuffer in, java.nio.ByteBuffer out) {
        if(excessByte != null) {
            if(out.remaining() > 0) {
                out.put(excessByte.byteValue());
                excessByte = null;
            } else
                return CoderResult.OVERFLOW;
            
        }
        while(in.remaining() > 0) {
            char inch = in.get();
            int code = (int)inch >= encTable.length ? CHARCODE_INVALID : encTable[(int)inch];
            if(encState < 4) {
                switch(code) {
                    case CHARCODE_INVALID:
                        throw new IllegalArgumentException("Invalid base-64 character'" + inch + "'" );
                    case CHARCODE_WHITESPACE:
                        break;
                    case CHARCODE_PADDER:
                        if(encState == 1)
                            throw new IllegalArgumentException("Mal-formed base-64 (= after one character");
                        encState = 4;
                        break;
                    default:
                        switch(encState) {
                            case 0:
                                bits = code << 2;
                                encState = 1;
                                break;
                            case 1:
                                encState = 2;
                                int v = bits | ((code >> 4) & 3);
                                bits = (code << 4) & 0xF0;
                                if(!out(out, v))
                                    return CoderResult.OVERFLOW;
                                break;
                            case 2:
                                encState = 3;
                                v = bits | (code >> 2) & 0x0f;
                                bits = (code << 6) & 0xC0;
                                if(!out(out, v))
                                    return CoderResult.OVERFLOW;
                                break;
                            case 3:
                                encState = 0;
                                bits |= (code & 0x3f);
                                if(!out(out, bits))
                                    return CoderResult.OVERFLOW;
                                break;
                        }
                        
                        break;
                }
                
                
            }
        }
        return CoderResult.UNDERFLOW;
    }

    /**
     * Reset - clear encoder state
     */
    protected void implReset() {
        encState = 0;
        bits = 0;
        excessByte = null;
    }
    
}
