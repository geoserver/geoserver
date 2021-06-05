/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.kvp;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CharsetKvpParserTest {

    private CharsetKVPParser parser;

    @Before
    public void setUp() throws Exception {
        parser = new CharsetKVPParser("charset");
    }

    @Test
    public void testUTF8() throws Exception {
        Charset charset = (Charset) parser.parse("UTF-8");
        Assert.assertNotNull(charset);
        Assert.assertEquals(StandardCharsets.UTF_8, charset);
    }

    @Test
    public void testInvalid() throws Exception {
        try {
            parser.parse("invalidCharset");
            Assert.fail("Should have failed with an exception?");
        } catch (UnsupportedCharsetException e) {
        }
    }
}
