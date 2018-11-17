/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3;

import io.swagger.v3.oas.models.OpenAPI;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs3.response.CollectionDocument;
import org.geoserver.wfs3.response.CollectionsDocument;
import org.geoserver.wfs3.response.ConformanceDocument;
import org.geoserver.wfs3.response.LandingPageDocument;
import org.geoserver.wfs3.response.TilingSchemeDescriptionDocument;
import org.geoserver.wfs3.response.TilingSchemesDocument;
import org.geotools.util.Version;

public interface WebFeatureService30 {

    static final Version V3 = new Version("3.0.0");

    /**
     * Returns the landing page of WFS 3.0
     *
     * @param request
     * @return
     */
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

    /**
     * The OpenAPI description of the service
     *
     * @param request
     * @return
     */
    OpenAPI api(APIRequest request);

    /**
     * The conformance declaration for this service
     *
     * @param request
     * @return
     */
    ConformanceDocument conformance(ConformanceRequest request);

    /**
     * Queries features and returns them
     *
     * @param request
     * @return
     */
    FeatureCollectionResponse getFeature(GetFeatureType request);

    /** Tiling Schemes available list */
    TilingSchemesDocument tilingSchemes(TilingSchemesRequest request);

    /** Tiling Scheme detail */
    TilingSchemeDescriptionDocument describeTilingScheme(TilingSchemeDescriptionRequest request);

    /** Queries Features for the requested tile coordinate */
    FeatureCollectionResponse getTile(GetFeatureType request);
}
