package org.vfny.geoserver.global.xml;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import junit.framework.TestCase;

public class WriterHelperTest extends TestCase {
    
    private ByteArrayOutputStream bos;
    private WriterHelper helper;

    @Override
    protected void setUp() throws Exception {
        bos = new ByteArrayOutputStream();
        Writer writer = new OutputStreamWriter(bos);
        helper = new WriterHelper(writer);
    }
    
    public void testNoEscape() throws Exception {
        helper.textTag("title", "$%()");
        String result = bos.toString();
        assertEquals("<title>$%()</title>\n", result);
    }
    
    public void testEscapePlain() throws Exception {
        helper.textTag("title", "Test < > & ' \"");
        String result = bos.toString();
        assertEquals("<title>Test &lt; &gt; &amp; &apos; &quot;</title>\n", result);
    }
    
    public void testEscapeNewlines() throws Exception {
        helper.textTag("title", "<\n>\n");
        String result = bos.toString();
        assertEquals("<title>&lt;\n&gt;\n</title>\n", result);
    }

}
