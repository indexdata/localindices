package com.indexdata.masterkey.localindices.client;

import static com.indexdata.utils.TextUtils.joinPath;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.URL;
import java.util.Date;

import javax.xml.transform.dom.DOMResult;

import org.marc4j.MarcException;
import org.marc4j.MarcStreamReader;
import org.marc4j.MarcWriter;
import org.marc4j.MarcXmlWriter;
import org.marc4j.TurboMarcXmlWriter;

import com.indexdata.masterkey.localindices.entity.XmlBulkResource;
import com.indexdata.masterkey.localindices.harvest.cache.CachingInputStream;
import com.indexdata.masterkey.localindices.harvest.cache.DiskCache;
import com.indexdata.masterkey.localindices.harvest.job.BulkRecordHarvestJob;
import com.indexdata.masterkey.localindices.harvest.job.HarvestStatus;
import com.indexdata.masterkey.localindices.harvest.job.MimeTypeCharSet;
import com.indexdata.masterkey.localindices.harvest.job.RecordStorageConsumer;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import com.indexdata.masterkey.localindices.harvest.storage.RecordDOMImpl;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;
import com.indexdata.masterkey.localindices.harvest.storage.XmlSplitter;
import com.indexdata.xml.filter.SplitContentHandler;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;
import org.xml.sax.SAXException;

public class XmlMarcClient extends AbstractHarvestClient {
  private String errorText = "Failed to download/parse/store : ";
  private String errors = errorText;
  private int splitAt = 1;

  public XmlMarcClient(XmlBulkResource resource, BulkRecordHarvestJob job,
    Proxy proxy, StorageJobLogger logger, DiskCache dc, Date lastRequested) {
    super(resource, job, proxy, logger, dc);
  }

  @Override
  public BulkRecordHarvestJob getJob() {
    return (BulkRecordHarvestJob) job;
  }

  @Override
  public XmlBulkResource getResource() {
    return (XmlBulkResource) resource;
  }

  public int download(RemoteFile file) throws Exception {
    int count = 0;
    if (file.isDirectory()) {
      RemoteFileIterator iterator = file.getIterator();
      while (iterator.hasNext()) {
        count += download(iterator.getNext());
      }
    } else {
      try {
        storeAny(file, proposeCachePath());
        count++;
      } catch (IOException ex) {     
        if (getResource().getAllowErrors()) {
          logger.warn(errorText + file.getAbsoluteName() + ". Error: " + ex.getMessage());
          setErrors(getErrors() + (file.getAbsoluteName() + " "));
        } else {
          throw ex;
        }
      }
    }
    return count;
  }

  private String proposeCachePath() {
    return resource.isCacheEnabled()
      ? joinPath(diskCache.getJobPath(), diskCache.proposeName())
      : null;
  }

  @Override
  public int download(URL url) throws Exception {
    ClientTransportFactory factory = new ResourceClientTransportFactory(
      (XmlBulkResource) resource, logger);
    logger.info("Preparing retrieval of " + url);
    try {
      ClientTransport clientTransport = factory.lookup(url);
      clientTransport.connect(url);
      try {
        RemoteFileIterator iterator = clientTransport.get(url);
        if (iterator.hasNext()) {
          while (iterator.hasNext()) {
            RemoteFile rf = iterator.getNext();
            if (rf.isDirectory()) {
              if (getResource().getRecurse()) {
                logger.info("Found subfolder '"+rf.getName()+"' and recursion is on.");
                download(rf);
              } else {
                logger.info("Found subfolder '"+rf.getName()
                  +"' but recursion is off, ignoring.");
              }
            } else {
              logger.info("Found harvestable file: "+rf.getName());
              download(rf);
            }
          }
        } else {
          getJob().setStatus(HarvestStatus.OK, "Found no files at "+url+ (getResource().getAllowCondReq() ? ", possibly due to filtering. " : ""));
        }
      } catch (ClientTransportError cte) {
        if (getResource().getAllowErrors()) {
          setErrors(getErrors() + (url.toString() + " "));
          return 1;
        } else {
          throw cte;
        }

      }
      // TODO HACK HACK HACK
      Thread.sleep(2000);
      logger.info("Finished - " + url.toString());
    } catch (StopException ex) {
      logger.info("Stop requested. Reason: " + ex.getMessage());
      return 0;
    } catch (Exception ex) {
      if (job.isKillSent()) {
        throw ex;
      }
      if (getResource().getAllowErrors()) {
        logger.warn(errorText + url.toString() + ". Error: " + ex.getMessage());
        setErrors(getErrors() + (url.toString() + " "));
        return 1;
      }
      throw ex;
    }
    return 0;
  }

  public int download(File file) throws Exception {
    try {
      storeAny(new LocalRemoteFile(file, logger), null);
    } catch (StopException ex) {
      logger.info("Stop requested. Reason: " + ex.getMessage());
    }
    return 0;
  }
  
  private void storeAny(RemoteFile file, String cacheFile) throws IOException {
    storeAny(file, cacheFile, true);
  }

  private void storeAny(RemoteFile file, String cacheFile, boolean shouldBuffer) throws
    IOException {
    //buffer reads
    InputStream input = shouldBuffer 
      ? new BufferedInputStream(file.getInputStream())
      : file.getInputStream();
    //first we attempt to deduce real content type
    if (file.getContentType() == null) {
      String cT = URLConnection.guessContentTypeFromStream(input);
      if (cT == null) {
        cT = URLConnection.guessContentTypeFromName(file.getName());
        if (cT != null) {
          logger.debug("Guessed content type from filename: "+cT);
        }
      } else {
        logger.debug("Guessed content type from stream: "+cT);
      }
      file.setContentType(cT);
    } else {
      logger.debug("Content type provided by transport: "+file.getContentType());
    }
    //octet-stream may mean marc or a compressed archive
    if (file.getContentType() == null || "application/octet-stream".equals(file.getContentType())) {
      if (file.getName() != null) {
        if (file.getName().endsWith(".zip")) {
          file.setContentType("application/zip");
        } else if (file.getName().endsWith(".gz")) {
          file.setContentType("application/gzip");
        } else { //assume marc for missing content type
          file.setContentType("application/marc");
        }
        logger.debug("Guessed content type from filename: "+file.getContentType());
      }
    }
    /*
    TODO detect binary marc:
    0,1,2,3,4 digits
    12,13,14,15,16 digits
    20 - 4
    21 - 5
    */
    MimeTypeCharSet mimeType = new MimeTypeCharSet(file.getContentType());
    //TODO RemoteFile abstraction is not good enough to make this clean
    //some transports may deal with compressed files (e.g http) others may not
    //if we end up with a compressed mimetype we need to decompress
    if (mimeType.isZip()) {
      logger.debug("Transport returned compressed file type, need to expand");
      ZipInputStream zis = new ZipInputStream(input);
      try {
        RemoteFileIterator it = new ZipRemoteFileIterator(file.getURL(),
          zis, null, logger);
        int count = 0;
        while (it.hasNext()) {
          RemoteFile rf = it.getNext();
          logger.info("Found harvestable file: "+rf.getName());
          try {
            storeAny(rf, proposeCachePath());
          } catch (IOException ex) {
            if (getResource().getAllowErrors()) {
              logger.warn(errorText + file.getAbsoluteName() + ". Error: " + ex.
                getMessage());
              setErrors(getErrors() + (file.getAbsoluteName() + " "));
            } else {
              throw ex;
            }
          }
          count++;
        }
        if (count == 0) {
          logger.debug("Found no files in the archive.");
        }
        return;
      } finally {
        //we need to close in case the iteration did not exhause all entries
        zis.close();
      }
    } else if (mimeType.isGzip()) {
      //built-in gzip does not support tar files
      //TODO use apache-compress
    }
    // user mime-type override
    if (getResource().getExpectedSchema() != null
      && !getResource().getExpectedSchema().isEmpty()) {
      logger.debug("Applying user content type override: "+getResource().getExpectedSchema());
      mimeType = new MimeTypeCharSet(getResource().getExpectedSchema());
    }
    //cache responses to filesystem
    if (cacheFile != null) {
      input = new CachingInputStream(input, cacheFile);
    }
    try {
      if (mimeType.isMimeType("application/marc")) {
        logger.debug("Setting up Binary MARC reader ("+mimeType+")");
        storeMarc(input, mimeType.getCharset());
      } else if (mimeType.isXML()) {
        logger.debug("Setting up XML reader ("+mimeType+")");
        storeXml(input);
      } else {
        logger.info("Ignoring file '"+file.getName()
          +"' because of unsupported content-type '"+file.getContentType()+"'");
      }
    } finally {
      //make sure the stream is closed!
      input.close();
    }
  }

  private void storeMarc(InputStream input, String encoding) throws
    IOException {
    //encoding defaults to MARC-8 when null
    MarcStreamReader reader = new MarcStreamReader(input, encoding);
    reader.setBadIndicators(false);
    boolean isTurboMarc = false;
    //check what MARC output we want
    MimeTypeCharSet mimeType = 
      new MimeTypeCharSet(getResource().getOutputSchema());
    if (mimeType.isMimeType("application/tmarc")) {
      isTurboMarc = true;
      logger.debug("MARC XML output type is TurboMarc");
    } else {
      logger.debug("MARC XML output type is MarcXML");
    }
    long index = 0;
    MarcWriter writer;
    RecordStorage storage = job.getStorage();
    while (reader.hasNext()) {
      try {
        org.marc4j.marc.Record record = reader.next();
        DOMResult result = new DOMResult();
        if (isTurboMarc) {
          writer = new TurboMarcXmlWriter(result);
        } else {
          writer = new MarcXmlWriter(result);
        }
        writer.write(record);
        writer.close();
        if (record.getLeader().getTypeOfRecord() == 'd') {
          storage.delete(record.getControlNumber());
        } else {
          storage.add(new RecordDOMImpl(record.getControlNumber(), null, result.
            getNode()));
        }
        if (job.isKillSent()) {
          // Close to end the pipe 
          writer.close();
          throw new IOException("Download interruted with a kill signal.");
        }
      } catch (MarcException e) {
        logger.error("Got MarcException: " + e.getClass().getCanonicalName()
          + " " + e.getMessage(), e);
        if (e.getCause() != null) {
          logger.error("Cause: " + e.getCause().getMessage(), e.getCause());
        }
        if (e.getCause() instanceof EOFException) {
          logger.warn("Received EOF when reading record # " + index);
        }
        break;
      }
      if ((++index) % 1000 == 0) {
        logger.info("MARC records read: " + index);
      }
    }
    logger.info("MARC records read: " + index);
  }

  private void storeXml(InputStream is) throws IOException {
    RecordStorage storage = job.getStorage();
    SplitContentHandler handler = new SplitContentHandler(
      new RecordStorageConsumer(storage, job.getLogger()),
      getJob().getNumber(getResource().getSplitAt(), splitAt));
    XmlSplitter xmlSplitter = new XmlSplitter(storage, logger, 
      handler, resource.isLaxParsing());
    try {
      xmlSplitter.processDataFromInputStream(is);
    } catch (SAXException se) {
      throw new IOException(se);
    }
  }

  public String getErrors() {
    return errors;
  }

  public void setErrors(String errors) {
    this.errors = errors;
  }
}
