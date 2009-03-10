/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.utils;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Formatter;
import java.util.Date;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

/**
 * A CronLine is an internal representation of the time specification
 * as used by cron. It consists of 5 fields: min, hr, mday, month, wday.
 * Each of these is a simple numerical String (but that can be changed later,
 * if we ever want to implement advanced features like 14,45 or 2/5).
 * 
 * @author heikki
 */
public class CronLine {
    public final static int MINUTE = 0;
    public final static int HOUR = 1;
    public final static int DAY_OF_MONTH = 2;
    public final static int MONTH = 3;
    public final static int DAY_OF_WEEK = 4;
    public final static int DAILY_PERIOD = 24 * 60;
    public final static int WEEKLY_PERIOD = 7 * 24 * 60;
    public final static int MONTHLY_PERIOD = 31 * 24 * 60;
    public final static int YEARLY_PERIOD = 12 * 31 * 24 * 60;
    private String[] fields;
    private final static int nfields = 5;
    private static Logger logger = Logger.getLogger("com.indexdata.masterkey.localindices.admin");

    /**
     * Constructs a CronLine from a string representation of following format:
     * "%d %d %d %d %d" applied to minute, hour, day-of-month, month, day-of-week
     * @param line For example: "55 23 * * 1" which means every Tuesday 23:55
     */
    public CronLine(String line) {
        
        if (line == null) {
            throw new CronLineParseException("Supplied cron line is null");
        }
        fields = line.split(" +");
        // todo: throw an exception if not exactly 5 numerical fields!
        if ((fields == null) || (fields.length != nfields)) {
            throw new CronLineParseException("Supplied cron line '" + line + "' cannot be parsed.");
        }

        if (!fields[0].equals("*") && (Integer.parseInt(fields[0]) < 0 || Integer.parseInt(fields[0]) > 59)) {
            throw new CronLineParseException("Minutes must have value between 0 and 59.");
        }
    } // Cronline constructor

    /**
     * Matches this cron line againts the parameter and returns true if the param
     * is more general (contains wildcards) or equal.
     * @param pattern pattern to match against
     * @return true/false
     */
    public boolean matches(CronLine pattern) {
        boolean m = true;
        for (int i = 0; i < fields.length; i++) {
            String pf = pattern.fields[i];
            String ff = fields[i];
            if (!pf.equals("*") && !pf.equals(ff)) {
                m = false;
            }
        }
        return m;
    }

    /**
     * Return a cron line that corresponds to current date and time.
     * @return and instance of CronLine
     */
    public static CronLine currentCronLine() {
        Calendar cal = new GregorianCalendar(); // defaults to now()
        return createCronLine(cal);
    }
    
    public static CronLine createCronLine (Calendar cal) {
        int min = cal.get(Calendar.MINUTE);
        int hr = cal.get(Calendar.HOUR_OF_DAY);
        int mday = cal.get(Calendar.DAY_OF_MONTH);
        int mon = cal.get(Calendar.MONTH) + 1;  // JAN = 1

        int wday = cal.get(Calendar.DAY_OF_WEEK);
        Formatter f = new Formatter();
        f.format("%d %d %d %d %d", min, hr, mday, mon, wday);
        CronLine c = new CronLine(f.toString());
        return c;        
    }

    @Override
    public String toString() {
        String s = "";
        String sep = " ";
        for (int i = 0; i < fields.length; i++) {
            if (i == fields.length - 1) {
                sep = "";
            }
            s += fields[i] + sep;
        }
        return s;
    }

    /**
     * Checks the shortest period of the cron line.
     * @return period in minutes
     */
    public int shortestPeriod() {
        int period = 0;
        if (fields[0].equals("*")) {
            return 1;
        }
        if (fields[1].equals("*")) {
            return 60;
        }
        if (fields[4].equals("*")) {
            period = CronLine.DAILY_PERIOD;
        }
        if (fields[2].equals("*")) {
            return (period == CronLine.DAILY_PERIOD ? period : CronLine.WEEKLY_PERIOD);
        }
        if (fields[3].equals("*")) {
            return (period == CronLine.DAILY_PERIOD ? CronLine.MONTHLY_PERIOD : CronLine.WEEKLY_PERIOD);
        }
        return CronLine.YEARLY_PERIOD;
    }
    
    /**
     * Returns value of a given cron field.
     * @param field cron field number
     * @return
     */
    public String get(int field) {
        if (field < 0 || field > 4) return null;
        return fields[field];
    }
    
    /**
     * Converts cron entry to date;
     * @param cronLine
     * @return
     */
    public Date toDate() {
        Calendar cal = new GregorianCalendar();
        cal.set(Calendar.AM_PM, Calendar.AM);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (!"*".equals(this.get(MINUTE))) 
            cal.set(Calendar.MINUTE, Integer.parseInt(this.get(MINUTE)));
        if (!"*".equals(this.get(HOUR))) 
            cal.set(Calendar.HOUR, Integer.parseInt(this.get(HOUR)));
        if (!"*".equals(this.get(MONTH))) 
            cal.set(Calendar.MONTH, Integer.parseInt(this.get(MONTH)) - 1);
        if (!"*".equals(this.get(DAY_OF_MONTH))) 
            cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(this.get(DAY_OF_MONTH)));
        if (!"*".equals(this.get(DAY_OF_WEEK))) 
            cal.set(Calendar.DAY_OF_WEEK, Integer.parseInt(this.get(DAY_OF_WEEK)));
        return cal.getTime();
    }

    /**
     * Finds the next day that matches this CronLine, ignoring the time of day (hr,min,seconds,millis)
     * @param offsetDate The point in time from which the search for a matching date should start
     * @return
     */
    public Date nextMatchingDate(Date offsetDate) throws CronLineParseException {        
        
        // Need to limit scan in case we're looking for a non-valid date
        int yearsToScan = 10; 
        int timeOut = 365*yearsToScan;
        // Adjust time part of offset date to make it matchable with this cron line
        Calendar cal = new GregorianCalendar();
        cal.setTime(offsetDate);        
        cal.set(Calendar.AM_PM, Calendar.AM);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.MINUTE, Integer.parseInt(this.get(MINUTE)));
        cal.set(Calendar.HOUR, Integer.parseInt(this.get(HOUR)));
        
        // Must start with a time in the future.
        if (!cal.getTime().after(new Date())) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
       
        // Create the first cron line to test
        CronLine offsetCronLine = CronLine.createCronLine(cal);
        // Make cron line a day at a time until match is found
        while (!offsetCronLine.matches(this) && timeOut>0) { 
           cal.add(Calendar.DATE, 1);
           offsetCronLine = CronLine.createCronLine(cal);
           timeOut--;
	}
        if (timeOut==0) {
            logger.log(Level.ERROR, "Could not find matching day for \""+this.toString()+"\" within the next "+yearsToScan+" years.");
            throw new CronLineParseException("Could not find matching day for \""+this.toString()+"\" within the next "+yearsToScan+" years.");
        }
        return cal.getTime();        
    }

} // class CronLine
