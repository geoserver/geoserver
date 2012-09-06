package org.geoserver.csw;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;
import net.opengis.ows10.GetCapabilitiesType;

import org.eclipse.emf.common.util.EList;
import org.geoserver.csw.kvp.GetCapabilitiesKvpRequestReader;
import org.geoserver.csw.xml.v2_0_2.CSWXmlReader;
import org.geoserver.platform.ServiceException;
import org.geotools.csw.CSWConfiguration;
import org.xml.sax.SAXParseException;

public class GetCapabilitiesTest extends CSWTestSupport {
    
    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new GetCapabilitiesTest());
    }

    public void testKVPReader() throws Exception {
        Map<String, Object> raw = new HashMap<String, Object>();
        raw.put("service", "CSW");
        raw.put("request", "GetCapabilities");
        raw.put("acceptVersions", "2.0.2,2.0.0,0.7.2");
        raw.put("sections", "OperationsMetadata,foo");
        raw.put("acceptFormats", "application/xml,text/plain");

        GetCapabilitiesKvpRequestReader reader = new GetCapabilitiesKvpRequestReader();
        Object request = reader.createRequest();
        GetCapabilitiesType caps = (GetCapabilitiesType) reader.read(request, parseKvp(raw), raw);
        assertReturnedCapabilitiesComplete(caps);
    }

    private void assertReturnedCapabilitiesComplete(GetCapabilitiesType caps) {
        assertNotNull(caps);
        
        EList versions = caps.getAcceptVersions().getVersion();
        assertEquals(3, versions.size());
        assertEquals("2.0.2", versions.get(0));
        assertEquals("2.0.0", versions.get(1));
        assertEquals("0.7.2", versions.get(2));
        
        EList sections = caps.getSections().getSection();
        assertEquals(2, sections.size());
        assertEquals("OperationsMetadata", sections.get(0));
        assertEquals("foo", sections.get(1));
        
        EList outputFormats = caps.getAcceptFormats().getOutputFormat();
        assertEquals(2, outputFormats .size());
        assertEquals("application/xml", outputFormats .get(0));
        assertEquals("text/plain", outputFormats .get(1));
    }
    
    public void testXMLReader() throws Exception {
        CSWXmlReader reader = new CSWXmlReader("GetCapabilities", "2.0.2", new CSWConfiguration());
        GetCapabilitiesType caps = (GetCapabilitiesType)  reader.read(null, getResourceAsReader("GetCapabilities.xml"), (Map) null);
        assertReturnedCapabilitiesComplete(caps);
    }
    
    public void testXMLReaderInvalid() throws Exception {
        // create a schema invalid request
        String capRequest = getResourceAsString("GetCapabilities.xml");
        capRequest = capRequest.replace("ows:Sections", "ows:foo");
        try {
            CSWXmlReader reader = new CSWXmlReader("GetCapabilities", "2.0.2", new CSWConfiguration());
            reader.read(null, new StringReader(capRequest), (Map) null);
            fail("the parsing should have failed, the document is invalid");
        } catch(ServiceException e) {
            // it is a validation exception right?
            assertTrue(e.getCause() instanceof SAXParseException);
            SAXParseException cause = (SAXParseException) e.getCause();
            assertTrue(cause.getMessage().contains("ows:foo"));
        }   
    }
}
