/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.indexdata.masterkey.localindices.harvest.storage.folio;

import static java.util.Comparator.comparing;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;

import org.apache.log4j.Level;

/**
 * Logs execution times for record updates into buckets of one hour, using the
 * {@link SummarizedProcessingTimes} structure. Writes the stats out
 * to the job log at each change of the hour.
 * @author ne
 */
public class HourlyPerformanceStats {
  private final SimpleDateFormat HOUR = new SimpleDateFormat("MM-dd HH");
  private final Map<String, SummarizedProcessingTimes> execTimes = new HashMap<>();

  SummarizedProcessingTimes hourstats = null;
  StorageJobLogger logger;
  String label;
  Level logLevelSummaries = Level.INFO;
  Level logLevelIntervals = Level.INFO;

  public HourlyPerformanceStats(String label, StorageJobLogger logger) {
    this.logger = logger;
    this.label = label;
  }

  public void time(long wasStartedAt) {
    setTiming(System.currentTimeMillis()-wasStartedAt);
  }

  public void time(long wasStartedAt, int records) {
    if (records>0) setTiming(System.currentTimeMillis()-wasStartedAt, records);
  }

  public void setLogLevelForSummaries(Level level) {
    logLevelSummaries = level;
  }

  public void setLogLevelForIntervals(Level level) {
    logLevelIntervals = level;
  }

  public void setTiming(long timing) {
    String hour = HOUR.format(new Date());
    if (execTimes.containsKey(hour)) {
      execTimes.get(hour).log(timing);
    } else {
      writeLog(); // at top of the hour, write out logs up until the previous hour
      // then create a new bucket for timing stats
      hourstats = new SummarizedProcessingTimes();
      hourstats.log(timing);
      execTimes.put(hour,hourstats);
    }
  }

  public void setTiming(long timing, int records) {
    String hour = HOUR.format(new Date());
    if (execTimes.containsKey(hour)) {
      execTimes.get(hour).log(timing, records);
    } else {
      writeLog(); // at top of the hour, write out logs up until the previous hour
      // then create a new bucket for timing stats
      hourstats = new SummarizedProcessingTimes();
      hourstats.log(timing, records);
      execTimes.put(hour,hourstats);
    }
  }

  /**
   * Prints the performance stats, hour by hour, to the job log.
   */
  public void writeLog() {
    if (execTimes.entrySet().size()>0) {
      logger.log(logLevelSummaries,"Metrics: " + label);
    }
    execTimes.entrySet().stream()
    .sorted(comparing(Map.Entry::getKey))
    .forEach(e -> { // for each hour
      SummarizedProcessingTimes hr = e.getValue();
      StringBuilder summary = new StringBuilder();
      summary.append("- ").append(e.getKey()).append(":00: ")
             .append(hr.execCount).append(" records processed in ").append(hr.totalExecTime/1000).append(" secs.")
             .append("~").append(hr.totalExecTime/60000).append(" mins. ");

      summary.append("Avg: ").append(hr.totalExecTime/hr.execCount).append(" ms. ")
             .append("Min: ").append(hr.minExecTime).append(" ms. ")
             .append("Max: ").append(hr.maxExecTime).append(" ms. ");
      logger.log(logLevelSummaries, summary.toString());

      e.getValue().execTimeIntervals.entrySet().stream()
              .sorted(comparing(Map.Entry::getKey))
              .forEach(f -> { // for each response time interval
                StringBuilder intv = new StringBuilder();
                intv.append("-- Up to ").append(f.getKey()).append(" ms for ").append(f.getValue()).append(" records. ");
                if (f.getValue()*100/hr.execCount>0)
                  intv.append("(").append(f.getValue()*100/hr.execCount).append("%)");
                logger.log(logLevelIntervals, intv.toString());
              });}
    );
  }
}
