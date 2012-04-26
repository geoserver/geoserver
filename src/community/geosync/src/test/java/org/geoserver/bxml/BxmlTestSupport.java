package org.geoserver.bxml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.logging.Logger;

import javax.xml.stream.XMLInputFactory;

import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.LiteralExpressionImpl;
import org.geotools.util.logging.Logging;
import org.gvsig.bxml.adapt.sax.XmlToBxmlSaxConverter;
import org.gvsig.bxml.adapt.stax.XmlStreamReaderAdapter;
import org.gvsig.bxml.stream.BxmlFactoryFinder;
import org.gvsig.bxml.stream.BxmlInputFactory;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.gvsig.bxml.util.ProgressListener;
import org.opengis.filter.spatial.BinarySpatialOperator;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public abstract class BxmlTestSupport extends TestCase {

    private boolean isBinary;

    private static final Logger LOGGER = Logging.getLogger(BxmlTestSupport.class);

    @Override
    protected void runTest() throws Throwable {

        final String isBinaryString = System.getProperty("isBinaryXML");
        if (null != isBinaryString) {
            LOGGER.fine("System property isBinaryXML explicitly set, avoiding double run...");
            isBinary = Boolean.valueOf(isBinaryString);
            super.runTest();
        } else {
            isBinary = false;
            super.runTest();
            isBinary = true;
            super.runTest();
        }
    }

    protected BxmlStreamReader getReader(final String resource) throws Exception {

        final String xmlResource = resource + ".xml";
        final String bxmlResource = resource + ".bxml";

        final InputStream input;
        if (isBinary) {
            if (null == getClass().getResourceAsStream(bxmlResource)) {
                LOGGER.warning(" ----------- BXML resource " + bxmlResource + " not found by "
                        + getClass().getName() + ", encoding XML resource...");
                input = getClass().getResourceAsStream(xmlResource);
            } else {
                input = getClass().getResourceAsStream(bxmlResource);
            }
        } else {
            input = getClass().getResourceAsStream(xmlResource);
        }

        return getReader(input);
    }

    protected BxmlStreamReader getReader(final InputStream in) throws Exception {
        final ByteArrayOutputStream buff = new ByteArrayOutputStream();
        IOUtils.copy(in, buff);

        final boolean dataIsBinary;
        if (1 == buff.toByteArray()[0]) {
            dataIsBinary = true;
        } else {
            dataIsBinary = false;
        }

        BxmlStreamReader reader;

        if (dataIsBinary) {
            reader = getBxmlReader(new ByteArrayInputStream(buff.toByteArray()));
        } else {
            reader = getXmlReader(new ByteArrayInputStream(buff.toByteArray()));
        }
        if (isBinary && !dataIsBinary) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            new XmlToBxmlSaxConverter().convert(new ByteArrayInputStream(buff.toByteArray()), out,
                    ProgressListener.NULL, true);
            reader = getBxmlReader(new ByteArrayInputStream(out.toByteArray()));
        }

        return reader;
    }

    private BxmlStreamReader getBxmlReader(final InputStream input) throws Exception {
        BxmlInputFactory factory = BxmlFactoryFinder.newInputFactory();

        factory.setNamespaceAware(true);
        BxmlStreamReader reader = factory.createScanner(input);
        return reader;
    }

    private BxmlStreamReader getXmlReader(final InputStream input) throws Exception {
        XMLInputFactory factory = XMLInputFactory.newInstance();

        factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
        BxmlStreamReader reader = new XmlStreamReaderAdapter(factory, input);
        return reader;
    }

    protected void testSpatialBinaryOperation(BinarySpatialOperator comparisonOperator,
            String property, double[][] fs) {
        assertEquals(property,
                ((AttributeExpressionImpl) comparisonOperator.getExpression1()).getPropertyName());
        Polygon p = (Polygon) ((LiteralExpressionImpl) comparisonOperator.getExpression2())
                .getValue();
        LineString exteriorRing = p.getExteriorRing();
        testLineString(fs, exteriorRing);
    }

    protected void testLineString(double[][] fs, LineString exteriorRing) {
        for (int i = 0; i < fs.length; i++) {
            Coordinate coordinate = exteriorRing.getCoordinateN(i);
            assertEquals(fs[i][0], coordinate.x, 0.01);
            assertEquals(fs[i][1], coordinate.y, 0.01);
            if (fs[i].length > 2) {
                assertEquals(fs[i][2], coordinate.z, 0.01);
            }
        }
    }

    protected void testPoint(double[] expected, Point point) {
        assertEquals(expected[0], point.getCoordinate().x);
        assertEquals(expected[1], point.getCoordinate().y);
        if (expected.length > 2) {
            assertEquals(expected[2], point.getCoordinate().z);
        }
    }

    protected void testMultiPoint(double[][] ds, MultiPoint multiPoint) {
        for (int i = 0; i < ds.length; i++) {
            testPoint(ds[i], (Point) multiPoint.getGeometryN(i));
        }
    }

    public boolean isBinary() {
        return isBinary;
    }

}
