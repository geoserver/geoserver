/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.csw.response;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.geoserver.ows.XmlObjectEncodingResponse;
import org.geoserver.ows.xml.v1_0.OWS;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geotools.filter.v1_1.OGC;
import org.geotools.gml3.v3_2.GML;
import org.geotools.xlink.XLINK;
import org.geotools.xml.Configuration;
import org.geotools.xml.Encoder;

/**
 * A response designed to encode a specific object into XML
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class CSWObjectEncodingResponse extends XmlObjectEncodingResponse {

    public CSWObjectEncodingResponse(Class binding, String elementName, Class xmlConfiguration) {
        super(binding, elementName, xmlConfiguration);
    }

    @Override
    protected Map<String, String> getSchemaLocations() {
        Map<String, String> locations = new HashMap<String, String>();
        locations.put("http://www.opengis.net/cat/csw/2.0.2",
                "http://schemas.opengis.net/csw/2.0.2/CSW-discovery.xsd");
        return locations;
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation) throws IOException,
            ServiceException {
        try {
            Configuration c = (Configuration) xmlConfiguration.newInstance();
            Encoder encoder = new Encoder(c);
            for (Map.Entry<String, String> entry : getSchemaLocations().entrySet()) {
                encoder.setSchemaLocation(entry.getKey(), entry.getValue());
            }

            encoder.setIndenting(true);
            encoder.setNamespaceAware(true);
            encoder.getNamespaces().declarePrefix("ows", OWS.NAMESPACE);
            encoder.getNamespaces().declarePrefix("ogc", OGC.NAMESPACE);
            encoder.getNamespaces().declarePrefix("gml", GML.NAMESPACE);
            encoder.getNamespaces().declarePrefix("xlink", XLINK.NAMESPACE);
            //String encoded = encoder.encodeAsString(caps, CSW.Capabilities)
            
            encoder.encode(value, new QName(c.getXSD().getNamespaceURI(), elementName), output);
        } catch (Exception e) {
            throw (IOException) new IOException().initCause(e);
        }
    }

}