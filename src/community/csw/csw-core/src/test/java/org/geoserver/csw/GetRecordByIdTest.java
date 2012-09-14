package org.geoserver.csw;

import java.util.HashMap;
import java.util.Map;
import junit.framework.Test;
import net.opengis.cat.csw20.ElementSetType;
import net.opengis.cat.csw20.GetRecordByIdType;
import org.geoserver.csw.kvp.GetRecordByIdKvpRequestReader;
import org.geoserver.csw.xml.v2_0_2.CSWXmlReader;
import org.geotools.csw.CSWConfiguration;


/**
 * 
 * @author Niels Charlier
 *
 */
public class GetRecordByIdTest extends CSWTestSupport {

    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new GetRecordByIdTest());
    }
 
    public void testKVPReader() throws Exception {
        Map<String, Object> raw = new HashMap<String, Object>();
        raw.put("service", "CSW");
        raw.put("version", "2.0.2");
        raw.put("request", "GetRecordById");
        raw.put("elementsetname", "summary");
        raw.put("id", "REC-10,REC-11,REC-12");
        raw.put("outputFormat", "application/xml");
        raw.put("outputSchema", "http://www.opengis.net/cat/csw/2.0.2");

        GetRecordByIdKvpRequestReader reader = new GetRecordByIdKvpRequestReader();
        Object request = reader.createRequest();
        GetRecordByIdType dr = (GetRecordByIdType) reader.read(request, parseKvp(raw), raw);
        
        assertGetRecordByIdValid(dr);
    }

    private void assertGetRecordByIdValid(GetRecordByIdType dr) {
        assertEquals("CSW", dr.getService());
        assertEquals("2.0.2", dr.getVersion());
        assertEquals(ElementSetType.SUMMARY, dr.getElementSetName().getValue());
        assertEquals("REC-10", dr.getId().get(0).toString());
        assertEquals("REC-11", dr.getId().get(1).toString());
        assertEquals("REC-12", dr.getId().get(2).toString());
    }

    public void testXMLReader() throws Exception {
        CSWXmlReader reader = new CSWXmlReader("GetRecordById", "2.0.2", new CSWConfiguration());
        GetRecordByIdType dr = (GetRecordByIdType)  reader.read(null, getResourceAsReader("GetRecordById.xml"), (Map) null);
        assertGetRecordByIdValid(dr);
    }

}
