package com.indexdata.masterkey.localindices.crawl;

import java.net.URL;

public class SiteRequest {

    public URL url = null;
    // where to start the job
    public int maxdepth = 0;
    // how deep to recurse
    public int seen = 0;
    // how many pages harvested
    public int togo = 0;
}
