package com.indexdata.masterkey.localindices.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Construct a matrix for counting occurrences of 'X' by 'Y'.
 * 
 * Y values can be pictured as the labels in the left pane of a matrix
 * and the X values as the labels going across on top. 
 * 
 * Instances are added by addObservation, the results can retrieved by
 * iterating yLabels going down the page and for each yLabel iterate xLabels
 * going across. 
 *  
 * @author Niels Erik
 *
 */
public class StatsMatrix {
  
  Map<String,CountsAcross> matrix = new HashMap<String,CountsAcross>();

  public StatsMatrix() {
  }

  public void addObservation(String yLabel, String xLabel) {
    yLabel = yLabel.trim();
    if (!matrix.containsKey(yLabel)) {
      matrix.put(yLabel, new CountsAcross());
    }
    matrix.get(yLabel).addObservation(xLabel);
  }

  public SortedSet<String> getYLabels () {
    return new TreeSet<String>(matrix.keySet());
  }
  
  public SortedSet<String> getLabelsDown () {
    return getYLabels();
  }
  
  public CountsAcross getCountsAcross(String yLabel) {
    return matrix.get(yLabel);
  }
  
  public int getCount(String yLabel, String xLabel) {
    return getCountsAcross(yLabel).getCount(xLabel);
  }
  public SortedSet<String> getXLabels () {
    Set<String> xlabels = new TreeSet<String>();
    for (String y : matrix.keySet()) {
      xlabels.addAll(matrix.get(y).getXLabels());
    }
    return new TreeSet<String>(xlabels);
  }
  public SortedSet<String> getLabelsAcross () {
    return getXLabels();
  }
  public StatsMatrix clone() {
    StatsMatrix clone = new StatsMatrix();
    for (String y : this.matrix.keySet()) {
      clone.matrix.put(y, getCountsAcross(y).clone());
    }
    return clone;
  }
  
  class CountsAcross {
    Map<String,Integer> xCounts = new HashMap<String,Integer>();
    CountsAcross () {
    }
    void addObservation(String xLabel) {
      xLabel = xLabel.trim();
      xCounts.put(xLabel, bump(xCounts.get(xLabel)));
    }
    Integer bump(Integer integer) {
      if (integer != null) {
        return new Integer(integer.intValue()+1);
      } else {
        return new Integer(1);
      }
    }
    Map<String,Integer> getXCounts () {
      return xCounts;
    }
    Set<String> getXLabels () {
      return xCounts.keySet();
    }
    int getCount(String xLabel) { 
      if (xCounts.get(xLabel)!=null) {
        return xCounts.get(xLabel).intValue();
      } else {
        return 0;
      }
    }
    public CountsAcross clone() {
      CountsAcross clone = new CountsAcross();
      clone.xCounts = new HashMap<String,Integer>();
      clone.xCounts.putAll(this.xCounts);
      return clone;
    }
  }

}
