package org.jhunt.jennynet.intfa;


public interface IObjectHeader {

   /** The object's identifier number.
    * 
    * @return long object ID
    */
   long getObjectID ();

   /** A path text associated with the transmitted object.
    * This is only optionally available.
    * 
    * @return String path or null
    */
   String getPath ();

   /** The serialisation method applicable.
    * 
    * @return int code for serialisation method
    */
   int getSerialisationMethod ();

   /** The total size of transmitted serial data for the object.
    * 
    * @return int
    */
   int getTransmissionSize ();

   /** Number of parcels transmitted for the object. 
    * 
    * @return int number of parcels
    */
   int getNumberOfParcels ();

   /** Whether the data set of this header record is valid. 
    * 
    * @return boolean
    */
   boolean verify();

   void setTransmissionSize(int length);

   void setNrOfParcels(int nrOfParcels);
   
   void setPath(String path);

   void setMethod(int method);

}
