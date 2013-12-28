package org.jhunt.jennynet.core;

import java.util.LinkedHashMap;

import org.jhunt.jennynet.intfa.Serialization;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class KryoSerialisation implements Cloneable, Serialization {
   private static final int METHOD_ID = 0;
   
   private LinkedHashMap<Class, Class> classMap = new LinkedHashMap<Class, Class>();
   private Kryo kryo = new Kryo();
   
   @Override
   public Serialization copy() {
      KryoSerialisation c = null;
      try { 
         // make a deep clone of this serialisation object
         c = (KryoSerialisation)super.clone();
         c.classMap = (LinkedHashMap)classMap.clone();
         c.kryo = new Kryo();
         for (Class type : classMap.keySet()) {
            c.kryo.register(type);
         }
      } catch (CloneNotSupportedException e) {
         e.printStackTrace();
      }
      return c;
   }

   @Override
   public boolean isRegisteredClass (Class c) {
      return classMap.containsKey(c);
   }

   @Override
   public void registerClassForTransmission (Class c) {
      if ( !classMap.containsKey(c) ) {
         kryo.register(c);
         classMap.put(c, null);
      }
   }

   @Override
   public byte[] serialiseObject (Object object) {
      Output output = new Output(1024, JennyNet.MAX_SERIALBUFFER_SIZE);
      kryo.writeClassAndObject(output, object);
      output.close();
      return output.toBytes();
   }

   @Override
   public Object deserialiseObject (byte[] buffer) {
      Input input = new Input(buffer);
      Object obj = kryo.readClassAndObject(input);
      return obj;
   }

   @Override
   public int getMethodID() {
      return METHOD_ID;
   }

}
