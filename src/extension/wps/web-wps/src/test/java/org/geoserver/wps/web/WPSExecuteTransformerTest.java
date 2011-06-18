package org.geoserver.wps.web;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.custommonkey.xmlunit.XMLAssert.*;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;

import junit.framework.TestCase;

import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.wps.web.InputParameterValues.ParameterType;
import org.geoserver.wps.web.InputParameterValues.ParameterValue;
import org.geoserver.wps.xml.WPSConfiguration;
import org.geotools.feature.NameImpl;
import org.geotools.xml.Configuration;
import org.geotools.xml.Parser;
import org.opengis.feature.type.Name;
import org.w3c.dom.Document;
import org.xml.sax.SAXParseException;

public class WPSExecuteTransformerTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // init xmlunit
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("wps", "http://www.opengis.net/wps/1.0.0");
        namespaces.put("ows", "http://www.opengis.net/ows/1.1");
        namespaces.put("gml", "http://www.opengis.net/gml");
        namespaces.put("wfs", "http://www.opengis.net/wfs");
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        namespaces.put("feature", "http://geoserver.sf.net");

        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
    }

    public void testSingleProcess() throws Exception {
        ExecuteRequest executeBuffer = getExecuteBuffer();

        WPSExecuteTransformer tx = new WPSExecuteTransformer();
        tx.setIndentation(2);
        String xml = tx.transform(executeBuffer);
        
        String expected = 
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
            "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
            "xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" " +
            "xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" " +
            "xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" " +
            "xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" " +
            "xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n" + 
            "  <ows:Identifier>JTS:buffer</ows:Identifier>\n" + 
            "  <wps:DataInputs>\n" + 
            "    <wps:Input>\n" + 
            "      <ows:Identifier>geom</ows:Identifier>\n" + 
            "      <wps:Data>\n" + 
            "        <wps:ComplexData mimeType=\"application/wkt\"><![CDATA[POINT(0 0)]]></wps:ComplexData>\n" + 
            "      </wps:Data>\n" + 
            "    </wps:Input>\n" + 
            "    <wps:Input>\n" + 
            "      <ows:Identifier>distance</ows:Identifier>\n" + 
            "      <wps:Data>\n" + 
            "        <wps:LiteralData>10</wps:LiteralData>\n" + 
            "      </wps:Data>\n" + 
            "    </wps:Input>\n" + 
            "  </wps:DataInputs>\n" + 
            "  <wps:ResponseForm>\n" + 
            "    <wps:RawDataOutput mimeType=\"text/xml; subtype=gml/3.1.1\">\n" + 
            "      <ows:Identifier>result</ows:Identifier>\n" + 
            "    </wps:RawDataOutput>\n" + 
            "  </wps:ResponseForm>\n" + 
            "</wps:Execute>";

        Document test = XMLUnit.buildTestDocument(xml);
        checkValidationErrors(test);
        Document control = XMLUnit.buildControlDocument(expected);

        assertXMLEqual(control, test);
    }
    
    public void testSubprocess() throws Exception {
        Name areaName = new NameImpl("JTS", "area");
        
        InputParameterValues areaGeomValues = new InputParameterValues(areaName, "geom");
        ParameterValue geom = areaGeomValues.values.get(0);
        geom.setType(ParameterType.SUBPROCESS);
        geom.setValue(getExecuteBuffer());
        
        OutputParameter bufferOutput = new OutputParameter(areaName, "result");
        
        ExecuteRequest executeArea = new ExecuteRequest(areaName.getURI(), Arrays.asList(
                areaGeomValues), Arrays.asList(bufferOutput));
        
        WPSExecuteTransformer tx = new WPSExecuteTransformer();
        tx.setIndentation(2);
        String xml = tx.transform(executeArea);
        // System.out.println(xml);
        
        String expected = 
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
        		"<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n" + 
        		"  <ows:Identifier>JTS:area</ows:Identifier>\n" + 
        		"  <wps:DataInputs>\n" + 
        		"    <wps:Input>\n" + 
        		"      <ows:Identifier>geom</ows:Identifier>\n" + 
        		"      <wps:Reference mimeType=\"text/xml; subtype=gml/3.1.1\" xlink:href=\"http://geoserver/wps\" method=\"POST\">\n" + 
        		"        <wps:Body>\n" + 
        		"          <wps:Execute version=\"1.0.0\" service=\"WPS\">\n" + 
        		"            <ows:Identifier>JTS:buffer</ows:Identifier>\n" + 
        		"            <wps:DataInputs>\n" + 
        		"              <wps:Input>\n" + 
        		"                <ows:Identifier>geom</ows:Identifier>\n" + 
        		"                <wps:Data>\n" + 
        		"                  <wps:ComplexData mimeType=\"application/wkt\"><![CDATA[POINT(0 0)]]></wps:ComplexData>\n" + 
        		"                </wps:Data>\n" + 
        		"              </wps:Input>\n" + 
        		"              <wps:Input>\n" + 
        		"                <ows:Identifier>distance</ows:Identifier>\n" + 
        		"                <wps:Data>\n" + 
        		"                  <wps:LiteralData>10</wps:LiteralData>\n" + 
        		"                </wps:Data>\n" + 
        		"              </wps:Input>\n" + 
        		"            </wps:DataInputs>\n" + 
        		"            <wps:ResponseForm>\n" + 
        		"              <wps:RawDataOutput mimeType=\"text/xml; subtype=gml/3.1.1\">\n" + 
        		"                <ows:Identifier>result</ows:Identifier>\n" + 
        		"              </wps:RawDataOutput>\n" + 
        		"            </wps:ResponseForm>\n" + 
        		"          </wps:Execute>\n" + 
        		"        </wps:Body>\n" + 
        		"      </wps:Reference>\n" + 
        		"    </wps:Input>\n" + 
        		"  </wps:DataInputs>\n" + 
        		"  <wps:ResponseForm>\n" + 
        		"    <wps:RawDataOutput>\n" + 
        		"      <ows:Identifier>result</ows:Identifier>\n" + 
        		"    </wps:RawDataOutput>\n" + 
        		"  </wps:ResponseForm>\n" + 
        		"</wps:Execute>";
        
        Document test = XMLUnit.buildTestDocument(xml);
        checkValidationErrors(test);
        Document control = XMLUnit.buildControlDocument(expected);

        assertXMLEqual(control, test);
    }

    private ExecuteRequest getExecuteBuffer() {
        Name bufferName = new NameImpl("JTS", "buffer");
        InputParameterValues bufferGeomValues = new InputParameterValues(bufferName, "geom");
        ParameterValue geom = bufferGeomValues.values.get(0);
        geom.setMime("application/wkt");
        geom.setType(ParameterType.TEXT);
        geom.setValue("POINT(0 0)");

        InputParameterValues bufferDistanceValues = new InputParameterValues(bufferName, "distance");
        ParameterValue distance = bufferDistanceValues.values.get(0);
        distance.setType(ParameterType.LITERAL);
        distance.setValue("10");

        OutputParameter bufferOutput = new OutputParameter(bufferName, "result");

        ExecuteRequest executeBuffer = new ExecuteRequest(bufferName.getURI(), Arrays.asList(
                bufferGeomValues, bufferDistanceValues), Arrays.asList(bufferOutput));
        return executeBuffer;
    }
    
    /**
     * Validates a document against the 
     * @param dom
     * @param configuration
     */
    protected void checkValidationErrors(Document dom) throws Exception {
        Parser p = new Parser(new WPSConfiguration());
        p.setValidating( true );
        p.parse( new DOMSource( dom ) );
    
        if ( !p.getValidationErrors().isEmpty() ) {
            for ( Iterator e = p.getValidationErrors().iterator(); e.hasNext(); ) {
                SAXParseException ex = (SAXParseException) e.next();
                System.out.println( ex.getLineNumber() + "," + ex.getColumnNumber() + " -" + ex.toString()  );
            }
            fail( "Document did not validate.");
        }
    }
}
