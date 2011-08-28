package com.indexdata.masterkey.localindices.dao.bean;

import java.net.URL;

public class CommonDAOWS {
	
	protected String serviceBaseURL;
	public CommonDAOWS(String serviceUrl) {
		serviceBaseURL = serviceUrl;
	}
	
	protected Long extractId(URL url) {
	    if (url != null) {
	    	String rest = url.toString().substring(serviceBaseURL.length());
    		rest = rest.replace("/", "");
	    	return Long.decode(rest);
	    }
	    return null; 
    }
}
