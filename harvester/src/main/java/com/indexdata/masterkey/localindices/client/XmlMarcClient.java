package com.indexdata.masterkey.localindices.client;

import com.indexdata.masterkey.localindices.csv.CSVConverter;
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
import static com.indexdata.utils.TextUtils.joinPath;
import com.indexdata.xml.filter.MessageConsumer;
import com.indexdata.xml.filter.SplitContentHandler;
import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;
import javax.xml.transform.dom.DOMResult;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.marc4j.MarcException;
import org.marc4j.MarcStreamReader;
import org.marc4j.MarcWriter;
import org.marc4j.MarcXmlWriter;
import org.marc4j.TurboMarcXmlWriter;
import org.xml.sax.SAXException;

public class XmlMarcClient extends AbstractHarvestClient {
  private String errorText = "Failed to download/parse/store : ";
  private String errors = errorText;
  private final Date lastFrom;
  private final static int defaultSplitAt = 0;

  public XmlMarcClient(XmlBulkResource resource, BulkRecordHarvestJob job,
    Proxy proxy, StorageJobLogger logger, DiskCache dc, Date lastRequested) {
    super(resource, job, proxy, logger, dc);
    lastFrom = lastRequested;
  }

  @Override
  public BulkRecordHarvestJob getJob() {
    return (BulkRecordHarvestJob) job;
  }

  @Override
  public XmlBulkResource getResource() {
    return (XmlBulkResource) resource;
  }

  public int download(RemoteFile file) throws IOException {
    int count = 0;
    if (file.isDirectory()) {
      RemoteFileIterator iterator = file.getIterator();
      while (iterator.hasNext()) {
        count += download(iterator.getNext());
      }
    } else {
      logger.info("Found harvestable file: "+file.getName());
      storeAny(file, proposeCachePath());
      count++;
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
      clientTransport.setFromDate(lastFrom);
      clientTransport.connect(url);
      try {
        RemoteFileIterator iterator = clientTransport.get(url);
        if (iterator.hasNext()) {
          while (iterator.hasNext()) {
            RemoteFile rf = iterator.getNext();
            try {
              if (rf.isDirectory()) {
                if (getResource().getRecurse()) {
                  logger.info("Found subfolder '"+rf.getName()+"' and recursion is on.");
                  download(rf);
                } else {
                  logger.info("Found subfolder '"+rf.getName()+"' but recursion is off, ignoring.");
                }
              } else {
                  download(rf);
              }
            } catch (FTPConnectionClosedException fcce) {
              if (getResource().getAllowErrors() && !job.isKillSent()) {
                retryFtpDownload(clientTransport, rf, fcce);
              } else {
                throw fcce;
              }
            } catch (SocketException se) {
              if (getResource().getAllowErrors() && !job.isKillSent() && clientTransport instanceof FtpClientTransport) {
                retryFtpDownload(clientTransport, rf, se);
              } else {
                throw se;
              } 
            } catch (Exception e) {
              if (job.isKillSent()) throw e;
              logger.info("Problem occured during download/store: " + e.getMessage());
              logger.info("Cause: " + e.getCause());
              if (getResource().getAllowErrors()) {
                logger.warn(errorText + rf.getAbsoluteName() + ". Error: " + e.getMessage());
                logger.debug("Cause", e);
                setErrors(getErrors() + (rf.getAbsoluteName() + " "));
              } else {
                throw e;
              }
            }

          }
        } else {
          getJob().setStatus(HarvestStatus.OK, "Found no files at "+url+ (getResource().getAllowCondReq() ? ", possibly due to filtering. " : ""));
        }
      } catch (ClientTransportError cte) {
        if (getResource().getAllowErrors()) {
          setErrors("ClientTransportError, " + getErrors() + (url.toString() + " "));
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
        logger.debug("Cause", ex);
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
    EOFException, FTPConnectionClosedException, IOException  {
    //buffer reads
    InputStream input = shouldBuffer 
      ? new BufferedInputStream(file.getInputStream())
      : file.getInputStream();
    MimeTypeCharSet mimeType = deduceMimeType(input, file.getName(), file.getContentType());
    //TODO RemoteFile abstraction is not good enough to make this clean
    //some transports may deal with compressed files (e.g http) others may not
    //if we end up with a compressed mimetype we need to decompress
    if (mimeType.isZip()) {
      logger.debug("Transport returned ZIP compressed file, expanding..");
      ZipInputStream zis = new ZipInputStream(input);
      try {
        RemoteFileIterator it = new ZipRemoteFileIterator(file.getURL(),zis, null, logger);
        int count = 0;
        while (it.hasNext()) {
          RemoteFile rf = it.getNext();
          logger.info("Found harvestable file: "+rf.getName());
          storeAny(rf, proposeCachePath());
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
    } else if (mimeType.isTar()) {
      logger.debug("Transport returned TAR archive file, expanding..");
      if (mimeType.isTarGz()) {
        logger.debug("TAR archive is GZIP compressed, decompressing..");
        input = new GZIPInputStream(input);
      }
      TarArchiveInputStream tis = new TarArchiveInputStream(input);
      try {
        RemoteFileIterator it = new TarRemoteFileIterator(file.getURL(),
          tis, null, logger);
        int count = 0;
        while (it.hasNext()) {
          RemoteFile rf = it.getNext();
          logger.info("Found harvestable file: "+rf.getName());
          storeAny(rf, proposeCachePath());
          count++;
        }
        if (count == 0) {
          logger.debug("Found no files in the archive.");
        }
        return;
      } finally {
        //we need to close in case the iteration did not exhause all entries
        tis.close();
      }
    } else if (mimeType.isGzip()) {
        input = new GZIPInputStream(input);
        file.setLength(-1);
        String trimmedName = null;
        if (file.getName().endsWith(".gz")) {
          trimmedName = file.getName().substring(0, file.getName().length()-3);
        }
        //we need to bugger again to detect file type
        input = new BufferedInputStream(input);
        mimeType = deduceMimeType(input, trimmedName, null);
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
      } else if (mimeType.isCSV() || mimeType.isTSV()) {
        logger.debug("Setting up CSV-to-XML converter");
        storeCSV(input, mimeType);
      } else {
        logger.info("Ignoring file '"+file.getName()+"' because of unsupported content-type '"+mimeType+"'");
      }
    } finally {
        // NOTE: If this was a FTP download and the FTP connection was lost, a
        //       FTPConnectionClosedException will be thrown here.
        //       This exception is used when deciding whether to attempt a reconnect.
        //       See download(URL url)
        input.close();
        logger.debug("StoreAny: Input stream closed.");
    }
  }
  
  private boolean isMarc(InputStream is) throws IOException {
    // If we can't read ahead safely, just give up on guessing
    if (!is.markSupported())
        return false;
    
    is.mark(22);
    int pos = 0;
    //0-4
    while (pos <= 4) {
      int c = is.read();
      if (c < '0' || c > '9') { //not digit
        is.reset();
        return false;
      }
      ++pos;
    }
    //5-11
    while (pos < 12) {
      is.read();
      ++pos;
    }
    //12-16
    while (pos <= 16) {
      int c = is.read();
      if (c < '0' || c > '9') { //not digit
        is.reset();
        return false;
      }
      ++pos;
    }
    //17-19
    while (pos < 20) {
      is.read();
      ++pos;
    }
    //20
    if (is.read() != '4') {
      is.reset();
      return false;
    }
    //21
    if (is.read() != '5') {
      is.reset();
      return false;
    }
    is.reset();
    return true;
  }
  
  private boolean isMarkup(InputStream is) throws IOException {
    if (!is.markSupported())
      return false;
    is.mark(2);
    if (is.read() != '<') {
      is.reset();
      return false;
    }
    int startChar = is.read();
    if (startChar != '?' && startChar != '!' && 
      !Character.isLetter(startChar)) {
      is.reset();
      return false;
    }
    is.reset();
    return true;
  }

  private MimeTypeCharSet deduceMimeType(InputStream input, String fileName, String contentTypeHint)
    throws IOException { 
    //if transport does not provide content type
    //we attempt to deduce it
    String guess = null;
    MimeTypeCharSet mimeType = new MimeTypeCharSet(contentTypeHint);
    if (contentTypeHint != null) {
      logger.debug("Content type provided by transport: "+contentTypeHint);
    }
    //first try the limited Java built-in content type detection
    if (mimeType.isUndefined() || mimeType.isPlainText() || mimeType.isBinary()) {
      if (input != null) {
        guess = URLConnection.guessContentTypeFromStream(input);
        if (guess == null) {
          guess = isMarc(input)
            ? "application/marc" 
            : isMarkup(input)
              ? "application/xml" //lucky assumption
              : null;
        }
      }
      if (guess == null) {
        guess = fileName != null
          ? URLConnection.guessContentTypeFromName(fileName)
          : null;
        if (guess != null) {
          logger.debug("Guessed content type from filename: "+guess);
        }
      } else {
        logger.debug("Guessed content type from stream: "+guess);
      }
    }
    //reset mime-type to guess
    if (guess != null) {
      mimeType = new MimeTypeCharSet(guess);
    }
    //sgml/html docs can be treated with the XML-lax parser
    if (mimeType.isHTML() || mimeType.isSGML()) {
      if (resource.isLaxParsing()) {
        logger.debug("Overriding HTML/SGML with XML content type because lax parsing is on.");
        mimeType = new MimeTypeCharSet("application/xml");
      } else {
        logger.debug("HTML/SGML content type will not be harvested unless lax parsing is enabled");
      }
    }
    //missing and some deduced or provided content type may not be enough
    //we check the extension in this case anyway
    if (mimeType.isUndefined() 
      || mimeType.isBinary() 
      || mimeType.isPlainText()
      || mimeType.isGzip() /* tar or plain gzip */) {
      if (fileName != null) {
        if (fileName.endsWith(".zip")) {
          guess = "application/zip";
        } else if (fileName.endsWith(".tar")) {
          guess = "application/x-tar";
        } else if (fileName.endsWith(".tar.gz")
          || fileName.endsWith(".tgz")) {
          guess = "application/x-gtar";
        } else if (fileName.endsWith(".gz")) {
          guess = "application/gzip";
        } else if (fileName.endsWith(".mrc") || fileName.endsWith(".marc")) {
          guess = "application/marc";
        } else if (fileName.endsWith(".csv")) {
          guess = "text/csv";
        } else if (fileName.endsWith(".tsv") || fileName.endsWith(".tab")) {
          guess = "text/tab-separated-values";
        }
        if (guess != null) {
          mimeType = new MimeTypeCharSet(guess);
          logger.debug("Guessed content type from filename: "+guess);
        }
      }
    }
    return mimeType;
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
          throw e;
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
    int splitAt = getJob().getNumber(getResource().getSplitAt(), defaultSplitAt);
    logger.debug("XML splitting depth: "+splitAt);
    SplitContentHandler handler = new SplitContentHandler(
      new RecordStorageConsumer(storage, job.getLogger()), splitAt);
    XmlSplitter xmlSplitter = new XmlSplitter(storage, logger, 
      handler, resource.isLaxParsing());
    try {
      xmlSplitter.processDataFromInputStream(is);
    } catch (SAXException se) {
      throw new IOException(se);
    }
  }
  
  /**
   * Reconnects to a FTP server and retries download of a file.
   *  
   * To be invoked in case a FTP connection dropped while downloading the file.
   * 
   * If reconnecting fails or another IO error occurs during this download attempt, the 
   * file is skipped (if allow-errors is on) or a ClientTransporError is thrown.
   * 
   * @param clientTransport
   * @param rf
   * @param ex
   * @throws ClientTransportError
   */
  private void retryFtpDownload(ClientTransport clientTransport, RemoteFile rf, IOException ex) throws ClientTransportError {
    logger.warn(errorText + rf.getAbsoluteName() + ". Error: " + ex.getMessage());
    logger.debug("Cause", ex);
    setErrors(getErrors() + (rf.getAbsoluteName() + " "));
    logger.info("Connection lost. Attempting reconnect in 10 seconds.");
    try {
      ((FtpClientTransport)clientTransport).reconnect(10000);
      download(rf);
    } catch (IOException ioe) {
      if (!job.isKillSent()) {
        logger.error("Second download attempt failed: " + ioe.getMessage());
        if (getResource().getAllowErrors()) {
          logger.error(getErrors() + ". Failed to download in two attempts");
          setErrors(getErrors() + ". Failed to download in two attempts");
        } else {
          throw new ClientTransportError("Attempt to reconnect and download failed: " + ioe.getMessage());
        }
      }
    }
  }


  private void storeCSV(InputStream input, MimeTypeCharSet mt) throws IOException {
    MessageConsumer mc = new RecordStorageConsumer(job.getStorage(), job.getLogger());
    try {
      char defaultDelim = mt.isTSV() ? '\t' : ',';
      String defaultCharset = mt.getCharset() != null ? mt.getCharset() : "iso-8859-1";
      CSVConverter converter = new CSVConverter(defaultCharset, defaultDelim,
        getResource().getCsvConfiguration() != null ? getResource().getCsvConfiguration() : "");
      int splitAt = getJob().getNumber(getResource().getSplitAt(), defaultSplitAt);
      boolean split = splitAt > 0;
      logger.debug("Converting CSV-to-XML using: '"
        +converter.getFormatString()
        +(split ? "' and splitting rows" : "' and not splitting rows"));
      converter.processViaDOM(input, mc, split);
    } catch (ParseException ex) {
      throw new IOException(ex);
    }
  }
  public String getErrors() {
    return errors;
  }

  public void setErrors(String errors) {
    this.errors = errors;
  }

}
