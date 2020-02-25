/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3;

import io.swagger.v3.oas.models.OpenAPI;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs3.response.CollectionDocument;
import org.geoserver.wfs3.response.CollectionsDocument;
import org.geoserver.wfs3.response.ConformanceDocument;
import org.geoserver.wfs3.response.LandingPageDocument;
import org.geoserver.wfs3.response.StylesDocument;
import org.geoserver.wfs3.response.TilingSchemeDescriptionDocument;
import org.geoserver.wfs3.response.TilingSchemesDocument;
import org.geotools.util.Version;

public interface WebFeatureService30 {

    static final Version V3 = new Version("3.0.0");

    /** Returns the landing page of WFS 3.0 */
    LandingPageDocument landingPage(LandingPageRequest request);

    /**
     * Returns a description of the collection(s)
     *
     * @param request A {@link CollectionRequest}
     * @return A {@link CollectionsDocument} depending on the request
     */
    CollectionsDocument collections(CollectionsRequest request);

    /**
     * Returns a description of a single collection
     *
     * @param request A {@link CollectionRequest}
     * @return A {@link CollectionDocument}
     */
    CollectionDocument collection(CollectionRequest request);

    /** The OpenAPI description of the service */
    OpenAPI api(APIRequest request);

    /** The conformance declaration for this service */
    ConformanceDocument conformance(ConformanceRequest request);

    /** Queries features and returns them */
    FeatureCollectionResponse getFeature(GetFeatureType request);

    /** Tiling Schemes available list */
    TilingSchemesDocument tilingSchemes(TilingSchemesRequest request);

    /** Tiling Scheme detail */
    TilingSchemeDescriptionDocument describeTilingScheme(TilingSchemeDescriptionRequest request);

    /** Queries Features for the requested tile coordinate */
    FeatureCollectionResponse getTile(GetFeatureType request);

    StylesDocument getStyles(GetStylesRequest request) throws IOException;

    StyleInfo getStyle(GetStyleRequest request) throws IOException;

    void postStyles(HttpServletRequest request, HttpServletResponse response, PostStyleRequest post)
            throws IOException;

    void putStyle(
            HttpServletRequest request, HttpServletResponse response, PutStyleRequest putStyle)
            throws IOException;

    void deleteStyle(DeleteStyleRequest request, HttpServletResponse response) throws IOException;
}
