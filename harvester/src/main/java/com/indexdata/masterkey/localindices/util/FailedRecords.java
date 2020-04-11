package com.indexdata.masterkey.localindices.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Stream;

public class FailedRecords {
    final String logDir;
    final long jobId;
    final String directory;

    public FailedRecords (String logDir, long jobId) {
        this.logDir = logDir;
        this.jobId = jobId;
        this.directory = new StringBuilder()
                .append(logDir).append("/failed-records/").append(jobId).append("/")
                .toString();
    }

    public File[] getRecords() throws FileNotFoundException {
        File dir = new File(directory);
        if (!dir.isDirectory()) {
          throw new FileNotFoundException("Error file directory not found: "+directory);
        }
        File[] failedRecords = dir.listFiles();
        return failedRecords;
    }

    public Iterator<File> getFilesIterator() throws FileNotFoundException {
        File[] files = getRecords();
        return Arrays.asList(files).iterator();
    }

    public File getRecord (String name) throws FileNotFoundException {
        File file = new File(directory + name);
        if (!file.exists()) {
            throw new FileNotFoundException("Error file not found: " + name);
        }
        return file;
    }

    public String getListOfFailedRecordsAsXml() throws FileNotFoundException {
      Iterator<File> files = getFilesIterator();
      StringBuilder response = new StringBuilder();
      response.append("<failed-records>");
      while (files.hasNext()) {
        File next = files.next();
        response.append("<record>")
        .append(next.getName())
        .append("</record>");
      }
      response.append("</failed-records>");
      return response.toString();
    }

    public String getFailedRecordAsString (String name) throws IOException {
      StringBuilder contentBuilder = new StringBuilder();
      try (Stream<String> stream = Files.lines( Paths.get(directory+name), StandardCharsets.UTF_8)) {
        stream.forEach(s -> contentBuilder.append(s).append(System.lineSeparator()));
      }
        return contentBuilder.toString();
    }

}