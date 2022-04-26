/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.indexdata.masterkey.localindices.harvest.storage.folio;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds record counts and summarized processing times
 * <ul>
 * <li>record counts</li>
 * <li>total execution time for all records processed</li>
 * <li>the fastest processing of a record</li>
 * <li>the slowest processing of a record</li>
 * <li>record counts distributed into 100 ms and 1000 ms intervals of execution time</li>
 * </ul>
 * @author ne
 */
public class SummarizedProcessingTimes {
  int execCount = 0;
  long totalExecTime = 0;
  long maxExecTime = 0;
  long minExecTime = Long.MAX_VALUE;
  Map <Long, Integer> execTimeIntervals = new HashMap<>();

  public void log (long start, long end) {
    log ( end - start);
  }

  public void log (long execTime) {
    execCount++;
    totalExecTime += execTime;
    maxExecTime = Math.max(maxExecTime, execTime);
    minExecTime = Math.min(minExecTime, execTime);
    long execTimeRounded = execTime<1000 ? ((execTime + 99) / 100) * 100 :
                                                     ((execTime + 999) / 1000) * 1000;

    if (execTimeIntervals.containsKey(execTimeRounded)) {
      execTimeIntervals.put(execTimeRounded,execTimeIntervals.get(execTimeRounded)+1);
    } else {
      execTimeIntervals.put(execTimeRounded,1);
    }
  }
}
