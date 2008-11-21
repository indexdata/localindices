package com.indexdata.masterkey.localindices.crawl;



import java.net.URL;

public class PageRequest {

    public URL url = null;
    // the page to harvest
    public int depth = 0;
    public SiteRequest sitereq = null;

    public PageRequest() {
    }
    
    public PageRequest(URL u) {
        url=u;
    }

    
    
}
