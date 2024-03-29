package com.indexdata.masterkey.localindices.client;

import static com.indexdata.utils.TextUtils.joinPath;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

import javax.xml.transform.dom.DOMResult;

import com.indexdata.masterkey.localindices.harvest.cache.NonClosableInputStream;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.apache.commons.io.IOUtils;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.log4j.Level;
import org.marc4j.*;
import org.xml.sax.SAXException;

import com.indexdata.masterkey.localindices.client.filefilters.CompositeEntryFilter;
import com.indexdata.masterkey.localindices.client.filefilters.EntryFilter;
import com.indexdata.masterkey.localindices.client.filefilters.EntryFilterExcludePattern;
import com.indexdata.masterkey.localindices.client.filefilters.EntryFilterIncludePattern;
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
import com.indexdata.masterkey.localindices.util.TextUtils;
import com.indexdata.xml.filter.MessageConsumer;
import com.indexdata.xml.filter.SplitContentHandler;
import org.w3c.dom.Node;

public class XmlMarcClient extends AbstractHarvestClient {
  private String errorText = "Failed to download/parse/store : ";
  private String errors = errorText;
  private final Date lastFrom;
  private final static int defaultSplitAt = 0;
  private ClientTransport clientTransport;

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
      RemoteFileIterator iterator;
      try {
        iterator = file.getIterator();
      } catch (IOException ioe) {
        if (ioe.getMessage().equals("Connection is not open")) {
          logger.error("Connection is not open, may have timed out during download of previous file; reconnecting to recurse into " + file.getName());
          ((FtpClientTransport)clientTransport).reconnect(10000);
          iterator = file.getIterator();
        } else {
          logger.error("Failed to recurse into  " + file.getName() +": " + ioe.getMessage());
          throw ioe;
        }
      }
      while (iterator.hasNext()) {
        logger.debug("Getting next file or directory from " + file.getName());
        count += download(iterator.getNext());
        logger.debug("Got " + count + " files from directory structure");
      }
    } else {
      logger.info("Begin processing of "+file.getName());

      try {
        storeAny(file, proposeCachePath());
        logger.debug("Done storing " + file.getName());
      } catch (IOException ioe) {
        if (ioe.getMessage() != null && ioe.getMessage().toLowerCase( Locale.ROOT ).contains("completependingcommand")) {
          logger.warn("Problem with CompletingPendingCommand after download of " + file.getName() + ": " + ioe.getMessage());
          logger.debug("Done storing " + file.getName());
        } else {
          logger.error(
                  "Failure storing " + file.getName() + ": " + ioe.getMessage() + ( getResource().getAllowErrors() ? " Continuing since job is set to proceed on errors." : "" ) );
          if ( !getResource().getAllowErrors() )
          {
            throw ioe;
          }
        }
      }
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
    logger.info("Preparing retrieval from " + url.getHost());
    try {
      clientTransport = factory.lookup(url);
      clientTransport.setFromDate(lastFrom);
      clientTransport.setIncludeFilePattern(getResource().getIncludeFilePattern());
      clientTransport.setExcludeFilePattern(getResource().getExcludeFilePattern());
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
                logger.info("XmlMarcClient downloading " + rf.getAbsoluteName());
                download(rf);
                logger.debug("XmlMarcClient done downloading " + rf.getAbsoluteName());
              }
            } catch (FTPConnectionClosedException fcce) {
              logger.error("XmlMarcClient caught FTPConnectionClosedException");
              if (getResource().getAllowErrors() && !job.isKillSent()) {
                retryFtpDownload(clientTransport, rf, fcce);
              } else {
                throw fcce;
              }
            } catch (SocketException se) {
              logger.error("XmlMarcClient caught SocketException");
              if (getResource().getAllowErrors() && !job.isKillSent() && clientTransport instanceof FtpClientTransport) {
                retryFtpDownload(clientTransport, rf, se);
              } else {
                throw se;
              }
            } catch (StopException stoppedAtLimit) {
              logger.info("Stop Exception: " + stoppedAtLimit.getMessage());
              throw stoppedAtLimit;
            } catch (IOException ioe) {
              if (job.isKillSent()) {
                logger.info("Detected kill sent during download exception handling. Exception was " + ioe.getMessage());
                throw ioe;
              } else if (getResource().getAllowErrors()) {
                logger.error("Problem occurred during download/store [" + ioe.getMessage() + ioe.getCause() + "] but job set to continue on errors");
                logger.warn(errorText + rf.getAbsoluteName() + ". Error: " + ioe.getMessage());
                setErrors(getErrors() + (rf.getAbsoluteName() + " "));
              } else {
                logger.error("Problem occurred during download/store of " + rf.getName() + " [" + ioe.getMessage() + ioe.getCause() + "] and job NOT set to continue on errors");
                throw ioe;
              }
            } catch (Exception e) {
              logger.debug("XmlMarcClient caught Exception processing " + rf.getName() +  (job.isKillSent()? ": job killed(1)." : ""));
              if (job.isKillSent()) {
                throw e;
              }
              logger.info("Problem occurred during download/store of " + rf.getName() + ": "  + e.getMessage() + e);
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
          logger.debug("XmlMarcClient done iterating file list at " + url.getHost());
        } else {
          getJob().setStatus(HarvestStatus.OK, "Found no files at "+url+ (getResource().getAllowCondReq() ? ", possibly due to filtering. " : ""));
        }
      } catch (ClientTransportError cte) {
        if (getResource().getAllowErrors()) {
          setErrors("ClientTransportError [" + getErrors() + "] retrieving from " + url.getHost() + " but job set to continue on errors");
          return 1;
        } else {
          logger.debug("XmlMarcClient caught ClientTransportError while retrieving from  " + url.getHost() + " and job NOT set to continue on errors");
          throw cte;
        }
      }
      logger.debug("XmlMarcClient sleeping for 2 seconds");
      // TODO HACK HACK HACK
      Thread.sleep(2000);
      logger.info("Finished retrieval from " + url.getHost());
    } catch (StopException ex) {
      logger.info("Stop requested. Reason: " + ex.getMessage());
      return 0;
    } catch (Exception ex) {
      logger.debug("XmlMarcClient caught exception" + (job.isKillSent() ? ": job killed(2)" : "") + " when retrieving from " + url.getHost());
      if (job.isKillSent()) {
        throw ex;
      }
      if (getResource().getAllowErrors()) {
        logger.warn(errorText + url.getHost() + ". Error: " + ex.getMessage() + " " + ex.getCause().toString());
        setErrors(getErrors() + (url.getHost() + " "));
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

  private void storeAny(RemoteFile file, String cacheFile) throws IOException  {
    InputStream input;
    try {
      logger.info("File: " + file.getAbsoluteName() + ". Length: " + file.getLength());
      input = new BufferedInputStream(file.getInputStream());
    } catch (IOException ioe) {
      if (ioe.getMessage().equals("Connection is not open")) {
        logger.error("Connection is not open, may have timed out during download of previous file, attempting to reconnect to download " + file.getName());
        ((FtpClientTransport)clientTransport).reconnect(10000);
        logger.info("Continue processing of " + file.getName());
        input = new BufferedInputStream(file.getInputStream());
      } else {
        throw ioe;
      }
    }
    MimeTypeCharSet mimeType = deduceMimeType(input, file.getName(), file.getContentType());
    EntryFilter excludefilter = new EntryFilterExcludePattern(((XmlBulkResource) resource).getExcludeFilePattern(),logger);
    EntryFilter includefilter = new EntryFilterIncludePattern(((XmlBulkResource) resource).getIncludeFilePattern(),logger);
    EntryFilter entryFilter = new CompositeEntryFilter(excludefilter,includefilter);
    //TODO RemoteFile abstraction is not good enough to make this clean
    //some transports may deal with compressed files (e.g http) others may not
    //if we end up with a compressed mimetype we need to decompress
    logger.debug("mimeType is " + mimeType.getMimeType());
    if (mimeType.isZip()) {
      logger.debug("Transport returned ZIP compressed file, expanding..");
      ZipInputStream zis = new ZipInputStream(input);
      try {
        RemoteFileIterator it = new ZipRemoteFileIterator(file.getURL(),zis, null, logger, entryFilter);
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
          tis, null, logger, entryFilter);
        int count = 0;
        while (it.hasNext()) {
          RemoteFile rf = it.getNext();
          logger.info("Found harvestable file in TAR archive: "+rf.getName());
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
    } else if (mimeType.is7z()) {
      logger.debug("Transport returned 7z archive file, expanding..");
      SeekableInMemoryByteChannel inMemoryByteChannel;
      try {
        input.mark(Integer.MAX_VALUE);
        inMemoryByteChannel = new SeekableInMemoryByteChannel(IOUtils.toByteArray(input));
        SevenZFile sevenZFile = new SevenZFile(inMemoryByteChannel);
        if (sevenZFileHasTooManyEntries(sevenZFile, logger)) {
          sevenZFile.close();
          input.close();
          throw new IOException("7z file has too many entries. Maximum supported entries is 65536.");
        } else {
          logger.debug("7z file is within record count maximum.");
        }
        input.reset();
        inMemoryByteChannel = new SeekableInMemoryByteChannel(IOUtils.toByteArray(input));
        sevenZFile = new SevenZFile(inMemoryByteChannel);
        SevenZArchiveEntry entry;
        int count = 0;
        while (( entry = sevenZFile.getNextEntry() ) != null) {
          if (!entry.isDirectory()) {
            URL url = new URL(file.getURL() + "/" + entry.getName());
            RemoteFile remoteFile = new RemoteFile(url,
                    new NonClosableInputStream(sevenZFile.getInputStream(entry)),
                    logger);
            remoteFile.setLength(entry.getSize());
            storeAny(remoteFile, proposeCachePath());
            count++;
          }
        }
        if (count == 0) {
          logger.info("Found no files in the 7z archive.");
        } else {
          logger.info("Found " + count + " files in the 7z archive");
        }
        sevenZFile.close();
        input.close();
        return;
      } catch (OutOfMemoryError oome) {
        logger.error("Out of memory error when reading input stream to in-memory byte channel.");
        throw new IOException("Out of memory when expanding 7z archive.");
      }
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
        logger.debug("Setting up binary MARC reader (mime type " + mimeType + ", encoding: [" + mimeType.getCharset() +"])");
        storeMarc(input, mimeType.getCharset());
      } else if (mimeType.isXML()) {
        logger.debug("XmlMarcClient setting up XML reader (" + mimeType + ")");
        storeXml(input);
        logger.debug("XmlMarcClient finished storeXml");
      } else if (mimeType.isCSV() || mimeType.isTSV()) {
        logger.debug("Setting up CSV-to-XML converter");
        storeCSV(input, mimeType);
      } else {
        logger.info("Ignoring file '" + file.getName() + "' because of unsupported content-type '" + mimeType + "'");
      }
    } catch (IOException ioe) {
      logger.error("IO exception occurred when running store function: " + ioe.getMessage());
      throw ioe;
    } finally {
        // NOTE: If this was an FTP download and the FTP connection was lost, a
        //       FTPConnectionClosedException will be thrown here.
        //       This exception is used when deciding whether to attempt a reconnect.
        //       See download(URL url)
        logger.debug("Done reading " + file.getName() + ". Closing input.");
        input.close();
        logger.debug("StoreAny closed input stream");
    }
  }

  private static boolean sevenZFileHasTooManyEntries (SevenZFile file, StorageJobLogger logger) throws IOException{
    SevenZArchiveEntry entry;
    int entries=0;
    while ((entry = file.getNextEntry()) != null) {
      entries++;
    }
    if (entries > 65536) {
      DecimalFormat df = new DecimalFormat("###,###,###");
      logger.error("SevenZFile has too many entries. Maximum entries supported is 65,536, file has " + df.format(entries) + ".");
    }
    return entries > 65536;
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
        } else if (fileName.endsWith(".7z")) {
          guess = "application/x-7z-compressed";
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
    MarcReader reader = new MarcStreamReader( input, encoding );
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
    logger.debug("Storage is " + storage.getClass().getName());

    ByteArrayOutputStream singleRecordByteStream = null;
    MarcStreamWriter iso2709writer = null;
    singleRecordByteStream = new ByteArrayOutputStream(20000);
    iso2709writer = new MarcStreamWriter(singleRecordByteStream);
    while (reader.hasNext()) {
      try {
        org.marc4j.marc.Record record = reader.next();
        // Reset single record stream before each record is written
        singleRecordByteStream.reset();
        // Write the single record to stream.
        iso2709writer.write(record);
        // Read the single record with permissive, UTF-8 converting reader.
        byte[] singleRecord = singleRecordByteStream.toByteArray();
        ByteArrayInputStream recordAsInputStream = new ByteArrayInputStream( singleRecord );

        MarcReader convertingReader = new MarcPermissiveStreamReader( recordAsInputStream, true, true );
        // Note: Permissive Reader cannot be used for the outer iteration instead of MarcStreamReader since it will
        //       stop reading certain MARC files mid-file and without a message. This is NOT a problem when using
        //       MarcPermissiveStreamReader on a regular InputStream but it _is_ a problem with the home-grown
        //       FtpInputStream that must be used for other reasons (the process for other file types breaks without it).
        //       Thus, the two-step reading of binary MARC streams.
        //
        // Note: No other good solution has been identified for converting incoming binary MARC to UTF-8 XML with
        //       this version of Marc4J.
        //
        // Note: The nine year old version of Marc4J must be used because it has support for TurboMarc.
        org.marc4j.marc.Record convertedRecord = convertingReader.next();
        // Write to XML
        DOMResult result = new DOMResult();
        if (isTurboMarc) {
          writer = new TurboMarcXmlWriter(result);
        } else {
          writer = new MarcXmlWriter(result);
        }
        writer.write(convertedRecord);
        writer.close();
        // Store or delete
        if (record.getLeader().getRecordStatus() == 'd') {
          logger.debug("Removing record from storage");
          logger.log(Level.TRACE,"XML value of Record is " + TextUtils.nodeToXMLString(result.getNode()));
          Node node = result.getNode();
          RecordDOMImpl rdi = new RecordDOMImpl(record.getControlNumber(), null, node, singleRecord );
          storage.delete(rdi);
        } else {
          logger.log( Level.TRACE, "Adding new Record to storage");
          logger.log( Level.TRACE, "XML value of Record is " + TextUtils.nodeToXMLString(result.getNode()));
          Node node = result.getNode();       
          RecordDOMImpl rdi = new RecordDOMImpl(record.getControlNumber(), null, node, singleRecord);
          storage.add(rdi);
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
          logger.error("Received EOF when reading record # " + index);
          throw e;
        }
        if (getResource().getAllowErrors()) {
          continue;
        } else {
          break;
        }
      } catch (RuntimeException re) {
        logger.error("XmlMarcClient, storeMarc(), Runtime exception: " + re.getMessage());
        throw(re);
      }
      if ((++index) % 1000 == 0) {
        logger.info("MARC records read: " + index);
      }
    }
    if (iso2709writer != null) iso2709writer.close();
    logger.info("MARC records read: " + index);
  }

  private void storeXml(InputStream is) throws IOException {
    RecordStorage storage = job.getStorage();
    int splitAt = getJob().getNumber(getResource().getSplitAt(), defaultSplitAt);
    logger.debug("XML splitting depth: "+splitAt);
    SplitContentHandler handler = new SplitContentHandler(
      new RecordStorageConsumer(storage, job.getLogger(), resource.isStoreOriginal()), splitAt);
    XmlSplitter xmlSplitter = new XmlSplitter(logger,
      handler, resource.isLaxParsing());
    try {
      logger.debug("XmlMarcClient: XML splitter begins processing data from input stream");
      xmlSplitter.processDataFromInputStream(is);
      logger.debug("XmlMarcClient: XML splitter done processing data from input stream");
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
    logger.debug("Cause" + ex.getCause() + ex.getMessage());
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
      } else {
        logger.error("retryFtpDownload detected killSent - canceling retry");
      }
    }
  }

  private void storeCSV(InputStream input, MimeTypeCharSet mt) throws IOException {
    MessageConsumer mc = new RecordStorageConsumer(job.getStorage(), job.getLogger(), resource.isStoreOriginal());
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
