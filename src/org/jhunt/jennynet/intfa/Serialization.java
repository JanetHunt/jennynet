
package org.jhunt.jennynet.intfa;


/** Controls how objects are transmitted over the network. */

public interface Serialization 
{

	/** Returns a clone of this Serialisation object. 
	 * 
	 * @return ISerialization
	 */
	public Serialization copy ();

	/** Whether the given object is registered here for net transmission.
	 * 
	 * @param object Object
	 * @return boolean true if and only if object is registered for transmission
    * @throws NullPointerException if parameter is null
	 */
   public boolean isRegisteredClass(Class c);
	
   public void registerClassForTransmission (Class c);

   public byte[] serialiseObject (Object object);
   
   public Object deserialiseObject (byte[] buffer);

   /** A code for this serialisation method.
    * 
    * @return int method code
    */
   public int getMethodID ();
   
}
