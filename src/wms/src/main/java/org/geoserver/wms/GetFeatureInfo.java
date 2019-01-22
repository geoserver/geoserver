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
import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wfs.WfsFactory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.featureinfo.FeatureCollectionDecorator;
import org.geoserver.wms.featureinfo.LayerIdentifier;
import org.geotools.data.DataUtilities;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;

/**
 * WMS GetFeatureInfo operation
 *
 * @author Gabriel Roldan
 */
public class GetFeatureInfo {

    @SuppressWarnings({"unchecked", "rawtypes"})
    public FeatureCollectionType run(final GetFeatureInfoRequest request) throws ServiceException {
        List<FeatureCollection> results;
        try {
            results = execute(request);
        } catch (ServiceException se) {
            se.printStackTrace();
            throw se;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException("Internal error occurred", e);
        }
        return buildResults(results);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private FeatureCollectionType buildResults(List<FeatureCollection> results) {

        FeatureCollectionType result = WfsFactory.eINSTANCE.createFeatureCollectionType();
        result.setTimeStamp(Calendar.getInstance());
        result.getFeature().addAll(results);

        return result;
    }

    @SuppressWarnings("rawtypes")
    private List<FeatureCollection> execute(GetFeatureInfoRequest request) throws Exception {
        final List<MapLayerInfo> requestedLayers = request.getQueryLayers();
        FeatureInfoRequestParameters requestParams = new FeatureInfoRequestParameters(request);

        List<FeatureCollection> results = new ArrayList<FeatureCollection>(requestedLayers.size());

        int maxFeatures = request.getFeatureCount();
        List<LayerIdentifier> identifiers = GeoServerExtensions.extensions(LayerIdentifier.class);
        for (int i = 0; i < requestedLayers.size(); i++) {
            final MapLayerInfo layer = requestedLayers.get(i);
            try {
                LayerIdentifier identifier = getLayerIdentifier(layer, identifiers);
                List<FeatureCollection> identifiedCollections =
                        identifier.identify(requestParams, maxFeatures);
                if (identifiedCollections != null) {
                    for (FeatureCollection identifierCollection : identifiedCollections) {
                        FeatureCollection fc =
                                selectProperties(requestParams, identifierCollection);
                        maxFeatures = addToResults(fc, results, layer, request, maxFeatures);
                    }

                    // exit when we have collected enough features
                    if (maxFeatures <= 0) {
                        break;
                    }
                }
            } catch (Exception e) {
                throw new ServiceException(
                        "Failed to run GetFeatureInfo on layer " + layer.getName(), e);
            }

            requestParams.nextLayer();
        }
        return results;
    }

    private LayerIdentifier getLayerIdentifier(
            MapLayerInfo layer, List<LayerIdentifier> identifiers) {
        for (LayerIdentifier identifier : identifiers) {
            if (identifier.canHandle(layer)) {
                return identifier;
            }
        }

        throw new ServiceException(
                "Could not find any identifier that can handle layer "
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
                Name name =
                        new NameImpl(
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

    protected FeatureCollection selectProperties(
            FeatureInfoRequestParameters params, FeatureCollection collection) throws IOException {
        String[] names = params.getPropertyNames();
        if (names != Query.ALL_NAMES) {
            Query q =
                    new Query(
                            collection.getSchema().getName().getLocalPart(), Filter.INCLUDE, names);
            return DataUtilities.source(collection).getFeatures(q);
        } else {
            return collection;
        }
    }
}
