/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nativelibs4java.opencl.demos;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author ochafik
 */
public class SetupUtilsTest {
    @Test
    public void checkValidDownloadLinks() throws IOException {
        for (SetupUtils.DownloadURL url : SetupUtils.DownloadURL.values()) {
            HttpURLConnection con = (HttpURLConnection) url.url.openConnection();
            assertEquals("Bad url for " + url + " : " + url.url, 200, con.getResponseCode());
            con.disconnect();
        }
    }
}
