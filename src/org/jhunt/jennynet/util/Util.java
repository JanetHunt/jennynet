package org.jhunt.jennynet.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;
import java.util.UUID;

import org.jhunt.jennynet.core.JennyNet;

public class Util {

   private static Random rand;
//   private static final byte[] BLANKBUFFER = new byte[ 4096 ];
   
   static {
      long seed = System.currentTimeMillis();
      rand = new Random( seed );
   }

   /**
    * Converts a byte array to a hexadecimal string.  Conversion starts at byte 
    * <code>offset</code> and continues for <code>length</code> bytes.
    * 
    * @param b      the array to be converted.
    * @param offset the start offset within <code>b</code>.
    * @param length the number of bytes to convert.
    * @return A string representation of the byte array.
    * @throws IllegalArgumentException if offset and length are misplaced
    */
   public static String bytesToHex( byte [] b, int offset, int length )
   {
      StringBuffer   sb;
      String         result;
      int i, top;
   
      top = offset + length;
      if ( length < 0 || top > b.length )
         throw new IllegalArgumentException();
   
      sb = new StringBuffer();
      for ( i = offset; i < top; i++ ) {
         sb.append( byteToHex( b[i] ) );
      }
      result = sb.toString();
      return result;
   }

   /**
    * Converts a byte array to a hexadecimal string.  
    * 
    * @param b      the array to be converted.
    * @return A string representation of the byte array.
    */
   public static String bytesToHex( byte [] b )
   {
      return bytesToHex( b, 0, b.length );
   }

   /** Returns a two char hexadecimal String representation of a single byte.
    * 
    * @param v integer with a byte value (-128 .. 255); other values get truncated
    * @return an absolute hex value representation (unsigned) of the input
    */
   public static String byteToHex ( int v )
   {
      String hstr;
      hstr = Integer.toString( v & 0xff, 16 );
      
      return hstr.length() == 1 ? "0" + hstr : hstr;
   }

   /**
    * Returns a byte array of the specified length filled with random values. 
    * @param length length of the returned byte array in bytes
    * @return random byte array
    */
   public static byte[] randBytes ( int length )
   {
      byte[] buf = new byte[ length ];
      rand.nextBytes( buf );
      return buf;
   }
   
   /**
    * Returns a random String of the specified length. The characters will be
    * in the range (char)30 .. (char)137.
    */
   public static String randString ( int length )
   {
      StringBuffer sb;
      int i;
      
      sb = new StringBuffer( length );
      for ( i = 0; i < length; i++ )
         sb.append( (char)(nextRand( 108 ) + 30) );
      return sb.toString();
   }

   /**
    * Returns the next random integer value in the range 0 .. <code>range</code>-1.
    * @param range a positive integer greater 0
    * @return random value in the range 0 .. <code>range</code>-1
    */
   public static int nextRand ( int range )
   {
      if ( range < 1 )
         throw new IllegalArgumentException("range must be positive greater zero");
      
      return rand.nextInt( range );
   }

   /**
    * Creates a new temporary file with prefix="jnet-" and suffix ".temp".
    * The file is created in JennyNet global TEMP folder.
    * 
    * @return the <code>File</code> representation of the created temporary file
    * @throws java.io.IOException
    */
   public static File getTempFile () throws java.io.IOException
   {
      return getTempFile( JennyNet.getDefaultTempDirectory() );
   }

   /**
    * Creates a new temporary file with prefix="jnet-" and suffix ".temp".
    * 
    * @param dir the directory where the file is to be created or <b>null</b>
    *        for the default temp-file directory
    * @return the <code>File</code> representation of the created temporary file
    * @throws java.io.IOException
    */
   public static File getTempFile ( File dir ) throws java.io.IOException
   {
      return File.createTempFile( "jnet-", ".temp", dir );
   }

   /**
    * Transfers the contents of the input stream to the output stream
    * until the end of input stream is reached.
    * 
    * @param input the input stream (non-null)
    * @param output the output stream (non-null)
    * @param bufferSize the size of the transfer buffer
    * @throws java.io.IOException
    */
   public static void transferData ( InputStream input, OutputStream output,
         int bufferSize  )
   throws java.io.IOException
   {
      byte[] buffer = new byte[ bufferSize ];
      int len;
   
      while ( (len = input.read( buffer )) > 0 )
      {
         output.write( buffer, 0, len );
      }
   }

   /** Creates a 4-byte short ID from the given UUID value.
    * 
    * @param uuid java.util.UUID
    * @return 4 byte array
    */
   public static byte[] makeShortId (UUID uuid) {
      long s1 = uuid.getLeastSignificantBits() ^ uuid.getMostSignificantBits();
      int s2 = (int)(s1 >>> 32) ^ (int)(s1 & 0xFFFFFFFF);
      byte[] res = new byte[4];
      writeInt(s2, res, 0);
      return res;
   }

   /**
    * Writes a 32-bit integer value to a byte array as
    * 4 sequential bytes in a Big-Endian manner 
    * (most significant stored first).
    *  
    * @param v int, the value to be written
    * @param dest the destination byte array
    * @param offs the start offset in <code>dest</code>
    * @since 2-1-0
    */
   public static void writeInt ( int v, byte[] dest, int offs )
   {
      dest[ offs ]     = (byte)(  (v >>> 24) );
      dest[ offs + 1 ] = (byte)(  (v >>> 16) );
      dest[ offs + 2 ] = (byte)(  (v >>>  8) );
      dest[ offs + 3 ]     = (byte)(  v );
   }

   /** Converts a textual hexadecimal integer representation into a corresponding
    *  byte value array. 
    * 
    * @param hex textual hex value
    * @return array of derived value bytes
    * @since 0-4-0        
    */
   public static byte[] hexToBytes ( String hex )
   {
      ByteArrayOutputStream out;
      int i, pos;
      
      if ( hex.length() % 2 != 0 )
         throw new IllegalArgumentException( "hex string must be even" );
      
      out = new ByteArrayOutputStream( hex.length() / 2 );
      pos = 0;
      while ( pos < hex.length() ) {
         i = Integer.parseInt( hex.substring( pos, pos+2 ), 16 );
         out.write( i );
         pos += 2;
      }
      return out.toByteArray();
   }  // hexToBytes

   /** Whether two byte arrays have equal contents.
    * 
    * @param a first byte array to compare
    * @param b second byte array to compare
    * @return <b>true</b> if and only if a) a and b have the same length, and 
    *          b) for all indices i for 0 to length holds a[i] == b[i]
    */
   public static boolean equalArrays ( byte[] a, byte[] b )
   {
      if ( a == null && b == null )
         return true;
      if ( a.length != b.length )
         return false;
      
      for ( int i = 0; i < a.length; i++ )
         if ( a[i] != b[i] )
            return false;
      return true;
   }

   
   /**
    * Creates a new temporary file name with prefix="jnet-" and suffix ".temp".
    * The temporary file does not exist when this method returns.
    * 
    * @param dir the directory where the file shall be located or <b>null</b>
    *        for the default temp-file directory
    * @return the <code>File</code> unique temporary file name
    * @throws java.io.IOException
   public static File getTempFileName(File dir) throws IOException {
      File f = getTempFile(dir);
      f.delete();
      return f;
   }
    */
   
   

}
