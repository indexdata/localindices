/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.indexdata.localindexes.scheduler;

import java.util.Calendar;
import java.util.Formatter;
import java.util.GregorianCalendar;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jakub
 */
public class CronLineTest {

    public CronLineTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {        
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of matches method, of class CronLine.
     */
    @Test
    public void testMatches() {
        CronLine pattern = new CronLine("1 2 3 * *");
        CronLine instance = new CronLine("* * * 1 2");
        assertEquals(false, instance.matches(pattern));
        assertEquals(false, pattern.matches(instance));
        pattern = new CronLine("* * * 1 2");
        instance = new CronLine("20 4 5 1 2");
        assertEquals(false, pattern.matches(instance));
        assertEquals(true, instance.matches(pattern));
    }

    /**
     * Test of currentCronLine method, of class CronLine.
     */
    @Test
    public void testCurrentCronLine() {
        Calendar g = new GregorianCalendar(); // defaults to now()
        int min = g.get(Calendar.MINUTE);
        int hr  = g.get(Calendar.HOUR_OF_DAY);
        int mday= g.get(Calendar.DAY_OF_MONTH);
        int mon = g.get(Calendar.MONTH) + 1;  // JAN = 1
        int wday= g.get(Calendar.DAY_OF_WEEK);
        Formatter f = new Formatter();
        f.format("%d %d %d %d %d", min, hr, mday, mon, wday);
        assertEquals(f.toString(), CronLine.currentCronLine().toString());
    }

    /**
     * Test of toString method, of class CronLine.
     */
    @Test
    public void testToString() {
        String expResult = "1 2 3 4 5";
        CronLine instance = new CronLine(expResult);
        String result = instance.toString();
        assertEquals(expResult, result);
    }

    /**
     * Test of shortestPeriod method, of class CronLine.
     */
    @Test
    public void testShortestPeriod() {        
        CronLine instance = new CronLine("* * * * *");
        assertTrue(instance.shortestPeriod() < CronLine.DAILY_PERIOD);
        
        instance = new CronLine("0 * * * *");
        assertTrue(instance.shortestPeriod() < CronLine.DAILY_PERIOD);
        
        instance = new CronLine("0 0 * * *");
        assertEquals(CronLine.DAILY_PERIOD, instance.shortestPeriod());
        
        instance = new CronLine("0 0 * * 1");
        assertEquals(CronLine.WEEKLY_PERIOD, instance.shortestPeriod());
        
        instance = new CronLine("0 0 * 1 1");
        assertEquals(CronLine.WEEKLY_PERIOD, instance.shortestPeriod());
        
        instance = new CronLine("0 0 1 * 1");
        assertEquals(CronLine.WEEKLY_PERIOD, instance.shortestPeriod());
        
        instance = new CronLine("0 0 1 * *");
        assertEquals(CronLine.MONTHLY_PERIOD, instance.shortestPeriod());
        
        instance = new CronLine("0 0 1 1 *");
        assertEquals(CronLine.YEARLY_PERIOD, instance.shortestPeriod());
        
        instance = new CronLine("0 0 1 1 1");
        assertEquals(CronLine.YEARLY_PERIOD, instance.shortestPeriod());
    }

}