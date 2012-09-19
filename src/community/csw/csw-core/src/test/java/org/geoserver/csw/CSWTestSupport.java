package org.geoserver.csw;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.data.test.MockData;
import org.geoserver.test.ows.KvpRequestReaderTestSupport;
import org.geotools.csw.CSW;
import org.geotools.csw.CSWConfiguration;
import org.geotools.csw.DC;
import org.geotools.csw.DCT;
import org.geotools.filter.v1_1.OGC;
import org.geotools.ows.OWS;
import org.geotools.xlink.XLINK;
import org.geotools.xml.Configuration;
import org.geotools.xml.Parser;
import org.w3c.dom.Document;
import org.xml.sax.SAXParseException;

public abstract class CSWTestSupport extends KvpRequestReaderTestSupport {
    protected static final String BASEPATH = "csw";
    
    protected void setUpInternal() throws Exception {
        // init xmlunit
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("csw", CSW.NAMESPACE);
        namespaces.put("dc", DC.NAMESPACE);
        namespaces.put("dct", DCT.NAMESPACE);
        namespaces.put("csw", CSW.NAMESPACE);
        namespaces.put("ows", OWS.NAMESPACE);
        namespaces.put("ogc", OGC.NAMESPACE);
        namespaces.put("gml", "http://www.opengis.net/gml");
        namespaces.put("xlink", XLINK.NAMESPACE);
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        namespaces.put("xsd", "http://www.w3.org/2001/XMLSchema");
        namespaces.put("xs", "http://www.w3.org/2001/XMLSchema");

        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
    };
    
    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        // do not call super, we don't need all the normal layers
        // super.populateDataDirectory(dataDirectory);
        
        // copy all records into the data directory
        File root = dataDirectory.getDataDirectoryRoot();
        File catalog = new File(root, "catalog");
        File records = new File("./src/test/resources/org/geoserver/csw/records");
        FileUtils.copyDirectory(records, catalog);
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
    
    /**
     * Loads the specified resource into a string
     * @param resourceLocation
     * @return
     */
    protected String getResourceAsString(String resourceLocation) throws IOException {
        InputStream is = null; 
        try {
            is = getClass().getResourceAsStream(resourceLocation);
            return IOUtils.toString(is);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }
    
    /**
     * Loads the specified resource into a reader
     * @param resourceLocation
     * @return
     */
    protected Reader getResourceAsReader(String resourceLocation) throws IOException {
        InputStream is = getClass().getResourceAsStream(resourceLocation);
        return new InputStreamReader(is);
    }

}
