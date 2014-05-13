/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nativelibs4java.velocity;

import static com.nativelibs4java.velocity.Utils.*;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author ochafik
 */
public class UtilsTest {
    @Test
    public void testQuote() {
        assertEquals(quoteSharpsInComments("#"), "#");
        String q = quoteSharpsInComments("a#\n/*\n#c*/");
        assertEquals("a#\n/*\nb\\#c*/", quoteSharpsInComments("a#\n/*\nb#c*/"));
        assertEquals("a#\n/*\n#b*/", quoteSharpsInComments("a#\n/*\n#b*/"));
        assertEquals("a#\n/*\n#c*/", unquoteSharpsInComments("a#\n/*\n\\#c*/"));
    }
}