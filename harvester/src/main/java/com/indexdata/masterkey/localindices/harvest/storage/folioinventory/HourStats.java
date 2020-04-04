/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.indexdata.masterkey.localindices.harvest.storage.folioinventory;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author ne
 */
public class HourStats {
  int execCount = 0;
  long totalExecTime = 0;
  long maxExecTime = 0;
  long minExecTime = Long.MAX_VALUE;
  Map <Long, Integer> execTimeIntervals = new HashMap<>();
  Map <String, Long> execTimeByOperation = new HashMap<>();

  public void log (long start, long end) {
    long execTime = end - start;
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
