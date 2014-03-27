package com.indexdata.masterkey.localindices.client;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.indexdata.masterkey.localindices.entity.XmlBulkResource;

public class ResourceClientTransportFactory implements ClientTransportFactory {

  
  XmlBulkResource resource;
  
  Map<String, ClientTransport> clientMap = new HashMap<String, ClientTransport>();
  
  public ResourceClientTransportFactory(XmlBulkResource resource) {
    this.resource = resource;
  }
  
  @Override
  public ClientTransport lookup(URL url) {
    
    ClientTransport transport = null; 
    if ("http".equals(url.getProtocol()))
      transport = new HttpClientTransport(this);
    else if (url.getProtocol().equals("ftp")) {
      String authority = url.getAuthority();
      if (authority != null) {
	transport = clientMap.get(authority);
	if (transport == null) {
	  transport = new FtpClientTransport(resource, resource.getLastUpdated());
	  clientMap.put(authority, transport);
	}
      }
      else
	throw new RuntimeException("Not Authority on ftp url: " + url);
    }

    if (transport == null) {
      throw new RuntimeException("Unsupported protocol: " + url);
    }
    if (resource.getTimeout() != null) {
      transport.setTimeout(1000*resource.getTimeout());
    }
    transport.setCompressedFormat(resource.getExpectedSchema());
    if (resource.getAllowCondReq())
      transport.setFromDate(resource.getLastUpdated());
    return transport;
  }

}
