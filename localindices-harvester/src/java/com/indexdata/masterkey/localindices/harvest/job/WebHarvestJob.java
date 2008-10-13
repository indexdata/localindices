/* WebHarvestJob
 * Crawls around web sites and stores full text etc
 */
package com.indexdata.masterkey.localindices.harvest.job;

import com.indexdata.masterkey.localindices.entity.WebCrawlResource;
import com.indexdata.masterkey.localindices.harvest.storage.HarvestStorage;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/** WebHarvestJob
 * Crawls around web sites and stores full text, title, url, etc.
 *
 * @author heikki
 */
public class WebHarvestJob implements HarvestJob {

    private static Logger logger = Logger.getLogger("com.indexdata.masterkey.harvester");
    private HarvestStorage storage;
    private HarvestStatus status;
    private String error;
    private WebCrawlResource resource;
    private boolean die = false;

    
    public WebHarvestJob(WebCrawlResource resource) {
        this.resource = resource;
        this.status = HarvestStatus.NEW;
    }

    private synchronized boolean isKillSendt() {
        if (die) {
            logger.log(Level.WARN, "Bulk harvest received kill signal.");
        }
        return die;
    }

    private synchronized void onKillSendt() {
        die = true;
    }

    public void kill() {
        if (status != HarvestStatus.FINISHED) {
            status = HarvestStatus.KILLED;
            onKillSendt();
        }
    }

    public HarvestStatus getStatus() {
        return status;
    }

    public void setStorage(HarvestStorage storage) {
        this.storage = storage;
    }

    public HarvestStorage getStorage() {
        return storage;
    }

    public void finishReceived() {
        status = HarvestStatus.WAITING;
    }

    private void setError( String e) {
        this.error=e;
        status = HarvestStatus.ERROR;
        resource.setError(e);
        logger.log(Level.ERROR, e);
    }
    
    public String getError() {
        return error;
    }

    public void run() {
        status = HarvestStatus.RUNNING;
        resource.setCurrentStatus("Sleeping");
        try {
            resource.setCurrentStatus("Sleeping");
            Thread.sleep(10000);
            resource.setCurrentStatus("Woke up");
        } catch (InterruptedException ex) {
            setError("Web crawler didn't even manage to sleep!");
        }
        if ( isKillSendt() ) {
            setError("Web Crawl interrupted with a kill signal");
        } else {
            setError( "Web Crawl not quite implemented yet");
        }
    } // run()

}

