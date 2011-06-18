package org.geoserver.wfs.v1_1;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.opengis.wfs.GetCapabilitiesType;
import net.opengis.wfs.WfsFactory;

import org.geoserver.util.ErrorHandler;
import org.geoserver.util.ReaderUtils;
import org.geoserver.wfs.CapabilitiesTransformer;
import org.geoserver.wfs.WFSTestSupport;
import org.geoserver.wfs.xml.v1_1_0.WFS;
import org.w3c.dom.Document;

public class CapabilitiesTransformerTest extends WFSTestSupport {

    static Logger logger = org.geotools.util.logging.Logging.getLogger("org.geoserver.wfs.test");

    GetCapabilitiesType request() {
        GetCapabilitiesType type = WfsFactory.eINSTANCE.createGetCapabilitiesType();
        type.setBaseUrl("http://localhost:8080/geoserver");
        return type;
    }

    public void test() throws Exception {
        CapabilitiesTransformer tx = new CapabilitiesTransformer.WFS1_1(getWFS(), getCatalog());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        tx.transform(request(), output);

        InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(output
                .toByteArray()));

        File f = new File("../web/src/main/webapp/schemas/wfs/1.1.0/wfs.xsd" );
        if ( !f.exists() ) {
            return;
        }
        
        ErrorHandler handler = new ErrorHandler(logger, Level.WARNING);
        // use the schema embedded in the web module
        ReaderUtils.validate(reader, handler, WFS.NAMESPACE,
                "../web/src/main/webapp/schemas/wfs/1.1.0/wfs.xsd");

        assertTrue(handler.errors.isEmpty());

    }

    /**
     * see GEOS-2461
     */
    public void testDefaultOutputFormat() throws Exception {
        CapabilitiesTransformer tx = new CapabilitiesTransformer.WFS1_1(getWFS(), getCatalog());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        tx.transform(request(), output);

        Document dom = super.dom(new ByteArrayInputStream(output.toByteArray()));

        // XpathEngine xpath = XMLUnit.newXpathEngine();

        final String expected = "text/xml; subtype=gml/3.1.1";
        String xpathExpr = "//wfs:WFS_Capabilities/ows:OperationsMetadata/ows:Operation[@name='DescribeFeatureType']"
                + "/ows:Parameter[@name='outputFormat']/ows:Value";
        assertXpathEvaluatesTo(expected, xpathExpr, dom);

        xpathExpr = "//wfs:WFS_Capabilities/ows:OperationsMetadata/ows:Operation[@name='GetFeature']"
                + "/ows:Parameter[@name='outputFormat']/ows:Value";
        assertXpathEvaluatesTo(expected, xpathExpr, dom);

        xpathExpr = "//wfs:WFS_Capabilities/ows:OperationsMetadata/ows:Operation[@name='GetFeatureWithLock']"
                + "/ows:Parameter[@name='outputFormat']/ows:Value";
        assertXpathEvaluatesTo(expected, xpathExpr, dom);

        xpathExpr = "//wfs:WFS_Capabilities/ows:OperationsMetadata/ows:Operation[@name='Transaction']"
                + "/ows:Parameter[@name='inputFormat']/ows:Value";
        assertXpathEvaluatesTo(expected, xpathExpr, dom);
    }
}
