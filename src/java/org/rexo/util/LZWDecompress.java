
package org.rexo.util;

import java.io.*;

/** Credits to gzip for the code. The code below is a ported version of the
 * code used in the gzip code. Also used some LZW code from DDJournal
 * @author Kedar Bellare
 */

public class LZWDecompress {
  private InputStream _zin;
  private OutputStream _unzout;
  private int _byteOrder;
  private int inSize;
  private int inPtr;
  private long maxmaxcode;
  private int block_mode;
  private int maxbits;

  // Error codes
  public static final int OK = 0;
  public static final int ERROR = 1;

  // Constants needed for LZW
  private static final int BITS = 16;
  private static final int INIT_BITS = 9;
  private static final int LZW_MAGIC0 = 0x1f;
  private static final int LZW_MAGIC1 = 0x9d;
  private static final int CLEAR = 256;
  private static final int FIRST = CLEAR + 1;

  private static final int BIT_MASK = 0x1f;
  private static final int BLOCK_MODE = 0x80;

  private static final int SHIFTCOUNT = 4, MAXVALUE = (1 << 16) - 1, MAXCODE = MAXVALUE - 1, TABLESIZE  = 66000;

  private static final int LZW_RESERVED = 0x60; /* reserved bits */
  private static final int INBUFSIZE = 0x8000;
  private static final int INBUF_EXTRA = 64;

  public LZWDecompress(InputStream in, OutputStream out) {
    _zin = in;
    _unzout = out;
    inSize = inPtr = 0;
  }

  public void initByteOrder(int ord) {
    _byteOrder = ord;
  }

  private int getByte() {
    try {
      int avail = _zin.available();

      if (avail > 0)
        return _zin.read();
    }
    catch (IOException ioe) {
      ioe.printStackTrace();
    }

    return -1;
  }

  private int getBytes(byte[] buf, int n) {
    int rsize = -1;
    
    try {
      rsize = _zin.read(buf, 0, n);
    }
    catch (IOException ioe) {
      ioe.printStackTrace();
    }

    return rsize;
  }

  // TODO: Optimize.
  private long inputCode(byte[] buf, long o, long m) {
    long retCode = -1;
    int index = (int) o>>3;
    int p0 = buf[index] & 0xff;
    int p1 = buf[index+1] & 0xff;
    int p2 = buf[index+2] & 0xff;
    
    //System.err.println("Made use of : " + index + " " + (index+1) + " " + (index+2));
    retCode = ((((long)p0) | (((long)p1)<<8) | (((long)p2)<<16))>>(o&0x7))&m;

    return retCode;
  }

  /* Checks whether file is lzw compressed and if yes, then skips over header
   * bytes in the input.
   */
  public boolean checkLZWHeader() {
    int inByte1, inByte2;

    inByte1 = getByte();
    inPtr++;
    inByte2 = getByte();
    inPtr++;
    if (inByte1 == LZW_MAGIC0 && inByte2 == LZW_MAGIC1) {
      //System.err.println("In LZW format.");
      return true;
    }
    else return false;
  }

  private static long MAXCODE(int n) {
    return (1L << n);
  }

  /*
   * @return Returns the error code which occured during processing.
   */
  public int process() {
    // Error checking
    maxbits = getByte();
    inPtr++;
    block_mode = maxbits & BLOCK_MODE;
    if ((maxbits & LZW_RESERVED) != 0) {
      //System.err.println("Warning : Unknown flags 0x" + (maxbits & LZW_RESERVED));
    }
    maxbits &= BIT_MASK;
    maxmaxcode = MAXCODE(maxbits);

    if (maxbits > BITS) {
      //System.err.println("Error : Compressed with " + maxbits + " bits, can only handle " + BITS);
      return ERROR;
    }
    
    try {
      int nExp = Expand(_unzout);

      return OK;
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    return ERROR;
  }

  /** Expand Compressed codes back to raw data.
   * This will fail if input was not LZW compressed data.
   * @param inComp unpacker from source.
   * @param outExp destination for decoded bytes.
   * @return count of decoded raw data bytes.
   * @exception IOException from either underlying stream.
   * @exception IllegalArgumentException if input was not LZW compressed data.
   */
  public int Expand(OutputStream outExp) throws IOException, IllegalArgumentException {
    // code_int is long
    int outctr = 0;
    int nPushed;
    long posbits = inPtr<<3;
    long inbits;
    int n_bits = INIT_BITS;
    long code;
    long oldCode = -1;
    long maxcode = MAXCODE(n_bits) - 1;
    long inCode;
    long bitmask = (1L<<n_bits) - 1;
    long free_ent = (block_mode != 0 ? FIRST : 256);
    int finchar = 0;
    byte inbuf[]; // Do not unnecessarily allocate space
    LZWHashtable tabPrefixOf = new LZWHashtable(), tabSuffixOf = new LZWHashtable();
    LZWByteStack charStack = new LZWByteStack();
    
    /* codes 0-255 already represent themselves as possible byte values */
    for (code = 255; code >= 0; --code) 
      tabSuffixOf.put(code, code);
    tabPrefixOf.clear();
      
    // TODO: Fill in resetbuf part
    long nextCode = 256;
    inbuf = new byte[INBUFSIZE];
    // Read chunk of data to process into inbuf
    inSize = _zin.read(inbuf, 0, INBUFSIZE);
    int rsize = inSize;
    posbits = 0;

    do {
      int k, e, o;
      o = (int) (posbits>>3);
      e = inSize - o;

      for (k = 0; k < e; ++k) {
        inbuf[k] = inbuf[k+o];
      }
      inSize = e;
      posbits = 0;
      //System.err.println("Posbits: " + posbits + " i: " + k + " e: " + e + " o: " + o);

      if (inSize < INBUF_EXTRA) {
        rsize = _zin.read(inbuf, inSize, INBUFSIZE-inSize);
        if (rsize < 0) {
          //System.err.println("Error : In read");
        }
        else {
          inSize += rsize;
        }
      }
      inbits = ((rsize > 0) ? ((long)inSize - inSize%n_bits)<<3 : ((long)inSize<<3)-(n_bits-1));
      if (inbits <= 0) break;

      while (inbits > posbits) {
        if (free_ent > maxcode) {
          posbits = ((posbits-1) + ((n_bits<<3)-(posbits-1+(n_bits<<3))%(n_bits<<3)));
          ++n_bits;
          if (n_bits == maxbits) {
            maxcode = maxmaxcode;
          } else {
            maxcode = MAXCODE(n_bits)-1;
          }
          bitmask = (1<<n_bits)-1;
          // goto resetbuf
          break;
        }
        code = (int) inputCode(inbuf, posbits, bitmask);
        posbits += n_bits;

        if (oldCode == -1) {
          if (code >= 256) {
            System.err.println("Error in input.");
            return -1;
          }
          // Output the character
          oldCode = code;
          finchar = (int) oldCode;
          _unzout.write(finchar);
          continue;
        }
        if (code == CLEAR && block_mode != 0) {
          //System.err.println("Error: Not handled CLEAR code!");
          tabPrefixOf.clear();
          free_ent = FIRST - 1;
          posbits = ((posbits-1) + ((n_bits<<3)-(posbits-1+(n_bits<<3))%(n_bits<<3)));
          maxcode = MAXCODE(n_bits = INIT_BITS)-1;
          bitmask = (1<<n_bits)-1;
          break;
        }

        inCode = code;
        charStack.pop();

        if (code >= free_ent) { // Special case for KwKwK string
          if (code > free_ent) {
            System.err.println("Error : Corrupt input? Code: " + code + " Free: " + free_ent);
            //return -1;
          }
          charStack.push(finchar);
          code = oldCode;
        }

        while (code >= 256) {
          int retCode = (int) tabSuffixOf.get(code);
          if (retCode < 0) System.err.println("Error : Push code < 0 onto stack!");
          //System.err.println("Pushing1: " + ((char) retCode));
          charStack.push(retCode);
          code = tabPrefixOf.get(code);
        }
        finchar = (int) tabSuffixOf.get(code);
        charStack.push(finchar);
        //System.err.println("Pushing2: " + ((char)finchar));


        while (!charStack.isEmpty())
          _unzout.write(charStack.pop());

        // Generate new entry
        code = free_ent;
        if (code < maxmaxcode) {
          tabPrefixOf.put(code, oldCode);
          tabSuffixOf.put(code, finchar);
          free_ent = code + 1;
        }
        oldCode = inCode;
      }
    } while (rsize != 0);

    return outctr;
  }

  public static void main(String args[]) {
    BufferedInputStream bin;
    if (args.length > 0) {
      try {
      bin = new BufferedInputStream(new FileInputStream(new File(args[0])));
      }
      catch (FileNotFoundException fnf) {
        fnf.printStackTrace();
        return;
      }
    }
    else 
      bin = new BufferedInputStream(System.in);
    OutputStream bout = System.out;
    LZWDecompress gunz = new LZWDecompress(bin,bout);

    /* TODO: Check for well-formedness of the input stream i.e. can it be
     * seek()ed? 
     **/
    //gunz.initByteOrder(gunz.LOW2HIGH);
    if (!gunz.checkLZWHeader()) {
      System.err.println("ERROR: Input not in LZW format!");
      return;
    }
    gunz.process();
  }
}

/* Note that java.util.stack handles Objects not native types,
 * so it is neither convenient nor efficient here. */

class LZWByteStack {
  // byte is enough for 0-255 problem is getting unsigned
  private int[] ints;
  private int ctr = 0;
  public LZWByteStack() { this(4000); }
  public LZWByteStack(int size) { ints = new int[size]; }
  public boolean isEmpty() { return (ctr == 0); }
  public void push (int bv) { ints[ctr++] = bv; }
  public int pop () { 
    if (ctr <= 0) return -1; 
    else return ints[--ctr]; 
  }
}

/* For native objects long stored as ordered array */
/** TODO: Optimize. Only need a fixed history not whole? */
class LZWHashtable {
  int size = 4096;
  int increment = 1024;
  long store[];

  public LZWHashtable() {
    store = new long[size];
  }
  
  public LZWHashtable(int n) {
    size = n;
    store = new long[size];
  }

  private void growIfNeeded(int co) {
    int newsize = size + increment;
    long newstore[];

    if (co >= size) {
      newstore = new long[newsize];
      System.arraycopy(store,0,newstore,0,store.length);

      store = newstore;
      size = newsize;
      //System.err.println("Growing size .. ");
    }
  }
  
  public long put(int code, long what) {
    growIfNeeded(code);
    long earlierValue = store[code];
    store[code] = what;

    return earlierValue;
  }

  public long put(long code, long what) {
    return put((int) code, what);
  }

  public long get(int code) {
    if (code >= size) return 0;
    else return store[code];
  }

  public long get(long code) {
    return get((int) code);
  }

  public void clear() {
    store = null;
    size = 500;
    store = new long[size];
  }
}
