package org.geoserver.csw;

import java.util.HashMap;
import java.util.Map;

import net.opengis.ows10.GetCapabilitiesType;

import org.eclipse.emf.common.util.EList;
import org.geoserver.csw.kvp.GetCapabilitiesKvpRequestReader;

public class GetCapabilitiesTest extends CSWTestSupport {

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
}
