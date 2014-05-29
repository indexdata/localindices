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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.transform.dom.DOMResult;

import org.marc4j.MarcException;
import org.marc4j.MarcStreamReader;
import org.marc4j.MarcWriter;
import org.marc4j.MarcXmlWriter;
import org.marc4j.TurboMarcXmlWriter;

import com.indexdata.masterkey.localindices.entity.XmlBulkResource;
import com.indexdata.masterkey.localindices.harvest.cache.CachingInputStream;
import com.indexdata.masterkey.localindices.harvest.cache.DiskCache;
import com.indexdata.masterkey.localindices.harvest.cache.NonClosableInputStream;
import com.indexdata.masterkey.localindices.harvest.job.BulkRecordHarvestJob;
import com.indexdata.masterkey.localindices.harvest.job.HarvestStatus;
import com.indexdata.masterkey.localindices.harvest.job.MimeTypeCharSet;
import com.indexdata.masterkey.localindices.harvest.job.RecordStorageConsumer;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import com.indexdata.masterkey.localindices.harvest.storage.RecordDOMImpl;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;
import com.indexdata.masterkey.localindices.harvest.storage.XmlSplitter;
import com.indexdata.xml.filter.SplitContentHandler;

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
        count += download(iterator.get());
      }
    } else {
      ReadStore readStore = prepareReadStore(file, resource.isCacheEnabled()
        ? joinPath(diskCache.getJobPath(), diskCache.proposeName())
        : null);
      readStore.readAndStore();
      count++;
    }
    return count;
  }

  @Override
  public int download(URL url) throws Exception {
    ClientTransportFactory factory = new ResourceClientTransportFactory(
      (XmlBulkResource) resource, logger);
    logger.info("Preparing retrieval of " + url.toString());
    try {
      ClientTransport clientTransport = factory.lookup(url);
      clientTransport.connect(url);
      try {
        RemoteFileIterator iterator = clientTransport.get(url);
        if (iterator.hasNext()) {
          while (iterator.hasNext()) {
            RemoteFile rf = iterator.get();
            logger.info("Found file or link at "+rf.getName());
            download(rf);
          }
        } else {
          getJob().setStatus(HarvestStatus.WARN, "Found no files at " + url.
            toString());
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
      ReadStore readStore = prepareReadStore(new LocalRemoteFile(file), null);
      readStore.readAndStore();
    } catch (StopException ex) {
      logger.info("Stop requested. Reason: " + ex.getMessage());
    }
    return 0;
  }

  interface ReadStore {
    void readAndStore() throws Exception;
  }

  class MarcReadStore implements ReadStore {
    InputStream input;
    boolean useTurboMarc = false;
    // Default MARC-8 encoding, use setEncoding to override
    String encoding = null;
    StreamIterator iterator;

    public MarcReadStore(InputStream input, StreamIterator iterator) {
      this.input = input;
      this.iterator = iterator;
    }

    public MarcReadStore(InputStream input, StreamIterator iterator,
      boolean useTurboMarc) {
      this.input = input;
      this.useTurboMarc = useTurboMarc;
    }

    void setEncoding(String encoding) {
      this.encoding = encoding;
    }

    @Override
    public void readAndStore() throws Exception {
      try {
        MarcStreamReader reader = new MarcStreamReader(input, encoding);
        reader.setBadIndicators(false);
        while (iterator.hasNext()) {
          store(reader, -1);
        }
      } finally {
        //failure to close the input stream will result in malformed cached data
        //and leaking fds
        input.close();
      }
    }
  }

  class InputStreamReadStore implements ReadStore {
    InputStream input;
    long contentLength;
    StreamIterator iterator;

    public InputStreamReadStore(InputStream input, long contentLength,
      StreamIterator iterator) {
      this.input = input;
      this.contentLength = contentLength;
      this.iterator = iterator;
    }

    @Override
    public void readAndStore() throws Exception {
      NonClosableInputStream ncis = new NonClosableInputStream(input);
      try {
        while (iterator.hasNext()) {
          store(ncis, contentLength);
        }
      } finally {
        ncis.reallyClose();
      }
    }
  }

  class StreamIterator {
    int once = 1;

    public boolean hasNext() throws IOException {
      return once-- > 0;
    }
  }

  class ZipStreamIterator extends StreamIterator {
    ZipInputStream zip;

    public ZipStreamIterator(ZipInputStream input) {
      zip = input;
    }

    public boolean hasNext() throws IOException {
      ZipEntry zipEntry = zip.getNextEntry();
      if (zipEntry != null) {
        @SuppressWarnings("unused")
        int method = zipEntry.getMethod();
      }
      return zipEntry != null;
    }
  }

  private ReadStore prepareReadStore(RemoteFile file, String cacheFile) throws
    IOException {
    // InputStream after possible Content-Encoding decoded.
    long contentLength = file.length();
    InputStream isDec = file.getInputStream();
    StreamIterator streamIterator = new StreamIterator();
    if (file.isCompressed()) {
      contentLength = -1;
    }
    //buffer reads
    isDec = new BufferedInputStream(isDec);
    //cache responses to filesystem
    if (cacheFile != null) {
      isDec = new CachingInputStream(isDec, cacheFile);
    }
    String contentType = file.getContentType();
    MimeTypeCharSet mimeCharset = new MimeTypeCharSet(contentType);
    // Expected type overrides content type
    if (getResource().getExpectedSchema() != null) {
      mimeCharset = new MimeTypeCharSet(getResource().getExpectedSchema());
    }

    if (mimeCharset.isMimeType("application/marc") || mimeCharset.isMimeType(
      "application/tmarc")) {
      logger.info("Setting up Binary MARC reader ("
        + (mimeCharset.getCharset() != null ? mimeCharset.getCharset()
        : "default") + ")"
        + (getResource().getExpectedSchema() != null
        ? " Override by resource mime-type: " + getResource().
        getExpectedSchema()
        : "Content-type: " + contentType));

      MarcReadStore readStore = new MarcReadStore(isDec, streamIterator);
      String encoding = mimeCharset.getCharset();
      if (encoding != null) {
        readStore.setEncoding(encoding);
      }
      return readStore;
    }

    logger.info("Setting up InputStream reader. "
      + (contentType != null ? "Content-Type:" + contentType : ""));
    return new InputStreamReadStore(isDec, contentLength, streamIterator);
  }

  private void store(MarcStreamReader reader, long contentLength) throws
    IOException {
    long index = 0;
    MarcWriter writer;
    MimeTypeCharSet mimetypeCharset = new MimeTypeCharSet(getResource().
      getOutputSchema());
    boolean isTurboMarc = false;
    if (mimetypeCharset.isMimeType("application/tmarc")) {
      isTurboMarc = true;
      logger.info("Setting up Binary MARC to TurboMarc converter");
    } else {
      logger.info("Setting up Binary MARC to MarcXml converter");
      //writer = new MarcXmlWriter(output, true);
    }
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
        logger.info("Marc record read: " + index);
      }
    }
    //writer.close();

    logger.info("Marc record read total: " + index);
  }

  private void store(InputStream is, long contentLength) throws Exception {
    RecordStorage storage = job.getStorage();
    SplitContentHandler handler = new SplitContentHandler(
      new RecordStorageConsumer(storage, job.getLogger()),
      getJob().getNumber(getResource().getSplitAt(), splitAt));
    XmlSplitter xmlSplitter = new XmlSplitter(storage, logger, handler);
    xmlSplitter.processDataFromInputStream(is);
  }

  public String getErrors() {
    return errors;
  }

  public void setErrors(String errors) {
    this.errors = errors;
  }
}
