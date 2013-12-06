package org.jhunt.jennynet.util;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class ArraySet<E> extends AbstractSet<E> 
   implements Cloneable 
   {
   private ArrayList<E> list = new ArrayList<E>();
   
   public ArraySet () {
   }

   public ArraySet ( Collection<E> c ) {
      addAll(c);
   }
   
   @Override
   public Iterator<E> iterator() {
      return list.iterator();
   }

   @Override
   public int size() {
      return list.size();
   }

   @Override
   public boolean add (E e) {
      if ( list.contains(e) ) {
         return false;
      }
      list.add(e);
      return true;
   }

   @Override
   public boolean contains(Object o) {
      return list.contains(o);
   }

   @Override
   public boolean remove(Object o) {
      return list.remove(o);
   }

   @Override
   public void clear() {
      list.clear();
   }

   @Override
   public Object clone() {
      try {
         ArraySet copy = (ArraySet)super.clone();
         copy.list = (ArrayList)list.clone();
         return copy;
      } catch (CloneNotSupportedException e) {
         return null;
      }
   }
   
   
}
