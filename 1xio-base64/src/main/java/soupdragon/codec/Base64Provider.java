/*
 * Base64Provider.java
 *
 * Created on 21 December 2005, 22:50
 *
 */

package soupdragon.codec;

import java.nio.charset.Charset;
import java.util.Iterator;

/**
 * <p>Character set provider for Bas64 etc.</p>
 * <p>Special function - serves base64:nn where nn is linewidth for
 * formatted output.</p>
 * <p>Also serves hex, HEX, hex:nn and HEX:nn. The case of the hex generated
 * matches the case of the 'H' in hex.</p>
 * @author malcolmm
 */
public class Base64Provider extends java.nio.charset.spi.CharsetProvider {
    
    /** Creates a new instance of Base65Provider */
    public Base64Provider() {
    }
    
    public java.nio.charset.Charset charsetForName(String str) {
        if("base64".equalsIgnoreCase(str) || "base-64".equalsIgnoreCase(str))
            return new Base64Charset();
        if("hex".equalsIgnoreCase(str))
            return new HexCharset(Character.isUpperCase(str.charAt(0)));
        if(str.toLowerCase().startsWith("base64:") || str.toLowerCase().startsWith("base-64:")){
            int idx = str.lastIndexOf(':');
            try {
                int w = Integer.parseInt(str.substring(idx + 1));
                if(w < 4 || w > 1024)
                    return null;
                return new Base64Charset(w);
            } catch(NumberFormatException e) {
                return null;
            }
            
        } else if(str.toLowerCase().startsWith("hex:")) {
            try {
                int w = Integer.parseInt(str.substring(4));
                if(w > 4 && w < 1024) {
                    if((w & 1) != 0)
                        w++;  // force even
                return new HexCharset(Character.isUpperCase(str.charAt(0)), w);
                }
            } catch(NumberFormatException e) {
                return null;
            }
        } 
        return null;
    }
    
    public java.util.Iterator<java.nio.charset.Charset> charsets() {
        return new Iterator<Charset>() {
            int seq;
            
            public boolean hasNext() {
                return seq < 2;
            }
            
            public Charset next() {
                return seq++ == 0 ? new Base64Charset() : new HexCharset(false);
            }
            
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
    
}
