
package org.janeth.jennynet.intfa;

import java.util.List;

/** Interface for a device to serialise objects of registered classes.
 * Allows to register Java classes for the serialisation / de-serialisation
 * functions realised by this device. Performs serialisation and de-serialisation
 * of objects which have to be instances of one of the registered classes.
 * 
 * <p>Each <code>Serialisation</code> instance bears a code name for the 
 * serialisation method it realises. In the system there may exist more 
 * than one instances with the same method name. 
 * 
 * <p>Individuality of devices comes from the possibility to bear different
 * sets of registered class for serialisation. Again there is no restriction
 * of multiplicity of devices and identical clones may exist. 
 * 
 * <p>IMPORTANT!! Depending on the concrete serialisation method, the order of 
 * registering of classes at this interface may matter for the proper functioning
 * of serialisation and de-serialisation. 
 * 
 * <p>A reference to the <code>Connection</code> for which a serialisation
 * instance will be working can be associated optionally or may be left void.  
 */

public interface Serialization 
{

	/** Returns a clone of this Serialisation object. If a reference to a 
	 * <code>Connection</code> was defined, it is included in the clone. 
	 * 
	 * @return ISerialization
	 */
	public Serialization copy ();

   /** Returns a clone of this Serialisation object, optionally with a new
    * connection reference. If the parameter is null, an existing connection
    * reference will not appear on the clone.
    *
    * @param c <code>Connection</code> connection for which the copy will be
    *          working; may be null for no connection
    * @return ISerialization
    */
   public Serialization copyFor (Connection c);

	/** Whether the given class is registered in this serialisation device.
	 * 
	 * @param c <code>Class</code> class to investigate
	 * @return boolean true if and only if class is registered for transmission
    * @throws NullPointerException if parameter is null
	 */
   public boolean isRegisteredClass (Class c);
	
   /** Registers the given class for serialisation in this device. 
    * 
    * @param c <code>Class</code> class to register for serialisation
    * @throws NullPointerException if parameter is null
    */
   public void registerClass (Class c);

   /** Returns a data block with serialisation of the given object.
    * 
    * @param object <code>Object</code> object to serialise
    * @return byte array holding the object serialisation
    *         (and nothing but the object serialisation)
    */
   public byte[] serialiseObject (Object object);

   /** De-serialises a data block and returns the de-serialised object.
    *  
    * @param buffer byte[] serialisation data block
    * @return <code>Object</code> de-serialised object
    */
   public Object deserialiseObject (byte[] buffer);

   /** A code name for the serialisation method performed by this
    * <code>Serialisation</code> device.
    * 
    * @return int serialisation method code
    */
   public int getMethodID ();

   /** Returns the <code>Connection</code> to which this serialisation is
    * associated or null if such a link has not been specified.
    * 
    * @return <code>Connection</code> or null
    */
   public Connection getConnection ();
   
   /** Returns the name of this serialisation. The name consists of a
    * serialisation type name plus a short-ID of the <code>Connection</code>
    * in case there is one associated with this serialisation.
    * 
    * @return String serialisation name
    */
   public String getName ();

   /** Renders the name of this serialisation (<code>getName()</code>).
    *  
    * @return String 
    */
   @Override
   public String toString ();

   /** Returns a list of the classes registered.
    * 
    * @return List&lt;Class&gt;
    */
   public List<Class> getRegisteredClasses ();
}
