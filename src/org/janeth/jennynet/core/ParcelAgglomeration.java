package org.janeth.jennynet.core;

import java.util.concurrent.LinkedBlockingQueue;

import org.janeth.jennynet.intfa.Connection;

abstract class ParcelAgglomeration extends LinkedBlockingQueue<TransmissionParcel> {

   public static final String THREAD_BASENAME = "Parcel-Receptor at ";
   private Thread worker;
   private String name;
   private boolean terminate;
   
   public ParcelAgglomeration (Connection connection) {
      super(connection.getParameters().getParcelQueueCapacity());
      
      worker = new Thread(THREAD_BASENAME + connection.getLocalAddress().getPort()) {
         @Override
         public void run() {
            setPriority(JennyNet.getBaseThreadPriority());
             
            while (!terminate) {
               try {
                  TransmissionParcel parcel = take();
                  processReceivedParcel(parcel);
                  
               } catch (InterruptedException e) {
                  ParcelAgglomeration.this.interrupted(e);
               } catch (Throwable e) {
                  exceptionThrown(e);
               } 
            }
         }
       };
       worker.start();
   }

   abstract protected void processReceivedParcel(TransmissionParcel parcel) throws Exception;

   /** Called when the receptor thread received an exception 
    * from the working method (<code>processReceivedParcel()</code>).
    * 
    * @param e InterruptedException
    */
   abstract protected void exceptionThrown (Throwable e);

   /** Called when the receptor thread has been interrupted.
    * 
    * @param e InterruptedException
    */
   protected void interrupted (InterruptedException e) {
   }
   
   public String getName() {
      return name;
   }

   /** Sets a name for this agglomeration.
    * Also serves for the thread name if a thread is activated.
    * 
    * @param name String 
    */
   public void setName(String name) {
      this.name = name;
      if (name != null) {
         worker.setName(THREAD_BASENAME.concat(name));
      }
   }

   public int getThreadPriority() {
      return worker.getPriority();
   }

   public void setThreadPriority(int threadPriority) {
      worker.setPriority(threadPriority);
   }

   public void terminate () {
      terminate = true;
      worker.interrupt();
   }

   @Override
   protected void finalize() throws Throwable {
      terminate();
      super.finalize();
   }
   
}
