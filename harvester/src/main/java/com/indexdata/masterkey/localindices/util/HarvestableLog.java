/*
 * Copyright (c) 1995-2009, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author jakub
 */
public class HarvestableLog {
    // charset and decoder for ISO-8859-15
    private static Charset charset = Charset.forName("ISO-8859-15");
    private static CharsetDecoder decoder = charset.newDecoder();
    // pattern used to parse lines
    private static File logFile = new File("/var/cache/harvested/harvester.log");

    // break the charBuffer into lines
    private static void readLines(Pattern lp, CharBuffer in, StringBuilder out) {
        Matcher lm = lp.matcher(in);	// Line matcher
        int lines = 0;
        while (lm.find()) {
            lines++;
            out.append(lm.group()); 	// The current line
            if (lm.end() == in.limit())
                break;
        }
    }


    public static String getHarvestableLog(long jobId) throws FileNotFoundException, IOException {
        // Open the file and then get a channel from the stream
        FileInputStream fis = new FileInputStream(logFile);
        FileChannel fc = fis.getChannel();

        // Get the file's size and then map it into memory
        int sz = (int)fc.size();
        MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, sz);

        // Decode the file into a char buffer
        CharBuffer cb = decoder.decode(bb);

        // Perform filtering
        StringBuilder sb = new StringBuilder(1024);
        Pattern lp = Pattern.compile(".*JOB#"+jobId+".*\r?\n");
        readLines(lp, cb ,sb);

        // Close the channel and the stream
        fc.close();
        return sb.toString();
    }

}
