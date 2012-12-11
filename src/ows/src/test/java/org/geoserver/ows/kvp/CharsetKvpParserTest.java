/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.kvp;

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

import junit.framework.TestCase;

public class CharsetKvpParserTest extends TestCase {
    
    private CharsetKVPParser parser;

    @Override
    protected void setUp() throws Exception {
        parser = new CharsetKVPParser("charset");
    }
    
    public void testUTF8() throws Exception {
        Charset charset = (Charset) parser.parse("UTF-8");
        assertNotNull(charset);
        assertEquals(Charset.forName("UTF-8"), charset);
    }
    
    public void testInvalid() throws Exception {
        try {
            parser.parse("invalidCharset");
            fail("Should have failed with an exception?"); 
        } catch(UnsupportedCharsetException e) {
        }
    }

}
