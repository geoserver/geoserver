/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wfs.WfsFactory;
import org.geoserver.feature.RetypingFeatureCollection;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.featureinfo.FeatureCollectionDecorator;
import org.geoserver.wms.featureinfo.LayerIdentifier;
import org.geotools.api.data.Query;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.util.logging.Logging;

/**
 * WMS GetFeatureInfo operation
 *
 * @author Gabriel Roldan
 */
public class GetFeatureInfo {
    static final Logger LOGGER = Logging.getLogger(GetFeatureInfo.class);

    private final WMS wms;

    public GetFeatureInfo(WMS wms) {
        this.wms = wms;
    }

    public FeatureCollectionType run(final GetFeatureInfoRequest request) throws ServiceException {
        List<FeatureCollection> results;
        try {
            results = execute(request);
        } catch (ServiceException se) {
            LOGGER.log(Level.FINE, "", se);
            throw se;
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "", e);
            throw new ServiceException("Internal error occurred", e);
        }
        return buildResults(results);
    }

    @SuppressWarnings("unchecked")
    private FeatureCollectionType buildResults(List<FeatureCollection> results) {

        FeatureCollectionType result = WfsFactory.eINSTANCE.createFeatureCollectionType();
        result.setTimeStamp(Calendar.getInstance());
        result.getFeature().addAll(results);

        return result;
    }

    private List<FeatureCollection> execute(GetFeatureInfoRequest request) throws Exception {
        final List<MapLayerInfo> requestedLayers = request.getQueryLayers();
        FeatureInfoRequestParameters requestParams = new FeatureInfoRequestParameters(request);

        List<FeatureCollection> results = new ArrayList<>(requestedLayers.size());

        int maxFeatures = request.getFeatureCount();
        List<LayerIdentifier> identifiers = GeoServerExtensions.extensions(LayerIdentifier.class);
        for (final MapLayerInfo layer : requestedLayers) {
            try {
                LayerIdentifier<?> identifier = getLayerIdentifier(layer, identifiers);
                if (request.getGetMapRequest() != null) {
                    List<Object> times = request.getGetMapRequest().getTime();
                    List<Object> elevations = request.getGetMapRequest().getElevation();
                    if (layer.getType() == MapLayerInfo.TYPE_VECTOR) {
                        wms.checkMaxDimensions(layer, times, elevations, false);
                    } else if (layer.getType() == MapLayerInfo.TYPE_RASTER) {
                        wms.checkMaxDimensions(layer, times, elevations, true);
                    }
                }
                List<FeatureCollection> identifiedCollections = identifier.identify(requestParams, maxFeatures);
                if (identifiedCollections != null) {
                    for (FeatureCollection identifierCollection : identifiedCollections) {
                        FeatureCollection fc = selectProperties(requestParams, identifierCollection);
                        maxFeatures = addToResults(fc, results, layer, request, maxFeatures);
                    }

                    // exit when we have collected enough features
                    if (maxFeatures <= 0) {
                        break;
                    }
                }
            } catch (Exception e) {
                if (e instanceof ServiceException) throw e;
                throw new ServiceException("Failed to run GetFeatureInfo on layer " + layer.getName(), e);
            }

            requestParams.nextLayer();
        }
        return results;
    }

    private LayerIdentifier getLayerIdentifier(MapLayerInfo layer, List<LayerIdentifier> identifiers) {
        for (LayerIdentifier identifier : identifiers) {
            if (identifier.canHandle(layer)) {
                return identifier;
            }
        }

        throw new ServiceException("Could not find any identifier that can handle layer "
                + layer.getLayerInfo().prefixedName()
                + " among these identifiers: "
                + identifiers);
    }

    private int addToResults(
            FeatureCollection collection,
            List<FeatureCollection> results,
            final MapLayerInfo layer,
            GetFeatureInfoRequest request,
            int maxFeatures) {
        if (collection != null) {
            if (!(collection.getSchema() instanceof SimpleFeatureType)) {
                // put wrapper around it with layer name
                Name name = new NameImpl(
                        layer.getFeature().getNamespace().getName(),
                        layer.getFeature().getName());
                collection = new FeatureCollectionDecorator(name, collection);
            }

            int size = collection.size();
            if (size != 0) {

                // HACK HACK HACK
                // For complex features, we need the targetCrs and version in scenario where we have
                // a top level feature that does not contain a geometry(therefore no crs) and has a
                // nested feature that contains geometry as its property.Furthermore it is possible
                // for each nested feature to have different crs hence we need to reproject on each
                // feature accordingly.
                // This is a Hack, this information should not be passed through feature type
                // appschema will need to remove this information from the feature type again
                if (!(collection instanceof SimpleFeatureCollection)) {
                    collection
                            .getSchema()
                            .getUserData()
                            .put("targetCrs", request.getGetMapRequest().getCrs());
                    collection.getSchema().getUserData().put("targetVersion", "wms:getfeatureinfo");
                }

                results.add(collection);

                // don't return more than FEATURE_COUNT
                maxFeatures -= size;
                if (maxFeatures <= 0) {
                    return 0;
                }
            }
        }
        return maxFeatures;
    }

    protected FeatureCollection selectProperties(FeatureInfoRequestParameters params, FeatureCollection collection)
            throws IOException {
        // no general way to reduce attribute names in complex features yet
        String[] names = params.getPropertyNames();
        if (names != Query.ALL_NAMES && collection != null && collection.getSchema() instanceof SimpleFeatureType) {
            // some collection wrappers can be made of simple features without being a
            // SimpleFeatureCollection, e.g. FilteringFeatureCollection
            @SuppressWarnings("unchecked")
            SimpleFeatureCollection sfc = DataUtilities.simple(collection);
            SimpleFeatureType source = sfc.getSchema();
            SimpleFeatureType target = SimpleFeatureTypeBuilder.retype(source, names);
            if (!target.equals(source)) {
                return new RetypingFeatureCollection(sfc, target);
            }
        }

        return collection;
    }
}
