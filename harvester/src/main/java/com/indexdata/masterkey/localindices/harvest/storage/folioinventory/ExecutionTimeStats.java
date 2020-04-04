/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.indexdata.masterkey.localindices.harvest.storage.folioinventory;

import static java.util.Comparator.comparing;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;

/**
 *
 * @author ne
 */
public class ExecutionTimeStats {
  private final SimpleDateFormat HOUR = new SimpleDateFormat("MM-dd HH");
  private final Map<String, HourStats> execTimes = new HashMap<>();

  HourStats hourstats = null;
  StorageJobLogger logger;

  public ExecutionTimeStats(StorageJobLogger logger) {
    this.logger = logger;
  }

  public void time(long wasStartedAt) {
    long endedAt = System.currentTimeMillis();
    String hour = HOUR.format(new Date());
    if (execTimes.containsKey(hour)) {
      execTimes.get(hour).log(wasStartedAt, endedAt);
    } else {
      writeLog(); // at top of the hour, write logs up until previous hour
      hourstats = new HourStats();
      hourstats.log(wasStartedAt, endedAt);
      execTimes.put(hour,hourstats);
    }
  }

  public void writeLog() {
    execTimes.entrySet().stream()
    .sorted(comparing(Map.Entry::getKey))
    .forEach(e -> { // for each hour
      HourStats hr = e.getValue();
      StringBuilder totals1 = new StringBuilder();
      totals1.append(e.getKey()).append(":00: ")
             .append(hr.execCount).append(" records processed in ").append(hr.totalExecTime/1000).append(" secs.")
             .append("~").append(hr.totalExecTime/60000).append(" mins. of execution time");
      logger.info(totals1.toString());

      StringBuilder totals2 = new StringBuilder();
      totals2.append("Average: ").append(hr.totalExecTime/hr.execCount).append(" ms. ")
             .append("Fastest: ").append(hr.minExecTime).append(" ms. ")
             .append("Slowest: ").append(hr.maxExecTime).append(" ms. ");
      logger.info(totals2.toString());

      e.getValue().execTimeIntervals.entrySet().stream()
              .sorted(comparing(Map.Entry::getKey))
              .forEach(f -> { // for each response time interval
                StringBuilder intv = new StringBuilder();
                intv.append("Up to ").append(f.getKey()).append(" ms for ").append(f.getValue()).append(" records. ");
                if (f.getValue()*100/hr.execCount>0)
                  intv.append("(").append(f.getValue()*100/hr.execCount).append("%)");
                logger.info(intv.toString());
              });}
    );
  }
}
