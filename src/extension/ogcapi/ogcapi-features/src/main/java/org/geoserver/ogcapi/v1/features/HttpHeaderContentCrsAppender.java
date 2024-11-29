/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.features;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.util.logging.Logging;

/**
 * Sets the Content-Crs header on the response, if the response is a feature collection response.
 */
public class HttpHeaderContentCrsAppender extends AbstractDispatcherCallback {
    static final Logger LOGGER = Logging.getLogger(HttpHeaderContentCrsAppender.class);
    /**
     * Name of the header stating the CRS of the response see
     * https://docs.ogc.org/is/18-058/18-058.html#_coordinate_reference_system_information_independent_of_the_feature_encoding
     */
    public static final String CRS_RESPONSE_HEADER = "Content-Crs";

    @Override
    public Response responseDispatched(
            Request request, Operation operation, Object result, Response response) {

        // is this a feature response we are about to encode?
        if (result instanceof FeaturesResponse) {
            HttpServletResponse httpResponse = request.getHttpResponse();
            FeatureCollectionResponse fcr = ((FeaturesResponse) result).getResponse();
            CoordinateReferenceSystem crs =
                    Optional.ofNullable(fcr)
                            .map(fct -> fct.getFeatures().get(0))
                            .map(fc -> fc.getSchema())
                            .map(ft -> ft.getCoordinateReferenceSystem())
                            .orElse(null);
            if (crs != null) {
                try {
                    String crsURI = FeatureService.getCRSURI(crs);
                    if (crsURI != null) {
                        httpResponse.addHeader(CRS_RESPONSE_HEADER, "<" + crsURI + ">");
                    }
                } catch (FactoryException e) {
                    LOGGER.log(
                            Level.INFO,
                            "Failed to lookup EPSG code of CRS, won't set the Content-Crs header."
                                    + crs,
                            e);
                }
            }
        }

        return response;
    }
}
