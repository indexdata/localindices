
package com.indexdata.localindexes.scheduler;

import java.util.Date;

import com.indexdata.localindexes.web.entitybeans.Harvestable;





/**
 * a JobInstance is one instance of a harvesting job.
 * It owns the actual harvesting thread, and has some status info,
 * as well as the parameters required to create the harvesting object.
 * 
 * @author heikki
 */
public class JobInstance {

    // private Harvester harvesterThing; // from Marc
    public Harvestable harvestableData; // from Jakub
    private Thread harvestingThread;
    public boolean seen; // for checking what has been deleted
    
    public JobInstance ( Harvestable hable ) {
        this.harvestableData = hable;
        this.seen=false;
        this.harvestingThread=null;
    } // JobInstance constructor
    
    /**
     * kill the job instance
     *   Tell the thread to stop, if running
     *   Tell the harvesterThing to clean up
     */
    public void kill () {
        // hrvesterThing.kill(); 
    } //
    
    public void startThread() {
        if ( harvestingThread == null ) {
        }
        
    } // startThread

    /**
     * Checks if the time has come to run this job
     * @param now  current time to check against
     */
    public boolean timeToRun( ) {
     
        return false;
    } // timeToRun
    
} // class JobInstance
