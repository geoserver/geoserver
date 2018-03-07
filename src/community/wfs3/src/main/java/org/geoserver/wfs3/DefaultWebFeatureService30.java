/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3;

import net.opengis.wfs20.GetFeatureType;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.WebFeatureService20;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.Feature;
import org.opengis.filter.FilterFactory2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * WFS 3.0 implementation
 */
public class DefaultWebFeatureService30 implements WebFeatureService30 {

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
    public ContentsDocument contents(ContentRequest request) {
        ContentsDocument contents = new ContentsDocument(request, geoServer.getService(WFSInfo.class), geoServer
                .getCatalog());
        return contents;
    }

    @Override
    public APIDocument api(APIRequest request) {
        return new APIDocument(geoServer.getService(WFSInfo.class), geoServer.getCatalog());
    }

    @Override
    public Object getFeature(GetFeatureType request) {
        FeatureCollectionResponse response = wfs20.getFeature(request);
        // was it a single feature request? Getting at the dispatcher thread local, as delving into the
        // request model is hard, the feature id is buried as a filter in one query object
//        Request dr = Dispatcher.REQUEST.get();
//        if (dr != null && dr.getKvp().get("featureId") != null && dr.getKvp().get("format").equals(BaseRequest
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
     * Returns a selection of supported formats favouring
     *
     * @return
     */
    public static List<String> getAvailableFormats() {
        Set<String> formatNames = new LinkedHashSet<>();
        Collection featureProducers = GeoServerExtensions.extensions(WFSGetFeatureOutputFormat.class);
        for (Iterator i = featureProducers.iterator(); i.hasNext(); ) {
            WFSGetFeatureOutputFormat format = (WFSGetFeatureOutputFormat) i.next();
            // TODO: get better collaboration from content
            Set<String> formats = format.getOutputFormats();
            if (formats.isEmpty()) {
                continue;
            }
            // try to get a MIME type, otherwise pick the first available
            String formatName = formats.stream()
                    .filter(f -> f.contains("/"))
                    .findFirst()
                    .orElse(formats.iterator().next());
            // hack to skip over the JSONP format, not recognizable as is
            if ("json".equals(formatName)) {
                continue;
            }
            formatNames.add(formatName);
        }
        return new ArrayList<>(formatNames);
    }
}
