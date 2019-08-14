/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.response;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.geoserver.csw.CSWException;
import org.geoserver.csw.records.RecordDescriptor;
import org.geoserver.csw.store.CatalogStore;
import org.geoserver.ows.XmlObjectEncodingResponse;
import org.geotools.gml3.GML;
import org.geotools.xlink.XLINK;
import org.geotools.xsd.Encoder;

/**
 * A response designed to encode a specific object into XML
 *
 * @author Andrea Aime - GeoSolutions
 */
public class CSWObjectEncodingResponse extends XmlObjectEncodingResponse {

    private CatalogStore catalogStore;

    public CSWObjectEncodingResponse(
            CatalogStore catalogStore,
            Class<?> binding,
            String elementName,
            Class<?> xmlConfiguration) {
        super(binding, elementName, xmlConfiguration);
        this.catalogStore = catalogStore;
    }

    @Override
    protected Map<String, String> getSchemaLocations() {
        Map<String, String> locations = new HashMap<String, String>();
        locations.put(
                "http://www.opengis.net/cat/csw/2.0.2",
                "http://schemas.opengis.net/csw/2.0.2/CSW-discovery.xsd");
        return locations;
    }

    @Override
    protected void configureEncoder(
            Encoder encoder, String elementName, Class<?> xmlConfiguration) {
        encoder.setNamespaceAware(true);
        encoder.getNamespaces().declarePrefix("gml", GML.NAMESPACE);
        encoder.getNamespaces().declarePrefix("xlink", XLINK.NAMESPACE);
        try {
            for (RecordDescriptor rd : catalogStore.getRecordDescriptors()) {
                java.util.Enumeration<?> declared = rd.getNamespaceSupport().getDeclaredPrefixes();
                while (declared.hasMoreElements()) {
                    String prefix1 = declared.nextElement().toString();
                    encoder.getNamespaces()
                            .declarePrefix(prefix1, rd.getNamespaceSupport().getURI(prefix1));
                }
            }
        } catch (IOException e) {
            throw new CSWException(e.getMessage(), e);
        }
    }
}
