/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENCE for details.
 */
package com.indexdata.masterkey.localindices.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 *
 * @author jakub
 */
public class TextUtils {

    public static void copyStream(InputStream is, OutputStream os) throws IOException {
        byte[] buf = new byte[4096];
        for (int len = -1; (len = is.read(buf)) != -1;) {
            os.write(buf, 0, len);
        }
        os.flush();
    }

    public static void copyStreamWithReplace(InputStream is, OutputStream os,
            String from, String to) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        for (String line = null; (line = br.readLine()) != null;) {
            sb.append(line.replaceAll(from, to) + "\n");
        }
        br.close();
        os.write(sb.toString().getBytes());
    }

    public static String readStream(InputStream stream) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        StringBuilder sb = new StringBuilder();
        String line = null;

        while ((line = br.readLine()) != null) {
            sb.append(line + "\n");
        }

        br.close();
        return sb.toString();
    }
}
