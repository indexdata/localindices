
package com.indexdata.localindexes.scheduler  ;

/**
 * The SchedulerThread does the actual scheduling of harvester threads
 * Simple pseudocode:
 *    * Get a list of active jobs from the WS, and update the joblist
 *    * For each job in joblist
 *       * If time to run it (and not already running), start it
 *       * If running, poll for status, and pass on to the WS
 *    * Sleep a while
 *  
 * @author heikki
 */
public class SchedulerThread implements Runnable  {
    boolean keeprunning;

    public void run() {
        System.err.print("Starting to run the background thread...\n");
        keeprunning = true;
        int i = 0;
        while (keeprunning) {
            System.err.print("Looping " + i + " \n");
            System.err.flush();
            i++;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                System.err.print("Caught an exception" + e.getMessage());
                e.printStackTrace();
            }
        }
        System.err.print("Someone has told us to stop, exiting after " + i + "rounds\n");
    }// run

    public void enough() {
        System.err.print("Telling the thread to stop running");
        keeprunning = false;
    }

}
