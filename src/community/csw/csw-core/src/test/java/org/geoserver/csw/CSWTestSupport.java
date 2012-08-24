package org.geoserver.csw;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;

import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.data.test.MockData;
import org.geoserver.test.GeoServerTestSupport;
import org.geotools.csw.CSW;
import org.geotools.csw.CSWConfiguration;
import org.geotools.csw.DC;
import org.geotools.csw.DCT;
import org.geotools.ows.v1_1.OWS;
import org.geotools.xlink.XLINK;
import org.geotools.xml.Configuration;
import org.geotools.xml.Parser;
import org.w3c.dom.Document;
import org.xml.sax.SAXParseException;

public abstract class CSWTestSupport extends GeoServerTestSupport {

    protected void setUpInternal() throws Exception {
        // init xmlunit
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("csw", CSW.NAMESPACE);
        namespaces.put("dc", DC.NAMESPACE);
        namespaces.put("dct", DCT.NAMESPACE);
        namespaces.put("csw", CSW.NAMESPACE);
        namespaces.put("ows", OWS.NAMESPACE);
        namespaces.put("gml", "http://www.opengis.net/gml");
        namespaces.put("xlink", XLINK.NAMESPACE);
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");

        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
    };
    
    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        // we need no data whatsoever for most of the CSW tests, let's not waste time adding some
    }

    protected String root() {
        return "csw?";
    }

    /**
     * Validates a document based on the CSW schemas
     * 
     * @throws TransformerException
     * @throws ParserConfigurationException
     */
    protected void checkValidationErrors(Document dom) throws Exception {
        checkValidationErrors(dom, new CSWConfiguration());
    }

    /**
     * Validates a document against the
     * 
     * @param dom
     * @param configuration
     */
    protected void checkValidationErrors(Document dom, Configuration configuration)
            throws Exception {
        Parser p = new Parser(configuration);
        p.setValidating(true);
        p.parse(new DOMSource(dom));

        if (!p.getValidationErrors().isEmpty()) {
            for (Iterator e = p.getValidationErrors().iterator(); e.hasNext();) {
                SAXParseException ex = (SAXParseException) e.next();
                System.out.println(ex.getLineNumber() + "," + ex.getColumnNumber() + " -"
                        + ex.toString());
            }
            fail("Document did not validate.");
        }
    }

}
