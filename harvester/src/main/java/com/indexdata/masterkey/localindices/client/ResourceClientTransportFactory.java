package com.indexdata.masterkey.localindices.client;

import com.indexdata.masterkey.localindices.entity.XmlBulkResource;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ResourceClientTransportFactory implements ClientTransportFactory {
  private final XmlBulkResource resource;
  private final StorageJobLogger logger;
  
  Map<String, ClientTransport> clientMap = new HashMap<String, ClientTransport>();
  
  public ResourceClientTransportFactory(XmlBulkResource resource, StorageJobLogger logger) {
    this.resource = resource;
    this.logger = logger;
  }
  
  @Override
  public ClientTransport lookup(URL url) throws ClientTransportError {
    
    ClientTransport transport = null; 
    if ("http".equals(url.getProtocol())) {
      transport = new HttpClientTransport(this, logger);
    }
    else if (url.getProtocol().equals("ftp")) {
      String authority = url.getAuthority();
      if (authority != null) {
	transport = clientMap.get(authority);
	if (transport == null) {
	  transport = new FtpClientTransport(logger);
	  clientMap.put(authority, transport);
	}
      }
      else
	throw new ClientTransportError("No Authority on ftp url: " + url);
    }

    if (transport == null) {
      throw new ClientTransportError("Unsupported protocol: " + url);
    }
    transport.setRecursive(true);
    if (resource.getTimeout() != null) {
      transport.setTimeout(1000*resource.getTimeout());
    }
    transport.setCompressedFormat(resource.getExpectedSchema());
    if (resource.getAllowCondReq())
      transport.setFromDate(resource.getLastUpdated());
    return transport;
  }

}
