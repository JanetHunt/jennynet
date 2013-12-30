
package org.jhunt.jennynet.intfa;


/** Interface for a device to serialise objects of registered classes.
 * Allows to register Java classes for the serialisation method
 * represented by this device. Performs serialisation and de-serialisation
 * of objects which have to be instances of one of the registered classes.
 * 
 *  <p>Each <code>Serialisation</code> instance bears a code name for the 
 *  serialisation method it represents (there may be more than one instances 
 *  with the same method name). Depending on the concrete method, the order
 *  of registering of classes matters for the proper functioning of the 
 *  serialisation device. 
 */

public interface Serialization 
{

	/** Returns a clone of this Serialisation object. 
	 * 
	 * @return ISerialization
	 */
	public Serialization copy ();

	/** Whether the given class is registered in this serialisation
	 * for net transmission.
	 * 
	 * @param c <code>Class</code>
	 * @return boolean true if and only if class c is registered for transmission
    * @throws NullPointerException if parameter is null
	 */
   public boolean isRegisteredClass(Class c);
	
   /** Registers the given class for net transmission in this 
    * serialisation.
    * 
    * @param c <code>Class</code> class to register
    * @throws NullPointerException if parameter is null
    */
   public void registerClassForTransmission (Class c);

   /** Returns a serialisation data block of the given object.
    * 
    * @param object Object object to serialise
    * @return byte array holding the object serialisation
    *         (and nothing but the object serialisation)
    */
   public byte[] serialiseObject (Object object);

   /** De-serialises a serialisation data block and returns the de-serialised
    * object.
    *  
    * @param buffer byte[] serialisation block
    * @return Object de-serialised object
    */
   public Object deserialiseObject (byte[] buffer);

   /** A code name for the serialisation method performed by this
    * <code>Serialisation</code> instance.
    * 
    * @return int method code
    */
   public int getMethodID ();
   
}
