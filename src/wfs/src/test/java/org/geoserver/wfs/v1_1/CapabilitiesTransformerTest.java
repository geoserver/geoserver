/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.v1_1;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.opengis.wfs.GetCapabilitiesType;
import net.opengis.wfs.WfsFactory;
import org.geoserver.util.ErrorHandler;
import org.geoserver.util.ReaderUtils;
import org.geoserver.wfs.CapabilitiesTransformer;
import org.geoserver.wfs.WFSExtendedCapabilitiesProvider;
import org.geoserver.wfs.WFSTestSupport;
import org.geoserver.wfs.xml.v1_1_0.WFS;
import org.junit.Test;
import org.w3c.dom.Document;

public class CapabilitiesTransformerTest extends WFSTestSupport {

    static Logger logger = org.geotools.util.logging.Logging.getLogger("org.geoserver.wfs.test");

    GetCapabilitiesType request() {
        GetCapabilitiesType type = WfsFactory.eINSTANCE.createGetCapabilitiesType();
        type.setBaseUrl("http://localhost:8080/geoserver");
        return type;
    }

    @Test
    public void test() throws Exception {
        GetCapabilitiesType request = request();
        CapabilitiesTransformer tx =
                new CapabilitiesTransformer.WFS1_1(
                        getWFS(),
                        request.getBaseUrl(),
                        getCatalog(),
                        Collections.<WFSExtendedCapabilitiesProvider>emptyList());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        tx.transform(request, output);

        InputStreamReader reader =
                new InputStreamReader(new ByteArrayInputStream(output.toByteArray()));

        File f = new File("../web/src/main/webapp/schemas/wfs/1.1.0/wfs.xsd");
        if (!f.exists()) {
            return;
        }

        ErrorHandler handler = new ErrorHandler(logger, Level.WARNING);
        // use the schema embedded in the web module
        ReaderUtils.validate(
                reader, handler, WFS.NAMESPACE, "../web/src/main/webapp/schemas/wfs/1.1.0/wfs.xsd");

        assertTrue(handler.errors.isEmpty());
    }

    /** see GEOS-2461 */
    @Test
    public void testDefaultOutputFormat() throws Exception {
        GetCapabilitiesType request = request();
        CapabilitiesTransformer tx =
                new CapabilitiesTransformer.WFS1_1(
                        getWFS(),
                        request.getBaseUrl(),
                        getCatalog(),
                        Collections.<WFSExtendedCapabilitiesProvider>emptyList());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        tx.transform(request, output);

        Document dom = super.dom(new ByteArrayInputStream(output.toByteArray()));

        // XpathEngine xpath = XMLUnit.newXpathEngine();

        final String expected = "text/xml; subtype=gml/3.1.1";
        String xpathExpr =
                "//wfs:WFS_Capabilities/ows:OperationsMetadata/ows:Operation[@name='DescribeFeatureType']"
                        + "/ows:Parameter[@name='outputFormat']/ows:Value";
        assertXpathEvaluatesTo(expected, xpathExpr, dom);

        xpathExpr =
                "//wfs:WFS_Capabilities/ows:OperationsMetadata/ows:Operation[@name='GetFeature']"
                        + "/ows:Parameter[@name='outputFormat']/ows:Value";
        assertXpathEvaluatesTo(expected, xpathExpr, dom);

        xpathExpr =
                "//wfs:WFS_Capabilities/ows:OperationsMetadata/ows:Operation[@name='GetFeatureWithLock']"
                        + "/ows:Parameter[@name='outputFormat']/ows:Value";
        assertXpathEvaluatesTo(expected, xpathExpr, dom);

        xpathExpr =
                "//wfs:WFS_Capabilities/ows:OperationsMetadata/ows:Operation[@name='Transaction']"
                        + "/ows:Parameter[@name='inputFormat']/ows:Value";
        assertXpathEvaluatesTo(expected, xpathExpr, dom);
    }

    @Test
    public void testContactInfo() throws Exception {
        GetCapabilitiesType request = request();
        CapabilitiesTransformer tx =
                new CapabilitiesTransformer.WFS1_1(
                        getWFS(),
                        request.getBaseUrl(),
                        getCatalog(),
                        Collections.<WFSExtendedCapabilitiesProvider>emptyList());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        tx.transform(request, output);

        Document dom = super.dom(new ByteArrayInputStream(output.toByteArray()));

        String xpathExpr =
                "//wfs:WFS_Capabilities/ows:ServiceProvider/ows:ServiceContact/ows:IndividualName";
        assertXpathExists(xpathExpr, dom);
        assertXpathEvaluatesTo("Andrea Aime", xpathExpr, dom);

        xpathExpr =
                "//wfs:WFS_Capabilities/ows:ServiceProvider/ows:ServiceContact/ows:ContactInfo/ows:Address/ows:DeliveryPoint";
        assertXpathExists(xpathExpr, dom);
        assertXpathEvaluatesTo(
                "1600 Pennsylvania Ave NW, Washington DC 20500, United States", xpathExpr, dom);

        xpathExpr =
                "//wfs:WFS_Capabilities/ows:ServiceProvider/ows:ServiceContact/ows:ContactInfo/ows:Address/ows:ElectronicMailAddress";
        assertXpathExists(xpathExpr, dom);
        assertXpathEvaluatesTo("andrea@geoserver.org", xpathExpr, dom);
    }
}
