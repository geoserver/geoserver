package org.geoserver.wfs.kvp;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.xml.XMLConstants;

import junit.framework.TestCase;

import org.xml.sax.helpers.NamespaceSupport;

public class NamespaceKvpParserTest extends TestCase {

    private NamespaceKvpParser parser;

    protected void setUp() throws Exception {
        parser = new NamespaceKvpParser("namespace");
    }

    public void testEmpty() throws Exception {
        NamespaceSupport ctx = parser.parse("");
        assertNotNull(ctx);
        List<String> prefixes = getPrefixes(ctx);
        assertTrue(prefixes.contains("xml"));// this one is always present
        assertEquals(1, prefixes.size());
    }

    public void testFormatError() throws Exception {
        try {
            parser.parse("xmlns[bad=format]");
            fail("Expected IAE");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            parser.parse("xmlns(bad=http://format]");
            fail("Expected IAE");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            parser.parse("bad=http://format");
            fail("Expected IAE");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    public void testSingle() throws Exception {
        NamespaceSupport ctx = parser.parse("xmlns(foo=http://bar)");
        assertEquals("http://bar", ctx.getURI("foo"));
    }

    public void testMultiple() throws Exception {
        NamespaceSupport ctx = parser
                .parse("xmlns(foo=http://bar), xmlns(ex=http://example.com),xmlns(gs=http://geoserver.org)");
        assertEquals("http://bar", ctx.getURI("foo"));
        assertEquals("http://example.com", ctx.getURI("ex"));
        assertEquals("http://geoserver.org", ctx.getURI("gs"));
    }

    public void testDefaultNamespace() throws Exception{
        NamespaceSupport ctx = parser.parse("xmlns(http://default.namespace.com)");
        assertEquals("http://default.namespace.com", ctx.getURI(XMLConstants.DEFAULT_NS_PREFIX));
    }
    
    @SuppressWarnings("unchecked")
    private List<String> getPrefixes(NamespaceSupport ctx) {
        Enumeration<String> prefixes = ctx.getPrefixes();
        List<String> l = new ArrayList<String>();
        while (prefixes.hasMoreElements()) {
            l.add(prefixes.nextElement());
        }
        return l;
    }
}
