package org.janeth.jennynet.util;

import java.util.Timer;
import java.util.TimerTask;

/** A java.util.TimerTask that can be scheduled at a Timer with pre-defined
 * delay and period parameters.
 * 
 */
public abstract class SchedulableTimerTask extends TimerTask {
   private final int delay;
   private final int period;
//   private String text;

   /** Creates a new schedulable timer task.
    * 
    * @param delay int milliseconds to delay task once scheduled
    * @param period int milliseconds of period for task repetition 
    *               (0 for no repetition) 
    * @param text String debugging text for this timer task (may be null)              
    */
   public SchedulableTimerTask (int delay, int period, String text) {
      this.delay = delay;
      this.period = period;
   }

   /** Creates a new schedulable timer task for one-time execution.
    * 
    * @param delay int milliseconds to delay task once scheduled
    * @param text String debugging text for this timer task (may be null)              
    */
   public SchedulableTimerTask (int delay, String text) {
      this.delay = delay;
      this.period = 0;
   }
   
   /** Schedules this timer-task according to its defined scheduling
    * parameters at the given timer instance.
    * 
    * @param timer java.util.Timer
    */
   public void schedule (Timer timer) {
      if (period > 0) {
         timer.schedule(this, delay, period);
      } else { 
         timer.schedule(this, delay);
      }
//      System.out.println("SCHEDULABLE TIMER (" + 
//      (text==null ? "" : text) + ") scheduled with delay=" + delay + ", period=" + period);
   }

   public int getDelay() {
      return delay;
   }

   public int getPeriod() {
      return period;
   }
   
}
