/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.decorator;

import static org.geoserver.ows.util.ResponseUtils.*;

import java.io.IOException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.kml.KmlEncodingContext;
import org.geoserver.kml.decorator.KmlDecoratorFactory.KmlDecorator;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.wms.GetMapRequest;
import org.geotools.data.FeatureSource;
import org.geotools.map.Layer;
import org.opengis.feature.type.Name;

/**
 * Base class for GeoSearch placemark decorators
 *
 * @author Andrea Aime - GeoSolutions
 */
public abstract class AbstractGeoSearchDecorator implements KmlDecorator {

    protected String getFeatureTypeURL(KmlEncodingContext context) throws IOException {
        GeoServer gs = context.getWms().getGeoServer();
        Catalog catalog = gs.getCatalog();
        Layer layer = context.getCurrentLayer();
        FeatureSource featureSource = layer.getFeatureSource();
        Name typeName = featureSource.getSchema().getName();
        String nsUri = typeName.getNamespaceURI();
        NamespaceInfo ns = catalog.getNamespaceByURI(nsUri);
        String featureTypeName = typeName.getLocalPart();
        GetMapRequest request = context.getRequest();
        String baseURL = request.getBaseUrl();
        String prefix = ns.getPrefix();
        return buildURL(
                baseURL, appendPath("rest", prefix, featureTypeName), null, URLType.SERVICE);
    }
}
