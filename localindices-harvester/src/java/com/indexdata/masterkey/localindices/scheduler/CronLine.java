/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.scheduler;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Formatter;
import com.indexdata.masterkey.localindices.scheduler.exception.CronLineParseException;

/**
 * a CronLine is an internal representation of the time specification
 * as used by cron. It consists of 5 fields: min, hr, mday, month, wday.
 * Each of these is a simple numerical String (but that can be changed later,
 * if we ever want to implement advanced features like 14,45 or 2/5).
 * 
 * A cronline knows how to match itself against another cron line - normally 
 * containing a fully specified version of the current time. There is also
 * a method for getting such a string from the system time.
 * 
 * @author heikki
 */
public class CronLine {

    public final static int DAILY_PERIOD = 24 * 60;
    public final static int WEEKLY_PERIOD = 7 * 24 * 60;
    public final static int MONTHLY_PERIOD = 31 * 24 * 60;
    public final static int YEARLY_PERIOD = 12 * 31 * 24 * 60;
    private String[] fields;
    private final static int nfields = 5;

    /**
     * Constructs a CronLine from a string representation.
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

    public static CronLine currentCronLine() {
        Calendar g = new GregorianCalendar(); // defaults to now()

        int min = g.get(Calendar.MINUTE);
        int hr = g.get(Calendar.HOUR_OF_DAY);
        int mday = g.get(Calendar.DAY_OF_MONTH);
        int mon = g.get(Calendar.MONTH) + 1;  // JAN = 1

        int wday = g.get(Calendar.DAY_OF_WEEK);
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
     * Checks granularity of the cron line.
     * @return granularity in minutes
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
} // class CronLine
