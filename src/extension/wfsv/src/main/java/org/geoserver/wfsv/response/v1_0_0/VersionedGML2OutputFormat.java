/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfsv.response.v1_0_0;

import java.util.Map;

import net.opengis.wfs.GetFeatureType;
import net.opengis.wfs.ResultTypeType;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.Operation;
import org.geoserver.wfs.xml.GML2OutputFormat;

/**
 * Encodes features in Geographic Markup Language (GML) version 2 adding the
 * versioning attributes to the mix.
 *
 * <p>
 * GML2-GZIP format is just GML2 with gzip compression. If GML2-GZIP format was
 * requested, <code>getContentEncoding()</code> will retutn
 * <code>"gzip"</code>, otherwise will return <code>null</code>
 * </p>
 *
 * @author Gabriel Rold?n
 * @author Andrea Aime
 * @version $Id$
 */
public class VersionedGML2OutputFormat extends GML2OutputFormat {
    /**
     * Creates the producer with a reference to the GetFeature operation
     * using it.
     */
    public VersionedGML2OutputFormat(GeoServer geoServer) {
        super(geoServer);
    }
    
    protected String wfsSchemaLocation(GeoServerInfo global, String baseUrl) {
        return ResponseUtils.buildSchemaURL(baseUrl, "/wfs/1.0.0/WFS-versioning.xsd");
    }

    protected String typeSchemaLocation(GeoServerInfo global, FeatureTypeInfo meta, String baseUrl) {
        Map<String, String> params = ResponseUtils.params("service", "WFS",
                "version", "1.0.0",
                "request", "DescribeVersionedFeatureType",
                "typeName", meta.getName());
        return ResponseUtils.buildURL(baseUrl, "wfsv", params, URLType.SERVICE);
    }
    
    
    
    public boolean canHandle(Operation operation) {
        // GetVersionedFeature operation?
        if ("GetVersionedFeature".equalsIgnoreCase(operation.getId())) {
            // also check that the resultType is "results"
            GetFeatureType request = (GetFeatureType) OwsUtils.parameter(
                    operation.getParameters(), GetFeatureType.class);

            if (request.getResultType() == ResultTypeType.RESULTS_LITERAL) {
                return true;
            }
        }

        return false;
    }

}
