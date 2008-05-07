
package com.indexdata.localindexes.scheduler;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Formatter;
import com.indexdata.localindexes.scheduler.exception.CronLineParseException;

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

/* minimal change */

public class CronLine {
    private String[] fields;
    private final static int nfields = 5;

    /**
     * Constructs a CronLine from a string representation.
     * @param line For example: "55 23 * * 1" which means every Tuesday 23:55
     */
    public CronLine(String line) {
        if (line == null)
            throw new CronLineParseException("Supplied cron line is null");
        
        fields = line.split(" +");
        // todo: throw an exception if not exactly 5 numerical fields!
        if ((fields == null) || (fields.length != nfields)) {
            throw new CronLineParseException("Supplied cron line '" + line +"' cannot be parsed.");
        }
    } // Cronline constructor

    public boolean matches (CronLine pattern) {
        boolean m=true;
        for ( int i = 0; i<fields.length; i++) {
            String pf = pattern.fields[i];
            String ff = fields[i];
            if (!pf.equals("*") && !pf.equals(ff)) {
                m=false;
            }                
        }
        return m;
    }
    
    public static CronLine currentCronLine() {
        GregorianCalendar g = new GregorianCalendar(); // defaults to now()
        int min = g.get(Calendar.MINUTE);
        int hr  = g.get(Calendar.HOUR_OF_DAY);
        int mday= g.get(Calendar.DAY_OF_MONTH);
        int mon = g.get(Calendar.MONTH)+1;  // JAN = 1
        int wday= g.get(Calendar.DAY_OF_WEEK);
        Formatter f = new Formatter();
        f.format("%d %d %d %d %d", min, hr, mday, mon, wday);
        CronLine c = new CronLine(f.toString());
        return c;
    }

    @Override
    public String toString() {
        String s="";
        for (String f : fields) {
            s = s + f + " ";
        }
        return s;
    }
    
} // class CronLine
