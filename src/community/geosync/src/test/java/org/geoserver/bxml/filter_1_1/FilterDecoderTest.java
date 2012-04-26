package org.geoserver.bxml.filter_1_1;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.geoserver.bxml.BxmlTestSupport;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.filter.v1_1.OGCConfiguration;
import org.geotools.xml.Parser;
import org.gvsig.bxml.adapt.stax.XmlStreamWriterAdapter;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.gvsig.bxml.stream.BxmlStreamWriter;
import org.gvsig.bxml.stream.EncodingOptions;
import org.opengis.filter.Filter;

public class FilterDecoderTest extends BxmlTestSupport {

    FilterDecoder decoder;

    @Override
    protected void setUp() throws Exception {
        decoder = new FilterDecoder();
    }

    /*
     * protected void setUpInternal() throws Exception { decoder = new FilterDecoder();
     * EncoderConfig mockEncoderConfig = mock(EncoderConfig.class);
     * when(mockEncoderConfig.getConfiguration()).thenReturn( new
     * org.geotools.wfs.v1_1.WFSConfiguration());
     * when(mockEncoderConfig.getSrsNameStyle()).thenReturn( GMLInfo.SrsNameStyle.XML);
     * 
     * }
     */

    public void testFilterDecoder() throws Exception {
        testFilter("a = 'a1' OR NOT(b='b1' AND c<'c1' AND d>'d1' AND e<='e1' AND f>='f1')");
    }

    public void testFilterDecoder2() throws Exception {
        Object expected = new Parser(new OGCConfiguration()).parse(getClass().getResourceAsStream(
                "filter.xml"));

        BxmlStreamReader reader = getReader("filter");
        reader.nextTag();

        Filter f = decoder.decode(reader);
        System.out.println("isBinary=" + isBinary());
        System.out.println(f.toString());
        System.out.println(expected.toString());
        assertNotNull(f);
        assertEquals(expected.toString(), f.toString());
    }

    public void testNot() throws Exception {
        testFilter("NOT(b='b1' AND c<'c1' AND d>'d1' AND e<='e1' AND f>='f1')");
    }

    public void testBinaryLogicComparison() throws Exception {
        testFilter("'and' = 'andValue1' AND (or1 = 'orvalue1' OR or2 = 'orvalue2')");
    }

    public void testBinaryComparisonOperation() throws Exception {
        testFilter("a = 'a1'");
        testFilter("b <> 'b1'");
        testFilter("c < 26");
        testFilter("d > 65");
        testFilter("f >= 37");
        testFilter("g <= 48");
    }

    public void testPropertyIsLike() throws Exception {
        testFilter("a like 'abc'");
        testFilter("b like 'ab c d'");
    }

    public void testPropertyIsNull() throws Exception {
        testFilter("a is null");
    }

    public void testPropertyIsBetween() throws Exception {
        testFilter("a between 100 and 200");
    }

    public void testAritmeticOperationDecoder() throws Exception {
        testFilter("a = (b+100)");
        testFilter("a = (b-100)");
        testFilter("a = (b*100)");
        testFilter("a = (b/100)");
    }

    public void testFunctionExpressionDecoder() throws Exception {
        testFilter("property4 = sin([dispersion])");
    }

    /*
     * public void testEquals() throws Exception {
     * testFilter("equals(geometry, POLYGON ((10 10, 20 20, 30 30, 40 40, 10 10), " +
     * "(15 16, 17 18, 19 20, 21 21, 15 16), (5 5, 7 7, 11 11, 13 13, 5 5)))");
     * 
     * }
     */

    public void testEquals() throws Exception {
        testFilterFromFile("[ geometry equals POLYGON ((10 10, 20 20, 30 30, 40 40, 10 10), "
                + "(15 16, 17 18, 19 20, 21 21, 15 16), (5 5, 7 7, 11 11, 13 13, 5 5)) ]", "equals");

    }

    public void testDisjoint() throws Exception {
        testFilterFromFile("[ geometry2 disjoint POLYGON ((10 10, 20 20, 30 30, 40 40, 10 10)) ]",
                "disjoint");
    }

    // TODO: This method is commented because toString() of TouchesImpl return
    // [ geometry3nullPOLYGON ((10 10, 20 20, 30 30, 40 40, 10 10)) ]
    /*
     * public void testTouches() throws Exception {
     * testFilterFromFile("[ geometry3 touches POLYGON ((10 10, 20 20, 30 30, 40 40, 10 10)) ]",
     * "touches"); }
     */

    public void testWithin() throws Exception {
        testFilterFromFile("[ geometry4 within POLYGON ((10 10, 20 20, 30 30, 40 40, 10 10)) ]",
                "within");
    }

    public void testOverlaps() throws Exception {
        testFilterFromFile("[ geometry5 overlaps POLYGON ((10 10, 20 20, 30 30, 40 40, 10 10)) ]",
                "overlaps");
    }

    public void testCrosses() throws Exception {
        testFilterFromFile("[ geometry6 crosses POLYGON ((10 10, 20 20, 30 30, 40 40, 10 10)) ]",
                "crosses");
    }

    public void testIntersects() throws Exception {
        testFilterFromFile(
                "[ geometry7 intersects POLYGON ((10 10, 20 20, 30 30, 40 40, 10 10)) ]",
                "intersects");
    }

    public void testContains() throws Exception {
        testFilterFromFile("[ geometry8 contains POLYGON ((10 10, 20 20, 30 30, 40 40, 10 10)) ]",
                "contains");
    }

    public void testDWithin() throws Exception {
        testFilterFromFile(
                "[ geometry9 dwithin POLYGON ((10 10, 20 20, 30 30, 40 40, 10 10)), distance: 101.25 ]",
                "dwithin");
    }

    public void testBeyond() throws Exception {
        testFilterFromFile(
                "[ geometry10 beyond POLYGON ((10 10, 20 20, 30 30, 40 40, 10 10)), distance: 142.23 ]",
                "beyond");
    }

    public void testBBox() throws Exception {
        testFilterFromFile(
                "[ geometry11 bbox POLYGON ((13.0983 31.5899, 13.0983 42.8143, 35.5472 42.8143, 35.5472 31.5899, 13.0983 31.5899)) ]",
                "bbox");
    }

    private BxmlStreamReader getXmlReader(final Filter expected) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        XMLStreamWriter staxWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(out);
        BxmlStreamWriter w = new XmlStreamWriterAdapter(new EncodingOptions(), staxWriter);
        new FilterEncoder().encode(expected, w);

        byte[] buf = out.toByteArray();
        String string = new String(buf);
        string = "<Filter xmlns=\"http://www.opengis.net/ogc\" xmlns:gml=\"http://www.opengis.net/gml\">"
                + string + "</Filter>";

        ByteArrayInputStream in = new ByteArrayInputStream(string.getBytes("UTF-8"));
        return getReader(in);
    }

    private void testFilterFromFile(final String expected, final String fileName) throws Exception,
            IOException {
        BxmlStreamReader reader = getReader(fileName);
        reader.nextTag();

        Filter f = decoder.decode(reader);
        System.out.println(f.toString());
        assertNotNull(f);
        assertEquals(expected.toString(), f.toString());
    }

    private void testFilter(final String ecql) throws Exception, IOException {

        Filter expectedFilter = ECQL.toFilter(ecql);
        BxmlStreamReader reader = getXmlReader(expectedFilter);
        reader.nextTag();

        Filter f = decoder.decode(reader);
        assertNotNull(f);
        assertEquals(expectedFilter.toString(), f.toString());
        reader.close();
    }

}
