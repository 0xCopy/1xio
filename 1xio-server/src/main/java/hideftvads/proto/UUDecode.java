package hideftvads.proto;

import sun.misc.*;

import java.io.*;

public class UUDecode {

  public static void main(String[] args) {

    if (args.length > 0) {
      for (int i = 0; i < args.length; i++) {
        try {
          UUDecoder uudc = new UUDecoder();
          InputStream is = new FileInputStream(args[i]);
          byte[] data = uudc.decodeBuffer(is);
          OutputStream os = new FileOutputStream(uudc.bufferName);
          os.write(data);
          os.close();
        }
        catch (IOException e) {
          System.err.println(e);
        }
      }  // end for
    } // end if 
  }  // end main 
}
