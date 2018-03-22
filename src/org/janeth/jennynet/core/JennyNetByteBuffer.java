package org.janeth.jennynet.core;

import org.janeth.jennynet.util.CRC32;

/** 
 * Class for sending byte data over the net. This can be used to perform
 * transmissions of user defined object serialisations. 
 * 
 * <p>This class is by default globally registered for transmission over
 * the net. 
 *
 */
public class JennyNetByteBuffer {
   protected byte[] data;
   protected transient int crc32;
   
   /** Creates a new byte buffer class from a section of a given 
    * data buffer.
    * 
    * @param buffer byte[] data buffer
    * @param start int offset
    * @param length int data length
    * @throws IllegalArgumentException if data addressing is wrong
    */
   public JennyNetByteBuffer (byte[] buffer, int start, int length) {
      // validity testing
      if (buffer == null )
         throw new NullPointerException("buffer == null");
      if (start < 0 | length < 0 | start+length > buffer.length)
         throw new IllegalArgumentException("illegal start/length setting for byte data");
      
      data = new byte[length];
      System.arraycopy(buffer, start, data, 0, length);
   }

   /** Creates a new byte buffer class from the given data buffer. 
    */
   public JennyNetByteBuffer (byte[] buffer) {
      data = buffer.clone();
   }

   protected JennyNetByteBuffer () {
   }
   
   /** Returns the stored data buffer.
    * 
    * @return byte[] data buffer (may be null) 
    */
   public byte[] getData () {
      return data;
   }
   
   public void setData (byte[] block) {
      data = block;
      crc32 = 0;
   }
   
   /** Returns the length of the stored data.
    * 
    * @return int data length
    */
   public int getLength () {
      return data == null ? 0 : data.length;
   }
   
   /** Returns a CRC32 value for the contained buffer data.
    * 
    * @return long CRC32 value
    */
   public int getCRC () {
      if (data == null) return 0;
      if (crc32 == 0) {
         CRC32 crc = new CRC32();
         crc.update(data);
         crc32 = crc.getIntValue();
      }
      return crc32;
   }
}
