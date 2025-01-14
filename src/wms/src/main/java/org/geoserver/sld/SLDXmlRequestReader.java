/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sld;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.Styles;
import org.geoserver.ows.XmlRequestReader;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.map.ProcessStandaloneSLDVisitor;
import org.geotools.api.style.StyledLayerDescriptor;
import org.geotools.util.Version;
import org.xml.sax.EntityResolver;

/**
 * Reads POST request bodies for StyledLayerDescriptor
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public class SLDXmlRequestReader extends XmlRequestReader {

    private WMS wms;

    public SLDXmlRequestReader(WMS wms) {
        super("http://www.opengis.net/sld", "StyledLayerDescriptor");
        this.wms = wms;
    }

    @Override
    public Object read(Object request, Reader reader, @SuppressWarnings("rawtypes") Map kvp) throws ServiceException {
        if (request == null) {
            throw new IllegalArgumentException("request must be not null");
        }
        try {
            GetMapRequest getMap = (GetMapRequest) request;
            String styleFormat = getMap.getStyleFormat();
            StyleHandler styleParser = Styles.handler(styleFormat);

            Version styleVersion = getMap.styleVersion();

            EntityResolver entityResolver = wms.getCatalog().getResourcePool().getEntityResolver();

            StyledLayerDescriptor sld = styleParser.parse(reader, styleVersion, null, entityResolver);

            // process the sld
            sld.accept(new ProcessStandaloneSLDVisitor(wms, getMap));

            return getMap;
        } catch (IOException e) {
            throw new ServiceException(cleanException(e));
        }
    }
}
