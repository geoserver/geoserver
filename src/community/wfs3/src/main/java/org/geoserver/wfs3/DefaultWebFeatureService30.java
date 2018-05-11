/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.Parameter;
import net.opengis.wfs20.GetFeatureType;
import org.geoserver.ManifestLoader;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.Response;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.WebFeatureService20;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs3.response.CollectionsDocument;
import org.geoserver.wfs3.response.ConformanceDocument;
import org.geoserver.wfs3.response.LandingPageDocument;
import org.geotools.util.logging.Logging;
import org.opengis.filter.FilterFactory2;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.geoserver.wfs3.response.ConformanceDocument.CORE;
import static org.geoserver.wfs3.response.ConformanceDocument.GEOJSON;
import static org.geoserver.wfs3.response.ConformanceDocument.GMLSF0;
import static org.geoserver.wfs3.response.ConformanceDocument.OAS30;

/** WFS 3.0 implementation */
public class DefaultWebFeatureService30 implements WebFeatureService30 {

    private static final Logger LOGGER = Logging.getLogger(DefaultWebFeatureService30.class);
    private FilterFactory2 filterFactory;
    private final GeoServer geoServer;
    private WebFeatureService20 wfs20;

    public DefaultWebFeatureService30(GeoServer geoServer, WebFeatureService20 wfs20) {
        this.geoServer = geoServer;
        this.wfs20 = wfs20;
    }

    public FilterFactory2 getFilterFactory() {
        return filterFactory;
    }

    public void setFilterFactory(FilterFactory2 filterFactory) {
        this.filterFactory = filterFactory;
    }

    @Override
    public LandingPageDocument landingPage(LandingPageRequest request) {
        LandingPageDocument contents = new LandingPageDocument(request, getService(), getCatalog());
        return contents;
    }

    @Override
    public Object collections(CollectionsRequest request) {
        if (request.getTypeName() == null) {
            // all collections
            return new CollectionsDocument(request, getService(), getCatalog());
        } else {
            // single collection
            QName typeName = request.getTypeName();
            NamespaceInfo ns = getCatalog().getNamespaceByURI(typeName.getNamespaceURI());
            FeatureTypeInfo featureType =
                    getCatalog().getFeatureTypeByName(ns, typeName.getLocalPart());
            if (featureType == null) {
                throw new ServiceException(
                        "Unknown collection " + typeName,
                        ServiceException.INVALID_PARAMETER_VALUE,
                        "typeName");
            } else {
                return new CollectionsDocument(request, getService(), getCatalog(), featureType);
            }
        }
    }

    @Override
    public ConformanceDocument conformance(ConformanceRequest request) {
        List<String> classes = Arrays.asList(CORE, OAS30, GEOJSON, GMLSF0);
        return new ConformanceDocument(classes);
    }

    private Catalog getCatalog() {
        return geoServer.getCatalog();
    }

    private WFSInfo getService() {
        return geoServer.getService(WFSInfo.class);
    }

    @Override
    public Object getFeature(GetFeatureType request) {
        FeatureCollectionResponse response = wfs20.getFeature(request);
        // was it a single feature request? Getting at the dispatcher thread local, as delving into
        // the
        // request model is hard, the feature id is buried as a filter in one query object
        //        Request dr = Dispatcher.REQUEST.get();
        //        if (dr != null && dr.getKvp().get("featureId") != null &&
        // dr.getKvp().get("format").equals(BaseRequest
        //                .HTML_MIME)) {
        //            List<FeatureCollection> features = response.getFeature();
        //            try (FeatureIterator fi = features.get(0).features()) {
        //                // there should be one, WFS 2.0 should have already thrown a 404 otherwise
        //                Feature next = fi.next();
        //                return next;
        //            }
        //        } else {
        // normal encoding
        return response;
        //        }
    }

    /**
     * Returns a selection of supported formats for a given response object
     *
     * <p>TODO: this should be moved in a more central place, as it's of general utility (maybe the
     * filtering part could be made customizable via a lambda)
     *
     * @return A list of MIME types
     */
    public static List<String> getAvailableFormats(Class responseType) {
        Set<String> formatNames = new LinkedHashSet<>();
        Collection responses = GeoServerExtensions.extensions(Response.class);
        for (Iterator i = responses.iterator(); i.hasNext(); ) {
            Response format = (Response) i.next();
            if (!responseType.isAssignableFrom(format.getBinding())) {
                continue;
            }
            // TODO: get better collaboration from content
            Set<String> formats = format.getOutputFormats();
            if (formats.isEmpty()) {
                continue;
            }
            // try to get a MIME type, otherwise pick the first available
            formats.stream().filter(f -> f.contains("/")).forEach(f -> formatNames.add(f));
        }
        return new ArrayList<>(formatNames);
    }

    @Override
    public OpenAPI api(APIRequest request) {
       return new OpenAPIBuilder().build(request, getService());
    }
   
}
