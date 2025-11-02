/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.features;

import java.util.logging.Logger;
import org.geoserver.config.GeoServer;
import org.geoserver.json.GeoJSONFeatureWriter;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.util.logging.Logging;
import org.springframework.stereotype.Component;

@Component("JSONFGFeaturesResponse")
public class JSONFGFeaturesResponse extends RFCGeoJSONFeaturesResponse {

    static final Logger LOGGER = Logging.getLogger(JSONFGFeaturesResponse.class);

    /** The MIME type for this format */
    public static final String MIME_TYPE = "application/vnd.ogc.fg+json";
    /** The key holding the CRS URI in the JSON output */
    public static final String COORD_REF_SYS = "coordRefSys";

    public JSONFGFeaturesResponse(GeoServer gs) {
        super(gs, MIME_TYPE);
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return MIME_TYPE;
    }

    @Override
    protected <T extends FeatureType, F extends Feature> GeoJSONFeatureWriter<T, F> getFeatureWriter(
            FeatureCollectionResponse response, Operation operation) {
        return new JSONFGFeatureWriter<>(gs, response, getItemId());
    }

    @Override
    protected <T extends FeatureType, F extends Feature> GeoJSONFeatureWriter<T, F> getFeatureWriter(
            Operation operation) {
        return new JSONFGFeatureWriter<>(gs, null, getItemId());
    }
}
